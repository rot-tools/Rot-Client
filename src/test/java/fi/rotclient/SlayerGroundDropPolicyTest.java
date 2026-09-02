package fi.rotclient;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerGroundDropPolicyTest {
    @Test
    void usesTheWholeStackValueForTheMinimumGate() {
        assertEquals(new BigDecimal("250.0"), SlayerGroundDropPolicy.totalValue(new BigDecimal("12.5"), 20));
        assertTrue(SlayerGroundDropPolicy.shouldShowLabel(true, new BigDecimal("12.5"), 20, 250));
        assertFalse(SlayerGroundDropPolicy.shouldShowLabel(true, new BigDecimal("12.5"), 20, 251));
    }

    @Test
    void rejectsUnavailableQuotesAndInvalidStacks() {
        assertFalse(SlayerGroundDropPolicy.shouldShowLabel(true, null, 1, 0));
        assertFalse(SlayerGroundDropPolicy.shouldShowLabel(true, BigDecimal.ZERO, 1, 0));
        assertFalse(SlayerGroundDropPolicy.shouldShowLabel(true, BigDecimal.ONE, 0, 0));
        assertFalse(SlayerGroundDropPolicy.shouldShowLabel(false, BigDecimal.ONE, 1, 0));
    }

    @Test
    void clampsPersistedMinimumToASafeRange() {
        assertEquals(0L, SlayerGroundDropPolicy.clampMinimum(-1L));
        assertEquals(100_000_000L, SlayerGroundDropPolicy.clampMinimum(200_000_000L));
    }
}
