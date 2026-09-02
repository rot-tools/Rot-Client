package fi.rotclient;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time byte-copy migration from legacy MiningTracker config/history files
 * to Rot Client paths. Never deletes or modifies legacy files.
 */
final class RotClientLegacyDataMigrator {
    static final String LEGACY_CONFIG = "miningtracker.json";
    static final String NEW_CONFIG = "rotclient.json";
    static final String LEGACY_HISTORY = "miningtracker-session-history.json";
    static final String NEW_HISTORY = "rotclient-session-history.json";

    record MigrationResult(
            boolean migratedConfig,
            boolean migratedHistory,
            boolean skippedBecauseNewExists,
            String message) {
    }

    private RotClientLegacyDataMigrator() {
    }

    static MigrationResult migrateIfNeeded(Path configDir) {
        if (configDir == null) {
            return new MigrationResult(false, false, false, "");
        }
        return migratePaths(
                configDir.resolve(LEGACY_CONFIG),
                configDir.resolve(NEW_CONFIG),
                configDir.resolve(LEGACY_HISTORY),
                configDir.resolve(NEW_HISTORY));
    }

    static MigrationResult migratePaths(
            Path legacyConfig,
            Path newConfig,
            Path legacyHistory,
            Path newHistory) {
        List<String> notes = new ArrayList<>();
        boolean migratedConfig = false;
        boolean migratedHistory = false;
        boolean skippedBecauseNewExists = false;

        FileMigrationResult configResult =
                migrateFile(legacyConfig, newConfig);
        migratedConfig = configResult.migrated();
        skippedBecauseNewExists |= configResult.skippedBecauseNewExists();
        appendNote(notes, configResult.message());

        FileMigrationResult historyResult =
                migrateFile(legacyHistory, newHistory);
        migratedHistory = historyResult.migrated();
        skippedBecauseNewExists |= historyResult.skippedBecauseNewExists();
        appendNote(notes, historyResult.message());

        return new MigrationResult(
                migratedConfig,
                migratedHistory,
                skippedBecauseNewExists,
                String.join(" ", notes).trim());
    }

    record FileMigrationResult(
            boolean migrated,
            boolean skippedBecauseNewExists,
            String message) {
    }

    static FileMigrationResult migrateFile(Path legacyPath, Path newPath) {
        if (legacyPath == null || newPath == null) {
            return new FileMigrationResult(false, false, "");
        }
        try {
            if (Files.exists(newPath)) {
                if (Files.exists(legacyPath)) {
                    return new FileMigrationResult(
                            false,
                            true,
                            describe(newPath.getFileName())
                                    + " already exists; legacy file preserved.");
                }
                return new FileMigrationResult(false, false, "");
            }
            if (!Files.isRegularFile(legacyPath)) {
                return new FileMigrationResult(false, false, "");
            }
            copyBytesAtomically(legacyPath, newPath);
            if (!Files.isRegularFile(newPath)) {
                return new FileMigrationResult(
                        false,
                        false,
                        "Migration failed for "
                                + describe(newPath.getFileName()) + ".");
            }
            return new FileMigrationResult(
                    true,
                    false,
                    "Migrated "
                            + describe(legacyPath.getFileName())
                            + " to "
                            + describe(newPath.getFileName()) + ".");
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(newPath);
            } catch (IOException ignored) {
            }
            return new FileMigrationResult(
                    false,
                    false,
                    "Migration failed for "
                            + describe(legacyPath.getFileName()) + ".");
        }
    }

    private static void copyBytesAtomically(Path source, Path target)
            throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path directory = parent != null ? parent : Path.of(".");
        Path temp = Files.createTempFile(
                directory,
                target.getFileName().toString() + ".",
                ".tmp");
        try {
            Files.copy(
                    source,
                    temp,
                    StandardCopyOption.REPLACE_EXISTING);
            try (var channel = java.nio.channels.FileChannel.open(
                    temp,
                    StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            temp = null;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void appendNote(List<String> notes, String message) {
        if (message != null && !message.isBlank()) {
            notes.add(message);
        }
    }

    private static String describe(Path fileName) {
        return fileName == null ? "file" : fileName.toString();
    }
}
