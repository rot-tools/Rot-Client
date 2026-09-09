package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Missing enchants: compare hovered lore / NBT enchant names against the
 * bundled pool for that item type. Blank keybind means always show.
 */
public final class MissingEnchantsPolicy {
    public record TooltipPlan(List<String> missing, List<String> upgrades) {
        public TooltipPlan {
            missing = missing == null ? List.of() : List.copyOf(missing);
            upgrades = upgrades == null ? List.of() : List.copyOf(upgrades);
        }

        public String summary() {
            return missing.size() + " missing · " + upgrades.size() + " upgradable";
        }
    }

    private static final Pattern TYPE = Pattern.compile(
            "(?:SHINY\\s+)?(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)\\s+"
                    + "(?:DUNGEON\\s+)?([A-Z][A-Z' -]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCHANT = Pattern.compile(
            "([A-Za-z][A-Za-z' -]{1,}?)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X|\\d{1,2})\\b");
    private static final Pattern TYPE_KEYWORD = Pattern.compile(
            "\\b(SWORD|LONGSWORD|SHORTSWORD|CLEAVER|DAGGER|KATANA|SCYTHE|RAPIER|BLADE|MACE|FOIL|"
                    + "WEAPON|BOW|SHORTBOW|PICKAXE|DRILL|GAUNTLET|AXE|HOE|SHOVEL|"
                    + "FISHING ROD|FISHING WEAPON|WAND|STAFF|"
                    + "HELMET|CHESTPLATE|LEGGINGS|BOOTS)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, List<String>> POOLS = Map.ofEntries(
            Map.entry("SWORD", List.of(
                    "Sharpness", "Smite", "Bane of Arthropods", "Critical", "Execute", "First Strike", "Giant Killer",
                    "Lethality", "Cubism", "Impaling", "Thunderlord", "Vampirism",
                    "Venomous", "Experience", "Looting", "Scavenger", "Luck", "Ender Slayer")),
            Map.entry("BOW", List.of(
                    "Power", "Aiming", "Infinite Quiver", "Cubism", "Impaling",
                    "Chance", "Dragon Tracer", "Piercing", "Overload", "Ultimate Jerry")),
            Map.entry("PICKAXE", List.of(
                    "Efficiency", "Fortune", "Pristine", "Smelting Touch",
                    "Experience", "Compact", "Silk Touch")),
            Map.entry("AXE", List.of(
                    "Efficiency", "Replenish", "Turbo-Crop", "Harvesting", "Cultivating")),
            Map.entry("FISHING", List.of(
                    "Lure", "Luck of the Sea", "Caster", "Frail", "Spiked Hook",
                    "Expertise", "Magnet", "Angler")),
            Map.entry("WAND", List.of(
                    "Ultimate Wise", "Mana Steal", "Experience")),
            Map.entry("HELMET", List.of(
                    "Growth", "Protection", "Aqua Affinity", "Respiration",
                    "Thorns", "Big Brain", "True Protection")),
            Map.entry("CHESTPLATE", List.of(
                    "Growth", "Protection", "Thorns", "True Protection", "Counter-Strike")),
            Map.entry("LEGGINGS", List.of(
                    "Growth", "Protection", "Thorns", "True Protection", "Smarty Pants")),
            Map.entry("BOOTS", List.of(
                    "Growth", "Protection", "Feather Falling", "Depth Strider",
                    "Sugar Rush", "True Protection")));

    private static final Map<String, Integer> MAX_LEVELS = Map.ofEntries(
            Map.entry("sharpness", 7),
            Map.entry("critical", 7),
            Map.entry("execute", 6),
            Map.entry("first strike", 5),
            Map.entry("giant killer", 7),
            Map.entry("lethality", 6),
            Map.entry("cubism", 6),
            Map.entry("impaling", 3),
            Map.entry("thunderlord", 7),
            Map.entry("vampirism", 6),
            Map.entry("venomous", 6),
            Map.entry("experience", 4),
            Map.entry("looting", 5),
            Map.entry("scavenger", 5),
            Map.entry("luck", 7),
            Map.entry("ender slayer", 7),
            Map.entry("smite", 7),
            Map.entry("bane of arthropods", 7),
            Map.entry("power", 7),
            Map.entry("infinite quiver", 10),
            Map.entry("chance", 5),
            Map.entry("dragon tracer", 5),
            Map.entry("piercing", 1),
            Map.entry("overload", 5),
            Map.entry("efficiency", 10),
            Map.entry("fortune", 4),
            Map.entry("pristine", 5),
            Map.entry("smelting touch", 1),
            Map.entry("compact", 10),
            Map.entry("silk touch", 1),
            Map.entry("growth", 7),
            Map.entry("protection", 7),
            Map.entry("thorns", 3),
            Map.entry("feather falling", 10),
            Map.entry("depth strider", 3),
            Map.entry("ultimate wise", 5));

