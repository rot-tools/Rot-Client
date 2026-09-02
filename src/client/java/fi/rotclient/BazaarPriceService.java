package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class BazaarPriceService {
    private static final long REFRESH_SECONDS = 20;

    interface PriceListener {
        void onPrice(MarketPrices prices);
    }

    record OrderLevel(long amount, double pricePerUnit) {
    }

    record ProductPrice(List<OrderLevel> levels, double fallbackPrice) {
        ProductPrice {
            levels = List.copyOf(levels);
        }

        double instantSellPrice() {
            return levels.isEmpty() ? fallbackPrice : levels.getFirst().pricePerUnit();
        }

        double quoteInstantSell(long amount) {
            long remaining = Math.max(0, amount);
            double total = 0;
            for (OrderLevel level : levels) {
                if (remaining <= 0) break;
                long filled = Math.min(remaining, level.amount());
                total += filled * level.pricePerUnit();
                remaining -= filled;
            }
            return total + remaining * Math.max(0, fallbackPrice);
        }
    }

    record MaterialPrices(ProductPrice raw, ProductPrice enchanted) {
        double quoteInstantSell(long rawItems, long enchantedItems) {
            if ((rawItems > 0L && raw == null)
                    || (enchantedItems > 0L && enchanted == null)) {
                return Double.NaN;
            }
            return (raw == null ? 0D : raw.quoteInstantSell(rawItems))
                    + (enchanted == null
                    ? 0D
                    : enchanted.quoteInstantSell(enchantedItems));
        }
    }

    record MarketPrices(
            Map<TrackedMaterial, MaterialPrices> byMaterial,
            Map<String, ProductPrice> byGemstoneProductId,
            Map<String, ProductPrice> byProductId) {
        MarketPrices(Map<TrackedMaterial, MaterialPrices> byMaterial) {
            this(byMaterial, Map.of(), Map.of());
        }

        MarketPrices(
                Map<TrackedMaterial, MaterialPrices> byMaterial,
                Map<String, ProductPrice> byGemstoneProductId) {
            this(byMaterial, byGemstoneProductId, Map.of());
        }

        MarketPrices {
            byMaterial = Map.copyOf(byMaterial);
            byGemstoneProductId = Map.copyOf(byGemstoneProductId);
            byProductId = Map.copyOf(byProductId);
        }

        MaterialPrices forMaterial(TrackedMaterial material) {
            return byMaterial.get(material);
        }

        ProductPrice forGemstoneProduct(String productId) {
            return byGemstoneProductId.get(productId);
        }

        ProductPrice forProduct(String productId) {
            return byProductId.get(productId);
        }
    }

    private static final URI BAZAAR_URI = URI.create("https://api.hypixel.net/v2/skyblock/bazaar");
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "RotClient-Bazaar");
        thread.setDaemon(true);
        return thread;
    });

    void start(PriceListener onPrice) {
        executor.scheduleWithFixedDelay(
                () -> fetch(onPrice), 0, REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    private void fetch(PriceListener onPrice) {
        try {
            HttpRequest request = HttpRequest.newBuilder(BAZAAR_URI)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "RotClient/2.0.0+mc26.2")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            JsonObject products = JsonParser.parseString(response.body())
                    .getAsJsonObject()
                    .getAsJsonObject("products");
            MarketPrices marketPrices = parseMarketPrices(products);
            if (!marketPrices.byMaterial().isEmpty()
                    || !marketPrices.byGemstoneProductId().isEmpty()) {
                onPrice.onPrice(marketPrices);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException ignored) {
        }
    }

    static MarketPrices parseMarketPrices(JsonObject products) {
        if (products == null) {
            return new MarketPrices(Map.of(), Map.of());
        }

        Map<TrackedMaterial, MaterialPrices> byMaterial =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : TrackedMaterial.values()) {
            try {
                ProductPrice raw = readInstantSellProduct(
                        products.getAsJsonObject(material.rawBazaarId()));
                ProductPrice enchanted = readInstantSellProduct(
                        products.getAsJsonObject(material.enchantedBazaarId()));
                if (raw != null || enchanted != null) {
                    byMaterial.put(
                            material, new MaterialPrices(raw, enchanted));
                }
            } catch (RuntimeException ignored) {
                // One malformed material product must not discard peers.
            }
        }

        Map<String, ProductPrice> byGemstoneProductId = new HashMap<>();
        for (String productId
                : MiningSessionResourcePriceMapping.pricedGemstoneProductIds()) {
            try {
                ProductPrice parsed = readInstantSellProduct(
                        products.getAsJsonObject(productId));
                if (parsed == null) {
                    continue;
                }
                double instantSell = parsed.instantSellPrice();
                if (instantSell <= 0D || !Double.isFinite(instantSell)) {
                    continue;
                }
                byGemstoneProductId.put(productId, parsed);
            } catch (RuntimeException ignored) {
            }
        }

        Map<String, ProductPrice> byProductId = new HashMap<>();
        for (SlayerRngCatalog.Entry entry : SlayerRngCatalog.entries()) {
            try {
                ProductPrice parsed = readInstantSellProduct(
                        products.getAsJsonObject(entry.skyBlockId()));
                if (parsed != null && parsed.instantSellPrice() > 0D
                        && Double.isFinite(parsed.instantSellPrice())) {
                    byProductId.put(entry.skyBlockId(), parsed);
                }
            } catch (RuntimeException ignored) {
                // A missing/non-Bazaar Slayer product is explicitly unpriced.
            }
        }

        return new MarketPrices(byMaterial, byGemstoneProductId, byProductId);
    }

    /**
     * Hypixel labels this {@code sell_summary}, but it contains the buy-order
     * side that instant sell fills against (not player sell offers).
     */
    static ProductPrice readInstantSellProduct(JsonObject product) {
        if (product == null) return null;

        JsonArray sellSummary = product.getAsJsonArray("sell_summary");
        List<OrderLevel> levels = new ArrayList<>();
        if (sellSummary != null) {
            for (JsonElement element : sellSummary) {
                try {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject order = element.getAsJsonObject();
                    if (!order.has("amount")) {
                        continue;
                    }
                    long amount = order.get("amount").getAsLong();
                    BigDecimal priceBd = readPositiveBigDecimal(
                            order.get("pricePerUnit"));
                    if (amount > 0 && priceBd != null) {
                        levels.add(new OrderLevel(
                                amount, priceBd.doubleValue()));
                    }
                } catch (RuntimeException ignored) {
                    // Skip malformed order levels; peers may still resolve.
                }
            }
        }
        levels.sort(Comparator.comparingDouble(OrderLevel::pricePerUnit).reversed());

        double fallbackPrice = 0;
        JsonObject quickStatus = null;
        try {
            if (product.has("quick_status")
                    && product.get("quick_status").isJsonObject()) {
                quickStatus = product.getAsJsonObject("quick_status");
            }
        } catch (RuntimeException ignored) {
            quickStatus = null;
        }
        if (quickStatus != null && quickStatus.has("sellPrice")) {
            BigDecimal sellPrice = readPositiveBigDecimal(
                    quickStatus.get("sellPrice"));
            if (sellPrice != null) {
                fallbackPrice = sellPrice.doubleValue();
            }
        }
        if (levels.isEmpty()
                && (fallbackPrice <= 0 || !Double.isFinite(fallbackPrice))) {
            return null;
        }
        if (fallbackPrice <= 0 || !Double.isFinite(fallbackPrice)) {
            fallbackPrice = levels.getLast().pricePerUnit();
        } else if (!levels.isEmpty()) {
            // Beyond the published depth, never let the weighted fallback
            // improve on the worst visible order that had to be consumed.
            fallbackPrice = Math.min(
                    fallbackPrice, levels.getLast().pricePerUnit());
        }
        return new ProductPrice(levels, fallbackPrice);
    }

    /**
     * Prefer JSON decimal text → {@link BigDecimal} over binary double when
     * Gson exposes it. Returns null for missing/non-positive/non-finite values.
     */
    private static BigDecimal readPositiveBigDecimal(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            BigDecimal value;
            if (element.isJsonPrimitive()
                    && element.getAsJsonPrimitive().isString()) {
                value = new BigDecimal(element.getAsString().trim());
            } else {
                value = element.getAsBigDecimal();
            }
            if (value == null || value.signum() <= 0) {
                return null;
            }
            double asDouble = value.doubleValue();
            if (!Double.isFinite(asDouble) || asDouble <= 0D) {
                return null;
            }
            return value;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
