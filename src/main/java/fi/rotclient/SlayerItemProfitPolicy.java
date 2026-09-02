package fi.rotclient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Pure projection for Slayer drops. Unknown Bazaar prices never become coins. */
public final class SlayerItemProfitPolicy {
    public record Row(String displayName, int quantity, BigDecimal totalValue, boolean priced) {
    }

    public record Projection(BigDecimal totalValue, int pricedItems, int unpricedItems, List<Row> rows) {
        public Projection {
            totalValue = totalValue == null ? BigDecimal.ZERO : totalValue;
            rows = List.copyOf(rows);
        }
    }

    private SlayerItemProfitPolicy() {
    }

    /** Stable SkyBlock item-id gate for world rendering; display names are not trusted here. */
    public static boolean isKnownSlayerDropId(String skyBlockId) {
        return SlayerRngCatalog.byId(skyBlockId).isPresent();
    }

    public static Projection project(
            Map<String, Integer> drops,
            Map<String, BigDecimal> unitPricesBySkyBlockId) {
        BigDecimal total = BigDecimal.ZERO;
        int pricedItems = 0;
        int unpricedItems = 0;
        List<Row> rows = new ArrayList<>();
        if (drops == null || drops.isEmpty()) {
            return new Projection(total, 0, 0, rows);
        }
        Map<String, BigDecimal> safePrices = unitPricesBySkyBlockId == null
                ? Map.of() : unitPricesBySkyBlockId;
        for (Map.Entry<String, Integer> drop : drops.entrySet()) {
            int quantity = Math.max(0, drop.getValue() == null ? 0 : drop.getValue());
            if (quantity == 0) continue;
            String display = drop.getKey() == null ? "Unknown drop" : drop.getKey().trim();
            BigDecimal unit = SlayerRngCatalog.byDisplay(display)
                    .map(SlayerRngCatalog.Entry::skyBlockId)
                    .map(safePrices::get)
                    .filter(SlayerItemProfitPolicy::isPositiveFinite)
                    .orElse(null);
            if (unit == null) {
                unpricedItems += quantity;
                rows.add(new Row(display, quantity, BigDecimal.ZERO, false));
                continue;
            }
            BigDecimal value = unit.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(value);
            pricedItems += quantity;
            rows.add(new Row(display, quantity, value, true));
        }
        rows.sort(Comparator.comparing(Row::priced).reversed()
                .thenComparing(Row::totalValue, Comparator.reverseOrder())
                .thenComparing(Row::displayName, String.CASE_INSENSITIVE_ORDER));
        return new Projection(total.setScale(2, RoundingMode.HALF_UP), pricedItems, unpricedItems, rows);
    }

    private static boolean isPositiveFinite(BigDecimal value) {
        return value != null && value.signum() > 0
                && Double.isFinite(value.doubleValue());
    }
}
