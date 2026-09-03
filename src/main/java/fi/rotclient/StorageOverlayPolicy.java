package fi.rotclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Storage title, page and compact overview layout rules. */
public final class StorageOverlayPolicy {
    public enum Kind { ENDER_CHEST, BACKPACK }

    public record Page(Kind kind, int number) {
        public Page {
            if (kind == null || number < 1 || number > 18) {
                throw new IllegalArgumentException("Invalid storage page");
            }
        }

        public String label() {
            return (kind == Kind.ENDER_CHEST ? "Ender Chest #" : "Backpack #") + number;
        }
    }

    public record OverlayLayout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int innerX,
            int innerY,
            int innerWidth,
            int innerHeight,
            int playerX,
            int playerY,
            int scrollBarX,
            int scrollBarY,
            int scrollBarHeight,
            int searchX,
            int searchY,
            int searchWidth,
            int searchHeight,
            int closeX,
            int closeY,
            int closeSize) {}

    public static final int SLOT_SIZE = 18;
    public static final int CARD_HEADER_HEIGHT = 18;
    public static final int HEADER_SLOT_GAP = 4;
    public static final int CARD_BOTTOM_PAD = 4;
    public static final int CONTENT_TOP_INSET = 8;
    public static final int CONTROL_ROW_SLOTS = 9;
    public static final int ENDER_CHEST_PAGES = 9;
    public static final int BACKPACK_PAGES = 18;
    /** Selector index when the page is known but Storage overview was never opened. */
    public static final int COMMAND_SELECTOR_SLOT = -1;
    public static final int SEARCH_GLOW_PURPLE = 0xFFB14CFF;
    public static final int SEARCH_GLOW_RED = 0xFFFF2D55;
    public static final int SEARCH_GLOW_PERIOD_MS = 1100;
    public static final int SEARCH_GLOW_TAIL = 28;
    public static final int CLOSE_BUTTON_SIZE = 14;
    public static final int VALUE_ICON_SIZE = 11;
    public static final int PLAYER_WIDTH = 184;
    public static final int PLAYER_HEIGHT = 98;
    public static final int PLAYER_GAP = 10;
    public static final int HEADER_HEIGHT = 22;
    public static final int SEARCH_WIDTH = 132;
    public static final int SEARCH_HEIGHT = 14;
    public static final int MAX_SEARCH_LENGTH = 48;
    public static final int EMPTY_PAGE_ROWS = 5;
    public static final int INNER_PADDING = 10;
    public static final int SCROLL_BAR_WIDTH = 8;
    public static final int SCROLL_BAR_HIT_WIDTH = 12;
    public static final int SCROLL_KNOB_HEIGHT = 16;
    public static final int HOTBAR_X = 12;
    public static final int HOTBAR_Y = 74;
    public static final int MAIN_INVENTORY_Y = 16;

    private static final Set<String> LOCKED_SELECTOR_ITEMS = Set.of(
            "red_stained_glass_pane",
            "brown_stained_glass_pane",
            "gray_dye");

    // Hypixel-style menus have used "#1", "Page 1" and "(1/9)" page
    // suffixes. Accept all three rather than silently declining a valid menu.
    private static final Pattern PAGE = Pattern.compile(
            "(?i)^(?:large )?(ender chest|backpack)(?:\\s+page)?\\s*(?:#|\\(|\\[)?\\s*(\\d{1,2})(?:\\s*(?:/\\s*\\d+|\\)|\\]))?.*$");
    private static final Pattern HYPIXEL_ENDER = Pattern.compile(
            "(?i)^ender chest(?:\\s+[✦✧*])?\\s*\\((\\d{1,2})/\\d+\\)$");
    private static final Pattern HYPIXEL_BACKPACK = Pattern.compile(
            "(?i)^.+backpack(?:\\s+[✦✧*])?\\s*\\(slot\\s*#(\\d{1,2})\\)$");

    private StorageOverlayPolicy() {}

    public static boolean isOverviewTitle(String title) {
        String normalized = normalize(title);
        return normalized.equals("storage") || normalized.equals("storage overview");
    }

    public static Optional<Page> pageFromTitle(String title) {
        String normalized = normalize(title);
        Matcher hypixelEnder = HYPIXEL_ENDER.matcher(normalized);
        if (hypixelEnder.matches()) {
            return page(Kind.ENDER_CHEST, hypixelEnder.group(1));
        }
        Matcher hypixelBackpack = HYPIXEL_BACKPACK.matcher(normalized);
        if (hypixelBackpack.matches()) {
            return page(Kind.BACKPACK, hypixelBackpack.group(1));
        }
        // The first Ender Chest is commonly presented without a page suffix.
        // Treat that real menu title as page one rather than declining to
        // observe it and leaving the overlay empty.
        if (normalized.equals("ender chest") || normalized.equals("large ender chest")) {
            return Optional.of(new Page(Kind.ENDER_CHEST, 1));
        }
        if (normalized.equals("backpack")
                || normalized.equals("small backpack")
                || normalized.equals("medium backpack")
                || normalized.equals("large backpack")
                || normalized.equals("greater backpack")
                || normalized.equals("jumbo backpack")) {
            return Optional.of(new Page(Kind.BACKPACK, 1));
        }
        Matcher matcher = PAGE.matcher(normalized);
        if (!matcher.matches()) return Optional.empty();
        Kind kind = matcher.group(1).toLowerCase(Locale.ROOT).startsWith("ender")
                ? Kind.ENDER_CHEST : Kind.BACKPACK;
        return page(kind, matcher.group(2));
    }

    /**
     * Hypixel Storage overview places Ender Chests on slots 9-17 and
     * Backpacks on slots 27-44. Index is more reliable than item names.
     */
    public static Optional<Page> pageFromOverviewSlotIndex(int slot) {
        if (slot >= 9 && slot < 18) {
            return Optional.of(new Page(Kind.ENDER_CHEST, slot - 8));
        }
        if (slot >= 27 && slot < 45) {
            return Optional.of(new Page(Kind.BACKPACK, slot - 26));
        }
        return Optional.empty();
    }

    public static boolean isLockedSelectorItem(String itemId) {
        String path = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        return LOCKED_SELECTOR_ITEMS.contains(path);
    }

    /** First chest row is back/page controls, not stored items. */
    public static int contentSlotStart() {
        return CONTROL_ROW_SLOTS;
    }

    public static int contentSlotCount(int containerSlots) {
        int rows = Math.max(0, Math.max(0, containerSlots) / CONTROL_ROW_SLOTS);
        return Math.max(0, rows * CONTROL_ROW_SLOTS - CONTROL_ROW_SLOTS);
    }

    public static String pageCommand(Page page) {
        if (page == null) return "";
        return page.kind() == Kind.ENDER_CHEST
                ? "enderchest " + page.number()
                : "backpack " + page.number();
    }

    public static boolean isPhysicalSelectorSlot(int slot) {
        return slot >= 0;
    }

    public static List<Page> defaultDirectory() {
        List<Page> pages = new ArrayList<>(ENDER_CHEST_PAGES + BACKPACK_PAGES);
        for (int i = 1; i <= ENDER_CHEST_PAGES; i++) {
            pages.add(new Page(Kind.ENDER_CHEST, i));
        }
        for (int i = 1; i <= BACKPACK_PAGES; i++) {
            pages.add(new Page(Kind.BACKPACK, i));
        }
        return List.copyOf(pages);
    }

    public static int clampColumns(int columns) { return Math.max(1, Math.min(5, columns)); }
    public static int clampHeight(int height) { return Math.max(180, Math.min(720, height)); }
    public static int clampScrollSpeed(int speed) { return Math.max(1, Math.min(40, speed)); }
    public static int clampSpacing(int value) { return Math.max(0, Math.min(40, value)); }

    /** Keeps a local overlay scroll offset inside the received page content. */
    public static int clampScrollOffset(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, Math.max(0, contentHeight) - Math.max(0, viewportHeight));
        return Math.max(0, Math.min(maximum, requested));
    }

    public static int pageWidth() { return SLOT_SIZE * 9 + 4; }

    /** Header strip + gap + slot rows + bottom pad. Must match {@link #contentSlotPosition}. */
    public static int pageHeight(int rows) {
        return CARD_HEADER_HEIGHT + HEADER_SLOT_GAP
                + Math.max(1, Math.min(6, rows)) * SLOT_SIZE
                + CARD_BOTTOM_PAD;
    }

    public static int emptyPageHeight() { return pageHeight(EMPTY_PAGE_ROWS); }
    public static int defaultEmptySlotCount() { return EMPTY_PAGE_ROWS * CONTROL_ROW_SLOTS; }

    /**
     * Cached pages keep their observed row count. Unknown empty placeholders
     * use the full Ender Chest / Greater Backpack 5-row well so cards do not
     * draw slots into the next row.
     */
    public static int slotRows(int cachedRows, boolean emptyItems) {
        if (cachedRows > 0) {
            return Math.max(1, Math.min(6, cachedRows));
        }
        return emptyItems ? EMPTY_PAGE_ROWS : 1;
    }

    public static OverlayLayout layout(
            int screenWidth,
            int screenHeight,
            int columns,
            int padding,
            int configuredHeight) {
        return layout(screenWidth, screenHeight, columns, padding, configuredHeight, INNER_PADDING);
    }

    public static OverlayLayout layout(
            int screenWidth,
            int screenHeight,
            int columns,
            int padding,
            int configuredHeight,
            int margin) {
        int cols = clampColumns(columns);
        int gap = clampSpacing(padding);
        int inset = Math.max(INNER_PADDING, clampSpacing(margin));
        int innerWidth = cols * pageWidth() + Math.max(0, cols - 1) * gap;
        int panelWidth = innerWidth + inset * 2 + INNER_PADDING + SCROLL_BAR_WIDTH;
        int reservedBelow = PLAYER_GAP + PLAYER_HEIGHT + Math.min(80, Math.max(0, screenHeight) / 10);
        int panelHeight = Math.min(
                Math.max(4, screenHeight - reservedBelow),
                clampHeight(configuredHeight));
        int stackHeight = panelHeight + PLAYER_GAP + PLAYER_HEIGHT;
        int panelX = screenWidth / 2 - panelWidth / 2;
        int panelY = Math.max(4, screenHeight / 2 - stackHeight / 2);
        if (panelY + stackHeight > screenHeight - 4) {
            panelY = Math.max(4, screenHeight - 4 - stackHeight);
        }
        int innerX = panelX + inset;
        int innerY = panelY + HEADER_HEIGHT + CONTENT_TOP_INSET;
        int innerHeight = Math.max(0, panelHeight - HEADER_HEIGHT - CONTENT_TOP_INSET - inset);
        int playerX = screenWidth / 2 - PLAYER_WIDTH / 2;
        int playerY = panelY + panelHeight + PLAYER_GAP;
        int scrollBarX = innerX + innerWidth + INNER_PADDING;
        int closeSize = CLOSE_BUTTON_SIZE;
        int closeX = panelX + panelWidth - 6 - closeSize;
        int closeY = panelY + Math.max(2, (HEADER_HEIGHT - closeSize) / 2);
        int searchWidth = SEARCH_WIDTH;
        int searchHeight = SEARCH_HEIGHT;
        int searchX = closeX - 6 - searchWidth;
        int searchY = panelY + Math.max(2, (HEADER_HEIGHT - searchHeight) / 2);
        return new OverlayLayout(
                panelX, panelY, panelWidth, panelHeight,
                innerX, innerY, innerWidth, innerHeight,
                playerX, playerY,
                scrollBarX, innerY, innerHeight,
                searchX, searchY, searchWidth, searchHeight,
                closeX, closeY, closeSize);
    }

    /** Chest menus store main inventory then hotbar; overlay positions use hotbar first. */
    public static int playerInventoryIndex(int chestPlayerIndex) {
        if (chestPlayerIndex < 0 || chestPlayerIndex >= 36) return chestPlayerIndex;
        return chestPlayerIndex < 27 ? chestPlayerIndex + 9 : chestPlayerIndex - 27;
    }

    public static int[] playerSlotPosition(int playerX, int playerY, int index) {
        if (index < 9) {
            return new int[] {playerX + index * SLOT_SIZE + HOTBAR_X, playerY + HOTBAR_Y};
        }
        return new int[] {
                playerX + (index % 9) * SLOT_SIZE + HOTBAR_X,
                playerY + (index / 9 - 1) * SLOT_SIZE + MAIN_INVENTORY_Y};
    }

    public static int[] contentSlotPosition(int cardX, int cardY, int index) {
        int originY = cardY + CARD_HEADER_HEIGHT + HEADER_SLOT_GAP;
        return new int[] {
                cardX + 3 + (index % 9) * SLOT_SIZE,
                originY + (index / 9) * SLOT_SIZE};
    }

    /**
     * Unique pixels of the slot's own 1px outline. Stays inside the 18×18 well
     * so adjacent matches never share a corner fragment.
     */
    public static int searchGlowPerimeter(int slotSize) {
        int size = Math.max(2, slotSize);
        int width = size - 1;
        int height = size - 1;
        return 2 * (width + height);
    }

    public static int searchGlowHead(long nowMillis, int perimeter) {
        if (perimeter <= 0) {
            return 0;
        }
        long period = Math.max(1L, SEARCH_GLOW_PERIOD_MS);
        long wrapped = Math.floorMod(nowMillis, period);
        return (int) (wrapped * (long) perimeter / period);
    }

    public static int searchGlowColor(int tailIndex, int tailLength) {
        int length = Math.max(1, tailLength);
        int index = Math.max(0, tailIndex);
        float t = Math.min(1.0F, index / (float) length);
        int alpha = Math.round(255.0F * (1.0F - t * 0.65F));
        return lerpArgb(SEARCH_GLOW_PURPLE, SEARCH_GLOW_RED, t, alpha);
    }

    public static int[] searchGlowPixel(int slotX, int slotY, int slotSize, int perimeterIndex) {
        int size = Math.max(2, slotSize);
        int left = slotX;
        int top = slotY;
        int width = size - 1;
        int height = size - 1;
        int perimeter = 2 * (width + height);
        int index = Math.floorMod(perimeterIndex, Math.max(1, perimeter));
        if (index < width) {
            return new int[] {left + index, top};
        }
        index -= width;
        if (index < height) {
            return new int[] {left + width, top + index};
        }
        index -= height;
        if (index < width) {
            return new int[] {left + width - index, top + height};
        }
        index -= width;
        return new int[] {left, top + height - index};
    }

    public static int[] searchGlowInward(int slotX, int slotY, int slotSize, int pixelX, int pixelY) {
        int size = Math.max(2, slotSize);
        int inwardX = pixelX <= slotX + 1 ? 1 : (pixelX >= slotX + size - 2 ? -1 : 0);
        int inwardY = pixelY <= slotY + 1 ? 1 : (pixelY >= slotY + size - 2 ? -1 : 0);
        return new int[] {pixelX + inwardX, pixelY + inwardY};
    }

    private static int lerpArgb(int from, int to, float t, int alpha) {
        float clamped = t < 0.0F ? 0.0F : Math.min(1.0F, t);
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * clamped);
        int g = Math.round(fg + (tg - fg) * clamped);
        int b = Math.round(fb + (tb - fb) * clamped);
        return ((alpha & 0xFF) << 24) | (r << 16) | (g << 8) | b;
    }

    public static boolean overScrollBar(OverlayLayout layout, int mouseX, int mouseY) {
        if (layout == null) {
            return false;
        }
        return inside(
                mouseX,
                mouseY,
                layout.scrollBarX(),
                layout.scrollBarY(),
                SCROLL_BAR_HIT_WIDTH,
                layout.scrollBarHeight());
    }

    public static int scrollBarTrackBottom(OverlayLayout layout) {
        if (layout == null) {
            return 0;
        }
        return layout.scrollBarY() + Math.max(0, layout.scrollBarHeight());
    }

    public static boolean overPlayerInventory(OverlayLayout layout, int mouseX, int mouseY) {
        if (layout == null) {
            return false;
        }
        return mouseX >= layout.playerX()
                && mouseX < layout.playerX() + PLAYER_WIDTH
                && mouseY >= layout.playerY()
                && mouseY < layout.playerY() + PLAYER_HEIGHT;
    }

    public static boolean overCardHeader(
            int cardX, int cardY, int cardWidth, int mouseX, int mouseY) {
        return mouseX >= cardX
                && mouseX < cardX + Math.max(1, cardWidth)
                && mouseY >= cardY
                && mouseY < cardY + CARD_HEADER_HEIGHT;
    }

    public static int[] valueIconPosition(int cardX, int cardY, int cardWidth) {
        int size = VALUE_ICON_SIZE;
        int x = cardX + Math.max(size + 4, cardWidth) - size - 3;
        int y = cardY + Math.max(2, (CARD_HEADER_HEIGHT - size) / 2);
        return new int[] {x, y, size};
    }

    public static boolean overValueIcon(
            int cardX, int cardY, int cardWidth, int mouseX, int mouseY) {
        int[] icon = valueIconPosition(cardX, cardY, cardWidth);
        return inside(mouseX, mouseY, icon[0], icon[1], icon[2], icon[2]);
    }

    public static int headerLabelMaxWidth(int cardWidth) {
        return Math.max(24, Math.max(1, cardWidth) - VALUE_ICON_SIZE - 14);
    }

    public record MarketLine(String marketId, int count) {
        public MarketLine {
            marketId = marketId == null ? "" : marketId;
            count = Math.max(0, count);
        }
    }

    public static double instantSellTotal(
            java.util.List<MarketLine> lines, java.util.Map<String, Double> unitPrices) {
        if (lines == null || unitPrices == null || unitPrices.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (MarketLine line : lines) {
            if (line == null || line.marketId().isBlank() || line.count() <= 0) {
                continue;
            }
            Double unit = unitPrices.get(line.marketId());
            if (unit == null || unit <= 0.0D || !Double.isFinite(unit)) {
                continue;
            }
            total += unit * line.count();
        }
        return total;
    }

    public static String formatCoins(long coins) {
        long abs = Math.abs(coins);
        String sign = coins < 0L ? "-" : "";
        if (abs >= 1_000_000_000L) {
            return sign + trimDecimal(abs / 1.0E9) + "b";
        }
        if (abs >= 1_000_000L) {
            return sign + trimDecimal(abs / 1_000_000.0) + "m";
        }
        if (abs >= 1_000L) {
            return sign + trimDecimal(abs / 1000.0) + "k";
        }
        return sign + abs;
    }

    public static double marketUnitValue(
            double lowestBin,
            double bazaarBuy,
            double bazaarSell) {
        if (bazaarSell > 0.0D && Double.isFinite(bazaarSell)) {
            return bazaarSell;
        }
        if (bazaarBuy > 0.0D && Double.isFinite(bazaarBuy)) {
            return bazaarBuy;
        }
        if (lowestBin > 0.0D && Double.isFinite(lowestBin)) {
            return lowestBin;
        }
        return 0.0D;
    }

    public static String pageValueLabel(double coins) {
        if (!(coins > 0.0D) || !Double.isFinite(coins)) {
            return "No AH/BZ prices yet";
        }
        return "Total value: " + formatCoins(Math.round(coins));
    }

    private static String trimDecimal(double value) {
        String raw = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (raw.indexOf('.') < 0) {
            return raw;
        }
        int end = raw.length();
        while (end > 0 && raw.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && raw.charAt(end - 1) == '.') {
            end--;
        }
        return raw.substring(0, end);
    }

    /**
     * A freshly opened page often arrives with empty slots for a few ticks.
     * Keep the last non-empty preview instead of wiping it.
     */
    /**
     * A freshly opened page often arrives with empty slots for a few ticks.
     * Keep the last non-empty preview instead of wiping it.
     */
    public static boolean shouldKeepExistingCache(boolean existingHasItems, boolean incomingAllEmpty) {
        return existingHasItems && incomingAllEmpty;
    }

    /**
     * Bare player heads without SkyBlock NBT or a skull texture are the
     * Steve placeholder. Do not let those overwrite a richer cached stack.
     */
    public static boolean incomingIsPlaceholder(
            boolean existingHasIdentity, boolean incomingHasIdentity, boolean incomingEmpty) {
        if (incomingEmpty) {
            return existingHasIdentity;
        }
        return existingHasIdentity && !incomingHasIdentity;
    }

    public record CachedStack(String itemId, int count, String name) {
        public CachedStack {
            itemId = itemId == null ? "" : itemId;
            count = Math.max(0, count);
            name = name == null ? "" : name;
        }
    }

    public record CachedPageSnapshot(Kind kind, int number, int rows, java.util.List<CachedStack> items) {
        public CachedPageSnapshot {
            rows = Math.max(0, rows);
            items = items == null ? java.util.List.of() : java.util.List.copyOf(items);
        }

        public Page page() {
            return new Page(kind, number);
        }
    }

    public static boolean shouldOpenPage(
            boolean shiftDown,
            boolean overRealSlot,
            boolean overPlayerInventory,
            boolean alreadySelected) {
        return !shiftDown && !overRealSlot && !overPlayerInventory && !alreadySelected;
    }

    public static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseX < x + Math.max(0, width)
                && mouseY >= y
                && mouseY < y + Math.max(0, height);
    }

    public static boolean clippedHit(
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            int clipX,
            int clipY,
            int clipWidth,
            int clipHeight) {
        int left = Math.max(x, clipX);
        int top = Math.max(y, clipY);
        int right = Math.min(x + Math.max(0, width), clipX + Math.max(0, clipWidth));
        int bottom = Math.min(y + Math.max(0, height), clipY + Math.max(0, clipHeight));
        return right > left && bottom > top && inside(mouseX, mouseY, left, top, right - left, bottom - top);
    }

    public static boolean insidePlayerPanel(int mouseX, int mouseY, int playerX, int playerY) {
        return inside(mouseX, mouseY, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    /**
     * Overlay clicks live far outside the vanilla 176-wide chest. Treat the
     * replacement panel and player inventory as inside the GUI so vanilla
     * does not rewrite the click to slot -999 (drop / close).
     */
    public static boolean isClickInsideOverlay(OverlayLayout layout, double mouseX, double mouseY) {
        if (layout == null) {
            return false;
        }
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        if (inside(mx, my, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight())) {
            return true;
        }
        if (insideSearchField(layout, mx, my)) {
            return true;
        }
        if (overScrollBar(layout, mx, my)) {
            return true;
        }
        return insidePlayerPanel(mx, my, layout.playerX(), layout.playerY());
    }

    public static boolean insideSearchField(OverlayLayout layout, int mouseX, int mouseY) {
        if (layout == null) {
            return false;
        }
        return inside(
                mouseX,
                mouseY,
                layout.searchX(),
                layout.searchY(),
                layout.searchWidth(),
                layout.searchHeight());
    }

    public static boolean searching(String query) {
        return !normalize(query).isEmpty();
    }

    public static String clampSearchQuery(String query) {
        String value = query == null ? "" : query;
        if (value.length() <= MAX_SEARCH_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_SEARCH_LENGTH);
    }

    public static String appendSearchChar(String query, String typed) {
        if (typed == null || typed.isEmpty()) {
            return clampSearchQuery(query);
        }
        return clampSearchQuery((query == null ? "" : query) + typed);
    }

    public static String deleteSearchChar(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        return query.substring(0, query.offsetByCodePoints(query.length(), -1));
    }

    /**
     * Page switches keep the cursor. Opening the dashboard or any other GUI
     * after Storage must use vanilla mouse ungrab so wheel events still land.
     */
    public static boolean shouldKeepCursorOnScreenChange(
            boolean overlayEnabled,
            boolean userExiting,
            boolean previousIsOverlay,
            boolean nextIsOverlay,
            boolean nextIsNull) {
        if (!overlayEnabled || userExiting) {
            return false;
        }
        if (nextIsNull) {
            return previousIsOverlay;
        }
        return previousIsOverlay && nextIsOverlay;
    }

    public static boolean shouldKeepUngrabbedCursor(
            boolean overlayEnabled,
            boolean currentScreenIsOverlay,
            boolean userExiting) {
        return overlayEnabled && currentScreenIsOverlay && !userExiting;
    }

    /** Tooltip pan must not swallow wheel on the dashboard or empty overlay chrome. */
    public static boolean shouldStealOverlayWheel(
            boolean containerScreen,
            boolean tooltipWantsWheel) {
        return shouldStealOverlayWheel(containerScreen, false, false, tooltipWantsWheel);
    }

    public static boolean shouldStealOverlayWheel(
            boolean containerScreen,
            boolean storageOverlay,
            boolean shiftHeld,
            boolean tooltipWantsWheel) {
        return CustomTooltipPolicy.shouldStealOverlayWheel(
                containerScreen, storageOverlay, shiftHeld, tooltipWantsWheel);
    }

    public static boolean shouldSuppressOutsideClick(
            boolean overlayActive, OverlayLayout layout, double mouseX, double mouseY) {
        return overlayActive && isClickInsideOverlay(layout, mouseX, mouseY);
    }

    public static boolean overCloseButton(OverlayLayout layout, int mouseX, int mouseY) {
        if (layout == null) {
            return false;
        }
        return inside(
                mouseX,
                mouseY,
                layout.closeX(),
                layout.closeY(),
                layout.closeSize(),
                layout.closeSize());
    }

    /**
     * Clicks on empty world around the replacement HUD close it so the player
     * can move again. The panel, search, scrollbar and inventory stay inside.
     */
    public static boolean shouldCloseOnOutsideClick(
            boolean overlayActive, OverlayLayout layout, int mouseX, int mouseY) {
        if (!overlayActive || layout == null) {
            return false;
        }
        return !isClickInsideOverlay(layout, mouseX, mouseY);
    }

    public static String clipSearchFromEnd(
            String query, int maxWidth, java.util.function.ToIntFunction<String> widthOf) {
        String shown = query == null ? "" : query;
        if (maxWidth <= 0) {
            return "";
        }
        java.util.function.ToIntFunction<String> measure =
                widthOf == null ? text -> text == null ? 0 : text.length() : widthOf;
        while (!shown.isEmpty() && measure.applyAsInt(shown) > maxWidth) {
            shown = shown.substring(shown.offsetByCodePoints(0, 1));
        }
        return shown;
    }

    public static int searchCaretX(int fieldX, int padding, int fieldWidth, int shownWidth) {
        int pad = Math.max(0, padding);
        int left = fieldX + pad;
        int right = fieldX + Math.max(pad + 1, fieldWidth) - Math.max(1, pad) - 1;
        return Math.max(left, Math.min(right, left + Math.max(0, shownWidth)));
    }

    /**
     * Page switches close the current chest for a tick. Keep the overlay
     * mounted so the mouse is never re-grabbed and recentered.
     */
    public static boolean shouldPinScreenOnClose(
            boolean overlayActive, boolean userExiting, boolean nextScreenIsNull) {
        return overlayActive && !userExiting && nextScreenIsNull;
    }

    public static boolean cacheFingerprintUnchanged(String previous, String incoming) {
        String left = previous == null ? "" : previous;
        String right = incoming == null ? "" : incoming;
        return left.equals(right);
    }

    /**
     * Opening another page must not steal inventory or live-slot clicks.
     * Shift-clicks always stay with vanilla quick-move.
     */
    public static boolean shouldNavigatePage(
            boolean shiftClick,
            boolean overLiveSlot,
            boolean activePage,
            int mouseX,
            int mouseY,
            int cardX,
            int cardY,
            int cardWidth,
            int cardHeight,
            OverlayLayout layout) {
        if (shiftClick || overLiveSlot || activePage || layout == null) {
            return false;
        }
        if (insidePlayerPanel(mouseX, mouseY, layout.playerX(), layout.playerY())) {
            return false;
        }
        return clippedHit(
                mouseX,
                mouseY,
                cardX,
                cardY,
                cardWidth,
                cardHeight,
                layout.innerX(),
                layout.innerY(),
                layout.innerWidth(),
                layout.innerHeight());
    }

    /** Case-insensitive local search over a cached item name. Blank means no filter. */
    public static boolean matchesSearch(String itemName, String query) {
        String needle = normalize(query);
        if (needle.isEmpty()) return true;
        String haystack = normalize(itemName);
        for (String term : needle.split(" ")) {
            if (!term.isBlank() && !haystack.contains(term)) return false;
        }
        return true;
    }

    public static int matchingNameCount(Collection<String> itemNames, String query) {
        if (itemNames == null || normalize(query).isEmpty()) return 0;
        int matches = 0;
        for (String itemName : itemNames) {
            if (matchesSearch(itemName, query)) matches++;
        }
        return matches;
    }

    /**
     * A storage search is deliberately local: only pages opened in a real
     * menu can be cached. Make that scope visible instead of implying that a
     * zero-result query searched pages the client has never received.
     */
    public static String searchSummary(String query, int cachedPageCount, int matchCount) {
        if (normalize(query).isEmpty()) return "Storage";
        int pages = Math.max(0, cachedPageCount);
        int matches = Math.max(0, matchCount);
        return "Storage · " + matches + " matches · " + pages
                + (pages == 1 ? " page cached" : " pages cached");
    }

    private static Optional<Page> page(Kind kind, String rawNumber) {
        int number;
        try {
            number = Integer.parseInt(rawNumber);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        if (number < 1 || number > 18) return Optional.empty();
        return Optional.of(new Page(kind, number));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("§.", "").trim()
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
