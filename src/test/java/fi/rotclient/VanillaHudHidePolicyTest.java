package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class VanillaHudHidePolicyTest {
    @Test
    void healthToggleOnlyAffectsHealth() {
        assertTrue(VanillaHudHidePolicy.shouldHideLayer(
                true, true, false, false, false, VanillaHudHidePolicy.Layer.HEALTH));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, true, false, false, false, VanillaHudHidePolicy.Layer.FOOD));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, true, false, false, false, VanillaHudHidePolicy.Layer.ARMOR));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, true, false, false, false, VanillaHudHidePolicy.Layer.XP));
    }

    @Test
    void foodToggleOnlyAffectsFood() {
        assertTrue(VanillaHudHidePolicy.shouldHideLayer(
                true, false, true, false, false, VanillaHudHidePolicy.Layer.FOOD));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, false, true, false, false, VanillaHudHidePolicy.Layer.HEALTH));
    }

    @Test
    void armorToggleOnlyAffectsArmor() {
        assertTrue(VanillaHudHidePolicy.shouldHideLayer(
                true, false, false, true, false, VanillaHudHidePolicy.Layer.ARMOR));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, false, false, true, false, VanillaHudHidePolicy.Layer.XP));
    }

    @Test
    void xpToggleOnlyAffectsXp() {
        assertTrue(VanillaHudHidePolicy.shouldHideLayer(
                true, false, false, false, true, VanillaHudHidePolicy.Layer.XP));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, false, false, false, true, VanillaHudHidePolicy.Layer.ARMOR));
    }

    @Test
    void unrelatedHudRemainsVisible() {
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                false, true, true, true, true, VanillaHudHidePolicy.Layer.HEALTH));
        assertFalse(VanillaHudHidePolicy.shouldHideLayer(
                true, false, false, false, false, VanillaHudHidePolicy.Layer.FOOD));
    }
}
