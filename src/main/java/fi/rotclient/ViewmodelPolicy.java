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
        return clamp(value, OFFSET_MIN, OFFSET_MAX);
    }

    public static double clampScale(double value) {
        return clamp(value, SCALE_MIN, SCALE_MAX);
    }

    public static double clampRotation(double value) {
        return clamp(value, ROT_MIN, ROT_MAX);
    }

    public static double clampSwing(double value) {
        return clamp(value, SWING_MIN, SWING_MAX);
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

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min < 0.0D && max > 0.0D ? 0.0D : min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
