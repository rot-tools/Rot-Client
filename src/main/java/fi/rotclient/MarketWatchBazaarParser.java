package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MarketWatchBazaarParser {
    private MarketWatchBazaarParser() {
    }

    static MarketWatchBazaarSnapshot parse(JsonObject root) {
        if (root == null) {
            return new MarketWatchBazaarSnapshot(-1L, Map.of());
        }

        Map<String, MarketWatchBazaarProduct> products = new HashMap<>();

        JsonObject productRoot = object(root, "products");
        if (productRoot != null) {
            for (Map.Entry<String, JsonElement> entry : productRoot.entrySet()) {
                if (entry.getKey() == null
                        || entry.getValue() == null
                        || !entry.getValue().isJsonObject()) {
                    continue;
                }

                String productId = entry.getKey()
                        .trim()
                        .toUpperCase(Locale.ROOT);

                if (productId.isBlank()) {
                    continue;
                }

                JsonObject product = entry.getValue().getAsJsonObject();
                JsonObject status = object(product, "quick_status");

                MarketWatchBazaarProduct parsed =
                        new MarketWatchBazaarProduct(
                                productId,
                                doubleNumber(status, "buyPrice"),
                                doubleNumber(status, "sellPrice"),
                                longNumber(status, "buyVolume"),
                                longNumber(status, "sellVolume"),
                                longNumber(status, "buyMovingWeek"),
                                longNumber(status, "sellMovingWeek"),
                                orders(product, "buy_summary"),
                                orders(product, "sell_summary"));

                products.put(productId, parsed);
            }
        }

        return new MarketWatchBazaarSnapshot(
                longNumber(root, "lastUpdated", -1L),
                products);
    }

    private static List<MarketWatchBazaarProduct.OrderLevel> orders(
            JsonObject product,
            String key) {

        List<MarketWatchBazaarProduct.OrderLevel> result =
                new ArrayList<>();

        JsonArray array = array(product, key);
        if (array == null) {
            return result;
        }

        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject level = element.getAsJsonObject();
            long amount = longNumber(level, "amount");
            double price = doubleNumber(level, "pricePerUnit");

            if (amount > 0L
                    && price > 0.0D
                    && Double.isFinite(price)) {
                result.add(
                        new MarketWatchBazaarProduct.OrderLevel(
                                amount,
                                price));
            }
        }

        return result;
    }

    private static JsonObject object(JsonObject source, String key) {
        try {
            return source != null
                    && source.has(key)
                    && source.get(key).isJsonObject()
                    ? source.getAsJsonObject(key)
                    : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject source, String key) {
        try {
            return source != null
                    && source.has(key)
                    && source.get(key).isJsonArray()
                    ? source.getAsJsonArray(key)
                    : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static long longNumber(JsonObject object, String key) {
        return longNumber(object, key, 0L);
    }

    private static long longNumber(
            JsonObject object,
            String key,
            long fallback) {
        try {
            return object != null && object.has(key)
                    ? object.get(key).getAsLong()
                    : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double doubleNumber(JsonObject object, String key) {
        try {
            return object != null && object.has(key)
                    ? object.get(key).getAsDouble()
                    : 0.0D;
        } catch (RuntimeException ignored) {
            return 0.0D;
        }
    }
}