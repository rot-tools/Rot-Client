package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerDropScalePolicyTest {
    @Test
    void recognizesDropsOnlyForTheirSlayerFamily() {
        assertTrue(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.REVENANT, "WARDEN_HEART"));
        assertTrue(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.TARANTULA, "DIGESTED_MOSQUITO"));
        assertTrue(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.SVEN, "OVERFLUX_CAPACITOR"));
        assertTrue(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.VOIDGLOOM, "JUDGEMENT_CORE"));
        assertTrue(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.INFERNO, "SUBZERO_INVERTER"));
        assertTrue(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.VAMPIRE, "MCGRUBBER_BURGER"));

        assertFalse(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.REVENANT, "JUDGEMENT_CORE"));
        assertFalse(SlayerDropScalePolicy.isDropFor(
                SlayerPolicy.SlayerType.SVEN, "DIAMOND"));
    }

    @Test
    void scalingRequiresTimeRangeAndEnabledFamily() {
        SlayerDropScalePolicy.Window window = new SlayerDropScalePolicy.Window(
                SlayerPolicy.SlayerType.VOIDGLOOM, 1_000L, 10.0D, 64.0D, 10.0D);
        assertTrue(SlayerDropScalePolicy.shouldScale(
                window, "JUDGEMENT_CORE", 2_000L,
                12.0D, 64.0D, 11.0D, 1.0D, 15));
        assertFalse(SlayerDropScalePolicy.shouldScale(
                window, "JUDGEMENT_CORE", 17_000L,
                12.0D, 64.0D, 11.0D, 1.0D, 15));
        assertFalse(SlayerDropScalePolicy.shouldScale(
                window, "JUDGEMENT_CORE", 2_000L,
                30.0D, 64.0D, 10.0D, 1.0D, 15));
        assertFalse(SlayerDropScalePolicy.shouldScale(
                window, "ENDER_PEARL", 2_000L,
                12.0D, 64.0D, 11.0D, 1.0D, 15));
    }

    @Test
    void clampsVisualSettingsToSafeValues() {
        assertEquals(1.0D, SlayerDropScalePolicy.clampScale(-2.0D));
        assertEquals(8.0D, SlayerDropScalePolicy.clampScale(99.0D));
        assertEquals(0.5D, SlayerDropScalePolicy.clampRangeMultiplier(0.0D));
        assertEquals(60, SlayerDropScalePolicy.clampUnscaleSeconds(1000));
    }
}
