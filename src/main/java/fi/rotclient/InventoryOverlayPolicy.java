package fi.rotclient;

import java.util.List;
import java.util.Locale;

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
    /** Vanilla armor column inside {@code InventoryScreen}. */
    public static final int ARMOR_COLUMN_X = 8;
    public static final int SLOT_STRIDE = 18;
    public static final int SLOT_SIZE = 18;

    public static final int EQUIPMENT_SLOT_COUNT = 4;
    /** Bottom equipment bar — pet sits to its right by default. */
    public static final int OPEN_STATS_SLOT_INDEX = 3;

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

    public static final int WRENCH_SIZE = 16;
    public static final int WRENCH_GAP = 4;
    public static final int EDITOR_WIDTH = 214;
    public static final int EDITOR_ROW = 18;
    public static final int EDITOR_SLIDER_HEIGHT = 10;

    public enum ChromeColorRole {
        INV_PANEL("Inventory panel", "qol.inventory_overlay.chrome_panel"),
        INV_HEADER("Crafting / armor", "qol.inventory_overlay.chrome_header"),
        INV_MAIN("Item grid", "qol.inventory_overlay.chrome_main"),
        INV_HOTBAR("Hotbar", "qol.inventory_overlay.chrome_hotbar"),
        INV_BORDER("Inventory border", "qol.inventory_overlay.chrome_border"),
        STORAGE_PANEL("Storage panel", "qol.storage_overlay.panel_color"),
        STORAGE_CARD("Storage cards", "qol.storage_overlay.card_color"),
        STORAGE_ACTIVE("Open storage", "qol.storage_overlay.card_active_color"),
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

    public static Rect editorRect(int wrenchX, int wrenchY, int screenWidth, int screenHeight) {
        int height = 22 + ChromeColorRole.values().length * EDITOR_ROW + 8
                + 4 * (EDITOR_SLIDER_HEIGHT + 6) + 18;
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

    public static Rect editorSwatchRect(Rect row) {
        return new Rect(row.x() + row.width() - 22, row.y() + 2, 20, row.height() - 4);
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
}
