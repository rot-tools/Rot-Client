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
 * Loads and saves user-created Rot Client loadouts.
 *
 * Loadouts are persisted separately from settings profiles and tracker data.
 */
final class RotClientLoadoutStore {
    static final String FILE_NAME =
            "rotclient-loadouts.json";

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private RotClientLoadoutStore() {
    }

    static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static RotClientLoadoutConfig load() {
        Path path =
                configPath();

        if (!Files.exists(path)) {
            return RotClientLoadoutConfig.defaults();
        }

        try (Reader reader =
                     Files.newBufferedReader(path)) {

            JsonElement root =
                    GSON.fromJson(
                            reader,
                            JsonElement.class);

            if (root == null
                    || !root.isJsonObject()) {

                return RotClientLoadoutConfig.defaults();
            }

            JsonObject object =
                    root.getAsJsonObject();

            int sourceVersion =
                    object.has("schemaVersion")
                            ? object.get("schemaVersion")
                            .getAsInt()
                            : 0;

            RotClientLoadoutConfig config =
                    GSON.fromJson(
                            object,
                            RotClientLoadoutConfig.class);

            if (config == null) {
                return RotClientLoadoutConfig.defaults();
            }

            config.normalize();

            /*
             * Preserve the newer version number so save() refuses to overwrite
             * data from a newer Rot Client version.
             */
            if (sourceVersion
                    > RotClientLoadoutConfig.SCHEMA_VERSION) {

                config.schemaVersion =
                        sourceVersion;
            }

            return config;

        } catch (Exception ignored) {
            return RotClientLoadoutConfig.defaults();
        }
    }

    static boolean save(
            RotClientLoadoutConfig config) {

        if (config == null) {
            return false;
        }

        /*
         * Never silently downgrade a file written by a newer version.
         */
        if (config.schemaVersion
                > RotClientLoadoutConfig.SCHEMA_VERSION) {

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