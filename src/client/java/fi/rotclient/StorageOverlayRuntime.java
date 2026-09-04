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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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
    private static StorageOverlayPolicy.OverlayLayout lastLayout;
    private static final RotClientScrollState PAGE_SCROLL = new RotClientScrollState();
    private static boolean cacheLoaded;
    private static boolean sawOverview;
    private static boolean userExiting;
    private static boolean searchFocused;
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
        if (!extras.storageOverlayEnabled || screen == null || graphics == null) {
            restoreSlots();
            lastLayout = null;
            searchFocused = false;
            HITS.clear();
            return;
        }
        loadCache();
        hoveredValueTip = null;
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
        List<CachedPage> pages = visiblePages(extras);
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
                layout.panelHeight());
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
                StorageOverlayPolicy.PLAYER_HEIGHT);
        RotClientUiDraw.text(graphics, font, "Storage",
                layout.innerX(), layout.panelY() + 7, RotClientTheme.TEXT, true);
        if (StorageOverlayPolicy.searching(extras.storageOverlaySearchQuery)) {
            String matches = searchMatches + (searchMatches == 1 ? " match" : " matches");
            int matchX = layout.innerX() + RotClientFonts.width(font, "Storage") + 8;
            if (matchX + RotClientFonts.width(font, matches) < layout.searchX() - 6) {
                RotClientUiDraw.text(graphics, font, matches, matchX, layout.panelY() + 7,
                        RotClientTheme.HUD_ACCENT, false);
            }
        }
        drawSearchField(graphics, font, layout, extras.storageOverlaySearchQuery);
        drawCloseButton(graphics, font, layout, mouseX, mouseY);

        int[] rowHeights = rowHeights(pages);
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
                            : "Open a Storage page to preview it here.",
                    layout.innerX(), layout.innerY() + 8, RotClientTheme.TEXT_MUTED, false);
        } else {
            int columns = StorageOverlayPolicy.clampColumns(extras.storageOverlayColumns);
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
    }

    public static Slot hoveredSlot(AbstractContainerScreen<?> screen, int left, int top, int mouseX, int mouseY) {
        if (screen == null || !shouldReplaceVanilla(screen)) return null;
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
            preserveCursor();
            Integer slot = SELECTOR_SLOTS.get(hit.page);
            Minecraft client = Minecraft.getInstance();
            if (overview
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
        if (!PAGE_SCROLL.canScroll()) {
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

    public static void resetWorld() {
        HITS.clear();
        restoreSlots();
        lastLayout = null;
        searchFocused = false;
        userExiting = false;
        if (!settings().storageOverlayRetainScroll) {
            PAGE_SCROLL.reset();
        }
        loadCache();
    }

    /** Clears only locally observed page contents and selectors. */
    public static void clearObservedPages() {
        HITS.clear();
        SELECTOR_SLOTS.clear();
        CACHE.clear();
        PAGE_FINGERPRINTS.clear();
        sawOverview = false;
        PAGE_SCROLL.reset();
        saveCacheNow();
    }

    private static List<CachedPage> visiblePages(QolSkyblockExtras extras) {
        Map<StorageOverlayPolicy.Page, CachedPage> visiblePages = new LinkedHashMap<>(CACHE);
        for (StorageOverlayPolicy.Page page : SELECTOR_SLOTS.keySet()) {
            visiblePages.putIfAbsent(page, new CachedPage(page, List.of(), 0));
        }
        boolean filterSearch = extras.storageOverlayFilterSearch
                && !extras.storageOverlaySearchQuery.isBlank();
        return visiblePages.values().stream()
                .filter(page -> !filterSearch || page.items().stream()
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
                active ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT);
        graphics.fill(x, y, x + cardWidth, y + StorageOverlayPolicy.CARD_HEADER_HEIGHT,
                RotClientTheme.HUD_HEADER);
        int border = active && extras.storageOverlayOutlineActive
                ? RotClientTheme.BORDER_BRIGHT
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
            ItemStack stack = active
                    ? liveContentStack(screen, contentStart + slot)
                    : stackAt(cached, slot);
            boolean searchMatch = !stack.isEmpty() && StorageOverlayPolicy.matchesSearch(
                    stack.getHoverName().getString(), extras.storageOverlaySearchQuery);
            if (!stack.isEmpty()) {
                graphics.item(stack, pos[0], pos[1]);
                graphics.itemDecorations(font, stack, pos[0], pos[1]);
            }
            if (searching) {
                paintSearchSlot(graphics, extras, pos[0], pos[1], searchMatch);
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
        if (CACHE.isEmpty() && SELECTOR_SLOTS.isEmpty() && !CACHE_DIRTY.get()) {
            return;
        }
        try {
            AtomicFileWriter.writeAtomically(cachePath(), buildCacheJson());
            CACHE_DIRTY.set(false);
        } catch (Exception ignored) {
        }
    }

    private static List<ItemStack> valuedItems(
            AbstractContainerScreen<?> screen,
            CachedPage cached,
            boolean active) {
        if (!active || screen == null) {
            return cached.items;
        }
        int containerSlots = Math.max(0, screen.getMenu().slots.size() - 36);
        int start = StorageOverlayPolicy.contentSlotStart();
        int count = StorageOverlayPolicy.contentSlotCount(containerSlots);
        List<ItemStack> live = new ArrayList<>(count);
        boolean any = false;
        for (int i = 0; i < count; i++) {
            ItemStack stack = liveContentStack(screen, start + i);
            if (!stack.isEmpty()) {
                any = true;
            }
            live.add(stack);
        }
        return any ? live : cached.items;
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
        int purple = 0xE8B14CFF;
        int red = 0xE8FF2D55;
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
            int glow = StorageOverlayPolicy.searchGlowColor(i, tail);
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
        int columns = StorageOverlayPolicy.clampColumns(extras.storageOverlayColumns);
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
            boolean content = selected != null && active != null
                    && i >= contentStart
                    && i < contentStart + contentCount;
            if (content) {
                int[] pos = StorageOverlayPolicy.contentSlotPosition(
                        activeX, activeY, i - contentStart);
                if (pos[1] + 16 < layout.innerY() || pos[1] > layout.innerY() + layout.innerHeight()) {
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
        List<Slot> slots = screen.getMenu().slots;
        int containerSlots = Math.max(0, slots.size() - 36);
        boolean selectorsChanged = false;
        if (overview) {
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
            return;
        }
        if (selected == null || containerSlots <= 0) return;
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
        CachedPage existing = CACHE.get(selected);
        for (int i = 0; i < count; i++) {
            int index = start + i;
            ItemStack live = index < containerSlots ? slots.get(index).getItem() : ItemStack.EMPTY;
            ItemStack previous = existing != null && i < existing.items.size()
                    ? existing.items.get(i)
                    : ItemStack.EMPTY;
            ItemStack chosen = live;
            if (StorageOverlayPolicy.incomingIsPlaceholder(
                    stackHasIdentity(previous), stackHasIdentity(live), live.isEmpty())) {
                chosen = previous;
            }
            if (!chosen.isEmpty()) {
                anyItem = true;
            }
            copy.add(chosen.isEmpty() ? ItemStack.EMPTY : chosen.copy());
        }
        boolean existingHasItems = existing != null && existing.items.stream().anyMatch(stack -> !stack.isEmpty());
        if (StorageOverlayPolicy.shouldKeepExistingCache(existingHasItems, !anyItem)) {
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
                    .append(profileTexture(stack)).append(';');
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

    private static int[] rowHeights(List<CachedPage> pages) {
        int columns = StorageOverlayPolicy.clampColumns(settings().storageOverlayColumns);
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
        return FabricLoader.getInstance().getConfigDir().resolve("rotclient-storage-cache.json");
    }

    private static void loadCache() {
        if (cacheLoaded) {
            return;
        }
        cacheLoaded = true;
        Path path = cachePath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = CACHE_GSON.fromJson(Files.readString(path), JsonObject.class);
            if (root == null || !root.has("pages") || !root.get("pages").isJsonArray()) {
                return;
            }
            for (JsonElement element : root.getAsJsonArray("pages")) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject pageJson = element.getAsJsonObject();
                String kindRaw = pageJson.has("kind") ? pageJson.get("kind").getAsString() : "";
                StorageOverlayPolicy.Kind kind = "BACKPACK".equalsIgnoreCase(kindRaw)
                        ? StorageOverlayPolicy.Kind.BACKPACK
                        : StorageOverlayPolicy.Kind.ENDER_CHEST;
                int number = pageJson.has("number") ? pageJson.get("number").getAsInt() : 0;
                if (number < 1 || number > 18) {
                    continue;
                }
                StorageOverlayPolicy.Page page = new StorageOverlayPolicy.Page(kind, number);
                int rows = pageJson.has("rows") ? pageJson.get("rows").getAsInt() : 0;
                List<ItemStack> items = new ArrayList<>();
                if (pageJson.has("items") && pageJson.get("items").isJsonArray()) {
                    for (JsonElement itemElement : pageJson.getAsJsonArray("items")) {
                        items.add(stackFromCache(itemElement));
                    }
                }
                CACHE.putIfAbsent(page, new CachedPage(page, List.copyOf(items), Math.max(0, rows)));
            }
            if (root.has("selectors") && root.get("selectors").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("selectors")) {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject selector = element.getAsJsonObject();
                    String kindRaw = selector.has("kind") ? selector.get("kind").getAsString() : "";
                    StorageOverlayPolicy.Kind kind = "BACKPACK".equalsIgnoreCase(kindRaw)
                            ? StorageOverlayPolicy.Kind.BACKPACK
                            : StorageOverlayPolicy.Kind.ENDER_CHEST;
                    int number = selector.has("number") ? selector.get("number").getAsInt() : 0;
                    int slot = selector.has("slot")
                            ? selector.get("slot").getAsInt()
                            : StorageOverlayPolicy.COMMAND_SELECTOR_SLOT;
                    if (number < 1 || number > 18) {
                        continue;
                    }
                    SELECTOR_SLOTS.putIfAbsent(
                            new StorageOverlayPolicy.Page(kind, number), slot);
                }
            } else if (!CACHE.isEmpty()) {
                for (StorageOverlayPolicy.Page page : CACHE.keySet()) {
                    SELECTOR_SLOTS.putIfAbsent(page, StorageOverlayPolicy.COMMAND_SELECTOR_SLOT);
                }
            }
            sawOverview = root.has("sawOverview") && root.get("sawOverview").getAsBoolean();
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
        String json;
        try {
            json = buildCacheJson();
            CACHE_DIRTY.set(false);
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

    private static String buildCacheJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
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
        return CACHE_GSON.toJson(root);
    }

    private static void saveCache() {
        scheduleSave();
    }

    private static JsonObject stackToCache(ItemStack stack) {
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

    private static ItemStack stackFromCache(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return ItemStack.EMPTY;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("stack")) {
            ItemStack decoded = decodeStack(json.get("stack"));
            if (decoded != null && !decoded.isEmpty()) {
                restoreMarketIdentity(decoded, json);
                return decoded;
            }
        }
        String id = json.has("id") ? json.get("id").getAsString() : "";
        int count = json.has("count") ? json.get("count").getAsInt() : 0;
        String name = json.has("name") ? json.get("name").getAsString() : "";
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
        ItemStack stack = new ItemStack(item, Math.max(1, count));
        if (name != null && !name.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
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

    private static JsonElement encodeStack(ItemStack stack) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.level == null || stack == null || stack.isEmpty()) {
                return null;
            }
            return ItemStack.CODEC.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, client.level.registryAccess()),
                    stack).result().orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ItemStack decodeStack(JsonElement element) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.level == null || element == null) {
                return ItemStack.EMPTY;
            }
            return ItemStack.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, client.level.registryAccess()),
                    element).result().orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static QolSkyblockExtras settings() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
