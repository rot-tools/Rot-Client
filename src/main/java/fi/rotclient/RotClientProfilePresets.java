package fi.rotclient;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads the example profiles bundled in the jar.
 *
 * The list is {@code index.json} (an array of ids) plus one
 * {@code <id>.json} per preset. A file that is missing, unreadable or fails
 * {@link RotClientProfilePreset#problems()} is skipped, so a bad example can
 * never break the Profiles page or the game; the build's tests fail instead.
 */
final class RotClientProfilePresets {
    static final String DIRECTORY = "/assets/rotclient/profile-presets/";

    private static final Gson GSON = new Gson();

    private static volatile List<RotClientProfilePreset> cached;

    private RotClientProfilePresets() {
    }

    /** Valid bundled presets, in index order. */
    static List<RotClientProfilePreset> bundled() {
        List<RotClientProfilePreset> presets = cached;
        if (presets == null) {
            presets = load();
            cached = presets;
        }
        return presets;
    }

    static RotClientProfilePreset findById(String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (RotClientProfilePreset preset : bundled()) {
            if (preset.id.toLowerCase(Locale.ROOT).equals(wanted)) {
                return preset;
            }
        }
        return null;
    }

    /** Every id in the index whether or not it loaded; for tests. */
    static List<String> indexedIds() {
        String[] ids = read("index.json", String[].class);
        if (ids == null) {
            return List.of();
        }
        // The Dungeon preset follows its Plus-only runtime. Keep the shared
        // index stable so Plus still offers the same profile order.
        return java.util.Arrays.stream(ids)
                .filter(id -> QolFlavorSupport.isPlus() || !"dungeons".equals(id))
                .toList();
    }

    /** Reads one preset without validating it; for tests. Null if unreadable. */
    static RotClientProfilePreset read(String id) {
        RotClientProfilePreset preset = read(id + ".json", RotClientProfilePreset.class);
        if (preset != null) {
            preset.normalize();
        }
        return preset;
    }

    private static List<RotClientProfilePreset> load() {
        List<RotClientProfilePreset> loaded = new ArrayList<>();

        for (String id : indexedIds()) {
            RotClientProfilePreset preset = read(id);

            if (preset != null && preset.problems().isEmpty()) {
                loaded.add(preset);
            }
        }

        return List.copyOf(loaded);
    }

    private static <T> T read(String file, Class<T> type) {
        try (InputStream in = RotClientProfilePresets.class
                .getResourceAsStream(DIRECTORY + file)) {

            if (in == null) {
                return null;
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, type);
            }
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }
}
