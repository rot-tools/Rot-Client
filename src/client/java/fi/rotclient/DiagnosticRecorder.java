package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DiagnosticRecorder {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static BufferedWriter writer;
    private static Path currentPath;

    private DiagnosticRecorder() {
    }

    public static synchronized Path start() throws IOException {
        stop();
        currentPath = outputPath(
                FabricLoader.getInstance().getConfigDir(),
                FILE_TIME.format(LocalDateTime.now()));
        Files.createDirectories(currentPath.getParent());
        writer = Files.newBufferedWriter(currentPath);
        record("START", "Rot Client targeted diagnostic; no chat, coordinates, or authentication data is recorded");
        return currentPath;
    }

    static Path outputPath(Path configDir, String timestamp) {
        if (configDir == null) {
            throw new IllegalArgumentException("configDir is required");
        }
        String safeTimestamp = timestamp == null ? "" : timestamp.trim();
        if (safeTimestamp.isEmpty()) {
            throw new IllegalArgumentException("timestamp is required");
        }
        return configDir.resolve("rotclient")
                .resolve("diagnostics")
                .resolve("rotclient-diagnostic-" + safeTimestamp + ".log");
    }

    public static synchronized Path stop() {
        Path path = currentPath;
        if (writer != null) {
            try {
                record("STOP", "diagnostic ended");
                writer.close();
            } catch (IOException ignored) {
            }
        }
        writer = null;
        currentPath = null;
        return path;
    }

    public static synchronized boolean isRecording() {
        return writer != null;
    }

    public static synchronized void record(String event, String details) {
        if (writer == null) return;
        try {
            writer.write(System.currentTimeMillis() + "\t" + sanitize(event) + "\t" + sanitize(details));
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    private static String sanitize(String text) {
        return text == null ? "" : text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }
}
