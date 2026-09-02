package fi.rotclient;

import java.nio.file.Path;

/**
 * Coordinates the two durable resources touched by Start New. History is
 * written first; if publishing the new Current Session fails, a history row
 * created by this attempt is removed and the original live session remains.
 */
final class CurrentSessionStartNewCoordinator {
    enum Result {
        STARTED,
        TRANSACTION_PREPARE_FAILED,
        ARCHIVE_FAILED,
        CURRENT_SAVE_FAILED_ROLLED_BACK,
        ROLLBACK_FAILED
    }

    enum RecoveryResult {
        NO_PENDING,
        CLEARED_BEFORE_ARCHIVE,
        COMPLETED_CURRENT_PUBLISH,
        ALREADY_COMMITTED,
        INVALID_JOURNAL,
        HISTORY_UNAVAILABLE,
        CURRENT_PUBLISH_FAILED,
        CLEANUP_FAILED
    }

    private CurrentSessionStartNewCoordinator() {
    }

    static Result start(
            RotClientCurrentSession currentSession,
            MiningSessionHistoryController history,
            long nowMillis) {
        if (currentSession == null || history == null) {
            return Result.ARCHIVE_FAILED;
        }

        MiningSessionHistoryDocument priorHistory = history.document();
        MiningSessionHistoryController.SaveResult[] archive = {
                MiningSessionHistoryController.SaveResult.WRITE_FAILED};
        boolean started = currentSession.startNewSession(frozen -> {
            MiningSessionHistoryController.SaveResult result =
                    history.archiveCurrentSession(frozen);
            archive[0] = result;
            return result == MiningSessionHistoryController.SaveResult.SAVED
                    || result
                    == MiningSessionHistoryController.SaveResult.ALREADY_SAVED;
        }, nowMillis);
        if (started) {
            return Result.STARTED;
        }

        MiningSessionHistoryController.SaveResult archiveResult = archive[0];
        if (archiveResult != MiningSessionHistoryController.SaveResult.SAVED
                && archiveResult
                != MiningSessionHistoryController.SaveResult.ALREADY_SAVED) {
            return Result.ARCHIVE_FAILED;
        }
        if (archiveResult
                == MiningSessionHistoryController.SaveResult.ALREADY_SAVED) {
            return Result.CURRENT_SAVE_FAILED_ROLLED_BACK;
        }
        return history.restoreAfterFailedStartNew(priorHistory)
                ? Result.CURRENT_SAVE_FAILED_ROLLED_BACK
                : Result.ROLLBACK_FAILED;
    }

    static Result startDurably(
            RotClientCurrentSession currentSession,
            MiningSessionHistoryController history,
            long nowMillis,
            Path journalPath) {
        if (currentSession == null || history == null) {
            return Result.ARCHIVE_FAILED;
        }
        RotClientSessionFreeze frozen = currentSession.freeze(nowMillis);
        if (!CurrentSessionStartNewJournal.begin(
                journalPath, currentSession.snapshotConfig(), frozen)) {
            return Result.TRANSACTION_PREPARE_FAILED;
        }

        Result result = start(currentSession, history, nowMillis);
        if (result != Result.ROLLBACK_FAILED) {
            // A stale marker is safe if cleanup itself fails: startup recovery
            // distinguishes the already-published and rolled-back states.
            CurrentSessionStartNewJournal.clear(journalPath);
        }
        return result;
    }

    static RecoveryResult recoverPending(
            RotClientCurrentSession currentSession,
            MiningSessionHistoryController history,
            Path journalPath) {
        return recoverPending(
                currentSession,
                history,
                journalPath,
                System.currentTimeMillis());
    }

    static RecoveryResult recoverPending(
            RotClientCurrentSession currentSession,
            MiningSessionHistoryController history,
            Path journalPath,
            long recoveryAtMillis) {
        CurrentSessionStartNewJournal.LoadResult loaded =
                CurrentSessionStartNewJournal.load(journalPath);
        if (loaded.status()
                == CurrentSessionStartNewJournal.LoadStatus.NONE) {
            return RecoveryResult.NO_PENDING;
        }
        if (loaded.status()
                == CurrentSessionStartNewJournal.LoadStatus.INVALID) {
            return RecoveryResult.INVALID_JOURNAL;
        }
        if (currentSession == null || history == null) {
            return RecoveryResult.CURRENT_PUBLISH_FAILED;
        }

        CurrentSessionStartNewJournal.Pending pending =
                loaded.pending().orElseThrow();
        if (!CurrentSessionStartNewJournal.matchesCurrent(
                pending, currentSession.snapshotConfig())) {
            return CurrentSessionStartNewJournal.clear(journalPath)
                    ? RecoveryResult.ALREADY_COMMITTED
                    : RecoveryResult.CLEANUP_FAILED;
        }
        if (!history.available()) {
            return RecoveryResult.HISTORY_UNAVAILABLE;
        }
        if (!history.containsFingerprint(pending.archiveFingerprint())) {
            return CurrentSessionStartNewJournal.clear(journalPath)
                    ? RecoveryResult.CLEARED_BEFORE_ARCHIVE
                    : RecoveryResult.CLEANUP_FAILED;
        }

        long nextSessionStartedAt = Math.max(
                pending.boundaryMillis(), recoveryAtMillis);
        if (!currentSession.startNewSession(
                null, nextSessionStartedAt)) {
            return RecoveryResult.CURRENT_PUBLISH_FAILED;
        }
        return CurrentSessionStartNewJournal.clear(journalPath)
                ? RecoveryResult.COMPLETED_CURRENT_PUBLISH
                : RecoveryResult.CLEANUP_FAILED;
    }
}
