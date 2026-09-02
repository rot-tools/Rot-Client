package fi.rotclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SkyBlockPublicDataCacheTest {
    @TempDir
    Path tempDir;

    @Test
    void offlineModeLoadsACompleteValidPublicCache() throws Exception {
        writeValidCache(tempDir);

        SkyBlockPublicDataCache.Result result =
                new SkyBlockPublicDataCache().prepare(tempDir, false);

        assertFalse(result.usedNetwork());
        assertEquals(4, result.files().size());
        assertEquals(0, result.warnings().size());
        for (SkyBlockPublicDataCache.Dataset dataset
                : SkyBlockPublicDataCache.Dataset.values()) {
            assertEquals(
                    dataset.fileName(),
                    result.file(dataset).getFileName().toString());
        }
    }

    @Test
    void offlineModeRejectsIncompleteOrMalformedCache() throws Exception {
        writeValidCache(tempDir);
        Files.writeString(
                tempDir.resolve("hypixel-skills.json"),
                "{\"success\":false}",
                StandardCharsets.UTF_8);

        assertThrows(
                IOException.class,
                () -> new SkyBlockPublicDataCache().prepare(tempDir, false));
    }

    private static void writeValidCache(Path root) throws IOException {
        Files.writeString(
                root.resolve("hypixel-items.json"),
                "{\"success\":true,\"items\":[]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                root.resolve("hypixel-collections.json"),
                "{\"success\":true,\"collections\":{}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                root.resolve("hypixel-skills.json"),
                "{\"success\":true,\"skills\":{}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                root.resolve("hypixel-bazaar.json"),
                "{\"success\":true,\"products\":{}}",
                StandardCharsets.UTF_8);
    }
}
