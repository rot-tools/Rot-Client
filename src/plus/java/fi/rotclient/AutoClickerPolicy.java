package fi.rotclient;

import java.util.Collection;

/**
 * Pure auto-clicker decisions: CPS accumulation, whitelist, dungeon
 * breaker guard, terminator-only left click, and block-breaking gate.
 */
public final class AutoClickerPolicy {
    public static final float MIN_CPS = 3.0F;
    // The current slider permits 3–20 CPS.
    public static final float MAX_CPS = 20.0F;
    public static final float DEFAULT_CPS = 5.0F;
    public static final double JITTER_MIN_FACTOR = 0.8D;
    public static final double JITTER_MAX_FACTOR = 1.2D;

    private AutoClickerPolicy() {
    }

    public static float clampCps(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_CPS;
        }
        return Math.max(MIN_CPS, Math.min(MAX_CPS, value));
    }

    /**
     * Uniform 0.8–1.2 factor so automated CPS / ping windows are not a
     * perfect metronome while still averaging the configured value.
     */
    public static double humanizeFactor(double random01) {
        double sample = random01;
        if (!Double.isFinite(sample)) {
            sample = 0.5D;
        }
        sample = Math.max(0.0D, Math.min(1.0D, sample));
        return JITTER_MIN_FACTOR
                + sample * (JITTER_MAX_FACTOR - JITTER_MIN_FACTOR);
    }

    public static double tickAccumulation(double accumulator, float cps) {
        return tickAccumulation(accumulator, cps, 0.5D);
    }

    public static double tickAccumulation(
            double accumulator, float cps, double random01) {
        return accumulator
                + clampCps(cps) / 20.0D * humanizeFactor(random01);
    }

    public static int consumeClicks(double accumulator) {
        int clicks = 0;
        while (accumulator >= 1.0D) {
            accumulator -= 1.0D;
            clicks++;
        }
        return clicks;
    }

    public static double remainderAfterClicks(double accumulator, int clicks) {
        return accumulator - clicks;
    }

    public static final String TERMINATOR_ID = "TERMINATOR";
    public static final String DUNGEON_BREAKER_ID = "DUNGEONBREAKER";

    public static boolean isWhitelisted(
            boolean whitelistOnly,
            Collection<String> whitelist,
            String heldIdentity) {
        if (!whitelistOnly) {
            return true;
        }
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        if (heldIdentity == null || heldIdentity.isBlank()) {
            return false;
        }
        for (String entry : whitelist) {
            if (entry != null && entry.equals(heldIdentity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The SkyBlock NBT {@code id} is compared exactly (case-sensitive, no
     * substring) against {@code DUNGEONBREAKER}.
     */
    public static boolean isDungeonBreakerItem(String skyBlockId) {
        return DUNGEON_BREAKER_ID.equals(skyBlockId);
    }

    /**
     * The SkyBlock NBT {@code id} is compared exactly (case-sensitive, no
     * substring) against {@code TERMINATOR}.
     */
    public static boolean isTerminatorItem(String skyBlockId) {
        return TERMINATOR_ID.equals(skyBlockId);
    }

    /**
     * Terminator-only mode replaces the normal left/right paths: hold
     * Terminator + physical right-click, fire left clicks.
     */
    public static boolean shouldTerminatorLeftClick(
            boolean moduleEnabled,
            boolean terminatorOnly,
            boolean physicalRightHeld,
            String skyBlockId) {
        if (!moduleEnabled || !terminatorOnly) {
            return false;
        }
        return physicalRightHeld && isTerminatorItem(skyBlockId);
    }

    public static boolean shouldBlockForDungeonBreaker(
            boolean blockBreaker,
            String heldIdentity) {
        return blockBreaker && isDungeonBreakerItem(heldIdentity);
    }

    public static boolean shouldAutoLeftClick(
            boolean moduleEnabled,
            boolean enableLeftClick,
            boolean activationHeld,
            boolean blockBreaker,
            boolean whitelistOnly,
            Collection<String> leftWhitelist,
            String whitelistIdentity,
            boolean targetingBreakableBlock,
            boolean allowBreaking) {
        return shouldAutoLeftClick(
                moduleEnabled,
                enableLeftClick,
                activationHeld,
                blockBreaker,
                whitelistOnly,
                leftWhitelist,
                whitelistIdentity,
                whitelistIdentity,
                targetingBreakableBlock,
                allowBreaking);
    }

    public static boolean shouldAutoLeftClick(
            boolean moduleEnabled,
            boolean enableLeftClick,
            boolean activationHeld,
            boolean blockBreaker,
            boolean whitelistOnly,
            Collection<String> leftWhitelist,
            String whitelistIdentity,
            String kindIdentity,
            boolean targetingBreakableBlock,
            boolean allowBreaking) {
        if (!moduleEnabled || !enableLeftClick || !activationHeld) {
            return false;
        }
        if (shouldBlockForDungeonBreaker(blockBreaker, kindIdentity)) {
            return false;
        }
        if (!isWhitelisted(whitelistOnly, leftWhitelist, whitelistIdentity)) {
            return false;
        }
        if (targetingBreakableBlock && !allowBreaking) {
            return false;
        }
        return true;
    }

    /**
     * Blocks need a held attack input rather than a sequence of discrete
     * clicks. This is intentionally separate from the normal combat click
     * path so a completed block can immediately begin the next one.
     */
    public static boolean shouldHoldBlockBreaking(
            boolean leftAutoClickActive,
            boolean targetingBreakableBlock,
            boolean allowBreaking) {
        return leftAutoClickActive && targetingBreakableBlock && allowBreaking;
    }

    /**
     * Discrete combat/use pulses must release the KeyMapping afterwards, even
     * when the physical mouse button is still down. Each pulse always
     * calls {@code set(key, false)} after the click.
     * Leaving attack held makes vanilla {@code continueAttack(true)} keep
     * {@code missTime} from resetting, so later {@code startAttack} calls from
     * {@code KeyMapping.click()} are ignored and hits drop to the hold rate.
     * Block mining uses {@link #shouldHoldBlockBreaking} instead of a pulse.
     */
    public static boolean mappingHeldAfterDiscretePulse() {
        return false;
    }

    public static boolean shouldAutoRightClick(
            boolean moduleEnabled,
            boolean enableRightClick,
            boolean activationHeld,
            boolean blockBreaker,
            boolean terminatorOnly,
            boolean physicalRightHeld,
            boolean whitelistOnly,
            Collection<String> rightWhitelist,
            String whitelistIdentity) {
        return shouldAutoRightClick(
                moduleEnabled,
                enableRightClick,
                activationHeld,
                blockBreaker,
                terminatorOnly,
                physicalRightHeld,
                whitelistOnly,
                rightWhitelist,
                whitelistIdentity,
                whitelistIdentity);
    }

    public static boolean shouldAutoRightClick(
            boolean moduleEnabled,
            boolean enableRightClick,
            boolean activationHeld,
            boolean blockBreaker,
            boolean terminatorOnly,
            boolean physicalRightHeld,
            boolean whitelistOnly,
            Collection<String> rightWhitelist,
            String whitelistIdentity,
            String kindIdentity) {
        if (!moduleEnabled || !enableRightClick || !activationHeld) {
            return false;
        }
        if (shouldBlockForDungeonBreaker(blockBreaker, kindIdentity)) {
            return false;
        }
        if (terminatorOnly) {
            return false;
        }
        if (!isWhitelisted(whitelistOnly, rightWhitelist, whitelistIdentity)) {
            return false;
        }
        return true;
    }

    public static float resolveLeftCps(
            boolean enableLeftClick,
            boolean enableRightClick,
            float sharedCps,
            float leftCps,
            float rightCps) {
        if (!enableLeftClick) {
            return 0.0F;
        }
        if (enableLeftClick && enableRightClick) {
            return clampCps(leftCps);
        }
        return clampCps(sharedCps > 0.0F ? sharedCps : leftCps);
    }

    public static float resolveRightCps(
            boolean enableLeftClick,
            boolean enableRightClick,
            float sharedCps,
            float leftCps,
            float rightCps) {
        if (!enableRightClick) {
            return 0.0F;
        }
        if (enableLeftClick && enableRightClick) {
            return clampCps(rightCps);
        }
        return clampCps(sharedCps > 0.0F ? sharedCps : rightCps);
    }
}
