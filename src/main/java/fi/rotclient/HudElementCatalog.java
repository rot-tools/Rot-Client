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
        if (id.contains("mining_tracker")) {
            return "mining_tracker";
        }
        if (id.contains("powder_chest")) {
            return "powder_chest";
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
        if (id.contains("dungeon_carry")) {
            return "dungeon_carry";
        }
        if (id.contains("dungeon_watcher")) {
            return "dungeon_watcher";
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
        if (id.contains("custom_scoreboard")) {
            return "custom_scoreboard";
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
                    + " Use Module to start the feature. HUD opens this overlay's settings. Settings has the rest of the options.";
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
            case SQUARE -> "Square latch. Off is idle; on fills red.";
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
                    new InspectorToggle("qol.performance_hud.show_ping", "Ping text"),
                    new InspectorToggle("qol.performance_hud.show_background", "Background"));
            case "slayer" -> List.of(
                    new InspectorToggle("qol.slayer_display.kill_time", "Kill time"));
            case "slayer_progress" -> List.of(
                    new InspectorToggle("qol.slayer_progress.show_remaining", "Remaining XP"));
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
                    new InspectorToggle("qol.dungeon_hud.cleared", "Cleared"),
                    new InspectorToggle("qol.dungeon_hud.invincibility", "Invincibility"),
                    new InspectorToggle("qol.dungeon_hud.terracotta", "Terracotta"),
                    new InspectorToggle("qol.dungeon_hud.blessings", "Blessings"),
                    new InspectorToggle("qol.dungeon_hud.ragnarock", "Ragnarock"),
                    new InspectorToggle("qol.dungeon_hud.melody", "Melody"),
                    new InspectorToggle("qol.dungeon_hud.quiz", "Quiz / Weirdos"),
                    new InspectorToggle("qol.dungeon_hud.score_overlay", "Score overlay"),
                    new InspectorToggle("qol.dungeon_hud.map", "Dungeon map"),
                    new InspectorToggle("qol.dungeon_hud.map_mode", "Map mode"),
                    new InspectorToggle("qol.dungeon_hud.map_doors", "Map doors"),
                    new InspectorToggle("qol.dungeon_hud.map_players", "Map players"),
                    new InspectorToggle("qol.dungeon_hud.room_names", "Room names"),
                    new InspectorToggle("qol.dungeon_hud.room_secrets", "Room secrets"),
                    new InspectorToggle("qol.dungeon_hud.player_names", "Player names"),
                    new InspectorToggle("qol.dungeon_hud.head_markers", "Head markers"),
                    new InspectorToggle("qol.dungeon_hud.class_icons", "Class icons"),
                    new InspectorToggle("qol.dungeon_hud.map_extra", "Map status bar"),
                    new InspectorToggle("qol.dungeon_hud.crypts", "Crypts"),
                    new InspectorToggle("qol.dungeon_hud.deaths", "Deaths"),
                    new InspectorToggle("qol.dungeon_hud.map_mimic", "Mimic chip"),
                    new InspectorToggle("qol.dungeon_hud.map_puzzles", "Puzzle chip"),
                    new InspectorToggle("qol.dungeon_hud.map_hide_boss", "Hide in boss"),
                    new InspectorToggle("qol.dungeon_hud.map_scale", "Map size"),
                    new InspectorToggle("qol.dungeon_hud.f7_timers", "F7 timers"),
                    new InspectorToggle("qol.dungeon_hud.puzzle_timer", "Puzzle timer"),
                    new InspectorToggle("qol.dungeon_hud.run_timers", "Run timers"),
                    new InspectorToggle("qol.dungeon_hud.show_split_pbs", "Split PBs"),
                    new InspectorToggle("qol.dungeon_hud.kuudra_splits", "Kuudra splits"));
            case "mining" -> List.of(
                    new InspectorToggle("qol.mining_helpers.metal_distance", "Treasure meters"),
                    new InspectorToggle("qol.mining_helpers.ability_hud", "Ability HUD"),
                    new InspectorToggle("qol.mining_helpers.drill_fuel", "Drill fuel"),
                    new InspectorToggle("qol.mining_scatha.hud", "Scatha HUD"),
                    new InspectorToggle("qol.mining_events.hud", "Event HUD"),
                    new InspectorToggle("qol.mining_glacite.pity_hud", "Pity HUD"));
            case "stall_bin" -> List.of(
                    new InspectorToggle("qol.stall_market.bin_overlay", "BIN overlay"));
            case "custom_scoreboard" -> List.of(
                    new InspectorToggle("qol.custom_scoreboard.hide_vanilla", "Hide vanilla"),
                    new InspectorToggle("qol.custom_scoreboard.bg_enabled", "Background"),
                    new InspectorToggle("qol.custom_scoreboard.outline", "Outline"));
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
        mergeLoneToggleWithEditor(byKey);
        return List.copyOf(byKey.values());
    }

    private static void mergeLoneToggleWithEditor(
            java.util.LinkedHashMap<String, HudPiece> byKey) {
        HudPiece editorPiece = null;
        String editorKey = "";
        HudPiece togglePiece = null;
        String toggleKey = "";
        int loneToggles = 0;
        for (var entry : byKey.entrySet()) {
            HudPiece piece = entry.getValue();
            if (piece.hasEditor() && !piece.hasToggle()) {
                editorPiece = piece;
                editorKey = entry.getKey();
            } else if (piece.hasToggle() && !piece.hasEditor()) {
                loneToggles++;
                togglePiece = piece;
                toggleKey = entry.getKey();
            }
        }
        if (editorPiece == null || togglePiece == null || loneToggles != 1) {
            return;
        }
        byKey.remove(toggleKey);
        byKey.put(editorKey, new HudPiece(
                togglePiece.toggleId(),
                editorPiece.editorId(),
                togglePiece.label(),
                editorPiece.focusId()));
    }

    public static boolean hudControlOpensMenu(QolUtilityCatalog.ModuleDef module) {
        return hudPieces(module).size() > 1;
    }

    public static boolean moduleShowsHudControl(QolUtilityCatalog.ModuleDef module) {
        return !hudPieces(module).isEmpty();
    }

    private static String hudEditorLabel(String raw) {
        String label = raw == null ? "" : raw.trim();
        label = label.replace("Open HUD Elements Editor", "HUD")
                .replace("Edit HUD Layout", "HUD")
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

    public static List<QolUtilityCatalog.SettingDef> hudContentSettings(
            QolUtilityCatalog.ModuleDef module,
            HudPiece piece) {
        return HudDrawerPolicy.hudCatalogSettings(module, piece);
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
