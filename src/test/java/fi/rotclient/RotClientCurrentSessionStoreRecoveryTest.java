package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RotClientCurrentSessionStoreRecoveryTest {
    @TempDir
    Path tempDir;

    @Test
    void malformedCurrentSessionIsQuarantinedInsteadOfOverwritten()
            throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        String malformed = "{not-json";
        Files.writeString(current, malformed, StandardCharsets.UTF_8);

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(
                RotClientCurrentSessionStore.LoadStatus.RECOVERY_REQUIRED,
                result.status());
        assertFalse(Files.exists(current));
        Path quarantined = tempDir.resolve(
                RotClientCurrentSessionStore.CORRUPT_FILE_NAME);
        assertTrue(Files.isRegularFile(quarantined));
        assertEquals(malformed,
                Files.readString(quarantined, StandardCharsets.UTF_8));
    }

    @Test
    void futureSchemaRemainsInPlaceAndIsNeverDowngraded() throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        String future = """
                {"schemaVersion":99,"sessionId":"future-ledger"}
                """;
        Files.writeString(current, future, StandardCharsets.UTF_8);

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(
                RotClientCurrentSessionStore.LoadStatus
                        .UNSUPPORTED_FUTURE_SCHEMA,
                result.status());
        assertEquals(future,
                Files.readString(current, StandardCharsets.UTF_8));
        assertFalse(Files.exists(tempDir.resolve(
                RotClientCurrentSessionStore.CORRUPT_FILE_NAME)));
    }

    @Test
    void existingRecoveryArtifactIsNeverOverwritten() throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        Path recovery = tempDir.resolve(
                RotClientCurrentSessionStore.CORRUPT_FILE_NAME);
        Files.writeString(current, "{second-broken", StandardCharsets.UTF_8);
        Files.writeString(recovery, "first-broken", StandardCharsets.UTF_8);

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(
                RotClientCurrentSessionStore.LoadStatus.RECOVERY_REQUIRED,
                result.status());
        assertEquals("first-broken",
                Files.readString(recovery, StandardCharsets.UTF_8));
        assertEquals("{second-broken",
                Files.readString(current, StandardCharsets.UTF_8));
    }

    @Test
    void blockedRecoveryStateCannotCollectEphemeralCredits() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.restoreLoadedState(new RotClientCurrentSessionStore.LoadResult(
                RotClientCurrentSessionConfig.defaults(),
                RotClientCurrentSessionStore.LoadStatus.RECOVERY_REQUIRED,
                RotClientCurrentSessionStore.RECOVERY_WARNING));

        assertTrue(session.isPaused());
        assertFalse(session.resume(
                session.snapshotConfig().startedAtMillis + 50L));
        assertTrue(session.isPaused());
        assertFalse(session.resumeAfterOffline(
                session.snapshotConfig().startedAtMillis + 100L));
        session.creditUnknown(
                "MUST_NOT_BE_EPHEMERAL",
                5L,
                SessionSourceType.MOB,
                session.snapshotConfig().startedAtMillis + 1L);
        assertTrue(session.snapshotConfig().items.isEmpty());
        assertFalse(session.persistenceWarning().isBlank());
    }

    @Test
    void failedLegacyBackupBlocksMigrationWritesAndPreservesSource()
            throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        String legacy = """
                {"schemaVersion":1,"sessionId":"legacy-session"}
                """;
        Files.writeString(current, legacy, StandardCharsets.UTF_8);
        Files.createDirectory(tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME + ".bak"));

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(
                RotClientCurrentSessionStore.LoadStatus
                        .MIGRATION_BACKUP_FAILED,
                result.status());
        assertFalse(result.writable());
        assertEquals(legacy,
                Files.readString(current, StandardCharsets.UTF_8));
        assertEquals(RotClientCurrentSessionConfig.SCHEMA_VERSION,
                result.config().schemaVersion);
    }

    @Test
    void partialPreexistingLegacyBackupIsNeverAccepted() throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        Path backup = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME + ".bak");
        String legacy = """
                {"schemaVersion":1,"sessionId":"legacy-session"}
                """;
        String partial = legacy.substring(0, legacy.length() / 2);
        Files.writeString(current, legacy, StandardCharsets.UTF_8);
        Files.writeString(backup, partial, StandardCharsets.UTF_8);

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(
                RotClientCurrentSessionStore.LoadStatus
                        .MIGRATION_BACKUP_FAILED,
                result.status());
        assertFalse(result.writable());
        assertEquals(legacy,
                Files.readString(current, StandardCharsets.UTF_8));
        assertEquals(partial,
                Files.readString(backup, StandardCharsets.UTF_8));
    }

    @Test
    void sameSizeStaleLegacyBackupIsNeverAccepted() throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        Path backup = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME + ".bak");
        String legacy = """
                {"schemaVersion":1,"sessionId":"legacy-session"}
                """;
        String stale = legacy.replace("legacy-session", "legacz-session");
        assertEquals(legacy.getBytes(StandardCharsets.UTF_8).length,
                stale.getBytes(StandardCharsets.UTF_8).length);
        Files.writeString(current, legacy, StandardCharsets.UTF_8);
        Files.writeString(backup, stale, StandardCharsets.UTF_8);

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(
                RotClientCurrentSessionStore.LoadStatus
                        .MIGRATION_BACKUP_FAILED,
                result.status());
        assertFalse(result.writable());
        assertEquals(legacy,
                Files.readString(current, StandardCharsets.UTF_8));
        assertEquals(stale,
                Files.readString(backup, StandardCharsets.UTF_8));
    }

    @Test
    void exactPreexistingLegacyBackupAllowsMigration() throws Exception {
        Path current = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME);
        Path backup = tempDir.resolve(
                RotClientCurrentSessionStore.FILE_NAME + ".bak");
        String legacy = """
                {"schemaVersion":1,"sessionId":"legacy-session"}
                """;
        Files.writeString(current, legacy, StandardCharsets.UTF_8);
        Files.writeString(backup, legacy, StandardCharsets.UTF_8);

        RotClientCurrentSessionStore.LoadResult result =
                RotClientCurrentSessionStore.load(current);

        assertEquals(RotClientCurrentSessionStore.LoadStatus.AVAILABLE,
                result.status());
        assertTrue(result.writable());
        assertEquals(legacy,
                Files.readString(current, StandardCharsets.UTF_8));
        assertEquals(legacy,
                Files.readString(backup, StandardCharsets.UTF_8));
    }
}
