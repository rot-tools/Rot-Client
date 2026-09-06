package fi.rotclient;

import fi.rotclient.mixin.SlotPositionAccessor;
import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compact storage projection. Server slots remain canonical:
 * this class only caches pages observed from real menus and all navigation is
 * sent back through an actual Storage selector slot or the real page command.
 */
public final class StorageOverlayRuntime {
    private record CachedPage(StorageOverlayPolicy.Page page, List<ItemStack> items, int rows) {}
    private record Hit(StorageOverlayPolicy.Page page, int x, int y, int width, int height) {}
    private record OriginalSlot(int x, int y) {}

    private static final Map<StorageOverlayPolicy.Page, CachedPage> CACHE = new LinkedHashMap<>();
    private static final Map<StorageOverlayPolicy.Page, Integer> SELECTOR_SLOTS = new LinkedHashMap<>();
    private static final List<Hit> HITS = new ArrayList<>();
    private static final IdentityHashMap<Slot, OriginalSlot> ORIGINALS = new IdentityHashMap<>();
    private static final int HIDDEN = -100000;

    private static AbstractContainerScreen<?> boundScreen;
    private static AbstractContainerScreen<?> observedScreen;
    private static final StorageMenuReadinessPolicy MENU_READINESS = new StorageMenuReadinessPolicy();
    private static final java.util.Set<StorageOverlayPolicy.Page> CONFIRMED_EMPTY_PAGES = new java.util.HashSet<>();
    private static StorageOverlayPolicy.OverlayLayout lastLayout;
    private static final RotClientScrollState PAGE_SCROLL = new RotClientScrollState();
    private static boolean cacheLoaded;
    private static boolean codecReady;
    private static boolean diskHadItems;
    private static boolean sawOverview;
    private static boolean userExiting;
    private static boolean searchFocused;
    private static StorageOverlayPolicy.Page liveTrustedPage;
    private static final ArrayDeque<StorageOverlayPolicy.Page> PREFETCH = new ArrayDeque<>();
    private static boolean prefetchActive;
    private static boolean prefetchDoneThisOpen;
    private static boolean userRequestedReload;
    private static boolean directoryScanCompleted;
    private static final LinkedHashSet<StorageOverlayPolicy.Page> PENDING_REFRESH = new LinkedHashSet<>();
    private static boolean returningToOverview;
    private static StorageOverlayPolicy.Page prefetchWaitingFor;
    private static int prefetchTicksOnPage;
    private static final Map<StorageOverlayPolicy.Page, String> PAGE_FINGERPRINTS = new LinkedHashMap<>();
    private static final Gson CACHE_GSON = new Gson();
    private static final AtomicBoolean CACHE_DIRTY = new AtomicBoolean();
    private static final AtomicBoolean CACHE_SAVE_SCHEDULED = new AtomicBoolean();
    private static final ScheduledExecutorService CACHE_IO = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "RotClient-StorageCache");
        thread.setDaemon(true);
        return thread;
    });
    private static final long CACHE_SAVE_DELAY_MS = 150L;
    private static String hoveredValueTip;
    private static boolean hoveredPageItem;

    static {
        // Nested records are separate class files. Touch them here so a later
        // JAR overwrite cannot ZipException mid-render on first CachedPage use.
        CachedPage.class.getName();
        Hit.class.getName();
        OriginalSlot.class.getName();
    }

    private StorageOverlayRuntime() {}

    public static boolean shouldReplaceVanilla(AbstractContainerScreen<?> screen) {
        if (screen == null || !settings().storageOverlayEnabled) return false;
        if (!InventoryOverlayPolicy.showSkyblockInventoryUi(SkyBlockAreaDetector.isInSkyblock())) {
            return false;
        }
        String title = screen.getTitle().getString();
        if (StorageOverlayPolicy.isOverviewTitle(title)) return true;
        return settings().storageOverlayAlwaysOpen
                && StorageOverlayPolicy.pageFromTitle(title).isPresent();
    }

    public static boolean isOverlayOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return false;
        }

        Screen current = client.gui.screen();
        return current instanceof AbstractContainerScreen<?> screen
                && shouldReplaceVanilla(screen);
    }

    public static StorageOverlayPolicy.OverlayLayout lastLayout() {
        return lastLayout;
    }

    static int cachedOwnedCount(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        int count = 0;
        for (CachedPage page : CACHE.values()) {
            for (ItemStack stack : page.items()) {
                if (!stack.isEmpty()
                        && itemId.equalsIgnoreCase(
                                SkyBlockItemData.marketId(stack))) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                              int left, int top, int guiWidth, int guiHeight,
                              int mouseX, int mouseY) {
        QolSkyblockExtras extras = settings();
        if (!extras.storageOverlayEnabled
                || !InventoryOverlayPolicy.showSkyblockInventoryUi(SkyBlockAreaDetector.isInSkyblock())
                || screen == null
                || graphics == null) {
            restoreSlots();
            lastLayout = null;
            searchFocused = false;
            HITS.clear();
            return;
        }
        loadCache();
        hoveredValueTip = null;
        hoveredPageItem = false;
        String title = screen.getTitle().getString();
        StorageOverlayPolicy.Page selected = StorageOverlayPolicy.pageFromTitle(title).orElse(null);
        boolean overview = StorageOverlayPolicy.isOverviewTitle(title);
        if (!overview && selected == null) {
            restoreSlots();
            lastLayout = null;
            searchFocused = false;
            HITS.clear();
            return;
        }
        userExiting = false;
        if (!overview && !extras.storageOverlayAlwaysOpen) {
            restoreSlots();
            lastLayout = null;
            searchFocused = false;
            HITS.clear();
            return;
        }
        observe(screen, overview, selected);
        StorageOverlayPolicy.OverlayLayout layout = StorageOverlayPolicy.layout(
                screen.width,
                screen.height,
                extras.storageOverlayColumns,
                extras.storageOverlayPadding,
                extras.storageOverlayHeight,
                extras.storageOverlayMargin);
        lastLayout = layout;
        List<CachedPage> pages = visiblePages(extras, selected);
        HITS.clear();
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int searchMatches = StorageOverlayPolicy.matchingNameCount(
                CACHE.values().stream()
                        .flatMap(page -> page.items().stream())
                        .filter(stack -> !stack.isEmpty())
                        .map(stack -> stack.getHoverName().getString())
                        .toList(),
                extras.storageOverlaySearchQuery);

        RotClientUiDraw.drawShadowedPanel(
                graphics,
                layout.panelX(),
                layout.panelY(),
                layout.panelWidth(),
                layout.panelHeight(), extras.storageOverlayPanelColor);
        RotClientUiDraw.roundedFill(
                graphics,
                layout.panelX(),
                layout.panelY(),
                layout.panelX() + layout.panelWidth(),
                layout.panelY() + StorageOverlayPolicy.HEADER_HEIGHT,
                RotClientTheme.DASHBOARD_HEADER,
                RotClientUiDraw.RADIUS_MD);
        graphics.fill(
                layout.panelX(),
                layout.panelY() + StorageOverlayPolicy.HEADER_HEIGHT - 1,
                layout.panelX() + layout.panelWidth(),
                layout.panelY() + StorageOverlayPolicy.HEADER_HEIGHT,
                RotClientTheme.DIVIDER);
        RotClientUiDraw.drawShadowedPanel(
                graphics,
                layout.playerX(),
                layout.playerY(),
                StorageOverlayPolicy.PLAYER_WIDTH,
                StorageOverlayPolicy.PLAYER_HEIGHT, extras.storageOverlayPlayerColor);
        RotClientUiDraw.text(graphics, font, "Storage",
                layout.innerX(), layout.panelY() + 7, RotClientTheme.TEXT, true);
        if (prefetchActive) {
            String loading = prefetchWaitingFor != null
                    ? "Loading " + prefetchWaitingFor.label()
                    : "Loading pages…";
            int loadX = layout.innerX() + RotClientFonts.width(font, "Storage") + 8;
            if (loadX + RotClientFonts.width(font, loading) < layout.searchX() - 6) {
                RotClientUiDraw.text(graphics, font, loading, loadX, layout.panelY() + 7,
                        RotClientTheme.HUD_ACCENT, false);
            }
        } else if (StorageOverlayPolicy.searching(extras.storageOverlaySearchQuery)) {
            String matches = searchMatches + (searchMatches == 1 ? " match" : " matches");
            int matchX = layout.innerX() + RotClientFonts.width(font, "Storage") + 8;
            if (matchX + RotClientFonts.width(font, matches) < layout.searchX() - 6) {
                RotClientUiDraw.text(graphics, font, matches, matchX, layout.panelY() + 7,
                        RotClientTheme.HUD_ACCENT, false);
            }
        }
        drawSearchField(graphics, font, layout, extras.storageOverlaySearchQuery);
        drawCloseButton(graphics, font, layout, mouseX, mouseY);

        int columns = StorageOverlayPolicy.layoutColumns(layout, extras.storageOverlayPadding);
        int[] rowHeights = rowHeights(pages, columns);
        int[] rowOffsets = rowOffsets(rowHeights);
        PAGE_SCROLL.setBounds(contentHeight(rowHeights), layout.innerHeight());
        PAGE_SCROLL.advance(System.nanoTime());
        int scroll = PAGE_SCROLL.scrollPixels();
        remapSlots(screen, layout, pages, selected, rowOffsets, left, top);

        graphics.enableScissor(layout.innerX(), layout.innerY(),
                layout.innerX() + layout.innerWidth(), layout.innerY() + layout.innerHeight());
        if (pages.isEmpty()) {
            RotClientUiDraw.text(graphics, font, extras.storageOverlayFilterSearch
                            ? "No cached page matches this search."
                            : prefetchActive
                                    ? "Loading Ender Chests and Backpacks…"
                                    : "Open Storage to load pages, or click a card.",
                    layout.innerX(), layout.innerY() + 8, RotClientTheme.TEXT_MUTED, false);
        } else {
            int padding = StorageOverlayPolicy.clampSpacing(extras.storageOverlayPadding);
            int cardWidth = StorageOverlayPolicy.pageWidth();
            for (int i = 0; i < pages.size(); i++) {
                CachedPage cached = pages.get(i);
                int col = i % columns;
                int row = i / columns;
                int cardHeight = cardHeight(cached);
                int x = layout.innerX() + col * (cardWidth + padding);
                int y = layout.innerY() - scroll + rowOffsets[row];
                drawPageCard(screen, graphics, font, extras, cached, selected, x, y, cardWidth, cardHeight,
                        mouseX, mouseY, layout);
                HITS.add(new Hit(cached.page, x, y, cardWidth, cardHeight));
            }
        }
        graphics.disableScissor();
        boolean barHovered = StorageOverlayPolicy.overScrollBar(layout, mouseX, mouseY);
        RotClientUiDraw.drawScrollbar(
                graphics,
                layout.scrollBarX(),
                layout.scrollBarY(),
                StorageOverlayPolicy.scrollBarTrackBottom(layout),
                PAGE_SCROLL.contentHeight(),
                scroll,
                barHovered || PAGE_SCROLL.isThumbDragging(),
                PAGE_SCROLL.isThumbDragging());
        drawPlayerInventory(screen, graphics, font, layout);
        ItemStack carried = screen.getMenu().getCarried();
        if (!carried.isEmpty()) {
            graphics.item(carried, mouseX - 8, mouseY - 8);
            graphics.itemDecorations(font, carried, mouseX - 8, mouseY - 8);
        }
    }

    public static Slot hoveredSlot(AbstractContainerScreen<?> screen, int left, int top, int mouseX, int mouseY) {
        if (screen == null || !shouldReplaceVanilla(screen) || !menuReady(screen)) return null;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.x <= HIDDEN / 2 || slot.y <= HIDDEN / 2) continue;
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                return slot;
            }
        }
        return null;
    }

    public static boolean shouldKeepCursorAcrossScreens(Screen previous, Screen next) {
        boolean previousOverlay = previous instanceof AbstractContainerScreen<?> prevScreen
                && shouldReplaceVanilla(prevScreen);
        boolean nextOverlay = next instanceof AbstractContainerScreen<?> nextScreen
                && shouldReplaceVanilla(nextScreen);
        return StorageOverlayPolicy.shouldKeepCursorOnScreenChange(
                settings().storageOverlayEnabled,
                userExiting,
                previousOverlay,
                nextOverlay,
                next == null);
    }

    public static boolean shouldPinClosedContainer(Screen previous) {
        if (userExiting || !(previous instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        return StorageOverlayPolicy.shouldPinScreenOnClose(
                shouldReplaceVanilla(screen), false, true);
    }

    public static void markUserExiting() {
        userExiting = true;
        searchFocused = false;
        lastLayout = null;
        liveTrustedPage = null;
        prefetchDoneThisOpen = false;
        cancelPrefetch();
        flushCacheToDisk();
    }

    private static void closeOverlay() {
        markUserExiting();
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.closeContainer();
        } else if (client != null && client.gui != null) {
            client.gui.setScreen(null);
        }
    }

    public static boolean shouldSuppressOutsideClick(double mouseX, double mouseY) {
        if (PAGE_SCROLL.isThumbDragging()) {
            return true;
        }
        return StorageOverlayPolicy.shouldSuppressOutsideClick(
                lastLayout != null && settings().storageOverlayEnabled,
                lastLayout,
                mouseX,
                mouseY);
    }

    public static boolean shouldKeepUngrabbedCursor() {
        Minecraft client = Minecraft.getInstance();
        Screen screen = client != null && client.gui != null ? client.gui.screen() : null;
        boolean currentOverlay = screen instanceof AbstractContainerScreen<?> current
                && shouldReplaceVanilla(current);
        return StorageOverlayPolicy.shouldKeepUngrabbedCursor(
                settings().storageOverlayEnabled,
                currentOverlay,
                userExiting);
    }

    public static boolean click(
            AbstractContainerScreen<?> screen,
            int left,
            int top,
            int mouseX,
            int mouseY,
            int button,
            boolean shiftDown) {
        if (!settings().storageOverlayEnabled || button != 0 || screen == null || lastLayout == null) {
            return false;
        }
        if (!shouldReplaceVanilla(screen)) {
            return false;
        }
        if (boundScreen != screen) return true;
        if (StorageOverlayPolicy.overCloseButton(lastLayout, mouseX, mouseY)
                || StorageOverlayPolicy.shouldCloseOnOutsideClick(true, lastLayout, mouseX, mouseY)) {
            closeOverlay();
            return true;
        }
        if (StorageOverlayPolicy.insideSearchField(lastLayout, mouseX, mouseY)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (handleScrollbarPress(mouseX, mouseY)) {
            return true;
        }
        if (hoveredSlot(screen, left, top, mouseX, mouseY) != null) {
            cancelPrefetch();
            return false;
        }
        String title = screen.getTitle().getString();
        StorageOverlayPolicy.Page selected = StorageOverlayPolicy.pageFromTitle(title).orElse(null);
        boolean overview = StorageOverlayPolicy.isOverviewTitle(title);
        for (Hit hit : HITS) {
            if (!StorageOverlayPolicy.shouldNavigatePage(
                    shiftDown,
                    false,
                    selected != null && selected.equals(hit.page),
                    mouseX,
                    mouseY,
                    hit.x,
                    hit.y,
                    hit.width,
                    hit.height,
                    lastLayout)) {
                continue;
            }
            rememberClickedPage(hit.page);
            cancelPrefetch();
            preserveCursor();
            Integer slot = SELECTOR_SLOTS.get(hit.page);
            Minecraft client = Minecraft.getInstance();
            if (overview
                    && menuReady(screen)
                    && slot != null
                    && StorageOverlayPolicy.isPhysicalSelectorSlot(slot)
                    && client.player != null
                    && client.gameMode != null) {
                client.gameMode.handleContainerInput(
                        screen.getMenu().containerId, slot, 0, ContainerInput.PICKUP, client.player);
            } else if (client.player != null && client.player.connection != null) {
                client.player.connection.sendCommand(StorageOverlayPolicy.pageCommand(hit.page));
            }
            return true;
        }
        return false;
    }

    public static boolean scroll(double verticalAmount, boolean overItem) {
        QolSkyblockExtras extras = settings();
        if (!extras.storageOverlayEnabled || lastLayout == null) {
            return false;
        }
        if (!PAGE_SCROLL.canScroll()
                || !StorageOverlayPolicy.allowPageScroll(
                        extras.storageOverlayBlockItemScroll, overItem || hoveredPageItem)) {
            return false;
        }
        double amount = extras.storageOverlayInvertScroll ? -verticalAmount : verticalAmount;
        PAGE_SCROLL.scrollBySteps(amount, StorageOverlayPolicy.clampScrollSpeed(extras.storageOverlayScrollSpeed));
        return amount != 0.0D;
    }

    public static boolean drag(int mouseY) {
        if (!settings().storageOverlayEnabled || lastLayout == null) {
            return false;
        }
        return PAGE_SCROLL.dragThumbTo(
                mouseY,
                lastLayout.scrollBarY(),
                StorageOverlayPolicy.scrollBarTrackBottom(lastLayout),
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT);
    }

    public static boolean mouseReleased() {
        return PAGE_SCROLL.endThumbDrag();
    }

    public static boolean charTyped(CharacterEvent event) {
        if (!searchFocused || event == null || !event.isAllowedChatCharacter()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null
                || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || !shouldReplaceVanilla(screen)) {
            searchFocused = false;
            return false;
        }
        QolSkyblockExtras extras = settings();
        extras.storageOverlaySearchQuery = StorageOverlayPolicy.appendSearchChar(
                extras.storageOverlaySearchQuery, event.codepointAsString());
        extras.storageOverlayHighlightSearch = true;
        return true;
    }

    public static boolean keyPressed(KeyEvent event) {
        if (!searchFocused || event == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null
                || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || !shouldReplaceVanilla(screen)) {
            searchFocused = false;
            return false;
        }
        QolSkyblockExtras extras = settings();
        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            extras.storageOverlaySearchQuery = StorageOverlayPolicy.deleteSearchChar(
                    extras.storageOverlaySearchQuery);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            searchFocused = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (StorageOverlayPolicy.searching(extras.storageOverlaySearchQuery)) {
                extras.storageOverlaySearchQuery = "";
                return true;
            }
            searchFocused = false;
            return false;
        }
        return true;
    }

    public static void highlightSearchMatches(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int left,
            int top) {
        if (screen == null || graphics == null || lastLayout == null) {
            return;
        }
        QolSkyblockExtras extras = settings();
        if (!extras.storageOverlayHighlightSearch
                || !StorageOverlayPolicy.searching(extras.storageOverlaySearchQuery)) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot.x <= HIDDEN / 2 || slot.y <= HIDDEN / 2) {
                continue;
            }
            int x = left + slot.x;
            int y = top + slot.y;
            ItemStack stack = slot.getItem();
            boolean match = !stack.isEmpty() && StorageOverlayPolicy.matchesSearch(
                    stack.getHoverName().getString(), extras.storageOverlaySearchQuery);
            paintSearchSlot(graphics, extras, x, y, match);
        }
    }

    public static void reloadCachedStacks() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }

        cacheLoaded = false;
        CACHE.clear();
        CONFIRMED_EMPTY_PAGES.clear();
        PAGE_FINGERPRINTS.clear();

        loadCache();

        System.err.println("[RotClient] Storage cache reloaded after resource reload");
    }

    public static void resetWorld() {
        observedScreen = null;
        MENU_READINESS.reset();
        HITS.clear();
        restoreSlots();
        lastLayout = null;
        searchFocused = false;
        userExiting = false;
        liveTrustedPage = null;
        prefetchDoneThisOpen = false;
        cancelPrefetch();
        if (!settings().storageOverlayRetainScroll) {
            PAGE_SCROLL.reset();
        }
        loadCache();
    }

    public static void onJoin() {
        observedScreen = null;
        MENU_READINESS.reset();
        liveTrustedPage = null;
        prefetchDoneThisOpen = false;
        userRequestedReload = false;
        cancelPrefetch();
        loadCache();
    }

    public static void tick(Minecraft client) {
        loadCache();
        if (client != null && client.gui != null
                && client.gui.screen() instanceof AbstractContainerScreen<?> screen
                && shouldReplaceVanilla(screen)) {
            String title = screen.getTitle().getString();
            observe(screen, StorageOverlayPolicy.isOverviewTitle(title),
                    StorageOverlayPolicy.pageFromTitle(title).orElse(null));
        } else {
            observedScreen = null;
            MENU_READINESS.reset();
            liveTrustedPage = null;
        }
        advancePrefetch(client);
    }

    /** Called after vanilla applies a full container snapshot on the client thread. */
    public static void onContainerContent(int containerId, int slotCount) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || client.player.containerMenu != screen.getMenu() || !shouldReplaceVanilla(screen)) return;
        loadCache();
        bindObservedScreen(screen);
        if (!MENU_READINESS.acceptContents(containerId, slotCount)) return;
        String title = screen.getTitle().getString();
        observe(screen, StorageOverlayPolicy.isOverviewTitle(title),
                StorageOverlayPolicy.pageFromTitle(title).orElse(null));
    }

    private static void bindObservedScreen(AbstractContainerScreen<?> screen) {
        if (observedScreen == screen) return;
        observedScreen = screen;
        liveTrustedPage = null;
        MENU_READINESS.beginVisit(screen.getMenu().containerId, screen.getMenu().slots.size());
    }

    private static boolean menuReady(AbstractContainerScreen<?> screen) {
        return observedScreen == screen && MENU_READINESS.ready();
    }

    /** Covers shift/number-key/drag actions routed through vanilla's slot handler. */
    public static boolean shouldBlockSlotInput(AbstractContainerScreen<?> screen, Slot slot,
                                               int slotId, ContainerInput input) {
        if (!shouldReplaceVanilla(screen)) return false;
        if (!menuReady(screen) || boundScreen != screen || lastLayout == null) return true;
        if (slot != null && (slot.x <= HIDDEN / 2 || slot.y <= HIDDEN / 2)) return true;
        // The overview is a selector menu, never a destination for shift-moved items.
        if (input == ContainerInput.QUICK_MOVE
                && StorageOverlayPolicy.isOverviewTitle(screen.getTitle().getString())) return true;
        cancelPrefetch();
        return false;
    }

    /** Opens Storage if needed and walks every unlocked Ender Chest / Backpack. */
    public static void requestReloadAll() {
        userRequestedReload = true;
        prefetchDoneThisOpen = false;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.connection == null) {
            return;
        }
        if (isOverlayOpen()) {
            Screen current = client.gui != null ? client.gui.screen() : null;
            if (current instanceof AbstractContainerScreen<?> screen
                    && StorageOverlayPolicy.isOverviewTitle(screen.getTitle().getString())) {
                maybeStartPrefetch(true);
                return;
            }
        }
        sendStorageCommand(StorageOverlayPolicy.OVERVIEW_COMMAND);
    }

    /** Clears only locally observed page contents and selectors. */
    public static void clearObservedPages() {
        HITS.clear();
        SELECTOR_SLOTS.clear();
        CACHE.clear();
        PAGE_FINGERPRINTS.clear();
        PENDING_REFRESH.clear();
        CONFIRMED_EMPTY_PAGES.clear();
        directoryScanCompleted = false;
        sawOverview = false;
        diskHadItems = false;
        liveTrustedPage = null;
        prefetchDoneThisOpen = false;
        cancelPrefetch();
        PAGE_SCROLL.reset();
        saveCacheNow();
    }

    private static List<CachedPage> visiblePages(QolSkyblockExtras extras, StorageOverlayPolicy.Page selected) {
        Map<StorageOverlayPolicy.Page, CachedPage> visiblePages = new LinkedHashMap<>(CACHE);
        for (StorageOverlayPolicy.Page page : SELECTOR_SLOTS.keySet()) {
            visiblePages.putIfAbsent(page, new CachedPage(page, List.of(), 0));
        }
        boolean filterSearch = extras.storageOverlayFilterSearch
                && !extras.storageOverlaySearchQuery.isBlank();
        return visiblePages.values().stream()
                .filter(page -> !filterSearch || page.page().equals(selected) || page.items().stream()
                        .filter(stack -> !stack.isEmpty())
                        .anyMatch(stack -> StorageOverlayPolicy.matchesSearch(
                                stack.getHoverName().getString(), extras.storageOverlaySearchQuery)))
                .sorted(Comparator.comparing((CachedPage p) -> p.page.kind()).thenComparingInt(p -> p.page.number()))
                .toList();
    }

    private static void drawPageCard(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            Font font,
            QolSkyblockExtras extras,
            CachedPage cached,
            StorageOverlayPolicy.Page selected,
            int x,
            int y,
            int cardWidth,
            int cardHeight,
            int mouseX,
            int mouseY,
            StorageOverlayPolicy.OverlayLayout layout) {
        boolean active = selected != null && selected.equals(cached.page);
        boolean pageSearchMatch = !extras.storageOverlaySearchQuery.isBlank()
                && cached.items.stream()
                .filter(stack -> !stack.isEmpty())
                .anyMatch(stack -> StorageOverlayPolicy.matchesSearch(
                        stack.getHoverName().getString(), extras.storageOverlaySearchQuery));
        graphics.fill(x, y, x + cardWidth, y + cardHeight,
                active ? extras.storageOverlayCardActiveColor : extras.storageOverlayCardColor);
        graphics.fill(x, y, x + cardWidth, y + StorageOverlayPolicy.CARD_HEADER_HEIGHT,
                RotClientTheme.HUD_HEADER);
        int border = active && extras.storageOverlayOutlineActive
                ? extras.storageOverlayOutlineColor
                : pageSearchMatch ? RotClientTheme.VIOLET : RotClientTheme.BORDER;
        RotClientUiDraw.roundedOutline(graphics, x, y, x + cardWidth, y + cardHeight, border);
        String label = RotClientUiDraw.ellipsize(
                font, cached.page.label(), StorageOverlayPolicy.headerLabelMaxWidth(cardWidth));
        RotClientUiDraw.text(graphics, font, label, x + 5, y + 5,
                active ? RotClientTheme.HUD_ACCENT : RotClientTheme.TEXT, true);
        drawValueIcon(graphics, font, cached, selected, screen, x, y, cardWidth, mouseX, mouseY);
        int containerSlots = Math.max(0, screen.getMenu().slots.size() - 36);
        int contentCount = StorageOverlayPolicy.contentSlotCount(containerSlots);
        int rows = StorageOverlayPolicy.slotRows(cached.rows, cached.items.isEmpty());
        int slots = rows * 9;
        if (active) {
            slots = Math.min(Math.max(0, contentCount), rows * 9);
        }
        boolean searching = extras.storageOverlayHighlightSearch
                && StorageOverlayPolicy.searching(extras.storageOverlaySearchQuery);
        int contentStart = StorageOverlayPolicy.contentSlotStart();
        for (int slot = 0; slot < slots; slot++) {
            int[] pos = StorageOverlayPolicy.contentSlotPosition(x, y, slot);
            drawSlotWell(graphics, pos[0], pos[1]);
            ItemStack live = active ? liveContentStack(screen, contentStart + slot) : ItemStack.EMPTY;
            ItemStack cachedStack = stackAt(cached, slot);
            ItemStack stack = StorageOverlayPolicy.useLiveDisplay(
                    active, selected != null && selected.equals(liveTrustedPage))
                    ? live
                    : cachedStack;
            boolean searchMatch = !stack.isEmpty() && StorageOverlayPolicy.matchesSearch(
                    stack.getHoverName().getString(), extras.storageOverlaySearchQuery);
            if (!stack.isEmpty()) {
                graphics.item(stack, pos[0], pos[1]);
                graphics.itemDecorations(font, stack, pos[0], pos[1]);
            }
            if (searching) {
                paintSearchSlot(graphics, extras, pos[0], pos[1], searchMatch);
            }
            if (!stack.isEmpty() && insideInner(layout, mouseX, mouseY)
                    && mouseX >= pos[0] && mouseX < pos[0] + 16
                    && mouseY >= pos[1] && mouseY < pos[1] + 16) {
                hoveredPageItem = true;
            }
            if (!active
                    && !stack.isEmpty()
                    && mouseX >= pos[0] && mouseX < pos[0] + 16 && mouseY >= pos[1] && mouseY < pos[1] + 16
                    && extras.storageOverlayInactiveTooltips
                    && insideInner(layout, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    private static void drawPlayerInventory(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            Font font,
            StorageOverlayPolicy.OverlayLayout layout) {
        RotClientUiDraw.text(graphics, font, "Inventory", layout.playerX() + 8, layout.playerY() + 4,
                RotClientTheme.TEXT, true);
        List<Slot> slots = screen.getMenu().slots;
        int playerStart = Math.max(0, slots.size() - 36);
        QolSkyblockExtras extras = settings();
        boolean searching = extras.storageOverlayHighlightSearch
                && StorageOverlayPolicy.searching(extras.storageOverlaySearchQuery);
        for (int i = playerStart; i < slots.size(); i++) {
            int invIndex = StorageOverlayPolicy.playerInventoryIndex(i - playerStart);
            int[] pos = StorageOverlayPolicy.playerSlotPosition(layout.playerX(), layout.playerY(), invIndex);
            drawSlotWell(graphics, pos[0], pos[1]);
            ItemStack stack = slots.get(i).getItem();
            if (!stack.isEmpty()) {
                graphics.item(stack, pos[0], pos[1]);
                graphics.itemDecorations(font, stack, pos[0], pos[1]);
            }
            if (searching) {
                boolean match = !stack.isEmpty() && StorageOverlayPolicy.matchesSearch(
                        stack.getHoverName().getString(), extras.storageOverlaySearchQuery);
                paintSearchSlot(graphics, extras, pos[0], pos[1], match);
            }
        }
    }

    private static boolean handleScrollbarPress(int mouseX, int mouseY) {
        if (lastLayout == null || !PAGE_SCROLL.canScroll()
                || !StorageOverlayPolicy.overScrollBar(lastLayout, mouseX, mouseY)) {
            return false;
        }
        int trackTop = lastLayout.scrollBarY();
        int trackBottom = StorageOverlayPolicy.scrollBarTrackBottom(lastLayout);
        int minThumb = RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT;
        if (PAGE_SCROLL.beginThumbDrag(mouseY, trackTop, trackBottom, minThumb)) {
            return true;
        }
        return PAGE_SCROLL.clickTrack(mouseY, trackTop, trackBottom, minThumb);
    }

    private static void drawSearchField(
            GuiGraphicsExtractor graphics,
            Font font,
            StorageOverlayPolicy.OverlayLayout layout,
            String query) {
        int x = layout.searchX();
        int y = layout.searchY();
        int width = layout.searchWidth();
        int height = layout.searchHeight();
        graphics.fill(x, y, x + width, y + height,
                searchFocused ? RotClientTheme.FIELD_ACTIVE : RotClientTheme.FIELD);
        int border = searchFocused ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER;
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);
        String shown = query == null ? "" : query;
        boolean placeholder = shown.isBlank();
        String text = placeholder ? "Search items..." : shown;
        int pad = 4;
        int maxText = Math.max(8, width - pad * 2 - 1);
        if (!placeholder) {
            text = StorageOverlayPolicy.clipSearchFromEnd(
                    shown, maxText, value -> RotClientFonts.width(font, value));
        }
        int color = placeholder ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT;
        RotClientUiDraw.text(graphics, font, text, x + pad, y + 3, color, false);
        if (searchFocused && !placeholder && (System.currentTimeMillis() / 400L) % 2L == 0L) {
            int caretX = StorageOverlayPolicy.searchCaretX(
                    x, pad, width, RotClientFonts.width(font, text));
            graphics.fill(caretX, y + 3, caretX + 1, y + height - 3, RotClientTheme.HUD_ACCENT);
        }
    }

    private static void drawCloseButton(
            GuiGraphicsExtractor graphics,
            Font font,
            StorageOverlayPolicy.OverlayLayout layout,
            int mouseX,
            int mouseY) {
        int x = layout.closeX();
        int y = layout.closeY();
        int size = layout.closeSize();
        boolean hover = StorageOverlayPolicy.overCloseButton(layout, mouseX, mouseY);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + size,
                y + size,
                hover ? RotClientTheme.WARNING : RotClientTheme.BUTTON,
                RotClientUiDraw.RADIUS_XS);
        RotClientUiDraw.text(
                graphics,
                font,
                "×",
                x + 3,
                y + 2,
                hover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                false);
    }

    private static void drawValueIcon(
            GuiGraphicsExtractor graphics,
            Font font,
            CachedPage cached,
            StorageOverlayPolicy.Page selected,
            AbstractContainerScreen<?> screen,
            int cardX,
            int cardY,
            int cardWidth,
            int mouseX,
            int mouseY) {
        int[] icon = StorageOverlayPolicy.valueIconPosition(cardX, cardY, cardWidth);
        int x = icon[0];
        int y = icon[1];
        int size = icon[2];
        boolean hover = StorageOverlayPolicy.overValueIcon(cardX, cardY, cardWidth, mouseX, mouseY);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + size,
                y + size,
                hover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.FIELD,
                RotClientUiDraw.RADIUS_XS);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, RotClientTheme.VIOLET);
        RotClientUiDraw.text(
                graphics,
                font,
                "$",
                x + 2,
                y + 1,
                hover ? RotClientTheme.TEXT : RotClientTheme.WARNING,
                true);
        if (hover) {
            boolean active = selected != null && selected.equals(cached.page);
            double coins = pageValue(valuedItems(screen, cached, active));
            hoveredValueTip = StorageOverlayPolicy.pageValueLabel(coins);
            String compact = coins > 0.0D
                    ? StorageOverlayPolicy.formatCoins(Math.round(coins))
                    : "?";
            int labelX = x - 4 - RotClientFonts.width(font, compact);
            if (labelX > cardX + 8) {
                RotClientUiDraw.text(graphics, font, compact, labelX, y + 1, RotClientTheme.WARNING, true);
            }
        }
    }

    public static void applyValueTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hoveredValueTip == null || hoveredValueTip.isBlank() || graphics == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }
        graphics.setTooltipForNextFrame(
                client.font, Component.literal(hoveredValueTip), mouseX, mouseY);
    }

    public static void flushForShutdown() {
        if (!CACHE_DIRTY.get()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        boolean levelReady = client != null && client.level != null;
        if (StorageOverlayPolicy.shouldSkipUnreadyShutdownSave(levelReady, diskHadItems)) {
            return;
        }
        if (StorageOverlayPolicy.shouldSkipEmptyStorageSave(
                cacheMemoryHasItems(), diskHadItems, false, allPagesConfirmedEmpty())) {
            return;
        }

        try {
            AtomicFileWriter.writeAtomically(
                    cachePath(),
                    buildCacheJson());

            CACHE_DIRTY.set(false);
            diskHadItems = diskHadItems || cacheMemoryHasItems();
        } catch (Exception ignored) {
        }
    }

    private static List<ItemStack> valuedItems(
            AbstractContainerScreen<?> screen,
            CachedPage cached,
            boolean active) {
        if (!StorageOverlayPolicy.useLiveDisplay(active, liveTrustedPage != null && cached.page.equals(liveTrustedPage))
                || screen == null) {
            return cached.items;
        }
        int containerSlots = Math.max(0, screen.getMenu().slots.size() - 36);
        int start = StorageOverlayPolicy.contentSlotStart();
        int count = StorageOverlayPolicy.contentSlotCount(containerSlots);
        List<ItemStack> live = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = liveContentStack(screen, start + i);
            live.add(stack);
        }
        return live;
    }

    private static double pageValue(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return 0.0D;
        }
        SkyBlockMarketQuoteService.Quotes quotes = SkyBlockMarketQuoteService.current();
        BazaarPriceService.MarketPrices bazaar = RotClientClient.currentMarketPrices();
        Map<String, Double> units = new HashMap<>();
        List<StorageOverlayPolicy.MarketLine> lines = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String id = SkyBlockItemData.marketId(stack);
            if (id.isBlank()) {
                continue;
            }
            lines.add(new StorageOverlayPolicy.MarketLine(id, stack.getCount()));
            if (units.containsKey(id)) {
                continue;
            }
            PriceTooltipsPolicy.Quote quote = quotes.quote(id);
            double unit = StorageOverlayPolicy.marketUnitValue(
                    quote.lowestBin(), quote.bazaarBuy(), quote.bazaarSell());
            if (!(unit > 0.0D) && bazaar != null) {
                BazaarPriceService.ProductPrice product = bazaar.forProduct(id);
                if (product == null && id.startsWith("STARRED_")) {
                    product = bazaar.forProduct(id.substring("STARRED_".length()));
                }
                if (product != null) {
                    unit = product.instantSellPrice();
                }
            }
            if (unit > 0.0D && Double.isFinite(unit)) {
                units.put(id, unit);
            }
        }
        return StorageOverlayPolicy.instantSellTotal(lines, units);
    }

    private static void paintSearchSlot(
            GuiGraphicsExtractor graphics,
            QolSkyblockExtras extras,
            int x,
            int y,
            boolean match) {
        if (match) {
            paintSearchOutline(graphics, extras.storageOverlayHighlightColor, x, y);
        } else {
            graphics.fill(x, y, x + 18, y + 18, RotClientUiDraw.withAlpha(RotClientTheme.BACKDROP, 0xC8));
        }
    }

    private static void paintSearchOutline(GuiGraphicsExtractor graphics, int color, int x, int y) {
        int size = StorageOverlayPolicy.SLOT_SIZE;
        int purple = color;
        int red = RotClientTheme.VIOLET;
        graphics.fill(x, y, x + size, y + 2, purple);
        graphics.fill(x, y, x + 2, y + size, purple);
        graphics.fill(x, y + size - 2, x + size, y + size, red);
        graphics.fill(x + size - 2, y, x + size, y + size, red);
        int dim = (color & 0x00FFFFFF) | 0x33000000;
        graphics.fill(x + 2, y + 2, x + size - 2, y + 3, dim);
        long now = System.currentTimeMillis();
        int perimeter = StorageOverlayPolicy.searchGlowPerimeter(size);
        int head = StorageOverlayPolicy.searchGlowHead(now, perimeter);
        int tail = StorageOverlayPolicy.SEARCH_GLOW_TAIL;
        for (int i = 0; i < tail; i++) {
            int[] pixel = StorageOverlayPolicy.searchGlowPixel(x, y, size, head - i);
            int glow = RotClientUiDraw.withAlpha(color, Math.round(255.0F * (1.0F - i / (float) tail)));
            graphics.fill(pixel[0], pixel[1], pixel[0] + 1, pixel[1] + 1, glow);
            int[] inward = StorageOverlayPolicy.searchGlowInward(
                    x, y, size, pixel[0], pixel[1]);
            graphics.fill(inward[0], inward[1], inward[0] + 1, inward[1] + 1, glow);
        }
    }

    private static void drawSlotWell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, RotClientTheme.BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, RotClientTheme.FIELD);
    }

    private static void remapSlots(
            AbstractContainerScreen<?> screen,
            StorageOverlayPolicy.OverlayLayout layout,
            List<CachedPage> pages,
            StorageOverlayPolicy.Page selected,
            int[] rowOffsets,
            int left,
            int top) {
        captureOriginals(screen);
        QolSkyblockExtras extras = settings();
        int columns = StorageOverlayPolicy.layoutColumns(layout, extras.storageOverlayPadding);
        int padding = StorageOverlayPolicy.clampSpacing(extras.storageOverlayPadding);
        int cardWidth = StorageOverlayPolicy.pageWidth();
        int activeIndex = -1;
        CachedPage active = null;
        for (int i = 0; i < pages.size(); i++) {
            if (selected != null && selected.equals(pages.get(i).page)) {
                activeIndex = i;
                active = pages.get(i);
                break;
            }
        }
        int activeX = 0;
        int activeY = 0;
        if (activeIndex >= 0) {
            activeX = layout.innerX() + (activeIndex % columns) * (cardWidth + padding);
            activeY = layout.innerY() - PAGE_SCROLL.scrollPixels() + rowOffsets[activeIndex / columns];
        }
        int containerSlots = Math.max(0, screen.getMenu().slots.size() - 36);
        int contentStart = StorageOverlayPolicy.contentSlotStart();
        int contentCount = StorageOverlayPolicy.contentSlotCount(containerSlots);
        int playerStart = containerSlots;
        for (int i = 0; i < screen.getMenu().slots.size(); i++) {
            Slot slot = screen.getMenu().slots.get(i);
            if (i >= playerStart) {
                int invIndex = StorageOverlayPolicy.playerInventoryIndex(i - playerStart);
                int[] pos = StorageOverlayPolicy.playerSlotPosition(
                        layout.playerX(), layout.playerY(), invIndex);
                moveSlot(slot, pos[0] - left, pos[1] - top);
                continue;
            }
            boolean content = menuReady(screen) && selected != null && active != null
                    && i >= contentStart
                    && i < contentStart + contentCount;
            if (content) {
                int[] pos = StorageOverlayPolicy.contentSlotPosition(
                        activeX, activeY, i - contentStart);
                if (!StorageOverlayPolicy.slotFullyVisible(layout, pos[0], pos[1])) {
                    moveSlot(slot, HIDDEN, HIDDEN);
                } else {
                    moveSlot(slot, pos[0] - left, pos[1] - top);
                }
            } else {
                moveSlot(slot, HIDDEN, HIDDEN);
            }
        }
    }

    private static void moveSlot(Slot slot, int x, int y) {
        SlotPositionAccessor access = (SlotPositionAccessor) (Object) slot;
        access.rotclient$setX(x);
        access.rotclient$setY(y);
    }

    private static void captureOriginals(AbstractContainerScreen<?> screen) {
        if (boundScreen == screen && !ORIGINALS.isEmpty()) return;
        restoreSlots();
        boundScreen = screen;
        for (Slot slot : screen.getMenu().slots) {
            ORIGINALS.put(slot, new OriginalSlot(slot.x, slot.y));
        }
    }

    private static void restoreSlots() {
        for (Map.Entry<Slot, OriginalSlot> entry : ORIGINALS.entrySet()) {
            moveSlot(entry.getKey(), entry.getValue().x(), entry.getValue().y());
        }
        ORIGINALS.clear();
        boundScreen = null;
    }

    private static void observe(AbstractContainerScreen<?> screen, boolean overview, StorageOverlayPolicy.Page selected) {
        bindObservedScreen(screen);
        // A loading menu or a disk preview is not a server-confirmed page.
        if (!menuReady(screen)) return;
        List<Slot> slots = screen.getMenu().slots;
        int containerSlots = Math.max(0, slots.size() - 36);
        boolean selectorsChanged = false;
        if (overview) {
            liveTrustedPage = null;
            Map<StorageOverlayPolicy.Page, Integer> next = new LinkedHashMap<>();
            for (int i = 0; i < containerSlots; i++) {
                ItemStack stack = slots.get(i).getItem();
                StorageOverlayPolicy.Page page = StorageOverlayPolicy.pageFromOverviewSlotIndex(i)
                        .or(() -> StorageOverlayPolicy.pageFromTitle(stack.getHoverName().getString()))
                        .orElse(null);
                if (page == null) continue;
                if (stack.isEmpty() || StorageOverlayPolicy.isLockedSelectorItem(itemId(stack))) {
                    continue;
                }
                next.put(page, i);
            }
            sawOverview = true;
            if (!next.equals(SELECTOR_SLOTS)) {
                SELECTOR_SLOTS.clear();
                SELECTOR_SLOTS.putAll(next);
                selectorsChanged = true;
            }
            if (selectorsChanged) {
                scheduleSave();
            }
            maybeStartPrefetch(true);
            return;
        }
        if (selected == null || containerSlots <= 0) return;
        if (!prefetchActive) {
            rememberClickedPage(selected);
        }
        if (liveTrustedPage != null && !selected.equals(liveTrustedPage)) {
            liveTrustedPage = null;
        }
        rememberSelector(selected, StorageOverlayPolicy.COMMAND_SELECTOR_SLOT);
        int controlEnd = Math.min(StorageOverlayPolicy.contentSlotStart(), containerSlots);
        for (int i = 0; i < controlEnd; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty() || StorageOverlayPolicy.isLockedSelectorItem(itemId(stack))) {
                continue;
            }
            StorageOverlayPolicy.pageFromTitle(stack.getHoverName().getString())
                    .ifPresent(page -> rememberSelector(page, StorageOverlayPolicy.COMMAND_SELECTOR_SLOT));
        }
        int start = StorageOverlayPolicy.contentSlotStart();
        int count = StorageOverlayPolicy.contentSlotCount(containerSlots);
        List<ItemStack> copy = new ArrayList<>(count);
        boolean anyItem = false;
        boolean liveTrusted = menuReady(screen);
        CachedPage existing = CACHE.get(selected);
        for (int i = 0; i < count; i++) {
            int index = start + i;
            ItemStack live = index < containerSlots ? slots.get(index).getItem() : ItemStack.EMPTY;
            ItemStack previous = existing != null && i < existing.items.size()
                    ? existing.items.get(i)
                    : ItemStack.EMPTY;
            ItemStack chosen = live;

            if (StorageOverlayPolicy.incomingIsPlaceholder(
                    stackHasIdentity(previous), stackHasIdentity(live), live.isEmpty(), liveTrusted)) {
                chosen = previous;

            } else if (!previous.isEmpty() && !live.isEmpty()) {

                chosen = preserveCachedRenderComponents(
                        previous,
                        live);
            }
            if (!chosen.isEmpty()) {
                anyItem = true;
            }
            copy.add(chosen.isEmpty() ? ItemStack.EMPTY : chosen.copy());
        }
        liveTrustedPage = selected;
        if (anyItem) CONFIRMED_EMPTY_PAGES.remove(selected);
        else CONFIRMED_EMPTY_PAGES.add(selected);
        boolean existingHasItems = existing != null && existing.items.stream().anyMatch(stack -> !stack.isEmpty());
        if (StorageOverlayPolicy.shouldKeepExistingCache(existingHasItems, !anyItem, liveTrusted)) {
            return;
        }
        String fingerprint = cacheFingerprint(selected, copy);
        if (StorageOverlayPolicy.cacheFingerprintUnchanged(
                PAGE_FINGERPRINTS.get(selected), fingerprint)) {
            return;
        }
        PAGE_FINGERPRINTS.put(selected, fingerprint);
        CACHE.put(selected, new CachedPage(selected, List.copyOf(copy), Math.max(1, (count + 8) / 9)));
        scheduleSave();
    }

    private static void rememberSelector(StorageOverlayPolicy.Page page, int slot) {
        if (page == null) {
            return;
        }
        Integer previous = SELECTOR_SLOTS.get(page);
        if (previous != null && StorageOverlayPolicy.isPhysicalSelectorSlot(previous)) {
            return;
        }
        if (previous != null && previous == slot) {
            return;
        }
        SELECTOR_SLOTS.put(page, slot);
        scheduleSave();
    }

    private static String cacheFingerprint(StorageOverlayPolicy.Page page, List<ItemStack> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(page.kind().name()).append('#').append(page.number()).append('|');
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                builder.append(".;");
                continue;
            }
            builder.append(itemId(stack)).append('*').append(stack.getCount()).append('*')
                    .append(AutoClickerItemIdentity.skyBlockId(stack)).append('*')
                    .append(SkyBlockItemData.uuid(stack)).append('*')
                    .append(stack.getHoverName().getString()).append('*')
                    .append(profileTexture(stack)).append('*')
                    .append(stack.getComponents().hashCode()).append(';');
        }
        return builder.toString();
    }

    private static String itemId(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ItemStack liveContentStack(AbstractContainerScreen<?> screen, int containerIndex) {
        List<Slot> slots = screen.getMenu().slots;
        if (containerIndex < 0 || containerIndex >= Math.max(0, slots.size() - 36)) {
            return ItemStack.EMPTY;
        }
        return slots.get(containerIndex).getItem();
    }

    private static ItemStack stackAt(CachedPage cached, int slot) {
        return slot >= 0 && slot < cached.items.size() ? cached.items.get(slot) : ItemStack.EMPTY;
    }

    private static int cardHeight(CachedPage cached) {
        return StorageOverlayPolicy.pageHeight(
                StorageOverlayPolicy.slotRows(cached.rows, cached.items.isEmpty()));
    }

    private static int[] rowHeights(List<CachedPage> pages, int columns) {
        int rowCount = pages.isEmpty() ? 0 : (pages.size() + columns - 1) / columns;
        int[] heights = new int[rowCount];
        for (int i = 0; i < pages.size(); i++) {
            int row = i / columns;
            heights[row] = Math.max(heights[row], cardHeight(pages.get(i)));
        }
        return heights;
    }

    private static int[] rowOffsets(int[] rowHeights) {
        int[] offsets = new int[rowHeights.length];
        int padding = StorageOverlayPolicy.clampSpacing(settings().storageOverlayPadding);
        int cursor = 0;
        for (int row = 0; row < rowHeights.length; row++) {
            offsets[row] = cursor;
            cursor += rowHeights[row];
            if (row + 1 < rowHeights.length) cursor += padding;
        }
        return offsets;
    }

    private static int contentHeight(int[] rowHeights) {
        if (rowHeights.length == 0) return 0;
        int padding = StorageOverlayPolicy.clampSpacing(settings().storageOverlayPadding);
        int height = 0;
        for (int i = 0; i < rowHeights.length; i++) {
            height += rowHeights[i];
            if (i + 1 < rowHeights.length) height += padding;
        }
        return height;
    }

    private static boolean insideInner(StorageOverlayPolicy.OverlayLayout layout, int mouseX, int mouseY) {
        return mouseX >= layout.innerX() && mouseX < layout.innerX() + layout.innerWidth()
                && mouseY >= layout.innerY() && mouseY < layout.innerY() + layout.innerHeight();
    }

    private static void preserveCursor() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.mouseHandler == null) {
            return;
        }
        RotClientClient.noCursorReset().forcePreserveNext(
                client.mouseHandler.xpos(),
                client.mouseHandler.ypos(),
                System.currentTimeMillis());
    }

    private static Path cachePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(StorageOverlayPolicy.CACHE_FILE);
    }

    private static boolean cacheMemoryEmpty() {
        for (CachedPage page : CACHE.values()) {
            for (ItemStack stack : page.items()) {
                if (stack != null && !stack.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean cacheMemoryHasItems() {
        return !cacheMemoryEmpty();
    }

    private static void loadCache() {
        Path path = cachePath();
        boolean fileExists = Files.exists(path);
        Minecraft client = Minecraft.getInstance();
        boolean levelReady = client != null && client.level != null;
        if (!StorageOverlayPolicy.shouldReloadCache(
                cacheLoaded, cacheMemoryEmpty(), fileExists, codecReady, levelReady)) {
            return;
        }
        boolean upgrade = cacheLoaded && levelReady && !codecReady;
        cacheLoaded = true;
        codecReady = levelReady;
        if (!fileExists) {
            return;
        }

        try {
            JsonObject root = CACHE_GSON.fromJson(Files.readString(path), JsonObject.class);

            if (root == null || !root.has("pages") || !root.get("pages").isJsonArray()) {
                System.err.println("[RotClient] Storage cache file is missing a valid pages array");
                return;
            }
            boolean loadedItems = false;
            for (JsonElement element : root.getAsJsonArray("pages")) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }

                JsonObject pageJson = element.getAsJsonObject();

                String kindRaw = pageJson.has("kind")
                        ? pageJson.get("kind").getAsString()
                        : "";

                StorageOverlayPolicy.Kind kind = "BACKPACK".equalsIgnoreCase(kindRaw)
                        ? StorageOverlayPolicy.Kind.BACKPACK
                        : StorageOverlayPolicy.Kind.ENDER_CHEST;

                int number = pageJson.has("number")
                        ? pageJson.get("number").getAsInt()
                        : 0;

                if (number < 1 || number > 18) {
                    continue;
                }

                StorageOverlayPolicy.Page page =
                        new StorageOverlayPolicy.Page(kind, number);

                int rows = pageJson.has("rows")
                        ? pageJson.get("rows").getAsInt()
                        : 0;

                List<ItemStack> items = new ArrayList<>();

                if (pageJson.has("items") && pageJson.get("items").isJsonArray()) {
                    for (JsonElement itemElement : pageJson.getAsJsonArray("items")) {
                        ItemStack stack = stackFromCache(itemElement);
                        items.add(stack);
                        if (!stack.isEmpty()) {
                            loadedItems = true;
                        }
                    }
                }
                CachedPage loaded = new CachedPage(page, List.copyOf(items), Math.max(0, rows));
                CachedPage current = CACHE.get(page);
                boolean currentHasIdentity = current != null && pageHasIdentity(current);
                if (upgrade && !currentHasIdentity) {
                    CACHE.put(page, loaded);
                } else {
                    CACHE.putIfAbsent(page, loaded);
                }
                PAGE_FINGERPRINTS.putIfAbsent(page, cacheFingerprint(page, loaded.items()));
            }

            if (root.has("selectors") && root.get("selectors").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("selectors")) {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }

                    JsonObject selector = element.getAsJsonObject();

                    String kindRaw = selector.has("kind")
                            ? selector.get("kind").getAsString()
                            : "";

                    StorageOverlayPolicy.Kind kind = "BACKPACK".equalsIgnoreCase(kindRaw)
                            ? StorageOverlayPolicy.Kind.BACKPACK
                            : StorageOverlayPolicy.Kind.ENDER_CHEST;

                    int number = selector.has("number")
                            ? selector.get("number").getAsInt()
                            : 0;

                    int slot = selector.has("slot")
                            ? selector.get("slot").getAsInt()
                            : StorageOverlayPolicy.COMMAND_SELECTOR_SLOT;

                    if (number < 1 || number > 18) {
                        continue;
                    }

                    SELECTOR_SLOTS.putIfAbsent(
                            new StorageOverlayPolicy.Page(kind, number),
                            slot
                    );
                }
            } else if (!CACHE.isEmpty()) {
                for (StorageOverlayPolicy.Page page : CACHE.keySet()) {
                    SELECTOR_SLOTS.putIfAbsent(
                            page,
                            StorageOverlayPolicy.COMMAND_SELECTOR_SLOT
                    );
                }
            }
            sawOverview = sawOverview
                    || (root.has("sawOverview") && root.get("sawOverview").getAsBoolean());
            diskHadItems = diskHadItems || loadedItems;
            if (!upgrade) {
                directoryScanCompleted = StorageOverlayPolicy.inferDirectoryScanCompleted(
                        root.has("directoryScanCompleted"),
                        root.has("directoryScanCompleted")
                                && root.get("directoryScanCompleted").getAsBoolean(),
                        CACHE.keySet(),
                        SELECTOR_SLOTS.keySet());
                if (root.has("pendingRefresh") && root.get("pendingRefresh").isJsonArray()) {
                    for (JsonElement element : root.getAsJsonArray("pendingRefresh")) {
                        if (element == null || !element.isJsonObject()) {
                            continue;
                        }
                        JsonObject pending = element.getAsJsonObject();
                        String kindRaw = pending.has("kind") ? pending.get("kind").getAsString() : "";
                        StorageOverlayPolicy.Kind kind = "BACKPACK".equalsIgnoreCase(kindRaw)
                                ? StorageOverlayPolicy.Kind.BACKPACK
                                : StorageOverlayPolicy.Kind.ENDER_CHEST;
                        int number = pending.has("number") ? pending.get("number").getAsInt() : 0;
                        if (number < 1 || number > 18) {
                            continue;
                        }
                        PENDING_REFRESH.add(new StorageOverlayPolicy.Page(kind, number));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void scheduleSave() {
        CACHE_DIRTY.set(true);
        if (!CACHE_SAVE_SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        CACHE_IO.schedule(StorageOverlayRuntime::flushScheduledSave, CACHE_SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private static void flushScheduledSave() {
        CACHE_SAVE_SCHEDULED.set(false);
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(StorageOverlayRuntime::flushCacheToDisk);
        } else {
            flushCacheToDisk();
        }
    }

    private static void saveCacheNow() {
        CACHE_DIRTY.set(true);
        flushCacheToDisk();
    }

    private static void flushCacheToDisk() {
        if (!CACHE_DIRTY.get()) {
            return;
        }
        if (StorageOverlayPolicy.shouldSkipEmptyStorageSave(
                cacheMemoryHasItems(), diskHadItems, false, allPagesConfirmedEmpty())) {
            CACHE_DIRTY.set(false);
            return;
        }
        String json;
        try {
            json = buildCacheJson();
            CACHE_DIRTY.set(false);
            diskHadItems = diskHadItems || cacheMemoryHasItems();
        } catch (Exception ignored) {
            return;
        }
        CACHE_IO.execute(() -> {
            try {
                AtomicFileWriter.writeAtomically(cachePath(), json);
            } catch (Exception ignored) {
            }
        });
        if (CACHE_DIRTY.get() && CACHE_SAVE_SCHEDULED.compareAndSet(false, true)) {
            CACHE_IO.schedule(StorageOverlayRuntime::flushScheduledSave, CACHE_SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static boolean allPagesConfirmedEmpty() {
        return !CACHE.isEmpty() && CONFIRMED_EMPTY_PAGES.containsAll(CACHE.keySet());
    }

    private static String buildCacheJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 3);
        JsonArray pages = new JsonArray();
        for (CachedPage cached : CACHE.values()) {
            JsonObject pageJson = new JsonObject();
            pageJson.addProperty("kind", cached.page.kind().name());
            pageJson.addProperty("number", cached.page.number());
            pageJson.addProperty("rows", cached.rows);
            JsonArray items = new JsonArray();
            for (ItemStack stack : cached.items) {
                items.add(stackToCache(stack));
            }
            pageJson.add("items", items);
            pages.add(pageJson);
        }
        root.add("pages", pages);
        JsonArray selectors = new JsonArray();
        for (Map.Entry<StorageOverlayPolicy.Page, Integer> entry : SELECTOR_SLOTS.entrySet()) {
            JsonObject selector = new JsonObject();
            selector.addProperty("kind", entry.getKey().kind().name());
            selector.addProperty("number", entry.getKey().number());
            selector.addProperty("slot", entry.getValue());
            selectors.add(selector);
        }
        root.add("selectors", selectors);
        root.addProperty("sawOverview", sawOverview);
        root.addProperty("directoryScanCompleted", directoryScanCompleted);
        JsonArray pendingRefresh = new JsonArray();
        for (StorageOverlayPolicy.Page page : PENDING_REFRESH) {
            JsonObject pending = new JsonObject();
            pending.addProperty("kind", page.kind().name());
            pending.addProperty("number", page.number());
            pendingRefresh.add(pending);
        }
        root.add("pendingRefresh", pendingRefresh);
        return CACHE_GSON.toJson(root);
    }

    private static void saveCache() {
        scheduleSave();
    }

    static JsonObject stackToCache(ItemStack stack) {
        JsonObject json = new JsonObject();
        if (stack == null || stack.isEmpty()) {
            json.addProperty("id", "");
            json.addProperty("count", 0);
            json.addProperty("name", "");
            return json;
        }
        json.addProperty("id", String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        json.addProperty("count", stack.getCount());
        json.addProperty("name", stack.getHoverName().getString());
        String marketId = SkyBlockItemData.marketId(stack);
        if (!marketId.isBlank()) {
            json.addProperty("marketId", marketId);
        }
        JsonElement encoded = encodeStack(stack);
        if (encoded != null) {
            json.add("stack", encoded);
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null && !custom.isEmpty()) {
            json.addProperty("nbt", custom.copyTag().toString());
        }
        writeProfile(json, stack);
        return json;
    }

    private static void writeProfile(JsonObject json, ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile != null && profile.partialProfile() != null) {
            GameProfile game = profile.partialProfile();
            if (game.id() != null) {
                json.addProperty("profileId", game.id().toString());
            }
            if (game.name() != null && !game.name().isBlank()) {
                json.addProperty("profileName", game.name());
            }
            for (Property property : game.properties().get("textures")) {
                json.addProperty("texture", property.value());
                if (property.signature() != null && !property.signature().isBlank()) {
                    json.addProperty("textureSig", property.signature());
                }
                return;
            }
        }
        writeProfileFromCustomData(json, stack);
    }

    private static void writeProfileFromCustomData(JsonObject json, ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) {
            return;
        }
        String texture = skullTexture(custom.copyTag());
        if (!texture.isBlank()) {
            json.addProperty("texture", texture);
        }
    }

    private static String skullTexture(CompoundTag tag) {
        if (tag == null) {
            return "";
        }
        if (tag.contains("Value")) {
            String value = tag.getString("Value").orElse("");
            if (value.length() > 20) {
                return value;
            }
        }
        for (String key : tag.keySet()) {
            if (tag.getCompound(key).isPresent()) {
                String nested = skullTexture(tag.getCompound(key).orElse(null));
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }
        return "";
    }

    static ItemStack stackFromCache(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return ItemStack.EMPTY;
        }

        JsonObject json = element.getAsJsonObject();

        boolean jsonHasTexture = json.has("texture")
                && !json.get("texture").getAsString().isBlank();
        boolean jsonHasNbt = json.has("nbt") && !json.get("nbt").getAsString().isBlank();
        boolean jsonHasMarketId = json.has("marketId")
                && !json.get("marketId").getAsString().isBlank();
        String cachedName = json.has("name")
                ? json.get("name").getAsString()
                : "";
        if (json.has("stack")) {
            JsonElement originalStackJson = json.get("stack");
            ItemStack decoded = decodeStack(originalStackJson);

            if (decoded != null && !decoded.isEmpty()) {
                applyProfile(decoded, json);
                restoreMarketIdentity(decoded, json);
                if (!StorageOverlayPolicy.codecStackNeedsFallback(
                        false,
                        stackHasIdentity(decoded),
                        jsonHasTexture,
                        jsonHasNbt,
                        jsonHasMarketId)) {
                    return decoded;
                }
            }
        }

        String id = json.has("id")
                ? json.get("id").getAsString()
                : "";

        int count = json.has("count")
                ? json.get("count").getAsInt()
                : 0;

        String name = cachedName;

        if (id == null || id.isBlank() || count <= 0) {
            return ItemStack.EMPTY;
        }

        Identifier identifier = Identifier.tryParse(id);

        if (identifier == null) {
            return ItemStack.EMPTY;
        }

        var item = BuiltInRegistries.ITEM.getValue(identifier);

        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(
                item,
                Math.max(1, count));

        if (name != null && !name.isBlank()) {
            stack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(name));
        }

        if (json.has("nbt")) {
            try {
                CompoundTag tag =
                        TagParser.parseCompoundFully(
                                json.get("nbt").getAsString());

                if (tag != null && !tag.isEmpty()) {
                    stack.set(
                            DataComponents.CUSTOM_DATA,
                            CustomData.of(tag));
                }
            } catch (Exception ignored) {
            }
        }

        applyProfile(stack, json);
        restoreMarketIdentity(stack, json);

        return stack;
    }

    private static void restoreMarketIdentity(ItemStack stack, JsonObject json) {
        if (stack == null || stack.isEmpty() || json == null) {
            return;
        }
        if (!AutoClickerItemIdentity.skyBlockId(stack).isBlank()) {
            return;
        }
        if (json.has("nbt")) {
            try {
                CompoundTag tag = TagParser.parseCompoundFully(json.get("nbt").getAsString());
                if (tag != null && !tag.isEmpty()) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            } catch (Exception ignored) {
            }
        }
        if (!AutoClickerItemIdentity.skyBlockId(stack).isBlank()) {
            return;
        }
        String marketId = json.has("marketId") ? json.get("marketId").getAsString() : "";
        if (marketId == null || marketId.isBlank()) {
            return;
        }
        try {
            CompoundTag tag = TagParser.parseCompoundFully("{id:\"" + marketId.replace("\"", "") + "\"}");
            if (tag != null && !tag.isEmpty()) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        } catch (Exception ignored) {
        }
    }

    private static void applyProfile(ItemStack stack, JsonObject json) {
        String texture = json.has("texture") ? json.get("texture").getAsString() : "";
        if (texture == null || texture.isBlank()) {
            return;
        }
        String signature = json.has("textureSig") ? json.get("textureSig").getAsString() : "";
        String profileName = json.has("profileName") ? json.get("profileName").getAsString() : "";
        UUID profileId = null;
        if (json.has("profileId")) {
            try {
                profileId = UUID.fromString(json.get("profileId").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (profileId == null) {
            profileId = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
        }
        Property property = signature == null || signature.isBlank()
                ? new Property("textures", texture)
                : new Property("textures", texture, signature);
        GameProfile game = new GameProfile(
                profileId,
                profileName == null || profileName.isBlank() ? "SkyBlock" : profileName,
                new PropertyMap(ImmutableListMultimap.of("textures", property)));
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(game));
    }

    private static String profileTexture(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile() == null) {
            return "";
        }
        for (Property property : profile.partialProfile().properties().get("textures")) {
            String value = property.value();
            return value == null ? "" : value.substring(0, Math.min(48, value.length()));
        }
        return "";
    }

    private static boolean stackHasIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!AutoClickerItemIdentity.skyBlockId(stack).isBlank()) {
            return true;
        }
        if (!profileTexture(stack).isBlank()) {
            return true;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null && !custom.isEmpty();
    }

    private static ItemStack preserveCachedRenderComponents(
            ItemStack previous,
            ItemStack live) {

        if (previous == null || previous.isEmpty()
                || live == null || live.isEmpty()) {
            return live;
        }

        String previousSkyBlockId =
                AutoClickerItemIdentity.skyBlockId(previous);
        String liveSkyBlockId =
                AutoClickerItemIdentity.skyBlockId(live);

        if (previousSkyBlockId.isBlank()
                || liveSkyBlockId.isBlank()
                || !previousSkyBlockId.equals(liveSkyBlockId)) {
            return live;
        }

        ItemStack merged = live.copy();

        var previousItemModel =
                previous.get(DataComponents.ITEM_MODEL);

        if (merged.get(DataComponents.ITEM_MODEL) == null
                && previousItemModel != null) {
            merged.set(
                    DataComponents.ITEM_MODEL,
                    previousItemModel);
        }

        var previousLore =
                previous.get(DataComponents.LORE);

        if (merged.get(DataComponents.LORE) == null
                && previousLore != null) {
            merged.set(
                    DataComponents.LORE,
                    previousLore);
        }

        var previousTooltipStyle =
                previous.get(DataComponents.TOOLTIP_STYLE);

        if (merged.get(DataComponents.TOOLTIP_STYLE) == null
                && previousTooltipStyle != null) {
            merged.set(
                    DataComponents.TOOLTIP_STYLE,
                    previousTooltipStyle);
        }

        var previousTooltipDisplay =
                previous.get(DataComponents.TOOLTIP_DISPLAY);

        if (merged.get(DataComponents.TOOLTIP_DISPLAY) == null
                && previousTooltipDisplay != null) {
            merged.set(
                    DataComponents.TOOLTIP_DISPLAY,
                    previousTooltipDisplay);
        }

        return merged;
    }

    private static JsonElement encodeStack(ItemStack stack) {
        try {
            var registries = itemRegistries();
            if (registries == null || stack == null || stack.isEmpty()) {
                return null;
            }

            return ItemStack.CODEC.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, registries),
                    stack).result().orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ItemStack decodeStack(JsonElement element) {
        try {
            var registries = itemRegistries();
            if (registries == null || element == null) {
                return ItemStack.EMPTY;
            }

            return ItemStack.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, registries),
                    element).result().orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static net.minecraft.core.HolderLookup.Provider itemRegistries() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        if (client.level != null) {
            return client.level.registryAccess();
        }
        if (client.player != null) {
            return client.player.registryAccess();
        }
        if (client.getConnection() != null) {
            return client.getConnection().registryAccess();
        }
        return null;
    }

    private static QolSkyblockExtras settings() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private static boolean pageHasIdentity(CachedPage page) {
        if (page == null) {
            return false;
        }
        for (ItemStack stack : page.items()) {
            if (stackHasIdentity(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void maybeStartPrefetch(boolean overview) {
        if (!overview) {
            return;
        }
        boolean idle = PREFETCH.isEmpty() && !prefetchActive;
        int unlocked = SELECTOR_SLOTS.size();
        boolean refreshAll = userRequestedReload;
        if (directoryScanCompleted
                && !StorageOverlayPolicy.directoryScanComplete(
                        false, SELECTOR_SLOTS.keySet(), CACHE.keySet())) {
            directoryScanCompleted = false;
        }
        if (!directoryScanCompleted
                && StorageOverlayPolicy.directoryScanComplete(
                        false, SELECTOR_SLOTS.keySet(), CACHE.keySet())) {
            directoryScanCompleted = true;
            scheduleSave();
        }
        List<StorageOverlayPolicy.Page> targets = StorageOverlayPolicy.pagesToPrefetch(
                SELECTOR_SLOTS.keySet(),
                CACHE.keySet(),
                PENDING_REFRESH,
                directoryScanCompleted,
                refreshAll);
        if (!StorageOverlayPolicy.shouldStartPrefetch(
                true, idle, prefetchDoneThisOpen, refreshAll, unlocked, targets.size())) {
            if (refreshAll && idle) {
                userRequestedReload = false;
            }
            return;
        }
        if (refreshAll) {
            PENDING_REFRESH.clear();
        }
        startPrefetch(targets);
    }

    private static void startPrefetch(List<StorageOverlayPolicy.Page> pages) {
        PREFETCH.clear();
        if (pages != null) {
            PREFETCH.addAll(pages);
            PENDING_REFRESH.addAll(pages);
            scheduleSave();
        }
        prefetchActive = !PREFETCH.isEmpty();
        prefetchWaitingFor = null;
        prefetchTicksOnPage = 0;
        returningToOverview = false;
        userRequestedReload = false;
        if (!prefetchActive) {
            prefetchDoneThisOpen = true;
        }
    }

    private static void rememberClickedPage(StorageOverlayPolicy.Page page) {
        if (page == null) {
            return;
        }
        List<StorageOverlayPolicy.Page> next = StorageOverlayPolicy.withClickedPage(PENDING_REFRESH, page);
        if (next.equals(List.copyOf(PENDING_REFRESH))) {
            return;
        }
        PENDING_REFRESH.clear();
        PENDING_REFRESH.addAll(next);
        scheduleSave();
    }

    private static void markPrefetchPageSettled(StorageOverlayPolicy.Page page) {
        if (page == null) {
            return;
        }
        if (PENDING_REFRESH.remove(page)) {
            scheduleSave();
        }
    }

    private static void markDirectoryScanIfCovered() {
        if (directoryScanCompleted) {
            return;
        }
        if (!PENDING_REFRESH.isEmpty() || !StorageOverlayPolicy.directoryScanComplete(
                false, SELECTOR_SLOTS.keySet(), CACHE.keySet())) {
            return;
        }
        directoryScanCompleted = true;
        scheduleSave();
    }

    private static void cancelPrefetch() {
        PREFETCH.clear();
        prefetchActive = false;
        prefetchWaitingFor = null;
        prefetchTicksOnPage = 0;
        returningToOverview = false;
        userRequestedReload = false;
    }

    private static void advancePrefetch(Minecraft client) {
        if (!prefetchActive && PREFETCH.isEmpty()) {
            return;
        }
        if (userExiting || client == null || client.player == null || client.player.connection == null) {
            cancelPrefetch();
            return;
        }
        Screen screen = client.gui != null ? client.gui.screen() : null;
        if (!(screen instanceof AbstractContainerScreen<?> container) || !shouldReplaceVanilla(container)) {
            prefetchTicksOnPage++;
            if (prefetchTicksOnPage >= StorageOverlayPolicy.PREFETCH_TIMEOUT_TICKS) {
                sendNextPrefetch();
            }
            return;
        }
        String title = container.getTitle().getString();
        boolean overview = StorageOverlayPolicy.isOverviewTitle(title);
        StorageOverlayPolicy.Page selected = StorageOverlayPolicy.pageFromTitle(title).orElse(null);
        if (returningToOverview) {
            if (overview) {
                returningToOverview = false;
                prefetchActive = false;
                prefetchDoneThisOpen = true;
                prefetchWaitingFor = null;
                prefetchTicksOnPage = 0;
                markDirectoryScanIfCovered();
            } else {
                prefetchTicksOnPage++;
                if (prefetchTicksOnPage >= StorageOverlayPolicy.PREFETCH_TIMEOUT_TICKS) {
                    // A rejected command must not create an endless automatic retry loop.
                    cancelPrefetch();
                    prefetchDoneThisOpen = true;
                }
            }
            return;
        }
        if (prefetchWaitingFor != null) {
            boolean match = selected != null && selected.equals(prefetchWaitingFor);
            boolean snapshotReceived = match && menuReady(container);
            prefetchTicksOnPage++;
            if (StorageOverlayPolicy.prefetchPageSettled(match, snapshotReceived, prefetchTicksOnPage)) {
                // Timeout skips the page but leaves it pending for the next user reload.
                if (snapshotReceived) markPrefetchPageSettled(prefetchWaitingFor);
                prefetchWaitingFor = null;
                prefetchTicksOnPage = 0;
                sendNextPrefetch();
            }
            return;
        }
        if (overview || selected != null) {
            sendNextPrefetch();
        }
    }

    private static void sendNextPrefetch() {
        if (PREFETCH.isEmpty()) {
            prefetchWaitingFor = null;
            prefetchTicksOnPage = 0;
            markDirectoryScanIfCovered();
            returningToOverview = true;
            sendStorageCommand(StorageOverlayPolicy.OVERVIEW_COMMAND);
            return;
        }
        StorageOverlayPolicy.Page next = PREFETCH.poll();
        prefetchWaitingFor = next;
        prefetchTicksOnPage = 0;
        sendStorageCommand(StorageOverlayPolicy.pageCommand(next));
    }

    private static void sendStorageCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client == null
                || client.player == null
                || client.player.connection == null
                || command == null
                || command.isBlank()) {
            return;
        }
        preserveCursor();
        client.player.connection.sendCommand(command);
    }
}
