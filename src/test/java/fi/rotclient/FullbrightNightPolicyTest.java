package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FullbrightNightPolicyTest {
    @Test
    void cardTracksEitherLightingMode() {
        FullbrightNightPolicy.LightingState off = FullbrightNightPolicy.LightingState.defaults();
        assertFalse(off.cardOn());
        assertTrue(FullbrightNightPolicy.enableCard(off).fullbright());
        assertFalse(FullbrightNightPolicy.enableCard(off).alwaysNight());
        FullbrightNightPolicy.LightingState nightOnly =
                FullbrightNightPolicy.setAlwaysNight(off, true);
        assertTrue(nightOnly.cardOn());
        assertTrue(FullbrightNightPolicy.disableCard(nightOnly).forceBoth() == nightOnly.forceBoth());
        assertFalse(FullbrightNightPolicy.disableCard(nightOnly).cardOn());
    }

    @Test
    void exclusiveTogglesUnlessForceBoth() {
        FullbrightNightPolicy.LightingState night =
                FullbrightNightPolicy.setAlwaysNight(FullbrightNightPolicy.LightingState.defaults(), true);
        FullbrightNightPolicy.LightingState fullbright =
                FullbrightNightPolicy.setFullbright(night, true);
        assertTrue(fullbright.fullbright());
        assertFalse(fullbright.alwaysNight());

        FullbrightNightPolicy.LightingState forced = FullbrightNightPolicy.setForceBoth(
                new FullbrightNightPolicy.LightingState(true, false, false), true);
        FullbrightNightPolicy.LightingState both =
                FullbrightNightPolicy.setAlwaysNight(forced, true);
        assertTrue(both.fullbright());
        assertTrue(both.alwaysNight());
        assertTrue(both.forceBoth());
    }

    @Test
    void releasingForceKeepsFullbrightWhenBothWereOn() {
        FullbrightNightPolicy.LightingState both =
                new FullbrightNightPolicy.LightingState(true, true, true);
        FullbrightNightPolicy.LightingState collapsed =
                FullbrightNightPolicy.setForceBoth(both, false);
        assertTrue(collapsed.fullbright());
        assertFalse(collapsed.alwaysNight());
        assertFalse(collapsed.forceBoth());
    }

    @Test
    void nightApplySnapsOnWorldChangeAndAnimatesOnEnable() {
        assertEquals(
                FullbrightNightPolicy.NightApply.OFF,
                FullbrightNightPolicy.decideNightApply(true, false, true, true, false));
        assertEquals(
                FullbrightNightPolicy.NightApply.SNAP,
                FullbrightNightPolicy.decideNightApply(true, true, false, true, false));
        assertEquals(
                FullbrightNightPolicy.NightApply.ANIMATE,
                FullbrightNightPolicy.decideNightApply(true, true, true, true, false));
        assertEquals(
                FullbrightNightPolicy.NightApply.LATCHED,
                FullbrightNightPolicy.decideNightApply(true, true, true, false, true));
        assertEquals(
                FullbrightNightPolicy.NightApply.SNAP,
                FullbrightNightPolicy.decideNightApply(true, true, true, false, false));
    }

    @Test
    void duskProgressSmoothstepsThenParks() {
        assertEquals(0.0F, FullbrightNightPolicy.duskProgress(0L, 0L, 8_000L));
        assertEquals(1.0F, FullbrightNightPolicy.duskProgress(8_000L, 0L, 8_000L));
        assertTrue(FullbrightNightPolicy.smoothstep(0.5F) > 0.4F);
        FullbrightNightPolicy.SkyPose start = new FullbrightNightPolicy.SkyPose(
                0.0F, (float) Math.PI, 0.0F, 0.0F, 0x87CEEB, 0, 1.0F);
        FullbrightNightPolicy.SkyPose parked = FullbrightNightPolicy.SkyPose.parkedNight(1.0F);
        FullbrightNightPolicy.SkyPose mid = FullbrightNightPolicy.lerpPose(start, parked, 1.0F);
        assertEquals(parked.sunAngle(), mid.sunAngle(), 0.0001F);
        assertEquals(parked.moonAngle(), mid.moonAngle(), 0.0001F);
        FullbrightNightPolicy.SkyPose halfway = FullbrightNightPolicy.lerpPose(start, parked, 0.5F);
        assertTrue(halfway.starBrightness() > start.starBrightness());
        assertTrue(halfway.starBrightness() < parked.starBrightness());
        assertTrue(FullbrightNightPolicy.applyNightLightmap(false, true));
        assertFalse(FullbrightNightPolicy.applyNightLightmap(true, true));
        assertTrue(FullbrightNightPolicy.applyFullbrightLightmap(true));
    }

    @Test
    void lightingSettingIdsAreOwnedOutsideQolUtilityConfig() {
        assertTrue(FullbrightNightPolicy.isLightingSetting(FullbrightNightPolicy.USE_FULLBRIGHT));
        assertTrue(FullbrightNightPolicy.isLightingSetting(FullbrightNightPolicy.ALWAYS_NIGHT));
        assertTrue(FullbrightNightPolicy.isLightingSetting(FullbrightNightPolicy.FORCE_BOTH));
        assertFalse(FullbrightNightPolicy.isLightingSetting("qol.render_optimizer"));
    }
}
