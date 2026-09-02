package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneSackCorrelationEvaluatorTest {
    @Test
    void acceptsRoughAfterConfirmedDirectBreak() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        2,
                        400L,
                        false,
                        0L);

        GemstoneSackCorrelationEvaluator.Decision decision =
                GemstoneSackCorrelationEvaluator.evaluate(
                        GemstoneTier.ROUGH,
                        correlation);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .ROUGH_AFTER_DIRECT_BREAK,
                decision);

        assertTrue(
                decision.acceptedCandidate());
    }

    @Test
    void acceptsRoughWhenBreakAndPristineBothExist() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        1,
                        250L,
                        true,
                        20L);

        GemstoneSackCorrelationEvaluator.Decision decision =
                GemstoneSackCorrelationEvaluator.evaluate(
                        GemstoneTier.ROUGH,
                        correlation);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .ROUGH_AFTER_DIRECT_BREAK,
                decision);

        assertTrue(
                decision.acceptedCandidate());
    }

    @Test
    void rejectsManualRoughSackDeposit() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        0,
                        -1L,
                        false,
                        0L);

        GemstoneSackCorrelationEvaluator.Decision decision =
                GemstoneSackCorrelationEvaluator.evaluate(
                        GemstoneTier.ROUGH,
                        correlation);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .NO_DIRECT_BREAK,
                decision);

        assertFalse(
                decision.acceptedCandidate());
    }

    @Test
    void pristineWithoutDirectBreakDoesNotAcceptRough() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        0,
                        -1L,
                        true,
                        20L);

        GemstoneSackCorrelationEvaluator.Decision decision =
                GemstoneSackCorrelationEvaluator.evaluate(
                        GemstoneTier.ROUGH,
                        correlation);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .NO_DIRECT_BREAK,
                decision);

        assertFalse(
                decision.acceptedCandidate());
    }

    @Test
    void flawedIsCountedFromPristineInsteadOfSack() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        1,
                        200L,
                        true,
                        24L);

        GemstoneSackCorrelationEvaluator.Decision decision =
                GemstoneSackCorrelationEvaluator.evaluate(
                        GemstoneTier.FLAWED,
                        correlation);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .FLAWED_FROM_PRISTINE,
                decision);

        assertFalse(
                decision.acceptedCandidate());
    }

    @Test
    void unsupportedTiersAreNotAccepted() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        3,
                        100L,
                        true,
                        20L);

        for (GemstoneTier tier : new GemstoneTier[] {
                GemstoneTier.FINE,
                GemstoneTier.FLAWLESS,
                GemstoneTier.PERFECT
        }) {
            GemstoneSackCorrelationEvaluator.Decision decision =
                    GemstoneSackCorrelationEvaluator.evaluate(
                            tier,
                            correlation);

            assertEquals(
                    GemstoneSackCorrelationEvaluator.Decision
                            .UNSUPPORTED_TIER,
                    decision);

            assertFalse(
                    decision.acceptedCandidate());
        }
    }

    @Test
    void rejectsInvalidArguments() {
        GemstoneMiningCorrelationGate.Correlation correlation =
                new GemstoneMiningCorrelationGate.Correlation(
                        0,
                        -1L,
                        false,
                        0L);

        assertThrows(
                IllegalArgumentException.class,
                () -> GemstoneSackCorrelationEvaluator.evaluate(
                        null,
                        correlation));

        assertThrows(
                IllegalArgumentException.class,
                () -> GemstoneSackCorrelationEvaluator.evaluate(
                        GemstoneTier.ROUGH,
                        null));
    }
}
