package fi.rotclient;

/**
 * Pure decision rules for the local Active Boss Transparency renderer.
 *
 * <p>The policy deliberately knows nothing about Minecraft entities. The
 * client bridge supplies only the facts needed to decide whether a rendered
 * target belongs in the background while a verified Slayer boss fight is
 * active.</p>
 */
public final class SlayerTransparencyPolicy {
    public static final int MIN_STRENGTH = 15;
    public static final int MAX_STRENGTH = 70;

    public record Options(boolean enabled, int strengthPercent, boolean includeOtherPlayers) {
        public Options {
            strengthPercent = clampStrength(strengthPercent);
        }
    }

    private SlayerTransparencyPolicy() {
    }

    /** Returns whether one background target should be rendered translucently. */
    public static boolean shouldFade(
            Options options,
            boolean verifiedFightActive,
            boolean targetIsTrackedBoss,
            boolean targetIsLocalPlayer,
            boolean targetIsOtherPlayer) {
        if (options == null || !options.enabled() || !verifiedFightActive || targetIsTrackedBoss) {
            return false;
        }
        if (targetIsLocalPlayer) {
            return false;
        }
        return !targetIsOtherPlayer || options.includeOtherPlayers();
    }

    /** Converts transparency strength to model opacity for the translucent render path. */
    public static int opacityPercent(Options options) {
        int strength = options == null ? MIN_STRENGTH : options.strengthPercent();
        return 100 - clampStrength(strength);
    }

    public static int clampStrength(int value) {
        return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, value));
    }
}
