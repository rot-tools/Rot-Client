package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneMiningBatchTrackerTest {
    @Test
    void roughCreditConsumesDirectBreaksOnce() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker(
                        8_000L,
                        30_000L);

        tracker.recordDirectBreak(
                GemstoneType.RUBY,
                10_000L);

        tracker.recordDirectBreak(
                GemstoneType.RUBY,
                10_100L);

        GemstoneMiningBatchTracker.SackDecision first =
                tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        825L,
                        10_500L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.ROUGH_CREDIT,
                first.outcome());

        assertTrue(
                first.shouldCredit());

        assertEquals(
                825L,
                first.creditAmount());

        assertEquals(
                2,
                first.consumedDirectBreaks());

        GemstoneMiningBatchTracker.SackDecision second =
                tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        500L,
                        10_600L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.NO_DIRECT_BREAK,
                second.outcome());

        assertFalse(
                second.shouldCredit());
    }

    @Test
    void manualRoughSackChangeIsRejected() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        GemstoneMiningBatchTracker.SackDecision decision =
                tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        1_792L,
                        20_000L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.NO_DIRECT_BREAK,
                decision.outcome());

        assertFalse(
                decision.shouldCredit());
    }

    @Test
    void pristineCreatesExactImmediateShadowCredit() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        GemstoneMiningBatchTracker.PristineCredit credit =
                tracker.recordPristine(
                        GemstoneType.RUBY,
                        20L,
                        30_000L);

        assertEquals(
                GemstoneType.RUBY,
                credit.gemstone());

        assertEquals(
                GemstoneTier.FLAWED,
                credit.tier());

        assertEquals(
                20L,
                credit.amount());

        GemstoneMiningBatchTracker.Snapshot snapshot =
                tracker.snapshot(
                        GemstoneType.RUBY,
                        30_100L);

        assertEquals(
                20L,
                snapshot.pendingPristineAmount());
    }

    @Test
    void flawedSackEntryConfirmsWithoutSecondCredit() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordPristine(
                GemstoneType.JADE,
                126L,
                40_000L);

        GemstoneMiningBatchTracker.SackDecision decision =
                tracker.evaluateSack(
                        GemstoneType.JADE,
                        GemstoneTier.FLAWED,
                        126L,
                        40_500L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.FLAWED_CONFIRMED,
                decision.outcome());

        assertFalse(
                decision.shouldCredit());

        assertEquals(
                126L,
                decision.matchedPristineAmount());

        assertEquals(
                0L,
                decision.remainingPristineAmount());
    }

    @Test
    void partialFlawedConfirmationKeepsRemainder() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordPristine(
                GemstoneType.TOPAZ,
                100L,
                50_000L);

        GemstoneMiningBatchTracker.SackDecision decision =
                tracker.evaluateSack(
                        GemstoneType.TOPAZ,
                        GemstoneTier.FLAWED,
                        40L,
                        50_500L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.FLAWED_CONFIRMED,
                decision.outcome());

        assertEquals(
                40L,
                decision.matchedPristineAmount());

        assertEquals(
                60L,
                decision.remainingPristineAmount());
    }

    @Test
    void differentGemstonesRemainIsolated() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordDirectBreak(
                GemstoneType.RUBY,
                60_000L);

        GemstoneMiningBatchTracker.SackDecision ruby =
                tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        400L,
                        60_500L);

        GemstoneMiningBatchTracker.SackDecision jade =
                tracker.evaluateSack(
                        GemstoneType.JADE,
                        GemstoneTier.ROUGH,
                        700L,
                        60_500L);

        assertTrue(
                ruby.shouldCredit());

        assertFalse(
                jade.shouldCredit());
    }

    @Test
    void expiredBreakCannotAuthorizeRoughCredit() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker(
                        2_500L,
                        30_000L);

        tracker.recordDirectBreak(
                GemstoneType.AMETHYST,
                70_000L);

        GemstoneMiningBatchTracker.SackDecision decision =
                tracker.evaluateSack(
                        GemstoneType.AMETHYST,
                        GemstoneTier.ROUGH,
                        300L,
                        72_501L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.NO_DIRECT_BREAK,
                decision.outcome());

        assertFalse(
                decision.shouldCredit());
    }

    @Test
    void unsupportedTiersNeverCreateMiningCredit() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordDirectBreak(
                GemstoneType.SAPPHIRE,
                80_000L);

        for (GemstoneTier tier : new GemstoneTier[] {
                GemstoneTier.FINE,
                GemstoneTier.FLAWLESS,
                GemstoneTier.PERFECT
        }) {
            GemstoneMiningBatchTracker.SackDecision decision =
                    tracker.evaluateSack(
                            GemstoneType.SAPPHIRE,
                            tier,
                            1L,
                            80_500L);

            assertEquals(
                    GemstoneMiningBatchTracker.Outcome.UNSUPPORTED_TIER,
                    decision.outcome());

            assertFalse(
                    decision.shouldCredit());
        }
    }

    @Test
    void resetClearsAllSignals() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordDirectBreak(
                GemstoneType.RUBY,
                90_000L);

        tracker.recordPristine(
                GemstoneType.RUBY,
                20L,
                90_000L);

        tracker.reset();

        GemstoneMiningBatchTracker.Snapshot snapshot =
                tracker.snapshot(
                        GemstoneType.RUBY,
                        90_100L);

        assertEquals(
                0,
                snapshot.directBreaks());

        assertEquals(
                0L,
                snapshot.pendingPristineAmount());
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GemstoneMiningBatchTracker(
                        0L,
                        30_000L));

        assertThrows(
                IllegalArgumentException.class,
                () -> new GemstoneMiningBatchTracker(
                        8_000L,
                        0L));

        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.recordDirectBreak(
                        null,
                        1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.recordPristine(
                        GemstoneType.RUBY,
                        0L,
                        1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.evaluateSack(
                        GemstoneType.RUBY,
                        null,
                        1L,
                        1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        1L,
                        -1L));
    }
    @Test
    void defaultBreakWindowAllowsDelayedSackFlush() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordDirectBreak(
                GemstoneType.RUBY,
                10_000L);

        GemstoneMiningBatchTracker.SackDecision decision =
                tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        1_131L,
                        20_281L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.ROUGH_CREDIT,
                decision.outcome());

        assertTrue(
                decision.shouldCredit());

        assertEquals(
                1_131L,
                decision.creditAmount());

        assertEquals(
                1,
                decision.consumedDirectBreaks());
    }

    @Test
    void defaultPristineWindowAllowsDelayedConfirmation() {
        GemstoneMiningBatchTracker tracker =
                new GemstoneMiningBatchTracker();

        tracker.recordPristine(
                GemstoneType.RUBY,
                14L,
                30_000L);

        GemstoneMiningBatchTracker.SackDecision decision =
                tracker.evaluateSack(
                        GemstoneType.RUBY,
                        GemstoneTier.FLAWED,
                        14L,
                        71_743L);

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.FLAWED_CONFIRMED,
                decision.outcome());

        assertFalse(
                decision.shouldCredit());

        assertEquals(
                14L,
                decision.matchedPristineAmount());

        assertEquals(
                0L,
                decision.remainingPristineAmount());
    }
}
