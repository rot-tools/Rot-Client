package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlayerHudTextPolicyTest {
    @Test
    void keepsShortLinesAtNormalScaleAndShrinksOnlyLongLines() {
        assertEquals(1.0F, SlayerHudTextPolicy.scaleFor(120, 144));
        assertEquals(0.8F, SlayerHudTextPolicy.scaleFor(180, 144));
        assertEquals(SlayerHudTextPolicy.MIN_SCALE, SlayerHudTextPolicy.scaleFor(500, 144));
    }
}
