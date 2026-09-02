package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads/saves {@link RotClientWorkspaceConfig} separately from tracker data.
 */
final class RotClientWorkspaceStore {
    static final String FILE_NAME = "rotclient-workspace.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object BACKUP_LOCK = new Object();
    private static boolean backupTakenForSession;

    private RotClientWorkspaceStore() {
    }

    static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    static RotClientWorkspaceConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return RotClientWorkspaceConfig.defaults();
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return RotClientWorkspaceConfig.defaults();
            }
            RotClientWorkspaceConfig config =
                    GSON.fromJson(json, RotClientWorkspaceConfig.class);
            if (config == null) {
                return RotClientWorkspaceConfig.defaults();
            }
            maybeBackupOnce(path, config.schemaVersion);
            migrate(config);
            config.normalize();
            return config;
        } catch (Exception ignored) {
            return RotClientWorkspaceConfig.defaults();
        }
    }

    static boolean save(RotClientWorkspaceConfig config) {
        if (config == null) {
            return false;
        }
        RotClientWorkspaceConfig safe = config.copy();
        migrate(safe);
        safe.normalize();
        try {
            AtomicFileWriter.writeAtomically(configPath(), GSON.toJson(safe));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static void migrate(RotClientWorkspaceConfig config) {
        if (config == null) {
            return;
        }
        // Forward-compatible: unknown older schemas get current schemaVersion
        // without wiping tab/layout fields already deserialized by Gson.
        if (config.schemaVersion <= 0) {
            config.schemaVersion = RotClientWorkspaceConfig.SCHEMA_VERSION;
        }
        if (config.schemaVersion < RotClientWorkspaceConfig.SCHEMA_VERSION) {
            config.schemaVersion = RotClientWorkspaceConfig.SCHEMA_VERSION;
        }
    }

    private static void maybeBackupOnce(Path path, int loadedSchema) {
        if (loadedSchema >= RotClientWorkspaceConfig.SCHEMA_VERSION) {
            return;
        }
        synchronized (BACKUP_LOCK) {
            if (backupTakenForSession) {
                return;
            }
            backupTakenForSession = true;
            try {
                Path backup = path.resolveSibling(FILE_NAME + ".bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
    }

    /** Test helper: parse JSON without Fabric config dir. */
    static RotClientWorkspaceConfig parseJson(String json) {
        if (json == null || json.isBlank()) {
            return RotClientWorkspaceConfig.defaults();
        }
        try {
            RotClientWorkspaceConfig config =
                    GSON.fromJson(json, RotClientWorkspaceConfig.class);
            if (config == null) {
                return RotClientWorkspaceConfig.defaults();
            }
            migrate(config);
            config.normalize();
            return config;
        } catch (Exception ignored) {
            return RotClientWorkspaceConfig.defaults();
        }
    }

    static String toJson(RotClientWorkspaceConfig config) {
        RotClientWorkspaceConfig safe = config == null
                ? RotClientWorkspaceConfig.defaults()
                : config.copy();
        safe.normalize();
        return GSON.toJson(safe);
    }
}
