package fi.rotclient;

import fi.rotclient.mixin.SlotPositionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static int scroll;
    private static int lastContentHeight;

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
            HITS.clear();
            return;
        }
        String title = screen.getTitle().getString();
        StorageOverlayPolicy.Page selected = StorageOverlayPolicy.pageFromTitle(title).orElse(null);
        boolean overview = StorageOverlayPolicy.isOverviewTitle(title);
        if (!overview && selected == null) {
            restoreSlots();
            lastLayout = null;
            HITS.clear();
            return;
        }
        if (!overview && !extras.storageOverlayAlwaysOpen) {
            restoreSlots();
            lastLayout = null;
            HITS.clear();
            return;
        }
        observe(screen, overview, selected);
        StorageOverlayPolicy.OverlayLayout layout = StorageOverlayPolicy.layout(
                screen.width,
                screen.height,
                extras.storageOverlayColumns,
                extras.storageOverlayPadding,
                extras.storageOverlayHeight);
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

        graphics.fill(layout.panelX(), layout.panelY(),
                layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(),
                extras.storageOverlayPanelColor);
        graphics.fill(layout.panelX(), layout.panelY(),
                layout.panelX() + layout.panelWidth(), layout.panelY() + 2, extras.storageOverlayOutlineColor);
        graphics.fill(layout.playerX(), layout.playerY(),
                layout.playerX() + StorageOverlayPolicy.PLAYER_WIDTH,
                layout.playerY() + StorageOverlayPolicy.PLAYER_HEIGHT, extras.storageOverlayPlayerColor);
        graphics.fill(layout.playerX(), layout.playerY(),
                layout.playerX() + StorageOverlayPolicy.PLAYER_WIDTH, layout.playerY() + 2, 0xFF31536A);
        RotClientUiDraw.text(graphics, font, StorageOverlayPolicy.searchSummary(
                        extras.storageOverlaySearchQuery, CACHE.size(), searchMatches),
                layout.innerX(), layout.panelY() + 6, 0xFFFFFFFF, true);

        int[] rowHeights = rowHeights(pages);
        int[] rowOffsets = rowOffsets(rowHeights);
        lastContentHeight = contentHeight(rowHeights);
        scroll = StorageOverlayPolicy.clampScrollOffset(
                scroll, lastContentHeight, layout.innerHeight());
        remapSlots(screen, layout, pages, selected, rowOffsets, left, top);

        graphics.enableScissor(layout.innerX(), layout.innerY(),
                layout.innerX() + layout.innerWidth(), layout.innerY() + layout.innerHeight());
        if (pages.isEmpty()) {
            RotClientUiDraw.text(graphics, font, extras.storageOverlayFilterSearch
                            ? "No cached page matches this search."
                            : "Open a Storage page to preview it here.",
                    layout.innerX(), layout.innerY() + 8, 0xFF91A7B8, false);
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
        drawScrollBar(graphics, layout);
        drawPlayerInventory(screen, graphics, font, layout);
    }

    public static Slot hoveredSlot(AbstractContainerScreen<?> screen, int left, int top, int mouseX, int mouseY) {
        if (screen == null || !shouldReplaceVanilla(screen)) return null;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.x <= HIDDEN / 2 || slot.y <= HIDDEN / 2) continue;
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    public static boolean shouldKeepCursorAcrossScreens(Screen previous, Screen next) {
        if (!settings().storageOverlayEnabled || next == null) {
            return false;
        }
        if (lastLayout != null) {
            return true;
        }
        if (previous instanceof AbstractContainerScreen<?> prevScreen
                && shouldReplaceVanilla(prevScreen)) {
            return true;
        }
        return next instanceof AbstractContainerScreen<?> nextScreen
                && shouldReplaceVanilla(nextScreen);
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
        String title = screen.getTitle().getString();
        StorageOverlayPolicy.Page selected = StorageOverlayPolicy.pageFromTitle(title).orElse(null);
        boolean overview = StorageOverlayPolicy.isOverviewTitle(title);
        boolean overLiveSlot = hoveredSlot(screen, left, top, mouseX, mouseY) != null;
        for (Hit hit : HITS) {
            if (!StorageOverlayPolicy.shouldNavigatePage(
                    shiftDown,
                    overLiveSlot,
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
            Integer slot = SELECTOR_SLOTS.get(hit.page);
            Minecraft client = Minecraft.getInstance();
            if (overview && slot != null && client.player != null && client.gameMode != null) {
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
        if (!extras.storageOverlayEnabled || HITS.isEmpty() || (overItem && extras.storageOverlayBlockItemScroll)) {
            return false;
        }
        int direction = verticalAmount > 0 ? -1 : verticalAmount < 0 ? 1 : 0;
        if (extras.storageOverlayInvertScroll) direction *= -1;
        scroll = Math.max(0, scroll + direction * StorageOverlayPolicy.clampScrollSpeed(extras.storageOverlayScrollSpeed));
        return direction != 0;
    }

    public static void resetWorld() {
        HITS.clear();
        SELECTOR_SLOTS.clear();
        CACHE.clear();
        restoreSlots();
        lastLayout = null;
        if (!settings().storageOverlayRetainScroll) scroll = 0;
    }

    /** Clears only locally observed page contents and selectors. */
    public static void clearObservedPages() {
        HITS.clear();
        SELECTOR_SLOTS.clear();
        CACHE.clear();
        scroll = 0;
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
                active ? extras.storageOverlayCardActiveColor : extras.storageOverlayCardColor);
        int border = active && extras.storageOverlayOutlineActive
                ? extras.storageOverlayOutlineColor
                : pageSearchMatch ? extras.storageOverlayHighlightColor : 0xFF31536A;
        graphics.fill(x, y, x + cardWidth, y + 1, border);
        graphics.fill(x, y + cardHeight - 1, x + cardWidth, y + cardHeight, border);
        RotClientUiDraw.text(graphics, font, cached.page.label(), x + 5, y + 4,
                active ? 0xFF54D7C7 : 0xFFD5E3EC, true);
        if (cached.items.isEmpty() && !active) {
            RotClientUiDraw.text(graphics, font, "Open to preview", x + 5, y + 16, 0xFF91A7B8, false);
            return;
        }
        int containerSlots = Math.max(0, screen.getMenu().slots.size() - 36);
        int contentCount = StorageOverlayPolicy.contentSlotCount(containerSlots);
        int slots = active ? Math.max(1, contentCount) : cached.items.size();
        int contentStart = StorageOverlayPolicy.contentSlotStart();
        for (int slot = 0; slot < slots; slot++) {
            int[] pos = StorageOverlayPolicy.contentSlotPosition(x, y, slot);
            drawSlotWell(graphics, pos[0], pos[1]);
            ItemStack stack = active
                    ? liveStack(screen, contentStart + slot)
                    : stackAt(cached, slot);
            boolean searchMatch = !stack.isEmpty() && StorageOverlayPolicy.matchesSearch(
                    stack.getHoverName().getString(), extras.storageOverlaySearchQuery);
            if (!extras.storageOverlaySearchQuery.isBlank()
                    && extras.storageOverlayHighlightSearch
                    && searchMatch) {
                graphics.fill(pos[0] - 1, pos[1] - 1, pos[0] + 17, pos[1] + 17,
                        extras.storageOverlayHighlightColor);
            }
            if (stack.isEmpty()) continue;
            graphics.item(stack, pos[0], pos[1]);
            graphics.itemDecorations(font, stack, pos[0], pos[1]);
            if (!active
                    && mouseX >= pos[0] && mouseX < pos[0] + 16 && mouseY >= pos[1] && mouseY < pos[1] + 16
                    && extras.storageOverlayInactiveTooltips
                    && insideInner(layout, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    private static ItemStack liveStack(AbstractContainerScreen<?> screen, int index) {
        List<Slot> slots = screen.getMenu().slots;
        return index >= 0 && index < slots.size() ? slots.get(index).getItem() : ItemStack.EMPTY;
    }

    private static void drawPlayerInventory(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            Font font,
            StorageOverlayPolicy.OverlayLayout layout) {
        RotClientUiDraw.text(graphics, font, "Inventory", layout.playerX() + 12, layout.playerY() + 3, 0xFF91A7B8, false);
        List<Slot> slots = screen.getMenu().slots;
        int playerStart = Math.max(0, slots.size() - 36);
        for (int i = playerStart; i < slots.size(); i++) {
            int invIndex = StorageOverlayPolicy.playerInventoryIndex(i - playerStart);
            int[] pos = StorageOverlayPolicy.playerSlotPosition(layout.playerX(), layout.playerY(), invIndex);
            drawSlotWell(graphics, pos[0], pos[1]);
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            graphics.item(stack, pos[0], pos[1]);
            graphics.itemDecorations(font, stack, pos[0], pos[1]);
        }
    }

    private static void drawScrollBar(GuiGraphicsExtractor graphics, StorageOverlayPolicy.OverlayLayout layout) {
        graphics.fill(layout.scrollBarX(), layout.scrollBarY(),
                layout.scrollBarX() + StorageOverlayPolicy.SCROLL_BAR_WIDTH,
                layout.scrollBarY() + layout.scrollBarHeight(), 0xFF0A1118);
        int track = Math.max(1, layout.scrollBarHeight() - StorageOverlayPolicy.SCROLL_KNOB_HEIGHT);
        int maxScroll = Math.max(0, lastContentHeight - layout.innerHeight());
        int knobY = layout.scrollBarY() + (maxScroll <= 0 ? 0 : track * scroll / maxScroll);
        graphics.fill(layout.scrollBarX(), knobY,
                layout.scrollBarX() + StorageOverlayPolicy.SCROLL_BAR_WIDTH,
                knobY + StorageOverlayPolicy.SCROLL_KNOB_HEIGHT, 0xFF54D7C7);
    }

    private static void drawSlotWell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF2A2A2A);
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
            activeY = layout.innerY() - scroll + rowOffsets[activeIndex / columns];
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
        if (overview) {
            for (int i = 0; i < containerSlots; i++) {
                ItemStack stack = slots.get(i).getItem();
                StorageOverlayPolicy.Page page = StorageOverlayPolicy.pageFromOverviewSlotIndex(i)
                        .or(() -> StorageOverlayPolicy.pageFromTitle(stack.getHoverName().getString()))
                        .orElse(null);
                if (page == null) continue;
                if (stack.isEmpty() || StorageOverlayPolicy.isLockedSelectorItem(itemId(stack))) {
                    SELECTOR_SLOTS.remove(page);
                    continue;
                }
                SELECTOR_SLOTS.put(page, i);
            }
            return;
        }
        if (selected == null || containerSlots <= 0) return;
        int start = StorageOverlayPolicy.contentSlotStart();
        int count = StorageOverlayPolicy.contentSlotCount(containerSlots);
        List<ItemStack> copy = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = start + i;
            copy.add(index < containerSlots ? slots.get(index).getItem().copy() : ItemStack.EMPTY);
        }
        CACHE.put(selected, new CachedPage(selected, List.copyOf(copy), Math.max(1, (count + 8) / 9)));
    }

    private static String itemId(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ItemStack stackAt(CachedPage cached, int slot) {
        return slot >= 0 && slot < cached.items.size() ? cached.items.get(slot) : ItemStack.EMPTY;
    }

    private static int cardHeight(CachedPage cached) {
        if (cached.items.isEmpty() && cached.rows <= 0) return StorageOverlayPolicy.emptyPageHeight();
        return StorageOverlayPolicy.pageHeight(Math.max(1, cached.rows));
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
        int cursor = 18;
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
        int height = 18;
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

    private static QolSkyblockExtras settings() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
