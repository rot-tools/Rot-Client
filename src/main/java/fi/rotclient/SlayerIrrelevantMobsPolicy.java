package fi.rotclient;

/** Pure, conservative classification for the local Slayer irrelevant-mob fade. */
public final class SlayerIrrelevantMobsPolicy {
    public static final int MIN_STRENGTH = 10;
    public static final int MAX_STRENGTH = 80;

    public enum MobKind {
        ZOMBIE,
        SPIDER,
        WOLF,
        ENDERMAN,
        BLAZE,
        OTHER
    }

    public record Options(boolean enabled, int strengthPercent) {
        public Options {
            strengthPercent = clampStrength(strengthPercent);
        }
    }

    private SlayerIrrelevantMobsPolicy() {
    }

    /**
     * Fades only known non-target mob categories. Vampire is intentionally
     * excluded until its server-side mob identities are verified at runtime.
     */
    public static boolean shouldFade(
            Options options,
            SlayerPolicy.SlayerType activeType,
            boolean targetIsTrackedSlayerEntity,
            boolean targetIsPlayer,
            MobKind targetKind) {
        if (options == null || !options.enabled() || activeType == null
                || targetIsTrackedSlayerEntity || targetIsPlayer
                || activeType == SlayerPolicy.SlayerType.VAMPIRE) {
            return false;
        }
        return !isRelevant(activeType, targetKind);
    }

    public static boolean isRelevant(SlayerPolicy.SlayerType type, MobKind kind) {
        if (type == null || kind == null) {
            return false;
        }
        return switch (type) {
            case REVENANT -> kind == MobKind.ZOMBIE;
            case TARANTULA -> kind == MobKind.SPIDER;
            case SVEN -> kind == MobKind.WOLF;
            case VOIDGLOOM -> kind == MobKind.ENDERMAN;
            case INFERNO -> kind == MobKind.BLAZE;
            case VAMPIRE -> true;
        };
    }

    public static int opacityPercent(Options options) {
        return 100 - (options == null ? MIN_STRENGTH : options.strengthPercent());
    }

    public static int clampStrength(int value) {
        return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, value));
    }
}
