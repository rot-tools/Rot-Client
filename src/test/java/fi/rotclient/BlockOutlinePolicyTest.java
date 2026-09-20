package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BlockOutlinePolicyTest {
    @Test
    void modeNormalizesToTheTwoKnownStyles() {
        assertEquals(BlockOutlinePolicy.MODE_RAINBOW, BlockOutlinePolicy.normalizeMode("Rainbow"));
        assertEquals(BlockOutlinePolicy.MODE_RAINBOW, BlockOutlinePolicy.normalizeMode("  rainbow "));
        assertEquals(BlockOutlinePolicy.MODE_SOLID, BlockOutlinePolicy.normalizeMode("Solid Color"));
        assertEquals(BlockOutlinePolicy.MODE_SOLID, BlockOutlinePolicy.normalizeMode("nonsense"));
        assertEquals(BlockOutlinePolicy.MODE_SOLID, BlockOutlinePolicy.normalizeMode(null));
        assertTrue(BlockOutlinePolicy.isRainbow("rainbow"));
        assertFalse(BlockOutlinePolicy.isRainbow(null));
    }

    @Test
    void numbersClampAndRejectNonFiniteValues() {
        assertEquals(BlockOutlinePolicy.MIN_WIDTH, BlockOutlinePolicy.clampWidth(-3.0D));
        assertEquals(BlockOutlinePolicy.MAX_WIDTH, BlockOutlinePolicy.clampWidth(99.0D));
        assertEquals(BlockOutlinePolicy.DEFAULT_WIDTH, BlockOutlinePolicy.clampWidth(Double.NaN));
        assertEquals(BlockOutlinePolicy.MIN_SPEED, BlockOutlinePolicy.clampSpeed(0.0D));
        assertEquals(BlockOutlinePolicy.DEFAULT_SPEED,
                BlockOutlinePolicy.clampSpeed(Double.POSITIVE_INFINITY));
        assertEquals(BlockOutlinePolicy.MAX_SPREAD, BlockOutlinePolicy.clampSpread(10.0D));
        assertEquals(0.0D, BlockOutlinePolicy.clampSpread(-1.0D));
    }

    @Test
    void widthScalesTheVanillaLineWidth() {
        assertEquals(2.0F, BlockOutlinePolicy.scaledWidth(2.0F, 1.0D));
        assertEquals(4.0F, BlockOutlinePolicy.scaledWidth(2.0F, 2.0D));
        assertEquals(1.0F, BlockOutlinePolicy.scaledWidth(2.0F, 0.0D));
        assertEquals(0.0F, BlockOutlinePolicy.scaledWidth(-1.0F, 1.0D));
    }

    @Test
    void transparentSolidColorFallsBackToOpaque() {
        assertEquals(0xFF123456, BlockOutlinePolicy.solidColor(0x00123456));
        assertEquals(0x80123456, BlockOutlinePolicy.solidColor(0x80123456));
    }

    @Test
    void hueStaysInRangeAndMovesWithTime() {
        for (double seconds = -10.0D; seconds < 50.0D; seconds += 1.7D) {
            double hue = BlockOutlinePolicy.hue(seconds, 1.0D, 1.0D, 0.3D, 0.6D, 0.9D);
            assertTrue(hue >= 0.0D && hue < 1.0D, "hue " + hue);
        }
        double early = BlockOutlinePolicy.hue(0.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        double later = BlockOutlinePolicy.hue(1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        assertEquals(0.25D, later - early, 1.0E-9D);
    }

    @Test
    void speedScalesTheCycleAndCompletesItInFourSecondsAtOne() {
        assertEquals(
                BlockOutlinePolicy.hue(0.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                BlockOutlinePolicy.hue(4.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                1.0E-9D);
        assertEquals(
                BlockOutlinePolicy.hue(2.0D, 2.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                BlockOutlinePolicy.hue(4.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                1.0E-9D);
    }

    @Test
    void spreadDecidesWhetherPositionChangesTheHue() {
        double a = BlockOutlinePolicy.hue(3.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        double b = BlockOutlinePolicy.hue(3.0D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        assertEquals(a, b, 1.0E-9D);
        double c = BlockOutlinePolicy.hue(3.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        double d = BlockOutlinePolicy.hue(3.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D);
        assertNotEquals(c, d);
        assertEquals(1.0D / 3.0D, BlockOutlinePolicy.hue(0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D), 1.0E-9D);
    }

    @Test
    void rainbowHitsThePrimaryAndSecondaryHues() {
        assertEquals(0xFFFF0000, BlockOutlinePolicy.rainbowArgb(0.0D, 0xFF));
        assertEquals(0xFFFFFF00, BlockOutlinePolicy.rainbowArgb(1.0D / 6.0D, 0xFF));
        assertEquals(0xFF00FF00, BlockOutlinePolicy.rainbowArgb(1.0D / 3.0D, 0xFF));
        assertEquals(0xFF00FFFF, BlockOutlinePolicy.rainbowArgb(0.5D, 0xFF));
        assertEquals(0xFF0000FF, BlockOutlinePolicy.rainbowArgb(2.0D / 3.0D, 0xFF));
        assertEquals(0xFFFF00FF, BlockOutlinePolicy.rainbowArgb(5.0D / 6.0D, 0xFF));
    }

    @Test
    void rainbowKeepsTheRequestedAlphaAndWrapsOutOfRangeHues() {
        assertEquals(0x66FF0000, BlockOutlinePolicy.rainbowArgb(0.0D, 0x66));
        assertEquals(
                BlockOutlinePolicy.rainbowArgb(0.25D, 0xFF),
                BlockOutlinePolicy.rainbowArgb(1.25D, 0xFF));
        assertEquals(
                BlockOutlinePolicy.rainbowArgb(0.75D, 0xFF),
                BlockOutlinePolicy.rainbowArgb(-0.25D, 0xFF));
    }

    @Test
    void everyRainbowColorIsFullyOpaqueBrightAndNeverBlack() {
        for (int i = 0; i < 360; i++) {
            int argb = BlockOutlinePolicy.rainbowArgb(i / 360.0D, 0xFF);
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            assertEquals(255, Math.max(r, Math.max(g, b)), "hue " + i);
            assertEquals(0, Math.min(r, Math.min(g, b)), "hue " + i);
        }
    }

    @Test
    void edgeSegmentsFollowLengthWithinBounds() {
        assertEquals(1, BlockOutlinePolicy.segmentCount(0.0D));
        assertEquals(1, BlockOutlinePolicy.segmentCount(Double.NaN));
        assertEquals(8, BlockOutlinePolicy.segmentCount(1.0D));
        assertEquals(4, BlockOutlinePolicy.segmentCount(0.5D));
        assertEquals(BlockOutlinePolicy.MAX_SEGMENTS, BlockOutlinePolicy.segmentCount(500.0D));
    }
}
