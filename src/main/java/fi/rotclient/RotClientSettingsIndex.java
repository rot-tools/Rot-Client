package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Searchable Client UI settings catalog. Pure read-model — no Minecraft /
 * disk I/O. Catalog is built once and cached; search never rebuilds it.
 */
final class RotClientSettingsIndex {
    enum Destination {
        MINING_TRACKER,
        QOL_SETTINGS,
        APPEARANCE,
        HUD_EDITOR,
        SESSION_ANALYTICS,
        SESSION_HISTORY,
        OVERVIEW
    }

    /**
     * @param id stable deep-link id (also used for HUD toggle highlight)
     * @param label user-facing primary label
     * @param description concise one-line help
     * @param category coarse group for result chrome
     * @param destination where activating the result should navigate
     * @param aliases extra search terms (never shown as the primary label)
     */
    record Entry(
            String id,
            String label,
            String description,
            String category,
            Destination destination,
            List<String> aliases) {
        Entry {
            id = id == null ? "" : id.trim();
            label = label == null ? "" : label.trim();
            description = description == null ? "" : description.trim();
            category = category == null ? "" : category.trim();
            destination = destination == null ? Destination.OVERVIEW : destination;
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }

        boolean matches(String query) {
            String needle = normalizeQuery(query);
            if (needle.isEmpty()) {
                return false;
            }
            for (String token : needle.split(" ")) {
                if (token.isEmpty()) {
                    continue;
                }
                if (!matchesToken(token)) {
                    return false;
                }
            }
            return true;
        }

        private boolean matchesToken(String token) {
            if (containsNormalized(id, token)
                    || containsNormalized(label, token)
                    || containsNormalized(description, token)
                    || containsNormalized(category, token)) {
                return true;
            }
            for (String alias : aliases) {
                if (containsNormalized(alias, token)) {
                    return true;
                }
            }
            return false;
        }

        int rank(String query) {
            String needle = normalizeQuery(query);
            if (needle.isEmpty()) {
                return Integer.MAX_VALUE;
            }
            String labelNorm = normalizeQuery(label);
            String idNorm = normalizeQuery(id);
            if (labelNorm.equals(needle) || idNorm.equals(needle)) {
                return 0;
            }
            if (labelNorm.startsWith(needle) || idNorm.startsWith(needle)) {
                return 1;
            }
            if (labelNorm.contains(needle) || idNorm.contains(needle)) {
                return 2;
            }
            if (containsNormalized(description, needle)
                    || containsNormalized(category, needle)) {
                return 3;
            }
            return 4;
        }
    }

    private static final List<Entry> CATALOG = buildCatalog();
    private static final Map<String, Entry> BY_ID = indexById(CATALOG);

    private RotClientSettingsIndex() {
    }

    /** Cached full catalog — never rebuilt per frame / per search. */
    static List<Entry> catalog() {
        return CATALOG;
    }

    static int catalogSize() {
        return CATALOG.size();
    }

    /**
     * Case-insensitive, whitespace-normalized substring match.
     * Deterministic: rank then stable catalog order. No duplicates.
     * Blank query returns empty (never dumps the full catalog).
     */
    static List<Entry> search(String query) {
        return search(query, 12);
    }

