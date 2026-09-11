package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class MarketWatchStore {
    static final String FILE_NAME =
            "rotclient-market-watch.json";

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private MarketWatchStore() {
    }

    static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static MarketWatchConfig load() {
        return load(configPath());
    }

    static MarketWatchConfig load(Path path) {
        if (path == null || !Files.exists(path)) {
            return MarketWatchConfig.defaults();
        }

        try (Reader reader =
                Files.newBufferedReader(
                        path,
                        StandardCharsets.UTF_8)) {

            JsonElement root =
                    GSON.fromJson(
                            reader,
                            JsonElement.class);

            if (root == null || !root.isJsonObject()) {
                return MarketWatchConfig.defaults();
            }

            JsonObject object =
                    root.getAsJsonObject();

            int sourceVersion =
                    object.has("schemaVersion")
                            ? object.get("schemaVersion").getAsInt()
                            : 0;

            MarketWatchConfig config =
                    GSON.fromJson(
                            object,
                            MarketWatchConfig.class);

            if (config == null) {
                return MarketWatchConfig.defaults();
            }

            config.normalize();

            if (sourceVersion
                    > MarketWatchConfig.SCHEMA_VERSION) {
                config.schemaVersion =
                        sourceVersion;
            }

            return config;

        } catch (Exception ignored) {
            return MarketWatchConfig.defaults();
        }
    }

    static boolean save(MarketWatchConfig config) {
        return save(
                configPath(),
                config);
    }

    static boolean save(
            Path path,
            MarketWatchConfig config) {

        if (path == null || config == null) {
            return false;
        }

        if (config.schemaVersion
                > MarketWatchConfig.SCHEMA_VERSION) {
            return false;
        }

        try {
            config.normalize();

            AtomicFileWriter.writeAtomically(
                    path,
                    GSON.toJson(config));

            return true;

        } catch (Exception ignored) {
            return false;
        }
    }
}