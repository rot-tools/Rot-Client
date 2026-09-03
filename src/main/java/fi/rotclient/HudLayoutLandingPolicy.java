package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HUD Elements Editor dashboard page: categorized overlay toggles plus Open editor.
 * Minecraft-free so draw and click share one list.
 */
public final class HudLayoutLandingPolicy {
    public static final String TITLE = "HUD Elements Editor";
    public static final String SUBTITLE =
            "Turn overlays on or off, open a HUD's settings, or edit placement.";
    public static final int BACK_HEIGHT = 22;
    public static final int EDITOR_TOP = 28;
    public static final int EDITOR_HEIGHT = 28;
    public static final int SECTION_LABEL_HEIGHT = 18;
    public static final int ROW_HEIGHT = 48;
    public static final int ROW_GAP = 6;
    public static final int SECTION_GAP = 12;
    public static final int TOGGLE_WIDTH = QolUtilityUiMath.TOGGLE_WIDTH;
    public static final int SETTINGS_WIDTH = 88;

    public record Row(
            String settingId,
            String label,
            String description,
            boolean moduleToggle,
            String settingsModuleId,
            String category) {
        public Row {
            settingId = settingId == null ? "" : settingId;
            label = label == null ? "" : label;
            description = description == null ? "" : description;
            settingsModuleId = settingsModuleId == null ? "" : settingsModuleId;
            category = category == null || category.isBlank() ? "Other" : category;
        }

        public boolean hasSettings() {
            return !settingsModuleId.isBlank();
        }
    }

    public record Section(String title, List<Row> rows) {
        public Section {
            title = title == null ? "" : title;
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }

    public record Disable(String settingId, boolean moduleToggle) {
    }

    public enum Action {
        NONE,
        BACK,
        OPEN_EDITOR,
        TOGGLE,
        SETTINGS
    }

    public record Hit(Action action, int rowIndex) {
        static final Hit NONE = new Hit(Action.NONE, -1);
    }

    private HudLayoutLandingPolicy() {
    }

