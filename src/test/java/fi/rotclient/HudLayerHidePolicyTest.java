package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

final class HudLayerHidePolicyTest {
    @Test
    void catalogListsVanillaHidesAndRotOverlays() throws Exception {
        assertEquals(14, HudLayerCatalog.vanillaLayers().size());
        assertTrue(HudLayerCatalog.vanillaLayers().stream().anyMatch(
                layer -> "qol.hud_layout.hide_scoreboard".equals(layer.settingId())));
        assertTrue(HudLayerCatalog.vanillaLayers().stream().anyMatch(
                layer -> "qol.hud_layout.hide_hotbar".equals(layer.settingId())));
        assertTrue(HudLayerCatalog.rotOverlays().stream().anyMatch(
                layer -> "qol.performance_hud".equals(layer.settingId())));
        assertTrue(HudLayerCatalog.inspectorToggles().stream().anyMatch(
                toggle -> "qol.hud_layout.hide_scoreboard".equals(toggle.settingId())));
        assertTrue(HudElementCatalog.isHudVisibilityToggle(
                QolUtilityCatalog.findById("qol.hud_layout").settings().stream()
                        .filter(setting -> "qol.hud_layout.hide_hotbar".equals(setting.id()))
                        .findFirst()
                        .orElseThrow()));
        assertTrue(HudEditorChromePolicy.helpLines().stream().anyMatch(
                line -> line.toLowerCase().contains("right-click")));
        String client = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"));
        assertTrue(client.contains("VanillaHudElements.SCOREBOARD"));
        assertTrue(client.contains("VanillaHudElements.HOTBAR"));
        assertTrue(client.contains("VanillaHudElements.AIR_BAR"));
        assertTrue(client.contains("VanillaHudElements.PLAYER_LIST"));
        String landing = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/fi/rotclient/HudLayoutLandingPolicy.java"));
        assertTrue(landing.contains("Vanilla / Hypixel"));
        assertTrue(landing.contains("HudLayerCatalog.vanillaLayers()"));
    }

