package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AutoClickerPolicyTest {
    @Test
    void clampCpsUsesConfiguredRange() {
        assertEquals(3.0F, AutoClickerPolicy.clampCps(1.0F));
        assertEquals(20.0F, AutoClickerPolicy.clampCps(99.0F));
        assertEquals(5.0F, AutoClickerPolicy.clampCps(5.0F));
    }

    @Test
    void dungeonBreakerBlocksAutoClickWithExactId() {
        assertFalse(AutoClickerPolicy.shouldBlockForDungeonBreaker(
                true, "Hyperion"));
        assertTrue(AutoClickerPolicy.shouldBlockForDungeonBreaker(
                true, "DUNGEONBREAKER"));
        assertFalse(AutoClickerPolicy.shouldBlockForDungeonBreaker(
                true, "DUNGEON_BREAKER"));
        assertFalse(AutoClickerPolicy.shouldBlockForDungeonBreaker(
                true, "Dungeon Breaker"));
        assertFalse(AutoClickerPolicy.shouldBlockForDungeonBreaker(
                false, "DUNGEONBREAKER"));
    }

    @Test
    void whitelistOnlyIsCaseSensitive() {
        List<String> list = List.of("terminator_bow");
        assertFalse(AutoClickerPolicy.isWhitelisted(
                true, list, "Hyperion"));
        assertTrue(AutoClickerPolicy.isWhitelisted(
                true, list, "terminator_bow"));
        assertFalse(AutoClickerPolicy.isWhitelisted(
                true, list, "Terminator_Bow"));
        assertTrue(AutoClickerPolicy.isWhitelisted(
                false, list, "Anything"));
    }

    @Test
    void leftClickSkipsBlocksWhenBreakingDisabled() {
        assertFalse(AutoClickerPolicy.shouldAutoLeftClick(
                true,
                true,
                true,
                false,
                false,
                List.of(),
                "Sword",
                true,
                false));
        assertTrue(AutoClickerPolicy.shouldAutoLeftClick(
                true,
                true,
                true,
                false,
                false,
                List.of(),
                "Sword",
                true,
                true));
    }

    @Test
    void blockBreakingUsesAContinuousAttackOnlyForAnAllowedTargetBlock() {
        assertTrue(AutoClickerPolicy.shouldHoldBlockBreaking(true, true, true));
        assertFalse(AutoClickerPolicy.shouldHoldBlockBreaking(false, true, true));
        assertFalse(AutoClickerPolicy.shouldHoldBlockBreaking(true, false, true));
        assertFalse(AutoClickerPolicy.shouldHoldBlockBreaking(true, true, false));
    }

    @Test
    void discreteCombatPulsesAlwaysReleaseTheMapping() {
        assertFalse(AutoClickerPolicy.mappingHeldAfterDiscretePulse());
    }

    @Test
    void terminatorOnlyLeftClicksWhenHoldingExactIdAndRightMouse() {
        assertFalse(AutoClickerPolicy.shouldTerminatorLeftClick(
                true, true, false, "TERMINATOR"));
        assertTrue(AutoClickerPolicy.shouldTerminatorLeftClick(
                true, true, true, "TERMINATOR"));
        assertFalse(AutoClickerPolicy.shouldTerminatorLeftClick(
                true, true, true, "Terminator"));
        assertFalse(AutoClickerPolicy.shouldTerminatorLeftClick(
                true, true, true, "HYPERION"));
        assertFalse(AutoClickerPolicy.shouldTerminatorLeftClick(
                true, false, true, "TERMINATOR"));
    }

    @Test
    void terminatorOnlyRightClickPathDoesNotFire() {
        assertFalse(AutoClickerPolicy.shouldAutoRightClick(
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                List.of(),
                "TERMINATOR"));
    }

    @Test
    void separateCpsWhenBothSidesEnabled() {
        assertEquals(7.0F, AutoClickerPolicy.resolveLeftCps(
                true, true, 5.0F, 7.0F, 9.0F));
        assertEquals(9.0F, AutoClickerPolicy.resolveRightCps(
                true, true, 5.0F, 7.0F, 9.0F));
        assertEquals(5.0F, AutoClickerPolicy.resolveLeftCps(
                true, false, 5.0F, 7.0F, 9.0F));
    }

    @Test
    void midpointRandomPreservesMetronomeRate() {
        assertEquals(
                5.0D / 20.0D,
                AutoClickerPolicy.tickAccumulation(0.0D, 5.0F, 0.5D),
                1.0e-12D);
        assertEquals(
                AutoClickerPolicy.tickAccumulation(0.0D, 5.0F),
                AutoClickerPolicy.tickAccumulation(0.0D, 5.0F, 0.5D),
                1.0e-12D);
    }

    @Test
    void jitteredTicksAverageConfiguredCps() {
        assertTrue(
                AutoClickerPolicy.tickAccumulation(0.0D, 5.0F, 0.0D)
                        < AutoClickerPolicy.tickAccumulation(0.0D, 5.0F, 0.5D));
        assertTrue(
                AutoClickerPolicy.tickAccumulation(0.0D, 5.0F, 0.5D)
                        < AutoClickerPolicy.tickAccumulation(0.0D, 5.0F, 1.0D));

        java.util.Random rng = new java.util.Random(1L);
        double acc = 0.0D;
        int clicks = 0;
        int ticks = 2000;
        for (int i = 0; i < ticks; i++) {
            acc = AutoClickerPolicy.tickAccumulation(acc, 5.0F, rng.nextDouble());
            int produced = AutoClickerPolicy.consumeClicks(acc);
            acc = AutoClickerPolicy.remainderAfterClicks(acc, produced);
            clicks += produced;
        }
        double seconds = ticks / 20.0D;
        double meanCps = clicks / seconds;
        assertEquals(5.0D, meanCps, 0.35D);
        assertTrue(clicks > 0);
    }

    @Test
    void terminatorAndDungeonBreakerUseExactSkyBlockId() {
        String uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        assertFalse(AutoClickerPolicy.isTerminatorItem(uuid));
        assertTrue(AutoClickerPolicy.isTerminatorItem("TERMINATOR"));
        assertFalse(AutoClickerPolicy.isTerminatorItem("TERMINATOR Terminator"));
        assertTrue(AutoClickerPolicy.isDungeonBreakerItem("DUNGEONBREAKER"));
        assertFalse(AutoClickerPolicy.isDungeonBreakerItem("Dungeon Breaker"));
        assertFalse(AutoClickerPolicy.shouldAutoRightClick(
                true, true, true, false, true, true, false, List.of(), uuid, uuid));
        assertTrue(AutoClickerPolicy.shouldBlockForDungeonBreaker(true, "DUNGEONBREAKER"));
        assertFalse(AutoClickerPolicy.shouldAutoLeftClick(
                true, true, true, true, false, List.of(), uuid, "DUNGEONBREAKER", false, true));
        assertTrue(AutoClickerPolicy.shouldAutoLeftClick(
                true, true, true, true, false, List.of(), uuid, "HYPERION", false, true));
    }

    @Test
    void whitelistHelpersKeepExactIdentity() {
        List<String> left = new ArrayList<>();
        assertTrue(AutoClickerWhitelist.add(left, "Terminator"));
        assertFalse(AutoClickerWhitelist.add(left, "Terminator"));
        assertTrue(AutoClickerWhitelist.add(left, "terminator"));
        assertEquals(List.of("Terminator", "terminator"), AutoClickerWhitelist.snapshot(left));
        assertTrue(AutoClickerWhitelist.remove(left, "Terminator"));
        assertEquals(List.of("terminator"), left);
    }
}
