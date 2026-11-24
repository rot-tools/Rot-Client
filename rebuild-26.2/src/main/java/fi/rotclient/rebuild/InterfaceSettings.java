package fi.rotclient.rebuild;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
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
    double hudX = 0.5;
    double hudY = 0.45;
    double hudScale = 1.0;
    double hudOpacity = 0.85;
    boolean hudShadow = true;
    boolean hudShowBackground = true;
    Set<String> hiddenVanillaLayers = new HashSet<>();
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
        settings.hudX = clamp(settings.hudX, 0, 1, 0.5);
        settings.hudY = clamp(settings.hudY, 0, 1, 0.45);
        settings.hudScale = clamp(settings.hudScale, 0.5, 2, 1);
        settings.hudOpacity = clamp(settings.hudOpacity, 0, 1, 0.85);
        if (settings.hiddenVanillaLayers == null) settings.hiddenVanillaLayers = new HashSet<>();
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

    void resetHud() {
        hudX = 0.5;
        hudY = 0.45;
        hudScale = 1;
        hudOpacity = 0.85;
        hudBackground = 0x100C12;
        hudText = DEFAULT_TEXT;
        hudShadow = true;
        hudShowBackground = true;
        save();
    }

    static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
