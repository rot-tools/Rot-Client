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

    public static boolean showOnlyName(boolean moduleEnabled, boolean keyUnbound, boolean keyHeld) {
        return moduleEnabled && !keyUnbound && keyHeld;
    }

    public static String normalizeStyle(String raw) {
        if (raw != null && raw.equalsIgnoreCase(STYLE_COMBINED)) {
            return STYLE_COMBINED;
        }
        return STYLE_SEPARATED;
    }
}
