package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NoCursorResetPolicyTest {
    @Test
    void disabledUsesVanillaBehavior() {
        assertFalse(NoCursorResetPolicy.shouldPreserveCursor(
                false, true, true, 1000L, 1010L, 150));
    }

    @Test
    void supportedScreenTransitionPreservesCursor() {
        assertTrue(NoCursorResetPolicy.shouldPreserveCursor(
                true, true, true, 1000L, 1050L, 150));
    }

    @Test
    void timeoutFallsBackSafely() {
        assertFalse(NoCursorResetPolicy.shouldPreserveCursor(
                true, true, true, 1000L, 1200L, 150));
        assertEquals(150, NoCursorResetPolicy.clampTimeoutMs(150));
        assertEquals(0, NoCursorResetPolicy.clampTimeoutMs(-5));
        assertEquals(1000, NoCursorResetPolicy.clampTimeoutMs(5000));
    }

    @Test
    void returningToGameplayRecapturesMouse() {
        assertTrue(NoCursorResetPolicy.shouldRecaptureOnGameplay(true, false));
        assertFalse(NoCursorResetPolicy.shouldPreserveCursor(
                true, true, false, 1000L, 1010L, 150));
        assertFalse(NoCursorResetPolicy.shouldRecaptureOnGameplay(false, true));
    }

    @Test
    void controllerRestoresSeveralTimesWithoutPinningCursorForTheWholeTimeout() {
        NoCursorResetController controller = new NoCursorResetController();
        controller.onScreenChanging(
                true, true, true, 120.0D, 80.0D, 1_000L, 150);

        for (int i = 0; i < 6; i++) {
            assertTrue(controller.consumeRestore(1_001L + i));
        }
        assertFalse(controller.consumeRestore(1_007L));
        assertEquals(120.0D, controller.savedX());
        assertEquals(80.0D, controller.savedY());
    }

    @Test
    void newerScreenTransitionReplacesAnOlderPendingPosition() {
        NoCursorResetController controller = new NoCursorResetController();
        controller.onScreenChanging(
                true, true, true, 10.0D, 20.0D, 1_000L, 150);
        controller.onScreenChanging(
                true, true, true, 70.0D, 90.0D, 1_010L, 150);

        assertEquals(70.0D, controller.savedX());
        assertEquals(90.0D, controller.savedY());
        assertTrue(controller.consumeRestore(1_011L));
    }
}
