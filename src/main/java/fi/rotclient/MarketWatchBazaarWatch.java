package fi.rotclient;

import java.util.Locale;
import java.util.UUID;

final class MarketWatchBazaarWatch {
    String id = UUID.randomUUID().toString();
    String productId = "";

    boolean enabled = true;

    double maxInstantBuyPrice;
    double minInstantSellPrice;
    double minSpreadCoins;
    double minSpreadPercent;

    long minWeeklyVolume;
    long cooldownSeconds = 60L;

    MarketWatchBazaarWatch copy() {
        MarketWatchBazaarWatch copy =
                new MarketWatchBazaarWatch();

        copy.id = id;
        copy.productId = productId;
        copy.enabled = enabled;

        copy.maxInstantBuyPrice = maxInstantBuyPrice;
        copy.minInstantSellPrice = minInstantSellPrice;
        copy.minSpreadCoins = minSpreadCoins;
        copy.minSpreadPercent = minSpreadPercent;

        copy.minWeeklyVolume = minWeeklyVolume;
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

        productId =
                productId == null
                        ? ""
                        : productId.trim().toUpperCase(Locale.ROOT);

        maxInstantBuyPrice =
                nonNegative(maxInstantBuyPrice);
        minInstantSellPrice =
                nonNegative(minInstantSellPrice);
        minSpreadCoins =
                nonNegative(minSpreadCoins);
        minSpreadPercent =
                nonNegative(minSpreadPercent);

        minWeeklyVolume = Math.max(0L, minWeeklyVolume);
        cooldownSeconds = Math.max(0L, cooldownSeconds);
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            return 0.0D;
        }
        return value;
    }
}