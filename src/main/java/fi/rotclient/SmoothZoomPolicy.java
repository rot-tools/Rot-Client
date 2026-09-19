package fi.rotclient;

public final class SmoothZoomPolicy {
    public static final double MIN_AMOUNT = 1.25D;
    public static final double MAX_AMOUNT = 10.0D;
    public static final double DEFAULT_AMOUNT = 4.0D;

    /**
     * The scroll wheel can push the magnification past the configured range while the key is
     * held: down to almost no zoom, up to the point where the 5 degree FOV floor takes over.
     */
    public static final double MIN_LIVE_AMOUNT = 1.1D;
    public static final double MAX_LIVE_AMOUNT = 30.0D;
    /** Each wheel notch multiplies the magnification by this much, so steps feel even. */
    public static final double SCROLL_STEP = 1.15D;
    private static final double MAX_SCROLL_NOTCHES = 5.0D;

    public static final double MIN_SPEED = 1.0D;
    public static final double MAX_SPEED = 20.0D;
    public static final double DEFAULT_SPEED = 8.0D;

    private static final double MIN_EFFECTIVE_FOV = 5.0D;
    private static final double MAX_FRAME_SECONDS = 0.10D;
    private static final double SNAP_EPSILON = 0.0005D;

    private SmoothZoomPolicy() {
    }

    /**
     * Zoom only applies while playing. It is a held key that is polled directly,
     * so with a screen open (chat, inventory) typing the bound letter would
     * otherwise zoom the camera behind the screen.
     */
    public static boolean canZoom(boolean inWorld, boolean screenOpen, boolean keyBound) {
        return inWorld && !screenOpen && keyBound;
    }

    public static double clampAmount(double value) {
        return clamp(
                value,
                MIN_AMOUNT,
                MAX_AMOUNT,
                DEFAULT_AMOUNT);
    }

    public static double clampLiveAmount(double value) {
        return clamp(value, MIN_LIVE_AMOUNT, MAX_LIVE_AMOUNT, DEFAULT_AMOUNT);
    }

    /** Wheel up (positive) zooms in, wheel down zooms out. */
    public static double scrollAmount(double current, double notches) {
        double base = clampLiveAmount(current);
        if (!Double.isFinite(notches)) {
            return base;
        }
        double bounded = Math.max(-MAX_SCROLL_NOTCHES, Math.min(MAX_SCROLL_NOTCHES, notches));
        return clampLiveAmount(base * Math.pow(SCROLL_STEP, bounded));
    }

    /**
     * Eases the live magnification toward the scrolled target. Zoom is multiplicative, so the
     * blend happens on a log scale and a step from 2x to 4x feels like one from 4x to 8x.
     */
    public static double advanceAmount(
            double current,
            double target,
            double speed,
            double deltaSeconds) {

        double from = clampLiveAmount(current);
        double to = clampLiveAmount(target);
        double dt = Double.isFinite(deltaSeconds)
                ? Math.max(0.0D, Math.min(MAX_FRAME_SECONDS, deltaSeconds))
                : 0.0D;
        if (dt <= 0.0D) {
            return from;
        }
        double alpha = 1.0D - Math.exp(-clampSpeed(speed) * dt);
        double logFrom = Math.log(from);
        double logTo = Math.log(to);
        double next = logFrom + (logTo - logFrom) * alpha;
        if (Math.abs(logTo - next) <= SNAP_EPSILON) {
            return to;
        }
        return clampLiveAmount(Math.exp(next));
    }

    public static double clampSpeed(double value) {
        return clamp(
                value,
                MIN_SPEED,
                MAX_SPEED,
                DEFAULT_SPEED);
    }

    public static double advanceProgress(
            double current,
            boolean held,
            double speed,
            double deltaSeconds) {

        double progress = clamp01(current);
        double target = held ? 1.0D : 0.0D;

        double dt =
                Double.isFinite(deltaSeconds)
                        ? Math.max(
                                0.0D,
                                Math.min(
                                        MAX_FRAME_SECONDS,
                                        deltaSeconds))
                        : 0.0D;

        if (dt <= 0.0D) {
            return progress;
        }

        double alpha =
                1.0D
                        - Math.exp(
                                -clampSpeed(speed) * dt);

        double next =
                progress
                        + (target - progress)
                        * alpha;

        if (Math.abs(target - next) <= SNAP_EPSILON) {
            return target;
        }

        return clamp01(next);
    }

    public static float applyToFov(
            float vanillaFov,
            double amount,
            double progress) {

        if (!Float.isFinite(vanillaFov)
                || vanillaFov <= 0.0F) {

            return vanillaFov;
        }

        double base = vanillaFov;

        double target =
                Math.min(
                        base,
                        Math.max(
                                MIN_EFFECTIVE_FOV,
                                base / clampLiveAmount(amount)));

        double p = clamp01(progress);

        double eased =
                p * p * (3.0D - 2.0D * p);

        return (float) (
                base
                        + (target - base)
                        * eased);
    }

    public static int applyToFov(
            int vanillaFov,
            double amount,
            double progress) {

        return Math.round(
                applyToFov(
                        (float) vanillaFov,
                        amount,
                        progress));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }

        return Math.max(
                0.0D,
                Math.min(
                        1.0D,
                        value));
    }

    private static double clamp(
            double value,
            double min,
            double max,
            double fallback) {

        if (!Double.isFinite(value)) {
            return fallback;
        }

        return Math.max(
                min,
                Math.min(
                        max,
                        value));
    }
}