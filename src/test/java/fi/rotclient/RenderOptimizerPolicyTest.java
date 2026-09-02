package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RenderOptimizerPolicyTest {
    @Test
    void disabledOptimizerSuppressesNothing() {
        assertFalse(RenderOptimizerPolicy.shouldSuppressEntity(
                false, true, true, true, true, true, true, true, true, true,
                RenderOptimizerPolicy.EntityKind.FALLING_BLOCK, true, true));
        assertFalse(RenderOptimizerPolicy.shouldSuppressParticle(
                false, true, RenderOptimizerPolicy.ParticleKind.EXPLOSION));
        assertFalse(RenderOptimizerPolicy.shouldSuppressFireOverlay(false, true));
    }

    @Test
    void fallingBlockToggle() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressEntity(
                true, true, false, false, false, false, false, false, false, false,
                RenderOptimizerPolicy.EntityKind.FALLING_BLOCK, false, false));
        assertFalse(RenderOptimizerPolicy.shouldSuppressEntity(
                true, false, false, false, false, false, false, false, false, false,
                RenderOptimizerPolicy.EntityKind.FALLING_BLOCK, false, false));
    }

    @Test
    void lightningToggle() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressEntity(
                true, false, true, false, false, false, false, false, false, false,
                RenderOptimizerPolicy.EntityKind.LIGHTNING, false, false));
    }

    @Test
    void experienceOrbToggle() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressEntity(
                true, false, false, true, false, false, false, false, false, false,
                RenderOptimizerPolicy.EntityKind.EXPERIENCE_ORB, false, false));
    }

    @Test
    void deathAnimationToggle() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressEntity(
                true, false, false, false, true, false, false, false, false, false,
                RenderOptimizerPolicy.EntityKind.LIVING_DYING, false, false));
    }

    @Test
    void explosionParticleToggle() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressParticle(
                true, true, RenderOptimizerPolicy.ParticleKind.EXPLOSION));
        assertFalse(RenderOptimizerPolicy.shouldSuppressParticle(
                true, true, RenderOptimizerPolicy.ParticleKind.OTHER));
    }

    @Test
    void fireOverlayToggle() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressFireOverlay(true, true));
        assertFalse(RenderOptimizerPolicy.shouldSuppressFireOverlay(true, false));
    }

    @Test
    void uncertainSkyblockEntityFailsOpen() {
        assertFalse(RenderOptimizerPolicy.shouldSuppressEntity(
                true, false, false, false, false, false, true, false, false, false,
                RenderOptimizerPolicy.EntityKind.SKYBLOCK_ARCHER_PASSIVE,
                false,
                false));
        assertFalse(RenderOptimizerPolicy.matchesSkyblockVisual(
                RenderOptimizerPolicy.EntityKind.SKYBLOCK_HEALER_FAIRY, "random mob"));
        assertFalse(RenderOptimizerPolicy.isSafeArmorStandHideTarget(
                true, false, false));
        assertTrue(RenderOptimizerPolicy.isSafeArmorStandHideTarget(
                false, false, false));
    }
}
