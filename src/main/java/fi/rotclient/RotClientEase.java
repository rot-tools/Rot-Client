package fi.rotclient;

/**
 * Frame-rate independent easing used by dashboard scroll and accordion motion.
 *
 * <p>Same model as native UI toolkits and OneConfig-style clients: exponential
 * decay toward a target using real frame dt so 60 Hz and 144 Hz look like the
 * same animation, not a faster/slower 0.28 lerp.
 */
final class RotClientEase {
    static final double SCROLL_STIFFNESS = 14.0D;
    static final double EXPAND_STIFFNESS = 16.0D;
    static final double TOGGLE_STIFFNESS = 18.0D;
    static final double SNAP_EPSILON = 5.0E-4D;

    private RotClientEase() {
    }

    static double clamp01(double value) {
        if (value <= 0.0D) {
            return 0.0D;
        }
        if (value >= 1.0D) {
            return 1.0D;
        }
        return value;
    }

    static double smoothstep(double value) {
        double t = clamp01(value);
        return t * t * (3.0D - 2.0D * t);
    }

    /**
     * Exponential approach: visually smooth, no overshoot, settles exactly.
     */
    static double expToward(double current, double target, double dtSeconds, double stiffness) {
        if (dtSeconds <= 0.0D) {
            return current;
        }
        double k = Math.max(0.1D, stiffness);
        double next = current + (target - current) * (1.0D - Math.exp(-k * dtSeconds));
        if (Math.abs(target - next) < SNAP_EPSILON) {
            return target;
        }
        return next;
    }

    static double frameDeltaSeconds(long lastNs, long nowNs) {
        if (lastNs <= 0L || nowNs <= lastNs) {
            return 1.0D / 60.0D;
        }
        return Math.min(0.05D, (nowNs - lastNs) / 1_000_000_000.0D);
    }

    /**
     * Minecraft {@code DeltaTracker.getRealtimeDeltaTicks()} is frame time in
     * ticks (20 ticks = 1 second). Caps hitch frames at 50 ms.
     */
    static double secondsFromRealtimeTicks(float ticks) {
        if (!Float.isFinite(ticks) || ticks <= 0.0F) {
            return 0.0D;
        }
        return Math.min(0.05D, ticks / 20.0D);
    }

    static double shownHeight(int fullHeight, double amount) {
        if (fullHeight <= 0) {
            return 0.0D;
        }
        return fullHeight * smoothstep(clamp01(amount));
    }

    static int shownPixels(int fullHeight, double amount) {
        return (int) Math.round(shownHeight(fullHeight, amount));
    }
}