    private static final List<Set<String>> CONFLICT_GROUPS = List.of(
            Set.of("sharpness", "smite", "bane of arthropods"),
            Set.of("first strike", "triple strike"),
            Set.of("execute", "prosecute"),
            Set.of("giant killer", "titan killer"),
            Set.of("life steal", "mana steal", "syphon"),
            Set.of("fortune", "silk touch"),
            Set.of("smelting touch", "silk touch"));

    private static final Set<String> SWORD_IDS = Set.of(
            "HYPERION", "SCYLLA", "ASTRAEA", "VALKYRIE", "NECRON_BLADE", "WITHER_BLADE",
            "LIVID_DAGGER", "SHADOW_FURY", "FLOWER_OF_TRUTH", "SPIRIT_SWORD",
            "ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID", "SILENT_DEATH", "GIANTS_SWORD",
            "DARK_CLAYMORE", "ATOMSPLIT_KATANA", "VOIDEDGE_KATANA", "VORPAL_KATANA",
            "REAPER_FALCHION", "REAPER_SCYTHE", "AXE_OF_THE_SHREDDED", "SOUL_WHIP",
            "EMERALD_BLADE", "MIDAS_SWORD", "FROZEN_SCYTHE", "LEAPING_SWORD");
    private static final Set<String> BOW_IDS = Set.of(
            "TERMINATOR", "JUJU_SHORTBOW", "LAST_BREATH", "RUNAANS_BOW", "MOSQUITO_BOW",
            "ARTISANAL_SHORTBOW", "MACHINE_GUN_SHORTBOW", "SOULS_REBOUND", "HURRICANE_BOW",
            "MAGMA_BOW", "DEATH_BOW", "UNDEAD_BOW", "END_STONE_BOW");
    private static final Set<String> PICKAXE_IDS = Set.of(
            "TITANIUM_DRILL_R1", "TITANIUM_DRILL_R2", "TITANIUM_DRILL_R3", "TITANIUM_DRILL_R4",
            "GEMSTONE_DRILL_1", "GEMSTONE_DRILL_2", "GEMSTONE_DRILL_3", "GEMSTONE_DRILL_4",
            "MITHRIL_DRILL_1", "MITHRIL_DRILL_2", "DIVAN_DRILL", "GAUNTLET_OF_CONTAGION",
            "GEMSTONE_GAUNTLET", "PICKONIMBUS");

    private MissingEnchantsPolicy() {
    }

    public static String itemType(List<String> loreLines) {
        return itemType(loreLines, "", "");
    }

    public static String itemType(List<String> loreLines, String hoverName, String skyBlockId) {
        String fromId = typeFromSkyBlockId(skyBlockId);
        if (!fromId.isEmpty()) {
            return fromId;
        }
        if (loreLines != null) {
            for (int i = loreLines.size() - 1; i >= 0; i--) {
                String stripped = ChatTextPolicy.stripFormatting(loreLines.get(i));
                Matcher matcher = TYPE.matcher(stripped.toUpperCase(Locale.ROOT));
                if (matcher.find()) {
                    String type = matcher.group(1).trim();
                    if (!type.isEmpty()) {
                        return type;
                    }
                }
                String keyword = keywordType(stripped);
                if (!keyword.isEmpty()) {
                    return keyword;
                }
            }
        }
        return keywordType(hoverName == null ? "" : hoverName);
    }

    public static Set<String> presentEnchantNames(List<String> loreLines) {
        return presentEnchantNames(loreLines, Set.of());
    }

