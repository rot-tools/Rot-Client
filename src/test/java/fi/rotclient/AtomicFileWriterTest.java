package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AtomicFileWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesSiblingTempThenReplacesDestination() throws Exception {
        Path target = tempDir.resolve("rotclient-session-history.json");
        Files.writeString(target, "previous-valid", StandardCharsets.UTF_8);

        AtomicFileWriter.writeAtomically(target, "{\n  \"schemaVersion\": 1\n}");

        assertEquals(
                "{\n  \"schemaVersion\": 1\n}",
                Files.readString(target, StandardCharsets.UTF_8));
        try (var stream = Files.list(tempDir)) {
            assertEquals(1L, stream.filter(Files::isRegularFile).count());
        }
        assertTrue(Files.exists(target));
    }

    @Test
    void failedReplacementPreservesPreviousValidDestination() throws Exception {
        Path target = tempDir.resolve("history.json");
        Files.writeString(target, "keep-me", StandardCharsets.UTF_8);

        Path blockedParent = tempDir.resolve("blocked");
        Files.writeString(blockedParent, "not-a-directory", StandardCharsets.UTF_8);
        Path blockedTarget = blockedParent.resolve("history.json");

        assertThrows(
                Exception.class,
                () -> AtomicFileWriter.writeAtomically(
                        blockedTarget,
                        "should-not-replace-other-file"));

        assertEquals(
                "keep-me",
                Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsNullArguments() {
        Path target = tempDir.resolve("history.json");
        assertThrows(
                IllegalArgumentException.class,
                () -> AtomicFileWriter.writeAtomically(null, "{}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AtomicFileWriter.writeAtomically(target, null));
    }
}
