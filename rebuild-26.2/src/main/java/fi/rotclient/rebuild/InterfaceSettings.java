package fi.rotclient.rebuild;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InterfaceSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("rotclient-rebuild");
    static final int DEFAULT_PANEL = 0x18141C;
    static final int DEFAULT_ACCENT = 0xE11D48;
    static final int DEFAULT_TEXT = 0xF8F5FF;

    int panel = DEFAULT_PANEL;
    int accent = DEFAULT_ACCENT;
    int hudBackground = 0x100C12;
    int hudText = DEFAULT_TEXT;
    double hudOpacity = 0.85;
    int hudInspectorX = HudEditorChromePolicy.UNPLACED;
    int hudInspectorY = HudEditorChromePolicy.UNPLACED;
    Set<String> hiddenVanillaLayers = new HashSet<>();
    Map<String, HudPlacement> vanillaHudPlacements = new HashMap<>();
    private transient Path path;

    static InterfaceSettings load(Path path) {
        InterfaceSettings settings = new InterfaceSettings();
        if (Files.isRegularFile(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                InterfaceSettings parsed = GSON.fromJson(reader, InterfaceSettings.class);
                if (parsed != null) settings = parsed;
            } catch (IOException | RuntimeException error) {
                LOGGER.warn("Could not read interface settings at {}", path, error);
            }
        }
        settings.path = path;
        if (settings.hudInspectorX == 12 && settings.hudInspectorY == 38) {
            settings.hudInspectorX = HudEditorChromePolicy.UNPLACED;
            settings.hudInspectorY = HudEditorChromePolicy.UNPLACED;
        }
        settings.hudOpacity = clamp(settings.hudOpacity, 0, 1, 0.85);
        if (settings.hiddenVanillaLayers == null) settings.hiddenVanillaLayers = new HashSet<>();
        if (settings.vanillaHudPlacements == null) settings.vanillaHudPlacements = new HashMap<>();
        settings.vanillaHudPlacements.values().removeIf(java.util.Objects::isNull);
        return settings;
    }

    void save() {
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "rot-interface-", ".tmp");
            Files.writeString(temporary, GSON.toJson(this));
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            LOGGER.warn("Could not save interface settings at {}", path, error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException error) {
                    LOGGER.warn("Could not remove temporary settings {}", temporary, error);
                }
            }
        }
    }

    static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
