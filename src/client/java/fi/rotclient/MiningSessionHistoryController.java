package fi.rotclient;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Session History actions shared by commands and the dashboard.
 * Never mutates live tracker totals, TrackerStore, or Bazaar caches, and
 * never triggers networking.
 */
final class MiningSessionHistoryController {
    /** Shared clear-confirmation validity window for commands and dashboard. */
    static final long CLEAR_CONFIRM_TTL_MILLIS = 120_000L;
    private static final int LIST_OUTPUT_LIMIT = 20;

    enum SaveResult {
        SAVED,
        ALREADY_SAVED,
        REJECTED_ACTIVE,
        REJECTED_NO_SESSION,
        UNAVAILABLE,
        WRITE_FAILED
    }

    enum OpenResult {
        OPENED,
        NOT_FOUND,
        UNAVAILABLE
    }

    enum CopyResult {
        COPIED,
        NOT_FOUND,
        UNAVAILABLE,
        FAILED
    }

    enum DeleteResult {
        DELETED,
        NOT_FOUND,
        UNAVAILABLE,
        WRITE_FAILED
    }

    enum ClearResult {
        CLEARED,
        CONFIRMATION_REQUIRED,
        CONFIRMATION_EXPIRED,
        UNAVAILABLE,
        WRITE_FAILED,
        NOTHING_TO_CLEAR
    }

    @FunctionalInterface
    interface ClipboardAccess {
        void copy(String text) throws Exception;
    }

    @FunctionalInterface
    interface Clock {
        long nowMillis();
    }

    private final MiningSessionAnalyticsController analytics;
    private final MiningSessionEngine engine;
    private final ClipboardAccess clipboard;
    private final Clock clock;
    private final java.nio.file.Path historyPath;
    private final MiningSessionSummaryFormatter formatter;
    private final RotClientCurrentSession currentSession;

    private MiningSessionHistoryDocument document;
    private MiningSessionHistoryStore.Availability availability;
    private String loadWarning;
    private String selectedRecordId;
    private String pendingClearToken;
    private long pendingClearExpiresAtMillis;
    private boolean actionInFlight;

    MiningSessionHistoryController(
            MiningSessionAnalyticsController analytics,
            MiningSessionEngine engine,
            ClipboardAccess clipboard,
            Clock clock,
            java.nio.file.Path historyPath) {
        this(analytics, engine, clipboard, clock, historyPath, null);
    }

    MiningSessionHistoryController(
            MiningSessionAnalyticsController analytics,
            MiningSessionEngine engine,
            ClipboardAccess clipboard,
            Clock clock,
            java.nio.file.Path historyPath,
            RotClientCurrentSession currentSession) {
        if (analytics == null
                || engine == null
                || clipboard == null
                || clock == null
                || historyPath == null) {
            throw new IllegalArgumentException(
                    "History controller dependencies cannot be null");
        }
        this.analytics = analytics;
        this.engine = engine;
        this.clipboard = clipboard;
        this.clock = clock;
        this.historyPath = historyPath;
        this.formatter = new MiningSessionSummaryFormatter();
        this.currentSession = currentSession;
        reload();
    }

    void reload() {
        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(historyPath);
        document = loaded.document();
        availability = loaded.availability();
        loadWarning = loaded.warning();
        if (selectedRecordId != null
                && MiningSessionHistoryStore.findByIndexOrId(
                        document, selectedRecordId).isEmpty()) {
            selectedRecordId = null;
        }
    }

    boolean available() {
        return availability
                == MiningSessionHistoryStore.Availability.AVAILABLE;
    }

    String loadWarning() {
        return loadWarning == null ? "" : loadWarning;
    }

    MiningSessionHistoryDocument document() {
        return document;
    }

