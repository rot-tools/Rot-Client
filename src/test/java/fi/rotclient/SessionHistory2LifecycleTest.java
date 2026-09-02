package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SessionHistory2LifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void freezeClosesLifecycleAndPreservesCanonicalRowsAndValuation() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;

        session.onTargetChanged("GOLD", started);
        session.onAreaChanged(SkyBlockArea.DWARVEN_MINES, started + 100L);
        session.creditItem(
                "GOLD_INGOT",
                "Gold Ingot",
                4L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 500L);
        session.pause(started + 1_000L);
        session.resume(started + 4_000L);
        session.onTargetChanged("DIAMOND", started + 5_000L);
        session.onAreaChanged(SkyBlockArea.CRYSTAL_HOLLOWS, started + 6_000L);
        session.creditItem(
                "MOB_DROP",
                "Mob Drop",
                2L,
                SessionSourceType.MOB,
                null,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED,
                0.0,
                started + 6_100L);
        session.creditItem(
                "CHEST_DROP",
                "Chest Drop",
                3L,
                SessionSourceType.CHEST,
                null,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED,
                0.0,
                started + 6_200L);
        session.creditItem(
                "GEMSTONE_POWDER",
                "Gemstone Powder",
                50L,
                SessionSourceType.CURRENCY,
                null,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED,
                0.0,
                started + 6_300L);
        session.refreshPerResourceValuation(
                MiningSessionPriceBook.available(
                        started + 7_000L,
                        Map.of("GOLD_INGOT", new BigDecimal("2.5"))),
                started + 7_100L);

        RotClientSessionFreeze frozen = session.freeze(started + 10_000L);

        assertEquals(3_000L, frozen.pausedDurationMillis());
        assertEquals(7_000L, frozen.activeDurationMillis());
        assertEquals(3, frozen.targetSegments().size());
        assertEquals(started + 10_000L,
                frozen.targetSegments().get(2).endedAtMillis());
        assertEquals(3, frozen.areaSegments().size());
        assertEquals(started + 10_000L,
                frozen.areaSegments().get(2).endedAtMillis());
        assertEquals(4, frozen.itemRows().size());
        assertEquals(10.0,
                frozen.itemRows().stream()
                        .filter(row -> row.itemId().equals("GOLD_INGOT"))
                        .findFirst().orElseThrow().resolvedGrossValue(),
                0.0001);
        assertEquals(started + 7_000L,
                frozen.priceBookObservedAtMillis().orElseThrow());
        assertThrows(UnsupportedOperationException.class,
                () -> frozen.itemRows().clear());
        assertEquals(
                RotClientSessionFreeze.SourceReadiness.CANONICAL,
                frozen.sourceReadiness(SessionSourceType.MINING));
        assertEquals(
                RotClientSessionFreeze.SourceReadiness.SCHEMA_READY,
                frozen.sourceReadiness(SessionSourceType.MOB));
        assertEquals(
                RotClientSessionFreeze.SourceReadiness.CANONICAL,
                frozen.sourceReadiness(SessionSourceType.CHEST));
        assertEquals(
                RotClientSessionFreeze.SourceReadiness.CANONICAL,
                frozen.sourceReadiness(SessionSourceType.CURRENCY));
    }

    @Test
    void startNewArchivesBeforeResetAndArchiveFailureKeepsCurrentSession() {
        RotClientCurrentSession session =
                new RotClientCurrentSession(config -> true);
        long started = session.snapshotConfig().startedAtMillis;
        session.creditUnknown("PERSIST_ME", 7L, SessionSourceType.MOB,
                started + 1L);
        String priorId = session.sessionId();

        assertFalse(session.startNewSession(frozen -> false, started + 100L));
        assertEquals(priorId, session.sessionId());
        assertEquals(7L, session.snapshotConfig().items.get(0).quantity());

        AtomicReference<RotClientSessionFreeze> archived =
                new AtomicReference<>();
        assertTrue(session.startNewSession(frozen -> {
            archived.set(frozen);
            return true;
        }, started + 200L));

        assertEquals(priorId, archived.get().sourceSessionIdForDedupe());
        assertEquals(7L, archived.get().itemRows().get(0).quantity());
        assertNotEquals(priorId, session.sessionId());
        assertEquals(2, session.displayNumber());
        assertTrue(session.snapshotConfig().items.isEmpty());
    }

    @Test
    void history2ArchiveRoundTripsFrozenCanonicalLifecycle() throws Exception {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.onTargetChanged("GOLD", started);
        session.onAreaChanged(SkyBlockArea.DWARVEN_MINES, started + 10L);
        session.creditItem(
                "HARD_STONE",
                "Hard Stone",
                12L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 20L);
        session.pause(started + 100L);
        RotClientSessionFreeze frozen = session.freeze(started + 500L);

        Path history = tempDir.resolve("history.json");
        MiningSessionHistoryStore.SaveResult saved =
                MiningSessionHistoryStore.saveCurrentSession(
                        history,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        frozen,
                        "hlifecycle2000001");

        assertEquals(MiningSessionHistoryStore.SaveOutcome.SAVED,
                saved.outcome());
        String json = Files.readString(history);
        assertTrue(json.contains("\"currentSession\""));
        assertFalse(json.contains(frozen.sourceSessionIdForDedupe()));
        assertFalse(json.toLowerCase().contains("sessionid"));
        MiningSessionHistoryRecord record = saved.record().orElseThrow();
        assertEquals(2, record.schemaVersion());
        RotClientSessionFreeze restored =
                record.currentSessionFreeze().orElseThrow();
        assertEquals(frozen.activeDurationMillis(),
                restored.activeDurationMillis());
        assertEquals(frozen.pausedDurationMillis(),
                restored.pausedDurationMillis());
        assertEquals(1, restored.targetSegments().size());
        assertEquals(1, restored.areaSegments().size());
        assertEquals(12L, restored.itemRows().get(0).quantity());

        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(history);
        assertTrue(loaded.available());
        assertEquals(2, loaded.document().schemaVersion());
        RotClientSessionFreeze loadedFreeze = loaded.document().sessions()
                .get(0).currentSessionFreeze().orElseThrow();
        assertEquals(restored.itemRows(), loadedFreeze.itemRows());
        assertEquals(restored.targetSegments(), loadedFreeze.targetSegments());
        assertEquals(restored.areaSegments(), loadedFreeze.areaSegments());
        assertEquals(100L,
                MiningSessionHistorySummaries.durationMillis(record));
    }

    @Test
    void sameItemInTwoAreasRoundTripsAsTwoCanonicalRows() throws Exception {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.creditItem(
                "HARD_STONE",
                "Hard Stone",
                4L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 1L);
        session.creditItem(
                "HARD_STONE",
                "Hard Stone",
                6L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 2L);

        RotClientSessionFreeze frozen = session.freeze(started + 10L);
        assertEquals(2, frozen.itemRows().size());

        Path history = tempDir.resolve("area-rows.json");
        MiningSessionHistoryStore.SaveResult saved =
                MiningSessionHistoryStore.saveCurrentSession(
                        history,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        frozen,
                        "harearows000001");
        assertEquals(MiningSessionHistoryStore.SaveOutcome.SAVED,
                saved.outcome());

        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(history);
        assertTrue(loaded.available());
        RotClientSessionFreeze restored = loaded.document().sessions()
                .getFirst().currentSessionFreeze().orElseThrow();
        assertEquals(frozen.itemRows(), restored.itemRows());
        assertEquals(2, restored.itemRows().stream()
                .map(RotClientCurrentSessionConfig.SessionItemRecord::areaId)
                .distinct()
                .count());
    }

    @Test
    void moreThanFiveHundredTargetAndAreaSegmentsRemainArchivable()
            throws Exception {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        long started = config.startedAtMillis;
        config.targetSegments = new java.util.ArrayList<>();
        config.areaSegments = new java.util.ArrayList<>();
        for (int index = 0; index < 513; index++) {
            long from = started + index * 2L;
            long to = from + 1L;
            config.targetSegments.add(
                    new RotClientCurrentSessionConfig.TargetSegment(
                            index % 2 == 0 ? "GOLD" : "DIAMOND",
                            from,
                            to));
            config.areaSegments.add(
                    new RotClientCurrentSessionConfig.AreaSegment(
                            index % 2 == 0
                                    ? SkyBlockArea.DWARVEN_MINES.id()
                                    : SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                            from,
                            to));
        }
        long stopped = started + 1_100L;
        RotClientSessionFreeze frozen =
                RotClientSessionFreeze.fromCurrentSession(config, stopped);
        assertEquals(513, frozen.targetSegments().size());
        assertEquals(513, frozen.areaSegments().size());

        Path history = tempDir.resolve("many-segments.json");
        MiningSessionHistoryStore.SaveResult saved =
                MiningSessionHistoryStore.saveCurrentSession(
                        history,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        frozen,
                        "hmanysegments001");
        assertEquals(MiningSessionHistoryStore.SaveOutcome.SAVED,
                saved.outcome());
        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(history);
        assertTrue(loaded.available());
        RotClientSessionFreeze restored = loaded.document().sessions()
                .getFirst().currentSessionFreeze().orElseThrow();
        assertEquals(513, restored.targetSegments().size());
        assertEquals(513, restored.areaSegments().size());
    }

    @Test
    void powderChestCountAndCanonicalRowsSurviveArchiveRoundTrip()
            throws Exception {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        assertTrue(session.creditPowderChestBatch(
                List.of(
                        new RotClientCurrentSession.PowderChestCredit(
                                "ROUGH_RUBY_GEM", "Rough Ruby Gemstone", 2L,
                                SessionSourceType.CHEST),
                        new RotClientCurrentSession.PowderChestCredit(
                                "GEMSTONE_POWDER", "Gemstone Powder", 296L,
                                SessionSourceType.CURRENCY)),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                started + 10L));

        Path history = tempDir.resolve("powder-chest-round-trip.json");
        MiningSessionHistoryStore.SaveResult saved =
                MiningSessionHistoryStore.saveCurrentSession(
                        history,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        session.freeze(started + 100L),
                        "hpowderchest001");

        assertEquals(MiningSessionHistoryStore.SaveOutcome.SAVED,
                saved.outcome());
        RotClientSessionFreeze loaded = MiningSessionHistoryStore.load(history)
                .document().sessions().getFirst()
                .currentSessionFreeze().orElseThrow();
        assertEquals(1L, loaded.powderChestsOpened());
        assertEquals(2, loaded.itemRows().size());
    }

    @Test
    void schemaReadyMobChestAndCurrencyRowsRemainDistinctAfterArchive()
            throws Exception {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        for (SessionSourceType source : new SessionSourceType[]{
                SessionSourceType.MOB,
                SessionSourceType.CHEST,
                SessionSourceType.CURRENCY}) {
            session.creditItem(
                    source.name() + "_ROW",
                    source.name() + " row",
                    source.ordinal() + 1L,
                    source,
                    null,
                    SkyBlockArea.DWARVEN_MINES,
                    RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED,
                    0.0,
                    started + source.ordinal());
        }
        Path history = tempDir.resolve("source-readiness.json");
        MiningSessionHistoryStore.SaveResult result =
                MiningSessionHistoryStore.saveCurrentSession(
                        history,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        session.freeze(started + 100L),
                        "hsourceready0001");
        assertEquals(MiningSessionHistoryStore.SaveOutcome.SAVED,
                result.outcome());

        MiningSessionHistoryDocument decoded =
                MiningSessionHistoryCodec.fromJsonString(
                        Files.readString(history));
        MiningSessionHistoryRecord record = decoded.sessions().get(0);
        RotClientSessionFreeze loaded = record
                .currentSessionFreeze().orElseThrow();
        assertEquals(3, loaded.itemRows().size());
        assertNotNull(loaded.itemRows().stream()
                .filter(row -> row.source() == SessionSourceType.MOB)
                .findFirst().orElse(null));
        assertNotNull(loaded.itemRows().stream()
                .filter(row -> row.source() == SessionSourceType.CHEST)
                .findFirst().orElse(null));
        assertNotNull(loaded.itemRows().stream()
                .filter(row -> row.source() == SessionSourceType.CURRENCY)
                .findFirst().orElse(null));
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED,
                loaded.itemRows().stream()
                        .filter(row -> row.source() == SessionSourceType.CURRENCY)
                        .findFirst().orElseThrow().price());
        assertEquals(1, MiningSessionHistorySummaries.sourceRowCount(
                record, SessionSourceType.MOB));
        assertTrue(MiningSessionHistorySummaries
                .chestAndMobQuantities(record).containsKey("MOB_ROW"));
        assertTrue(MiningSessionHistorySummaries
                .chestAndMobQuantities(record).containsKey("CHEST_ROW"));
        assertThrows(UnsupportedOperationException.class,
                () -> MiningSessionHistorySummaries
                        .chestAndMobQuantities(record).clear());
    }

    @Test
    void freezeMarksExpiredValuationStaleWithoutMutatingLiveRows() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.creditItem(
                "GOLD_INGOT",
                "Gold Ingot",
                4L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 1L);
        long observed = started + 10L;
        session.refreshPerResourceValuation(
                MiningSessionPriceBook.available(
                        observed,
                        Map.of("GOLD_INGOT", new BigDecimal("2.5"))),
                observed + 1L);

        RotClientSessionFreeze frozen = session.freeze(
                observed + MiningSessionPriceBook.STALE_THRESHOLD_MILLIS);

        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                session.snapshotConfig().items.get(0).price());
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.STALE,
                frozen.itemRows().get(0).price());
        assertEquals(10.0,
                frozen.itemRows().get(0).resolvedGrossValue(),
                0.0001);
    }

    @Test
    void freezeOmitsZeroDurationTargetAndAreaSegments() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        long boundary = config.startedAtMillis;
        config.currentTargetId = "GOLD";
        config.targetSegments = java.util.List.of(
                new RotClientCurrentSessionConfig.TargetSegment(
                        "GOLD", boundary, 0L));
        config.currentAreaId = SkyBlockArea.DWARVEN_MINES.id();
        config.areaSegments = java.util.List.of(
                new RotClientCurrentSessionConfig.AreaSegment(
                        SkyBlockArea.DWARVEN_MINES.id(), boundary, 0L));

        RotClientSessionFreeze frozen =
                RotClientSessionFreeze.fromCurrentSession(config, boundary);

        assertTrue(frozen.targetSegments().isEmpty());
        assertTrue(frozen.areaSegments().isEmpty());
    }
}
