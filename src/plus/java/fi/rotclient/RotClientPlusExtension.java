package fi.rotclient;

import com.google.gson.JsonObject;
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
    public boolean isGameplayCheatModule(String moduleId) {
        return "qol.secret_hitboxes".equals(moduleId)
                || "qol.inventory_walk".equals(moduleId)
                || "qol.mob_highlight".equals(moduleId);
    }

    @Override
    public List<QolUtilityCatalog.SettingDef> extraSettings(String moduleId) {
        return QolPlusCatalog.extraSettings(moduleId);
    }

    @Override
    public List<HudLayerCatalog.Layer> extraHudLayers() {
        return List.of(
                new HudLayerCatalog.Layer(
                        "qol.auto_clicker.cps_hud",
                        "qol.auto_clicker.cps_hud",
                        "Auto Clicker CPS",
                        HudLayerCatalog.Kind.ROT,
                        false),
                new HudLayerCatalog.Layer(
                        "wardrobe",
                        "qol.wardrobe_keybinds",
                        "Wardrobe Equipping",
                        HudLayerCatalog.Kind.ROT,
                        true));
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
        if ("qol.mob_highlight".equals(moduleId)) {
            return MobHighlightSettings.from(config).enabled();
        }
        if ("qol.world_scanner".equals(moduleId)) {
            return WorldScannerSettings.from(config).enabled();
        }
        if ("qol.trajectories".equals(moduleId)) {
            return TrajectoriesSettings.from(config).enabled();
        }
        if ("qol.auto_conversation".equals(moduleId)) {
            return AutoConversationSettings.from(config).enabled();
        }
        if ("qol.inventory_walk".equals(moduleId)) {
            return InventoryWalkSettings.from(config).enabled();
        }
        if ("qol.secret_hitboxes".equals(moduleId)) {
            return SecretHitboxesSettings.from(config).enabled();
        }
        if ("qol.auto_clicker".equals(moduleId)) {
            return config.autoClickerEnabled;
        }
        return null;
    }

    @Override
    public boolean writeModuleEnabled(
            QolUtilityConfig config, String moduleId, boolean enabled) {
        if ("qol.mob_highlight".equals(moduleId)) {
            MobHighlightSettings.enabled(config, enabled);
            return true;
        }
        if ("qol.world_scanner".equals(moduleId)) {
            WorldScannerSettings.enabled(config, enabled);
            return true;
        }
        if ("qol.trajectories".equals(moduleId)) {
            TrajectoriesSettings.enabled(config, enabled);
            return true;
        }
        if ("qol.auto_conversation".equals(moduleId)) {
            AutoConversationSettings.write(config, "autoConversationEnabled", enabled);
            return true;
        }
        if ("qol.inventory_walk".equals(moduleId)) {
            InventoryWalkSettings.enabled(config, enabled);
            return true;
        }
        if ("qol.secret_hitboxes".equals(moduleId)) {
            SecretHitboxesSettings.write(config, "secretHitboxesEnabled", enabled);
            return true;
        }
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
        if (settingId.startsWith("qol.world_scanner.")) {
            return WorldScannerSettings.readBoolean(config, settingId);
        }
        return switch (settingId) {
            case "qol.mob_highlight.highlight_key" -> MobHighlightSettings.from(config).requireKey();
            case "qol.etherwarp.depth" -> PlusOpaqueSettings.bool(config, "etherwarpDepth", true);
            case "qol.mob_highlight.depth" -> MobHighlightSettings.from(config).depth();
            case "qol.mob_highlight.tracers" -> MobHighlightSettings.from(config).tracers();
            case "qol.trajectories.bows" -> TrajectoriesSettings.from(config).bows();
            case "qol.trajectories.pearls" -> TrajectoriesSettings.from(config).pearls();
            case "qol.trajectories.lines" -> TrajectoriesSettings.from(config).lines();
            case "qol.trajectories.boxes" -> TrajectoriesSettings.from(config).boxes();
            case "qol.trajectories.depth" -> TrajectoriesSettings.from(config).depth();
            case "qol.trajectories.plane" -> TrajectoriesSettings.from(config).plane();
            case "qol.trajectories.entities" -> TrajectoriesSettings.from(config).entities();
            case "qol.auto_conversation.multi" -> AutoConversationSettings.from(config).multi();
            case "qol.auto_conversation.green" -> AutoConversationSettings.from(config).green();
            case "qol.secret_hitboxes.only_dungeons" -> SecretHitboxesSettings.from(config).onlyDungeons();
            case "qol.secret_hitboxes.lever" -> SecretHitboxesSettings.from(config).lever();
            case "qol.secret_hitboxes.old_lever" -> SecretHitboxesSettings.from(config).oldLever();
            case "qol.secret_hitboxes.button" -> SecretHitboxesSettings.from(config).button();
            case "qol.secret_hitboxes.flat_button" -> SecretHitboxesSettings.from(config).flatButton();
            case "qol.secret_hitboxes.skull" -> SecretHitboxesSettings.from(config).skull();
            case "qol.secret_hitboxes.chests" -> SecretHitboxesSettings.from(config).chests();
            case "qol.secret_hitboxes.only_trapped" -> SecretHitboxesSettings.from(config).onlyTrappedChests();
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
        if (settingId.startsWith("qol.world_scanner.")) {
            return WorldScannerSettings.writeBoolean(config, settingId, value);
        }
        switch (settingId) {
            case "qol.mob_highlight.highlight_key", "qol.mob_highlight.depth",
                 "qol.mob_highlight.tracers" -> MobHighlightSettings.writeBoolean(config, settingId, value);
            case "qol.etherwarp.depth" -> PlusOpaqueSettings.write(config, "etherwarpDepth", value);
            case "qol.trajectories.bows", "qol.trajectories.pearls",
                 "qol.trajectories.lines", "qol.trajectories.boxes",
                 "qol.trajectories.depth", "qol.trajectories.plane",
                 "qol.trajectories.entities" -> TrajectoriesSettings.writeBoolean(config, settingId, value);
            case "qol.auto_conversation.multi" -> AutoConversationSettings.write(config, "autoConversationMulti", value);
            case "qol.auto_conversation.green" -> AutoConversationSettings.write(config, "autoConversationGreen", value);
            case "qol.secret_hitboxes.only_dungeons" -> SecretHitboxesSettings.write(config, "secretHitboxesOnlyDungeons", value);
            case "qol.secret_hitboxes.lever" -> SecretHitboxesSettings.write(config, "secretHitboxesLever", value);
            case "qol.secret_hitboxes.old_lever" -> SecretHitboxesSettings.write(config, "secretHitboxesOldLever", value);
            case "qol.secret_hitboxes.button" -> SecretHitboxesSettings.write(config, "secretHitboxesButton", value);
            case "qol.secret_hitboxes.flat_button" -> SecretHitboxesSettings.write(config, "secretHitboxesFlatButton", value);
            case "qol.secret_hitboxes.skull" -> SecretHitboxesSettings.write(config, "secretHitboxesSkull", value);
            case "qol.secret_hitboxes.chests" -> SecretHitboxesSettings.write(config, "secretHitboxesChests", value);
            case "qol.secret_hitboxes.only_trapped" -> SecretHitboxesSettings.write(config, "secretHitboxesOnlyTrappedChests", value);
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
        if (settingId.startsWith("qol.world_scanner.")) {
            return WorldScannerSettings.readNumber(config, settingId);
        }
        return switch (settingId) {
            case "qol.trajectories.range" -> (double) TrajectoriesSettings.from(config).range();
            case "qol.trajectories.width" -> (double) TrajectoriesSettings.from(config).width();
            case "qol.trajectories.box_size" -> (double) TrajectoriesSettings.from(config).boxSize();
            case "qol.trajectories.plane_size" -> (double) TrajectoriesSettings.from(config).planeSize();
            case "qol.auto_conversation.delay" -> (double) AutoConversationSettings.from(config).delayTicks();
            case "qol.inventory_walk.ping" -> (double) InventoryWalkSettings.from(config).pingMs();
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
        if (settingId.startsWith("qol.world_scanner.")) {
            return WorldScannerSettings.writeNumber(config, settingId, value);
        }
        switch (settingId) {
            case "qol.trajectories.range", "qol.trajectories.width",
                 "qol.trajectories.box_size", "qol.trajectories.plane_size" ->
                    TrajectoriesSettings.writeNumber(config, settingId, value);
            case "qol.auto_conversation.delay" -> AutoConversationSettings.delayTicks(config, (int) Math.round(value));
            case "qol.inventory_walk.ping" -> InventoryWalkSettings.pingMs(config, (int) Math.round(value));
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
            case "qol.mob_highlight.add_key" -> MobHighlightSettings.from(config).addKey();
            case "qol.auto_clicker.left_keybind" -> config.autoClickerLeftKeybind;
            case "qol.auto_clicker.right_keybind" -> config.autoClickerRightKeybind;
            default -> null;
        };
    }

    @Override
    public String readEnum(QolUtilityConfig config, String settingId) {
        return WorldScannerSettings.readEnum(config, settingId);
    }

    @Override
    public boolean writeEnum(QolUtilityConfig config, String settingId, String value) {
        return WorldScannerSettings.writeEnum(config, settingId, value);
    }

    @Override
    public Integer readColor(QolUtilityConfig config, String settingId) {
        if ("qol.mob_highlight.color".equals(settingId)) {
            return MobHighlightSettings.from(config).color();
        }
        if (settingId != null && settingId.startsWith("qol.world_scanner.")) {
            return WorldScannerSettings.readColor(config, settingId);
        }
        return "qol.trajectories.color".equals(settingId)
                ? TrajectoriesSettings.from(config).color() : null;
    }

    @Override
    public boolean writeColor(QolUtilityConfig config, String settingId, int value) {
        if ("qol.mob_highlight.color".equals(settingId)) {
            MobHighlightSettings.color(config, value);
            return true;
        }
        if (settingId != null && settingId.startsWith("qol.world_scanner.")) {
            return WorldScannerSettings.writeColor(config, settingId, value);
        }
        if (!"qol.trajectories.color".equals(settingId)) return false;
        TrajectoriesSettings.color(config, value);
        return true;
    }

    @Override
    public boolean writeKeybind(
            QolUtilityConfig config, String settingId, String value) {
        String stored = value == null ? "" : value.trim();
        switch (settingId == null ? "" : settingId) {
            case "qol.mob_highlight.add_key" -> MobHighlightSettings.addKey(config, stored);
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
        if ("qol.etherwarp".equals(moduleId)) {
            PlusOpaqueSettings.reset(config, "etherwarpDepth");
            return true;
        }
        if ("qol.mob_highlight".equals(moduleId)) {
            MobHighlightSettings.reset(config);
            return true;
        }
        if ("qol.world_scanner".equals(moduleId)) {
            WorldScannerSettings.reset(config);
            return true;
        }
        if ("qol.trajectories".equals(moduleId)) {
            TrajectoriesSettings.reset(config);
            return true;
        }
        if ("qol.auto_conversation".equals(moduleId)) {
            AutoConversationSettings.reset(config);
            return true;
        }
        if ("qol.inventory_walk".equals(moduleId)) {
            InventoryWalkSettings.reset(config);
            return true;
        }
        if ("qol.secret_hitboxes".equals(moduleId)) {
            SecretHitboxesSettings.reset(config);
            return true;
        }
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
    public void migrateConfigJson(JsonObject root) {
        if (root == null) return;
        JsonObject qol = root.has("qolUtilities") && root.get("qolUtilities").isJsonObject()
                ? root.getAsJsonObject("qolUtilities") : new JsonObject();
        JsonObject fields = qol.has("extensionFields") && qol.get("extensionFields").isJsonObject()
                ? qol.getAsJsonObject("extensionFields") : new JsonObject();
        for (String key : List.of(
                "secretHitboxesEnabled", "secretHitboxesOnlyDungeons",
                "secretHitboxesLever", "secretHitboxesOldLever",
                "secretHitboxesButton", "secretHitboxesFlatButton",
                "secretHitboxesSkull", "secretHitboxesChests",
                "secretHitboxesOnlyTrappedChests")) {
            if (!fields.has(key) && qol.has(key)) {
                fields.add(key, qol.get(key).deepCopy());
            }
        }
        if (!fields.has("secretHitboxesLever")) fields.addProperty("secretHitboxesLever", true);
        if (!fields.has("secretHitboxesButton")) fields.addProperty("secretHitboxesButton", true);
        if (!fields.has("secretHitboxesSkull")) fields.addProperty("secretHitboxesSkull", true);
        qol.add("extensionFields", fields);
        root.add("qolUtilities", qol);
    }

    @Override
    public void savePersistence() {
    }

    @Override
    public QolNumberSettings.Spec numberSpec(String settingId) {
        if (settingId == null || settingId.isBlank()) {
            return null;
        }
        if (settingId.startsWith("qol.world_scanner.target.")) {
            if (settingId.endsWith(".opacity"))
                return new QolNumberSettings.Spec(0.0D, 1.0D, 0.01D, true);
            if (settingId.endsWith(".name_scale"))
                return new QolNumberSettings.Spec(0.5D, 2.0D, 0.05D, true);
        }
        return switch (settingId) {
            case "qol.world_scanner.esp_range" -> new QolNumberSettings.Spec(
                    WorldScannerPolicy.MIN_ESP_RANGE, WorldScannerPolicy.MAX_ESP_RANGE, 1.0D, true);
            case "qol.trajectories.range" -> new QolNumberSettings.Spec(
                    TrajectoryPredictor.MIN_RANGE, TrajectoryPredictor.MAX_RANGE, 1.0D, true);
            case "qol.trajectories.width" -> new QolNumberSettings.Spec(0.1D, 5.0D, 0.1D, true);
            case "qol.trajectories.box_size" -> new QolNumberSettings.Spec(0.5D, 3.0D, 0.1D, true);
            case "qol.trajectories.plane_size" -> new QolNumberSettings.Spec(0.5D, 8.0D, 0.1D, true);
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
