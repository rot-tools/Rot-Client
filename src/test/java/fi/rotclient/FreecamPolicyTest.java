package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FreecamPolicyTest {
    @Test
    void clampSpeedRejectsOutOfRangeAndNaN() {
        assertEquals(FreecamPolicy.DEFAULT_SPEED, FreecamPolicy.clampSpeed(Double.NaN));
        assertEquals(FreecamPolicy.MIN_SPEED, FreecamPolicy.clampSpeed(0.0D));
        assertEquals(FreecamPolicy.MAX_SPEED, FreecamPolicy.clampSpeed(99.0D));
        assertEquals(1.5D, FreecamPolicy.clampSpeed(1.5D));
    }

    @Test
    void runGateRequiresModuleAndWorld() {
        assertTrue(FreecamPolicy.shouldRun(true, true));
        assertFalse(FreecamPolicy.shouldRun(false, true));
        assertFalse(FreecamPolicy.shouldRun(true, false));
    }

    @Test
    void blocksLookAndMovePacketsWhileActive() {
        assertTrue(FreecamPolicy.shouldBlockMovePacket(true, true, false));
        assertTrue(FreecamPolicy.shouldBlockMovePacket(true, false, true));
        assertFalse(FreecamPolicy.shouldBlockMovePacket(true, false, false));
        assertFalse(FreecamPolicy.shouldBlockMovePacket(false, true, true));
    }

    @Test
    void hidesLocalBodyOnlyWhenShowBodyIsOff() {
        assertTrue(FreecamPolicy.shouldHideLocalBody(true, false, true));
        assertFalse(FreecamPolicy.shouldHideLocalBody(true, true, true));
        assertFalse(FreecamPolicy.shouldHideLocalBody(true, false, false));
        assertFalse(FreecamPolicy.shouldHideLocalBody(false, false, true));
    }

    @Test
    void noclipIsTheInverseOfCollide() {
        assertTrue(FreecamPolicy.shouldNoclip(false));
        assertFalse(FreecamPolicy.shouldNoclip(true));
    }

    @Test
    void flyForwardLooksSouthAlongPositiveZ() {
        FreecamPolicy.Vec3d delta = FreecamPolicy.flyDelta(
                true, false, false, false, false, false, false,
                0.0D, 0.0D, 1.0D);
        assertEquals(0.0D, delta.x(), 1.0E-6D);
        assertEquals(0.0D, delta.y(), 1.0E-6D);
        assertEquals(1.0D, delta.z(), 1.0E-6D);
    }

    @Test
    void sprintDoublesSpeedAndJumpMovesUp() {
        FreecamPolicy.Vec3d walk = FreecamPolicy.flyDelta(
                true, false, false, false, false, false, false,
                0.0D, 0.0D, 1.0D);
        FreecamPolicy.Vec3d sprint = FreecamPolicy.flyDelta(
                true, false, false, false, false, false, true,
                0.0D, 0.0D, 1.0D);
        assertEquals(walk.z() * FreecamPolicy.SPRINT_MULTIPLIER, sprint.z(), 1.0E-6D);
        FreecamPolicy.Vec3d up = FreecamPolicy.flyDelta(
                false, false, false, false, true, false, false,
                0.0D, 0.0D, 1.0D);
        assertEquals(1.0D, up.y(), 1.0E-6D);
        assertEquals(0.0D, up.x(), 1.0E-6D);
    }

    @Test
    void collisionNudgesBackFromTheHit() {
        FreecamPolicy.Vec3d from = new FreecamPolicy.Vec3d(0.0D, 0.0D, 0.0D);
        FreecamPolicy.Vec3d to = new FreecamPolicy.Vec3d(0.0D, 0.0D, 2.0D);
        FreecamPolicy.Vec3d hit = new FreecamPolicy.Vec3d(0.0D, 0.0D, 1.0D);
        FreecamPolicy.Vec3d clipped = FreecamPolicy.applyCollision(from, to, true, hit);
        assertEquals(1.0D - FreecamPolicy.HIT_NUDGE, clipped.z(), 1.0E-6D);
        assertEquals(to, FreecamPolicy.applyCollision(from, to, false, hit));
    }

    @Test
    void extrasRoundTripFreecamSettings() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertFalse(config.isModuleEnabled("qol.freecam"));
        config.setModuleEnabled("qol.freecam", true);
        assertTrue(config.isModuleEnabled("qol.freecam"));
        assertEquals(FreecamPolicy.DEFAULT_SPEED, config.readNumber("qol.freecam.speed"));
        assertTrue(config.readBoolean("qol.freecam.show_body"));
        assertFalse(config.readBoolean("qol.freecam.collide"));
        assertTrue(config.writeNumber("qol.freecam.speed", 2.5D));
        assertEquals(2.5D, config.readNumber("qol.freecam.speed"));
        config.writeBoolean("qol.freecam.show_body", false);
        config.writeBoolean("qol.freecam.collide", true);
        assertFalse(config.readBoolean("qol.freecam.show_body"));
        assertTrue(config.readBoolean("qol.freecam.collide"));
        assertTrue(config.writeKeybind("qol.freecam.keybind", "U"));
        assertEquals("U", config.readKeybind("qol.freecam.keybind"));
        assertTrue(config.resetModuleToDefaults("qol.freecam"));
        assertFalse(config.isModuleEnabled("qol.freecam"));
        assertEquals(FreecamPolicy.DEFAULT_SPEED, config.readNumber("qol.freecam.speed"));
        assertTrue(config.readBoolean("qol.freecam.show_body"));
        assertFalse(config.readBoolean("qol.freecam.collide"));
        assertEquals("", config.readKeybind("qol.freecam.keybind"));
    }
}
