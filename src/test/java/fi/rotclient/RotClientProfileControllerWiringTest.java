package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientProfileControllerWiringTest {

    private static final Path CONTROLLER = Path.of(
            "src/client/java/fi/rotclient/"
                    + "RotClientProfileController.java");

    private static final Path CLIENT = Path.of(
            "src/client/java/fi/rotclient/"
                    + "RotClientClient.java");

    @Test
    void controllerSupportsAllProfileOperations()
            throws Exception {

        String source =
                Files.readString(
                        CONTROLLER,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "RotClientProfile createFromCurrent("));

        assertTrue(source.contains(
                "RotClientProfile createDefault("));

        assertTrue(source.contains(
                "RotClientProfile duplicate("));

        assertTrue(source.contains(
                "boolean rename("));

        assertTrue(source.contains(
                "boolean switchTo("));

        assertTrue(source.contains(
                "boolean delete("));
    }

    @Test
    void controllerCapturesOutgoingProfileBeforeCreation()
            throws Exception {

        String source =
                Files.readString(
                        CONTROLLER,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "captureOutgoingProfile()"));

        assertTrue(source.contains(
                "profiles.captureCurrentSettings("));
    }

    @Test
    void controllerUsesLiveSwitchCoordinator()
            throws Exception {

        String source =
                Files.readString(
                        CONTROLLER,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "new RotClientProfileSwitchCoordinator("));

        assertTrue(source.contains(
                "switchCoordinator.switchTo(profileId)"));
    }

    @Test
    void rotClientExposesProfileController()
            throws Exception {

        String source =
                Files.readString(
                        CLIENT,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "RotClientProfileController SETTINGS_PROFILE_CONTROLLER"));

        assertTrue(source.contains(
                "static RotClientProfileController settingsProfileController()"));
    }
}