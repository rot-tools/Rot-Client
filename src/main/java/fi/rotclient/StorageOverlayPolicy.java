package fi.rotclient;

import java.util.Locale;
import java.util.Collection;
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
            int scrollBarHeight) {}

    public static final int SLOT_SIZE = 18;
    public static final int CARD_HEADER_HEIGHT = 18;
    public static final int CONTROL_ROW_SLOTS = 9;
    public static final int PLAYER_WIDTH = 184;
    public static final int PLAYER_HEIGHT = 91;
    public static final int PLAYER_Y_INSET = 3;
    public static final int INNER_PADDING = 10;
    public static final int SCROLL_BAR_WIDTH = 8;
    public static final int SCROLL_KNOB_HEIGHT = 16;
    public static final int HOTBAR_X = 12;
    public static final int HOTBAR_Y = 67;
    public static final int MAIN_INVENTORY_Y = 9;

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
    public static int pageHeight(int rows) { return Math.max(1, Math.min(6, rows)) * SLOT_SIZE + 24; }
    public static int emptyPageHeight() { return 28; }

    public static OverlayLayout layout(
            int screenWidth,
            int screenHeight,
            int columns,
            int padding,
            int configuredHeight) {
        int cols = clampColumns(columns);
        int gap = clampSpacing(padding);
        int innerWidth = cols * pageWidth() + Math.max(0, cols - 1) * gap;
        int panelWidth = innerWidth + INNER_PADDING * 3 + SCROLL_BAR_WIDTH;
        int panelHeight = Math.min(
                Math.max(4, screenHeight - PLAYER_HEIGHT - Math.min(80, Math.max(0, screenHeight) / 10)),
                clampHeight(configuredHeight));
        int panelX = screenWidth / 2 - panelWidth / 2;
        int panelY = Math.max(4, screenHeight / 2 - (panelHeight + PLAYER_HEIGHT) / 2);
        int innerX = panelX + INNER_PADDING;
        int innerY = panelY + INNER_PADDING;
        int innerHeight = Math.max(0, panelHeight - INNER_PADDING * 2);
        int playerX = screenWidth / 2 - PLAYER_WIDTH / 2;
        int playerY = panelY + panelHeight - PLAYER_Y_INSET;
        int scrollBarX = innerX + innerWidth + INNER_PADDING;
        return new OverlayLayout(
                panelX, panelY, panelWidth, panelHeight,
                innerX, innerY, innerWidth, innerHeight,
                playerX, playerY,
                scrollBarX, innerY, innerHeight);
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
        return new int[] {
                cardX + 3 + (index % 9) * SLOT_SIZE,
                cardY + 20 + (index / 9) * SLOT_SIZE};
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

    /**
     * Page cards must not steal shift-clicks, player-inventory clicks, or
     * clicks that already sit on a live remapped slot.
     */
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
