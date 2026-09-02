package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Bounded transaction marker for Start New's two durable file replacements.
 * This is not a ledger: it stores no quantities, item rows, player data, or
 * Current Session payload. A marker exists only while the operation may need
 * startup recovery.
 */
final class CurrentSessionStartNewJournal {
    static final int SCHEMA_VERSION = 1;
    static final long MAX_FILE_BYTES = 4_096L;
    static final String FILE_NAME =
            "rotclient-session-start-new-transaction.json";
    static final String INVALID_WARNING =
            "Start New recovery marker is unreadable. Current Session and "
                    + "History were left unchanged for manual review.";

    enum LoadStatus {
        NONE,
        PENDING,
        INVALID
    }

    record Pending(
            int schemaVersion,
            String sourceSessionKey,
            String archiveFingerprint,
            long boundaryMillis) {
    }

    record LoadResult(
            LoadStatus status,
            Optional<Pending> pending,
            String warning) {
        LoadResult {
            if (status == null || pending == null) {
                throw new IllegalArgumentException(
                        "Journal load result fields cannot be null");
            }
            warning = warning == null ? "" : warning;
        }
    }

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private CurrentSessionStartNewJournal() {
    }

    static Path defaultPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static boolean begin(
            Path path,
            RotClientCurrentSessionConfig current,
            RotClientSessionFreeze frozen) {
        if (path == null || current == null || frozen == null
                || current.sessionId == null
                || current.sessionId.isBlank()
                || Files.exists(path)) {
            return false;
        }
        try {
            Pending pending = new Pending(
                    SCHEMA_VERSION,
                    sessionKey(current.sessionId),
                    MiningSessionHistoryCodec.contentFingerprint(frozen),
                    frozen.stoppedAtMillis());
            if (!valid(pending)) {
                return false;
            }
            String json = GSON.toJson(pending);
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_BYTES) {
                return false;
            }
            AtomicFileWriter.writeAtomically(path, json);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static LoadResult load(Path path) {
        if (path == null) {
            return invalid();
        }
        if (!Files.exists(path)) {
            return new LoadResult(
                    LoadStatus.NONE, Optional.empty(), "");
        }
        try {
            long size = Files.size(path);
            if (size <= 0L || size > MAX_FILE_BYTES) {
                return invalid();
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Pending pending = GSON.fromJson(json, Pending.class);
            if (!valid(pending)) {
                return invalid();
            }
            return new LoadResult(
                    LoadStatus.PENDING, Optional.of(pending), "");
        } catch (Exception ignored) {
            return invalid();
        }
    }

    static boolean matchesCurrent(
            Pending pending,
            RotClientCurrentSessionConfig current) {
        if (pending == null || current == null
                || current.sessionId == null
                || current.sessionId.isBlank()) {
            return false;
        }
        return pending.sourceSessionKey().equals(
                sessionKey(current.sessionId));
    }

    static boolean clear(Path path) {
        if (path == null) {
            return false;
        }
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean valid(Pending pending) {
        return pending != null
                && pending.schemaVersion() == SCHEMA_VERSION
                && isSha256(pending.sourceSessionKey())
                && isSha256(pending.archiveFingerprint())
                && pending.boundaryMillis() > 0L;
    }

    private static boolean isSha256(String value) {
        if (value == null || value.length() != 64) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f')) {
                return false;
            }
        }
        return true;
    }

    private static String sessionKey(String sessionId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    ("rotclient-start-new:" + sessionId)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static LoadResult invalid() {
        return new LoadResult(
                LoadStatus.INVALID,
                Optional.empty(),
                INVALID_WARNING);
    }
}
