package fi.rotclient;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

/** Known menu grammar. Semantic actions must be unique and restricted to actual container slots. */
final class MarketTradeMenuPolicy {
    record Item(int slot, String name, String id, String uuid, String tier, String reforge, int count, List<String> lore) {
        Item { lore = List.copyOf(lore); }
    }
    record Menu(Object identity, int containerId, int stateId, String title, int size, List<Item> items) {
        Menu { items = List.copyOf(items); }
        Item slot(int n) { return items.stream().filter(i -> i.slot == n).findFirst().orElse(null); }
        int unique(String... names) {
            var matches = items.stream().filter(i -> i.slot >= 0 && i.slot < size)
                    .filter(i -> java.util.Arrays.stream(names).anyMatch(n -> n.equalsIgnoreCase(i.name))).toList();
            return matches.size() == 1 ? matches.getFirst().slot : -1;
        }
    }
    static boolean variant(Item item, String id, String uuid, String tier, String reforge, int count) {
        return item != null && !id.isBlank() && id.equals(item.id) && (uuid.isBlank() || uuid.equals(item.uuid))
                && (tier.isBlank() || tier.equals(item.tier)) && reforge.equals(item.reforge) && count == item.count;
    }
    static String tier(List<String> lore) {
        String result = "";
        for (String row : lore) {
            var line = CommissionDisplayPolicy.normalizeLine(row).replaceAll("[✦✪⚚]", "").trim();
            for (String tier : MarketWatchVariantPolicy.TIERS) {
                if (!tier.isBlank() && line.matches(Pattern.quote(tier.replace('_', ' ')) + "(?: DUNGEON)?(?: [A-Z ]+)?")) result = tier;
            }
        }
        return result;
    }
    static OptionalDouble coins(List<String> lore, String... labels) {
        Double value = null;
        for (String row : lore) for (String label : labels) {
            var pattern = Pattern.compile("(?i)^" + Pattern.quote(label) + ":\\s*([0-9,]+(?:\\.[0-9]+)?) coins[.!]?$" );
            var m = pattern.matcher(CommissionDisplayPolicy.normalizeLine(row));
            if (!m.matches()) continue;
            try {
                double next = new BigDecimal(m.group(1).replace(",", "")).doubleValue();
                if (!Double.isFinite(next) || next < 0 || value != null && Math.abs(value - next) > .00001)
                    return OptionalDouble.empty();
                value = next;
            } catch (RuntimeException malformed) { return OptionalDouble.empty(); }
        }
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }
    static boolean selected(Item item, String target) {
        return item != null && item.lore.stream().map(CommissionDisplayPolicy::normalizeLine)
                .anyMatch(line -> line.matches("(?i)^▶\\s*" + Pattern.quote(target.replace('_', ' ')) + "$"));
    }
    static boolean searchSign(List<String> lines) {
        return lines.size() == 4 && CommissionDisplayPolicy.normalizeLine(lines.get(2) + " " + lines.get(3))
                .equalsIgnoreCase("Enter query");
    }
    static boolean numericSign(List<String> lines, String purpose) {
        if (lines.size() != 4) return false;
        String text = CommissionDisplayPolicy.normalizeLine(lines.get(2) + " " + lines.get(3)).toLowerCase(java.util.Locale.ROOT);
        return switch (purpose) {
            case "price" -> text.matches("(?:enter|set) (?:a |your )?price(?: per unit)?(?: here| above)?");
            case "buy_amount" -> text.equals("enter amount to order");
            case "sell_amount" -> text.equals("enter amount to sell");
            default -> false;
        };
    }
    static boolean equal(double a, double b) { return Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) < .00001; }
    private MarketTradeMenuPolicy() {}
}
