package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RateGraphMathTest {
    @Test
    void constantRateRemainsFlatAfterFiltering() {
        assertArrayEquals(
                new double[]{48_000, 48_000, 48_000, 48_000},
                RateGraphMath.smooth(new double[]{48_000, 48_000, 48_000, 48_000}),
                0.0001);
    }

    @Test
    void isolatedSampleSpikeBecomesAContinuousHill() {
        double[] smoothed = RateGraphMath.smooth(new double[]{0, 0, 100, 0, 0});

        assertTrue(smoothed[2] < 100);
        assertTrue(smoothed[1] > 0);
        assertTrue(smoothed[3] > 0);
    }

    @Test
    void cubicSamplingPassesThroughSamplesWithoutOvershoot() {
        double[] values = {0, 20, 60, 80};

        assertEquals(20, RateGraphMath.sampleCatmullRom(values, 1), 0.0001);
        double between = RateGraphMath.sampleCatmullRom(values, 1.5);
        assertTrue(between >= 20 && between <= 60);
    }
}
