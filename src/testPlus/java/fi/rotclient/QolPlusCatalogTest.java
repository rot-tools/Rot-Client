package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolPlusCatalogTest {
    @Test
    void dungeonExampleReturnsAfterSwitchingFromLiteToPlus() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        try {
            QolFlavorSupport.install(QolFlavorExtension.NONE);
            assertFalse(RotClientProfilePresets.indexedIds().contains("dungeons"));
            assertEquals(83, QolUtilityCatalog.modules().size());
            QolFlavorSupport.install(new RotClientPlusExtension());
            assertTrue(RotClientProfilePresets.indexedIds().contains("dungeons"));
            assertNotNull(RotClientProfilePresets.findById("dungeons"));
            assertEquals(135, QolUtilityCatalog.modules().size());
        } finally {
            QolFlavorSupport.install(previous);
        }
    }

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
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.simon");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.arrow_align");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.i4");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.sharp_shooter");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.wither_esp");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.dragon_boxes");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.gate");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.relics");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.relic_highlight");
        assertSettingPresent("qol.dungeon_f7", "qol.dungeon_f7.melody_display");
        assertSettingPresent("qol.dungeon_hud", "qol.dungeon_hud.melody");
        assertSettingPresent("qol.dungeon_hud", "qol.dungeon_hud.quiz");
        assertSettingPresent("qol.slayer_highlights", "qol.slayer_highlights.depth");
        assertEquals("F7", QolUtilityCatalog.findById("qol.dungeon_termsim").section());
        assertNotNull(QolUtilityCatalog.findById("qol.auto_clicker"));
        assertNotNull(QolUtilityCatalog.findById("qol.auto_sprint"));
        assertNotNull(QolUtilityCatalog.findById("qol.experiment_solver"));
        assertNotNull(QolUtilityCatalog.findById("qol.diana_burrows"));
        assertNotNull(QolUtilityCatalog.findById("qol.diana_mobs"));
        assertNotNull(QolUtilityCatalog.findById("qol.diana_profit"));
        assertNotNull(QolUtilityCatalog.findById("qol.dungeon_term_click"));
        assertNotNull(QolUtilityCatalog.findById("qol.dungeon_esp"));
        assertNotNull(QolUtilityCatalog.findById("qol.dungeon_terminals"));
        assertNotNull(QolUtilityCatalog.findById("qol.dungeon_puzzles"));
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

    @Test
    void plusRetainsTheMovedDungeonParents() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.dungeon_esp", true);
        config.setModuleEnabled("qol.dungeon_terminals", true);
        config.setModuleEnabled("qol.dungeon_puzzles", true);

        assertTrue(config.isModuleEnabled("qol.dungeon_esp"));
        assertTrue(config.isModuleEnabled("qol.dungeon_terminals"));
        assertTrue(config.isModuleEnabled("qol.dungeon_puzzles"));
    }

    @Test
    void plusRetainsRevealHiddenMapModeForExistingProfiles() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(QolFlavorSupport.isPlus());
        assertTrue(config.writeEnum("qol.dungeon_hud.map_mode", "Cheater"));
        assertEquals(DungeonMapPolicy.MAP_MODE_REVEAL, config.readEnum("qol.dungeon_hud.map_mode"));
        assertTrue(config.extras().dungeonMapRevealHidden());
    }

    private static void assertSettingPresent(String moduleId, String settingId) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(moduleId);
        assertNotNull(module, moduleId);
        assertTrue(module.settings().stream().anyMatch(setting -> settingId.equals(setting.id())), settingId);
    }
}
