package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class RotClientCurrentSessionControllerTest {
    @Test
    void powderChestBatchIsCanonicalAndCountsOneChest() {
        RotClientCurrentSession session = new RotClientCurrentSession();

        boolean credited = session.creditPowderChestBatch(
                List.of(
                        new RotClientCurrentSession.PowderChestCredit(
                                "ROUGH_RUBY_GEM", "Rough Ruby Gemstone", 2L,
                                SessionSourceType.CHEST),
                        new RotClientCurrentSession.PowderChestCredit(
                                "GEMSTONE_POWDER", "Gemstone Powder", 296L,
                                SessionSourceType.CURRENCY)),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_000L);

        RotClientCurrentSessionConfig snapshot = session.snapshotConfig();
        assertTrue(credited);
        assertEquals(1L, snapshot.powderChestsOpened);
        assertEquals(2, snapshot.items.size());
        assertEquals(SessionSourceType.CHEST, snapshot.items.get(0).source());
        assertEquals(SessionSourceType.CURRENCY, snapshot.items.get(1).source());
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                snapshot.items.get(0).areaId());
    }

    @Test
    void powderChestBatchRejectsOverflowWithoutPartialMutation() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditItem(
                "GEMSTONE_POWDER", "Gemstone Powder", Long.MAX_VALUE,
                SessionSourceType.CURRENCY, null,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 900L);

        boolean credited = session.creditPowderChestBatch(
                List.of(
                        new RotClientCurrentSession.PowderChestCredit(
                                "ROUGH_RUBY_GEM", "Rough Ruby Gemstone", 2L,
                                SessionSourceType.CHEST),
                        new RotClientCurrentSession.PowderChestCredit(
                                "GEMSTONE_POWDER", "Gemstone Powder", 1L,
                                SessionSourceType.CURRENCY)),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_000L);

        RotClientCurrentSessionConfig snapshot = session.snapshotConfig();
        assertFalse(credited);
        assertEquals(0L, snapshot.powderChestsOpened);
        assertEquals(1, snapshot.items.size());
        assertEquals(Long.MAX_VALUE, snapshot.items.getFirst().quantity());
    }

    @Test
    void pausedCurrentSessionRejectsWholePowderChestBatch() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.pause(900L);

        assertFalse(session.creditPowderChestBatch(
                List.of(new RotClientCurrentSession.PowderChestCredit(
                        "GEMSTONE_POWDER", "Gemstone Powder", 80L,
                        SessionSourceType.CURRENCY)),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_000L));
        assertEquals(0L, session.snapshotConfig().powderChestsOpened);
        assertTrue(session.snapshotConfig().items.isEmpty());
    }

    @Test
    void sessionResetClearsPowderChestCountWithCanonicalRows() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        assertTrue(session.creditPowderChestBatch(
                List.of(new RotClientCurrentSession.PowderChestCredit(
                        "GEMSTONE_POWDER", "Gemstone Powder", 80L,
                        SessionSourceType.CURRENCY)),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_000L));

        assertTrue(session.clearCreditedItems(2_000L));
        assertEquals(0L, session.snapshotConfig().powderChestsOpened);
        assertTrue(session.snapshotConfig().items.isEmpty());
    }

    @Test
    void creditUnknownSurvivesAndMerges() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditUnknown("weird_drop", 5L, SessionSourceType.UNATTRIBUTED, 1000L);
        session.creditUnknown("weird_drop", 7L, SessionSourceType.UNATTRIBUTED, 1001L);
        RotClientCurrentSessionConfig snap = session.snapshotConfig();
        assertEquals(1, snap.items.size());
        assertEquals("WEIRD_DROP", snap.items.get(0).itemId());
        assertEquals(12L, snap.items.get(0).quantity());
        assertFalse(snap.items.get(0).known());
        assertEquals(SessionSourceType.UNATTRIBUTED.name(), snap.items.get(0).sourceType());
    }

    @Test
    void noteMagicFindRecordsDisplayedTotalWithoutInventingZero() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.noteMagicFind(-3, 1_000L);
        assertEquals(-1, session.snapshotConfig().lastMagicFind);
        session.noteMagicFind(42, 1_000L);
        assertEquals(42, session.snapshotConfig().lastMagicFind);
        assertEquals(1_000L, session.snapshotConfig().lastMagicFindAtMillis);
        session.noteMagicFind(42, 2_000L);
        assertEquals(1_000L, session.snapshotConfig().lastMagicFindAtMillis);
    }

    @Test
    void targetChangeClosesOpenSegment() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.onTargetChanged("GOLD", 1000L);
        session.onTargetChanged("DIAMOND", 2000L);
        RotClientCurrentSessionConfig snap = session.snapshotConfig();
        assertEquals("DIAMOND", snap.currentTargetId);
        assertEquals(2, snap.targetSegments.size());
        assertEquals(2000L, snap.targetSegments.get(0).endedAtMillis());
        assertTrue(snap.targetSegments.get(1).isOpen());
    }

    @Test
    void selectingTheCurrentTargetAgainDoesNotSplitItsSegment() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.onTargetChanged("GOLD", 1_000L);
        session.onTargetChanged("GOLD", 2_000L);

        RotClientCurrentSessionConfig snap = session.snapshotConfig();
        assertEquals(1, snap.targetSegments.size());
        assertEquals("GOLD", snap.targetSegments.getFirst().targetId());
        assertTrue(snap.targetSegments.getFirst().isOpen());
    }

    @Test
    void otherHudSummaryAlwaysAvailable() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        MiningHudOtherSummary summary = session.otherHudSummary();
        assertTrue(summary.analyticsActive());
        assertEquals(0L, summary.totalQuantity());
    }

    @Test
    void pauseAndResumeToggleState() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        assertTrue(session.isActive());
        session.pause(10L);
        assertTrue(session.isPaused());
        session.resume(20L);
        assertTrue(session.isActive());
    }

    @Test
    void pausedSessionRejectsTerminalItemCredits() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.pause(10L);

        session.creditItem(
                "HARD_STONE",
                "Hard Stone",
                64L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                11L);

        assertTrue(session.snapshotConfig().items.isEmpty());
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                session.snapshotConfig().currentAreaId);
    }

    @Test
    void sameResourceInDifferentAreasKeepsSeparateAttributionRows() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditItem(
                "HARD_STONE", "Hard Stone", 4L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 10L);
        session.creditItem(
                "HARD_STONE", "Hard Stone", 7L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 20L);

        RotClientCurrentSessionConfig snapshot = session.snapshotConfig();
        assertEquals(2, snapshot.items.size());
        assertEquals(SkyBlockArea.DWARVEN_MINES.id(),
                snapshot.items.get(0).areaId());
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                snapshot.items.get(1).areaId());
        assertEquals(11L, snapshot.items.stream()
                .mapToLong(
                        RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum());
    }

    @Test
    void unresolvedMergeNeverRetainsPartialResolvedGrossValue() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditItem(
                "DIAMOND", "Diamond", 4L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                20.0, 10L);
        session.creditItem(
                "DIAMOND", "Diamond", 3L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 11L);

        RotClientCurrentSessionConfig.SessionItemRecord row =
                session.snapshotConfig().items.getFirst();
        assertEquals(7L, row.quantity());
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                row.price());
        assertEquals(0.0, row.resolvedGrossValue(), 0.0);
    }

    @Test
    void pauseResumeCycleAccumulatesTotalPausedTime() {
        RotClientCurrentSession session = new RotClientCurrentSession();

        session.pause(1_000L);
        session.resume(4_000L);
        assertEquals(3_000L, session.snapshotConfig().totalPausedMillis);

        session.pause(10_000L);
        session.resume(10_500L);
        assertEquals(3_500L, session.snapshotConfig().totalPausedMillis);
    }

    @Test
    void activeDurationMatchesPureHelperAfterPauseResume() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;

        session.pause(started + 1_000L);
        session.resume(started + 4_000L);

        long now = started + 10_000L;
        assertEquals(7_000L, session.activeDurationMillis(now));
        assertEquals(
                RotClientCurrentSessionMath.activeDurationMillis(
                        session.snapshotConfig(), now),
                session.activeDurationMillis(now));
    }

    @Test
    void resumeWithoutAPriorPauseIsANoOp() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;

        session.resume(started + 5_000L);

        assertEquals(0L, session.snapshotConfig().totalPausedMillis);
        assertEquals(5_000L, session.activeDurationMillis(started + 5_000L));
    }

    @Test
    void offlineAutoPauseExcludesWallClockFromActiveDuration() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;

        // 10 min active
        long afterActive = started + 10 * 60_000L;
        assertEquals(10 * 60_000L, session.activeDurationMillis(afterActive));

        // 12 h offline
        session.pauseForOffline(afterActive);
        assertTrue(session.snapshotConfig().offlineAutoPaused);
        long afterOffline = afterActive + 12 * 3_600_000L;
        assertEquals(10 * 60_000L, session.activeDurationMillis(afterOffline));

        assertTrue(session.resumeAfterOffline(afterOffline));
        assertFalse(session.snapshotConfig().offlineAutoPaused);

        // +5 min active → 15 min total
        long afterResume = afterOffline + 5 * 60_000L;
        assertEquals(15 * 60_000L, session.activeDurationMillis(afterResume));
    }

    @Test
    void worldResetWhileOfflinePreventsPhantomPreviousAreaOnResume() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.onAreaChanged(SkyBlockArea.DWARVEN_MINES, started + 10L);
        session.pauseForOffline(started + 100L);

        session.onAreaChanged(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA, started + 110L);
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                session.snapshotConfig().currentAreaId);

        assertTrue(session.resumeAfterOffline(started + 200L));
        RotClientSessionFreeze frozen = session.freeze(started + 300L);
        assertEquals(1, frozen.areaSegments().size());
        assertEquals(SkyBlockArea.DWARVEN_MINES.id(),
                frozen.areaSegments().getFirst().areaId());
        assertEquals(started + 100L,
                frozen.areaSegments().getFirst().endedAtMillis());
    }

    @Test
    void explicitPauseIsNotAutoResumedAfterOfflineHook() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.pause(started + 1_000L);
        assertFalse(session.snapshotConfig().offlineAutoPaused);
        assertFalse(session.resumeAfterOffline(started + 2_000L));
        assertTrue(session.isPaused());
    }

    @Test
    void pauseExcludesWallClockFromTargetAndAreaSegments() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.onTargetChanged("GOLD", started + 10L);
        session.onAreaChanged(SkyBlockArea.DWARVEN_MINES, started + 10L);
        session.pause(started + 100L);
        session.onTargetChanged("DIAMOND", started + 150L);
        session.onAreaChanged(SkyBlockArea.CRYSTAL_HOLLOWS, started + 150L);
        session.resume(started + 200L);

        RotClientSessionFreeze frozen = session.freeze(started + 300L);
        assertEquals(2, frozen.targetSegments().size());
        assertEquals(started + 100L,
                frozen.targetSegments().get(0).endedAtMillis());
        assertEquals(started + 200L,
                frozen.targetSegments().get(1).startedAtMillis());
        assertEquals(started + 300L,
                frozen.targetSegments().get(1).endedAtMillis());
        assertEquals(2, frozen.areaSegments().size());
        assertEquals(started + 100L,
                frozen.areaSegments().get(0).endedAtMillis());
        assertEquals(started + 200L,
                frozen.areaSegments().get(1).startedAtMillis());
        assertEquals(100L, frozen.pausedDurationMillis());
    }

    @Test
    void shouldSyncOnValuationHeartbeatNotEngineLedgerAdvances() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        // Quantity ownership no longer follows engine event timestamps.
        assertFalse(session.shouldSync(1_000L, 50L));
        assertFalse(session.shouldSync(1_100L, -1L));
        assertTrue(session.shouldSync(4_000L, -1L));
    }

    @Test
    void startNewSessionBumpsDisplayNumber() {
        RotClientCurrentSession session =
                new RotClientCurrentSession(config -> true);
        session.creditUnknown("x", 1L, SessionSourceType.MOB, 1L);
        session.startNewSession(null, 50L);
        RotClientCurrentSessionConfig snap = session.snapshotConfig();
        assertEquals(2, snap.displayNumber);
        assertTrue(snap.items.isEmpty());
        assertEquals(RotClientCurrentSessionConfig.STATE_ACTIVE, snap.state);
        assertFalse(snap.offlineAutoPaused);
    }

    @Test
    void clearingCanonicalRowsReportsWhetherAnythingWasActuallyCleared() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        assertFalse(session.clearCreditedItems(started));

        session.creditUnknown(
                "PERSISTED_ROW", 2L, SessionSourceType.MOB, started + 1L);
        assertTrue(session.clearCreditedItems(started + 2L));
        assertTrue(session.snapshotConfig().items.isEmpty());
        assertFalse(session.clearCreditedItems(started + 3L));
    }

    @Test
    void failedAutosaveUsesRetryBackoffAndSurfacesWarning() {
        AtomicInteger attempts = new AtomicInteger();
        RotClientCurrentSession session = new RotClientCurrentSession(config -> {
            attempts.incrementAndGet();
            return false;
        });
        long started = session.snapshotConfig().startedAtMillis;
        session.creditUnknown(
                "DURABLE_ROW", 1L, SessionSourceType.MOB, started + 1L);

        long firstAttempt = started
                + RotClientCurrentSessionStore.SAVE_DEBOUNCE_MILLIS + 1L;
        session.flushIfDirty(firstAttempt, false);
        assertEquals(1, attempts.get());
        assertFalse(session.persistenceWarning().isBlank());

        session.flushIfDirty(firstAttempt + 1L, false);
        session.flushIfDirty(
                firstAttempt
                        + RotClientCurrentSessionStore
                        .SAVE_RETRY_BACKOFF_MILLIS - 1L,
                false);
        assertEquals(1, attempts.get());

        session.flushIfDirty(
                firstAttempt
                        + RotClientCurrentSessionStore
                        .SAVE_RETRY_BACKOFF_MILLIS,
                false);
        assertEquals(2, attempts.get());
    }
}
