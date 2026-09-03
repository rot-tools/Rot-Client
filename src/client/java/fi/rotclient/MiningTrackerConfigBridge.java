package fi.rotclient;

/**
 * Maps catalog setting ids onto {@link TrackerConfig} HUD / enable flags.
 * One source of truth: tracker JSON, not a second extras map.
 */
final class MiningTrackerConfigBridge {
    private MiningTrackerConfigBridge() {
    }

    static Boolean readBoolean(TrackerConfig config, String settingId) {
        if (config == null || settingId == null) {
            return null;
        }
        return switch (settingId) {
            case "qol.mining_tracker.show_blocks" -> config.showBlocks;
            case "qol.mining_tracker.show_raw" -> config.showRawMaterial;
            case "qol.mining_tracker.show_enchanted" -> config.showEnchantedMaterial;
            case "qol.mining_tracker.show_session_profit" -> config.showSessionProfit;
            case "qol.mining_tracker.show_unsold" -> config.showUnsoldValue;
            case "qol.mining_tracker.show_cph" -> config.showCoinsPerHour;
            case "qol.mining_tracker.show_mph" -> config.showMaterialPerHour;
            case "qol.mining_tracker.show_session_time" -> config.showSessionTime;
            case "qol.mining_tracker.show_tool" -> config.showActiveTool;
            case "qol.mining_tracker.show_area" -> config.showArea;
            case "qol.mining_tracker.show_graph" -> config.showRateGraph;
            case "qol.mining_tracker.show_fortune" -> config.showDropAndFortune;
            case "qol.mining_tracker.show_bazaar" -> config.showBazaarPrices;
            case "qol.mining_tracker.show_value_panel" -> config.showValuePanel;
            case "qol.mining_tracker.show_hud_title" -> config.showHudTitle;
            case "qol.mining_tracker.show_hud_status" -> config.showHudStatus;
            case "qol.mining_tracker.show_hud_version" -> config.showHudVersion;
            case "qol.mining_tracker.show_auto_pause" -> config.showHudAutoPause;
            case "qol.mining_tracker.hud_background" -> config.hudShowBackground;
            case "qol.mining_tracker.show_target_heading" -> config.showTargetHeading;
            case "qol.mining_tracker.show_other_section" -> config.showOtherSection;
            case "qol.mining_tracker.show_target_value" -> config.showTargetValue;
            case "qol.mining_tracker.show_other_value" -> config.showOtherValue;
            case "qol.mining_tracker.show_total_mined" -> config.showTotalMinedValue;
            case "qol.powder_chest.hud" -> config.powderChestHudEnabled;
            case "qol.powder_chest.hud_background" -> config.powderChestHudShowBackground;
            default -> null;
        };
    }

    static boolean writeBoolean(TrackerConfig config, String settingId, boolean value) {
        if (config == null || settingId == null) {
            return false;
        }
        switch (settingId) {
            case "qol.mining_tracker.show_blocks" -> config.showBlocks = value;
            case "qol.mining_tracker.show_raw" -> config.showRawMaterial = value;
            case "qol.mining_tracker.show_enchanted" -> config.showEnchantedMaterial = value;
            case "qol.mining_tracker.show_session_profit" -> config.showSessionProfit = value;
            case "qol.mining_tracker.show_unsold" -> config.showUnsoldValue = value;
            case "qol.mining_tracker.show_cph" -> config.showCoinsPerHour = value;
            case "qol.mining_tracker.show_mph" -> config.showMaterialPerHour = value;
            case "qol.mining_tracker.show_session_time" -> config.showSessionTime = value;
            case "qol.mining_tracker.show_tool" -> config.showActiveTool = value;
            case "qol.mining_tracker.show_area" -> config.showArea = value;
            case "qol.mining_tracker.show_graph" -> config.showRateGraph = value;
            case "qol.mining_tracker.show_fortune" -> config.showDropAndFortune = value;
            case "qol.mining_tracker.show_bazaar" -> config.showBazaarPrices = value;
            case "qol.mining_tracker.show_value_panel" -> config.showValuePanel = value;
            case "qol.mining_tracker.show_hud_title" -> config.showHudTitle = value;
            case "qol.mining_tracker.show_hud_status" -> config.showHudStatus = value;
            case "qol.mining_tracker.show_hud_version" -> config.showHudVersion = value;
            case "qol.mining_tracker.show_auto_pause" -> config.showHudAutoPause = value;
            case "qol.mining_tracker.hud_background" -> config.hudShowBackground = value;
            case "qol.mining_tracker.show_target_heading" -> config.showTargetHeading = value;
            case "qol.mining_tracker.show_other_section" -> config.showOtherSection = value;
            case "qol.mining_tracker.show_target_value" -> config.showTargetValue = value;
            case "qol.mining_tracker.show_other_value" -> config.showOtherValue = value;
            case "qol.mining_tracker.show_total_mined" -> config.showTotalMinedValue = value;
            case "qol.powder_chest.hud" -> config.powderChestHudEnabled = value;
            case "qol.powder_chest.hud_background" -> config.powderChestHudShowBackground = value;
            default -> {
                return false;
            }
        }
        return true;
    }

    static boolean resetModule(TrackerConfig config, String moduleId) {
        if (config == null || moduleId == null) {
            return false;
        }
        TrackerConfig defaults = new TrackerConfig();
        if (MiningTrackerCatalogPolicy.TRACKER.equals(moduleId)) {
            config.enabled = defaults.enabled;
            config.showBlocks = defaults.showBlocks;
            config.showRawMaterial = defaults.showRawMaterial;
            config.showEnchantedMaterial = defaults.showEnchantedMaterial;
            config.showSessionProfit = defaults.showSessionProfit;
            config.showUnsoldValue = defaults.showUnsoldValue;
            config.showCoinsPerHour = defaults.showCoinsPerHour;
            config.showMaterialPerHour = defaults.showMaterialPerHour;
            config.showSessionTime = defaults.showSessionTime;
            config.showActiveTool = defaults.showActiveTool;
            config.showArea = defaults.showArea;
            config.showRateGraph = defaults.showRateGraph;
            config.showDropAndFortune = defaults.showDropAndFortune;
            config.showBazaarPrices = defaults.showBazaarPrices;
            config.showValuePanel = defaults.showValuePanel;
            config.showHudTitle = defaults.showHudTitle;
            config.showHudStatus = defaults.showHudStatus;
            config.showHudVersion = defaults.showHudVersion;
            config.showHudAutoPause = defaults.showHudAutoPause;
            config.hudShowBackground = defaults.hudShowBackground;
            config.showTargetHeading = defaults.showTargetHeading;
            config.showOtherSection = defaults.showOtherSection;
            config.showTargetValue = defaults.showTargetValue;
            config.showOtherValue = defaults.showOtherValue;
            config.showTotalMinedValue = defaults.showTotalMinedValue;
            return true;
        }
        if (MiningTrackerCatalogPolicy.POWDER.equals(moduleId)) {
            config.powderChestTrackerEnabled = defaults.powderChestTrackerEnabled;
            config.powderChestHudEnabled = defaults.powderChestHudEnabled;
            config.powderChestHudShowBackground = defaults.powderChestHudShowBackground;
            return true;
        }
        return false;
    }
}
