package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolWorkspaceViewTest {
    @Test
    void captureRemembersPageAndSettingsDrawer() {
        QolWorkspaceView view = QolWorkspaceView.capture(
                "DUNGEONS", false, false, true, false, "qol.terminal");
        assertEquals("DUNGEONS", view.groupId());
        assertEquals("qol.terminal", view.moduleId());
        assertEquals(QolWorkspaceView.DRAWER_MODULE, view.drawerKind());
        assertTrue(view.hasDrawerModule());
        assertFalse(view.hudDrawer());
        assertFalse(view.appearanceLanding());
        assertFalse(view.hudLayoutLanding());
    }

    @Test
    void captureRemembersHudDrawerSeparateFromSettings() {
        QolWorkspaceView view = QolWorkspaceView.capture(
                "DUNGEONS", false, false, true, true, "qol.terminal");
        assertTrue(view.hudDrawer());
        assertEquals(QolWorkspaceView.DRAWER_HUD, view.drawerKind());
        assertEquals("qol.terminal", view.moduleId());
    }

    @Test
    void captureKeepsHudLayoutLandingWithHudDrawer() {
        QolWorkspaceView view = QolWorkspaceView.capture(
                "HUD_DISPLAY", false, true, true, true, "qol.custom_scoreboard");
        assertTrue(view.hudLayoutLanding());
        assertTrue(view.hudDrawer());
        assertEquals("qol.custom_scoreboard", view.moduleId());
    }

    @Test
    void captureDualWritesAppearanceCatalogIdWhenDrawerClosed() {
        QolWorkspaceView view = QolWorkspaceView.capture(
                "HUD_DISPLAY", true, false, false, false, "");
        assertTrue(view.appearanceLanding());
        assertFalse(view.hasDrawerModule());
        assertEquals(MiningTrackerCatalogPolicy.APPEARANCE, view.moduleId());
    }

    @Test
    void legacyTabWithoutDrawerKindOpensSettingsDrawer() {
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab();
        tab.qolGroup = "COMBAT";
        tab.qolModuleId = "qol.auto_clicker";
        QolWorkspaceView view = QolWorkspaceView.fromTab(tab);
        assertEquals("COMBAT", view.groupId());
        assertTrue(view.hasDrawerModule());
        assertFalse(view.hudDrawer());
        assertEquals(QolWorkspaceView.DRAWER_MODULE, view.drawerKind());
    }

    @Test
    void legacyAppearanceModuleIdIsLandingNotDrawer() {
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab();
        tab.qolModuleId = MiningTrackerCatalogPolicy.APPEARANCE;
        QolWorkspaceView view = QolWorkspaceView.fromTab(tab);
        assertTrue(view.appearanceLanding());
        assertFalse(view.hasDrawerModule());
    }

    @Test
    void applyRoundTripsThroughTab() {
        QolWorkspaceView original = QolWorkspaceView.capture(
                "GUI", false, false, true, true, "qol.custom_scoreboard");
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab();
        original.applyTo(tab);
        assertEquals(original, QolWorkspaceView.fromTab(tab));
        assertTrue(original.sameAs(tab));
    }

    @Test
    void closedDrawerKeepsPageOnly() {
        QolWorkspaceView view = QolWorkspaceView.capture(
                "SLAYER", false, false, false, false, "");
        assertEquals("SLAYER", view.groupId());
        assertFalse(view.hasDrawerModule());
        assertEquals("", view.moduleId());
    }

    @Test
    void noneAndNullDoNotReplaceSavedRoute() {
        assertFalse(QolWorkspaceView.shouldNavigateTo(null));
        assertFalse(QolWorkspaceView.shouldNavigateTo(DashboardModule.NONE));
        assertTrue(QolWorkspaceView.shouldNavigateTo(DashboardModule.QOL_SETTINGS));
        assertTrue(QolWorkspaceView.shouldNavigateTo(DashboardModule.MINING_TRACKER));
    }
}
