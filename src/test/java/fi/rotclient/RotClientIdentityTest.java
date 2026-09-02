package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class RotClientIdentityTest {
    @Test
    void fabricModJsonUsesRotClientIdentity() throws IOException {
        String raw = Files.readString(
                Path.of("src/main/resources/fabric.mod.json"),
                StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        assertEquals("rotclient", root.get("id").getAsString());
        assertEquals("Rot Client", root.get("name").getAsString());
        assertEquals(
                "fi.rotclient.RotClientClient",
                root.getAsJsonObject("entrypoints")
                        .getAsJsonArray("client")
                        .get(0)
                        .getAsString());
        assertEquals(
                "assets/rotclient/icon.png",
                root.get("icon").getAsString());
        assertEquals(
                "rotclient.client.mixins.json",
                root.getAsJsonArray("mixins").get(0).getAsString());
        assertTrue(root.has("breaks"));
        assertTrue(root.getAsJsonObject("breaks").has("miningtracker"));
        assertFalse(raw.contains("Rot Client contributors"));
        assertTrue(raw.contains("\"name\": \"OgRudolf\"")
                || raw.contains("\"name\":\"OgRudolf\""));
        assertEquals(
                "https://github.com/rot-tools/Rot-Client",
                root.getAsJsonObject("contact").get("homepage").getAsString());
        assertEquals(
                "https://github.com/rot-tools/Rot-Client",
                root.getAsJsonObject("contact").get("sources").getAsString());
        assertEquals(
                "https://github.com/rot-tools/Rot-Client/issues",
                root.getAsJsonObject("contact").get("issues").getAsString());
        assertFalse(raw.toLowerCase(Locale.ROOT).contains("fi.miningtracker"));
        assertFalse(raw.contains("assets/miningtracker"));
    }

    @Test
    void buildPropertiesUseRotClientArtifactIdentity() throws IOException {
        String properties = Files.readString(
                Path.of("gradle.properties"),
                StandardCharsets.UTF_8);
        assertTrue(properties.contains("mod_version=2.0.1+mc26.2"));
        assertTrue(properties.contains("maven_group=fi.rotclient"));

        String settings = Files.readString(
                Path.of("settings.gradle"),
                StandardCharsets.UTF_8);
        assertTrue(settings.contains("rootProject.name = 'RotClient'"));

        String build = Files.readString(
                Path.of("build.gradle"),
                StandardCharsets.UTF_8);
        assertTrue(build.contains("\"rotclient\""));
        assertFalse(build.contains("\"miningtracker\""));
    }

    @Test
    void versionLabelUsesRotClientBrandAndModId() {
        assertEquals("rotclient", RotClientVersionLabel.MOD_ID);
        assertEquals(
                "Rot Client v2.0.0+mc26.2",
                RotClientVersionLabel.brandLabel("2.0.0+mc26.2"));
    }

    @Test
    void productionSourcesDoNotDeclareLegacyPackage() throws IOException {
        Path root = Path.of("src");
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("test"))
                    .forEach(path -> {
                        String text;
                        try {
                            text = Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException exception) {
                            throw new AssertionError(
                                    "Unable to read " + path, exception);
                        }
                        assertFalse(
                                text.contains("package fi.miningtracker"),
                                () -> "Legacy package in " + path);
                        assertFalse(
                                text.contains("import fi.miningtracker"),
                                () -> "Legacy import in " + path);
                    });
        }
    }

    @Test
    void legacyPersistenceConstantsRemainForMigration() {
        assertEquals("miningtracker.json", RotClientLegacyDataMigrator.LEGACY_CONFIG);
        assertEquals("rotclient.json", RotClientLegacyDataMigrator.NEW_CONFIG);
        assertEquals(
                "miningtracker-session-history.json",
                RotClientLegacyDataMigrator.LEGACY_HISTORY);
        assertEquals(
                "rotclient-session-history.json",
                RotClientLegacyDataMigrator.NEW_HISTORY);
        assertEquals(
                "rotclient-session-start-new-transaction.json",
                CurrentSessionStartNewJournal.FILE_NAME);
    }
}
