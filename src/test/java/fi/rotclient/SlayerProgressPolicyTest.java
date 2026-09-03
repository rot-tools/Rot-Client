package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SlayerProgressPolicyTest {
    @Test
    void parsesFormattedSkyBlockCombatXpFraction() {
        SlayerProgressPolicy.Progress progress = SlayerProgressPolicy.parse(
                "§cSlayer Quest §7| §aCombat XP: 2,400/3,000")
                .orElseThrow();
        assertEquals(2_400L, progress.earnedXp());
        assertEquals(3_000L, progress.requiredXp());
        assertEquals(80, progress.percent());
        assertEquals(600L, progress.remainingXp());
    }

    @Test
    void rejectsUnrelatedActionBarText() {
        assertTrue(SlayerProgressPolicy.parse("Mana 1,200/1,500").isEmpty());
    }

    @Test
    void parsesShortHandSkyBlockXpAmounts() {
        SlayerProgressPolicy.Progress progress = SlayerProgressPolicy.parse(
                "Slayer Quest | Combat XP: 2.4k/3k").orElseThrow();
        assertEquals(2_400L, progress.earnedXp());
        assertEquals(3_000L, progress.requiredXp());
    }

    @Test
    void parsesFractionBeforeCombatXpLabel() {
        SlayerProgressPolicy.Progress progress = SlayerProgressPolicy.parse(
                "Slayer Quest | 2.4k/3k Combat XP").orElseThrow();
        assertEquals(2_400L, progress.earnedXp());
        assertEquals(3_000L, progress.requiredXp());
    }

    @Test
    void parsesTheVanillaSlayerQuestFractionWithoutRequiringCombatXpWords() {
        SlayerProgressPolicy.Progress progress = SlayerProgressPolicy.parse(
                "Slayer Quest | (2.4k/3k) | Kill the boss!").orElseThrow();

        assertEquals(2_400L, progress.earnedXp());
        assertEquals(3_000L, progress.requiredXp());
    }

    @Test
    void rejectsCombatSkillTotalsAndAcceptsSidebarQuestFractions() {
        assertTrue(SlayerProgressPolicy.parse(
                "3,898/3,898 +408.2 Combat (19,896,487/0) 1,817/1,869 115/115").isEmpty());
        assertTrue(SlayerProgressPolicy.parse("(19,896,487/0)").isEmpty());
        assertTrue(SlayerProgressPolicy.parse("3,898/3,898", true).isEmpty());
        SlayerProgressPolicy.Progress sidebar = SlayerProgressPolicy.parse("2,403/3,000", true)
                .orElseThrow();
        assertEquals(2_403L, sidebar.earnedXp());
        assertEquals(3_000L, sidebar.requiredXp());
        assertTrue(SlayerProgressPolicy.parse("2,403/3,000", false).isEmpty());
    }

    @Test
    void rejectsMalformedAndZeroTotalFractions() {
        assertTrue(SlayerProgressPolicy.parse("Combat XP: nope/3k").isEmpty());
        assertTrue(SlayerProgressPolicy.parse("Combat XP: 1k/0").isEmpty());
        assertTrue(SlayerProgressPolicy.parse("2.4k/3k Mana").isEmpty());
    }

    @Test
    void supportsOverkillWithoutNegativeRemainingXp() {
        SlayerProgressPolicy.Progress progress = SlayerProgressPolicy.parse(
                "Combat XP: 4k/3k").orElseThrow();
        assertEquals(100, progress.percent());
        assertEquals(0L, progress.remainingXp());
    }

    @Test
    void inactiveQuestNeverAlertsAndResetsTheLatch() {
        SlayerProgressPolicy.ThresholdState state = new SlayerProgressPolicy.ThresholdState();
        SlayerProgressPolicy.Progress above = new SlayerProgressPolicy.Progress(2_403, 3_000);
        assertTrue(!state.observe(false, above, 80, false));
        assertTrue(state.observe(true, above, 80, false));
    }

    @Test
    void usesStrictThresholdAndAlertsOnInitialValueAboveIt() {
        SlayerProgressPolicy.ThresholdState state = new SlayerProgressPolicy.ThresholdState();
        assertTrue(!state.observe(true, new SlayerProgressPolicy.Progress(2_400, 3_000), 80, false));
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_403, 3_000), 80, false));
    }

    @Test
    void nonRepeatingWarningRearmsOnlyAfterProgressDropsToThresholdOrBelow() {
        SlayerProgressPolicy.ThresholdState state = new SlayerProgressPolicy.ThresholdState();
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_403, 3_000), 80, false));
        assertTrue(!state.observe(true, new SlayerProgressPolicy.Progress(2_700, 3_000), 80, false));
        assertTrue(!state.observe(true, new SlayerProgressPolicy.Progress(2_400, 3_000), 80, false));
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_401, 3_000), 80, false));
    }

    @Test
    void repeatingWarningOnlyAcceptsAHigherCompletion() {
        SlayerProgressPolicy.ThresholdState state = new SlayerProgressPolicy.ThresholdState();
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_403, 3_000), 80, true));
        assertTrue(!state.observe(true, new SlayerProgressPolicy.Progress(2_403, 3_000), 80, true));
        assertTrue(!state.observe(true, new SlayerProgressPolicy.Progress(2_402, 3_000), 80, true));
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_500, 3_000), 80, true));
    }

    @Test
    void lifecycleOrTierResetRearmsTheWarning() {
        SlayerProgressPolicy.ThresholdState state = new SlayerProgressPolicy.ThresholdState();
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_403, 3_000), 80, false));
        assertTrue(!state.observe(false, null, 80, false));
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(2_403, 3_000), 80, false));
        assertTrue(state.observe(true, new SlayerProgressPolicy.Progress(3_201, 4_000), 80, false));
    }

    @Test
    void clampsWarningThresholdToSupportedRange() {
        assertEquals(50, SlayerProgressPolicy.clampThreshold(1));
        assertEquals(80, SlayerProgressPolicy.clampThreshold(80));
        assertEquals(90, SlayerProgressPolicy.clampThreshold(99));
    }
}
