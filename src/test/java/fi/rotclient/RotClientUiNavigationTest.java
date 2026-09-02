package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientUiNavigationTest {
    @Test
    void hitTestFindsOverviewAndQolPages() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                58, RotClientSidebarNav.defaultExpandedSections());
        assertEquals(
                RotClientSidebarNav.HitTarget.OVERVIEW,
                RotClientSidebarNav.hitTest(layout, 20, layout.overviewY() + 2, 10, 140, 12, 140));
        int utilitiesY = layout.qolUtilitiesY();
        assertTrue(utilitiesY > layout.overviewY());
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_UTILITIES,
                RotClientSidebarNav.hitTest(layout, 20, utilitiesY + 2, 10, 140, 12, 140));
    }

    @Test
    void routesRoundTripDashboardModules() {
        for (DashboardModule module : DashboardModule.values()) {
            RotClientWorkspaceRoute route =
                    RotClientWorkspaceRoute.fromDashboardModule(module);
            assertEquals(module, route.toDashboardModule());
        }
    }
}
