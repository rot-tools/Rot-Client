package fi.rotclient;

import java.util.Locale;

/** Shared passive filters; unknown NBT never proves a specific reforge or canonical ID. */
final class MarketWatchVariantPolicy {
    static final java.util.List<String> TIERS = java.util.List.of("", "COMMON", "UNCOMMON", "RARE", "EPIC",
            "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL", "VERY_SPECIAL");
    static String reforgeFilter(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return text.equals("any") || text.equals("*") ? "" : text;
    }
    static boolean matches(MarketWatchAuctionWatch watch, MarketWatchAuction auction) {
        if (watch == null || auction == null || auction.uuid().isBlank()) return false;
        if (watch.binOnly && !auction.bin()) return false;
        var variant = auction.variant();
        // Preserve legacy passive watches when item_bytes is unavailable. This fallback never authorizes Plus trading.
        boolean item = !watch.itemId.isBlank() && variant.known()
                ? watch.itemId.equalsIgnoreCase(variant.itemId())
                : !watch.itemName.isBlank() && watch.itemName.equalsIgnoreCase(auction.itemName());
        if (!item || !watch.tier.isBlank() && !watch.tier.equalsIgnoreCase(auction.tier())) return false;
        String reforge = reforgeFilter(watch.reforge);
        return reforge.isBlank() || variant.known()
                && (reforge.equals("none") ? variant.reforge().isBlank() : reforge.equals(variant.reforge()));
    }
    static String comparableKey(MarketWatchAuction auction) {
        var variant = auction.variant();
        String item = variant.known() ? "ID:" + variant.itemId() : "NAME:" + clean(auction.itemName());
        // A visible name also retains upgrade/star/special-instance distinctions already used by the scanner.
        return item + '\0' + clean(auction.itemName()) + '\0' + clean(auction.tier()) + '\0'
                + clean(auction.category()) + '\0' + (variant.known() ? variant.reforge() : "?unknown")
                + '\0' + variant.attributesKey();
    }
    private static String clean(String value) {
        return CommissionDisplayPolicy.normalizeLine(value).toLowerCase(Locale.ROOT);
    }
    private MarketWatchVariantPolicy() {}
}
