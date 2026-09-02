package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneGainDetectorTest {
    @Test
    void storesParsedPristineSignal() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        PristineMessageParser.Reward reward =
                detector.inspectPlainMessage(
                        "PRISTINE! You found "
                                + "\uE010 Flawed Ruby "
                                + "Gemstone x20!",
                        10_000L);

        assertEquals(
                GemstoneType.RUBY,
                reward.gemstone());

        assertEquals(
                20L,
                reward.flawedAmount());

        GemstoneMiningCorrelationGate.Correlation result =
                detector.correlationSnapshot(
                        GemstoneType.RUBY,
                        10_500L);

        assertTrue(
                result.pristineObserved());

        assertEquals(
                20L,
                result.pristineAmount());

        assertEquals(
                0,
                result.directBreaks());
    }

    @Test
    void ignoresUnrelatedMessage() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        assertNull(
                detector.inspectPlainMessage(
                        "[Sacks] Added items",
                        20_000L));

        GemstoneMiningCorrelationGate.Correlation result =
                detector.correlationSnapshot(
                        GemstoneType.RUBY,
                        20_001L);

        assertFalse(
                result.pristineObserved());

        assertEquals(
                0L,
                result.pristineAmount());
    }

    @Test
    void keepsDifferentGemstonesIsolated() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Jade Gemstone x16!",
                30_000L);

        assertTrue(
                detector.correlationSnapshot(
                        GemstoneType.JADE,
                        30_100L)
                        .pristineObserved());

        assertFalse(
                detector.correlationSnapshot(
                        GemstoneType.RUBY,
                        30_100L)
                        .pristineObserved());
    }

    @Test
    void laterPristineMessageReplacesPreviousAmount() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Topaz Gemstone x12!",
                40_000L);

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Topaz Gemstone x24!",
                41_000L);

        GemstoneMiningCorrelationGate.Correlation result =
                detector.correlationSnapshot(
                        GemstoneType.TOPAZ,
                        41_100L);

        assertTrue(
                result.pristineObserved());

        assertEquals(
                24L,
                result.pristineAmount());
    }

    @Test
    void resetClearsPristineSignals() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        detector.inspectPlainMessage(
                "PRISTINE! You found "
                        + "Flawed Sapphire Gemstone x20!",
                50_000L);

        detector.reset();

        GemstoneMiningCorrelationGate.Correlation result =
                detector.correlationSnapshot(
                        GemstoneType.SAPPHIRE,
                        50_001L);

        assertFalse(
                result.pristineObserved());

        assertEquals(
                0L,
                result.pristineAmount());
    }

    @Test
    void rejectsNegativeTimestamp() {
        GemstoneGainDetector detector =
                new GemstoneGainDetector();

        assertThrows(
                IllegalArgumentException.class,
                () -> detector.inspectPlainMessage(
                        "PRISTINE! You found "
                                + "Flawed Ruby "
                                + "Gemstone x20!",
                        -1L));
    }
}
