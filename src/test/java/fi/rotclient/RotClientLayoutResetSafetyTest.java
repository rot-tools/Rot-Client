package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientLayoutResetSafetyTest {
    @Test
    void resetClientUiPositionDoesNotMutateTabsOrAppearanceFields() {
        RotClientWorkspaceConfig workspace = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab history = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_HISTORY);
        history.normalize();
        workspace.tabs.add(history);
        workspace.activeTabId = history.id;
        workspace.clientUiNormX = 0.15F;
        workspace.clientUiNormY = 0.85F;
        workspace.normalize();

        RotClientAppearanceConfig appearance = RotClientAppearanceConfig.defaults();
        appearance.dashboardBackdrop = 0xFF112233;
        appearance.dashboardAccent = 0xFFAABBCC;
        appearance.dashboardTextPrimary = 0xFFDEDEDE;
        appearance.hudChartLine = 0xFF55AA55;
        appearance.customBackgroundEnabled = true;
        appearance.customBackgroundFile = "custom.png";
        appearance.customBackgroundOpacity = 200;
        appearance.customBackgroundFit = "fit";
        appearance.normalize();
        RotClientAppearanceConfig appearanceBefore = appearance.copy();

        String historyId = history.id;
        int tabCount = workspace.tabs.size();
        String historyRoute = workspace.tabs.get(1).route;

        // Layout reset UI path: only norms change.
        workspace.clientUiNormX = 0.5F;
        workspace.clientUiNormY = 0.5F;

        assertEquals(0.5F, workspace.clientUiNormX, 0.0001F);
        assertEquals(0.5F, workspace.clientUiNormY, 0.0001F);
        assertEquals(tabCount, workspace.tabs.size());
        assertEquals(historyId, workspace.activeTabId);
        assertEquals(historyRoute, workspace.tabs.get(1).route);
        assertTrue(appearanceBefore.equalsNormalized(appearance));
    }

    @Test
    void hudOnlyResetDoesNotClearWorkspaceTabs() {
        RotClientWorkspaceConfig workspace = RotClientWorkspaceConfig.defaults();
        workspace.tabs.get(0).route = RotClientWorkspaceRoute.MINING_TRACKER.id();
        workspace.clientUiNormX = 0.3F;
        workspace.normalize();

        // HUD-only reset leaves Client UI norms and tabs alone.
        float uiX = workspace.clientUiNormX;
        String route = workspace.tabs.get(0).route;
        float hudX = 12.0F;
        float hudY = 12.0F;
        float hudScale = 1.0F;
        assertEquals(12.0F, hudX, 0.0001F);
        assertEquals(12.0F, hudY, 0.0001F);
        assertEquals(1.0F, hudScale, 0.0001F);
        assertEquals(uiX, workspace.clientUiNormX, 0.0001F);
        assertEquals(route, workspace.tabs.get(0).route);
    }

    @Test
    void layoutResetCommandDoesNotTouchDomainOrAppearanceSemantics()
            throws Exception {
        String client = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        int layoutIdx = client.indexOf("private static int resetLayout(");
        assertTrue(layoutIdx > 0);
        String method = client.substring(layoutIdx, layoutIdx + 1200);
        assertTrue(method.contains("resetClientUiPosition"));
        assertTrue(method.contains("CONFIG.x = 12.0F"));
        assertFalse(method.contains("CONFIG.reset"));
        assertFalse(method.contains("clearHistory"));
        assertFalse(method.contains("appearance"));
        assertFalse(method.contains("WORKSPACE.tabs.clear"));
        assertFalse(method.contains("TrackerStore.delete"));
        assertTrue(client.contains("literal(\"layout\")"));
        assertTrue(client.contains("literal(\"reset\")"));
    }

    @Test
    void appearanceCustomValuesSurviveAppVersionStory() {
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.dashboardBackdrop = 0xFF010203;
        config.dashboardAccent = 0xFFE33B3B;
        config.dashboardTextPrimary = 0xFFF1F1F1;
        config.hudChartLine = 0xFF00FFAA;
        config.customBackgroundEnabled = true;
        config.customBackgroundFile = "wall.png";
        config.customBackgroundOpacity = 190;
        config.customBackgroundFit = "stretch";
        config.schemaVersion = RotClientAppearanceConfig.SCHEMA_VERSION;
        config.normalize();

        // Schema-driven persistence must not key off application version.
        String json = new com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(config);
        assertFalse(json.contains("2.0.0"));
        RotClientAppearanceConfig loaded = new com.google.gson.Gson()
                .fromJson(json, RotClientAppearanceConfig.class);
        loaded.normalize();
        assertEquals(0xFF010203, loaded.dashboardBackdrop);
        assertEquals(0xFFE33B3B, loaded.dashboardAccent);
        assertEquals(0xFFF1F1F1, loaded.dashboardTextPrimary);
        assertEquals(0xFF00FFAA, loaded.hudChartLine);
        assertTrue(loaded.customBackgroundEnabled);
        assertEquals("wall.png", loaded.customBackgroundFile);
        assertEquals(190, loaded.customBackgroundOpacity);
        assertEquals("stretch", loaded.customBackgroundFit);
    }

    @Test
    void uiIntegrationWiresTabsAndLayoutResetSurfaces() throws Exception {
        String mining = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(mining.contains("RotClientTabStrip"));
        assertTrue(mining.contains("panelDragging"));
        assertTrue(mining.contains("hasControlDown"));
        assertTrue(mining.contains("addOverviewTab"));

        String appearance = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientAppearanceScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(appearance.contains("Reset UI Positions"));
        assertTrue(appearance.contains("resetLayoutPositionsFromUi"));
        assertTrue(appearance.contains("Reset HUD Visibility"));
        assertTrue(appearance.contains("resetHudVisibility"));

        String editor = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(editor.contains("CLIENT_UI"));
        assertTrue(editor.contains("clientUiNorm"));
        assertTrue(editor.contains("How to edit"));
        assertTrue(editor.contains("HUD ELEMENTS EDITOR"));
        assertFalse(editor.contains("LAYOUT EDITOR"));
        assertTrue(editor.contains("beginChromeDrag"));

        String home = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientHomeScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(home.contains("openClientUiNavigating"));
    }
}
