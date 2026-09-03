package fi.rotclient;

import java.util.Locale;

/**
 * Catalog ids and HUD-drawer classification for Mining Tracker surfaces.
 * Field storage lives on {@code TrackerConfig}; this class stays Minecraft-free.
 */
public final class MiningTrackerCatalogPolicy {
    public static final String TRACKER = "qol.mining_tracker";
    public static final String POWDER = "qol.powder_chest";
    public static final String SESSION = "qol.mining_session";
    public static final String HISTORY = "qol.mining_history";
    public static final String APPEARANCE = "qol.appearance";
    public static final String HUD_LAYOUT = "qol.hud_layout";

    public static final String TRACKER_OPEN_PAGE = "qol.mining_tracker.open_page";
    public static final String TRACKER_HUD_EDITOR = "qol.mining_tracker.open_hud_editor";
    public static final String POWDER_OPEN_PAGE = "qol.powder_chest.open_page";
    public static final String POWDER_HUD = "qol.powder_chest.hud";
    public static final String POWDER_HUD_EDITOR = "qol.powder_chest.open_hud_editor";
    public static final String SESSION_OPEN_PAGE = "qol.mining_session.open_page";
    public static final String HISTORY_OPEN_PAGE = "qol.mining_history.open_page";

    private MiningTrackerCatalogPolicy() {
    }

    public static boolean isHudContentSetting(String settingId) {
        String id = normalize(settingId);
        return id.startsWith("qol.mining_tracker.show_")
                || id.equals("qol.mining_tracker.hud_background")
                || id.equals(POWDER_HUD)
                || id.equals("qol.powder_chest.hud_background");
    }

    public static boolean isTrackerOwnedSetting(String settingId) {
        String id = normalize(settingId);
        return id.startsWith("qol.mining_tracker.")
                || id.startsWith("qol.powder_chest.");
    }

    public static boolean usesQolHudStyle(String focusId) {
        String id = normalize(focusId);
        return !id.isBlank()
                && !"mining_tracker".equals(id)
                && !"powder_chest".equals(id);
    }

    public static boolean isOpenPageAction(String settingId) {
        String id = normalize(settingId);
        return TRACKER_OPEN_PAGE.equals(id)
                || POWDER_OPEN_PAGE.equals(id)
                || SESSION_OPEN_PAGE.equals(id)
                || HISTORY_OPEN_PAGE.equals(id);
    }

    public static String catalogIdForLegacyHudToggle(String legacyId) {
        if (legacyId == null || legacyId.isBlank()) {
            return "";
        }
        return switch (legacyId.trim()) {
            case "showBlocks" -> "qol.mining_tracker.show_blocks";
            case "showRawMaterial" -> "qol.mining_tracker.show_raw";
            case "showEnchantedMaterial" -> "qol.mining_tracker.show_enchanted";
            case "showSessionProfit" -> "qol.mining_tracker.show_session_profit";
            case "showUnsoldValue" -> "qol.mining_tracker.show_unsold";
            case "showCoinsPerHour" -> "qol.mining_tracker.show_cph";
            case "showMaterialPerHour" -> "qol.mining_tracker.show_mph";
            case "showSessionTime" -> "qol.mining_tracker.show_session_time";
            case "showActiveTool" -> "qol.mining_tracker.show_tool";
            case "showArea" -> "qol.mining_tracker.show_area";
            case "showRateGraph" -> "qol.mining_tracker.show_graph";
            case "showDropAndFortune" -> "qol.mining_tracker.show_fortune";
            case "showBazaarPrices" -> "qol.mining_tracker.show_bazaar";
            case "showValuePanel" -> "qol.mining_tracker.show_value_panel";
            case "showHudTitle" -> "qol.mining_tracker.show_hud_title";
            case "showHudStatus" -> "qol.mining_tracker.show_hud_status";
            case "showHudVersion" -> "qol.mining_tracker.show_hud_version";
            case "showHudAutoPause" -> "qol.mining_tracker.show_auto_pause";
            case "hudShowBackground" -> "qol.mining_tracker.hud_background";
            case "showTargetHeading" -> "qol.mining_tracker.show_target_heading";
            case "showOtherSection" -> "qol.mining_tracker.show_other_section";
            case "showTargetValue" -> "qol.mining_tracker.show_target_value";
            case "showOtherValue" -> "qol.mining_tracker.show_other_value";
            case "showTotalMinedValue" -> "qol.mining_tracker.show_total_mined";
            default -> "";
        };
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
