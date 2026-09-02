package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Inventory button model, validation, anchoring and presets. */
public final class InventoryButtonsPolicy {
    public static final int SMALL_SIZE = 18;
    public static final int LARGE_SIZE = 38;
    public static final int GAP = 2;

    public static final class Button {
        public int x;
        public int y;
        public boolean anchorRight;
        public boolean anchorBottom;
        public String icon;
        public String command;
        public boolean large;

        public Button() {}

        public Button(int x, int y, boolean anchorRight, boolean anchorBottom,
                      String icon, String command, boolean large) {
            this.x = x;
            this.y = y;
            this.anchorRight = anchorRight;
            this.anchorBottom = anchorBottom;
            this.icon = normalizeIcon(icon);
            this.command = normalizeCommand(command);
            this.large = large;
        }

        public Button copy() {
            return new Button(x, y, anchorRight, anchorBottom, icon, command, large);
        }
    }

    private InventoryButtonsPolicy() {}

    public static String normalizeCommand(String value) {
        String command = value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").trim();
        while (command.startsWith("/")) command = command.substring(1).trim();
        command = command.replaceAll("\\s+", " ");
        return command.length() > 128 ? command.substring(0, 128) : command;
    }

    public static String normalizeIcon(String value) {
        String icon = value == null ? "minecraft:command_block" : value.trim().toLowerCase(Locale.ROOT);
        if (icon.isBlank()) icon = "minecraft:command_block";
        if (!icon.contains(":")) icon = "minecraft:" + icon;
        return icon;
    }

    public static boolean valid(Button button) {
        return button != null && !normalizeCommand(button.command).isBlank();
    }

    /** Offset used when an editor duplicates a button, keeping the copy visible. */
    public static Button duplicatedBeside(Button source) {
        if (source == null) return null;
        Button duplicate = source.copy();
        duplicate.x += size(duplicate) + GAP;
        return duplicate;
    }

    public static int size(Button button) { return button != null && button.large ? LARGE_SIZE : SMALL_SIZE; }

    public static int screenX(Button button, int guiLeft, int guiWidth) {
        return button.anchorRight ? guiLeft + guiWidth + button.x : guiLeft + button.x;
    }

    public static int screenY(Button button, int guiTop, int guiHeight) {
        return button.anchorBottom ? guiTop + guiHeight + button.y : guiTop + button.y;
    }

    public static boolean hit(Button button, int guiLeft, int guiTop, int guiWidth, int guiHeight,
                              int mouseX, int mouseY) {
        int x = screenX(button, guiLeft, guiWidth);
        int y = screenY(button, guiTop, guiHeight);
        int size = size(button);
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    /** Convert a screen-space top-left into persistent anchored offsets. */
    public static void moveTo(
            Button button,
            int screenX,
            int screenY,
            int guiLeft,
            int guiTop,
            int guiWidth,
            int guiHeight) {
        if (button == null) {
            return;
        }
        int size = size(button);
        int centerX = screenX + size / 2;
        int centerY = screenY + size / 2;
        boolean right = centerX > guiLeft + guiWidth / 2;
        boolean bottom = centerY > guiTop + guiHeight / 2;
        button.anchorRight = right;
        button.anchorBottom = bottom;
        button.x = right ? screenX - (guiLeft + guiWidth) : screenX - guiLeft;
        button.y = bottom ? screenY - (guiTop + guiHeight) : screenY - guiTop;
    }

    public static List<Button> simplePreset() {
        return List.of(
                new Button(-20, 0, false, false, "minecraft:ender_chest", "storage", false),
                new Button(-20, 20, false, false, "minecraft:player_head", "pets", false),
                new Button(-20, 40, false, false, "minecraft:golden_chestplate", "wardrobe", false));
    }

    public static List<Button> allWarpsPreset() {
        String[] commands = {"warp hub", "is", "warp dungeon_hub", "warp forge", "warp mines",
                "warp crystal_hollows", "warp kuudra", "warp garden"};
        String[] icons = {"grass_block", "oak_sapling", "wither_skeleton_skull", "anvil",
                "diamond_pickaxe", "amethyst_block", "magma_cream", "wheat"};
        List<Button> result = new ArrayList<>();
        for (int i = 0; i < commands.length; i++) {
            result.add(new Button(i * 20, -20, false, false, icons[i], commands[i], false));
        }
        return List.copyOf(result);
    }
}
