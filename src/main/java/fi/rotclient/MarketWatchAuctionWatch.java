package fi.rotclient;

import java.util.Locale;
import java.util.UUID;

final class MarketWatchAuctionWatch {
    String id = UUID.randomUUID().toString();

    /*
     * Canonical SkyBlock item ID when known.
     * itemName remains available as a temporary/display fallback until
     * Market Watch item-byte normalization is implemented.
     */
    String itemId = "";
    String itemName = "";
    String tier = "";

    boolean enabled = true;
    boolean binOnly = true;

    long maxPriceCoins;
    double minDiscountPercent;
    long minProfitCoins;
    double minProfitPercent;

    int minSampleSize = 3;
    long cooldownSeconds = 60L;

    MarketWatchAuctionWatch copy() {
        MarketWatchAuctionWatch copy =
                new MarketWatchAuctionWatch();

        copy.id = id;
        copy.itemId = itemId;
        copy.itemName = itemName;
        copy.tier = tier;

        copy.enabled = enabled;
        copy.binOnly = binOnly;

        copy.maxPriceCoins = maxPriceCoins;
        copy.minDiscountPercent = minDiscountPercent;
        copy.minProfitCoins = minProfitCoins;
        copy.minProfitPercent = minProfitPercent;

        copy.minSampleSize = minSampleSize;
        copy.cooldownSeconds = cooldownSeconds;

        copy.normalize();
        return copy;
    }

    void normalize() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        } else {
            id = id.trim();
        }

        itemId = upper(itemId);
        itemName = clean(itemName);
        tier = upper(tier);

        maxPriceCoins = Math.max(0L, maxPriceCoins);
        minDiscountPercent =
                clamp(cleanNumber(minDiscountPercent), 0.0D, 100.0D);
        minProfitCoins = Math.max(0L, minProfitCoins);
        minProfitPercent =
                Math.max(0.0D, cleanNumber(minProfitPercent));

        minSampleSize = Math.max(1, minSampleSize);
        cooldownSeconds = Math.max(0L, cooldownSeconds);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private static double cleanNumber(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum) {

        return Math.max(
                minimum,
                Math.min(maximum, value));
    }
}