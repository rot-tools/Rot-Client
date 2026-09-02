package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerRngMeterPolicyTest {
    @Test
    void warnsOnlyForAnObservedMeterUpdateWithoutASelectedDrop() {
        assertTrue(SlayerRngMeterPolicy.shouldWarnEmpty(true, true, false));
        assertFalse(SlayerRngMeterPolicy.shouldWarnEmpty(false, true, false));
        assertFalse(SlayerRngMeterPolicy.shouldWarnEmpty(true, false, false));
        assertFalse(SlayerRngMeterPolicy.shouldWarnEmpty(true, true, true));
    }

    @Test
    void hidesOnlyAConfirmedMeterUpdateWithAKnownSelection() {
        assertTrue(SlayerRngMeterPolicy.shouldHideChat(true, true, true));
        assertFalse(SlayerRngMeterPolicy.shouldHideChat(false, true, true));
        assertFalse(SlayerRngMeterPolicy.shouldHideChat(true, false, true));
        assertFalse(SlayerRngMeterPolicy.shouldHideChat(true, true, false));
    }
}
