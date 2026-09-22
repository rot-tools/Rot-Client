package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientExamplesWiringTest {

    private static final Path SCREEN = Path.of(
            "src/client/java/fi/rotclient/MiningUiScreen.java");
    private static final Path MANAGER = Path.of(
            "src/client/java/fi/rotclient/RotClientProfileManager.java");
    private static final Path CONTROLLER = Path.of(
            "src/client/java/fi/rotclient/RotClientProfileController.java");
    private static final Path CONFIG = Path.of(
            "src/main/java/fi/rotclient/RotClientProfileConfig.java");

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void profilesPageExposesTheExamplesPage() throws Exception {
        String source = read(SCREEN);

        assertTrue(source.contains("drawExamplesPage("));
        assertTrue(source.contains("handleExamplesBodyClick("));
        assertTrue(source.contains("controller.addExample("));
        assertTrue(source.contains("RotClientProfilePresets.bundled()"));
        assertTrue(source.contains("\"EXAMPLES\""));
        assertTrue(source.contains("\"ADD ALL\""));
    }

    @Test
    void examplesAreAddedWithoutSwitchingToThem() throws Exception {
        String manager = read(MANAGER);

        int start = manager.indexOf("RotClientProfile addInactive(");
        int end = manager.indexOf("RotClientAutoSwitchConfig autoSwitch()", start);
        String body = manager.substring(start, end);

        // Adding an example must never change which profile is active.
        assertFalse(body.contains("activeProfileId ="), body);
        assertTrue(body.contains("config.activeProfile() == null"),
                "must refuse when nothing is active, or autosave would "
                        + "overwrite the new profile with the live settings");
        assertTrue(body.contains("config.profiles.remove(profile)"),
                "must roll back when saving fails");
    }

    @Test
    void theLiveSetupIsSavedFirstWhenThereAreNoProfiles() throws Exception {
        String controller = read(CONTROLLER);

        int start = controller.indexOf("RotClientProfile addExample(");
        int end = controller.indexOf("boolean rename(", start);
        String body = controller.substring(start, end);

        assertTrue(body.contains("createFromCurrent(CURRENT_SETUP_NAME)"), body);
        assertTrue(body.contains("preset.problems().isEmpty()"),
                "an invalid preset must never be installed");
        assertTrue(body.contains("profiles.addInactive("), body);
        assertFalse(body.contains("switchTo("), body);
    }

    @Test
    void presetsAreNeverCreatedBehindTheUsersBack() throws Exception {
        // The profile store's contract: it starts empty and only grows when
        // the user asks. Examples are offered, not seeded.
        assertTrue(read(CONFIG).contains(
                "Rot Client does not create predefined"));

        String client = read(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"));
        assertFalse(client.contains("RotClientProfilePresets"),
                "the client must not install presets on its own");
    }

    @Test
    void examplesPageRowsNeverOverlapTheFooter() {
        for (int count = 0; count <= 12; count++) {
            int footer = RotClientExamplesLayout.footerY(100, count);

            for (int i = 0; i < count; i++) {
                int bottom = RotClientExamplesLayout.rowY(100, i)
                        + RotClientExamplesLayout.ROW_HEIGHT;

                assertTrue(footer >= bottom + RotClientExamplesLayout.FOOTER_GAP,
                        "footer overlaps row " + i + " of " + count);
            }
        }
    }

    @Test
    void examplesPageRowsAreEvenlySpacedWithAGap() {
        for (int i = 0; i < 8; i++) {
            int gap = RotClientExamplesLayout.rowY(0, i + 1)
                    - (RotClientExamplesLayout.rowY(0, i)
                    + RotClientExamplesLayout.ROW_HEIGHT);

            assertEquals(RotClientExamplesLayout.ROW_GAP, gap);
        }
        assertEquals(RotClientExamplesLayout.rowY(50, 0),
                RotClientExamplesLayout.listBottom(50, 0));
    }

    @Test
    void theFivePageRowsFitBelowTheHeaderInAShortWindow() {
        // Same vertical budget the Auto Switch page already lives in
        // (about 364px below the page top).
        int bottom = RotClientExamplesLayout.footerY(0, 5) + 12 + 12;

        assertTrue(bottom <= 364, "examples page is " + bottom + "px tall");
    }
}
