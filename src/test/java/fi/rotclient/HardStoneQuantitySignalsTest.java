package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HardStoneQuantitySignalsTest {
    @Test
    void blockOnlyIsNotFortuneSafe() {
        HardStoneQuantitySignals.Assessment assessment =
                HardStoneQuantitySignals.assess(true, false, false, false);
        assertTrue(assessment.blockEvidencePresent());
        assertEquals(
                HardStoneQuantitySignals.Signal.NONE,
                assessment.quantitySignal());
        assertFalse(assessment.fortuneSafe());
        assertTrue(assessment.liveTestHint().contains("unresolved"));
    }

    @Test
    void sackQuantityPreferred() {
        HardStoneQuantitySignals.Assessment assessment =
                HardStoneQuantitySignals.assess(true, true, true, true);
        assertEquals(
                HardStoneQuantitySignals.Signal.SACK_MESSAGE,
                assessment.quantitySignal());
        assertTrue(assessment.fortuneSafe());
    }

    @Test
    void actionBarSupportedWhenNoSack() {
        HardStoneQuantitySignals.Assessment assessment =
                HardStoneQuantitySignals.assess(true, false, true, false);
        assertEquals(
                HardStoneQuantitySignals.Signal.ACTION_BAR,
                assessment.quantitySignal());
        assertTrue(assessment.fortuneSafe());
    }
}
