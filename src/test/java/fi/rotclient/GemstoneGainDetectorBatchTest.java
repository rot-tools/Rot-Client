package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneGainDetectorBatchTest {
    @Test
    void roughSackCreditConsumesDetectorBatch() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                10_000L);

        List<GemstoneGainDetector.SackObservation> first =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+825 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        10_500L);

        assertEquals(
                1,
                first.size());

        assertTrue(
                first.get(0)
                        .batchDecision()
                        .shouldCredit());

        assertEquals(
                825L,
                first.get(0)
                        .batchDecision()
                        .creditAmount());

        List<GemstoneGainDetector.SackObservation> second =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+200 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        10_600L);

        assertEquals(
                1,
                second.size());

        assertFalse(
                second.get(0)
                        .batchDecision()
                        .shouldCredit());

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.NO_DIRECT_BREAK,
                second.get(0)
                        .batchDecision()
                        .outcome());
    }

    @Test
    void flawedSackEntryOnlyConfirmsPristineCredit() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Ruby Gemstone x28!",
                20_000L);

        GemstoneMiningBatchTracker.Snapshot before =
                detector.batchSnapshot(
                        GemstoneType.RUBY,
                        20_100L);

        assertEquals(
                28L,
                before.pendingPristineAmount());

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+28 Flawed Ruby Gemstone "
                                + "(Gemstones Sack)",
                        20_500L);

        assertEquals(
                1,
                observations.size());

        GemstoneMiningBatchTracker.SackDecision decision =
                observations.get(0)
                        .batchDecision();

        assertFalse(
                decision.shouldCredit());

        assertEquals(
                GemstoneMiningBatchTracker.Outcome.FLAWED_CONFIRMED,
                decision.outcome());

        assertEquals(
                28L,
                decision.matchedPristineAmount());

        assertEquals(
                0L,
                decision.remainingPristineAmount());
    }

    @Test
    void unrelatedGemstoneInSameSackMessageIsRejected() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                30_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+1,339 Rough Ruby Gemstone "
                                + "(Gemstones Sack)\n"
                                + "+56 Rough Topaz Gemstone "
                                + "(Gemstones Sack)",
                        30_500L);

        assertEquals(
                2,
                observations.size());

        GemstoneGainDetector.SackObservation ruby =
                observations.get(0);

        GemstoneGainDetector.SackObservation topaz =
                observations.get(1);

        assertEquals(
                GemstoneType.RUBY,
                ruby.gemstone());

        assertTrue(
                ruby.batchDecision()
                        .shouldCredit());

        assertEquals(
                GemstoneType.TOPAZ,
                topaz.gemstone());

        assertFalse(
                topaz.batchDecision()
                        .shouldCredit());
    }

    @Test
    void duplicateLinesAreAggregatedBeforeConsumption() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.JADE,
                40_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+100 Rough Jade Gemstone "
                                + "(Gemstones Sack)\n"
                                + "+250 Rough Jade Gemstone "
                                + "(Gemstones Sack)",
                        40_500L);

        assertEquals(
                1,
                observations.size());

        assertEquals(
                350L,
                observations.get(0)
                        .amount());

        assertTrue(
                observations.get(0)
                        .batchDecision()
                        .shouldCredit());

        assertEquals(
                350L,
                observations.get(0)
                        .batchDecision()
                        .creditAmount());
    }

    @Test
    void resetClearsDetectorBatchSignals() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                50_000L);

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Ruby Gemstone x20!",
                50_000L);

        detector.reset();

        GemstoneMiningBatchTracker.Snapshot snapshot =
                detector.batchSnapshot(
                        GemstoneType.RUBY,
                        50_100L);

        assertEquals(
                0,
                snapshot.directBreaks());

        assertEquals(
                0L,
                snapshot.pendingPristineAmount());
    }
}
