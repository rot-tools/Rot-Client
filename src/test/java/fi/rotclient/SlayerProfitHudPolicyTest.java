package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerProfitHudPolicyTest {
    @Test
    void respectsTheOptionalInventoryOnlyGate() {
        assertTrue(SlayerProfitHudPolicy.shouldRender(true, true, false, false, false));
        assertFalse(SlayerProfitHudPolicy.shouldRender(true, true, true, false, false));
        assertTrue(SlayerProfitHudPolicy.shouldRender(true, true, true, false, true));
    }

    @Test
    void keepsTheHudVisibleInsideItsOwnEditor() {
        assertTrue(SlayerProfitHudPolicy.shouldRender(true, true, true, true, false));
        assertFalse(SlayerProfitHudPolicy.shouldRender(false, true, false, true, true));
        assertFalse(SlayerProfitHudPolicy.shouldRender(true, false, false, true, true));
    }
}
