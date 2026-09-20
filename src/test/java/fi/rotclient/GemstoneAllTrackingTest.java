package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The All Gemstones selection: per-gem ledgers plus a shared timeline. */
final class GemstoneAllTrackingTest {
    private static TrackerConfig allConfig() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(TrackerSelection.ALL_GEMSTONES);
        config.enabled = true;
        return config;
    }

    @Test
    void selectionIdentityAndCoverage() {
        TrackerSelection all = TrackerSelection.ALL_GEMSTONES;
        assertEquals("GEMSTONE_ALL", all.id());
        assertEquals("All Gemstones", all.displayName());
        assertTrue(all.isGemstone());
        assertTrue(all.isAllGemstones());
        assertFalse(all.isMaterial());
        assertTrue(all.supportsLiveTracking());
        assertNull(all.gemstone());
        assertEquals(List.of(GemstoneType.values()), all.gemstones());
        assertSame(all, TrackerSelection.fromId("GEMSTONE_ALL"));
        assertSame(all, TrackerSelection.findKnown("gemstone_all"));
        for (GemstoneType gemstone : GemstoneType.values()) {
            assertTrue(all.tracksGemstone(gemstone), gemstone.id());
        }
        assertFalse(all.tracksGemstone(null));
    }

    @Test
    void singleGemstoneStillTracksOnlyItself() {
        TrackerSelection ruby = TrackerSelection.RUBY;
        assertFalse(ruby.isAllGemstones());
        assertEquals(List.of(GemstoneType.RUBY), ruby.gemstones());
        assertTrue(ruby.tracksGemstone(GemstoneType.RUBY));
        assertFalse(ruby.tracksGemstone(GemstoneType.JADE));
        assertEquals(List.of(), TrackerSelection.GOLD.gemstones());
        assertFalse(TrackerSelection.GOLD.tracksGemstone(GemstoneType.RUBY));
    }

    @Test
    void everyGemstoneIsAcceptedIntoItsOwnLedger() {
        TrackerConfig config = allConfig();
        long at = 1_000L;
        for (GemstoneType gemstone : GemstoneType.values()) {
            assertTrue(GemstoneLiveAccounting.recordBlock(
                    config, gemstone, at++), gemstone.id());
            assertTrue(GemstoneLiveAccounting.recordGain(
                    config, gemstone, GemstoneTier.ROUGH,
                    10L + gemstone.ordinal()), gemstone.id());
        }
        for (GemstoneType gemstone : GemstoneType.values()) {
            GemstoneTrackerState state = config.gemstoneState(gemstone);
            assertEquals(1L, state.sessionBlocks, gemstone.id());
            assertEquals(10L + gemstone.ordinal(),
                    state.sessionLedger().quantity(GemstoneTier.ROUGH),
                    gemstone.id());
        }
    }

    @Test
    void blocksFeedTheSharedTimelineButItemsAreNotDuplicated() {
        TrackerConfig config = allConfig();
        GemstoneLiveAccounting.recordBlock(config, GemstoneType.RUBY, 1_000L);
        GemstoneLiveAccounting.recordBlock(config, GemstoneType.JADE, 2_000L);
        GemstoneLiveAccounting.recordGain(
                config, GemstoneType.RUBY, GemstoneTier.FLAWED, 3L);

        GemstoneTrackerState aggregate = config.gemstoneAggregateState();
        assertEquals(2L, aggregate.sessionBlocks);
        assertEquals(2_000L, aggregate.lastBreakEpochMillis);
        assertEquals(0L, aggregate.sessionLedger().totalItemCount(),
                "aggregate never stores items; they are summed on demand");
        assertSame(aggregate, config.selectedGemstoneTimeline());
    }

    @Test
    void sessionLedgerIsTheSumAcrossGemstones() {
        TrackerConfig config = allConfig();
        GemstoneLiveAccounting.recordGain(
                config, GemstoneType.RUBY, GemstoneTier.ROUGH, 100L);
        GemstoneLiveAccounting.recordGain(
                config, GemstoneType.JADE, GemstoneTier.ROUGH, 50L);
        GemstoneLiveAccounting.recordGain(
                config, GemstoneType.JADE, GemstoneTier.FLAWED, 2L);

        GemstoneLedger sum = config.selectedGemstoneSessionLedger();
        assertEquals(150L, sum.quantity(GemstoneTier.ROUGH));
        assertEquals(2L, sum.quantity(GemstoneTier.FLAWED));
        assertEquals(150L + 2L * 80L, sum.totalRoughEquivalent());

        // The sum is a copy: mutating it must not touch the real ledgers.
        sum.add(GemstoneTier.PERFECT, 9L);
        assertEquals(0L, config.gemstoneState(GemstoneType.RUBY)
                .sessionLedger().quantity(GemstoneTier.PERFECT));
    }

    @Test
    void singleGemstoneDoesNotTouchTheAggregate() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(TrackerSelection.RUBY);
        config.enabled = true;
        assertTrue(GemstoneLiveAccounting.recordBlock(
                config, GemstoneType.RUBY, 1_000L));
        assertFalse(GemstoneLiveAccounting.recordBlock(
                config, GemstoneType.JADE, 2_000L));
        assertEquals(0L, config.gemstoneAggregateState().sessionBlocks);
        assertSame(config.gemstoneState(GemstoneType.RUBY),
                config.selectedGemstoneTimeline());
    }

    @Test
    void disabledTrackerRecordsNothing() {
        TrackerConfig config = allConfig();
        config.enabled = false;
        assertFalse(GemstoneLiveAccounting.recordBlock(
                config, GemstoneType.RUBY, 1_000L));
        assertFalse(GemstoneLiveAccounting.recordGain(
                config, GemstoneType.RUBY, GemstoneTier.ROUGH, 5L));
    }

    @Test
    void resetClearsEveryGemstoneAndTheAggregate() {
        TrackerConfig config = allConfig();
        for (GemstoneType gemstone : GemstoneType.values()) {
            GemstoneLiveAccounting.recordBlock(config, gemstone, 1_000L);
            GemstoneLiveAccounting.recordGain(
                    config, gemstone, GemstoneTier.ROUGH, 7L);
        }
        config.gemstoneAggregateState().addActiveMillis(5_000L);

        config.resetSelectedSessionState();

        assertEquals(0L, config.gemstoneAggregateState().sessionBlocks);
        assertEquals(0L, config.gemstoneAggregateState().sessionActiveMillis);
        assertEquals(0L, config.selectedGemstoneSessionLedger()
                .totalItemCount());
        for (GemstoneType gemstone : GemstoneType.values()) {
            assertEquals(0L, config.gemstoneState(gemstone).sessionBlocks,
                    gemstone.id());
        }
    }

    @Test
    void resettingASingleGemstoneLeavesTheAggregateAndOthersAlone() {
        TrackerConfig config = new TrackerConfig();
        config.gemstoneAggregateState().recordBlock(500L);
        config.gemstoneState(GemstoneType.JADE).recordGain(
                GemstoneTier.ROUGH, 4L);
        config.setSelectedSelection(TrackerSelection.RUBY);

        config.resetSelectedSessionState();

        assertEquals(1L, config.gemstoneAggregateState().sessionBlocks);
        assertEquals(4L, config.gemstoneState(GemstoneType.JADE)
                .sessionLedger().quantity(GemstoneTier.ROUGH));
    }

    @Test
    void clearingBreakClocksZeroesEveryClock() {
        TrackerConfig config = allConfig();
        GemstoneLiveAccounting.recordBlock(config, GemstoneType.RUBY, 1_000L);
        config.clearGemstoneBreakClocks();
        assertEquals(0L, config.gemstoneAggregateState().lastBreakEpochMillis);
        for (GemstoneType gemstone : GemstoneType.values()) {
            assertEquals(0L,
                    config.gemstoneState(gemstone).lastBreakEpochMillis,
                    gemstone.id());
        }
    }

    @Test
    void timelineRejectsOresInsteadOfReturningTheWrongState() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(TrackerSelection.GOLD);
        assertThrows(IllegalStateException.class,
                config::selectedGemstoneTimeline);
    }

    @Test
    void aggregateAndPerGemstoneLedgersSurviveAStoreRoundTrip() {
        TrackerConfig config = allConfig();
        GemstoneLiveAccounting.recordBlock(config, GemstoneType.RUBY, 1_000L);
        GemstoneLiveAccounting.recordGain(
                config, GemstoneType.RUBY, GemstoneTier.FLAWED, 12L);
        config.gemstoneAggregateState().addActiveMillis(9_000L);

        TrackerConfig restored = TrackerStore.fromJson(
                TrackerStore.toJson(config));

        assertSame(TrackerSelection.ALL_GEMSTONES,
                restored.selectedSelection());
        assertEquals(1L, restored.gemstoneAggregateState().sessionBlocks);
        assertEquals(9_000L,
                restored.gemstoneAggregateState().sessionActiveMillis);
        assertEquals(12L, restored.gemstoneState(GemstoneType.RUBY)
                .sessionLedger().quantity(GemstoneTier.FLAWED));
    }

    @Test
    void configFromAnOlderFileGetsAFreshAggregate() {
        var json = TrackerStore.toJson(new TrackerConfig());
        json.remove("gemstoneAllState");
        TrackerConfig restored = TrackerStore.fromJson(json);
        assertEquals(0L, restored.gemstoneAggregateState().sessionBlocks);
    }
}
