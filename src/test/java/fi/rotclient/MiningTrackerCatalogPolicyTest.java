package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MiningTrackerCatalogPolicyTest {
    @Test
    void trackerLineTogglesBelongInHudDrawer() {
        assertTrue(MiningTrackerCatalogPolicy.isHudContentSetting(
                "qol.mining_tracker.show_blocks"));
        assertTrue(MiningTrackerCatalogPolicy.isHudContentSetting(
                "qol.powder_chest.hud"));
        assertFalse(MiningTrackerCatalogPolicy.isHudContentSetting(
                "qol.mining_tracker.open_page"));
        assertEquals(
                "qol.mining_tracker.show_area",
                MiningTrackerCatalogPolicy.catalogIdForLegacyHudToggle("showArea"));
        assertFalse(MiningTrackerCatalogPolicy.usesQolHudStyle("mining_tracker"));
        assertTrue(MiningTrackerCatalogPolicy.usesQolHudStyle("slayer"));
        assertTrue(MiningTrackerCatalogPolicy.isOpenPageAction(
                "qol.mining_history.open_page"));
    }

    @Test
    void catalogLockIsOneHundredThirty() {
        assertEquals(111, QolUtilityCatalog.modules().size());
        assertEquals(QolUtilityCatalog.Group.MINING,
                QolUtilityCatalog.findById("qol.mining_tracker").group());
        assertEquals(QolUtilityCatalog.Group.HUD_DISPLAY,
                QolUtilityCatalog.findById("qol.appearance").group());
        assertFalse(QolUtilityCatalog.findById("qol.mining_history").toggleable());
    }
}
