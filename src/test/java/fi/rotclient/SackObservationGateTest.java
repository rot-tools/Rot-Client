package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SackObservationGateTest {
    @Test
    void identicalSackComponentCanOnlyBeCreditedOnce() {
        SackObservationGate gate = new SackObservationGate();

        assertTrue(gate.shouldCredit(
                "[Sacks] +20,890 items. (Last 26s.)",
                20_361, 59, 202, 1_000));
        assertFalse(gate.shouldCredit(
                "[Sacks] +20,890 items. (Last 26s.)",
                20_361, 59, 202, 1_001));
    }

    @Test
    void replayWithoutNewBlocksIsRejectedOutsideFingerprintWindow() {
        SackObservationGate gate = new SackObservationGate();

        assertTrue(gate.shouldCredit(
                "[Sacks] +20,890 items. (Last 26s.)",
                20_361, 59, 202, 1_000));
        assertFalse(gate.shouldCredit(
                "[Sacks] +20,890 items. (Last 26s.)",
                20_361, 59, 202, 5_000));
    }

    @Test
    void delayedSummaryRequiresNewVerifiedBlocks() {
        SackObservationGate gate = new SackObservationGate();

        assertTrue(gate.shouldCredit(
                "[Sacks] +100 items. (Last 30s.)",
                100, 0, 10, 1_000));
        assertFalse(gate.shouldCredit(
                "[Sacks] +200 items. (Last 30s.)",
                200, 0, 10, 4_000));
        assertTrue(gate.shouldCredit(
                "[Sacks] +200 items. (Last 30s.)",
                200, 0, 11, 4_001));
    }

    @Test
    void legitimateDelayedSummaryAfterNewBlocksIsAccepted() {
        SackObservationGate gate = new SackObservationGate();

        assertTrue(gate.shouldCredit(
                "[Sacks] +474 items. (Last 26s.)",
                0, 198, 1_200, 1_000));
        assertTrue(gate.shouldCredit(
                "[Sacks] +625 items. (Last 30s.)",
                0, 211, 1_450, 31_000));
    }

    @Test
    void interleavedReplayIsRejectedWithinDeliveryWindow() {
        SackObservationGate gate = new SackObservationGate();

        assertTrue(gate.shouldCredit("[Sacks] +111 items. (Last 26s.)",
                0, 111, 100, 1_000));
        assertTrue(gate.shouldCredit("[Sacks] +80 items. (Last 28s.)",
                0, 80, 101, 1_100));
        assertFalse(gate.shouldCredit("[Sacks] +111 items. (Last 26s.)",
                0, 111, 102, 1_200));
    }

    @Test
    void identicalLegitimateSummaryIsAllowedAfterWindowAndNewBlocks() {
        SackObservationGate gate = new SackObservationGate();

        assertTrue(gate.shouldCredit("[Sacks] +100 items. (Last 30s.)",
                100, 0, 10, 1_000));
        assertTrue(gate.shouldCredit("[Sacks] +100 items. (Last 30s.)",
                100, 0, 20, 31_000));
    }

    @Test
    void completeObservedSessionAcceptsFinalDelayedSummary() {
        SackObservationGate gate = new SackObservationGate();
        long acceptedEnchanted = 0;

        if (gate.shouldCredit("[Sacks] +111 items. (Last 28s.)",
                0, 111, 100, 1_000)) acceptedEnchanted += 111;
        if (gate.shouldCredit("[Sacks] +80 items. (Last 29s.)",
                0, 80, 200, 31_000)) acceptedEnchanted += 80;
        if (gate.shouldCredit("[Sacks] +289 items. (Last 30s.)",
                0, 289, 300, 61_000)) acceptedEnchanted += 289;
        // The delayed +198 is valid regardless of which menu is currently open.
        if (gate.shouldCredit("[Sacks] +474 items. (Last 26s.)",
                0, 198, 400, 91_000)) acceptedEnchanted += 198;

        assertEquals(678, acceptedEnchanted);
    }

    @Test
    void restoredSessionRequiresANewBlockBeforeFirstSummary() {
        SackObservationGate gate = new SackObservationGate();
        gate.reset(250);

        assertFalse(gate.shouldCredit(
                "[Sacks] +100 items. (Last 30s.)",
                100, 0, 250, 1_000));
        assertTrue(gate.shouldCredit(
                "[Sacks] +100 items. (Last 30s.)",
                100, 0, 251, 2_000));
    }
}
