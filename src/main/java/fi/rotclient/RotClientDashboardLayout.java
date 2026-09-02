package fi.rotclient;

/**
 * Responsive dashboard panel sizing. Targets a large control-center canvas
 * (~72% of the logical Minecraft UI viewport) with safe fallbacks on small
 * resolutions. The dashboard intentionally leaves room for the game world:
 * it is a control panel, not a replacement full-screen menu.
 */
final class RotClientDashboardLayout {
    static final int BASE_WIDTH = 920;
    static final int BASE_HEIGHT = 580;
    static final int MIN_WIDTH = 700;
    static final int MIN_HEIGHT = 440;
    static final int VIEWPORT_MARGIN = 36;
    static final double VIEWPORT_FRACTION = 0.72D;
    static final int SIDEBAR_WIDTH = 196;
    static final int HEADER_HEIGHT = 42;
    static final int TAB_STRIP_HEIGHT = 28;
    static final int OMNIBOX_HEIGHT = 32;
    static final int CONTENT_INSET = 16;
    static final int HOME_BUTTON_Y_OFFSET = 38;
    static final int ACTION_ROW_HEIGHT = 24;
    static final int ACTION_ROW_GAP = 10;

    private RotClientDashboardLayout() {
    }

    record PanelSize(int width, int height) {
        PanelSize {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Panel size must be positive");
            }
        }
    }

    /**
     * Chooses a panel size that fills most of the logical Minecraft UI
     * viewport while remaining inset from the edges.
     */
    static PanelSize resolve(int logicalWidth, int logicalHeight) {
        int widthBasis = logicalWidth > 0 ? logicalWidth : BASE_WIDTH;
        int heightBasis = logicalHeight > 0 ? logicalHeight : BASE_HEIGHT;
        int maxW = Math.max(1, widthBasis - VIEWPORT_MARGIN);
        int maxH = Math.max(1, heightBasis - VIEWPORT_MARGIN);
        int minW = Math.min(MIN_WIDTH, maxW);
        int minH = Math.min(MIN_HEIGHT, maxH);
        int targetW = (int) Math.round(widthBasis * VIEWPORT_FRACTION);
        int targetH = (int) Math.round(heightBasis * VIEWPORT_FRACTION);
        if (logicalWidth <= 0) {
            targetW = BASE_WIDTH;
            maxW = Math.max(maxW, BASE_WIDTH);
            minW = MIN_WIDTH;
        }
        if (logicalHeight <= 0) {
            targetH = BASE_HEIGHT;
            maxH = Math.max(maxH, BASE_HEIGHT);
            minH = MIN_HEIGHT;
        }
        int width = clamp(targetW, minW, maxW);
        int height = clamp(targetH, minH, maxH);
        if (logicalWidth > 0) {
            width = Math.min(width, Math.max(1, logicalWidth - 8));
        }
        if (logicalHeight > 0) {
            height = Math.min(height, Math.max(1, logicalHeight - 8));
        }
        return new PanelSize(Math.max(1, width), Math.max(1, height));
    }

    static int contentLeft(int panelX) {
        return panelX + SIDEBAR_WIDTH + CONTENT_INSET;
    }

    static int contentRight(int panelX, int panelWidth) {
        return panelX + panelWidth - CONTENT_INSET;
    }

    static int actionRowY(int panelY, int panelHeight) {
        return panelY + panelHeight - HOME_BUTTON_Y_OFFSET - ACTION_ROW_HEIGHT
                - ACTION_ROW_GAP;
    }

    static int contentClipBottom(int panelY, int panelHeight) {
        return actionRowY(panelY, panelHeight) - 10;
    }

    /** Tabs at the top plus the omnibox toolbar, matching Chrome chrome. */
    static int chromeHeight() {
        return TAB_STRIP_HEIGHT + OMNIBOX_HEIGHT;
    }

    static int omniboxY(int panelY) {
        return panelY + TAB_STRIP_HEIGHT;
    }

    static int contentTop(int panelY) {
        return panelY + chromeHeight();
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