    boolean containsFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return false;
        }
        for (MiningSessionHistoryRecord record : document.sessions()) {
            if (fingerprint.equals(record.contentFingerprint())) {
                return true;
            }
        }
        return false;
    }

    List<MiningSessionHistoryRecord> sessionsNewestFirst() {
        return document.sessions();
    }

    Optional<MiningSessionHistoryRecord> selectedRecord() {
        if (selectedRecordId == null) {
            return Optional.empty();
        }
        return MiningSessionHistoryStore.findByIndexOrId(
                document,
                selectedRecordId);
    }

    void clearSelection() {
        selectedRecordId = null;
    }

    boolean hasUnsavedStoppedSnapshot() {
        if (!available()) {
            return false;
        }
        if (currentSession != null) {
            if (!currentSession.isPaused()) {
                return false;
            }
            RotClientSessionFreeze frozen =
                    currentSession.freeze(clock.nowMillis());
            for (MiningSessionHistoryRecord record : document.sessions()) {
                if (record.currentSessionFreeze().isPresent()
                        && MiningSessionHistoryStore.sameCurrentSessionSnapshot(
                        record.currentSessionFreeze().get(), frozen)) {
                    return false;
                }
            }
            String fingerprint;
            try {
                fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                        frozen);
            } catch (RuntimeException ignored) {
                return false;
            }
            for (MiningSessionHistoryRecord record : document.sessions()) {
                if (fingerprint.equals(record.contentFingerprint())) {
                    return false;
                }
            }
            return true;
        }
        MiningSessionAnalyticsViewModel model = analytics.viewModel();
        if (model.sessionState()
                != MiningSessionAnalyticsViewModel.SessionState.STOPPED
                || !model.hasViewableSession()) {
            return false;
        }
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                model,
                currentPriceBookObservedAt());
        for (MiningSessionHistoryRecord record : document.sessions()) {
            if (fingerprint.equals(record.contentFingerprint())) {
                return false;
            }
        }
        return true;
    }

    MiningSessionHistoryPresentation presentation() {
        return MiningSessionHistoryPresentation.from(
                this,
                analytics.viewModel());
    }

    SaveResult saveCurrentStoppedSession() {
        if (!beginAction()) {
            return SaveResult.WRITE_FAILED;
        }
        try {
            if (!available()) {
                return SaveResult.UNAVAILABLE;
            }
            if (currentSession != null) {
                if (!currentSession.isPaused()) {
                    return SaveResult.REJECTED_ACTIVE;
                }
                MiningSessionHistoryStore.SaveResult currentResult =
                        MiningSessionHistoryStore.saveCurrentSession(
                                historyPath,
                                document,
                                currentSession.freeze(clock.nowMillis()),
                                MiningSessionHistoryStore.newRecordId());
                document = currentResult.document();
                return mapSaveResult(currentResult);
            }
            MiningSessionAnalyticsViewModel model = analytics.viewModel();
            MiningSessionHistoryStore.SaveResult result =
                    MiningSessionHistoryStore.saveStoppedSession(
                            historyPath,
                            document,
                            model,
                            currentPriceBookObservedAt(),
                            MiningSessionHistoryStore.newRecordId());
            document = result.document();
            return mapSaveResult(result);
        } catch (RuntimeException ignored) {
            return SaveResult.WRITE_FAILED;
        } finally {
            endAction();
        }
    }

    SaveResult archiveCurrentSession(RotClientSessionFreeze frozen) {
        if (!beginAction()) {
            return SaveResult.WRITE_FAILED;
        }
        try {
            if (!available()) {
                return SaveResult.UNAVAILABLE;
            }
            MiningSessionHistoryStore.SaveResult result =
                    MiningSessionHistoryStore.saveCurrentSession(
                            historyPath,
                            document,
                            frozen,
                            MiningSessionHistoryStore.newRecordId());
            document = result.document();
            return mapSaveResult(result);
        } catch (RuntimeException ignored) {
            return SaveResult.WRITE_FAILED;
        } finally {
            endAction();
        }
    }

    /**
     * Restores the exact pre-archive document when Start New cannot publish
     * its replacement Current Session. This also restores a retention-cap
     * record that the tentative archive may have evicted.
     */
    boolean restoreAfterFailedStartNew(
            MiningSessionHistoryDocument priorDocument) {
        if (priorDocument == null || !beginAction()) {
            return false;
        }
        try {
            if (!available()
                    || !MiningSessionHistoryStore.write(
                    historyPath, priorDocument)) {
                return false;
            }
            document = priorDocument;
            if (selectedRecordId != null
                    && MiningSessionHistoryStore.findByIndexOrId(
                    document, selectedRecordId).isEmpty()) {
                selectedRecordId = null;
            }
            return true;
        } finally {
            endAction();
        }
    }

    String listText() {
        if (!available()) {
            return "Session History unavailable. "
                    + (loadWarning.isBlank()
                    ? "Local history file could not be read."
                    : loadWarning);
        }
        List<MiningSessionHistoryRecord> sessions = document.sessions();
        if (sessions.isEmpty()) {
            return "Session History is empty.";
        }
        StringBuilder result = new StringBuilder(256);
        result.append("Session History (newest first)\n");
        int limit = Math.min(LIST_OUTPUT_LIMIT, sessions.size());
        for (int i = 0; i < limit; i++) {
            MiningSessionHistoryRecord record = sessions.get(i);
            result.append(i + 1)
                    .append(". ")
                    .append(boundedChatText(
                            record.selectedTargetDisplayName(), 32))
                    .append(" | ")
                    .append(formatInstant(record.stoppedMillis()))
                    .append(" | entries ")
                    .append(record.entryCount())
                    .append(" | value ");
            if (record.resolvedValueAvailable()) {
                result.append(MiningSessionPriceBook.formatCoinAmount(
                        record.resolvedItemValue()));
            } else {
                result.append("unavailable");
            }
            result.append(" | id ")
                    .append(boundedChatText(record.recordId(), 40))
                    .append('\n');
        }
        if (sessions.size() > limit) {
            result.append('+')
                    .append(sessions.size() - limit)
                    .append(" more retained locally");
        }
        String text = result.toString().trim();
        if (text.length() <= 4_000) {
            return text;
        }
        return text.substring(0, 3_985) + "\n...truncated";
    }

    OpenResult open(String indexOrId) {
        if (!beginAction()) {
            return OpenResult.UNAVAILABLE;
        }
        try {
            if (!available()) {
                return OpenResult.UNAVAILABLE;
            }
            Optional<MiningSessionHistoryRecord> record =
                    MiningSessionHistoryStore.findByIndexOrId(
                            document,
                            indexOrId);
            if (record.isEmpty()) {
                return OpenResult.NOT_FOUND;
            }
            selectedRecordId = record.get().recordId();
            return OpenResult.OPENED;
        } finally {
            endAction();
        }
    }

    CopyResult copy(String indexOrId) {
        if (!beginAction()) {
            return CopyResult.FAILED;
        }
        try {
            if (!available()) {
                return CopyResult.UNAVAILABLE;
            }
            Optional<MiningSessionHistoryRecord> record =
                    resolveRecord(indexOrId);
            if (record.isEmpty()) {
                return CopyResult.NOT_FOUND;
            }
            clipboard.copy(formatHistoricalSummary(record.get()));
            return CopyResult.COPIED;
        } catch (Exception ignored) {
            return CopyResult.FAILED;
        } finally {
            endAction();
        }
    }

    DeleteResult delete(String indexOrId) {
        if (!beginAction()) {
            return DeleteResult.WRITE_FAILED;
        }
        try {
            if (!available()) {
                return DeleteResult.UNAVAILABLE;
            }
            Optional<MiningSessionHistoryRecord> record =
                    MiningSessionHistoryStore.findByIndexOrId(
                            document,
                            indexOrId);
            if (record.isEmpty()) {
                return DeleteResult.NOT_FOUND;
            }
            String id = record.get().recordId();
            if (!MiningSessionHistoryStore.deleteRecord(
                    historyPath,
                    document,
                    id)) {
                return DeleteResult.WRITE_FAILED;
            }
            reload();
            if (id.equals(selectedRecordId)) {
                selectedRecordId = null;
            }
            return DeleteResult.DELETED;
        } finally {
            endAction();
        }
    }

    ClearResult requestClear() {
        if (!available()) {
            return ClearResult.UNAVAILABLE;
        }
        if (document.isEmpty()) {
            return ClearResult.NOTHING_TO_CLEAR;
        }
        pendingClearToken = MiningSessionHistoryStore.newRecordId();
        pendingClearExpiresAtMillis =
                clock.nowMillis() + CLEAR_CONFIRM_TTL_MILLIS;
        return ClearResult.CONFIRMATION_REQUIRED;
    }

    String pendingClearToken() {
        return pendingClearToken;
    }

    /**
     * User-facing validity window derived from
     * {@link #CLEAR_CONFIRM_TTL_MILLIS}.
     */
    static String clearConfirmValidityPhrase() {
        long totalSeconds = CLEAR_CONFIRM_TTL_MILLIS / 1_000L;
        if (totalSeconds > 0L && totalSeconds % 60L == 0L) {
            long minutes = totalSeconds / 60L;
            if (minutes == 1L) {
                return "within 1 minute";
            }
            return "within " + minutes + " minutes";
        }
        return "within " + totalSeconds + "s";
    }

    static String clearConfirmationPrompt(String token) {
        return "Confirm Session History clear "
                + clearConfirmValidityPhrase()
                + ": /rot history clear confirm "
                + token;
    }

    ClearResult confirmClear(String token) {
        if (!beginAction()) {
            return ClearResult.WRITE_FAILED;
        }
        try {
            if (!available()) {
                return ClearResult.UNAVAILABLE;
            }
            if (document.isEmpty()) {
                clearPendingClear();
                return ClearResult.NOTHING_TO_CLEAR;
            }
            long now = clock.nowMillis();
            if (pendingClearToken == null
                    || pendingClearExpiresAtMillis <= now) {
                clearPendingClear();
                return ClearResult.CONFIRMATION_EXPIRED;
            }
            if (token == null || !pendingClearToken.equals(token)) {
                clearPendingClear();
                return ClearResult.CONFIRMATION_EXPIRED;
            }
            if (!MiningSessionHistoryStore.clear(historyPath, document)) {
                return ClearResult.WRITE_FAILED;
            }
            reload();
            selectedRecordId = null;
            clearPendingClear();
            return ClearResult.CLEARED;
        } finally {
            endAction();
        }
    }

    private Optional<MiningSessionHistoryRecord> resolveRecord(
            String indexOrId) {
        if (indexOrId == null || indexOrId.isBlank()) {
            return selectedRecord();
        }
        return MiningSessionHistoryStore.findByIndexOrId(
                document,
                indexOrId);
    }

    private static SaveResult mapSaveResult(
            MiningSessionHistoryStore.SaveResult result) {
        return switch (result.outcome()) {
            case SAVED -> SaveResult.SAVED;
            case ALREADY_SAVED -> SaveResult.ALREADY_SAVED;
            case REJECTED_ACTIVE -> SaveResult.REJECTED_ACTIVE;
            case REJECTED_NO_SESSION -> SaveResult.REJECTED_NO_SESSION;
            case WRITE_FAILED -> SaveResult.WRITE_FAILED;
        };
    }

    private OptionalLong currentPriceBookObservedAt() {
        return engine.snapshot(clock.nowMillis())
                .map(snapshot -> snapshot.valuation().priceBookObservedAtMillis())
                .orElse(OptionalLong.empty());
    }

    private String formatHistoricalSummary(
            MiningSessionHistoryRecord record) {
        String body = formatter.format(record.toViewModel());
        StringBuilder result = new StringBuilder(body.length() + 64);
        result.append("Rot Client Saved Session History\n");
        result.append("Source: stored historical session\n");
        result.append("Record: ")
                .append(record.recordId())
                .append('\n');
        record.currentSessionFreeze().ifPresent(frozen -> {
            result.append("Active duration: ")
                    .append(MiningSessionHistorySummaries.durationLabel(record))
                    .append('\n');
            result.append("Paused duration: ")
                    .append(MiningSessionHistorySummaries
                            .pausedDurationLabel(record))
                    .append('\n');
            result.append("Target segments: ")
                    .append(frozen.targetSegments().size())
                    .append('\n');
            result.append("Area segments: ")
                    .append(frozen.areaSegments().size())
                    .append('\n');
            result.append("Canonical item rows: ")
                    .append(frozen.itemRows().size())
                    .append('\n');
            result.append("MOB/CHEST/CURRENCY: schema-ready; live ingress remains source-gated\n");
            for (RotClientCurrentSessionConfig.TargetSegment segment
                    : frozen.targetSegments()) {
                result.append("Target segment: ")
                        .append(segment.targetId().isBlank()
                                ? "UNKNOWN"
                                : segment.targetId())
                        .append(" | ")
                        .append(Math.max(0L,
                                segment.endedAtMillis()
                                        - segment.startedAtMillis()))
                        .append(" ms\n");
            }
            for (RotClientCurrentSessionConfig.AreaSegment segment
                    : frozen.areaSegments()) {
                result.append("Area segment: ")
                        .append(segment.areaId())
                        .append(" | ")
                        .append(Math.max(0L,
                                segment.endedAtMillis()
                                        - segment.startedAtMillis()))
                        .append(" ms\n");
            }
            result.append("Archived item rows:\n");
            for (RotClientCurrentSessionConfig.SessionItemRecord row
                    : frozen.itemRows()) {
                result.append("- ")
                        .append(row.itemId())
                        .append(" x")
                        .append(row.quantity())
                        .append(" | ")
                        .append(row.sourceType());
                if (row.miningClass() != null) {
                    result.append('/').append(row.miningClass());
                }
                result.append(" | area ")
                        .append(row.areaId())
                        .append(" | price ")
                        .append(row.priceStatus());
                if (row.resolvedGrossValue() > 0.0) {
                    result.append(" | gross ")
                            .append(MiningSessionPriceBook.formatCoinAmount(
                                    java.math.BigDecimal.valueOf(
                                            row.resolvedGrossValue())));
                }
                result.append('\n');
            }
        });
        result.append('\n');
        // Replace the live analytics header with the historical label.
        if (body.startsWith("Rot Client Session Analytics\n")) {
            result.append(body.substring(
                    "Rot Client Session Analytics\n".length()));
        } else {
            result.append(body);
        }
        return result.toString();
    }

    private static String formatInstant(long epochMillis) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(epochMillis);
        return java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(java.time.ZoneOffset.UTC)
                .format(instant)
                + " UTC";
    }

    private static String boundedChatText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }
        String sanitized = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, Math.max(1, maxLength - 3)) + "...";
    }

    private void clearPendingClear() {
        pendingClearToken = null;
        pendingClearExpiresAtMillis = 0L;
    }

    private boolean beginAction() {
        if (actionInFlight) {
            return false;
        }
        actionInFlight = true;
        return true;
    }

    private void endAction() {
        actionInFlight = false;
    }
}
