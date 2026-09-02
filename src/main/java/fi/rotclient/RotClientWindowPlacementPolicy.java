package fi.rotclient;

import java.util.Locale;

/**
 * Windows-style window placement for the in-game dashboard: floating,
 * maximized, and left/right half snap. Minecraft-free so unit tests can lock
 * snap thresholds and restore-on-drag geometry.
 */
public final class RotClientWindowPlacementPolicy {
    public static final int SNAP_EDGE_PX = 28;
    public static final int SNAP_TOP_PX = 16;
    public static final int RESIZE_GRIP_PX = 6;
    public static final int FLOATING_MIN_WIDTH = 480;
    public static final int FLOATING_MIN_HEIGHT = 320;
    public static final String FLOATING = "floating";
    public static final String MAXIMIZED = "maximized";
    public static final String SNAP_LEFT = "left";
    public static final String SNAP_RIGHT = "right";

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Rect size must be positive");
            }
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    public enum ResizeEdge {
        NONE,
        N,
        S,
        E,
        W,
        NE,
        NW,
        SE,
        SW
    }

    private RotClientWindowPlacementPolicy() {
    }

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case MAXIMIZED, SNAP_LEFT, SNAP_RIGHT -> value;
            default -> FLOATING;
        };
    }

    public static boolean isSnapped(String placement) {
        String value = normalize(placement);
        return MAXIMIZED.equals(value)
                || SNAP_LEFT.equals(value)
                || SNAP_RIGHT.equals(value);
    }

    /**
     * Default floating size is the existing 86% canvas, then user-resized
     * fractions override when {@code normW}/{@code normH} are stored.
     */
    public static Rect floatingRect(
            int viewportW,
            int viewportH,
            float normX,
            float normY,
            float normW,
            float normH) {
        RotClientDashboardLayout.PanelSize fallback =
                RotClientDashboardLayout.resolve(viewportW, viewportH);
        int width = fallback.width();
        int height = fallback.height();
        if (normW > 0.02F && viewportW > 0) {
            width = Math.round(clamp01(normW) * viewportW);
        }
        if (normH > 0.02F && viewportH > 0) {
            height = Math.round(clamp01(normH) * viewportH);
        }
        width = clamp(width, minWidth(viewportW), Math.max(1, viewportW));
        height = clamp(height, minHeight(viewportH), Math.max(1, viewportH));
        int x = RotClientClientUiLayout.clampPanelX(viewportW, width,
                RotClientClientUiLayout.panelX(viewportW, width, normX));
        int y = RotClientClientUiLayout.clampPanelY(viewportH, height,
                RotClientClientUiLayout.panelY(viewportH, height, normY));
        return new Rect(x, y, width, height);
    }

    public static Rect apply(String placement, int viewportW, int viewportH, Rect floating) {
        String value = normalize(placement);
        int vw = Math.max(1, viewportW);
        int vh = Math.max(1, viewportH);
        Rect safeFloating = floating == null
                ? floatingRect(vw, vh, 0.5F, 0.5F, 0.0F, 0.0F)
                : floating;
        return switch (value) {
            case MAXIMIZED -> new Rect(0, 0, vw, vh);
            case SNAP_LEFT -> new Rect(0, 0, Math.max(1, vw / 2), vh);
            case SNAP_RIGHT -> {
                int width = Math.max(1, vw / 2);
                yield new Rect(vw - width, 0, width, vh);
            }
            default -> safeFloating;
        };
    }

    public static String snapPreview(int viewportW, int viewportH, int mouseX, int mouseY) {
        int vw = Math.max(1, viewportW);
        int vh = Math.max(1, viewportH);
        if (mouseY <= SNAP_TOP_PX) {
            return MAXIMIZED;
        }
        if (mouseX <= SNAP_EDGE_PX) {
            return SNAP_LEFT;
        }
        if (mouseX >= vw - SNAP_EDGE_PX) {
            return SNAP_RIGHT;
        }
        if (mouseY < 0 || mouseY > vh || mouseX < 0 || mouseX > vw) {
            return FLOATING;
        }
        return FLOATING;
    }

    /**
     * When dragging a maximized/snapped window, restore the floating size and
     * keep the cursor on the caption — same idea as Windows.
     */
    public static Rect restoreUnderCursor(Rect floating, int mouseX, int viewportW, int viewportH) {
        Rect rest = floating == null
                ? floatingRect(viewportW, viewportH, 0.5F, 0.5F, 0.0F, 0.0F)
                : floating;
        int width = rest.width();
        int height = rest.height();
        int x = mouseX - (width / 2);
        int y = 0;
        x = RotClientClientUiLayout.clampPanelX(viewportW, width, x);
        y = RotClientClientUiLayout.clampPanelY(viewportH, height, y);
        return new Rect(x, y, width, height);
    }

    public static String toggleMaximize(String placement) {
        return MAXIMIZED.equals(normalize(placement)) ? FLOATING : MAXIMIZED;
    }

    public static ResizeEdge hitResizeEdge(Rect panel, int mouseX, int mouseY) {
        if (panel == null) {
            return ResizeEdge.NONE;
        }
        int grip = RESIZE_GRIP_PX;
        boolean onLeft = mouseX >= panel.x() - 1 && mouseX <= panel.x() + grip;
        boolean onRight = mouseX >= panel.right() - grip && mouseX <= panel.right() + 1;
        boolean onTop = mouseY >= panel.y() - 1 && mouseY <= panel.y() + grip;
        boolean onBottom = mouseY >= panel.bottom() - grip && mouseY <= panel.bottom() + 1;
        boolean insideX = mouseX >= panel.x() - 1 && mouseX <= panel.right() + 1;
        boolean insideY = mouseY >= panel.y() - 1 && mouseY <= panel.bottom() + 1;
        if (!insideX || !insideY) {
            return ResizeEdge.NONE;
        }
        if (onTop && onLeft) {
            return ResizeEdge.NW;
        }
        if (onTop && onRight) {
            return ResizeEdge.NE;
        }
        if (onBottom && onLeft) {
            return ResizeEdge.SW;
        }
        if (onBottom && onRight) {
            return ResizeEdge.SE;
        }
        if (onTop) {
            return ResizeEdge.N;
        }
        if (onBottom) {
            return ResizeEdge.S;
        }
        if (onLeft) {
            return ResizeEdge.W;
        }
        if (onRight) {
            return ResizeEdge.E;
        }
        return ResizeEdge.NONE;
    }

    public static Rect resize(
            Rect panel,
            ResizeEdge edge,
            int mouseX,
            int mouseY,
            int viewportW,
            int viewportH) {
        if (panel == null || edge == null || edge == ResizeEdge.NONE) {
            return panel;
        }
        int minW = minWidth(viewportW);
        int minH = minHeight(viewportH);
        int x = panel.x();
        int y = panel.y();
        int right = panel.right();
        int bottom = panel.bottom();
        if (edge == ResizeEdge.W || edge == ResizeEdge.NW || edge == ResizeEdge.SW) {
            x = Math.min(mouseX, right - minW);
        }
        if (edge == ResizeEdge.E || edge == ResizeEdge.NE || edge == ResizeEdge.SE) {
            right = Math.max(mouseX, x + minW);
        }
        if (edge == ResizeEdge.N || edge == ResizeEdge.NE || edge == ResizeEdge.NW) {
            y = Math.min(mouseY, bottom - minH);
        }
        if (edge == ResizeEdge.S || edge == ResizeEdge.SE || edge == ResizeEdge.SW) {
            bottom = Math.max(mouseY, y + minH);
        }
        int width = Math.max(minW, right - x);
        int height = Math.max(minH, bottom - y);
        x = RotClientClientUiLayout.clampPanelX(viewportW, width, x);
        y = RotClientClientUiLayout.clampPanelY(viewportH, height, y);
        width = Math.min(width, Math.max(1, viewportW - x));
        height = Math.min(height, Math.max(1, viewportH - y));
        return new Rect(x, y, width, height);
    }

    public static float normW(int viewportW, int width) {
        int vw = Math.max(1, viewportW);
        return clamp01(width / (float) vw);
    }

    public static float normH(int viewportH, int height) {
        int vh = Math.max(1, viewportH);
        return clamp01(height / (float) vh);
    }

    static int minWidth(int viewportW) {
        int vw = Math.max(1, viewportW);
        return Math.min(FLOATING_MIN_WIDTH, vw);
    }

    static int minHeight(int viewportH) {
        int vh = Math.max(1, viewportH);
        return Math.min(FLOATING_MIN_HEIGHT, vh);
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
