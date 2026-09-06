package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

/**
 * Layout and chest-title rules for the SkyBlock inventory overlay: four
 * stacked equipment bars, a pet slot to the right of the bottom bar, and
 * related menus. Minecraft-free so unit tests can lock titles and hit boxes.
 */
public final class InventoryOverlayPolicy {
    /** Necklace, cloak, belt, gloves in the Hypixel "Your Equipment and Stats" chest. */
    public static final int[] EQUIPMENT_CHEST_SLOTS = {10, 19, 28, 37};

    /**
     * Pet icon in the Serveri Stats & Equipment 9x6 doll
     * (column 3, under the boots).
     */
    public static final int STATS_MENU_PET_SLOT = 47;

    /** Command that opens the Stats & Equipment chest. */
    public static final String OPEN_STATS_COMMAND = "stats";

    public static final String OPEN_STATS_LABEL = "Stats & Equipment";

    /** Command that opens the Equipment Wardrobe chest. */
    public static final String OPEN_WARDROBE_COMMAND = "equipment";

    public static final String OPEN_WARDROBE_LABEL = "Equipment Wardrobe";

    public static final String OPEN_PETS_COMMAND = "pets";

    public static final String OPEN_PETS_LABEL = "Pets";

    /** Pixel offset from the survival-inventory GUI origin. */
    public static final int EQUIPMENT_COLUMN_X = 76;
    public static final int EQUIPMENT_COLUMN_Y = 8;
    /**
     * Vanilla paper-doll scissor box inside {@code InventoryScreen}
     * ({@code leftPos+26, topPos+8} to {@code leftPos+75, topPos+78}).
     */
    public static final int PLAYER_PREVIEW_X = 26;
    public static final int PLAYER_PREVIEW_Y = 8;
    public static final int PLAYER_PREVIEW_WIDTH = 49;
    public static final int PLAYER_PREVIEW_HEIGHT = 70;
    /**
     * 2×2 crafting grid, result slot, and the Crafting label. Stops above
     * the SkyBlock mascot strip at y=50+ so that cat stay covered.
     */
    public static final int CRAFTING_GRID_X = 97;
    public static final int CRAFTING_GRID_Y = 6;
    public static final int CRAFTING_GRID_WIDTH = 76;
    public static final int CRAFTING_GRID_HEIGHT = 48;
    /** Vanilla armor column inside {@code InventoryScreen}. */
    public static final int ARMOR_COLUMN_X = 8;
    public static final int ARMOR_COLUMN_Y = 8;
    public static final int CRAFT_INPUT_X = 98;
    public static final int CRAFT_INPUT_Y = 18;
    public static final int CRAFT_RESULT_X = 154;
    public static final int CRAFT_RESULT_Y = 28;
    public static final int MAIN_INVENTORY_X = 8;
    public static final int MAIN_INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;
    public static final int MAIN_ROWS = 3;
    public static final int MAIN_COLUMNS = 9;
    public static final int SLOT_STRIDE = 18;
    public static final int SLOT_SIZE = 18;
    public static final int DEFAULT_SLOT_BORDER = 0xFF7A628C;
    public static final int DEFAULT_SLOT_WELL = 0xFF100C14;

    public static final int EQUIPMENT_SLOT_COUNT = 4;
    /** Bottom equipment bar — pet sits to its right by default. */
    public static final int OPEN_STATS_SLOT_INDEX = 3;

    /**
     * Last observed necklace/cloak/belt/gloves and the chosen pet are stored
     * locally under this config file, same idea as Storage Overlay's page cache.
     */
    public static final String CHROME_CACHE_FILE = "rotclient-inventory-chrome-cache.json";

    public static final int CHROME_CACHE_SCHEMA = 1;

    /**
     * Right of the bottom equipment bar, under the 2x2 crafting grid.
     * {@code 76 + 18}, {@code 8 + 3 * 18}.
     */
    public static final int DEFAULT_PET_SLOT_X = EQUIPMENT_COLUMN_X + SLOT_SIZE;
    public static final int DEFAULT_PET_SLOT_Y =
            EQUIPMENT_COLUMN_Y + OPEN_STATS_SLOT_INDEX * SLOT_STRIDE;

    public static final int PET_OFFSET_MIN = -240;
    public static final int PET_OFFSET_MAX = 240;

