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
    void clickGuiOpenResetsToDefaultOverview() throws IOException {
        String client = read("src/client/java/fi/rotclient/RotClientClient.java");
        assertTrue(client.contains("WORKSPACE.resetToDefaultOpenState()"));
        assertTrue(client.contains("void openClickGui()"));
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
        String hudClick = section(
                dashboard,
                "private boolean handleHudMenuClick",
                "private boolean handleDrawerClick");
        assertTrue(hudClick.contains("if (!inside)"));
        assertTrue(hudClick.contains("closeHudMenu();"));
        int outside = hudClick.indexOf("if (!inside)");
        String outsideBlock = hudClick.substring(outside, outside + 80);
        assertTrue(outsideBlock.contains("return true;"));
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
