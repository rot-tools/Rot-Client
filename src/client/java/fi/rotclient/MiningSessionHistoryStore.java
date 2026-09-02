package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Local Session History persistence. Completely separate from TrackerStore.
 * Malformed or unsupported files fail closed to an empty unavailable state.
 */
final class MiningSessionHistoryStore {
    static final int LEGACY_SCHEMA_VERSION = 1;
    static final int SCHEMA_VERSION = 2;
    static final int MAX_RECORDS = 20;
    static final long MAX_FILE_BYTES = 64L * 1024L * 1024L;
    static final String FILE_NAME = "rotclient-session-history.json";
    static final String CORRUPT_FILE_NAME =
            "rotclient-session-history.corrupt.json";
    static final String LOAD_WARNING =
            "Session History file was unreadable and was reset.";
    static final String FUTURE_SCHEMA_WARNING =
            "Session History was written by a newer Rot Client version. "
                    + "The file was left in place and writes are blocked.";

    private static final SecureRandom RANDOM = new SecureRandom();

    enum Availability {
        AVAILABLE,
        UNAVAILABLE
    }

    record LoadResult(
            MiningSessionHistoryDocument document,
            Availability availability,
            String warning) {
        LoadResult {
            if (document == null || availability == null) {
                throw new IllegalArgumentException(
                        "Load result fields cannot be null");
            }
            warning = warning == null ? "" : warning;
        }

        boolean available() {
            return availability == Availability.AVAILABLE;
        }
    }

    enum SaveOutcome {
        SAVED,
        ALREADY_SAVED,
        REJECTED_ACTIVE,
        REJECTED_NO_SESSION,
        WRITE_FAILED
    }

    record SaveResult(
            SaveOutcome outcome,
            MiningSessionHistoryDocument document,
            Optional<MiningSessionHistoryRecord> record) {
        SaveResult {
            if (outcome == null || document == null || record == null) {
                throw new IllegalArgumentException(
                        "Save result fields cannot be null");
            }
        }
    }

    private MiningSessionHistoryStore() {
    }

