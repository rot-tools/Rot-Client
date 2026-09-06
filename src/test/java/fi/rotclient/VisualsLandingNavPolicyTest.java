package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class VisualsLandingNavPolicyTest {
    @Test
    void appearanceAndHudEditorAreVisualsOnlyCards() {
        assertTrue(VisualsLandingNavPolicy.hiddenFromGroupPage(
                MiningTrackerCatalogPolicy.APPEARANCE));
        assertTrue(VisualsLandingNavPolicy.hiddenFromGroupPage(
                MiningTrackerCatalogPolicy.HUD_LAYOUT));
        assertFalse(VisualsLandingNavPolicy.hiddenFromGroupPage(
                "qol.custom_cursor"));
        assertFalse(VisualsLandingNavPolicy.hiddenFromGroupPage(null));
        assertTrue(QolUtilityCatalog.hiddenFromGroupPage(
                QolUtilityCatalog.findById(MiningTrackerCatalogPolicy.APPEARANCE)));
        assertTrue(QolUtilityCatalog.hiddenFromGroupPage(
                QolUtilityCatalog.findById(MiningTrackerCatalogPolicy.HUD_LAYOUT)));
        assertFalse(QolUtilityCatalog.modulesOnGroupPage(QolUtilityCatalog.Group.HUD_DISPLAY)
                .stream()
                .anyMatch(module -> VisualsLandingNavPolicy.hiddenFromGroupPage(module.id())));
        assertEquals(131, QolUtilityCatalog.modules().size());
    }

    @Test
    void unhandledClickDismissesOpenVisualsLanding() {
        assertTrue(VisualsLandingNavPolicy.dismissOnUnhandledClick(
                true, RotClientSidebarNav.HitTarget.NONE));
        assertFalse(VisualsLandingNavPolicy.dismissOnUnhandledClick(
                true, RotClientSidebarNav.HitTarget.HUD_LAYOUT));
        assertFalse(VisualsLandingNavPolicy.dismissOnUnhandledClick(
                true, RotClientSidebarNav.HitTarget.OVERVIEW));
        assertFalse(VisualsLandingNavPolicy.dismissOnUnhandledClick(
                false, RotClientSidebarNav.HitTarget.NONE));
    }
}
