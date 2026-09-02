package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneSignedSackIntegrationTest {
    @Test
    void removedGemstonesDoNotConsumePendingMiningBatch() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                10_000L);

        List<GemstoneGainDetector.SackObservation> removed =
                detector.inspectSackChangeText(
                        "Removed items:\n"
                                + "-500 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        10_500L);

        assertTrue(
                removed.isEmpty());

        assertEquals(
                1,
                detector.batchSnapshot(
                        GemstoneType.RUBY,
                        10_500L)
                        .directBreaks());

        List<GemstoneGainDetector.SackObservation> added =
                detector.inspectSackChangeText(
                        "Added items:\n"
                                + "+825 Rough Ruby Gemstone "
                                + "(Gemstones Sack)",
                        11_000L);

        assertEquals(
                1,
                added.size());

        assertTrue(
                added.get(0)
                        .batchDecision()
                        .shouldCredit());

        assertEquals(
                825L,
                added.get(0)
                        .batchDecision()
                        .creditAmount());
    }

    @Test
    void wrongSackDoesNotConsumePendingMiningBatch() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.recordDirectBreakSignal(
                GemstoneType.RUBY,
                20_000L);

        List<GemstoneGainDetector.SackObservation> wrongSack =
                detector.inspectSackChangeText(
                        "Added items:\n"
                                + "+900 Rough Ruby Gemstone "
                                + "(Mining Sack)",
                        20_500L);

        assertTrue(
                wrongSack.isEmpty());

        assertEquals(
                1,
                detector.batchSnapshot(
                        GemstoneType.RUBY,
                        20_500L)
                        .directBreaks());

        List<GemstoneGainDetector.SackObservation> correctSack =
                detector.inspectSackChangeText(
                        "Added items:\n"
                                + "+900 Rough Ruby Gemstone "
                                + "(Gemstone Sack)",
                        21_000L);

        assertEquals(
                1,
                correctSack.size());

        assertTrue(
                correctSack.get(0)
                        .batchDecision()
                        .shouldCredit());
    }

    @Test
    void unrelatedMiningItemsDoNotCreateGemstoneObservations() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        assertTrue(
                detector.inspectSackChangeText(
                        "Added items:\n"
                                + "+64 Gold Ingot "
                                + "(Mining Sack)\n"
                                + "+3 Enchanted Diamond "
                                + "(Enchanted Mining Sack)",
                        30_000L)
                        .isEmpty());
    }
}
