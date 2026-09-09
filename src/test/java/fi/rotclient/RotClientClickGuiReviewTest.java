package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientClickGuiReviewTest {
    @Test
    void clickGuiOpenRestoresLastWorkspaceView() throws IOException {
        String client = read("src/client/java/fi/rotclient/RotClientClient.java");
        String screen = read("src/client/java/fi/rotclient/MiningUiScreen.java");
        String dashboard = read("src/client/java/fi/rotclient/QolUtilityDashboard.java");
        String openClick = section(client, "static void openClickGui()", "static void closeClickGuiIfOpen");
        String openMining = section(
                client,
                "private static int openMiningUi(",
                "static void openClientUiNavigating");
        String openNavigating = section(
                client,
                "static void openClientUiNavigating",
                "static void openHudEditor(Screen parent)");
        assertTrue(client.contains("void openClickGui()"));
        assertFalse(openClick.contains("resetToDefaultOpenState"));
        assertFalse(openMining.contains("resetToDefaultOpenState"));
        assertFalse(openNavigating.contains("resetToDefaultOpenState"));
        assertTrue(openNavigating.contains("QolWorkspaceView.shouldNavigateTo"));
        assertTrue(screen.contains("QolWorkspaceView.shouldNavigateTo(initialModule)"));
        assertTrue(screen.contains("qolDashboard.restoreFromWorkspace"));
        assertTrue(dashboard.contains("QolWorkspaceView.fromTab"));
        assertTrue(dashboard.contains("view.hudDrawer()"));
        assertTrue(dashboard.contains("openHudSettings(view.moduleId())"));
        assertTrue(screen.contains("frame.hudDrawer()"));
        assertTrue(screen.contains("openHudSettings(frame.focusId())"));
    }

    @Test
    void appearanceColorsScrollbarIsDraggable() throws IOException {
        String appearance = read(
                "src/client/java/fi/rotclient/RotClientAppearanceScreen.java");
        assertTrue(appearance.contains("scroll.beginThumbDrag("));
        assertTrue(appearance.contains("scroll.dragThumbTo("));
        assertTrue(appearance.contains("scroll.endThumbDrag()"));
    }

    @Test
    void cheatsFilterUsesBracketCount() throws IOException {
        String dashboard = read(
                "src/client/java/fi/rotclient/QolUtilityDashboard.java");
        assertTrue(dashboard.contains("cheatFilterLabel(cheatCount)"));
        assertFalse(dashboard.contains("\"Cheat \" + cheatCount"));
    }

    @Test
    void settingsPopupDimsAndConsumesOutsideClicks() throws IOException {
        String dashboard = read(
                "src/client/java/fi/rotclient/QolUtilityDashboard.java");
        assertTrue(dashboard.contains("RotClientUiDraw.drawScrim("));
        assertTrue(dashboard.contains("closeDrawer();"));
        assertFalse(dashboard.contains("private boolean handleHudMenuClick"));
        String hudClick = section(
                dashboard,
                "private boolean handleCardClick",
                "private boolean handleDrawerClick");
        assertTrue(hudClick.contains("openHudSettings(module.id())"));
        assertTrue(hudClick.contains("OPEN_HUD_SETTINGS"));
        assertFalse(hudClick.contains("OPEN_HUD_MENU"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0, () -> "Missing marker: " + start);
        assertTrue(to > from, () -> "Missing marker: " + end);
        return source.substring(from, to);
    }
}
