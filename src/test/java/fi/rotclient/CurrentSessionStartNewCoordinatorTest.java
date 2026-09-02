package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CurrentSessionStartNewCoordinatorTest {
    @TempDir
    Path tempDir;

    @Test
    void currentStoreFailureRollsBackNewHistoryRecordAndKeepsOriginalSession() {
        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> false);
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "KEEP_ME", 7L, SessionSourceType.MOB, started + 1L);
        String originalId = current.sessionId();
        MiningSessionHistoryController history = history(current);

        CurrentSessionStartNewCoordinator.Result result =
                CurrentSessionStartNewCoordinator.start(
                        current, history, started + 100L);

        assertSame(
                CurrentSessionStartNewCoordinator.Result
                        .CURRENT_SAVE_FAILED_ROLLED_BACK,
                result);
        assertEquals(originalId, current.sessionId());
        assertEquals(7L,
                current.snapshotConfig().items.getFirst().quantity());
        assertTrue(history.document().isEmpty());
    }

    @Test
    void successfulArchiveAndCurrentSavePublishesExactlyOneNewSession() {
        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> true);
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "ARCHIVE_ME", 3L, SessionSourceType.MOB, started + 1L);
        String originalId = current.sessionId();
        MiningSessionHistoryController history = history(current);

        CurrentSessionStartNewCoordinator.Result result =
                CurrentSessionStartNewCoordinator.start(
                        current, history, started + 100L);

        assertSame(CurrentSessionStartNewCoordinator.Result.STARTED, result);
        assertFalse(originalId.equals(current.sessionId()));
        assertEquals(2, current.displayNumber());
        assertTrue(current.snapshotConfig().items.isEmpty());
        assertEquals(1, history.document().size());
    }

    @Test
    void currentStoreFailureRestoresAllTwentyPriorHistoryRecords() {
        Path historyPath = tempDir.resolve("history.json");
        MiningSessionHistoryDocument seeded =
                MiningSessionHistoryDocument.empty(
                        MiningSessionHistoryStore.SCHEMA_VERSION);
        for (int index = 1;
                index <= MiningSessionHistoryStore.MAX_RECORDS;
                index++) {
            RotClientCurrentSessionConfig config =
                    RotClientCurrentSessionConfig.defaults();
            config.displayNumber = index;
            config.startedAtMillis = index * 1_000L;
            config.lastHeartbeatMillis = config.startedAtMillis;
            RotClientSessionFreeze frozen =
                    RotClientSessionFreeze.fromCurrentSession(
                            config, config.startedAtMillis + 100L);
            MiningSessionHistoryStore.SaveResult saved =
                    MiningSessionHistoryStore.saveCurrentSession(
                            historyPath,
                            seeded,
                            frozen,
                            "hseed" + String.format("%011d", index));
            assertSame(MiningSessionHistoryStore.SaveOutcome.SAVED,
                    saved.outcome());
            seeded = saved.document();
        }
        List<String> originalIds = seeded.sessions().stream()
                .map(MiningSessionHistoryRecord::recordId)
                .toList();

        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> false);
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "KEEP_ME", 7L, SessionSourceType.MOB, started + 1L);
        MiningSessionHistoryController history = history(current);
        assertEquals(MiningSessionHistoryStore.MAX_RECORDS,
                history.document().size());

        CurrentSessionStartNewCoordinator.Result result =
                CurrentSessionStartNewCoordinator.start(
                        current, history, started + 100L);

        assertSame(
                CurrentSessionStartNewCoordinator.Result
                        .CURRENT_SAVE_FAILED_ROLLED_BACK,
                result);
        assertEquals(originalIds, history.document().sessions().stream()
                .map(MiningSessionHistoryRecord::recordId)
                .toList());
        MiningSessionHistoryStore.LoadResult reloaded =
                MiningSessionHistoryStore.load(historyPath);
        assertTrue(reloaded.available());
        assertEquals(originalIds, reloaded.document().sessions().stream()
                .map(MiningSessionHistoryRecord::recordId)
                .toList());
    }

    @Test
    void recoveryCompletesPublishAfterArchiveButBeforeCurrentSave() {
        Path journal = tempDir.resolve("start-new-transaction.json");
        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> true);
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "RECOVER_ME", 5L, SessionSourceType.MOB, started + 1L);
        String priorSessionId = current.sessionId();
        RotClientSessionFreeze frozen = current.freeze(started + 100L);
        MiningSessionHistoryController history = history(current);

        assertTrue(CurrentSessionStartNewJournal.begin(
                journal, current.snapshotConfig(), frozen));
        assertSame(
                MiningSessionHistoryController.SaveResult.SAVED,
                history.archiveCurrentSession(frozen));

        long recoveredAt = started + 10_000L;
        CurrentSessionStartNewCoordinator.RecoveryResult recovered =
                CurrentSessionStartNewCoordinator.recoverPending(
                        current, history, journal, recoveredAt);

        assertSame(
                CurrentSessionStartNewCoordinator.RecoveryResult
                        .COMPLETED_CURRENT_PUBLISH,
                recovered);
        assertFalse(priorSessionId.equals(current.sessionId()));
        assertEquals(2, current.displayNumber());
        assertEquals(recoveredAt,
                current.snapshotConfig().startedAtMillis);
        assertEquals(0L, current.activeDurationMillis(recoveredAt));
        assertTrue(current.snapshotConfig().items.isEmpty());
        assertEquals(1, history.document().size());
        assertFalse(Files.exists(journal));
    }

    @Test
    void recoveryClearsPreparedMarkerWhenArchiveWasNeverWritten() {
        Path journal = tempDir.resolve("prepared-only.json");
        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> true);
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "KEEP_CURRENT", 2L, SessionSourceType.MOB, started + 1L);
        String originalId = current.sessionId();
        RotClientSessionFreeze frozen = current.freeze(started + 100L);
        MiningSessionHistoryController history = history(current);
        assertTrue(CurrentSessionStartNewJournal.begin(
                journal, current.snapshotConfig(), frozen));

        CurrentSessionStartNewCoordinator.RecoveryResult recovered =
                CurrentSessionStartNewCoordinator.recoverPending(
                        current, history, journal);

        assertSame(
                CurrentSessionStartNewCoordinator.RecoveryResult
                        .CLEARED_BEFORE_ARCHIVE,
                recovered);
        assertEquals(originalId, current.sessionId());
        assertEquals(2L,
                current.snapshotConfig().items.getFirst().quantity());
        assertTrue(history.document().isEmpty());
        assertFalse(Files.exists(journal));
    }

    @Test
    void recoveryOnlyClearsMarkerWhenCurrentPublishAlreadyCompleted() {
        Path journal = tempDir.resolve("publish-complete.json");
        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> true);
        long started = current.snapshotConfig().startedAtMillis;
        RotClientSessionFreeze frozen = current.freeze(started + 100L);
        assertTrue(CurrentSessionStartNewJournal.begin(
                journal, current.snapshotConfig(), frozen));
        assertTrue(current.startNewSession(null, started + 100L));
        String nextId = current.sessionId();
        MiningSessionHistoryController history = history(current);

        CurrentSessionStartNewCoordinator.RecoveryResult recovered =
                CurrentSessionStartNewCoordinator.recoverPending(
                        current, history, journal);

        assertSame(
                CurrentSessionStartNewCoordinator.RecoveryResult
                        .ALREADY_COMMITTED,
                recovered);
        assertEquals(nextId, current.sessionId());
        assertEquals(2, current.displayNumber());
        assertFalse(Files.exists(journal));
    }

    @Test
    void durableStartStopsBeforeArchiveWhenJournalCannotBeWritten()
            throws Exception {
        Path blockedParent = tempDir.resolve("not-a-directory");
        Files.writeString(blockedParent, "blocked");
        Path journal = blockedParent.resolve("transaction.json");
        RotClientCurrentSession current =
                new RotClientCurrentSession(config -> true);
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "KEEP_ME", 9L, SessionSourceType.MOB, started + 1L);
        String originalId = current.sessionId();
        MiningSessionHistoryController history = history(current);

        CurrentSessionStartNewCoordinator.Result result =
                CurrentSessionStartNewCoordinator.startDurably(
                        current, history, started + 100L, journal);

        assertSame(
                CurrentSessionStartNewCoordinator.Result
                        .TRANSACTION_PREPARE_FAILED,
                result);
        assertEquals(originalId, current.sessionId());
        assertEquals(9L,
                current.snapshotConfig().items.getFirst().quantity());
        assertTrue(history.document().isEmpty());
    }

    private MiningSessionHistoryController history(
            RotClientCurrentSession current) {
        MiningSessionEngine engine = new MiningSessionEngine();
        MiningSessionAnalyticsController analytics =
                new MiningSessionAnalyticsController(
                        engine,
                        text -> {
                        },
                        (selection, now) ->
                                MiningSessionParity.LiveBaseline.material(
                                        selection, Map.of(), Map.of(), now),
                        () -> current.snapshotConfig().startedAtMillis + 100L);
        return new MiningSessionHistoryController(
                analytics,
                engine,
                text -> {
                },
                () -> current.snapshotConfig().startedAtMillis + 100L,
                tempDir.resolve("history.json"),
                current);
    }
}
