package fi.rotclient;

/**
 * Resolution-independent Client UI panel placement helpers.
 */
final class RotClientClientUiLayout {
    private RotClientClientUiLayout() {
    }

    static int panelX(
            int logicalWidth,
            int panelWidth,
            float normX) {
        int usable = Math.max(1, logicalWidth - panelWidth);
        int x = Math.round(clamp01(normX) * usable);
        return clamp(x, minMargin(logicalWidth, panelWidth), maxX(logicalWidth, panelWidth));
    }

    static int panelY(
            int logicalHeight,
            int panelHeight,
            float normY) {
        int usable = Math.max(1, logicalHeight - panelHeight);
        int y = Math.round(clamp01(normY) * usable);
        return clamp(y, minMargin(logicalHeight, panelHeight), maxY(logicalHeight, panelHeight));
    }

    static float normXFromPanelX(int logicalWidth, int panelWidth, int panelX) {
        int usable = Math.max(1, logicalWidth - panelWidth);
        return clamp01(panelX / (float) usable);
    }

    static float normYFromPanelY(int logicalHeight, int panelHeight, int panelY) {
        int usable = Math.max(1, logicalHeight - panelHeight);
        return clamp01(panelY / (float) usable);
    }

    static int clampPanelX(int logicalWidth, int panelWidth, int panelX) {
        return clamp(panelX, minMargin(logicalWidth, panelWidth), maxX(logicalWidth, panelWidth));
    }

    static int clampPanelY(int logicalHeight, int panelHeight, int panelY) {
        return clamp(panelY, minMargin(logicalHeight, panelHeight), maxY(logicalHeight, panelHeight));
    }

    /**
     * Ensures at least the header strip remains reachable when the panel is
     * larger than the viewport.
     */
    private static int minMargin(int viewport, int panel) {
        if (panel >= viewport) {
            return Math.min(0, viewport - Math.min(panel, viewport));
        }
        return 4;
    }

    private static int maxX(int logicalWidth, int panelWidth) {
        if (panelWidth >= logicalWidth) {
            return 0;
        }
        return Math.max(4, logicalWidth - panelWidth - 4);
    }

    private static int maxY(int logicalHeight, int panelHeight) {
        if (panelHeight >= logicalHeight) {
            // Keep top header reachable.
            return Math.max(0, logicalHeight - Math.min(48, panelHeight));
        }
        return Math.max(2, logicalHeight - panelHeight - 2);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.5F;
        }
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }
}
