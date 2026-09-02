package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudEditorAndCursorPolicyTest {
    @Test
    void moduleHudEditorActionsFocusTheMatchingOverlay() {
        assertEquals(
                "diana",
                HudElementCatalog.focusIdForHudEditorSetting(
                        "qol.diana_burrows.open_hud_editor"));
        assertEquals(
                "fishing",
                HudElementCatalog.focusIdForHudEditorSetting(
                        "qol.fishing_helper.open_hud_editor"));
        assertEquals(
                "mining",
                HudElementCatalog.focusIdForHudEditorSetting(
                        "qol.mining_helpers.open_hud_editor"));
        assertEquals(
                "",
                HudElementCatalog.focusIdForHudEditorSetting(
                        "qol.click_gui.open_hud_editor"));
        assertEquals(
                "",
                HudElementCatalog.focusIdForHudEditorSetting(
                        "qol.hud_layout.open_hud_editor"));
        assertTrue(HudElementCatalog.moduleHasHudEditor(
                QolUtilityCatalog.findById("qol.dungeon_hud")));
        assertEquals(
                "dungeon",
                HudElementCatalog.focusIdForHudEditorSetting(
                        "qol.dungeon_hud.open_hud_editor"));
        assertTrue(HudElementCatalog.moduleHasHudToggle(
                QolUtilityCatalog.findById("qol.fishing_creatures")));
        assertTrue(HudElementCatalog.explainedDescription(
                QolUtilityCatalog.findById("qol.performance_hud"))
                .contains("Use Module"));
        assertTrue(HudElementCatalog.hudControlOpensMenu(
                QolUtilityCatalog.findById("qol.performance_hud")));
        assertTrue(HudElementCatalog.hudPieces(
                QolUtilityCatalog.findById("qol.performance_hud")).size() >= 3);
        assertTrue(HudElementCatalog.moduleShowsHudControl(
                QolUtilityCatalog.findById("qol.slayer_display")));
        assertEquals(
                "FPS text",
                HudElementCatalog.inspectorToggles("performance").get(0).label());
        assertEquals(
                "Health HUD",
                HudElementCatalog.inspectorToggles("health").get(0).label());
        assertTrue(HudElementCatalog.inspectorToggles("health").stream().anyMatch(
                toggle -> "Icons".equals(toggle.label())));
        assertTrue(HudElementCatalog.inspectorToggles("health").stream().anyMatch(
                toggle -> "Name text".equals(toggle.label())));
        assertTrue(HudElementCatalog.inspectorToggles("health").stream().anyMatch(
                toggle -> "Current / max".equals(toggle.label())));
        assertEquals(
                "Treasure meters",
                HudElementCatalog.inspectorToggles("mining").get(0).label());
        assertTrue(HudElementCatalog.explainedSetting(
                QolUtilityCatalog.findById("qol.command_keybinds").settings().get(0))
                .contains("pets-menu"));
    }

    @Test
    void unfocusedHudUsesLowerAlphaWhenDimmingIsOn() {
        int focused = HudStylePolicy.dim(0xFFFFFFFF, true, true);
        int faded = HudStylePolicy.dim(0xFFFFFFFF, false, true);
        assertEquals(0xFFFFFFFF, focused);
        assertEquals(HudStylePolicy.UNFOCUSED_ALPHA, (faded >>> 24) & 0xFF);
        assertTrue(HudStylePolicy.isFocused("diana", ""));
        assertFalse(HudStylePolicy.isFocused("fishing", "diana"));
        assertEquals(2.5F, HudStylePolicy.clampScale(9.0F), 0.001F);
    }

    @Test
    void perElementStyleOverridesGlobalDefaults() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.hud_layout", true);
        config.writeBoolean("qol.hud_layout.show_background", false);
        HudStyleState override = new HudStyleState();
        override.showBackground = true;
        override.backgroundColor = 0x8800FF00;
        override.textColor = 0xFFFF0000;
        override.scale = 1.5F;
        config.extras().putHudStyle("diana", override);
        HudStyleState resolved = config.extras().resolvedHudStyle("diana");
        assertTrue(resolved.showBackground);
        assertEquals(0xFFFF0000, resolved.textColor);
        assertEquals(1.5F, resolved.scale, 0.001F);
        assertTrue(config.resetModuleToDefaults("qol.hud_layout"));
        assertTrue(config.extras().hudStyles.isEmpty());
    }

    @Test
    void customCursorClampsSizeAndMapsResizeEdges() throws Exception {
        assertEquals(0.6D, CustomCursorPolicy.clampSize(0.01D), 0.001D);
        assertEquals(
                CustomCursorPolicy.ResizeKind.E,
                CustomCursorPolicy.fromWindowEdge(
                        RotClientWindowPlacementPolicy.ResizeEdge.E));
        assertTrue(CustomCursorPolicy.clickPulse(true, true, 20L) > 1.0F);
        assertEquals(0, CustomCursorPolicy.holdRingAlpha(false, true, 500L));
        assertTrue(CustomCursorPolicy.holdRingAlpha(true, true, 200L) > 0);
        assertFalse(CustomCursorPolicy.shouldHideVanillaCursor(false, true, true));
        assertFalse(CustomCursorPolicy.shouldHideVanillaCursor(true, true, false));
        assertTrue(CustomCursorPolicy.shouldHideVanillaCursor(true, true, true));
        assertFalse(CustomCursorPolicy.shouldDrawOverlay(true, false, true));
        assertEquals(0xFFF2F2F2, CustomCursorPolicy.effectiveFill(0xFFFF4D6D));
        assertEquals(0xFF111111, CustomCursorPolicy.effectiveOutline(0xFF08080C));
        String cursorRuntime = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/CustomCursorRuntime.java"));
        assertTrue(cursorRuntime.contains("GLFW_CURSOR_HIDDEN"));
        assertTrue(cursorRuntime.contains("GLFW_CURSOR_NORMAL"));
        assertTrue(cursorRuntime.contains("else if (menuOpen)"));
        assertFalse(cursorRuntime.contains("smoothX"));
        assertTrue(cursorRuntime.contains("keepCursorFromSnapping"));
        String containerCursor = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenCustomCursorMixin.java"));
        String screenCursor = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/mixin/ScreenCustomCursorMixin.java"));
        assertFalse(containerCursor.contains("@Shadow"));
        assertFalse(screenCursor.contains("@Shadow"));
        assertFalse(containerCursor.contains("client.font"));
        assertTrue(screenCursor.contains("client.font"));
        assertFalse(containerCursor.contains("CustomCursorRuntime.render"));
        assertTrue(screenCursor.contains(
                "extractRenderStateWithTooltipAndSubtitles"));
    }

    @Test
    void legacyTexturesMapsKnownSkyblockIdsWithoutRemoteData() throws Exception {
        assertEquals(
                "minecraft:iron_sword",
                LegacyTexturesPolicy.vanillaModel("HYPERION"));
        assertEquals("", LegacyTexturesPolicy.vanillaModel("UNKNOWN_ITEM"));
        assertEquals("", LegacyTexturesPolicy.vanillaModel("TARANTULA_CATALYST"));
        assertEquals(
                "minecraft:stone_sword",
                LegacyTexturesPolicy.vanillaModel("DARK_CLAYMORE"));
        assertEquals(
                "minecraft:prismarine_shard",
                LegacyTexturesPolicy.vanillaModel("DIVAN_DRILL"));
        assertEquals(
                "minecraft:nether_star",
                LegacyTexturesPolicy.vanillaModel("SKYBLOCK_MENU"));
        assertTrue(LegacyTexturesPolicy.mappedCount() > 3000);
        assertTrue(LegacyTexturesPolicy.shouldReplace(true, true, "TERMINATOR"));
        assertTrue(LegacyTexturesPolicy.shouldReplace(
                true, true, "HYPERION", "hypixel_skyblock"));
        assertFalse(LegacyTexturesPolicy.shouldReplace(
                true, true, "HYPERION", "minecraft"));
        assertFalse(LegacyTexturesPolicy.shouldReplace(false, true, "TERMINATOR"));
        String mixin = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/mixin/ItemStackLegacyTexturesMixin.java"));
        assertTrue(mixin.contains("@Mixin(PatchedDataComponentMap.class)"));
        assertTrue(!mixin.contains("@Mixin(DataComponentHolder.class)"));
    }

    @Test
    void newAppearanceModulesRoundTripOnConfig() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.custom_cursor", true);
        config.setModuleEnabled("qol.legacy_textures", true);
        config.setModuleEnabled("qol.custom_resource_pack", true);
        assertTrue(config.isModuleEnabled("qol.custom_cursor"));
        assertTrue(config.isModuleEnabled("qol.custom_resource_pack"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.custom_resource_pack.overworld"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.custom_resource_pack.crimson"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.custom_resource_pack.end"));
        config.writeBoolean("qol.custom_resource_pack.end", false);
        assertEquals(Boolean.FALSE, config.readBoolean("qol.custom_resource_pack.end"));
        assertTrue(config.writeNumber("qol.custom_cursor.size", 1.6D));
        assertEquals(1.6D, config.readNumber("qol.custom_cursor.size"));
        assertTrue(config.resetModuleToDefaults("qol.custom_cursor"));
        assertFalse(config.isModuleEnabled("qol.custom_cursor"));
        assertTrue(config.resetModuleToDefaults("qol.legacy_textures"));
        assertTrue(config.resetModuleToDefaults("qol.custom_resource_pack"));
        assertFalse(config.isModuleEnabled("qol.custom_resource_pack"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.custom_resource_pack.end"));
    }

    @Test
    void miningAssistSettingsRoundTripOnHelpersModule() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.mining_helpers", true);
        config.writeBoolean("qol.mining_helpers.detector_solver", true);
        config.writeBoolean("qol.mining_helpers.fossil_excavator", true);
        config.writeBoolean("qol.mining_helpers.wishing_compass", true);
        assertEquals(Boolean.TRUE, config.readBoolean("qol.mining_helpers.detector_solver"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.mining_helpers.fossil_excavator"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.mining_helpers.wishing_compass"));
        assertTrue(config.resetModuleToDefaults("qol.mining_helpers"));
        assertFalse(config.isModuleEnabled("qol.mining_helpers"));
        assertFalse(config.readBoolean("qol.mining_helpers.detector_solver"));
        QolUtilityCatalog.ModuleDef helpers = QolUtilityCatalog.findById("qol.mining_helpers");
        assertTrue(helpers.settings().stream().anyMatch(
                setting -> "qol.mining_helpers.detector_solver".equals(setting.id())));
        assertTrue(helpers.settings().stream().anyMatch(
                setting -> "qol.mining_helpers.fossil_excavator".equals(setting.id())));
        assertTrue(helpers.settings().stream().anyMatch(
                setting -> "qol.mining_helpers.wishing_compass".equals(setting.id())));
    }
}
