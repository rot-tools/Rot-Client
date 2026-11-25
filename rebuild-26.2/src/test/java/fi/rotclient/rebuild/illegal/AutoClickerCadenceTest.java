package fi.rotclient.rebuild.illegal;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AutoClickerCadenceTest {
    @Test
    void fixedRateUsesOneClickPerTickAtMostAndPreservesCadence() {
        var cadence = new AutoClickerCadence();
        assertFalse(cadence.active(false, 0));
        assertFalse(cadence.active(true, 0));
        int clicks = 0;
        for (int tick = 1; tick <= 20; tick++) {
            assertTrue(cadence.active(true, AutoClickerCadence.HOLD_DELAY_NANOS + tick * 50_000_000L));
            if (cadence.tick(7)) clicks++;
        }
        assertEquals(7, clicks);
        for (int tick = 1; tick <= 20; tick++) assertTrue(cadence.tick(20));
    }

    @Test
    void ionicJitterDoublesTheSliderValueBeforeClampingTheSample() {
        var random = new Random(42);
        int min = 20;
        int max = 0;
        for (int sample = 0; sample < 100; sample++) {
            int rate = AutoClickerCadence.variedRate(19, 5, random);
            min = Math.min(min, rate);
            max = Math.max(max, rate);
        }
        assertEquals(13, min);
        assertEquals(20, max);
        assertEquals(10, AutoClickerCadence.variedRate(10, 0, random));
        assertEquals(20, AutoClickerCadence.variedRate(33, 0, random));
        assertEquals(5, AutoClickerCadence.variedRate(0, 0, random));
    }

    @Test
    void rapidTapsAndReleasesBetweenTicksDoNotActivateTheRepeater() {
        var cadence = new AutoClickerCadence();
        long delay = AutoClickerCadence.HOLD_DELAY_NANOS;
        assertFalse(cadence.active(false, 0));
        assertFalse(cadence.active(true, 0));
        assertFalse(cadence.active(true, delay - 1));
        cadence.release();
        assertFalse(cadence.active(true, delay));
        assertFalse(cadence.active(true, 2 * delay - 1));
        assertTrue(cadence.active(true, 2 * delay));
    }

    @Test
    void releasingAndLeavingGameplayDiscardStoredClickProgress() {
        var cadence = new AutoClickerCadence();
        long delay = AutoClickerCadence.HOLD_DELAY_NANOS;
        assertFalse(cadence.active(true, delay));
        assertFalse(cadence.active(true, 2 * delay));
        assertFalse(cadence.active(false, 2 * delay));
        assertFalse(cadence.active(true, 2 * delay));
        assertTrue(cadence.active(true, 3 * delay));
        assertFalse(cadence.tick(19));

        cadence.disarm();
        assertFalse(cadence.active(true, 4 * delay));
        assertFalse(cadence.active(false, 4 * delay));
        assertFalse(cadence.active(true, 4 * delay));
        assertTrue(cadence.active(true, 5 * delay));
        assertFalse(cadence.tick(19));
        cadence.release();
        assertFalse(cadence.active(true, 5 * delay));
        assertTrue(cadence.active(true, 6 * delay));
        assertFalse(cadence.tick(19));
        assertTrue(cadence.tick(1));
    }

    @Test
    void singleplayerAndAnExtraTestServerRemainAvailable() {
        assertTrue(AutoClicker.serverAllowed(true, "", null));
        assertFalse(AutoClicker.serverAllowed(false, "", "private.example"));
        assertTrue(AutoClicker.serverAllowed(false, "private.example:25566", "PRIVATE.example:25566"));
        assertTrue(AutoClicker.serverAllowed(false, "private.example:25565", "PRIVATE.example"));
        assertFalse(AutoClicker.serverAllowed(false, "private.example:25566", "private.example"));
        assertFalse(AutoClicker.serverAllowed(false, "private.example", "private.example.other"));
        assertFalse(AutoClicker.serverAllowed(false, "private.example", null));
    }

    @Test
    void hypixelWorksWithoutAnExtraServerAddress() {
        assertTrue(AutoClicker.serverAllowed(false, "", "hypixel.net"));
        assertTrue(AutoClicker.serverAllowed(false, "", "MC.HYPIXEL.NET:25565"));
        assertTrue(AutoClicker.serverAllowed(false, "private.example", "hypixel.net"));
        assertTrue(AutoClicker.isHypixelAddress("hypixel.net."));
        assertFalse(AutoClicker.serverAllowed(false, "", "nothypixel.net"));
        assertFalse(AutoClicker.serverAllowed(false, "", "hypixel.net.example"));
    }

    @Test
    void preservesLeftAndRightMouseButtonsAsDistinctSavedBindings() {
        var settings = new AutoClickerSettings();
        settings.left.binding = AutoClickerSettings.mouseBinding(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        settings.right.binding = AutoClickerSettings.mouseBinding(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        settings.normalize();
        var left = AutoClickerSettings.inputKey(settings.left.binding);
        var right = AutoClickerSettings.inputKey(settings.right.binding);
        assertEquals(InputConstants.Type.MOUSE, left.getType());
        assertEquals(InputConstants.Type.MOUSE, right.getType());
        assertEquals(GLFW.GLFW_MOUSE_BUTTON_LEFT, left.getValue());
        assertEquals(GLFW.GLFW_MOUSE_BUTTON_RIGHT, right.getValue());
        assertFalse(AutoClickerSettings.isMouseBinding(InputConstants.UNKNOWN));
    }
}
