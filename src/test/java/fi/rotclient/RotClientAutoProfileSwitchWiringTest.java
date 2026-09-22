package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientAutoProfileSwitchWiringTest {

    private static final Path CLIENT = Path.of(
            "src/client/java/fi/rotclient/RotClientClient.java");
    private static final Path SWITCHER = Path.of(
            "src/client/java/fi/rotclient/RotClientAutoProfileSwitcher.java");
    private static final Path MANAGER = Path.of(
            "src/client/java/fi/rotclient/RotClientProfileManager.java");
    private static final Path SCREEN = Path.of(
            "src/client/java/fi/rotclient/MiningUiScreen.java");
    private static final Path CONFIG = Path.of(
            "src/main/java/fi/rotclient/RotClientProfileConfig.java");
    private static final Path SETTINGS = Path.of(
            "src/main/java/fi/rotclient/RotClientProfileSettings.java");

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void clientFeedsTheSwitcherFromTheSidebarAndResetsOnWorldChange()
            throws Exception {

        String source = read(CLIENT);

        assertTrue(source.contains("AUTO_PROFILE_SWITCHER.observeSidebar("));
        assertTrue(source.contains("AUTO_PROFILE_SWITCHER.onWorldChanged()"));
        assertTrue(source.contains(
                "SkyBlockDungeonDetector.confidentlyInDungeon()"));
    }

    @Test
    void switcherUsesTheOrdinaryProfileSwitchPath() throws Exception {
        String source = read(SWITCHER);

        assertTrue(source.contains("controller.switchTo("));
        assertTrue(source.contains("AutoProfileContext.classify("));
        assertTrue(source.contains("tracker.observe("));
        assertTrue(source.contains("rules.switchTargetFor("));
    }

    @Test
    void switcherDoesNothingWhileDisabledAndResetsSoReEnablingApplies()
            throws Exception {

        String source = read(SWITCHER);

        assertTrue(source.contains("if (!rules.enabled)"));
        assertTrue(source.contains("tracker.reset()"));
    }

    @Test
    void switcherOnlyNoticesSuccessWhenTheUserAskedForIt() throws Exception {
        String source = read(SWITCHER);

        assertTrue(source.contains("if (rules.notify)"));
        assertTrue(source.contains("Could not switch to profile"));
    }

    @Test
    void everyManagerMutationIsPersistedAndRolledBack() throws Exception {
        String source = read(MANAGER);

        assertTrue(source.contains("boolean setAutoSwitchEnabled("));
        assertTrue(source.contains("boolean setAutoSwitchNotify("));
        assertTrue(source.contains("boolean setAutoSwitchRule("));
        assertTrue(source.contains("boolean setAutoSwitchFallback("));
        assertTrue(source.contains("config.autoSwitch = previous;"));
    }

    @Test
    void rulesAreGlobalNotPartOfAnyProfile() throws Exception {
        assertTrue(read(CONFIG).contains(
                "RotClientAutoSwitchConfig autoSwitch"));
        assertFalse(read(SETTINGS).contains("autoSwitch"),
                "rules decide which profile is active, so a profile "
                        + "must not own or overwrite them");
    }

    @Test
    void profilesPageExposesTheAutoSwitchPage() throws Exception {
        String source = read(SCREEN);

        assertTrue(source.contains("drawAutoSwitchPage("));
        assertTrue(source.contains("handleProfileSubpageClick("));
        assertTrue(source.contains("handleAutoSwitchBodyClick("));
        assertTrue(source.contains("setAutoSwitchEnabled("));
        assertTrue(source.contains("setAutoSwitchNotify("));
        assertTrue(source.contains("setAutoSwitchRule("));
        assertTrue(source.contains("setAutoSwitchFallback("));
        assertTrue(source.contains("\"AUTO SWITCH\""));
    }
}
