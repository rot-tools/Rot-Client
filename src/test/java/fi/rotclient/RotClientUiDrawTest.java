package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientUiDrawTest {
    @Test
    void scrollbarUsesReadableVisualAndHitTargets() {
        assertTrue(RotClientUiDraw.SCROLLBAR_WIDTH >= 8);
        assertTrue(RotClientUiDraw.SCROLLBAR_WIDTH <= 10);
        assertTrue(RotClientUiDraw.SCROLLBAR_HIT_WIDTH >= 12);
        assertTrue(RotClientUiDraw.SCROLLBAR_HIT_WIDTH <= 14);
        assertTrue(RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                >= RotClientUiDraw.SCROLLBAR_WIDTH);
        assertTrue(RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT >= 40);
    }

    @Test
    void cornerInsetIsMonotonicWithinRadius() {
        int radius = 4;
        int previous = Integer.MAX_VALUE;
        for (int dy = 0; dy < radius; dy++) {
            int inset = RotClientUiDraw.cornerInset(dy, radius);
            assertTrue(inset >= 0);
            assertTrue(inset <= radius);
            assertTrue(inset <= previous);
            previous = inset;
        }
        assertTrue(RotClientUiDraw.cornerInset(radius - 1, radius) <= 1);
    }

    @Test
    void withAlphaReplacesOnlyAlphaChannel() {
        assertEquals(0x55E33B3B, RotClientUiDraw.withAlpha(0xFFE33B3B, 0x55));
        assertEquals(0x00ABCDEF, RotClientUiDraw.withAlpha(0xFFABCDEF, 0));
    }

    @Test
    void roundedFillUsesCornerSpansNotPerRowScanlines() {
        assertEquals(1, RotClientUiDraw.roundedFillSpanCount(900, 0));
        assertEquals(17, RotClientUiDraw.roundedFillSpanCount(900, 8));
        assertEquals(9, RotClientUiDraw.roundedFillSpanCount(26, 4));
        assertEquals(8, RotClientUiDraw.roundedFillSpanCount(8, 4));
        assertEquals(0, RotClientUiDraw.roundedFillSpanCount(0, 4));
    }
}
