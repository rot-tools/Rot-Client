package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RotClientWorkspacePersistenceTest {
    @Test
    void saveLoadRoundTripPreservesTabsOrderActiveAndWindow() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab history = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_HISTORY);
        history.normalize();
        history.scrollPixels = 42;
        config.tabs.add(history);
        config.activeTabId = history.id;
        config.clientUiNormX = 0.2F;
        config.clientUiNormY = 0.8F;
        config.clientUiNormW = 0.7F;
        config.clientUiNormH = 0.6F;
        config.windowPlacement = RotClientWindowPlacementPolicy.SNAP_LEFT;
        config.expandedSidebarSections = List.of(
                RotClientSidebarNav.SECTION_SESSIONS);
        config.normalize();

        String json = RotClientWorkspaceStore.toJson(config);
        RotClientWorkspaceConfig loaded = RotClientWorkspaceStore.parseJson(json);

        assertEquals(2, loaded.tabs.size());
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                loaded.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_HISTORY.id(),
                loaded.tabs.get(1).route);
        assertEquals(history.id, loaded.activeTabId);
        assertEquals(0.2F, loaded.clientUiNormX, 0.0001F);
        assertEquals(0.8F, loaded.clientUiNormY, 0.0001F);
        assertEquals(0.7F, loaded.clientUiNormW, 0.0001F);
        assertEquals(0.6F, loaded.clientUiNormH, 0.0001F);
        assertEquals(RotClientWindowPlacementPolicy.SNAP_LEFT, loaded.windowPlacement);
        assertEquals(42, loaded.tabs.get(1).scrollPixels);
        assertEquals(RotClientWorkspaceConfig.SCHEMA_VERSION, loaded.schemaVersion);
        assertEquals(
                List.of(RotClientSidebarNav.SECTION_QOL),
                loaded.expandedSidebarSections);
    }

    @Test
    void malformedWorkspaceFallsBackToDefaults() {
        RotClientWorkspaceConfig loaded =
                RotClientWorkspaceStore.parseJson("{not-json");
        assertEquals(1, loaded.tabs.size());
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                loaded.tabs.get(0).route);
        assertEquals(0.5F, loaded.clientUiNormX, 0.0001F);
    }

    @Test
    void missingWorkspaceMeansOneOverviewTab() {
        RotClientWorkspaceConfig loaded =
                RotClientWorkspaceStore.parseJson("");
        assertEquals(1, loaded.tabs.size());
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                loaded.tabs.get(0).route);
    }

    @Test
    void olderSchemaMigratesAndPreservesValues() {
        String json = """
                {
                  "schemaVersion": 0,
                  "activeTab": "tab-a",
                  "activeTabId": "tab-a",
                  "clientUiNormX": 0.1,
                  "clientUiNormY": 0.9,
                  "tabs": [
                    {
                      "id": "tab-a",
                      "route": "session_history",
                      "scrollPixels": 15
                    },
                    {
                      "id": "tab-b",
                      "route": "mining_tracker"
                    }
                  ]
                }
                """;
        RotClientWorkspaceConfig loaded = RotClientWorkspaceStore.parseJson(json);
        assertEquals(RotClientWorkspaceConfig.SCHEMA_VERSION, loaded.schemaVersion);
        assertEquals(2, loaded.tabs.size());
        assertEquals("tab-a", loaded.activeTabId);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_HISTORY.id(),
                loaded.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.MINING_TRACKER.id(),
                loaded.tabs.get(1).route);
        assertEquals(0.1F, loaded.clientUiNormX, 0.0001F);
        assertEquals(0.9F, loaded.clientUiNormY, 0.0001F);
        assertEquals(15, loaded.tabs.get(0).scrollPixels);
    }

    @Test
    void unknownFutureFieldsDoNotBreakLoading() {
        String json = """
                {
                  "schemaVersion": 1,
                  "activeTabId": "t1",
                  "clientUiNormX": 0.4,
                  "clientUiNormY": 0.6,
                  "futureExperimentalField": {"nested": true},
                  "tabs": [
                    {
                      "id": "t1",
                      "route": "overview",
                      "futureTabField": 123
                    }
                  ]
                }
                """;
        RotClientWorkspaceConfig loaded = RotClientWorkspaceStore.parseJson(json);
        assertEquals(1, loaded.tabs.size());
        assertEquals("t1", loaded.activeTabId);
        assertEquals(0.4F, loaded.clientUiNormX, 0.0001F);
    }

    @Test
    void applicationVersionChangeDoesNotWipeWorkspace() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.tabs.get(0).route = RotClientWorkspaceRoute.APPEARANCE_COLORS.id();
        config.clientUiNormX = 0.33F;
        String json = RotClientWorkspaceStore.toJson(config);
        // Simulate reload after a jar update: same schema, different app version
        // is never consulted by the workspace parser.
        RotClientWorkspaceConfig.migrateNote("2.5.0+mc26.2");
        RotClientWorkspaceConfig loaded = RotClientWorkspaceStore.parseJson(json);
        assertEquals(
                RotClientWorkspaceRoute.APPEARANCE_COLORS.id(),
                loaded.tabs.get(0).route);
        assertEquals(0.33F, loaded.clientUiNormX, 0.0001F);
    }

    @Test
    void unknownSavedRouteFallsBackSafelyPerTab() {
        String json = """
                {
                  "schemaVersion": 1,
                  "activeTabId": "t1",
                  "tabs": [
                    {
                      "id": "t1",
                      "route": "removed_module_v3"
                    },
                    {
                      "id": "t2",
                      "route": "session_analytics"
                    }
                  ]
                }
                """;
        RotClientWorkspaceConfig loaded = RotClientWorkspaceStore.parseJson(json);
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                loaded.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_ANALYTICS.id(),
                loaded.tabs.get(1).route);
        assertEquals(2, loaded.tabs.size());
    }

    @Test
    void atomicWriteRoundTrip(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("rotclient-workspace.json");
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.clientUiNormX = 0.7F;
        String json = RotClientWorkspaceStore.toJson(config);
        AtomicFileWriter.writeAtomically(path, json);
        assertTrue(Files.isRegularFile(path));
        String loaded = Files.readString(path, StandardCharsets.UTF_8);
        RotClientWorkspaceConfig parsed = RotClientWorkspaceStore.parseJson(loaded);
        assertEquals(0.7F, parsed.clientUiNormX, 0.0001F);
    }

    @Test
    void emptyTabsNormalizeToOverview() {
        RotClientWorkspaceConfig config = new RotClientWorkspaceConfig();
        config.tabs.clear();
        config.activeTabId = "";
        config.normalize();
        assertEquals(1, config.tabs.size());
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                config.tabs.get(0).route);
    }

    @Test
    void resetViewToDefaultReturnsActiveTabToOverviewAndClearsQol() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab tab = config.activeTab();
        tab.route = RotClientWorkspaceRoute.QOL_SETTINGS.id();
        tab.qolGroup = "DUNGEONS";
        tab.qolModuleId = "qol.terminal";
        tab.scrollPixels = 80;
        config.expandedSidebarSections = List.of();
        config.resetViewToDefault();
        assertEquals(RotClientWorkspaceRoute.OVERVIEW.id(), tab.route);
        assertEquals("", tab.qolGroup);
        assertEquals("", tab.qolModuleId);
        assertEquals(0, tab.scrollPixels);
        assertEquals(
                RotClientSidebarNav.defaultExpandedSections(),
                config.expandedSidebarSections);
    }
}
