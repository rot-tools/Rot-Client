package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudDrawerPolicyTest {
    @Test
    void slayerDisplayHudDrawerOwnsLookAndOverlayRows() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.slayer_display");
        HudElementCatalog.HudPiece piece = HudElementCatalog.hudPieces(module).get(0);
        assertTrue(HudDrawerPolicy.hudVisibilityUsesModuleEnable(piece));
        assertEquals("slayer", HudDrawerPolicy.styleFocusId(piece));
        assertTrue(HudDrawerPolicy.hudCatalogSettings(module, piece).stream().anyMatch(
                setting -> "qol.slayer_display.kill_time".equals(setting.id())));
        assertTrue(HudDrawerPolicy.hudCatalogSettings(module, piece).stream().anyMatch(
                setting -> "qol.slayer_display.dynamic_size".equals(setting.id())));
        assertTrue(HudDrawerPolicy.hudCatalogSettings(module, piece).stream().anyMatch(
                setting -> setting.id().endsWith("open_hud_editor")));
        assertTrue(HudDrawerPolicy.moduleCatalogSettings(module).isEmpty());
        assertTrue(HudElementCatalog.hudContentSettings(module, piece).size()
                == HudDrawerPolicy.hudCatalogSettings(module, piece).size());
    }

    @Test
    void slayerStatsKeepsResetInModuleSettings() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.slayer_stats");
        HudElementCatalog.HudPiece piece = HudElementCatalog.hudPieces(module).get(0);
        assertTrue(HudDrawerPolicy.hudCatalogSettings(module, piece).stream().anyMatch(
                setting -> "qol.slayer_stats.bosses_killed".equals(setting.id())));
        assertTrue(HudDrawerPolicy.moduleCatalogSettings(module).stream().anyMatch(
                setting -> "qol.slayer_stats.reset_session".equals(setting.id())));
    }

    @Test
    void playerDisplayPieceKeepsSharedTextTogglesWithoutOtherHuds() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.player_display");
        HudElementCatalog.HudPiece health = HudElementCatalog.hudPieces(module).stream()
                .filter(piece -> "qol.player_display.health_hud".equals(piece.toggleId()))
                .findFirst()
                .orElseThrow();
        var rows = HudDrawerPolicy.hudCatalogSettings(module, health);
        assertTrue(rows.stream().anyMatch(
                setting -> "qol.player_display.health_hud".equals(setting.id())));
        assertTrue(rows.stream().anyMatch(
                setting -> "qol.player_display.show_icons".equals(setting.id())));
        assertFalse(rows.stream().anyMatch(
                setting -> "qol.player_display.mana_hud".equals(setting.id())));
        assertFalse(HudDrawerPolicy.hudVisibilityUsesModuleEnable(health));
    }

    @Test
    void styleRowsCoverTitleBackgroundColorsAndScale() {
        assertTrue(HudDrawerPolicy.isStyleSetting(HudDrawerPolicy.STYLE_TITLE));
        assertTrue(HudDrawerPolicy.styleSettings().stream().anyMatch(
                setting -> HudDrawerPolicy.STYLE_SCALE.equals(setting.id())));
        assertEquals(
                QolUtilityCatalog.SettingType.COLOR,
                HudDrawerPolicy.styleSettings().stream()
                        .filter(setting -> HudDrawerPolicy.STYLE_TEXT.equals(setting.id()))
                        .findFirst()
                        .orElseThrow()
                        .type());
    }

    @Test
    void miningTrackerHudDrawerOwnsLineTogglesAndKeepsOpenPageInSettings() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.mining_tracker");
        HudElementCatalog.HudPiece piece = HudElementCatalog.hudPieces(module).get(0);
        assertEquals("mining_tracker", piece.focusId());
        assertFalse(HudDrawerPolicy.usesQolHudStyle(piece));
        assertTrue(HudDrawerPolicy.hudCatalogSettings(module, piece).stream().anyMatch(
                setting -> "qol.mining_tracker.show_blocks".equals(setting.id())));
        assertTrue(HudDrawerPolicy.hudCatalogSettings(module, piece).stream().anyMatch(
                setting -> setting.id().endsWith("open_hud_editor")));
        assertTrue(HudDrawerPolicy.moduleCatalogSettings(module).stream().anyMatch(
                setting -> "qol.mining_tracker.open_page".equals(setting.id())));
        assertFalse(HudDrawerPolicy.moduleCatalogSettings(module).stream().anyMatch(
                setting -> "qol.mining_tracker.show_blocks".equals(setting.id())));
    }

    @Test
    void powderChestMergesHudToggleWithEditor() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.powder_chest");
        assertEquals(1, HudElementCatalog.hudPieces(module).size());
        HudElementCatalog.HudPiece piece = HudElementCatalog.hudPieces(module).get(0);
        assertEquals("qol.powder_chest.hud", piece.toggleId());
        assertEquals("powder_chest", piece.focusId());
        assertFalse(HudDrawerPolicy.hudVisibilityUsesModuleEnable(piece));
    }

    @Test
    void dungeonHudCombinedDrawerOwnsLineTogglesAndKeepsResetsInSettings() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.dungeon_hud");
        var hud = HudDrawerPolicy.allHudCatalogSettings(module);
        var settings = HudDrawerPolicy.moduleCatalogSettings(module);
        assertTrue(hud.stream().anyMatch(setting -> "qol.dungeon_hud.floor".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.dungeon_hud.secrets".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(setting -> "qol.dungeon_hud.map".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(setting -> "qol.dungeon_hud.map_mode".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(setting -> "qol.dungeon_hud.room_names".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(setting -> "qol.dungeon_hud.map_scale".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(setting -> "qol.dungeon_hud.section_map".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.dungeon_hud.open_hud_editor".equals(setting.id())));
        assertFalse(hud.stream().anyMatch(
                setting -> "qol.dungeon_hud.reset_split_pbs".equals(setting.id())));
        assertFalse(hud.stream().anyMatch(
                setting -> "qol.dungeon_hud.cheater_darken_factor".equals(setting.id())));
        assertTrue(settings.stream().anyMatch(
                setting -> "qol.dungeon_hud.reset_split_pbs".equals(setting.id())));
        assertTrue(settings.stream().anyMatch(
                setting -> "qol.dungeon_hud.cheater_darken_factor".equals(setting.id())));
        assertEquals("dungeon", HudDrawerPolicy.uniqueStyleFocus(module));
        assertFalse(HudDrawerPolicy.hudDrawerShowsModuleEnableRow(module));
    }

    @Test
    void performanceHudCombinedDrawerOwnsFpsTpsPing() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.performance_hud");
        var hud = HudDrawerPolicy.allHudCatalogSettings(module);
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.performance_hud.show_fps".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.performance_hud.show_tps".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.performance_hud.show_ping".equals(setting.id())));
        assertEquals("performance", HudDrawerPolicy.uniqueStyleFocus(module));
    }

    @Test
    void slayerDisplayCombinedHudMatchesSinglePieceRows() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.slayer_display");
        HudElementCatalog.HudPiece piece = HudElementCatalog.hudPieces(module).get(0);
        assertEquals(
                HudDrawerPolicy.hudCatalogSettings(module, piece),
                HudDrawerPolicy.allHudCatalogSettings(module));
        assertTrue(HudDrawerPolicy.hudDrawerShowsModuleEnableRow(module));
        assertEquals("slayer", HudDrawerPolicy.uniqueStyleFocus(module));
    }

    @Test
    void playerDisplayCombinedHudSkipsSharedStyleFocus() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.player_display");
        var hud = HudDrawerPolicy.allHudCatalogSettings(module);
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.player_display.health_hud".equals(setting.id())));
        assertTrue(hud.stream().anyMatch(
                setting -> "qol.player_display.mana_hud".equals(setting.id())));
        assertEquals("", HudDrawerPolicy.uniqueStyleFocus(module));
        assertFalse(HudDrawerPolicy.hudDrawerShowsModuleEnableRow(module));
    }
}