    static Path defaultPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static Path trackerStorePath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("rotclient.json");
    }

    static LoadResult load() {
        return load(defaultPath());
    }

    static LoadResult load(Path path) {
        if (path == null) {
            return unavailableEmpty(LOAD_WARNING);
        }
        if (!Files.exists(path)) {
            return new LoadResult(
                    MiningSessionHistoryDocument.empty(SCHEMA_VERSION),
                    Availability.AVAILABLE,
                    "");
        }
        try {
            long size = Files.size(path);
            if (size > MAX_FILE_BYTES) {
                quarantineCorrupt(path);
                return unavailableEmpty(LOAD_WARNING);
            }
            if (size == 0L) {
                return new LoadResult(
                        MiningSessionHistoryDocument.empty(SCHEMA_VERSION),
                        Availability.AVAILABLE,
                        "");
            }
            String json;
            try (Reader reader = Files.newBufferedReader(
                    path,
                    StandardCharsets.UTF_8)) {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[4_096];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    builder.append(buffer, 0, read);
                }
                json = builder.toString();
            }
            if (json.isBlank()) {
                return new LoadResult(
                        MiningSessionHistoryDocument.empty(SCHEMA_VERSION),
                        Availability.AVAILABLE,
                        "");
            }
            JsonElement root = JsonParser.parseString(json);
            if (root == null || !root.isJsonObject()) {
                quarantineCorrupt(path);
                return unavailableEmpty(LOAD_WARNING);
            }
            JsonElement schema = root.getAsJsonObject().get("schemaVersion");
            if (schema != null
                    && schema.isJsonPrimitive()
                    && schema.getAsInt() > SCHEMA_VERSION) {
                return unavailableEmpty(FUTURE_SCHEMA_WARNING);
            }
            MiningSessionHistoryDocument document =
                    MiningSessionHistoryCodec.fromJson(root.getAsJsonObject());
            return new LoadResult(
                    document,
                    Availability.AVAILABLE,
                    "");
        } catch (Exception ignored) {
            quarantineCorrupt(path);
            return unavailableEmpty(LOAD_WARNING);
        }
    }

    static SaveResult saveStoppedSession(
            Path path,
            MiningSessionHistoryDocument current,
            MiningSessionAnalyticsViewModel model,
            java.util.OptionalLong priceBookObservedAtMillis,
            String recordId) {
        if (model == null) {
            return new SaveResult(
                    SaveOutcome.REJECTED_NO_SESSION,
                    safeDocument(current),
                    Optional.empty());
        }
        if (model.sessionState()
                == MiningSessionAnalyticsViewModel.SessionState.ACTIVE) {
            return new SaveResult(
                    SaveOutcome.REJECTED_ACTIVE,
                    safeDocument(current),
                    Optional.empty());
        }
        if (model.sessionState()
                != MiningSessionAnalyticsViewModel.SessionState.STOPPED
                || !model.hasViewableSession()) {
            return new SaveResult(
                    SaveOutcome.REJECTED_NO_SESSION,
                    safeDocument(current),
                    Optional.empty());
        }

        MiningSessionHistoryDocument document = safeDocument(current);
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                model,
                priceBookObservedAtMillis);
        for (MiningSessionHistoryRecord existing : document.sessions()) {
            if (fingerprint.equals(existing.contentFingerprint())) {
                return new SaveResult(
                        SaveOutcome.ALREADY_SAVED,
                        document,
                        Optional.of(existing));
            }
        }

        String id = recordId == null || recordId.isBlank()
                ? newRecordId()
                : recordId;
        MiningSessionHistoryRecord record =
                MiningSessionHistoryRecord.fromStoppedViewModel(
                        id,
                        SCHEMA_VERSION,
                        fingerprint,
                        model,
                        priceBookObservedAtMillis);

        ArrayList<MiningSessionHistoryRecord> next =
                new ArrayList<>(document.sessions().size() + 1);
        next.add(record);
        next.addAll(document.sessions());
        while (next.size() > MAX_RECORDS) {
            next.remove(next.size() - 1);
        }
        MiningSessionHistoryDocument updated =
                MiningSessionHistoryDocument.of(SCHEMA_VERSION, next);
        if (!write(path, updated)) {
            return new SaveResult(
                    SaveOutcome.WRITE_FAILED,
                    document,
                    Optional.empty());
        }
        return new SaveResult(
                SaveOutcome.SAVED,
                updated,
                Optional.of(record));
    }

    static SaveResult saveCurrentSession(
            Path path,
            MiningSessionHistoryDocument current,
            RotClientSessionFreeze frozen,
            String recordId) {
        if (frozen == null) {
            return new SaveResult(
                    SaveOutcome.REJECTED_NO_SESSION,
                    safeDocument(current),
                    Optional.empty());
        }
        MiningSessionHistoryDocument document = safeDocument(current);
        for (MiningSessionHistoryRecord existing : document.sessions()) {
            Optional<RotClientSessionFreeze> prior =
                    existing.currentSessionFreeze();
            if (prior.isPresent()
                    && sameCurrentSessionSnapshot(prior.get(), frozen)) {
                return new SaveResult(
                        SaveOutcome.ALREADY_SAVED,
                        document,
                        Optional.of(existing));
            }
        }
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                frozen);
        for (MiningSessionHistoryRecord existing : document.sessions()) {
            if (fingerprint.equals(existing.contentFingerprint())) {
                return new SaveResult(
                        SaveOutcome.ALREADY_SAVED,
                        document,
                        Optional.of(existing));
            }
        }
        String id = recordId == null || recordId.isBlank()
                ? newRecordId()
                : recordId;
        MiningSessionHistoryRecord record =
                MiningSessionHistoryRecord.fromCurrentSessionFreeze(
                        id,
                        SCHEMA_VERSION,
                        fingerprint,
                        frozen);
        ArrayList<MiningSessionHistoryRecord> next =
                new ArrayList<>(document.sessions().size() + 1);
        next.add(record);
        next.addAll(document.sessions());
        while (next.size() > MAX_RECORDS) {
            next.remove(next.size() - 1);
        }
        MiningSessionHistoryDocument updated =
                MiningSessionHistoryDocument.of(SCHEMA_VERSION, next);
        if (!write(path, updated)) {
            return new SaveResult(
                    SaveOutcome.WRITE_FAILED,
                    document,
                    Optional.empty());
        }
        return new SaveResult(
                SaveOutcome.SAVED,
                updated,
                Optional.of(record));
    }

    static boolean sameCurrentSessionSnapshot(
            RotClientSessionFreeze left,
            RotClientSessionFreeze right) {
        if (left == null || right == null) {
            return false;
        }
        return left.displayNumber() == right.displayNumber()
                && left.startedAtMillis() == right.startedAtMillis()
                && left.activeDurationMillis() == right.activeDurationMillis()
                && left.selectedTargetId().equals(right.selectedTargetId())
                && left.itemRows().equals(right.itemRows())
                && left.priceBookObservedAtMillis().equals(
                right.priceBookObservedAtMillis())
                && sameTargetSegments(left, right)
                && sameAreaSegments(left, right);
    }

    private static boolean sameTargetSegments(
            RotClientSessionFreeze left,
            RotClientSessionFreeze right) {
        if (left.targetSegments().size() != right.targetSegments().size()) {
            return false;
        }
        for (int index = 0; index < left.targetSegments().size(); index++) {
            RotClientCurrentSessionConfig.TargetSegment a =
                    left.targetSegments().get(index);
            RotClientCurrentSessionConfig.TargetSegment b =
                    right.targetSegments().get(index);
            if (!a.targetId().equals(b.targetId())
                    || a.startedAtMillis() != b.startedAtMillis()
                    || !sameStableOrOpenEnd(
                    a.endedAtMillis(), left.stoppedAtMillis(),
                    b.endedAtMillis(), right.stoppedAtMillis())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameAreaSegments(
            RotClientSessionFreeze left,
            RotClientSessionFreeze right) {
        if (left.areaSegments().size() != right.areaSegments().size()) {
            return false;
        }
        for (int index = 0; index < left.areaSegments().size(); index++) {
            RotClientCurrentSessionConfig.AreaSegment a =
                    left.areaSegments().get(index);
            RotClientCurrentSessionConfig.AreaSegment b =
                    right.areaSegments().get(index);
            if (!a.areaId().equals(b.areaId())
                    || a.startedAtMillis() != b.startedAtMillis()
                    || !sameStableOrOpenEnd(
                    a.endedAtMillis(), left.stoppedAtMillis(),
                    b.endedAtMillis(), right.stoppedAtMillis())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameStableOrOpenEnd(
            long leftEnd,
            long leftStopped,
            long rightEnd,
            long rightStopped) {
        return leftEnd == rightEnd
                || (leftEnd == leftStopped && rightEnd == rightStopped);
    }

    static boolean deleteRecord(
            Path path,
            MiningSessionHistoryDocument current,
            String recordId) {
        if (recordId == null || recordId.isBlank()) {
            return false;
        }
        MiningSessionHistoryDocument document = safeDocument(current);
        ArrayList<MiningSessionHistoryRecord> next = new ArrayList<>();
        boolean removed = false;
        for (MiningSessionHistoryRecord record : document.sessions()) {
            if (!removed && recordId.equals(record.recordId())) {
                removed = true;
                continue;
            }
            next.add(record);
        }
        if (!removed) {
            return false;
        }
        return write(
                path,
                MiningSessionHistoryDocument.of(SCHEMA_VERSION, next));
    }

    static boolean clear(
            Path path,
            MiningSessionHistoryDocument current) {
        return write(
                path,
                MiningSessionHistoryDocument.empty(SCHEMA_VERSION));
    }

    static boolean write(Path path, MiningSessionHistoryDocument document) {
        return writeWithinLimit(path, document, MAX_FILE_BYTES);
    }

    /**
     * Shared UTF-8 byte-budget gate used by production writes and deterministic
     * boundary tests. A rejected payload never reaches the atomic writer.
     */
    static boolean writeWithinLimit(
            Path path,
            MiningSessionHistoryDocument document,
            long maxFileBytes) {
        if (path == null || document == null) {
            return false;
        }
        try {
            String json = MiningSessionHistoryCodec.toJsonString(document);
            if (maxFileBytes < 0L
                    || json.getBytes(StandardCharsets.UTF_8).length
                    > maxFileBytes) {
                return false;
            }
            AtomicFileWriter.writeAtomically(
                    path,
                    json);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static String newRecordId() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return "h" + HexFormat.of().formatHex(bytes);
    }

    static Optional<MiningSessionHistoryRecord> findByIndexOrId(
            MiningSessionHistoryDocument document,
            String indexOrId) {
        if (document == null || indexOrId == null || indexOrId.isBlank()) {
            return Optional.empty();
        }
        String token = indexOrId.trim();
        for (MiningSessionHistoryRecord record : document.sessions()) {
            if (token.equalsIgnoreCase(record.recordId())) {
                return Optional.of(record);
            }
        }
        try {
            int index = Integer.parseInt(token);
            if (index >= 1 && index <= document.sessions().size()) {
                return Optional.of(document.sessions().get(index - 1));
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    private static MiningSessionHistoryDocument safeDocument(
            MiningSessionHistoryDocument current) {
        return current == null
                ? MiningSessionHistoryDocument.empty(SCHEMA_VERSION)
                : current;
    }

    private static LoadResult unavailableEmpty(String warning) {
        return new LoadResult(
                MiningSessionHistoryDocument.empty(SCHEMA_VERSION),
                Availability.UNAVAILABLE,
                warning);
    }

    private static void quarantineCorrupt(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return;
            }
            Path parent = path.getParent();
            if (parent == null) {
                return;
            }
            Path corrupt = parent.resolve(CORRUPT_FILE_NAME);
            try {
                Files.move(
                        path,
                        corrupt);
            } catch (Exception ignored) {
                // Fail closed without exposing details.
            }
        } catch (Exception ignored) {
        }
    }
}
