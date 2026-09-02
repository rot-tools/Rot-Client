package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientEaseTest {
    @Test
    void smoothstepStaysInUnitIntervalAndEasesEnds() {
        assertEquals(0.0D, RotClientEase.smoothstep(-1.0D), 1.0E-9);
        assertEquals(1.0D, RotClientEase.smoothstep(2.0D), 1.0E-9);
        assertEquals(0.5D, RotClientEase.smoothstep(0.5D), 1.0E-9);
        assertTrue(RotClientEase.smoothstep(0.25D) < 0.25D);
        assertTrue(RotClientEase.smoothstep(0.75D) > 0.75D);
    }

    @Test
    void expTowardSettlesOnTargetWithoutOvershoot() {
        double value = 0.0D;
        for (int i = 0; i < 40; i++) {
            double next = RotClientEase.expToward(value, 1.0D, 1.0D / 60.0D, 14.0D);
            assertTrue(next >= value - 1.0E-9);
            assertTrue(next <= 1.0D + 1.0E-9);
            value = next;
        }
        assertEquals(1.0D, value, 1.0E-6);
    }

    @Test
    void shownPixelsScalesFullHeight() {
        assertEquals(0, RotClientEase.shownPixels(120, 0.0D));
        assertEquals(120, RotClientEase.shownPixels(120, 1.0D));
        int mid = RotClientEase.shownPixels(120, 0.5D);
        assertEquals(60, mid);
    }

    @Test
    void realtimeTicksConvertToFrameSeconds() {
        assertEquals(0.0D, RotClientEase.secondsFromRealtimeTicks(0.0F), 1.0E-9);
        assertEquals(1.0D / 60.0D, RotClientEase.secondsFromRealtimeTicks(20.0F / 60.0F), 1.0E-6);
        assertEquals(1.0D / 144.0D, RotClientEase.secondsFromRealtimeTicks(20.0F / 144.0F), 1.0E-6);
        assertEquals(0.05D, RotClientEase.secondsFromRealtimeTicks(20.0F), 1.0E-9);
    }
}
