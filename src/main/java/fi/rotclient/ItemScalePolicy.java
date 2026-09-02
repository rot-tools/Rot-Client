package fi.rotclient;

/**
 * Scale of dropped item entities on the ground.
 */
public final class ItemScalePolicy {
    public static final double MIN = 0.1D;
    public static final double MAX = 5.0D;
    public static final double DEFAULT = 1.0D;

    private ItemScalePolicy() {
    }

    public static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT;
        }
        return Math.max(MIN, Math.min(MAX, value));
    }

    public static float renderScale(boolean enabled, double scale) {
        if (!enabled) {
            return 1.0F;
        }
        return (float) clamp(scale);
    }
}
