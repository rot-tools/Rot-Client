package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves {@link RotClientAppearanceConfig} separately from TrackerStore.
 */
final class RotClientAppearanceStore {
    static final String FILE_NAME = "rotclient-appearance.json";

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private RotClientAppearanceStore() {
    }

    static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    static RotClientAppearanceConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return RotClientAppearanceConfig.defaults();
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return RotClientAppearanceConfig.defaults();
            }
            RotClientAppearanceConfig config =
                    GSON.fromJson(json, RotClientAppearanceConfig.class);
            if (config == null) {
                return RotClientAppearanceConfig.defaults();
            }
            config.normalize();
            return config;
        } catch (Exception ignored) {
            return RotClientAppearanceConfig.defaults();
        }
    }

    static boolean save(RotClientAppearanceConfig config) {
        if (config == null) {
            return false;
        }
        RotClientAppearanceConfig safe = config.copy();
        safe.normalize();
        try {
            AtomicFileWriter.writeAtomically(
                    configPath(),
                    GSON.toJson(safe));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
