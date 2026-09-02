package fi.rotclient;

/** Small layout rule that keeps the primary Slayer HUD compact without truncation. */
public final class SlayerHudTextPolicy {
    public static final float MIN_SCALE = 0.70F;

    private SlayerHudTextPolicy() {
    }

    public static float scaleFor(int measuredTextWidth, int preferredTextWidth) {
        if (measuredTextWidth <= 0 || preferredTextWidth <= 0
                || measuredTextWidth <= preferredTextWidth) {
            return 1.0F;
        }
        return Math.max(MIN_SCALE, Math.min(1.0F,
                (float) preferredTextWidth / (float) measuredTextWidth));
    }
}
