package fi.rotclient;

import java.util.List;

/**
 * Layout-editor chrome (title, how-to card, inspector) is itself movable so
 * those panels can be dragged off live HUDs. Unplaced coordinates keep the
 * default right-hand column.
 */
public final class HudEditorChromePolicy {
    public static final int UNPLACED = Integer.MIN_VALUE;
    public static final int TITLE_WIDTH = 268;
    public static final int TITLE_HEIGHT = 50;
    public static final int HELP_WIDTH = 268;
    public static final int HELP_HEIGHT = 106;
    public static final int INSPECTOR_WIDTH = 228;
    public static final int INSPECTOR_HEADER_HEIGHT = 36;
    public static final int MARGIN = 8;
    public static final int GAP = 8;

    public enum Panel {
        TITLE,
        HELP,
        INSPECTOR
    }

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }

        public boolean containsHeader(int mouseX, int mouseY, int headerHeight) {
            return contains(mouseX, mouseY)
                    && mouseY < y + Math.max(1, headerHeight);
        }
    }

    private HudEditorChromePolicy() {
    }

    public static boolean isPlaced(int storedX, int storedY) {
        return storedX != UNPLACED && storedY != UNPLACED;
    }

    public static Rect defaultRect(Panel panel, int screenWidth, int screenHeight) {
        int width = switch (panel) {
            case TITLE -> TITLE_WIDTH;
            case HELP -> HELP_WIDTH;
            case INSPECTOR -> INSPECTOR_WIDTH;
        };
        int x = Math.max(MARGIN, screenWidth - width - MARGIN);
        int titleY = MARGIN;
        int helpY = titleY + TITLE_HEIGHT + GAP;
        int inspectorY = helpY + HELP_HEIGHT + GAP;
        int inspectorH = Math.max(120, screenHeight - inspectorY - MARGIN);
        return switch (panel) {
            case TITLE -> new Rect(x, titleY, TITLE_WIDTH, TITLE_HEIGHT);
            case HELP -> new Rect(x, helpY, HELP_WIDTH, HELP_HEIGHT);
            case INSPECTOR -> new Rect(x, inspectorY, INSPECTOR_WIDTH, inspectorH);
        };
    }

    public static Rect resolve(
            Panel panel,
            int storedX,
            int storedY,
            int screenWidth,
            int screenHeight) {
        Rect fallback = defaultRect(panel, screenWidth, screenHeight);
        if (!isPlaced(storedX, storedY)) {
            return fallback;
        }
        return new Rect(
                clamp(storedX, 0, Math.max(0, screenWidth - fallback.w())),
                clamp(storedY, 0, Math.max(0, screenHeight - fallback.h())),
                fallback.w(),
                fallback.h());
    }

    public static int clampDragX(int x, int panelWidth, int screenWidth) {
        return clamp(x, 0, Math.max(0, screenWidth - panelWidth));
    }

    public static int clampDragY(int y, int panelHeight, int screenHeight) {
        return clamp(y, 0, Math.max(0, screenHeight - panelHeight));
    }

    public static List<String> helpLines() {
        return List.of(
                "Drag a pink outline to move that HUD.",
                "Scroll the wheel on a HUD to scale it.",
                "Inspector: Background (text only), scale, colors, on/off bits.",
                "H / V / C center  ·  R reset  ·  Shift+R all",
                "1 Mining  ·  2 Powder  ·  3 Client UI  ·  4 QoL",
                "Drag these cards if they cover a HUD.");
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
