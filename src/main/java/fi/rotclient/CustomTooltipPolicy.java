package fi.rotclient;

/** Hover-box pan offsets and compact name-only mode. */
public final class CustomTooltipPolicy {
    public static final String STYLE_SEPARATED = "Separated";
    public static final String STYLE_COMBINED = "Combined";

    private CustomTooltipPolicy() {
    }

    public static int nextOffset(
            int current,
            int delta,
            int speed,
            boolean enabled,
            boolean infinite,
            int max) {
        if (!enabled || speed <= 0) {
            return current;
        }
        int next = current + delta * speed;
        if (infinite) {
            if (max <= 0) {
                return next;
            }
            int span = max + speed;
            next %= span;
            if (next < 0) {
                next += span;
            }
            return next;
        }
        int limit = Math.max(0, max);
        return Math.max(-limit, Math.min(limit, next));
    }

    public static int panAfterClamp(int clamped, int offset) {
        return clamped - offset;
    }

    /**
     * Wheel-down should reveal the lower part of a tall hover box. Minecraft
     * reports that as a negative vertical amount, so invert before applying.
     */
    public static int verticalWheelDelta(double verticalAmount) {
        int delta = (int) Math.round(verticalAmount);
        if (delta == 0 && verticalAmount != 0.0D) {
            delta = verticalAmount > 0.0D ? 1 : -1;
        }
        return -delta;
    }

    public static int keepOnScreen(
            int clamped,
            int offset,
            int screenSize,
            int boxSize,
            boolean infinite) {
        int min = Math.min(0, screenSize - boxSize);
        int max = Math.max(0, screenSize - boxSize);
        int next = panAfterClamp(clamped, offset);
        if (infinite) {
            return wrapInto(next, min, max);
        }
        return Math.max(min, Math.min(max, next));
    }

    static int wrapInto(int value, int min, int max) {
        if (max <= min) {
            return min;
        }
        int span = max - min;
        int shifted = value - min;
        int wrapped = Math.floorMod(shifted, span);
        return min + wrapped;
    }

    public static boolean showOnlyName(boolean moduleEnabled, boolean keyUnbound, boolean keyHeld) {
        return moduleEnabled && !keyUnbound && keyHeld;
    }

    /**
     * Storage Overlay: Shift+wheel pans the hover box; a plain wheel pages
     * the overlay. Other inventories: wheel pans vertically, Shift pans
     * sideways.
     */
    public static boolean panTooltipVertically(boolean storageOverlay, boolean shiftHeld) {
        return storageOverlay ? shiftHeld : !shiftHeld;
    }

    public static boolean panTooltipHorizontally(
            boolean storageOverlay, boolean shiftHeld, boolean horizontalEnabled) {
        return horizontalEnabled && !storageOverlay && shiftHeld;
    }

    public static boolean storageOverlayTakesWheel(boolean storageOverlay, boolean shiftHeld) {
        return storageOverlay && !shiftHeld;
    }

    public static boolean shouldStealOverlayWheel(
            boolean containerScreen,
            boolean storageOverlay,
            boolean shiftHeld,
            boolean tooltipWantsWheel) {
        if (!containerScreen || !tooltipWantsWheel) {
            return false;
        }
        if (storageOverlay) {
            return shiftHeld;
        }
        return true;
    }

    public static String normalizeStyle(String raw) {
        if (raw != null && raw.equalsIgnoreCase(STYLE_COMBINED)) {
            return STYLE_COMBINED;
        }
        return STYLE_SEPARATED;
    }
}