    @Test
    void layoutHideRequiresHudLayoutModule() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.hud_layout", true);
        qol.writeBoolean("qol.hud_layout.hide_scoreboard", true);
        qol.writeBoolean("qol.hud_layout.hide_hotbar", true);
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(qol)));
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.HOTBAR, HudLayerHidePolicy.flags(qol)));
        qol.setModuleEnabled("qol.hud_layout", false);
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(qol)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.HOTBAR, HudLayerHidePolicy.flags(qol)));
    }

    @Test
    void customScoreboardHidesVanillaSidebarWhenShowing() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.hud_layout", false);
        qol.setModuleEnabled(CustomScoreboardPolicy.MODULE_ID, true);
        try {
            SkyBlockAreaDetector.clearSkyblockPresence();
            assertFalse(HudLayerHidePolicy.shouldHide(
                    HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(qol)));
            SkyBlockAreaDetector.updateSkyblockPresence(List.of("SKYBLOCK"));
            assertTrue(HudLayerHidePolicy.shouldHide(
                    HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(qol)));
            qol.writeBoolean("qol.custom_scoreboard.hide_vanilla", false);
            assertFalse(HudLayerHidePolicy.shouldHide(
                    HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(qol)));
        } finally {
            SkyBlockAreaDetector.clearSkyblockPresence();
        }
    }

    @Test
    void playerDisplayVanillaHidesStopWhenModuleOff() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.player_display", true);
        qol.writeBoolean("qol.player_display.hide_vanilla_health", true);
        qol.writeBoolean("qol.player_display.hide_vanilla_food", true);
        qol.writeBoolean("qol.player_display.hide_vanilla_armor", true);
        qol.writeBoolean("qol.player_display.hide_vanilla_xp", true);
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.HEALTH, HudLayerHidePolicy.flags(qol)));
        qol.setModuleEnabled("qol.player_display", false);
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.HEALTH, HudLayerHidePolicy.flags(qol)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.FOOD, HudLayerHidePolicy.flags(qol)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.ARMOR, HudLayerHidePolicy.flags(qol)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.XP, HudLayerHidePolicy.flags(qol)));
    }

    @Test
    void optimizerHidesStopWhenModuleOff() {
        QolUtilityConfig optimizer = new QolUtilityConfig();
        optimizer.setModuleEnabled("qol.render_optimizer", true);
        optimizer.writeBoolean("qol.render_optimizer.hide_boss_bar", true);
        optimizer.writeBoolean("qol.render_optimizer.hide_food_bar", true);
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.BOSS, HudLayerHidePolicy.flags(optimizer)));
        optimizer.setModuleEnabled("qol.render_optimizer", false);
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.BOSS, HudLayerHidePolicy.flags(optimizer)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.FOOD, HudLayerHidePolicy.flags(optimizer)));
    }

    @Test
    void playerDisplayAndOptimizerHidesStillApplyWithoutLayout() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.player_display", true);
        qol.writeBoolean("qol.player_display.hide_vanilla_health", true);
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.HEALTH, HudLayerHidePolicy.flags(qol)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.FOOD, HudLayerHidePolicy.flags(qol)));

        QolUtilityConfig optimizer = new QolUtilityConfig();
        optimizer.setModuleEnabled("qol.render_optimizer", true);
        optimizer.writeBoolean("qol.render_optimizer.hide_boss_bar", true);
        optimizer.writeBoolean("qol.render_optimizer.hide_food_bar", true);
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.BOSS, HudLayerHidePolicy.flags(optimizer)));
        assertTrue(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.FOOD, HudLayerHidePolicy.flags(optimizer)));
        assertFalse(HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(optimizer)));
    }

    @Test
    void hideTogglesRoundTripAndInspectorUsesModuleSwitchesForOverlays() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.hud_layout", true);
        assertEquals(Boolean.FALSE, qol.readBoolean("qol.hud_layout.hide_scoreboard"));
        assertTrue(qol.toggleBooleanSetting("qol.hud_layout.hide_scoreboard"));
        assertEquals(Boolean.TRUE, qol.readBoolean("qol.hud_layout.hide_scoreboard"));
        assertTrue(HudLayerCatalog.rotOverlays().stream()
                .filter(layer -> "qol.performance_hud".equals(layer.settingId()))
                .findFirst()
                .orElseThrow()
                .moduleToggle());
        assertFalse(HudLayerCatalog.rotOverlays().stream()
                .filter(layer -> "qol.player_display.health_hud".equals(layer.settingId()))
                .findFirst()
                .orElseThrow()
                .moduleToggle());
        assertTrue(HudLayerTogglePolicy.toggle(qol, "qol.performance_hud", true));
        assertTrue(HudLayerTogglePolicy.isOn(qol, "qol.performance_hud", true));
        assertTrue(HudLayerTogglePolicy.isOn(qol, "qol.player_display.health_hud", false));
        assertTrue(HudLayerTogglePolicy.toggle(qol, "qol.player_display.health_hud", false));
        assertFalse(HudLayerTogglePolicy.isOn(qol, "qol.player_display.health_hud", false));
        assertTrue(HudLayerTogglePolicy.toggle(qol, "qol.hud_layout.hide_hotbar", false));
        assertTrue(HudLayerTogglePolicy.isOn(qol, "qol.hud_layout.hide_hotbar", false));
        assertTrue(HudLayerTogglePolicy.disable(qol, "qol.hud_layout.hide_hotbar", false));
        assertFalse(HudLayerTogglePolicy.isOn(qol, "qol.hud_layout.hide_hotbar", false));
        assertTrue(HudLayerTogglePolicy.enable(qol, "qol.hud_layout.hide_hotbar", false));
        assertTrue(HudLayerTogglePolicy.isOn(qol, "qol.hud_layout.hide_hotbar", false));
        assertFalse(HudLayerTogglePolicy.enable(qol, "qol.hud_layout.hide_hotbar", false));
        assertTrue(qol.resetModuleToDefaults("qol.hud_layout"));
        assertEquals(Boolean.FALSE, qol.readBoolean("qol.hud_layout.hide_scoreboard"));
        assertEquals(Boolean.FALSE, qol.readBoolean("qol.hud_layout.hide_hotbar"));
    }
}
