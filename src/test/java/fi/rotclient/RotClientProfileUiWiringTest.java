package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientProfileUiWiringTest {

    private static final Path SCREEN = Path.of(
            "src/client/java/fi/rotclient/"
                    + "MiningUiScreen.java");

    @Test
    void profileUiSupportsSwitchRenameDuplicateAndDelete()
            throws Exception {

        String source =
                Files.readString(
                        SCREEN,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "controller.switchTo("));

        assertTrue(source.contains(
                "controller.rename("));

        assertTrue(source.contains(
                "controller.duplicate("));

        assertTrue(source.contains(
                "controller.delete("));
    }

    @Test
    void profileUiHasContextActionsAndEditModes()
            throws Exception {

        String source =
                Files.readString(
                        SCREEN,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "ProfileEditMode"));

        assertTrue(source.contains(
                "profileMenuProfileId"));

        assertTrue(source.contains(
                "\"RENAME\""));

        assertTrue(source.contains(
                "\"DUPLICATE\""));

        assertTrue(source.contains(
                "\"DELETE\""));
    }

    @Test
    void deletingActiveProfileCanSelectReplacement()
            throws Exception {

        String source =
                Files.readString(
                        SCREEN,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "profileDeleteReplacementId"));

        assertTrue(source.contains(
                "cycleProfileDeleteReplacement()"));
    }
}