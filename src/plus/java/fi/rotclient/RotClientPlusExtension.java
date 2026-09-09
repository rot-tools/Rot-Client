package fi.rotclient;

import java.util.List;

/**
 * ServiceLoader provider that adds Plus catalog modules/settings and HUD bits.
 * Config field storage stays on {@link QolUtilityConfig} / {@link QolSkyblockExtras};
 * this class only supplies Plus-only specs and catalog rows.
 */
public final class RotClientPlusExtension implements QolFlavorExtension {
    @Override
    public boolean isPlus() {
        return true;
    }

    @Override
    public String productName() {
        return "Rot Client+";
    }

    @Override
    public String productHeader() {
        return "ROT CLIENT+";
    }

    @Override
    public String modId() {
        return "rotclientplus";
    }

    @Override
    public String configFileName() {
        return "rotclient-plus.json";
    }

    @Override
    public List<QolUtilityCatalog.ModuleDef> extraModules() {
        return QolPlusCatalog.extraModules();
    }

    @Override
    public List<QolUtilityCatalog.SettingDef> extraSettings(String moduleId) {
        return QolPlusCatalog.extraSettings(moduleId);
    }

    @Override
    public List<HudLayerCatalog.Layer> extraHudLayers() {
        return List.of(new HudLayerCatalog.Layer(
                "qol.auto_clicker.cps_hud",
                "qol.auto_clicker.cps_hud",
                "Auto Clicker CPS",
                HudLayerCatalog.Kind.ROT,
                false));
    }

    @Override
    public String hudFocusId(String settingId) {
        if (settingId != null && settingId.contains("auto_clicker")) {
            return "auto_clicker";
        }
        return "";
    }

    @Override
    public List<HudElementCatalog.InspectorToggle> inspectorToggles(String poseId) {
        if ("auto_clicker".equals(poseId)) {
            return List.of(new HudElementCatalog.InspectorToggle(
                    "qol.auto_clicker.cps_hud", "CPS HUD"));
        }
        return List.of();
    }

    @Override
    public String disableSettingId(String poseId) {
        if ("auto_clicker".equals(poseId)) {
            return "qol.auto_clicker.cps_hud";
        }
        return "";
    }

    @Override
    public Boolean readModuleEnabled(QolUtilityConfig config, String moduleId) {
        if ("qol.auto_clicker".equals(moduleId)) {
            return config.autoClickerEnabled;
        }
        return null;
    }

    @Override
    public boolean writeModuleEnabled(
            QolUtilityConfig config, String moduleId, boolean enabled) {
        if ("qol.auto_clicker".equals(moduleId)) {
            config.autoClickerEnabled = enabled;
            return true;
        }
        return false;
    }

    @Override
    public Boolean readBoolean(QolUtilityConfig config, String settingId) {
        if (settingId == null) {
            return null;
        }
        return switch (settingId) {
            case "qol.auto_clicker.whitelist_only" -> config.autoClickerWhitelistOnly;
            case "qol.auto_clicker.cps_hud" -> config.autoClickerCpsHudEnabled;
            case "qol.auto_clicker.allow_breaking" -> config.autoClickerAllowBreaking;
            case "qol.auto_clicker.block_breaker" -> config.autoClickerBlockBreaker;
            case "qol.auto_clicker.terminator_only" -> config.autoClickerTerminatorOnly;
            case "qol.auto_clicker.enable_left" -> config.autoClickerEnableLeft;
            case "qol.auto_clicker.enable_right" -> config.autoClickerEnableRight;
            default -> null;
        };
    }

