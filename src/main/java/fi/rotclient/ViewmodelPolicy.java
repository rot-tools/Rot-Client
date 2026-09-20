package fi.rotclient;

/**
 * First-person held-item pose with bounded offset and scale ranges.
 */
public final class ViewmodelPolicy {
    public static final double OFFSET_MIN = -2.0D;
    public static final double OFFSET_MAX = 2.0D;
    public static final double SCALE_MIN = 0.0D;
    public static final double SCALE_MAX = 5.0D;
    public static final double ROT_MIN = -180.0D;
    public static final double ROT_MAX = 180.0D;
    public static final double SWING_MIN = 0.0D;
    public static final double SWING_MAX = 2.0D;
    public static final int SPEED_MIN = 0;
    public static final int SPEED_MAX = 100;

    private ViewmodelPolicy() {
    }

    public static double clampOffset(double value) {
        return clamp(value, OFFSET_MIN, OFFSET_MAX, 0.0D);
    }

    public static double clampScale(double value) {
        return clamp(value, SCALE_MIN, SCALE_MAX, 1.0D);
    }

    public static double clampRotation(double value) {
        return clamp(value, ROT_MIN, ROT_MAX, 0.0D);
    }

    public static double clampSwing(double value) {
        return clamp(value, SWING_MIN, SWING_MAX, 1.0D);
    }

    public static int clampSpeed(int value) {
        return Math.max(SPEED_MIN, Math.min(SPEED_MAX, value));
    }

    public static boolean applyToStack(boolean applyToHand, boolean stackEmpty) {
        return applyToHand || !stackEmpty;
    }

    public static int swingDuration(int vanilla, boolean noBowSwing, boolean holdingBow, int customSpeed) {
        if (noBowSwing && holdingBow) {
            return 0;
        }
        if (customSpeed > 0) {
            return customSpeed;
        }
        return vanilla;
    }

    /** A non-finite value falls back to the neutral pose (scale 1, offset 0), not to the minimum. */
    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