    static List<Entry> search(String query, int limit) {
        int safeLimit = Math.max(0, limit);
        String needle = normalizeQuery(query);
        if (safeLimit == 0 || needle.isEmpty()) {
            return List.of();
        }
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : CATALOG) {
            if (entry.matches(needle)) {
                matches.add(entry);
            }
        }
        matches.sort(Comparator
                .comparingInt((Entry e) -> e.rank(needle))
                .thenComparingInt(e -> CATALOG.indexOf(e)));
        if (matches.size() > safeLimit) {
            return List.copyOf(matches.subList(0, safeLimit));
        }
        return List.copyOf(matches);
    }

    static Entry findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Entry exact = BY_ID.get(id.trim());
        if (exact != null) {
            return exact;
        }
        return BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    static boolean isHudVisibilityToggle(String id) {
        return id != null && id.startsWith("show");
    }

    static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static List<Entry> buildCatalog() {
        List<Entry> entries = new ArrayList<>();

        addHud(entries, "showBlocks", "Blocks",
                "Toggle broken-block counter on the Mining HUD.",
                "blocks", "broken blocks");
        addHud(entries, "showRawMaterial", "Raw Material",
                "Toggle raw material quantity on the Mining HUD.",
                "raw", "raw items");
        addHud(entries, "showEnchantedMaterial", "Enchanted Material",
                "Toggle enchanted material quantity on the Mining HUD.",
                "enchanted", "enchanted items");
        addHud(entries, "showSessionProfit", "Est. Session Value",
                "Toggle estimated session value / profit line.",
                "session value", "profit", "price", "session");
        addHud(entries, "showUnsoldValue", "Unsold Value",
                "Toggle estimated unsold inventory value.",
                "unsold", "price");
        addHud(entries, "showCoinsPerHour", "Coins / Hour",
                "Toggle coins-per-hour estimate.",
                "cph", "coins per hour", "price");
        addHud(entries, "showMaterialPerHour", "Material / Hour",
                "Toggle material-per-hour rate.",
                "per hour", "rate");
        addHud(entries, "showSessionTime", "Session Time",
                "Toggle active session timer on the Mining HUD.",
                "timer", "duration", "session");
        addHud(entries, "showActiveTool", "Mining Tool",
                "Toggle held mining tool display.",
                "tool", "pickaxe");
        addHud(entries, "showArea", "Area / Location",
                "Toggle live SkyBlock parent and sub-area on the Mining HUD.",
                "area", "location", "zone", "sub-area", "subarea",
                "dwarven", "crystal hollows");
        addHud(entries, "showRateGraph", "Rate Graph",
                "Toggle the Mining HUD rate graph.",
                "graph", "chart");
        addHud(entries, "showDropAndFortune", "Drop + Fortune",
                "Toggle drop and Mining Fortune readout.",
                "fortune", "drop");
        addHud(entries, "showBazaarPrices", "Bazaar + Tax",
                "Toggle bazaar pricing and tax on the HUD.",
                "bazaar", "tax", "price");
        addHud(entries, "showValuePanel", "Value Panel",
                "Master toggle for the HUD value / profit card.",
                "profit panel", "price");
        addHud(entries, "showOtherSection", "Others Section",
                "Toggle the HUD OTHERS aggregate row.",
                "other", "others", "other mined", "mob", "chest");
        addHud(entries, "showTargetValue", "Target Value",
                "Toggle target mined value line.",
                "target coins", "price");
        addHud(entries, "showOtherValue", "Others Value",
                "Toggle OTHERS value line under the value panel.",
                "other value", "others", "price");
        addHud(entries, "showTotalMinedValue", "Total Mined Value",
                "Toggle combined target + others value line.",
                "total value", "price");
        addHud(entries, "showHudTitle", "HUD Title",
                "Toggle Mining HUD title chrome.",
                "title", "hud");
        addHud(entries, "showHudStatus", "Status Pill",
                "Toggle HUD status pill.",
                "status", "hud");
        addHud(entries, "showHudVersion", "Version",
                "Toggle mod version on the Mining HUD.",
                "mod version", "hud");
        addHud(entries, "showHudAutoPause", "Auto-Pause Line",
                "Toggle target auto-pause status line.",
                "auto pause", "autopause", "hud");
        addHud(entries, "showTargetHeading", "Target Heading",
                "Toggle TARGET heading above item rows.",
                "heading", "hud");

        entries.add(new Entry(
                "hud.edit_position",
                "Open HUD Elements Editor",
                "Move and scale Mining HUD, Client UI, and QoL HUD elements.",
                "Mining HUD",
                Destination.HUD_EDITOR,
                List.of("move hud", "drag hud", "layout", "scale", "position",
                        "editor", "hud", "reset")));

        entries.add(new Entry(
                "qol.fullbright",
                "Fullbright",
                "Toggle client fullbright / gamma assist.",
                "QoL",
                Destination.QOL_SETTINGS,
                List.of("brightness", "gamma", "night vision")));
        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if ("qol.fullbright".equals(module.id())) {
                continue; // already added above for stable label
            }
            List<String> moduleAliases = new ArrayList<>(module.searchAliases());
            moduleAliases.add(module.name());
            entries.add(new Entry(
                    module.id(),
                    module.name(),
                    module.description(),
                    "QoL",
                    Destination.QOL_SETTINGS,
                    moduleAliases));
            for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                    continue;
                }
                List<String> settingAliases = new ArrayList<>(setting.searchAliases());
                settingAliases.add(setting.label());
                settingAliases.add(module.name());
                entries.add(new Entry(
                        setting.id(),
                        setting.label(),
                        setting.description().isBlank()
                                ? module.name()
                                : setting.description() + " (" + module.name() + ")",
                        "QoL",
                        Destination.QOL_SETTINGS,
                        settingAliases));
            }
        }
        entries.add(new Entry(
                "appearance.open",
                "Appearance",
                "Open the Appearance submenu for dashboard, colors, background, charts, and reset.",
                "Appearance",
                Destination.QOL_SETTINGS,
                List.of("theme", "colors", "background", "customizer", "gui",
                        "appearance", "reset")));
        entries.add(new Entry(
                "appearance.reset",
                "Reset Appearance",
                "Reset Appearance colors or layout from the Reset section.",
                "Appearance",
                Destination.APPEARANCE,
                List.of("reset", "defaults", "restore")));

        entries.add(new Entry(
                "nav.mining_tracker",
                "Mining Tracker",
                "Open Mining Tracker in Modules.",
                "Navigation",
                Destination.QOL_SETTINGS,
                List.of("tracker", "target", "hud")));
        entries.add(new Entry(
                "nav.session_analytics",
                "Session Analytics",
                "Open live Current Session analytics.",
                "Navigation",
                Destination.SESSION_ANALYTICS,
                List.of("analytics", "current session", "session", "mob",
                        "chest")));
        entries.add(new Entry(
                "nav.session_history",
                "Session History",
                "Open saved local session history.",
                "Navigation",
                Destination.SESSION_HISTORY,
                List.of("history", "saved sessions", "session")));
        entries.add(new Entry(
                "nav.overview",
                "Overview",
                "Open the Client UI overview hub.",
                "Navigation",
                Destination.OVERVIEW,
                List.of("home", "dashboard")));

        return List.copyOf(entries);
    }

    private static Map<String, Entry> indexById(List<Entry> entries) {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (Entry entry : entries) {
            map.put(entry.id(), entry);
            map.putIfAbsent(entry.id().toLowerCase(Locale.ROOT), entry);
        }
        return Map.copyOf(map);
    }

    private static void addHud(
            List<Entry> entries,
            String id,
            String label,
            String description,
            String... aliases) {
        entries.add(new Entry(
                id,
                label,
                description,
                "Mining HUD",
                Destination.QOL_SETTINGS,
                List.of(aliases)));
    }

    private static boolean containsNormalized(String haystack, String needle) {
        if (haystack == null || haystack.isBlank() || needle.isEmpty()) {
            return false;
        }
        return normalizeQuery(haystack).contains(needle);
    }
}
