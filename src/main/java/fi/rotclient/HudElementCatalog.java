package fi.rotclient;

import java.util.List;
import java.util.Locale;

/**
 * Maps dashboard HUD-editor actions onto overlay pose ids used by
 * {@code QolOverlayHud} / {@code QolUtilityConfig.pose}.
 */
public final class HudElementCatalog {
    private HudElementCatalog() {
    }

    public static String focusIdForHudEditorSetting(String settingId) {
        if (settingId == null || settingId.isBlank()) {
            return "";
        }
        String id = settingId.trim().toLowerCase(Locale.ROOT);
        if (!id.endsWith("_hud_editor") && !id.endsWith(".open_hud_editor")) {
            return "";
        }
        if (id.contains("player_display") || id.contains("open_rng") || id.contains("open_profit")) {
            if (id.contains("open_rng")) {
                return "slayer_rng";
            }
            if (id.contains("open_profit")) {
                return "slayer_profit";
            }
            return "health";
        }
        if (id.contains("performance")) {
            return "performance";
        }
        if (id.contains("pet_hud")) {
            return "pet";
        }
        if (id.contains("commission")) {
            return "commission";
        }
        if (id.contains("wardrobe")) {
            return "wardrobe";
        }
        if (id.contains("auto_clicker")) {
            return "auto_clicker";
        }
        if (id.contains("fishing")) {
            return "fishing";
        }
        if (id.contains("foraging")) {
            return "foraging";
        }
        if (id.contains("mining") || id.contains("world_scanner") || id.contains("scatha")
                || id.contains("glacite") || id.contains("hotm")) {
            return "mining";
        }
        if (id.contains("diana")) {
            return "diana";
        }
        if (id.contains("dungeon")) {
            return "dungeon";
        }
        if (id.contains("slayer_progress")) {
            return "slayer_progress";
        }
        if (id.contains("slayer_stats")) {
            return "slayer_stats";
        }
        if (id.contains("slayer_carry")) {
            return "slayer_carry";
        }
        if (id.contains("slayer_cocoon")) {
            return "slayer_cocoon";
        }
        if (id.contains("slayer_attunement")) {
            return "slayer_attunement";
        }
        if (id.contains("slayer_vengeance")) {
            return "slayer_vengeance";
        }
        if (id.contains("slayer_display") || id.contains("slayer_drops")) {
            return "slayer";
        }
        if (id.contains("click_gui") || id.contains("hud_layout")) {
            return "";
        }
        return "";
    }