    public static Set<String> presentEnchantNames(List<String> loreLines, Set<String> nbtKeys) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (nbtKeys != null) {
            for (String key : nbtKeys) {
                String normalized = normalizeEnchant(key);
                if (!normalized.isEmpty()) {
                    names.add(normalized);
                }
            }
        }
        if (loreLines == null) {
            return names;
        }
        for (String line : loreLines) {
            String stripped = ChatTextPolicy.stripFormatting(line);
            Matcher matcher = ENCHANT.matcher(stripped);
            while (matcher.find()) {
                names.add(normalizeEnchant(matcher.group(1)));
            }
        }
        return names;
    }

    public static List<String> missing(String itemType, Set<String> present) {
        List<String> pool = poolFor(itemType);
        if (pool.isEmpty()) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        Set<String> have = present == null ? Set.of() : present;
        for (String enchant : pool) {
            if (!have.contains(normalizeEnchant(enchant))) {
                missing.add(enchant);
            }
        }
        return List.copyOf(missing);
    }

    public static TooltipPlan plan(
            String itemType,
            Map<String, Integer> presentLevels,
            boolean showUpgradable,
            boolean showConflicting) {
        Map<String, Integer> levels = presentLevels == null ? Map.of() : presentLevels;
        LinkedHashSet<String> present = new LinkedHashSet<>();
        for (String key : levels.keySet()) {
            String normalized = normalizeEnchant(key);
            if (!normalized.isEmpty()) {
                present.add(normalized);
            }
        }

        List<String> missing = new ArrayList<>();
        for (String enchant : missing(itemType, present)) {
            String normalized = normalizeEnchant(enchant);
            if (showConflicting || !conflictsWithPresent(normalized, present)) {
                missing.add(enchant);
            }
        }

        List<String> upgrades = new ArrayList<>();
        if (showUpgradable) {
            for (Map.Entry<String, Integer> entry : levels.entrySet()) {
                String normalized = normalizeEnchant(entry.getKey());
                int current = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
                int maximum = MAX_LEVELS.getOrDefault(normalized, current);
                if (current > 0 && maximum > current) {
                    upgrades.add(prettify(normalized) + " " + roman(current)
                            + " → " + roman(maximum));
                }
            }
        }
        upgrades.sort(String.CASE_INSENSITIVE_ORDER);
        return new TooltipPlan(missing, upgrades);
    }

    public static boolean shouldShow(boolean moduleEnabled, boolean keyUnbound, boolean keyHeld) {
        return moduleEnabled && (keyUnbound || keyHeld);
    }

    public static String prettify(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(id.length());
        boolean cap = true;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '_') {
                out.append(' ');
                cap = true;
            } else if (cap) {
                out.append(Character.toUpperCase(c));
                cap = false;
            } else {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    static List<String> poolFor(String itemType) {
        if (itemType == null) {
            return List.of();
        }
        String type = itemType.trim().toUpperCase(Locale.ROOT);
        if (type.contains("FISHING")) {
            return POOLS.get("FISHING");
        }
        if (containsAny(type,
                "SWORD", "CLEAVER", "DAGGER", "KATANA", "SCYTHE", "RAPIER",
                "BLADE", "MACE", "FOIL", "WEAPON", "FALCHION", "CLAYMORE")) {
            return POOLS.get("SWORD");
        }
        if (type.contains("BOW") || type.contains("SHORTBOW")) {
            return POOLS.get("BOW");
        }
        if (containsAny(type, "PICKAXE", "DRILL", "GAUNTLET")) {
            return POOLS.get("PICKAXE");
        }
        if (type.contains("AXE") && !type.contains("PICKAXE")) {
            return POOLS.get("AXE");
        }
        if (type.contains("FISHING")) {
            return POOLS.get("FISHING");
        }
        if (type.contains("WAND") || type.contains("STAFF") || type.contains("SCEPTRE")) {
            return POOLS.get("WAND");
        }
        if (type.contains("HELMET")) {
            return POOLS.get("HELMET");
        }
        if (type.contains("CHESTPLATE")) {
            return POOLS.get("CHESTPLATE");
        }
        if (type.contains("LEGGINGS")) {
            return POOLS.get("LEGGINGS");
        }
        if (type.contains("BOOTS")) {
            return POOLS.get("BOOTS");
        }
        return List.of();
    }

    static String normalizeEnchant(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace("'", "")
                .replaceAll("\\s+", " ");
    }

    static int levelFromToken(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        String value = token.trim().toUpperCase(Locale.ROOT);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            int total = 0;
            int previous = 0;
            for (int i = value.length() - 1; i >= 0; i--) {
                int current = switch (value.charAt(i)) {
                    case 'I' -> 1;
                    case 'V' -> 5;
                    case 'X' -> 10;
                    default -> 0;
                };
                if (current < previous) {
                    total -= current;
                } else {
                    total += current;
                    previous = current;
                }
            }
            return total;
        }
    }

    static String roman(int level) {
        int remaining = Math.max(1, Math.min(50, level));
        int[] values = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                out.append(symbols[i]);
                remaining -= values[i];
            }
        }
        return out.toString();
    }

    private static boolean conflictsWithPresent(String candidate, Set<String> present) {
        for (Set<String> group : CONFLICT_GROUPS) {
            if (!group.contains(candidate)) {
                continue;
            }
            for (String applied : present) {
                if (!candidate.equals(applied) && group.contains(applied)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String typeFromSkyBlockId(String skyBlockId) {
        if (skyBlockId == null || skyBlockId.isBlank()) {
            return "";
        }
        String id = skyBlockId.trim().toUpperCase(Locale.ROOT);
        if (SWORD_IDS.contains(id) || id.contains("SWORD") || id.contains("KATANA")
                || id.contains("SCYTHE") || id.contains("DAGGER") || id.contains("CLEAVER")
                || id.contains("BLADE") || id.contains("CLAYMORE")) {
            return "SWORD";
        }
        if (BOW_IDS.contains(id) || id.contains("BOW")) {
            return "BOW";
        }
        if (PICKAXE_IDS.contains(id) || id.contains("PICKAXE") || id.contains("DRILL")
                || id.contains("GAUNTLET")) {
            return "PICKAXE";
        }
        if (id.contains("FISHING") || id.endsWith("_ROD")) {
            return "FISHING ROD";
        }
        if (id.contains("WAND") || id.contains("STAFF") || id.contains("SCEPTRE")) {
            return "WAND";
        }
        return "";
    }

    private static String keywordType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        Matcher matcher = TYPE_KEYWORD.matcher(raw.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static boolean containsAny(String type, String... tokens) {
        for (String token : tokens) {
            if (type.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
