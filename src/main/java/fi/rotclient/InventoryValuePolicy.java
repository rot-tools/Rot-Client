package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live AH/BZ inventory totals for the survival-inventory S-value mark.
 * Minecraft-free so tests can lock pet NEU ids, slot filters, and quotes.
 */
public final class InventoryValuePolicy {
    private static final Pattern PET_TYPE =
            Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_TIER =
            Pattern.compile("\"tier\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern LVL_NAME =
            Pattern.compile("\\[Lvl\\s+\\d+\\]\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_ID = Pattern.compile("[^A-Z0-9_]+");

    private InventoryValuePolicy() {
    }

    /**
     * Survival crafting output is a preview of the grid and would double-count
     * the ingredients. Every other vanilla inventory well is part of the bag.
     */
    public static boolean includeSurvivalSlot(int slotX, int slotY) {
        return slotX != InventoryOverlayPolicy.CRAFT_RESULT_X
                || slotY != InventoryOverlayPolicy.CRAFT_RESULT_Y;
    }

    /**
     * Candidate market ids for one stack. Pets expand to NEU {@code TYPE;tier}
     * plus {@code PET_TYPE} so lowest-BIN maps can price them.
     */
    public static List<String> marketIdCandidates(
            String skyblockId, String petInfo, String hoverName) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        String sky = skyblockId == null ? "" : skyblockId.trim().toUpperCase(Locale.ROOT);
        boolean pet = sky.equals("PET") || (petInfo != null && !petInfo.isBlank());
        if (pet) {
            String type = petType(petInfo, hoverName);
            int tier = petTierIndex(petInfo);
            if (!type.isEmpty()) {
                if (tier >= 0) {
                    ids.add(type + ";" + tier);
                }
                ids.add("PET_" + type);
                ids.add(type);
            }
        }
        if (!sky.isEmpty() && !sky.equals("PET")) {
            ids.add(sky);
            if (sky.startsWith("STARRED_")) {
                ids.add(sky.substring("STARRED_".length()));
            }
        }
        return List.copyOf(ids);
    }

    public static String pricedMarketId(
            List<String> candidates,
            Map<String, Double> lowestBin,
            Map<String, Double> bazaarBuy,
            Map<String, Double> bazaarSell) {
        if (candidates == null) {
            return "";
        }
        for (String id : candidates) {
            if (unitValue(id, lowestBin, bazaarBuy, bazaarSell) > 0.0D) {
                return id;
            }
        }
        return "";
    }

    public static double unitValue(
            String marketId,
            Map<String, Double> lowestBin,
            Map<String, Double> bazaarBuy,
            Map<String, Double> bazaarSell) {
        return StorageOverlayPolicy.marketUnitValue(
                lookup(lowestBin, marketId),
                lookup(bazaarBuy, marketId),
                lookup(bazaarSell, marketId));
    }

    public static double totalCoins(
            List<StorageOverlayPolicy.MarketLine> lines,
            Map<String, Double> lowestBin,
            Map<String, Double> bazaarBuy,
            Map<String, Double> bazaarSell) {
        if (lines == null || lines.isEmpty()) {
            return 0.0D;
        }
        java.util.HashMap<String, Double> units = new java.util.HashMap<>();
        List<StorageOverlayPolicy.MarketLine> priced = new ArrayList<>();
        for (StorageOverlayPolicy.MarketLine line : lines) {
            if (line == null || line.marketId().isBlank() || line.count() <= 0) {
                continue;
            }
            String id = line.marketId();
            if (!units.containsKey(id)) {
                double unit = unitValue(id, lowestBin, bazaarBuy, bazaarSell);
                if (!(unit > 0.0D)) {
                    continue;
                }
                units.put(id, unit);
            }
            priced.add(line);
        }
        return StorageOverlayPolicy.instantSellTotal(priced, units);
    }

    /** Compact number beside the S mark, same suffixes as Storage Overlay. */
    public static String compactMark(double coins) {
        if (!(coins > 0.0D) || !Double.isFinite(coins)) {
            return "?";
        }
        return StorageOverlayPolicy.formatCoins(Math.round(coins));
    }

    public static String tooltip(double coins) {
        return StorageOverlayPolicy.pageValueLabel(coins);
    }

    static String petType(String petInfo, String hoverName) {
        Matcher matcher = PET_TYPE.matcher(petInfo == null ? "" : petInfo);
        if (matcher.find()) {
            return normalizeId(matcher.group(1));
        }
        String title = MenuKeybindPolicy.stripGuiText(hoverName);
        Matcher named = LVL_NAME.matcher(title);
        if (named.find()) {
            title = named.group(1);
        }
        return normalizeId(title);
    }

    static int petTierIndex(String petInfo) {
        Matcher matcher = PET_TIER.matcher(petInfo == null ? "" : petInfo);
        if (!matcher.find()) {
            return -1;
        }
        return switch (matcher.group(1).trim().toUpperCase(Locale.ROOT)) {
            case "COMMON" -> 0;
            case "UNCOMMON" -> 1;
            case "RARE" -> 2;
            case "EPIC" -> 3;
            case "LEGENDARY" -> 4;
            case "MYTHIC" -> 5;
            default -> -1;
        };
    }

    private static String normalizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return NON_ID.matcher(upper).replaceAll("");
    }

    private static double lookup(Map<String, Double> map, String id) {
        if (map == null || id == null || id.isBlank()) {
            return 0.0D;
        }
        Double direct = map.get(id);
        if (direct != null && Double.isFinite(direct)) {
            return direct;
        }
        String upper = id.trim().toUpperCase(Locale.ROOT);
        Double folded = map.get(upper);
        if (folded != null && Double.isFinite(folded)) {
            return folded;
        }
        if (upper.startsWith("STARRED_")) {
            return lookup(map, upper.substring("STARRED_".length()));
        }
        return 0.0D;
    }
}
