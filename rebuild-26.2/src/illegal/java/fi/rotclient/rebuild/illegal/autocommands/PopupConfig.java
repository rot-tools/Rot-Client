// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class PopupConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("autocommands");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("rotclientillegal");
    private static final Path FILE = DIRECTORY.resolve("autocommands.json");

    public boolean enabled = false;
    public double durationSeconds = 4.0;
    public double scale = 1.5;
    public int textColor = 0x63F594;
    @SerializedName(value = "textOpacity", alternate = "opacity")
    public double textOpacity = 1.0;
    public boolean backgroundEnabled = true;
    public int backgroundColor = 0x101914;
    public double backgroundOpacity = Double.NaN;
    public double horizontalPosition = 0.5;
    public double verticalPosition = 0.32;
    public List<CommandShortcut> shortcuts;

    public static PopupConfig load() {
        Path input = FILE;
        PopupConfig config = new PopupConfig();
        if (Files.exists(input)) {
            try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                PopupConfig decoded = GSON.fromJson(reader, PopupConfig.class);
                if (decoded == null) throw new IOException("Empty configuration");
                config = decoded;
            } catch (IOException | RuntimeException error) {
                LOGGER.warn("Cannot read {}. Using default settings.", input, error);
            }
        }
        if (config.shortcuts == null) config.shortcuts = defaultShortcuts();
        config.validate();
        return config;
    }

    private static ArrayList<CommandShortcut> defaultShortcuts() {
        return new ArrayList<>(List.of(
                new CommandShortcut("/warp mines", "", ""),
                new CommandShortcut("/warp basecamp", "", ""),
                new CommandShortcut("/wardrobe", "", "")));
    }

    private void validate() {
        durationSeconds = Math.round(clamp(durationSeconds, 0.1, 5, 4) * 10) / 10.0;
        scale = clamp(scale, 0.5, 3, 1.5);
        textColor &= 0xFFFFFF;
        textOpacity = clamp(textOpacity, 0, 1, 1);
        backgroundColor &= 0xFFFFFF;
        backgroundOpacity = clamp(backgroundOpacity, 0, 1, textOpacity * 0.82);
        horizontalPosition = clamp(horizontalPosition, 0, 1, 0.5);
        verticalPosition = clamp(verticalPosition, 0, 1, 0.32);
        var validated = new ArrayList<CommandShortcut>();
        var keys = new HashSet<String>();
        for (CommandShortcut shortcut : shortcuts) {
            if (shortcut == null) continue;
            String command = CommandShortcut.cleanCommand(shortcut.command());
            if (command.isEmpty()) continue;
            String key = CommandShortcut.cleanKey(shortcut.key());
            if (!key.isEmpty() && !keys.add(key)) key = "";
            validated.add(new CommandShortcut(command, CommandShortcut.cleanLabel(shortcut.label()), key));
        }
        shortcuts = validated;
    }

    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    public boolean save() {
        validate();
        try {
            Files.createDirectories(DIRECTORY);
            Path staging = Files.createTempFile(DIRECTORY, "autocommands-", ".tmp");
            try {
                Files.writeString(staging, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
                try {
                    Files.move(staging, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(staging, FILE, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(staging);
            }
            return true;
        } catch (IOException error) {
            LOGGER.warn("Cannot save {}", FILE, error);
            return false;
        }
    }
}