    @Override
    public boolean writeBoolean(
            QolUtilityConfig config, String settingId, boolean value) {
        if (settingId == null) {
            return false;
        }
        switch (settingId) {
            case "qol.auto_clicker.whitelist_only" -> config.autoClickerWhitelistOnly = value;
            case "qol.auto_clicker.cps_hud" -> config.autoClickerCpsHudEnabled = value;
            case "qol.auto_clicker.allow_breaking" -> config.autoClickerAllowBreaking = value;
            case "qol.auto_clicker.block_breaker" -> config.autoClickerBlockBreaker = value;
            case "qol.auto_clicker.terminator_only" -> config.autoClickerTerminatorOnly = value;
            case "qol.auto_clicker.enable_left" -> config.autoClickerEnableLeft = value;
            case "qol.auto_clicker.enable_right" -> config.autoClickerEnableRight = value;
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public Double readNumber(QolUtilityConfig config, String settingId) {
        if (settingId == null) {
            return null;
        }
        return switch (settingId) {
            case "qol.auto_clicker.cps" -> (double) config.autoClickerCps;
            case "qol.auto_clicker.left_cps" -> (double) config.autoClickerLeftCps;
            case "qol.auto_clicker.right_cps" -> (double) config.autoClickerRightCps;
            default -> null;
        };
    }

    @Override
    public boolean writeNumber(
            QolUtilityConfig config, String settingId, double value) {
        if (settingId == null) {
            return false;
        }
        switch (settingId) {
            case "qol.auto_clicker.cps" ->
                    config.autoClickerCps = AutoClickerPolicy.clampCps((float) value);
            case "qol.auto_clicker.left_cps" ->
                    config.autoClickerLeftCps = AutoClickerPolicy.clampCps((float) value);
            case "qol.auto_clicker.right_cps" ->
                    config.autoClickerRightCps = AutoClickerPolicy.clampCps((float) value);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public String readKeybind(QolUtilityConfig config, String settingId) {
        if (settingId == null) {
            return null;
        }
        return switch (settingId) {
            case "qol.auto_clicker.left_keybind" -> config.autoClickerLeftKeybind;
            case "qol.auto_clicker.right_keybind" -> config.autoClickerRightKeybind;
            default -> null;
        };
    }

    @Override
    public boolean writeKeybind(
            QolUtilityConfig config, String settingId, String value) {
        String stored = value == null ? "" : value.trim();
        switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.left_keybind" -> config.autoClickerLeftKeybind = stored;
            case "qol.auto_clicker.right_keybind" -> config.autoClickerRightKeybind = stored;
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean resetModule(QolUtilityConfig config, String moduleId) {
        if (!"qol.auto_clicker".equals(moduleId)) {
            return false;
        }
        QolUtilityConfig defaults = new QolUtilityConfig();
        config.autoClickerEnabled = defaults.autoClickerEnabled;
        config.autoClickerCpsHudEnabled = defaults.autoClickerCpsHudEnabled;
        config.autoClickerWhitelistOnly = defaults.autoClickerWhitelistOnly;
        config.autoClickerAllowBreaking = defaults.autoClickerAllowBreaking;
        config.autoClickerBlockBreaker = defaults.autoClickerBlockBreaker;
        config.autoClickerTerminatorOnly = defaults.autoClickerTerminatorOnly;
        config.autoClickerCps = defaults.autoClickerCps;
        config.autoClickerEnableLeft = defaults.autoClickerEnableLeft;
        config.autoClickerEnableRight = defaults.autoClickerEnableRight;
        config.autoClickerLeftCps = defaults.autoClickerLeftCps;
        config.autoClickerRightCps = defaults.autoClickerRightCps;
        config.autoClickerLeftKeybind = defaults.autoClickerLeftKeybind;
        config.autoClickerRightKeybind = defaults.autoClickerRightKeybind;
        config.autoClickerLeftWhitelist = new java.util.ArrayList<>(defaults.autoClickerLeftWhitelist);
        config.autoClickerRightWhitelist = new java.util.ArrayList<>(defaults.autoClickerRightWhitelist);
        return true;
    }

    @Override
    public void loadPersistence() {
        QolPlusConfigStore.migrateFromSharedIfNeeded();
    }

    @Override
    public void savePersistence() {
    }

    @Override
    public QolNumberSettings.Spec numberSpec(String settingId) {
        if (settingId == null || settingId.isBlank()) {
            return null;
        }
        return switch (settingId) {
            case "qol.auto_clicker.cps",
                 "qol.auto_clicker.left_cps",
                 "qol.auto_clicker.right_cps" ->
                    new QolNumberSettings.Spec(
                            AutoClickerPolicy.MIN_CPS,
                            AutoClickerPolicy.MAX_CPS,
                            0.5D,
                            false);
            case "qol.inventory_walk.ping" ->
                    new QolNumberSettings.Spec(
                            InventoryWalkPolicy.MIN_PING_MS,
                            InventoryWalkPolicy.MAX_PING_MS,
                            10.0D,
                            true);
            case "qol.freecam.speed" ->
                    new QolNumberSettings.Spec(
                            FreecamPolicy.MIN_SPEED,
                            FreecamPolicy.MAX_SPEED,
                            0.1D,
                            true);
            case "qol.foraging_cheats.min_cluster" ->
                    new QolNumberSettings.Spec(1.0D, 35.0D, 1.0D, true);
            case "qol.foraging_cheats.click_delay" ->
                    new QolNumberSettings.Spec(1.0D, 20.0D, 1.0D, true);
            case "qol.camera.distance" ->
                    new QolNumberSettings.Spec(
                            TempleDungeonPolicy.MIN_CAMERA_DISTANCE,
                            TempleDungeonPolicy.MAX_CAMERA_DISTANCE,
                            0.5D,
                            true);
            case "qol.auto_conversation.delay" ->
                    new QolNumberSettings.Spec(0.0D, 40.0D, 1.0D, true);
            case "qol.auto_experiments.click_delay" ->
                    new QolNumberSettings.Spec(100.0D, 1000.0D, 1.0D, true);
            case "qol.auto_experiments.delay_variety" ->
                    new QolNumberSettings.Spec(0.0D, 1000.0D, 1.0D, true);
            case "qol.auto_experiments.serum_count" ->
                    new QolNumberSettings.Spec(0.0D, 3.0D, 1.0D, true);
            case "qol.auto_gfs.timer_increments" ->
                    new QolNumberSettings.Spec(1.0D, 60.0D, 1.0D, true);
            case "qol.auto_sell.delay" ->
                    new QolNumberSettings.Spec(2.0D, 10.0D, 1.0D, true);
            case "qol.auto_sell.randomization" ->
                    new QolNumberSettings.Spec(0.0D, 5.0D, 1.0D, true);
            default -> null;
        };
    }
}
