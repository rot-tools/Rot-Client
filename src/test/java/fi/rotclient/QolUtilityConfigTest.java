package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class QolUtilityConfigTest {
    @Test
    void everyVisibleSettingHasAWorkingConfigContract() {
        QolUtilityConfig config = new QolUtilityConfig();
        List<String> unsupported = new ArrayList<>();

        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                boolean supported = switch (setting.type()) {
                    case TOGGLE -> config.readBoolean(setting.id()) != null;
                    case ENUM -> !setting.enumOptions().isEmpty()
                            && config.writeEnum(setting.id(), setting.enumOptions().getFirst());
                    case NUMBER -> config.readNumber(setting.id()) != null
                            && QolNumberSettings.spec(setting.id()) != null;
                    case COLOR -> config.readColor(setting.id()) != null;
                    case KEYBIND -> config.writeKeybind(setting.id(), "K")
                            && "K".equals(config.readKeybind(setting.id()));
                    case TEXT -> config.writeText(setting.id(), "contract-value");
                    case ACTION, SECTION -> true;
                };
                if (!supported) {
                    unsupported.add(setting.type() + " " + setting.id());
                }
            }
        }

        assertTrue(unsupported.isEmpty(), "Unsupported dashboard settings: " + unsupported);
    }

    @Test
    void everyInternallyOwnedToggleableModuleRoundTripsAndResets() {
        QolUtilityConfig config = new QolUtilityConfig();
        List<String> unsupported = new ArrayList<>();
        List<String> external = List.of("qol.fullbright", "qol.auto_sprint", "qol.camera");

        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if (!module.toggleable() || external.contains(module.id())) {
                continue;
            }
            config.setModuleEnabled(module.id(), true);
            boolean enabled = config.isModuleEnabled(module.id());
            config.setModuleEnabled(module.id(), false);
            boolean disabled = !config.isModuleEnabled(module.id());
            boolean reset = config.resetModuleToDefaults(module.id());
            if (!enabled || !disabled || !reset) {
                unsupported.add(module.id());
            }
        }

        assertTrue(unsupported.isEmpty(), "Modules without full toggle/reset contract: " + unsupported);
    }

    @Test
    void resetSupportsExternallyOwnedAutoSprintAndCameraModules() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.autoSprintKeybind = "K";
        config.cameraKeybind = "V";

        assertTrue(config.resetModuleToDefaults("qol.auto_sprint"));
        assertTrue(config.resetModuleToDefaults("qol.camera"));
        assertEquals(new QolUtilityConfig().autoSprintKeybind, config.autoSprintKeybind);
        assertEquals(new QolUtilityConfig().cameraKeybind, config.cameraKeybind);
    }

    @Test
    void toggleAndEnumSettingsRoundTrip() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertFalse(config.hideLightning);
        assertTrue(config.toggleBooleanSetting(
                "qol.render_optimizer.hide_lightning"));
        assertTrue(config.hideLightning);

        assertEquals("Horizontal", config.performanceDirection);
        assertTrue(config.cycleEnum(
                "qol.performance_hud.direction",
                java.util.List.of("Horizontal", "Vertical")));
        assertEquals("Vertical", config.performanceDirection);
    }

    @Test
    void playerSizeDefaultsAndNudges() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertEquals(1.0F, config.playerSizeX, 0.0001F);
        assertEquals(1.0F, config.playerSizeY, 0.0001F);
        assertEquals(1.0F, config.playerSizeZ, 0.0001F);
        assertTrue(config.nudgeNumber("qol.player_size.x", true));
        assertTrue(config.playerSizeX > 1.0F);
    }

    @Test
    void playerSizeCanBeReducedToTenPercent() {
        QolUtilityConfig config = new QolUtilityConfig();
        for (int i = 0; i < 40; i++) {
            assertTrue(config.nudgeNumber("qol.player_size.x", false));
        }
        assertEquals(0.1F, config.playerSizeX, 0.0001F);
    }

    @Test
    void keybindsRoundTripThroughWriteAndRead() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(config.writeKeybind("qol.command_keybinds.pets", "P"));
        assertEquals("P", config.readKeybind("qol.command_keybinds.pets"));
        assertTrue(config.writeKeybind("qol.auto_clicker.left_keybind", "LMB"));
        assertEquals("LMB", config.readKeybind("qol.auto_clicker.left_keybind"));
        assertTrue(config.writeKeybind("qol.wardrobe_keybinds.next", "RIGHT"));
        assertEquals("RIGHT", config.displayKeybind("qol.wardrobe_keybinds.next"));
        assertFalse(config.writeKeybind("qol.unknown.keybind", "X"));
    }

    @Test
    void resetModuleToDefaultsOnlyTouchesThatModule() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.renderOptimizerEnabled = true;
        config.hideLightning = true;
        config.performanceHudEnabled = true;
        config.performanceShowFps = false;
        assertTrue(config.resetModuleToDefaults("qol.render_optimizer"));
        assertFalse(config.renderOptimizerEnabled);
        assertFalse(config.hideLightning);
        assertTrue(config.performanceHudEnabled);
        assertFalse(config.performanceShowFps);
    }

    @Test
    void secretHitboxesDefaultToParentSubToggles() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(config.secretHitboxesOnlyDungeons);
        assertFalse(config.secretHitboxesLever);
        assertFalse(config.secretHitboxesButton);
        assertFalse(config.secretHitboxesSkull);
        assertTrue(config.secretHitboxesOldLever);
        assertFalse(config.secretHitboxesChests);
    }

    @Test
    void inventoryOverlayAndPetHudDefaultOn() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(config.inventoryOverlayEnabled);
        assertTrue(config.inventoryOverlayEquipment);
        assertTrue(config.inventoryOverlayHideRecipeBook);
        assertTrue(config.inventoryOverlayHideStatusEffects);
        assertTrue(config.inventoryOverlayPetSlot);
        assertTrue(config.skillLevelsEnabled);
        assertTrue(config.petHudEnabled);
        assertTrue(config.toggleBooleanSetting("qol.inventory_overlay.hide_recipe_book"));
        assertFalse(config.inventoryOverlayHideRecipeBook);
        assertTrue(config.toggleBooleanSetting("qol.inventory_overlay.hide_status_effects"));
        assertFalse(config.inventoryOverlayHideStatusEffects);
        config.inventoryOverlayPetOffsetX = 40;
        config.inventoryOverlayPetOffsetY = -12;
        assertTrue(config.resetModuleToDefaults("qol.inventory_overlay"));
        assertTrue(config.inventoryOverlayHideRecipeBook);
        assertTrue(config.inventoryOverlayHideStatusEffects);
        assertEquals(0, config.inventoryOverlayPetOffsetX);
        assertEquals(0, config.inventoryOverlayPetOffsetY);
        config.writeColor("qol.inventory_overlay.chrome_panel", 0x80FF0000);
        assertEquals(0x80FF0000, config.readColor("qol.inventory_overlay.chrome_panel"));
        assertTrue(config.resetModuleToDefaults("qol.inventory_overlay"));
        assertEquals(0, config.inventoryChromePanel);
        assertEquals(0, config.inventoryChromeHeader);
        assertEquals(0, config.inventoryChromeMain);
        assertEquals(0, config.inventoryChromeHotbar);
        assertEquals(0, config.inventoryChromeBorder);
    }

    @Test
    void nameHiderCustomTextAndModeRoundTrip() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertFalse(config.nameHiderEnabled);
        config.setModuleEnabled("qol.name_hider", true);
        assertTrue(config.isModuleEnabled("qol.name_hider"));
        assertEquals(NameHiderPolicy.MODE_SCRAMBLE, config.readEnum("qol.name_hider.mode"));
        assertTrue(config.writeEnum("qol.name_hider.mode", "Custom"));
        assertEquals(NameHiderPolicy.MODE_CUSTOM, config.nameHiderMode);
        assertTrue(config.writeText("qol.name_hider.custom_name", "  Shadow  "));
        assertEquals("  Shadow  ", config.readText("qol.name_hider.custom_name"));
        assertTrue(config.resetModuleToDefaults("qol.name_hider"));
        assertFalse(config.nameHiderEnabled);
        assertEquals("", config.nameHiderCustomName);
    }

    @Test
    void chatSlotAndWaypointSettingsRoundTrip() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertFalse(config.isModuleEnabled("qol.waypoints"));
        config.setModuleEnabled("qol.waypoints", true);
        assertTrue(config.isModuleEnabled("qol.waypoints"));
        assertTrue(config.waypointsFromParty);
        assertTrue(config.toggleBooleanSetting("qol.waypoints.from_all"));
        assertTrue(config.waypointsFromAll);
        assertEquals(WaypointPolicy.PING_OFF, config.readEnum("qol.waypoints.ping_dropdown"));
        assertTrue(config.writeEnum("qol.waypoints.ping_dropdown", "Look Target"));
        assertEquals(WaypointPolicy.PING_LOOK_TARGET, config.waypointsPingMode);
        assertTrue(config.writeKeybind("qol.waypoints.keybind", "G"));
        assertEquals("G", config.readKeybind("qol.waypoints.keybind"));

        java.util.Map<Integer, Integer> binds = new java.util.LinkedHashMap<>();
        binds.put(12, 36);
        config.putSlotBindsForActiveProfile(binds);
        assertEquals(36, config.slotBindsForActiveProfile().get(12));
        assertTrue(config.resetModuleToDefaults("qol.slot_binds"));
        assertTrue(config.slotBindsForActiveProfile().isEmpty());
        assertTrue(config.resetModuleToDefaults("qol.waypoints"));
        assertFalse(config.waypointsEnabled);
        assertFalse(config.waypointsFromAll);
        assertEquals(WaypointPolicy.PING_OFF, config.waypointsPingMode);
    }

    @Test
    void itemRarityOpacitySlidersRoundTrip() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertEquals(ItemRarityPolicy.DEFAULT_FILL_ALPHA, config.itemRarityFillAlpha, 0.0001F);
        assertEquals(ItemRarityPolicy.DEFAULT_OUTLINE_ALPHA, config.itemRarityOutlineAlpha, 0.0001F);
        assertTrue(config.writeNumber("qol.item_rarity.fill_alpha", 0.18D));
        assertTrue(config.writeNumber("qol.item_rarity.outline_alpha", 0.7D));
        assertEquals(0.18F, config.itemRarityFillAlpha, 0.0001F);
        assertEquals(0.7F, config.itemRarityOutlineAlpha, 0.0001F);
        assertTrue(config.resetModuleToDefaults("qol.item_rarity"));
        assertEquals(ItemRarityPolicy.DEFAULT_FILL_ALPHA, config.itemRarityFillAlpha, 0.0001F);
        assertEquals(ItemRarityPolicy.DEFAULT_OUTLINE_ALPHA, config.itemRarityOutlineAlpha, 0.0001F);
    }
}
