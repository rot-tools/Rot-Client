package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerIrrelevantMobsPolicyTest {
    @Test
    void keepsTheExpectedQuestMobFamilyVisible() {
        SlayerIrrelevantMobsPolicy.Options options = new SlayerIrrelevantMobsPolicy.Options(true, 40);

        assertFalse(SlayerIrrelevantMobsPolicy.shouldFade(
                options, SlayerPolicy.SlayerType.REVENANT, false, false,
                SlayerIrrelevantMobsPolicy.MobKind.ZOMBIE));
        assertTrue(SlayerIrrelevantMobsPolicy.shouldFade(
                options, SlayerPolicy.SlayerType.REVENANT, false, false,
                SlayerIrrelevantMobsPolicy.MobKind.SPIDER));
    }

    @Test
    void neverFadesPlayersTrackedSlayerEntitiesOrUnverifiedVampireMobs() {
        SlayerIrrelevantMobsPolicy.Options options = new SlayerIrrelevantMobsPolicy.Options(true, 40);

        assertFalse(SlayerIrrelevantMobsPolicy.shouldFade(
                options, SlayerPolicy.SlayerType.VOIDGLOOM, true, false,
                SlayerIrrelevantMobsPolicy.MobKind.ZOMBIE));
        assertFalse(SlayerIrrelevantMobsPolicy.shouldFade(
                options, SlayerPolicy.SlayerType.VOIDGLOOM, false, true,
                SlayerIrrelevantMobsPolicy.MobKind.ZOMBIE));
        assertFalse(SlayerIrrelevantMobsPolicy.shouldFade(
                options, SlayerPolicy.SlayerType.VAMPIRE, false, false,
                SlayerIrrelevantMobsPolicy.MobKind.OTHER));
    }
}
