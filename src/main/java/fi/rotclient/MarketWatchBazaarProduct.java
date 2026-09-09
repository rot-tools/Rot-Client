package fi.rotclient;

import java.util.List;
import java.util.Locale;

record MarketWatchBazaarProduct(
        String productId,
        double quickBuyPrice,
        double quickSellPrice,
        long buyVolume,
        long sellVolume,
        long buyMovingWeek,
        long sellMovingWeek,
        List<OrderLevel> buyOrders,
        List<OrderLevel> sellOrders) {

    MarketWatchBazaarProduct {
        productId = productId == null ? "" : productId.trim().toUpperCase(Locale.ROOT);
        buyOrders = List.copyOf(buyOrders == null ? List.of() : buyOrders);
        sellOrders = List.copyOf(sellOrders == null ? List.of() : sellOrders);
    }

    record OrderLevel(long amount, double pricePerUnit) {
    }
}