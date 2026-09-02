package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads/saves {@link RotClientCurrentSessionConfig} to
 * {@code rotclient-current-session.json}. Malformed payloads fall back to
 * Session 1 defaults. Schema upgrades take a one-shot {@code .bak}.
 */
final class RotClientCurrentSessionStore {
    static final String FILE_NAME = "rotclient-current-session.json";
    static final String CORRUPT_FILE_NAME =
            "rotclient-current-session.corrupt.json";
    static final long SAVE_DEBOUNCE_MILLIS = 3_000L;
    static final long SAVE_RETRY_BACKOFF_MILLIS = 30_000L;
    static final long MAX_FILE_BYTES = 64L * 1024L * 1024L;
    static final String RECOVERY_WARNING =
            "Current Session could not be read. The original file was "
                    + "preserved for recovery and automatic persistence is blocked.";
    static final String FUTURE_SCHEMA_WARNING =
            "Current Session was written by a newer Rot Client version. "
                    + "Automatic persistence is blocked to prevent a downgrade.";
    static final String MIGRATION_BACKUP_WARNING =
            "Current Session migration backup could not be created. "
                    + "The original file was left unchanged and persistence is blocked.";

    enum LoadStatus {
        AVAILABLE,
        RECOVERY_REQUIRED,
        MIGRATION_BACKUP_FAILED,
        UNSUPPORTED_FUTURE_SCHEMA
    }

    record LoadResult(
            RotClientCurrentSessionConfig config,
            LoadStatus status,
            String warning) {
        LoadResult {
            if (config == null || status == null) {
                throw new IllegalArgumentException(
                        "Current Session load fields cannot be null");
            }
            warning = warning == null ? "" : warning;
        }

        boolean writable() {
            return status == LoadStatus.AVAILABLE;
        }
    }

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private RotClientCurrentSessionStore() {
    }

    static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static LoadResult load() {
        return load(configPath());
    }

    static LoadResult load(Path path) {
        if (path == null) {
            return recoveryRequired();
        }
        if (!Files.exists(path)) {
            return available(RotClientCurrentSessionConfig.defaults());
        }
        try {
            if (Files.size(path) > MAX_FILE_BYTES) {
                quarantineCorrupt(path);
                return recoveryRequired();
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                quarantineCorrupt(path);
                return recoveryRequired();
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            RotClientCurrentSessionConfig config =
                    GSON.fromJson(root, RotClientCurrentSessionConfig.class);
            if (config == null) {
                quarantineCorrupt(path);
                return recoveryRequired();
            }
            int loadedSchema = config.schemaVersion;
            if (loadedSchema > RotClientCurrentSessionConfig.SCHEMA_VERSION) {
                return new LoadResult(
                        RotClientCurrentSessionConfig.defaults(),
                        LoadStatus.UNSUPPORTED_FUTURE_SCHEMA,
                        FUTURE_SCHEMA_WARNING);
            }
            boolean backupReady = maybeBackupOnce(path, loadedSchema);
            migrate(config);
            config.normalize();
            RotClientCurrentSessionConfig migrated =
                    RotClientCurrentSessionMath.ensureSession1IfMissing(config);
            return backupReady
                    ? available(migrated)
                    : new LoadResult(
                    migrated,
                    LoadStatus.MIGRATION_BACKUP_FAILED,
                    MIGRATION_BACKUP_WARNING);
        } catch (Exception ignored) {
            quarantineCorrupt(path);
            return recoveryRequired();
        }
    }

    static boolean save(RotClientCurrentSessionConfig config) {
        if (config == null) {
            return false;
        }
        RotClientCurrentSessionConfig safe = config.copy();
        migrate(safe);
        safe.normalize();
        try {
            AtomicFileWriter.writeAtomically(configPath(), GSON.toJson(safe));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static void migrate(RotClientCurrentSessionConfig config) {
        if (config == null) {
            return;
        }
        if (config.schemaVersion <= 0) {
            config.schemaVersion = RotClientCurrentSessionConfig.SCHEMA_VERSION;
        }
        if (config.schemaVersion
                < RotClientCurrentSessionConfig.SCHEMA_VERSION) {
            config.schemaVersion = RotClientCurrentSessionConfig.SCHEMA_VERSION;
        }
    }

    /** Test helper: parse JSON without requiring Fabric config dir. */
    static RotClientCurrentSessionConfig parseJson(String json) {
        return parseJson(json, null);
    }

    private static RotClientCurrentSessionConfig parseJson(
            String json,
            Path backupSource) {
        if (json == null || json.isBlank()) {
            return RotClientCurrentSessionConfig.defaults();
        }
        try {
            RotClientCurrentSessionConfig config =
                    GSON.fromJson(json, RotClientCurrentSessionConfig.class);
            if (config == null) {
                return RotClientCurrentSessionConfig.defaults();
            }
            int loadedSchema = config.schemaVersion;
            if (backupSource != null) {
                maybeBackupOnce(backupSource, loadedSchema);
            }
            migrate(config);
            config.normalize();
            return RotClientCurrentSessionMath.ensureSession1IfMissing(config);
        } catch (Exception ignored) {
            return RotClientCurrentSessionConfig.defaults();
        }
    }

    static String toJson(RotClientCurrentSessionConfig config) {
        RotClientCurrentSessionConfig safe = config == null
                ? RotClientCurrentSessionConfig.defaults()
                : config.copy();
        migrate(safe);
        safe.normalize();
        return GSON.toJson(safe);
    }

    private static boolean maybeBackupOnce(Path path, int loadedSchema) {
        if (loadedSchema >= RotClientCurrentSessionConfig.SCHEMA_VERSION) {
            return true;
        }
        try {
            Path backup = path.resolveSibling(FILE_NAME + ".bak");
            if (Files.exists(backup)) {
                return backupMatches(path, backup);
            }
            synchronized (RotClientCurrentSessionStore.class) {
                if (Files.exists(backup)) {
                    return backupMatches(path, backup);
                }
                try {
                    Files.copy(path, backup);
                } catch (FileAlreadyExistsException ignored) {
                    // Another process won the race. Accept its backup only if
                    // it is an exact copy of the source being migrated.
                }
                return backupMatches(path, backup);
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean backupMatches(Path source, Path backup)
            throws java.io.IOException {
        return Files.isRegularFile(backup)
                && Files.mismatch(source, backup) == -1L;
    }

    private static LoadResult available(
            RotClientCurrentSessionConfig config) {
        return new LoadResult(config, LoadStatus.AVAILABLE, "");
    }

    private static LoadResult recoveryRequired() {
        return new LoadResult(
                RotClientCurrentSessionConfig.defaults(),
                LoadStatus.RECOVERY_REQUIRED,
                RECOVERY_WARNING);
    }

    private static void quarantineCorrupt(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.move(
                        path,
                        path.resolveSibling(CORRUPT_FILE_NAME));
            }
        } catch (Exception ignored) {
            // Leave the unreadable original in place. The LoadResult still
            // blocks writes, so it cannot be overwritten automatically.
        }
    }
}
