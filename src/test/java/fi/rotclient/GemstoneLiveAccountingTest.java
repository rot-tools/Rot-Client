package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneLiveAccountingTest {
    @Test
    void recordsSelectedGemstoneBlockAndGain() {
        TrackerConfig config =
                new TrackerConfig();

        config.setSelectedSelection(
                TrackerSelection.RUBY);

        config.enabled =
                true;

        assertTrue(
                GemstoneLiveAccounting.recordBlock(
                        config,
                        GemstoneType.RUBY,
                        10_000L));

        assertTrue(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        1_672L));

        assertTrue(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.RUBY,
                        GemstoneTier.FLAWED,
                        97L));

        GemstoneTrackerState state =
                config.gemstoneState(
                        GemstoneType.RUBY);

        assertEquals(
                1L,
                state.sessionBlocks);

        assertEquals(
                1L,
                state.totalBlocks);

        assertEquals(
                1_672L,
                state.sessionLedger()
                        .quantity(
                                GemstoneTier.ROUGH));

        assertEquals(
                97L,
                state.sessionLedger()
                        .quantity(
                                GemstoneTier.FLAWED));

        assertEquals(
                1_672L + 97L * 80L,
                state.sessionLedger()
                        .totalRoughEquivalent());
    }

    @Test
    void rejectsDifferentGemstone() {
        TrackerConfig config =
                new TrackerConfig();

        config.setSelectedSelection(
                TrackerSelection.RUBY);

        config.enabled =
                true;

        assertFalse(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.TOPAZ,
                        GemstoneTier.ROUGH,
                        56L));

        assertEquals(
                0L,
                config.gemstoneState(
                        GemstoneType.TOPAZ)
                        .sessionLedger()
                        .totalItemCount());
    }

    @Test
    void rejectsEventsWhileTrackerIsDisabled() {
        TrackerConfig config =
                new TrackerConfig();

        config.setSelectedSelection(
                TrackerSelection.RUBY);

        config.enabled =
                false;

        assertFalse(
                GemstoneLiveAccounting.recordBlock(
                        config,
                        GemstoneType.RUBY,
                        20_000L));

        assertFalse(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        500L));
    }

    @Test
    void rejectsGemstoneEventsForMaterialSelection() {
        TrackerConfig config =
                new TrackerConfig();

        config.setSelectedSelection(
                TrackerSelection.GOLD);

        config.enabled =
                true;

        assertFalse(
                GemstoneLiveAccounting.isActive(
                        config));

        assertFalse(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        500L));
    }

    @Test
    void rejectsInvalidAmountsAndArguments() {
        TrackerConfig config =
                new TrackerConfig();

        config.setSelectedSelection(
                TrackerSelection.RUBY);

        config.enabled =
                true;

        assertFalse(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        0L));

        assertFalse(
                GemstoneLiveAccounting.recordGain(
                        config,
                        GemstoneType.RUBY,
                        null,
                        1L));

        assertFalse(
                GemstoneLiveAccounting.recordGain(
                        config,
                        null,
                        GemstoneTier.ROUGH,
                        1L));

        assertFalse(
                GemstoneLiveAccounting.recordBlock(
                        config,
                        GemstoneType.RUBY,
                        -1L));
    }

    @Test
    void gemstoneSelectionsSupportLiveTracking() {
        for (TrackerSelection selection :
                TrackerSelection.values()) {
            assertTrue(
                    selection.supportsLiveTracking());
        }
    }
}
