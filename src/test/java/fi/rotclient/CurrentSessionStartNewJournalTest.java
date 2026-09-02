package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CurrentSessionStartNewJournalTest {
    @TempDir
    Path tempDir;

    @Test
    void roundTripStoresOnlyBoundedNonLedgerRecoveryMetadata()
            throws Exception {
        Path path = tempDir.resolve("transaction.json");
        RotClientCurrentSession current = new RotClientCurrentSession();
        long boundary = current.snapshotConfig().startedAtMillis + 100L;
        RotClientCurrentSessionConfig snapshot = current.snapshotConfig();
        RotClientSessionFreeze frozen = current.freeze(boundary);

        assertTrue(CurrentSessionStartNewJournal.begin(
                path, snapshot, frozen));
        CurrentSessionStartNewJournal.LoadResult loaded =
                CurrentSessionStartNewJournal.load(path);

        assertSame(CurrentSessionStartNewJournal.LoadStatus.PENDING,
                loaded.status());
        assertTrue(CurrentSessionStartNewJournal.matchesCurrent(
                loaded.pending().orElseThrow(), snapshot));
        assertEquals(boundary,
                loaded.pending().orElseThrow().boundaryMillis());
        String json = Files.readString(path, StandardCharsets.UTF_8);
        assertFalse(json.contains(snapshot.sessionId));
        assertFalse(json.contains("RECOVER_ME"));
        assertFalse(json.contains("itemRows"));
        assertFalse(json.contains("quantity"));
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length
                <= CurrentSessionStartNewJournal.MAX_FILE_BYTES);
    }

    @Test
    void malformedJournalFailsClosedWithoutDeletingEvidence()
            throws Exception {
        Path path = tempDir.resolve("malformed.json");
        Files.writeString(path, "{not-json", StandardCharsets.UTF_8);

        CurrentSessionStartNewJournal.LoadResult loaded =
                CurrentSessionStartNewJournal.load(path);

        assertSame(CurrentSessionStartNewJournal.LoadStatus.INVALID,
                loaded.status());
        assertTrue(Files.exists(path));
        assertFalse(loaded.warning().isBlank());

        RotClientCurrentSession current = new RotClientCurrentSession();
        assertFalse(CurrentSessionStartNewJournal.begin(
                path,
                current.snapshotConfig(),
                current.freeze(
                        current.snapshotConfig().startedAtMillis + 100L)));
        assertEquals("{not-json",
                Files.readString(path, StandardCharsets.UTF_8));
    }
}
