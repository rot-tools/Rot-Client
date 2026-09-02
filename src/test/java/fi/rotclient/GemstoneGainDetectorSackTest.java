package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneGainDetectorSackTest {
    @Test
    void acceptsRoughAfterMatchingDirectBreak() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                10_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+825 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        10_500L);

        assertEquals(
                1,
                observations.size());

        GemstoneGainDetector.SackObservation observation =
                observations.get(0);

        assertEquals(
                GemstoneType.RUBY,
                observation.gemstone());

        assertEquals(
                GemstoneTier.ROUGH,
                observation.tier());

        assertEquals(
                825L,
                observation.amount());

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .ROUGH_AFTER_DIRECT_BREAK,
                observation.decision());

        assertTrue(
                observation.decision()
                        .acceptedCandidate());

        assertEquals(
                1,
                observation.correlation()
                        .directBreaks());
    }

    @Test
    void rejectsManualRoughDepositWithoutBreak() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+1,792 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        20_000L);

        assertEquals(
                1,
                observations.size());

        GemstoneGainDetector.SackObservation observation =
                observations.get(0);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .NO_DIRECT_BREAK,
                observation.decision());

        assertFalse(
                observation.decision()
                        .acceptedCandidate());

        assertEquals(
                0,
                observation.correlation()
                        .directBreaks());
    }

    @Test
    void flawedEntryUsesPristineDecision() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Ruby Gemstone x20!",
                30_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+20 Flawed Ruby Gemstone "
                                + "(Gemstones Sack)",
                        30_500L);

        assertEquals(
                1,
                observations.size());

        GemstoneGainDetector.SackObservation observation =
                observations.get(0);

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .FLAWED_FROM_PRISTINE,
                observation.decision());

        assertFalse(
                observation.decision()
                        .acceptedCandidate());

        assertTrue(
                observation.correlation()
                        .pristineObserved());

        assertEquals(
                20L,
                observation.correlation()
                        .pristineAmount());
    }

    @Test
    void keepsDifferentGemstoneSignalsIsolated() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                40_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+500 Rough Ruby Gemstone "
                                + "(Gemstones Sack)\n"
                                + "+700 Rough Jade Gemstone "
                                + "(Gemstones Sack)",
                        40_500L);

        assertEquals(
                2,
                observations.size());

        GemstoneGainDetector.SackObservation ruby =
                observations.get(0);

        GemstoneGainDetector.SackObservation jade =
                observations.get(1);

        assertEquals(
                GemstoneType.RUBY,
                ruby.gemstone());

        assertTrue(
                ruby.decision()
                        .acceptedCandidate());

        assertEquals(
                GemstoneType.JADE,
                jade.gemstone());

        assertFalse(
                jade.decision()
                        .acceptedCandidate());
    }

    @Test
    void parserHandlesPrivateUseSackIcon() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                50_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+104 \uE010 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        50_500L);

        assertEquals(
                1,
                observations.size());

        assertEquals(
                GemstoneType.RUBY,
                observations.get(0)
                        .gemstone());

        assertEquals(
                104L,
                observations.get(0)
                        .amount());
    }

    @Test
    void unsupportedTierRemainsRejected() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.TOPAZ,
                60_000L);

        List<GemstoneGainDetector.SackObservation> observations =
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+2 Fine Topaz Gemstone "
                                + "(Gemstones Sack)",
                        60_500L);

        assertEquals(
                1,
                observations.size());

        assertEquals(
                GemstoneSackCorrelationEvaluator.Decision
                        .UNSUPPORTED_TIER,
                observations.get(0)
                        .decision());

        assertFalse(
                observations.get(0)
                        .decision()
                        .acceptedCandidate());
    }

    @Test
    void emptyAndUnrelatedTextReturnNoObservations() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        assertTrue(
                detector.inspectSackAddedText(
                        null,
                        70_000L)
                        .isEmpty());

        assertTrue(
                detector.inspectSackAddedText(
                        "",
                        70_000L)
                        .isEmpty());

        assertTrue(
                detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+5 Hard Stone "
                                + "(Mining Sack)",
                        70_000L)
                        .isEmpty());
    }

    @Test
    void rejectsNegativeTimestamp() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        assertThrows(
                IllegalArgumentException.class,
                () -> detector.inspectSackAddedText(
                        "Added items:\n"
                                + "+100 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        -1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> detector.recordDirectBreakSignal(
                        GemstoneType.RUBY,
                        -1L));
    }
}
