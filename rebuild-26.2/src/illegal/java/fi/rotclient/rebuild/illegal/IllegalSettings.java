package fi.rotclient.rebuild.illegal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class IllegalSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger("rotclient-illegal-options");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("rotclientillegal");
    private static final Path FILE = DIRECTORY.resolve("modules.json");

    boolean fullbright;
    boolean alwaysNight;
    boolean forceBoth;
    boolean autoCommands;
    boolean autoSprint;
    boolean clickGui;
    int autoSprintKey = GLFW.GLFW_KEY_UNKNOWN;
    int clickGuiKey = GLFW.GLFW_KEY_UNKNOWN;
    AutoLoadoutSettings autoLoadout = new AutoLoadoutSettings();

    static IllegalSettings load() {
        if (Files.isRegularFile(FILE)) {
            try (var input = Files.newBufferedReader(FILE)) {
                IllegalSettings restored = GSON.fromJson(input, IllegalSettings.class);
                if (restored != null) {
                    if (restored.autoLoadout == null) restored.autoLoadout = new AutoLoadoutSettings();
                    restored.autoLoadout.normalize();
                    return restored;
                }
            } catch (IOException | RuntimeException error) {
                LOGGER.warn("Could not read {}", FILE, error);
            }
        }
        return new IllegalSettings();
    }

    void save() {
        try {
            Files.createDirectories(DIRECTORY);
            Path temporary = Files.createTempFile(DIRECTORY, "modules-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(this) + System.lineSeparator());
                try {
                    Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException error) {
            LOGGER.warn("Could not save {}", FILE, error);
        }
    }
}
