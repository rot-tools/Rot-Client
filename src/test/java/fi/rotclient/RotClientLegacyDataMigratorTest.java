package fi.rotclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RotClientLegacyDataMigratorTest {
    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyConfigBytesAndPreservesLegacyFile() throws Exception {
        Path legacy = tempDir.resolve(RotClientLegacyDataMigrator.LEGACY_CONFIG);
        byte[] payload = "{\"dataVersion\":8,\"enabled\":true}".getBytes(
                StandardCharsets.UTF_8);
        Files.write(legacy, payload);

        RotClientLegacyDataMigrator.MigrationResult result =
                RotClientLegacyDataMigrator.migrateIfNeeded(tempDir);

        Path migrated = tempDir.resolve(RotClientLegacyDataMigrator.NEW_CONFIG);
        assertTrue(result.migratedConfig());
        assertFalse(result.migratedHistory());
        assertFalse(result.skippedBecauseNewExists());
        assertTrue(Files.isRegularFile(migrated));
        assertArrayEquals(payload, Files.readAllBytes(migrated));
        assertArrayEquals(payload, Files.readAllBytes(legacy));
    }

    @Test
    void neitherFileIsNoOp() {
        RotClientLegacyDataMigrator.MigrationResult result =
                RotClientLegacyDataMigrator.migrateIfNeeded(tempDir);

        assertFalse(result.migratedConfig());
        assertFalse(result.migratedHistory());
        assertFalse(result.skippedBecauseNewExists());
        assertEquals("", result.message());
    }

    @Test
    void existingNewFileWinsWithoutTouchingLegacy() throws Exception {
        Path legacy = tempDir.resolve(RotClientLegacyDataMigrator.LEGACY_CONFIG);
        Path migrated = tempDir.resolve(RotClientLegacyDataMigrator.NEW_CONFIG);
        Files.writeString(legacy, "legacy-only", StandardCharsets.UTF_8);
        Files.writeString(migrated, "new-wins", StandardCharsets.UTF_8);

        RotClientLegacyDataMigrator.MigrationResult result =
                RotClientLegacyDataMigrator.migrateIfNeeded(tempDir);

        assertFalse(result.migratedConfig());
        assertTrue(result.skippedBecauseNewExists());
        assertEquals("legacy-only", Files.readString(legacy, StandardCharsets.UTF_8));
        assertEquals("new-wins", Files.readString(migrated, StandardCharsets.UTF_8));
    }

    @Test
    void repeatedMigrationIsIdempotent() throws Exception {
        Path legacy = tempDir.resolve(RotClientLegacyDataMigrator.LEGACY_HISTORY);
        Files.writeString(legacy, "{\"schemaVersion\":1}", StandardCharsets.UTF_8);

        RotClientLegacyDataMigrator.MigrationResult first =
                RotClientLegacyDataMigrator.migrateIfNeeded(tempDir);
        RotClientLegacyDataMigrator.MigrationResult second =
                RotClientLegacyDataMigrator.migrateIfNeeded(tempDir);

        assertTrue(first.migratedHistory());
        assertFalse(second.migratedHistory());
        assertTrue(second.skippedBecauseNewExists());
        assertEquals(
                Files.readString(legacy, StandardCharsets.UTF_8),
                Files.readString(
                        tempDir.resolve(RotClientLegacyDataMigrator.NEW_HISTORY),
                        StandardCharsets.UTF_8));
    }

    @Test
    void failureLeavesLegacyUntouched() throws Exception {
        Path legacy = tempDir.resolve(RotClientLegacyDataMigrator.LEGACY_CONFIG);
        Files.writeString(legacy, "keep-me", StandardCharsets.UTF_8);

        Path blockedParent = tempDir.resolve("blocked");
        Files.writeString(blockedParent, "not-a-directory", StandardCharsets.UTF_8);
        Path newPath = blockedParent.resolve(RotClientLegacyDataMigrator.NEW_CONFIG);

        RotClientLegacyDataMigrator.FileMigrationResult result =
                RotClientLegacyDataMigrator.migrateFile(legacy, newPath);

        assertFalse(result.migrated());
        assertFalse(result.skippedBecauseNewExists());
        assertFalse(Files.exists(newPath));
        assertEquals("keep-me", Files.readString(legacy, StandardCharsets.UTF_8));
    }

    @Test
    void newPathDirectorySkipsMigration() throws Exception {
        Path legacy = tempDir.resolve(RotClientLegacyDataMigrator.LEGACY_CONFIG);
        Path newPath = tempDir.resolve(RotClientLegacyDataMigrator.NEW_CONFIG);
        Files.writeString(legacy, "legacy", StandardCharsets.UTF_8);
        Files.createDirectory(newPath);

        RotClientLegacyDataMigrator.FileMigrationResult result =
                RotClientLegacyDataMigrator.migrateFile(legacy, newPath);

        assertFalse(result.migrated());
        assertTrue(result.skippedBecauseNewExists());
        assertEquals("legacy", Files.readString(legacy, StandardCharsets.UTF_8));
    }
}
