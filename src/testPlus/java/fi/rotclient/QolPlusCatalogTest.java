package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolPlusCatalogTest {
    @Test
    void plusCatalogIncludesMapArtOverride() {
        assertTrue(QolFlavorSupport.isPlus());
        assertEquals(135, QolUtilityCatalog.modules().size());
        assertNotNull(QolUtilityCatalog.findById("qol.eye_height_fix"));
        assertNotNull(QolUtilityCatalog.findById("qol.instant_sneak"));
        assertNotNull(QolUtilityCatalog.findById("qol.item_count_fix"));
        // Moved out of the standard edition because they cancel actions or click menus for the player.
        assertNotNull(QolUtilityCatalog.findById("qol.double_use_fix"));
        assertNotNull(QolUtilityCatalog.findById("qol.pet_keybinds"));
        assertNotNull(QolUtilityCatalog.findById("qol.loadout_keybinds"));
        assertSettingPresent("qol.stall_market", "qol.stall_market.bazaar_search");
        assertSettingPresent("qol.stall_market", "qol.stall_market.sell_protection");
        assertSettingPresent("qol.stall_market", "qol.stall_market.angry_coop");
        assertSettingPresent("qol.storage_overlay", "qol.storage_overlay.reload_pages");
        assertSettingPresent("qol.render_optimizer", "qol.render_optimizer.hide_fog");
        assertSettingPresent("qol.iota", "qol.iota.fix_fishing_hook");
        assertSettingPresent("qol.mining_helpers", "qol.mining_helpers.break_reset");
        assertSettingPresent("qol.dungeon_esp", "qol.dungeon_esp.hate_doors");
        assertSettingPresent("qol.dungeon_esp", "qol.dungeon_esp.depth");
        assertSettingPresent("qol.dungeon_terminals", "qol.dungeon_terminals.depth_test");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.hide_diorite");
        assertSettingPresent("qol.slayer_highlights", "qol.slayer_highlights.depth");
        assertEquals("F7", QolUtilityCatalog.findById("qol.dungeon_termsim").section());
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

    private static void assertSettingPresent(String moduleId, String settingId) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(moduleId);
        assertNotNull(module, moduleId);
        assertTrue(module.settings().stream().anyMatch(setting -> settingId.equals(setting.id())), settingId);
    }
}