    /** Vanilla survival-inventory off-hand slot origin inside the GUI. */
    public static final int VANILLA_OFFHAND_SLOT_X = 77;
    public static final int VANILLA_OFFHAND_SLOT_Y = 62;

    private InventoryOverlayPolicy() {
    }

    public static final int DEFAULT_INV_PANEL = 0xF518141C;
    public static final int DEFAULT_INV_HEADER = 0xF524182C;
    public static final int DEFAULT_INV_MAIN = 0xF50C0A10;
    public static final int DEFAULT_INV_HOTBAR = 0xF5221A28;
    public static final int DEFAULT_INV_BORDER = 0xFF4A3858;

    public static boolean isEquipmentMenu(String title) {
        String text = MenuKeybindPolicy.stripGuiText(title);
        if (text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("equipment sets") || lower.contains("wardrobe")) {
            return false;
        }
        return lower.contains("your equipment")
                || lower.equals("equipment")
                || lower.equals("equipment and stats")
                || lower.contains("equipment and stats")
                || lower.contains("stats & equipment")
                || lower.contains("stats and equipment");
    }

    public static boolean isEquipmentSetsMenu(String title) {
        String text = MenuKeybindPolicy.stripGuiText(title);
        if (text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("equipment sets")
                || (lower.contains("wardrobe") && lower.contains("equipment"));
    }

    /**
     * Lime dye in chest slots 36–44 marks the selected wardrobe column.
     */
    public static OptionalInt equipmentSetsColumn(int slotIndex, String itemPath) {
        String path = itemPath == null ? "" : itemPath.toLowerCase(Locale.ROOT);
        if (slotIndex > 35 && slotIndex < 45 && path.contains("lime_dye")) {
            return OptionalInt.of(slotIndex % 9);
        }
        return OptionalInt.empty();
    }

    public static boolean isEquipmentSetsPieceSlot(int slotIndex, int column) {
        return column >= 0
                && column < 9
                && slotIndex >= 0
                && slotIndex < 36
                && slotIndex % 9 == column
                && slotIndex / 9 < 4;
    }

    /**
     * Equipment bars, pet slot, inventory chrome, inventory buttons, storage
     * overlay, and skill-level digits stay off in Hypixel lobby and other
     * modes. Cached loadouts still persist for the next SkyBlock join.
     */
    public static boolean showSkyblockInventoryUi(boolean inSkyblock) {
        return inSkyblock;
    }

    /**
     * Hide Inventory Effects follows the overlay module, not the SkyBlock
     * scoreboard, so lobby still suppresses the vanilla potion cards.
     */
    public static boolean hideInventoryStatusEffects(
            boolean overlayEnabled, boolean hideToggle) {
        return overlayEnabled && hideToggle;
    }

    public static boolean isSkillsMenu(String title) {
        String text = MenuKeybindPolicy.stripGuiText(title);
        if (text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("your skills")
                || lower.equals("skills")
                || lower.startsWith("skills (")
                || lower.startsWith("skills -")
                || lower.contains("skill menu")
                || (lower.contains("skills") && !lower.contains("bestiary"));
    }

    /**
     * A freshly opened Stats / Pets chest often arrives with empty slots for a
     * few ticks. Keep the last non-empty preview instead of wiping it.
     */
    public static boolean shouldKeepExistingCache(
            boolean existingHasItems, boolean incomingAllEmpty) {
        return existingHasItems && incomingAllEmpty;
    }

    /**
     * Reload from disk when memory is empty, or once more after the world
     * exists so ItemStack codec JSON can decode. Do not loop if there is no file.
     */
    public static boolean shouldReloadChromeCache(
            boolean alreadyLoaded,
            boolean memoryEmpty,
            boolean fileExists,
            boolean codecReady,
            boolean levelReady) {
        if (!fileExists) {
            return !alreadyLoaded;
        }
        if (!alreadyLoaded || memoryEmpty) {
            return true;
        }
        return levelReady && !codecReady;
    }

    /**
     * An empty overlay must not replace a file that already has a loadout.
     */
    public static boolean shouldSkipEmptyChromeSave(
            boolean memoryEmpty, boolean diskHadItems) {
        return memoryEmpty && diskHadItems;
    }

    public static boolean isPlaceholder(String hoverName, String itemPath) {
        if (itemPath == null) {
            itemPath = "";
        }
        String path = itemPath.toLowerCase(Locale.ROOT);
        if (path.endsWith("glass_pane") || path.endsWith("stained_glass")) {
            return true;
        }
        String name = MenuKeybindPolicy.stripGuiText(hoverName).toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            return true;
        }
        return name.contains("empty")
                || name.endsWith(" slot")
                || name.contains("empty slot")
                || name.contains("click to equip");
    }

    /**
     * 0 necklace, 1 cloak, 2 belt, 3 gloves. {@code -1} when the item is not
     * a recognizable equipment piece.
     */
    public static int classifyEquipmentIndex(String hoverName, List<String> loreLines) {
        StringBuilder blob = new StringBuilder(MenuKeybindPolicy.stripGuiText(hoverName));
        if (loreLines != null) {
            for (String line : loreLines) {
                blob.append('\n').append(MenuKeybindPolicy.stripGuiText(line));
            }
        }
        String lower = blob.toString().toLowerCase(Locale.ROOT);
        if (containsToken(lower, "necklace")
                || containsToken(lower, "amulet")
                || containsToken(lower, "pendant")) {
            return 0;
        }
        if (containsToken(lower, "cloak") || containsToken(lower, "cape")) {
            return 1;
        }
        if (containsToken(lower, "belt")) {
            return 2;
        }
        if (containsToken(lower, "gloves")
                || containsToken(lower, "bracelet")
                || containsToken(lower, "gauntlet")) {
            return 3;
        }
        return -1;
    }

    private static boolean containsToken(String haystack, String token) {
        return haystack.contains(token);
    }

    public static int equipmentSlotX() {
        return EQUIPMENT_COLUMN_X;
    }

    public static int equipmentSlotY(int index) {
        return EQUIPMENT_COLUMN_Y + index * SLOT_STRIDE;
    }

    public static int hitEquipmentIndex(int guiLeft, int guiTop, int mouseX, int mouseY) {
        for (int i = 0; i < EQUIPMENT_SLOT_COUNT; i++) {
            int x = guiLeft + equipmentSlotX();
            int y = guiTop + equipmentSlotY(i);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hitOpenStatsSlot(int guiLeft, int guiTop, int mouseX, int mouseY) {
        return hitEquipmentIndex(guiLeft, guiTop, mouseX, mouseY) >= 0;
    }

    public static int clampPetOffset(int value) {
        return Math.max(PET_OFFSET_MIN, Math.min(PET_OFFSET_MAX, value));
    }

    public static int petSlotX() {
        return petSlotX(0);
    }

    public static int petSlotY() {
        return petSlotY(0);
    }

    public static int petSlotX(int offsetX) {
        return DEFAULT_PET_SLOT_X + clampPetOffset(offsetX);
    }

    public static int petSlotY(int offsetY) {
        return DEFAULT_PET_SLOT_Y + clampPetOffset(offsetY);
    }

    public static boolean hitPetSlot(int guiLeft, int guiTop, int mouseX, int mouseY) {
        return hitPetSlot(guiLeft, guiTop, 0, 0, mouseX, mouseY);
    }

    public static boolean hitPetSlot(
            int guiLeft,
            int guiTop,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY) {
        int x = guiLeft + petSlotX(offsetX);
        int y = guiTop + petSlotY(offsetY);
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    public static boolean isVanillaOffhandSlot(int slotX, int slotY) {
        return slotX == VANILLA_OFFHAND_SLOT_X && slotY == VANILLA_OFFHAND_SLOT_Y;
    }

    public static int petGrabX(int guiLeft, int mouseX, int offsetX) {
        return mouseX - (guiLeft + petSlotX(offsetX));
    }

    public static int petGrabY(int guiTop, int mouseY, int offsetY) {
        return mouseY - (guiTop + petSlotY(offsetY));
    }

    public static PetDragOffset offsetFromDrag(
            int guiLeft,
            int guiTop,
            int mouseX,
            int mouseY,
            int grabX,
            int grabY) {
        int slotX = mouseX - grabX;
        int slotY = mouseY - grabY;
        return new PetDragOffset(
                clampPetOffset(slotX - guiLeft - DEFAULT_PET_SLOT_X),
                clampPetOffset(slotY - guiTop - DEFAULT_PET_SLOT_Y));
    }

    public record PetDragOffset(int x, int y) {
    }

    public enum ChromeRegion {
        PANEL,
        HEADER,
        MAIN,
        HOTBAR,
        BORDER
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public static final int WRENCH_SIZE = 12;
    public static final int WRENCH_GAP = 8;
    public static final int EDITOR_WIDTH = 248;
    public static final int EDITOR_ROW = 18;
    public static final int EDITOR_SLIDER_HEIGHT = 10;
    public static final int EDITOR_RESET_WIDTH = 34;
    public static final int EDITOR_CLOSE_SIZE = 10;
    /** S-value and Rot R sit on the pet-slot row and match its 18×18 well. */
    public static final int STRIP_CONTROL_SIZE = SLOT_SIZE;
    public static final int VALUE_MARK_GAP = 3;
    public static final int DEFAULT_STORAGE_PANEL = 0xF00A1520;
    public static final int DEFAULT_STORAGE_CARD = 0xFF122433;
    public static final int DEFAULT_STORAGE_ACTIVE = 0xFF18384A;
    public static final int DEFAULT_STORAGE_PLAYER = 0xF00A1520;

    public enum ChromeColorRole {
        INV_PANEL("Whole window", "qol.inventory_overlay.chrome_panel"),
        INV_HEADER("Armor column", "qol.inventory_overlay.chrome_header"),
        INV_MAIN("Backpack slots", "qol.inventory_overlay.chrome_main"),
        INV_HOTBAR("Hotbar", "qol.inventory_overlay.chrome_hotbar"),
        INV_BORDER("Outline", "qol.inventory_overlay.chrome_border"),
        STORAGE_PANEL("Storage window", "qol.storage_overlay.panel_color"),
        STORAGE_CARD("Storage cards", "qol.storage_overlay.card_color"),
        STORAGE_ACTIVE("Open storage card", "qol.storage_overlay.card_active_color"),
        STORAGE_PLAYER("Storage inventory", "qol.storage_overlay.player_color");

        private final String label;
        private final String settingId;

        ChromeColorRole(String label, String settingId) {
            this.label = label;
            this.settingId = settingId;
        }

        public String label() {
            return label;
        }

        public String hint() {
            return switch (this) {
                case INV_PANEL -> "Tints the inventory window behind items";
                case INV_HEADER -> "Armor column and the strip beside the player";
                case INV_MAIN -> "The 3x9 backpack rows";
                case INV_HOTBAR -> "The bottom hotbar row";
                case INV_BORDER -> "1-pixel edge around the window";
                case STORAGE_PANEL -> "Background of /storage (not this screen)";
                case STORAGE_CARD -> "Ender Chest / backpack cards in /storage";
                case STORAGE_ACTIVE -> "The storage page you currently have open";
                case STORAGE_PLAYER -> "Your inventory strip inside /storage";
            };
        }

        public String settingId() {
            return settingId;
        }
    }

    public static Rect wrenchRect(int guiLeft, int guiTop, int guiWidth) {
        int width = Math.max(WRENCH_SIZE + WRENCH_GAP * 2, guiWidth);
        return new Rect(
                guiLeft + width - WRENCH_SIZE - WRENCH_GAP,
                guiTop + WRENCH_GAP,
                WRENCH_SIZE,
                WRENCH_SIZE);
    }

    /**
     * SkyBlock's inventory texture paints a bowtie cat beside the 2x2
     * crafting grid. Cover that strip, including a few pixels past the
     * panel edge, without overlapping the left crafting column.
     */
    public static Rect mascotCoverRect(int guiLeft, int guiTop, int guiWidth) {
        int x = guiLeft + 140;
        int y = guiTop + 50;
        int width = Math.max(28, Math.max(1, guiWidth) - 140 + 12);
        return new Rect(x, y, width, 34);
    }

    public static Rect editorRect(int wrenchX, int wrenchY, int screenWidth, int screenHeight) {
        int height = 22 + ChromeColorRole.values().length * EDITOR_ROW + 8
                + 4 * (EDITOR_SLIDER_HEIGHT + 6) + 36;
        int x = wrenchX + WRENCH_SIZE + 6;
        if (x + EDITOR_WIDTH > screenWidth - 4) {
            x = wrenchX - EDITOR_WIDTH - 6;
        }
        if (x < 4) {
            x = Math.max(4, screenWidth - EDITOR_WIDTH - 4);
        }
        int y = wrenchY;
        if (y + height > screenHeight - 4) {
            y = Math.max(4, screenHeight - height - 4);
        }
        return new Rect(x, y, EDITOR_WIDTH, height);
    }

    public static Rect editorRowRect(Rect editor, int index) {
        return new Rect(
                editor.x() + 8,
                editor.y() + 20 + index * EDITOR_ROW,
                editor.width() - 16,
                EDITOR_ROW - 2);
    }

    public static Rect editorResetRect(Rect row) {
        return new Rect(
                row.x() + row.width() - 22 - 4 - EDITOR_RESET_WIDTH,
                row.y() + 2,
                EDITOR_RESET_WIDTH,
                row.height() - 4);
    }

    public static Rect editorSwatchRect(Rect row) {
        return new Rect(row.x() + row.width() - 22, row.y() + 2, 20, row.height() - 4);
    }

    public static int defaultChromeColor(ChromeColorRole role) {
        if (role == null) {
            return 0;
        }
        return switch (role) {
            case INV_PANEL -> DEFAULT_INV_PANEL;
            case INV_HEADER -> DEFAULT_INV_HEADER;
            case INV_MAIN -> DEFAULT_INV_MAIN;
            case INV_HOTBAR -> DEFAULT_INV_HOTBAR;
            case INV_BORDER -> DEFAULT_INV_BORDER;
            case STORAGE_PANEL -> DEFAULT_STORAGE_PANEL;
            case STORAGE_CARD -> DEFAULT_STORAGE_CARD;
            case STORAGE_ACTIVE -> DEFAULT_STORAGE_ACTIVE;
            case STORAGE_PLAYER -> DEFAULT_STORAGE_PLAYER;
        };
    }

    public static Rect editorCloseRect(Rect editor) {
        if (editor == null) {
            return new Rect(0, 0, EDITOR_CLOSE_SIZE, EDITOR_CLOSE_SIZE);
        }
        return new Rect(
                editor.x() + editor.width() - EDITOR_CLOSE_SIZE - 4,
                editor.y() + 4,
                EDITOR_CLOSE_SIZE,
                EDITOR_CLOSE_SIZE);
    }

    /**
     * Clicking the close icon or anywhere outside the open panel dismisses it.
     * The wrench still toggles and is handled separately.
     */
    public static boolean dismissColorEditor(
            boolean open,
            boolean inWrench,
            boolean inEditor,
            boolean inClose) {
        if (!open || inWrench) {
            return false;
        }
        return inClose || !inEditor;
    }

    /**
     * S-mark immediately right of the live pet well, same 18×18 slot chrome.
     */
    public static Rect valueMarkRect(int guiLeft, int guiTop) {
        return valueMarkRect(guiLeft, guiTop, 0, 0);
    }

    public static Rect valueMarkRect(
            int guiLeft, int guiTop, int petOffsetX, int petOffsetY) {
        return stripControlRect(guiLeft, guiTop, petOffsetX, petOffsetY, 0);
    }

    /**
     * Rot R, same 18×18 well as the S-mark, one gap to its right. Stays in
     * that row even when the pet is Ctrl-dragged.
     */
    public static Rect dashboardButtonRect(int guiLeft, int guiTop, int guiWidth) {
        return dashboardButtonRect(guiLeft, guiTop, 0, 0);
    }

    public static Rect dashboardButtonRect(
            int guiLeft, int guiTop, int petOffsetX, int petOffsetY) {
        return stripControlRect(guiLeft, guiTop, petOffsetX, petOffsetY, 1);
    }

    static Rect stripControlRect(
            int guiLeft,
            int guiTop,
            int petOffsetX,
            int petOffsetY,
            int index) {
        int slot = Math.max(0, index);
        int x = guiLeft
                + petSlotX(petOffsetX)
                + (1 + slot) * (SLOT_SIZE + VALUE_MARK_GAP);
        int y = guiTop + petSlotY(petOffsetY);
        return new Rect(x, y, SLOT_SIZE, SLOT_SIZE);
    }

    public static boolean hitDashboardButton(
            int guiLeft, int guiTop, int guiWidth, int mouseX, int mouseY) {
        return hitDashboardButton(guiLeft, guiTop, 0, 0, mouseX, mouseY);
    }

    public static boolean hitDashboardButton(
            int guiLeft,
            int guiTop,
            int petOffsetX,
            int petOffsetY,
            int mouseX,
            int mouseY) {
        return dashboardButtonRect(guiLeft, guiTop, petOffsetX, petOffsetY)
                .contains(mouseX, mouseY);
    }

    public static boolean hitValueMark(int guiLeft, int guiTop, int mouseX, int mouseY) {
        return hitValueMark(guiLeft, guiTop, 0, 0, mouseX, mouseY);
    }

    public static boolean hitValueMark(
            int guiLeft,
            int guiTop,
            int petOffsetX,
            int petOffsetY,
            int mouseX,
            int mouseY) {
        return valueMarkRect(guiLeft, guiTop, petOffsetX, petOffsetY)
                .contains(mouseX, mouseY);
    }

    public static Rect editorSliderRect(Rect editor, int channel) {
        int top = editor.y() + 20 + ChromeColorRole.values().length * EDITOR_ROW + 10
                + channel * (EDITOR_SLIDER_HEIGHT + 6);
        return new Rect(editor.x() + 28, top, editor.width() - 36, EDITOR_SLIDER_HEIGHT);
    }

    public static int channelValue(int argb, int channel) {
        return switch (channel) {
            case 0 -> (argb >>> 16) & 0xFF;
            case 1 -> (argb >>> 8) & 0xFF;
            case 2 -> argb & 0xFF;
            default -> (argb >>> 24) & 0xFF;
        };
    }

    public static int withChannel(int argb, int channel, int value) {
        int clamped = Math.max(0, Math.min(255, value));
        return switch (channel) {
            case 0 -> (argb & 0xFF00FFFF) | (clamped << 16);
            case 1 -> (argb & 0xFFFF00FF) | (clamped << 8);
            case 2 -> (argb & 0xFFFFFF00) | clamped;
            default -> (argb & 0x00FFFFFF) | (clamped << 24);
        };
    }

    public static int sliderValue(Rect slider, int mouseX) {
        if (slider.width() <= 1) {
            return 0;
        }
        float t = (mouseX - slider.x()) / (float) (slider.width() - 1);
        return Math.max(0, Math.min(255, Math.round(t * 255)));
    }

    /**
     * RGB edits that would stay fully transparent stay invisible. Bump opacity
     * so the first color drag is visible; Alpha 0 still hides on purpose.
     */
    public static int withChannelEnsuringVisible(int argb, int channel, int value) {
        int next = withChannel(argb, channel, value);
        if (channel == 3) {
            return next;
        }
        if (channelValue(next, 3) == 0
                && (channelValue(next, 0) != 0
                || channelValue(next, 1) != 0
                || channelValue(next, 2) != 0)) {
            return withChannel(next, 3, 224);
        }
        return next;
    }

    public static boolean hitWrench(int guiLeft, int guiTop, int guiWidth, int mouseX, int mouseY) {
        return wrenchRect(guiLeft, guiTop, guiWidth).contains(mouseX, mouseY);
    }

    public static Rect chromeRegion(
            ChromeRegion region,
            int guiLeft,
            int guiTop,
            int guiWidth,
            int guiHeight) {
        int width = Math.max(1, guiWidth);
        int height = Math.max(1, guiHeight);
        return switch (region) {
            case PANEL -> new Rect(guiLeft, guiTop, width, height);
            case HEADER -> new Rect(guiLeft, guiTop, width, Math.min(76, height));
            case MAIN -> new Rect(guiLeft + 7, guiTop + 83, Math.max(1, width - 14), 54);
            case HOTBAR -> new Rect(guiLeft + 7, guiTop + 141, Math.max(1, width - 14), 18);
            case BORDER -> new Rect(guiLeft, guiTop, width, height);
        };
    }

    public static Rect playerPreviewRect(int guiLeft, int guiTop) {
        return new Rect(
                guiLeft + PLAYER_PREVIEW_X,
                guiTop + PLAYER_PREVIEW_Y,
                PLAYER_PREVIEW_WIDTH,
                PLAYER_PREVIEW_HEIGHT);
    }

    public static Rect craftingGridRect(int guiLeft, int guiTop) {
        return new Rect(
                guiLeft + CRAFTING_GRID_X,
                guiTop + CRAFTING_GRID_Y,
                CRAFTING_GRID_WIDTH,
                CRAFTING_GRID_HEIGHT);
    }

    /**
     * PANEL and HEADER skip the paper doll. Item slots are painted as
     * explicit wells on top of the remaining chrome.
     */
    public static List<Rect> chromeFillRects(
            ChromeRegion region,
            int guiLeft,
            int guiTop,
            int guiWidth,
            int guiHeight) {
        Rect fill = chromeRegion(region, guiLeft, guiTop, guiWidth, guiHeight);
        if (region == ChromeRegion.PANEL || region == ChromeRegion.HEADER) {
            return subtractAll(fill, playerPreviewRect(guiLeft, guiTop));
        }
        return List.of(fill);
    }

    public static Rect slotRect(int guiLeft, int guiTop, int slotX, int slotY) {
        return new Rect(guiLeft + slotX, guiTop + slotY, SLOT_SIZE, SLOT_SIZE);
    }

    /**
     * Vanilla survival-inventory wells: armor, 2×2 craft, result, 3×9
     * backpack, hotbar, and optionally the off-hand slot.
     */
    public static List<Rect> survivalSlotRects(int guiLeft, int guiTop, boolean includeOffhand) {
        List<Rect> slots = new ArrayList<>();
        for (int i = 0; i < EQUIPMENT_SLOT_COUNT; i++) {
            slots.add(slotRect(guiLeft, guiTop, ARMOR_COLUMN_X, ARMOR_COLUMN_Y + i * SLOT_STRIDE));
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                slots.add(slotRect(
                        guiLeft,
                        guiTop,
                        CRAFT_INPUT_X + col * SLOT_STRIDE,
                        CRAFT_INPUT_Y + row * SLOT_STRIDE));
            }
        }
        slots.add(slotRect(guiLeft, guiTop, CRAFT_RESULT_X, CRAFT_RESULT_Y));
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < MAIN_COLUMNS; col++) {
                slots.add(slotRect(
                        guiLeft,
                        guiTop,
                        MAIN_INVENTORY_X + col * SLOT_STRIDE,
                        MAIN_INVENTORY_Y + row * SLOT_STRIDE));
            }
        }
        for (int col = 0; col < MAIN_COLUMNS; col++) {
            slots.add(slotRect(
                    guiLeft,
                    guiTop,
                    MAIN_INVENTORY_X + col * SLOT_STRIDE,
                    HOTBAR_Y));
        }
        if (includeOffhand) {
            slots.add(slotRect(guiLeft, guiTop, VANILLA_OFFHAND_SLOT_X, VANILLA_OFFHAND_SLOT_Y));
        }
        return slots;
    }

    static List<Rect> subtractAll(Rect outer, Rect... holes) {
        List<Rect> current = new ArrayList<>();
        if (outer != null && outer.width() > 0 && outer.height() > 0) {
            current.add(outer);
        }
        if (holes == null) {
            return current;
        }
        for (Rect hole : holes) {
            List<Rect> next = new ArrayList<>();
            for (Rect piece : current) {
                next.addAll(subtract(piece, hole));
            }
            current = next;
        }
        return current;
    }

    static List<Rect> subtract(Rect outer, Rect hole) {
        if (outer == null || outer.width() <= 0 || outer.height() <= 0) {
            return List.of();
        }
        if (hole == null || hole.width() <= 0 || hole.height() <= 0) {
            return List.of(outer);
        }
        int x1 = Math.max(outer.x(), hole.x());
        int y1 = Math.max(outer.y(), hole.y());
        int x2 = Math.min(outer.x() + outer.width(), hole.x() + hole.width());
        int y2 = Math.min(outer.y() + outer.height(), hole.y() + hole.height());
        if (x2 <= x1 || y2 <= y1) {
            return List.of(outer);
        }
        List<Rect> parts = new ArrayList<>();
        if (y1 > outer.y()) {
            parts.add(new Rect(outer.x(), outer.y(), outer.width(), y1 - outer.y()));
        }
        int outerBottom = outer.y() + outer.height();
        if (y2 < outerBottom) {
            parts.add(new Rect(outer.x(), y2, outer.width(), outerBottom - y2));
        }
        if (x1 > outer.x()) {
            parts.add(new Rect(outer.x(), y1, x1 - outer.x(), y2 - y1));
        }
        int outerRight = outer.x() + outer.width();
        if (x2 < outerRight) {
            parts.add(new Rect(x2, y1, outerRight - x2, y2 - y1));
        }
        return parts;
    }
}
