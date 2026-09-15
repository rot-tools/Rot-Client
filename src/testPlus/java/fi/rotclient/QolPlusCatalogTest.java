package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolPlusCatalogTest {
    @Test
    void plusCatalogIncludesMapArtOverride() {
        assertTrue(QolFlavorSupport.isPlus());
        assertEquals(134, QolUtilityCatalog.modules().size());
        assertNotNull(QolUtilityCatalog.findById("qol.auto_clicker"));
        assertNotNull(QolUtilityCatalog.findById("qol.auto_sprint"));
        assertNotNull(QolUtilityCatalog.findById("qol.experiment_solver"));
        assertNotNull(QolUtilityCatalog.findById("qol.diana_burrows"));
        assertNotNull(QolUtilityCatalog.findById("qol.diana_mobs"));
        assertNotNull(QolUtilityCatalog.findById("qol.diana_profit"));
        assertNotNull(QolUtilityCatalog.findById("qol.dungeon_term_click"));
        assertTrue(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.diana_burrows")));
        assertTrue(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.auto_sprint")));
        assertNotNull(QolUtilityCatalog.findById("qol.camera"));
        assertNotNull(QolUtilityCatalog.findById("qol.freecam"));
        assertNotNull(QolUtilityCatalog.findById("qol.farm_keys"));
        assertEquals("qol.map_art_override", QolUtilityCatalog.findById(
                "qol.map_art_override.image_path").id());
        assertTrue(QolUtilityCatalog.findById("qol.map_art_override").searchAliases()
                .contains("painting"));
        assertTrue(QolUtilityCatalog.findById("qol.command_keybinds").settings().stream()
                .anyMatch(setting -> "qol.command_keybinds.macros".equals(setting.id())));
        assertTrue(QolUtilityCatalog.findById("qol.dungeon_hud").settings().stream()
                .anyMatch(setting -> "qol.dungeon_hud.map_mode".equals(setting.id())));
        assertNotNull(QolUtilityCatalog.findById("qol.ghosts"));
        assertNotNull(QolUtilityCatalog.findById("qol.trajectories"));
        assertNotNull(QolUtilityCatalog.findById("qol.world_scanner"));
        assertEquals("Fox", QolUtilityCatalog.findById("qol.map_art_override").name());
    }
}
