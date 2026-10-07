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
        assertTrue(inventory.contains("their implementation was not copied"));
        assertTrue(inventory.contains("META-INF/licenses/"));
        assertFalse(inventory.toLowerCase().contains("nofrills"));
    }

    @Test
    void gradleJarCopiesNoticeFiles() throws Exception {
        String gradle = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        assertTrue(gradle.contains("from(\"NOTICE\")"));
        assertTrue(gradle.contains("from(\"THIRD_PARTY.md\")"));
    }

    @Test
    void playableLiteRetainsFullUpstreamBinaryNotices() throws Exception {
        java.util.Properties properties = new java.util.Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"))) {
            properties.load(reader);
        }
        try (var jar = new java.util.zip.ZipFile("build/libs/RotClient-"
                + properties.getProperty("mod_version") + ".jar")) {
            for (String project : java.util.List.of("Athen", "Nebulune", "Odin", "OdinClient")) {
                String file = project + "-LICENSE.txt";
                var entry = jar.getEntry("META-INF/licenses/" + file);
                org.junit.jupiter.api.Assertions.assertNotNull(entry, file);
                try (var stream = jar.getInputStream(entry)) {
                    org.junit.jupiter.api.Assertions.assertArrayEquals(
                            Files.readAllBytes(Path.of("docs/third-party", file)), stream.readAllBytes(), file);
                }
            }
        }
    }
}
