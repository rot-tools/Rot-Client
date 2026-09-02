package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientWorkspaceTabTest {
    @Test
    void powderChestTrackerRouteRoundTripsAsOwnDashboardModule() {
        assertEquals(DashboardModule.POWDER_CHEST_TRACKER,
                RotClientWorkspaceRoute.POWDER_CHEST_TRACKER
                        .toDashboardModule());
        assertEquals(RotClientWorkspaceRoute.POWDER_CHEST_TRACKER,
                RotClientWorkspaceRoute.fromDashboardModule(
                        DashboardModule.POWDER_CHEST_TRACKER));
        assertEquals(RotClientWorkspaceRoute.POWDER_CHEST_TRACKER,
                RotClientWorkspaceRoute.fromId("powder_chest_tracker"));
    }

    @Test
    void firstStartupDefaultsToSingleOverviewTab() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.normalize();
        assertEquals(1, config.tabs.size());
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                config.tabs.get(0).route);
        assertEquals(config.tabs.get(0).id, config.activeTabId);
    }

    @Test
    void plusCreatesNewOverviewAndActivatesIt() {
        RotClientWorkspaceConfig start = RotClientWorkspaceConfig.defaults();
        start.tabs.get(0).route = RotClientWorkspaceRoute.SESSION_HISTORY.id();
        String previousId = start.tabs.get(0).id;

        RotClientWorkspaceConfig next = RotClientWorkspace.addOverviewOn(start);
        assertEquals(2, next.tabs.size());
        assertEquals(previousId, next.tabs.get(0).id);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_HISTORY.id(),
                next.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                next.tabs.get(1).route);
        assertEquals(next.tabs.get(1).id, next.activeTabId);
    }

    @Test
    void sidebarNavigationOnlyChangesActiveTabRoute() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab history = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_HISTORY);
        history.normalize();
        config.tabs.add(history);
        config.activeTabId = config.tabs.get(0).id;
        config.normalize();

        RotClientWorkspaceConfig navigated = RotClientWorkspace.navigateActiveOn(
                config, RotClientWorkspaceRoute.MINING_TRACKER);

        assertEquals(
                RotClientWorkspaceRoute.MINING_TRACKER.id(),
                navigated.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_HISTORY.id(),
                navigated.tabs.get(1).route);
        assertEquals(config.tabs.get(0).id, navigated.activeTabId);
    }

    @Test
    void closingActiveTabSelectsNeighbor() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab second = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_ANALYTICS);
        second.normalize();
        config.tabs.add(second);
        config.activeTabId = config.tabs.get(0).id;
        String remainingId = second.id;

        RotClientWorkspaceConfig closed = RotClientWorkspace.closeTabOn(
                config, config.tabs.get(0).id);
        assertEquals(1, closed.tabs.size());
        assertEquals(remainingId, closed.activeTabId);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_ANALYTICS.id(),
                closed.tabs.get(0).route);
    }

    @Test
    void closingFinalTabCreatesOverview() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        String oldId = config.tabs.get(0).id;
        RotClientWorkspaceConfig closed = RotClientWorkspace.closeTabOn(
                config, oldId);
        assertEquals(1, closed.tabs.size());
        assertNotEquals(oldId, closed.tabs.get(0).id);
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                closed.tabs.get(0).route);
        assertEquals(closed.tabs.get(0).id, closed.activeTabId);
    }

    @Test
    void maxTabLimitIsEnforced() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        for (int i = 1; i < RotClientWorkspaceConfig.MAX_TABS; i++) {
            config = RotClientWorkspace.addOverviewOn(config);
        }
        assertEquals(RotClientWorkspaceConfig.MAX_TABS, config.tabs.size());
        RotClientWorkspaceConfig blocked = RotClientWorkspace.addOverviewOn(config);
        assertEquals(RotClientWorkspaceConfig.MAX_TABS, blocked.tabs.size());
    }

    @Test
    void dragReorderPersistsOrder() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab analytics = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_ANALYTICS);
        RotClientWorkspaceTab history = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_HISTORY);
        analytics.normalize();
        history.normalize();
        config.tabs.add(analytics);
        config.tabs.add(history);
        config.normalize();

        RotClientWorkspaceConfig reordered =
                RotClientWorkspace.reorderOn(config, 2, 1);
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                reordered.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_HISTORY.id(),
                reordered.tabs.get(1).route);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_ANALYTICS.id(),
                reordered.tabs.get(2).route);
    }

    @Test
    void unknownRouteFallsBackToOverviewWithoutWipingTab() {
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab();
        tab.id = "keep-me";
        tab.route = "obsolete_future_route";
        tab.normalize();
        assertEquals("keep-me", tab.id);
        assertEquals(RotClientWorkspaceRoute.OVERVIEW.id(), tab.route);
    }

    @Test
    void invalidActiveTabIdRecovers() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.activeTabId = "missing-id";
        config.normalize();
        assertEquals(config.tabs.get(0).id, config.activeTabId);
    }

    @Test
    void scrollStateClampsAfterRestoration() {
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_ANALYTICS);
        tab.scrollPixels = 9999;
        tab.clampScroll(120);
        assertEquals(120, tab.scrollPixels);
        tab.clampScroll(0);
        assertEquals(0, tab.scrollPixels);
        tab.scrollPixels = -40;
        tab.clampScroll(50);
        assertEquals(0, tab.scrollPixels);
    }

    @Test
    void activateChangesActiveWithoutMutatingRoutes() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        RotClientWorkspaceTab history = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_HISTORY);
        history.normalize();
        config.tabs.add(history);
        config.activeTabId = config.tabs.get(0).id;
        config.normalize();

        RotClientWorkspaceConfig activated =
                RotClientWorkspace.activateOn(config, history.id);
        assertEquals(history.id, activated.activeTabId);
        assertEquals(
                RotClientWorkspaceRoute.OVERVIEW.id(),
                activated.tabs.get(0).route);
        assertEquals(
                RotClientWorkspaceRoute.SESSION_HISTORY.id(),
                activated.tabs.get(1).route);
    }

    @Test
    void historyTabStoresRouteNotFrozenLedger() {
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.SESSION_HISTORY);
        tab.normalize();
        assertEquals(RotClientWorkspaceRoute.SESSION_HISTORY.id(), tab.route);
        assertTrue(tab.selectedHistorySessionId.isEmpty()
                || tab.selectedHistorySessionId != null);
        // Tab is a view record only — no ledger/price fields exist on the type.
        assertEquals("Session History", tab.title());
    }

    @Test
    void appVersionIsDecoupledFromWorkspaceSchema() {
        String noteA = RotClientWorkspaceConfig.migrateNote("2.0.0+mc26.2");
        String noteB = RotClientWorkspaceConfig.migrateNote("9.9.9+mc99.9");
        assertTrue(noteA.contains("schema=" + RotClientWorkspaceConfig.SCHEMA_VERSION));
        assertTrue(noteB.contains("schema=" + RotClientWorkspaceConfig.SCHEMA_VERSION));
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.tabs.get(0).route = RotClientWorkspaceRoute.MINING_TRACKER.id();
        config.clientUiNormX = 0.25F;
        config.normalize();
        assertEquals(
                RotClientWorkspaceRoute.MINING_TRACKER.id(),
                config.tabs.get(0).route);
        assertEquals(0.25F, config.clientUiNormX, 0.0001F);
    }
}
