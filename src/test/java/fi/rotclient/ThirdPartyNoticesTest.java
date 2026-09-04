package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ThirdPartyNoticesTest {
    @Test
    void rootNoticeAndInventoryExist() throws Exception {
        String notice = Files.readString(Path.of("NOTICE"), StandardCharsets.UTF_8);
        String inventory = Files.readString(Path.of("THIRD_PARTY.md"), StandardCharsets.UTF_8);
        assertTrue(notice.contains("MIT License"));
        assertTrue(notice.contains("THIRD_PARTY.md"));
        assertTrue(inventory.contains("SIL Open Font License 1.1"));
        assertTrue(inventory.contains("does **not** vendor other SkyBlock client source trees"));
        assertTrue(inventory.contains("legacy-item-models.json"));
        assertFalse(inventory.toLowerCase().contains("skyhanni"));
        assertFalse(inventory.toLowerCase().contains("nofrills"));
    }

    @Test
    void gradleJarCopiesNoticeFiles() throws Exception {
        String gradle = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        assertTrue(gradle.contains("from(\"NOTICE\")"));
        assertTrue(gradle.contains("from(\"THIRD_PARTY.md\")"));
    }
}
