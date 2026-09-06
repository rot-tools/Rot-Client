package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientProfileSyncWiringTest {

    private static final Path CLIENT = Path.of(
            "src/client/java/fi/rotclient/RotClientClient.java");

    @Test
    void normalSaveCapturesActiveProfileSettings()
            throws Exception {

        String source =
                Files.readString(
                        CLIENT,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "static void save()"));

        assertTrue(source.contains(
                "saveActiveSettingsProfile();"));

        assertTrue(source.contains(
                "SETTINGS_PROFILES.captureCurrentSettings("));

        assertTrue(source.contains(
                "TrackerStore.save(CONFIG);"));
    }

    @Test
    void trackerAutosaveUsesCentralSavePath()
            throws Exception {

        String source =
                Files.readString(
                        CLIENT,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "\"TRACKER_AUTOSAVE\""));

        assertTrue(source.contains(
                "RotClientClient::save"));
    }

    @Test
    void startupAppliesStoredActiveProfile()
            throws Exception {

        String source =
                Files.readString(
                        CLIENT,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "applyActiveSettingsProfileOnStartup();"));

        assertTrue(source.contains(
                "private static void applyActiveSettingsProfileOnStartup()"));

        assertTrue(source.contains(
                "SETTINGS_PROFILES.activeProfile()"));

        assertTrue(source.contains(
                "RotClientProfileSettingsAdapter.apply("));
    }

    @Test
    void shutdownUsesProfileAwareSavePath()
            throws Exception {

        String source =
                Files.readString(
                        CLIENT,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "ClientLifecycleEvents.CLIENT_STOPPING.register"));

        assertTrue(source.contains(
                "save();"));

        assertTrue(source.contains(
                "saveActiveSettingsProfile();"));
    }


}