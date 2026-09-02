package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SaleObservationGateTest {
    @Test
    void duplicatePartialSaleDeliveryIsRejected() {
        SaleObservationGate gate = new SaleObservationGate();

        assertTrue(gate.shouldCredit(false, 100, 12_345, 1_000));
        assertFalse(gate.shouldCredit(false, 100, 12_345, 1_001));
    }

    @Test
    void separateLaterSaleIsAccepted() {
        SaleObservationGate gate = new SaleObservationGate();

        assertTrue(gate.shouldCredit(true, 100, 46_270, 1_000));
        assertTrue(gate.shouldCredit(true, 100, 46_270, 2_000));
    }
}
