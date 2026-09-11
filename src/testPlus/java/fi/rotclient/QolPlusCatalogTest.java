package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolPlusCatalogTest {
    @Test
    void plusCatalogKeepsOneHundredThirtyTwoParents() {
        assertTrue(QolFlavorSupport.isPlus());
        assertEquals(132, QolUtilityCatalog.modules().size());
        assertNotNull(QolUtilityCatalog.findById("qol.auto_clicker"));
        assertNotNull(QolUtilityCatalog.findById("qol.camera"));
        assertNotNull(QolUtilityCatalog.findById("qol.freecam"));
        assertNotNull(QolUtilityCatalog.findById("qol.farm_keys"));
        assertTrue(QolUtilityCatalog.findById("qol.command_keybinds").settings().stream()
                .anyMatch(setting -> "qol.command_keybinds.macros".equals(setting.id())));
        assertTrue(QolUtilityCatalog.findById("qol.dungeon_hud").settings().stream()
                .anyMatch(setting -> "qol.dungeon_hud.map_mode".equals(setting.id())));
    }
}
