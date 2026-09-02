package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneMiningCorrelationGateTest {
    @Test
    void exposesRecentMatchingSignals() {
        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate(
                        8_000L);

        gate.recordDirectBreak(
                GemstoneType.RUBY,
                10_000L);

        gate.recordPristine(
                GemstoneType.RUBY,
                24L,
                11_000L);

        GemstoneMiningCorrelationGate.Correlation result =
                gate.snapshot(
                        GemstoneType.RUBY,
                        12_000L);

        assertEquals(
                1,
                result.directBreaks());

        assertEquals(
                2_000L,
                result.latestDirectBreakAgeMillis());

        assertTrue(
                result.pristineObserved());

        assertEquals(
                24L,
                result.pristineAmount());
    }

    @Test
    void keepsGemstoneSignalsIsolated() {
        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate(
                        8_000L);

        gate.recordDirectBreak(
                GemstoneType.RUBY,
                10_000L);

        gate.recordPristine(
                GemstoneType.RUBY,
                16L,
                10_500L);

        GemstoneMiningCorrelationGate.Correlation jade =
                gate.snapshot(
                        GemstoneType.JADE,
                        11_000L);

        assertEquals(
                0,
                jade.directBreaks());

        assertFalse(
                jade.pristineObserved());

        assertEquals(
                0L,
                jade.pristineAmount());
    }

    @Test
    void manualSackEventHasNoMiningSignals() {
        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate(
                        8_000L);

        GemstoneMiningCorrelationGate.Correlation result =
                gate.snapshot(
                        GemstoneType.RUBY,
                        20_000L);

        assertEquals(
                0,
                result.directBreaks());

        assertEquals(
                -1L,
                result.latestDirectBreakAgeMillis());

        assertFalse(
                result.pristineObserved());
    }

    @Test
    void removesExpiredSignals() {
        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate(
                        8_000L);

        gate.recordDirectBreak(
                GemstoneType.TOPAZ,
                1_000L);

        gate.recordPristine(
                GemstoneType.TOPAZ,
                12L,
                1_500L);

        GemstoneMiningCorrelationGate.Correlation result =
                gate.snapshot(
                        GemstoneType.TOPAZ,
                        20_000L);

        assertEquals(
                0,
                result.directBreaks());

        assertFalse(
                result.pristineObserved());

        assertEquals(
                0L,
                result.pristineAmount());
    }

    @Test
    void consumedPristineCannotBeReused() {
        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate(
                        8_000L);

        gate.recordPristine(
                GemstoneType.JADE,
                20L,
                30_000L);

        assertTrue(
                gate.snapshot(
                        GemstoneType.JADE,
                        31_000L)
                        .pristineObserved());

        gate.consumePristine(
                GemstoneType.JADE,
                31_000L);

        GemstoneMiningCorrelationGate.Correlation result =
                gate.snapshot(
                        GemstoneType.JADE,
                        31_001L);

        assertFalse(
                result.pristineObserved());

        assertEquals(
                0L,
                result.pristineAmount());
    }

    @Test
    void resetClearsAllGemstones() {
        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate(
                        8_000L);

        gate.recordDirectBreak(
                GemstoneType.RUBY,
                40_000L);

        gate.recordPristine(
                GemstoneType.JADE,
                20L,
                40_000L);

        gate.reset();

        assertEquals(
                0,
                gate.snapshot(
                        GemstoneType.RUBY,
                        40_001L)
                        .directBreaks());

        assertFalse(
                gate.snapshot(
                        GemstoneType.JADE,
                        40_001L)
                        .pristineObserved());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GemstoneMiningCorrelationGate(
                        0L));

        GemstoneMiningCorrelationGate gate =
                new GemstoneMiningCorrelationGate();

        assertThrows(
                IllegalArgumentException.class,
                () -> gate.recordDirectBreak(
                        null,
                        1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> gate.recordPristine(
                        GemstoneType.RUBY,
                        0L,
                        1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> gate.snapshot(
                        GemstoneType.RUBY,
                        -1L));
    }
}
