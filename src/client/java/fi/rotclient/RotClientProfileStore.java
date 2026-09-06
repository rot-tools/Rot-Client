package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves user-created Rot Client profiles.
 *
 * Profile data is stored separately from tracker history and other persistent
 * gameplay data.
 */
final class RotClientProfileStore {
    static final String FILE_NAME = "rotclient-profiles.json";

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private RotClientProfileStore() {
    }

    static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static RotClientProfileConfig load() {
        Path path = configPath();

        if (!Files.exists(path)) {
            return RotClientProfileConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);

            if (root == null || !root.isJsonObject()) {
                return RotClientProfileConfig.defaults();
            }

            JsonObject object = root.getAsJsonObject();

            int sourceVersion = object.has("schemaVersion")
                    ? object.get("schemaVersion").getAsInt()
                    : 0;

            RotClientProfileConfig config =
                    GSON.fromJson(object, RotClientProfileConfig.class);

            if (config == null) {
                return RotClientProfileConfig.defaults();
            }

            config.normalize();

            /*
             * Remember that this file came from a newer build.
             * save() will refuse to overwrite a schema we do not understand.
             */
            if (sourceVersion > RotClientProfileConfig.SCHEMA_VERSION) {
                config.schemaVersion = sourceVersion;
            }

            return config;

        } catch (Exception ignored) {
            return RotClientProfileConfig.defaults();
        }
    }

    static boolean save(RotClientProfileConfig config) {
        if (config == null) {
            return false;
        }

        /*
         * Never silently downgrade a profile file written by a newer
         * Rot Client version.
         */
        if (config.schemaVersion > RotClientProfileConfig.SCHEMA_VERSION) {
            return false;
        }

        try {
            config.normalize();

            AtomicFileWriter.writeAtomically(
                    configPath(),
                    GSON.toJson(config));

            return true;

        } catch (Exception ignored) {
            return false;
        }
    }
}