    public static List<Section> sections() {
        Map<String, List<Row>> grouped = new LinkedHashMap<>();
        grouped.put("Vanilla / Hypixel", new ArrayList<>());
        grouped.put("Player", new ArrayList<>());
        grouped.put("Combat", new ArrayList<>());
        grouped.put("Mining", new ArrayList<>());
        grouped.put("Fishing", new ArrayList<>());
        grouped.put("Slayer", new ArrayList<>());
        grouped.put("Dungeons", new ArrayList<>());
        grouped.put("Kuudra", new ArrayList<>());
        grouped.put("Events", new ArrayList<>());
        grouped.put("Foraging", new ArrayList<>());
        grouped.put("Other", new ArrayList<>());
        for (HudLayerCatalog.Layer layer : HudLayerCatalog.vanillaLayers()) {
            grouped.get("Vanilla / Hypixel").add(rowFromLayer(layer));
        }
        addExtra(grouped, extra(
                MiningTrackerCatalogPolicy.TRACKER,
                "Mining Tracker HUD",
                "Live mining overlay. Settings match Modules → Mining Tracker.",
                true,
                MiningTrackerCatalogPolicy.TRACKER,
                "Mining"));
        addExtra(grouped, extra(
                MiningTrackerCatalogPolicy.POWDER_HUD,
                "Powder Chest HUD",
                "Crystal Hollows chest overlay. Independent of the ore tracker.",
                false,
                MiningTrackerCatalogPolicy.POWDER,
                "Mining"));
        addExtra(grouped, extra(
                "qol.wardrobe_keybinds",
                "Wardrobe Equipping",
                "Shows Equipping [slot] while a wardrobe set is applied.",
                true,
                "qol.wardrobe_keybinds",
                "Player"));
        addExtra(grouped, extra(
                "qol.slayer_carry",
                "Slayer Carry Display",
                "Active carry progress. Settings match Modules → Slayer Carry.",
                true,
                "qol.slayer_carry",
                "Slayer"));
        addExtra(grouped, extra(
                "qol.slayer_cocoon_alert",
                "Cocoon Timer",
                "Tarantula cocoon countdown.",
                true,
                "qol.slayer_cocoon_alert",
                "Slayer"));
        addExtra(grouped, extra(
                "qol.slayer_attunement_display",
                "Attunement Display",
                "Blaze attunement HUD.",
                true,
                "qol.slayer_attunement_display",
                "Slayer"));
        addExtra(grouped, extra(
                "qol.slayer_vengeance",
                "Vengeance Timer",
                "Vengeance duration HUD.",
                true,
                "qol.slayer_vengeance",
                "Slayer"));
        addExtra(grouped, extra(
                "qol.iota.arrow_tracker",
                "Arrow Tracker",
                "Quiver type and remaining arrows.",
                false,
                "qol.iota",
                "Kuudra"));
        addExtra(grouped, extra(
                "qol.iota",
                "Kuudra Alerts",
                "Fresh Tools, party FRESH, and build info.",
                true,
                "qol.iota",
                "Kuudra"));
        addExtra(grouped, extra(
                "qol.stall_market.bin_overlay",
                "BIN Overlay",
                "BIN highlight on auction confirm screens.",
                false,
                "qol.stall_market",
                "Other"));
        for (HudLayerCatalog.Layer layer : HudLayerCatalog.rotOverlays()) {
            addExtra(grouped, rowFromLayer(layer));
        }
        List<Section> out = new ArrayList<>();
        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                out.add(new Section(entry.getKey(), entry.getValue()));
            }
        }
        return List.copyOf(out);
    }

    public static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        for (Section section : sections()) {
            out.addAll(section.rows());
        }
        return List.copyOf(out);
    }

    public static int headerHeight() {
        return EDITOR_TOP + EDITOR_HEIGHT;
    }

    public static int contentHeight() {
        int y = SECTION_GAP;
        for (Section section : sections()) {
            y += SECTION_LABEL_HEIGHT;
            y += section.rows().size() * (ROW_HEIGHT + ROW_GAP);
            y += 8;
        }
        return y;
    }

    public static Hit hit(
            int mouseX,
            int mouseY,
            int contentLeft,
            int listTop,
            int width,
            int scroll) {
        if (AppearanceLandingPolicy.hitBack(mouseX, mouseY, contentLeft, listTop)) {
            return new Hit(Action.BACK, -1);
        }
        int editorY = listTop + EDITOR_TOP;
        if (mouseX >= contentLeft
                && mouseX < contentLeft + Math.min(200, Math.max(1, width))
                && mouseY >= editorY
                && mouseY < editorY + EDITOR_HEIGHT) {
            return new Hit(Action.OPEN_EDITOR, -1);
        }
        int y = listTop + headerHeight() + SECTION_GAP - scroll;
        int index = 0;
        int safeWidth = Math.max(1, width);
        for (Section section : sections()) {
            y += SECTION_LABEL_HEIGHT;
            for (Row row : section.rows()) {
                int rowY = y;
                if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                        && mouseX >= contentLeft
                        && mouseX < contentLeft + safeWidth) {
                    int settingsX = contentLeft + safeWidth
                            - TOGGLE_WIDTH - 12 - (row.hasSettings() ? SETTINGS_WIDTH + 8 : 0);
                    int toggleX = contentLeft + safeWidth - TOGGLE_WIDTH - 12;
                    if (row.hasSettings()
                            && mouseX >= settingsX
                            && mouseX < settingsX + SETTINGS_WIDTH) {
                        return new Hit(Action.SETTINGS, index);
                    }
                    if (mouseX >= toggleX) {
                        return new Hit(Action.TOGGLE, index);
                    }
                    return new Hit(Action.TOGGLE, index);
                }
                y += ROW_HEIGHT + ROW_GAP;
                index++;
            }
            y += 8;
        }
        return Hit.NONE;
    }

    public static Disable disableForPose(String poseId) {
        String id = poseId == null ? "" : poseId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "performance" -> new Disable("qol.performance_hud", true);
            case "health" -> new Disable("qol.player_display.health_hud", false);
            case "mana" -> new Disable("qol.player_display.mana_hud", false);
            case "overflow" -> new Disable("qol.player_display.overflow_mana_hud", false);
            case "defense" -> new Disable("qol.player_display.defense_hud", false);
            case "vitality" -> new Disable("qol.player_display.vitality_hud", false);
            case "ehp" -> new Disable("qol.player_display.ehp_hud", false);
            case "speed" -> new Disable("qol.player_display.speed_hud", false);
            case "pet" -> new Disable("qol.pet_hud", true);
            case "commission" -> new Disable("qol.commission_display", true);
            case "auto_clicker" -> new Disable("qol.auto_clicker.cps_hud", false);
            case "fishing" -> new Disable("qol.fishing_helper.hook_timer_hud", false);
            case "mining" -> new Disable("qol.mining_helpers.ability_hud", false);
            case "dungeon" -> new Disable("qol.dungeon_hud", true);
            case "slayer" -> new Disable("qol.slayer_display", true);
            case "slayer_progress" -> new Disable("qol.slayer_progress", true);
            case "slayer_rng" -> new Disable("qol.slayer_drops.rng_hud", false);
            case "slayer_profit" -> new Disable("qol.slayer_drops.profit_hud", false);
            case "slayer_stats" -> new Disable("qol.slayer_stats", true);
            case "diana" -> new Disable("qol.diana_burrows", true);
            case "foraging" -> new Disable("qol.foraging_trees.progress_hud", false);
            case "wardrobe" -> new Disable("qol.wardrobe_keybinds", true);
            case "slayer_carry" -> new Disable("qol.slayer_carry", true);
            case "slayer_cocoon" -> new Disable("qol.slayer_cocoon_alert", true);
            case "slayer_attunement" -> new Disable("qol.slayer_attunement_display", true);
            case "slayer_vengeance" -> new Disable("qol.slayer_vengeance", true);
            case "iota_arrows" -> new Disable("qol.iota.arrow_tracker", false);
            case "kuudra_alerts" -> new Disable("qol.iota", true);
            case "stall_bin" -> new Disable("qol.stall_market.bin_overlay", false);
            case "mining_tracker" -> new Disable(MiningTrackerCatalogPolicy.TRACKER, true);
            case "powder_chest" -> new Disable(MiningTrackerCatalogPolicy.POWDER_HUD, false);
            default -> null;
        };
    }

    private static void addExtra(Map<String, List<Row>> grouped, Row row) {
        grouped.computeIfAbsent(row.category(), ignored -> new ArrayList<>()).add(row);
    }

    private static Row rowFromLayer(HudLayerCatalog.Layer layer) {
        String category = categoryFor(layer.settingId());
        String settingsModule = settingsModuleId(layer.settingId(), layer.moduleToggle());
        return new Row(
                layer.settingId(),
                layer.label(),
                descriptionFor(layer.settingId(), settingsModule, layer.moduleToggle()),
                layer.moduleToggle(),
                settingsModule,
                category);
    }

    private static Row extra(
            String settingId,
            String label,
            String description,
            boolean moduleToggle,
            String settingsModuleId,
            String category) {
        return new Row(settingId, label, description, moduleToggle, settingsModuleId, category);
    }

    static String categoryFor(String settingId) {
        String id = settingId == null ? "" : settingId.toLowerCase(Locale.ROOT);
        if (id.startsWith("qol.hud_layout.hide_")) {
            return "Vanilla / Hypixel";
        }
        if (id.contains("auto_clicker")) {
            return "Combat";
        }
        if (id.contains("mining") || id.contains("powder") || id.contains("commission")) {
            return "Mining";
        }
        if (id.contains("fishing")) {
            return "Fishing";
        }
        if (id.contains("slayer")) {
            return "Slayer";
        }
        if (id.contains("dungeon")) {
            return "Dungeons";
        }
        if (id.contains("iota") || id.contains("kuudra")) {
            return "Kuudra";
        }
        if (id.contains("diana")) {
            return "Events";
        }
        if (id.contains("foraging")) {
            return "Foraging";
        }
        if (id.contains("performance") || id.contains("player_display")
                || id.contains("pet_hud") || id.contains("wardrobe")) {
            return "Player";
        }
        if (id.contains("stall")) {
            return "Other";
        }
        return "Other";
    }

    public static boolean hide(QolUtilityConfig qol, String settingId, boolean moduleToggle) {
        boolean changed = HudLayerTogglePolicy.disable(qol, settingId, moduleToggle);
        if ("qol.wardrobe_keybinds".equals(settingId)
                && HudLayerTogglePolicy.disable(qol, "qol.cheater_wardrobe", true)) {
            changed = true;
        }
        return changed;
    }

    static String settingsModuleId(String settingId, boolean moduleToggle) {
        if (settingId == null || settingId.startsWith("qol.hud_layout.hide_")) {
            return "";
        }
        if (moduleToggle) {
            return settingId;
        }
        QolUtilityCatalog.ModuleDef best = null;
        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if (settingId.startsWith(module.id() + ".")
                    && (best == null || module.id().length() > best.id().length())) {
                best = module;
            }
        }
        return best == null ? "" : best.id();
    }

    static String descriptionFor(String settingId, String settingsModuleId, boolean moduleToggle) {
        if (!settingsModuleId.isBlank()) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(settingsModuleId);
            if (module != null) {
                for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                    if (setting.id().equals(settingId) && !setting.description().isBlank()) {
                        return setting.description();
                    }
                }
                if (moduleToggle && !module.description().isBlank()) {
                    return module.description();
                }
            }
        }
        QolUtilityCatalog.ModuleDef layout = QolUtilityCatalog.findById(
                MiningTrackerCatalogPolicy.HUD_LAYOUT);
        if (layout != null) {
            for (QolUtilityCatalog.SettingDef setting : layout.settings()) {
                if (setting.id().equals(settingId) && !setting.description().isBlank()) {
                    return setting.description();
                }
            }
        }
        return "Show or hide this overlay.";
    }
}
