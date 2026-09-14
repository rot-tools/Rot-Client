package fi.rotclient;

import java.util.List;

/**
 * Compact world-HUD editor chrome.
 *
 * The TITLE panel is the single visible inspector. HELP and INSPECTOR remain
 * as compatibility IDs for old persisted coordinates, but the world editor
 * no longer makes users manage several floating instruction cards.
 */
public final class HudEditorChromePolicy {
    public static final int UNPLACED = Integer.MIN_VALUE;

    public static final int TITLE_WIDTH = 252;
    public static final int TITLE_HEIGHT = 224;

    public static final int HELP_WIDTH = 252;
    public static final int HELP_HEIGHT = 64;

    public static final int INSPECTOR_WIDTH = 220;
    public static final int INSPECTOR_HEADER_HEIGHT = 32;

    public static final int MARGIN = 10;
    public static final int GAP = 8;

    public enum Panel {
        TITLE,
        HELP,
        INSPECTOR
    }

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= x
                    && mouseX < x + w
                    && mouseY >= y
                    && mouseY < y + h;
        }

        public boolean containsHeader(
                int mouseX,
                int mouseY,
                int headerHeight) {

            return contains(mouseX, mouseY)
                    && mouseY < y + Math.max(1, headerHeight);
        }
    }

    private HudEditorChromePolicy() {
    }

    public static boolean isPlaced(int storedX, int storedY) {
        return storedX != UNPLACED
                && storedY != UNPLACED;
    }

    public static Rect defaultRect(
            Panel panel,
            int screenWidth,
            int screenHeight) {

        int width = switch (panel) {
            case TITLE -> TITLE_WIDTH;
            case HELP -> HELP_WIDTH;
            case INSPECTOR -> INSPECTOR_WIDTH;
        };

        int x = Math.max(
                MARGIN,
                screenWidth - width - MARGIN);

        int titleY = MARGIN;
        int helpY = titleY + TITLE_HEIGHT + GAP;
        int inspectorY = helpY + HELP_HEIGHT + GAP;
        int inspectorH = Math.max(
                120,
                screenHeight - inspectorY - MARGIN);

        return switch (panel) {
            case TITLE ->
                    new Rect(
                            x,
                            titleY,
                            TITLE_WIDTH,
                            TITLE_HEIGHT);

            case HELP ->
                    new Rect(
                            x,
                            helpY,
                            HELP_WIDTH,
                            HELP_HEIGHT);

            case INSPECTOR ->
                    new Rect(
                            x,
                            inspectorY,
                            INSPECTOR_WIDTH,
                            inspectorH);
        };
    }

    public static Rect resolve(
            Panel panel,
            int storedX,
            int storedY,
            int screenWidth,
            int screenHeight) {

        Rect fallback =
                defaultRect(
                        panel,
                        screenWidth,
                        screenHeight);

        if (!isPlaced(
                storedX,
                storedY)) {

            return fallback;
        }

        return new Rect(
                clamp(
                        storedX,
                        0,
                        Math.max(
                                0,
                                screenWidth - fallback.w())),
                clamp(
                        storedY,
                        0,
                        Math.max(
                                0,
                                screenHeight - fallback.h())),
                fallback.w(),
                fallback.h());
    }

    public static int clampDragX(
            int x,
            int panelWidth,
            int screenWidth) {

        return clamp(
                x,
                0,
                Math.max(
                        0,
                        screenWidth - panelWidth));
    }

    public static int clampDragY(
            int y,
            int panelHeight,
            int screenHeight) {

        return clamp(
                y,
                0,
                Math.max(
                        0,
                        screenHeight - panelHeight));
    }

    /**
     * Power-user hints only. The primary editor controls are now visible
     * buttons instead of a required shortcut cheat-sheet.
     */
    public static List<String> helpLines() {
        return List.of(
                "Drag HUDs directly; use the wheel or Scale controls.",
                "Right-click hides. Shortcuts: C center, R reset, Ctrl+Z undo.");
    }

    private static int clamp(
            int value,
            int min,
            int max) {

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}