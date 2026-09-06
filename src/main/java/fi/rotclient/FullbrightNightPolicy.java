package fi.rotclient;

/**
 * Fullbright and Always Night share one catalog card. Force both off means
 * the two modes are exclusive. Night dusk vs snap is a session decision;
 * saved flags are not SkyBlock-gated.
 */
public final class FullbrightNightPolicy {
    public static final String MODULE_ID = "qol.fullbright";
    public static final String USE_FULLBRIGHT = "qol.fullbright.use_fullbright";
    public static final String ALWAYS_NIGHT = "qol.fullbright.always_night";
    public static final String FORCE_BOTH = "qol.fullbright.force_both";

    /** Vanilla midnight sun is opposite the moon (radians on SkyRenderState). */
    public static final float SUN_ANGLE_MIDNIGHT = (float) Math.PI;
    /** Slightly off zenith so the moon sits in a readable part of the sky. */
    public static final float MOON_ANGLE_PARK = 0.32F;
    public static final float STAR_ANGLE_PARK = MOON_ANGLE_PARK;
    public static final float STAR_BRIGHTNESS_NIGHT = 0.55F;
    /** Dark overworld night disc. RGB, no alpha. */
    public static final int SKY_COLOR_NIGHT = 0x0A1228;
    public static final int SUNRISE_COLOR_NIGHT = 0;
    /** Outdoor sky-light scale that reads as vanilla night, not pitch black. */
    public static final float NIGHT_SKY_FACTOR = 0.14F;
    public static final float NIGHT_SKY_LIGHT_R = 0.42F;
    public static final float NIGHT_SKY_LIGHT_G = 0.48F;
    public static final float NIGHT_SKY_LIGHT_B = 0.72F;
    public static final float NIGHT_AMBIENT_R = 0.04F;
    public static final float NIGHT_AMBIENT_G = 0.05F;
    public static final float NIGHT_AMBIENT_B = 0.09F;
    public static final long DUSK_DURATION_MS = 8_000L;

    public enum NightApply {
        OFF,
        ANIMATE,
        SNAP,
        LATCHED
    }

    public record LightingState(boolean fullbright, boolean alwaysNight, boolean forceBoth) {
        public static LightingState defaults() {
            return new LightingState(false, false, false);
        }

        public boolean cardOn() {
            return fullbright || alwaysNight;
        }
    }

    public record SkyPose(
            float sunAngle,
            float moonAngle,
            float starAngle,
            float starBrightness,
            int skyColor,
            int sunriseColor,
            float skyFactor) {
        public static SkyPose parkedNight(float rainBrightness) {
            return new SkyPose(
                    SUN_ANGLE_MIDNIGHT,
                    MOON_ANGLE_PARK,
                    STAR_ANGLE_PARK,
                    STAR_BRIGHTNESS_NIGHT * clamp01(rainBrightness),
                    SKY_COLOR_NIGHT,
                    SUNRISE_COLOR_NIGHT,
                    NIGHT_SKY_FACTOR);
        }

        /** Fallback when dusk starts before the first sky extract. */
        public static SkyPose daytimeStart() {
            return new SkyPose(
                    0.0F,
                    SUN_ANGLE_MIDNIGHT,
                    0.0F,
                    0.0F,
                    0x78A7FF,
                    0,
                    1.0F);
        }
    }

    private FullbrightNightPolicy() {
    }

    public static boolean isLightingSetting(String settingId) {
        if (settingId == null) {
            return false;
        }
        return USE_FULLBRIGHT.equals(settingId)
                || ALWAYS_NIGHT.equals(settingId)
                || FORCE_BOTH.equals(settingId);
    }

    public static LightingState enableCard(LightingState current) {
        LightingState state = current == null ? LightingState.defaults() : current;
        if (state.cardOn()) {
            return state;
        }
        return new LightingState(true, false, state.forceBoth());
    }

    public static LightingState disableCard(LightingState current) {
        LightingState state = current == null ? LightingState.defaults() : current;
        return new LightingState(false, false, state.forceBoth());
    }

    public static LightingState setFullbright(LightingState current, boolean enabled) {
        LightingState state = current == null ? LightingState.defaults() : current;
        if (!enabled) {
            return new LightingState(false, state.alwaysNight(), state.forceBoth());
        }
        boolean night = state.forceBoth() && state.alwaysNight();
        return new LightingState(true, night, state.forceBoth());
    }

    public static LightingState setAlwaysNight(LightingState current, boolean enabled) {
        LightingState state = current == null ? LightingState.defaults() : current;
        if (!enabled) {
            return new LightingState(state.fullbright(), false, state.forceBoth());
        }
        boolean fullbright = state.forceBoth() && state.fullbright();
        return new LightingState(fullbright, true, state.forceBoth());
    }

    public static LightingState setForceBoth(LightingState current, boolean enabled) {
        LightingState state = current == null ? LightingState.defaults() : current;
        if (enabled) {
            return new LightingState(state.fullbright(), state.alwaysNight(), true);
        }
        if (state.fullbright() && state.alwaysNight()) {
            return new LightingState(true, false, false);
        }
        return new LightingState(state.fullbright(), state.alwaysNight(), false);
    }

    public static boolean applyNightSky(boolean alwaysNight) {
        return alwaysNight;
    }

    public static boolean applyFullbrightLightmap(boolean fullbright) {
        return fullbright;
    }

    public static boolean applyNightLightmap(boolean fullbright, boolean alwaysNight) {
        return alwaysNight && !fullbright;
    }

    /**
     * {@code sameWorld} is the current ClientLevel identity vs the last one
     * this session already applied. A new level while Always Night is already
     * on always snaps so hub/server transfers do not replay dusk.
     */
    public static NightApply decideNightApply(
            boolean alwaysNightEnabled,
            boolean worldLoaded,
            boolean sameWorld,
            boolean duskRunning,
            boolean latched) {
        if (!alwaysNightEnabled || !worldLoaded) {
            return NightApply.OFF;
        }
        if (!sameWorld) {
            return NightApply.SNAP;
        }
        if (latched) {
            return NightApply.LATCHED;
        }
        if (duskRunning) {
            return NightApply.ANIMATE;
        }
        return NightApply.SNAP;
    }

    public static float duskProgress(long nowMs, long startMs, long durationMs) {
        if (durationMs <= 0L) {
            return 1.0F;
        }
        return clamp01((nowMs - startMs) / (float) durationMs);
    }

    public static float smoothstep(float t) {
        float x = clamp01(t);
        return x * x * (3.0F - 2.0F * x);
    }

    public static SkyPose lerpPose(SkyPose start, SkyPose target, float t) {
        SkyPose from = start == null ? target : start;
        SkyPose to = target == null ? from : target;
        if (from == null) {
            return SkyPose.parkedNight(1.0F);
        }
        float u = clamp01(t);
        return new SkyPose(
                lerp(from.sunAngle(), to.sunAngle(), u),
                lerp(from.moonAngle(), to.moonAngle(), u),
                lerp(from.starAngle(), to.starAngle(), u),
                lerp(from.starBrightness(), to.starBrightness(), u),
                lerpRgb(from.skyColor(), to.skyColor(), u),
                lerpArgb(from.sunriseColor(), to.sunriseColor(), u),
                lerp(from.skyFactor(), to.skyFactor(), u));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int lerpRgb(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int lerpArgb(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int alpha = Math.round(aa + (ba - aa) * t);
        return (alpha << 24) | lerpRgb(a, b, t);
    }

    static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }
}
