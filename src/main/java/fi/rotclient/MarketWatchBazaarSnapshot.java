package fi.rotclient;

import java.util.Locale;
import java.util.Map;

record MarketWatchBazaarSnapshot(
        long lastUpdated,
        Map<String, MarketWatchBazaarProduct> products) {

    MarketWatchBazaarSnapshot {
        products = Map.copyOf(products == null ? Map.of() : products);
    }

    MarketWatchBazaarProduct product(String productId) {
        if (productId == null || productId.isBlank()) {
            return null;
        }
        return products.get(productId.trim().toUpperCase(Locale.ROOT));
    }
}