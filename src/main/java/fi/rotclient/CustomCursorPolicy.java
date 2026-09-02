package fi.rotclient;

/**
 * Rot Client pointer look and click/hold/resize animation math.
 * Rendered by the client overlay; no GLFW here so tests stay Minecraft-free.
 */
public final class CustomCursorPolicy {
    public static final double MIN_SIZE = 0.6D;
    public static final double MAX_SIZE = 2.4D;
    public static final double DEFAULT_SIZE = 1.0D;
    public static final int DEFAULT_FILL = 0xFFF2F2F2;
    public static final int DEFAULT_OUTLINE = 0xFF111111;
    public static final int DEFAULT_ACCENT = 0xFFB8B8B8;
    private static final int LEGACY_DEFAULT_FILL = 0xFFFF4D6D;
    private static final int LEGACY_DEFAULT_OUTLINE = 0xFF08080C;
    private static final int LEGACY_DEFAULT_ACCENT = 0xFFC084FC;

    public enum ResizeKind {
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

    public record Snapshot(
            double size,
            int fill,
            int outline,
            int accent,
            boolean clickAnim,
            boolean holdAnim,
            boolean hideVanilla,
            ResizeKind resize,
            boolean pressed,
            long heldMs) {
    }

    private CustomCursorPolicy() {
    }

    public static double clampSize(double size) {
        if (!Double.isFinite(size)) {
            return DEFAULT_SIZE;
        }
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }

    public static ResizeKind fromWindowEdge(RotClientWindowPlacementPolicy.ResizeEdge edge) {
        if (edge == null) {
            return ResizeKind.NONE;
        }
        return switch (edge) {
            case N -> ResizeKind.N;
            case S -> ResizeKind.S;
            case E -> ResizeKind.E;
            case W -> ResizeKind.W;
            case NE -> ResizeKind.NE;
            case NW -> ResizeKind.NW;
            case SE -> ResizeKind.SE;
            case SW -> ResizeKind.SW;
            case NONE -> ResizeKind.NONE;
        };
    }

    public static float clickPulse(boolean pressed, boolean clickAnim, long heldMs) {
        if (!clickAnim || !pressed) {
            return 1.0F;
        }
        if (heldMs < 90L) {
            return 1.18F;
        }
        return 1.08F;
    }

    public static int holdRingAlpha(boolean pressed, boolean holdAnim, long heldMs) {
        if (!holdAnim || !pressed || heldMs < 80L) {
            return 0;
        }
        int wave = (int) ((heldMs / 16L) % 40L);
        return 70 + wave;
    }

    public static int pointerSizePx(double size, float pulse) {
        return Math.max(10, (int) Math.round(18.0D * clampSize(size) * pulse));
    }

    public static int effectiveFill(int configured) {
        return configured == LEGACY_DEFAULT_FILL ? DEFAULT_FILL : configured;
    }

    public static int effectiveOutline(int configured) {
        return configured == LEGACY_DEFAULT_OUTLINE ? DEFAULT_OUTLINE : configured;
    }

    public static int effectiveAccent(int configured) {
        return configured == LEGACY_DEFAULT_ACCENT ? DEFAULT_ACCENT : configured;
    }

    /**
     * Hide the OS pointer while a menu is open and the overlay is on.
     * Restore {@code GLFW_CURSOR_NORMAL} only while that menu stays open.
     * Never force NORMAL in the world: that un-grabs look.
     */
    public static boolean shouldHideVanillaCursor(
            boolean moduleEnabled,
            boolean hideVanilla,
            boolean menuOpen) {
        return moduleEnabled && hideVanilla && menuOpen;
    }

    public static boolean shouldDrawOverlay(
            boolean moduleEnabled,
            boolean hideVanilla,
            boolean menuOpen) {
        return shouldHideVanillaCursor(moduleEnabled, hideVanilla, menuOpen);
    }
}