    public static String explainedDescription(QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return "";
        }
        String base = module.description() == null ? "" : module.description().trim();
        base = base.replace("The card switch is this module", "The Module switch starts this feature")
                .replace("the card switch", "the Module switch")
                .replace("card switch", "Module switch");
        if (moduleHasHudEditor(module) || moduleHasHudToggle(module)) {
            return base
                    + " Use Module to start the feature. HUD switches only the overlays. Settings has every option, including Edit HUD.";
        }
        return base + " Use Module to start or stop this feature. Open Settings for every option.";
    }

    public static String explainedSetting(QolUtilityCatalog.SettingDef setting) {
        if (setting == null) {
            return "";
        }
        String label = setting.label() == null ? "" : setting.label().trim();
        String detail = setting.description() == null ? "" : setting.description().trim();
        String typeHint = switch (setting.type()) {
            case TOGGLE -> "On/off switch.";
            case NUMBER -> "Number. Drag the slider or click the stepper.";
            case COLOR -> "Opens the RGB color picker, including alpha.";
            case KEYBIND -> "Click, then press a key. Blank means unbound.";
            case TEXT -> "Click to type a custom string.";
            case ENUM -> "Click to pick one of the listed options.";
            case ACTION -> "Click the button to run this action.";
            case SECTION -> "Click to collapse or expand this group.";
        };
        if (detail.isBlank()) {
            return label.isBlank() ? typeHint : label + " — " + typeHint;
        }
        if (detail.length() >= 48) {
            return label.isBlank() ? detail : label + " — " + detail;
        }
        return (label.isBlank() ? "" : label + " — ") + detail + " " + typeHint;
    }

    public record InspectorToggle(String settingId, String label) {
    }

    public static List<InspectorToggle> inspectorToggles(String poseId) {
        String id = poseId == null ? "" : poseId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "performance" -> List.of(
                    new InspectorToggle("qol.performance_hud.show_fps", "FPS text"),
                    new InspectorToggle("qol.performance_hud.show_tps", "TPS text"),
                    new InspectorToggle("qol.performance_hud.show_ping", "Ping text"));
            case "health" -> List.of(
                    new InspectorToggle("qol.player_display.health_hud", "Health HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"),
                    new InspectorToggle("qol.player_display.show_max", "Current / max"));
            case "mana" -> List.of(
                    new InspectorToggle("qol.player_display.mana_hud", "Mana HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"),
                    new InspectorToggle("qol.player_display.show_max", "Current / max"));
            case "overflow" -> List.of(
                    new InspectorToggle("qol.player_display.overflow_mana_hud", "Overflow HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"));
            case "defense" -> List.of(
                    new InspectorToggle("qol.player_display.defense_hud", "Defense HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"));
            case "vitality" -> List.of(
                    new InspectorToggle("qol.player_display.vitality_hud", "Vitality HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"));
            case "ehp" -> List.of(
                    new InspectorToggle("qol.player_display.ehp_hud", "EHP HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"));
            case "speed" -> List.of(
                    new InspectorToggle("qol.player_display.speed_hud", "Speed HUD"),
                    new InspectorToggle("qol.player_display.show_icons", "Icons"),
                    new InspectorToggle("qol.player_display.show_labels", "Name text"));
            case "auto_clicker" -> List.of(
                    new InspectorToggle("qol.auto_clicker.cps_hud", "CPS HUD"));
            case "fishing" -> List.of(
                    new InspectorToggle("qol.fishing_helper.hook_timer_hud", "Hook timer"),
                    new InspectorToggle("qol.fishing_creatures.hud", "Creature HUD"),
                    new InspectorToggle("qol.fishing_tools.bait_hud", "Bait HUD"));
            case "dungeon" -> List.of(
                    new InspectorToggle("qol.dungeon_hud.floor", "Floor text"),
                    new InspectorToggle("qol.dungeon_hud.class", "Class text"),
                    new InspectorToggle("qol.dungeon_hud.secrets", "Secrets text"),
                    new InspectorToggle("qol.dungeon_hud.score", "Score text"),
                    new InspectorToggle("qol.dungeon_hud.map", "Dungeon map"));
            case "mining" -> List.of(
                    new InspectorToggle("qol.mining_helpers.metal_distance", "Treasure meters"),
                    new InspectorToggle("qol.mining_helpers.ability_hud", "Ability HUD"),
                    new InspectorToggle("qol.mining_helpers.drill_fuel", "Drill fuel"),
                    new InspectorToggle("qol.mining_scatha.hud", "Scatha HUD"),
                    new InspectorToggle("qol.mining_events.hud", "Event HUD"),
                    new InspectorToggle("qol.mining_glacite.pity_hud", "Pity HUD"));
            default -> List.of();
        };
    }

    public record HudPiece(
            String toggleId,
            String editorId,
            String label,
            String focusId) {
        public boolean hasToggle() {
            return toggleId != null && !toggleId.isBlank();
        }

        public boolean hasEditor() {
            return editorId != null && !editorId.isBlank();
        }
    }

    public static List<HudPiece> hudPieces(QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return List.of();
        }
        java.util.LinkedHashMap<String, HudPiece> byKey = new java.util.LinkedHashMap<>();
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (setting.type() == QolUtilityCatalog.SettingType.ACTION
                    && setting.id().endsWith("_hud_editor")) {
                String focus = focusIdForHudEditorSetting(setting.id());
                String key = focus.isBlank() ? setting.id() : "editor:" + focus;
                HudPiece prev = byKey.get(key);
                String label = hudEditorLabel(setting.label());
                byKey.put(key, new HudPiece(
                        prev == null ? "" : prev.toggleId(),
                        setting.id(),
                        label,
                        focus));
            } else if (isHudVisibilityToggle(setting)) {
                String key = "toggle:" + setting.id();
                HudPiece prev = byKey.get(key);
                byKey.put(key, new HudPiece(
                        setting.id(),
                        prev == null ? "" : prev.editorId(),
                        setting.label() == null || setting.label().isBlank()
                                ? "HUD"
                                : setting.label(),
                        prev == null ? "" : prev.focusId()));
            }
        }
        return List.copyOf(byKey.values());
    }

    public static boolean hudControlOpensMenu(QolUtilityCatalog.ModuleDef module) {
        return hudPieces(module).size() > 1;
    }

    public static boolean moduleShowsHudControl(QolUtilityCatalog.ModuleDef module) {
        return !hudPieces(module).isEmpty();
    }

    private static String hudEditorLabel(String raw) {
        String label = raw == null ? "" : raw.trim();
        label = label.replace("Edit HUD Layout", "HUD")
                .replace("Edit ", "")
                .replace(" Layout", "")
                .trim();
        return label.isBlank() ? "HUD" : label;
    }

    public static boolean isHudVisibilityToggle(QolUtilityCatalog.SettingDef setting) {
        if (setting == null || setting.type() != QolUtilityCatalog.SettingType.TOGGLE) {
            return false;
        }
        String id = setting.id() == null ? "" : setting.id();
        return id.endsWith(".hud")
                || id.endsWith("_hud")
                || id.contains(".cps_hud")
                || id.contains("progress_hud")
                || id.contains("pity_hud")
                || id.contains("show_fps")
                || id.contains("show_tps")
                || id.contains("show_ping")
                || id.contains("show_icons")
                || id.contains("show_labels")
                || id.contains("show_max")
                || id.contains("health_hud")
                || id.contains("mana_hud")
                || id.contains("overflow_mana_hud")
                || id.contains("defense_hud")
                || id.contains("vitality_hud")
                || id.contains("ehp_hud")
                || id.contains("speed_hud")
                || id.contains("_hud.")
                || id.contains("hud_layout.hide_");
    }

    public static boolean moduleHasHudEditor(QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return false;
        }
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (setting.id().endsWith("_hud_editor")) {
                return true;
            }
        }
        return false;
    }

    public static boolean moduleHasHudToggle(QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return false;
        }
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (isHudVisibilityToggle(setting)) {
                return true;
            }
        }
        return false;
    }
}
