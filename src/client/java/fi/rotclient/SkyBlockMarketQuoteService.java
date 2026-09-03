package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Public Hypixel Bazaar / items / lowest-BIN quotes for Price Tooltips.
 */
final class SkyBlockMarketQuoteService {
    private static final URI BAZAAR = URI.create("https://api.hypixel.net/v2/skyblock/bazaar");
    private static final URI ITEMS = URI.create("https://api.hypixel.net/v2/resources/skyblock/items");
    /**
     * Public NEU-id → coins maps. The legacy Moulberry lowestbin.json now
     * fails with Cloudflare 525, so Elite and tricked.pro are tried first.
     */
    private static final URI[] LOWEST_BIN = {
            URI.create("https://api.eliteskyblock.com/resources/auctions/neu"),
            URI.create("https://lb.tricked.pro/lowestbins"),
            URI.create("https://moulberry.codes/lowestbin.json")
    };

    record Quotes(
            Map<String, Double> bazaarBuy,
            Map<String, Double> bazaarSell,
            Map<String, Double> npcCoins,
            Map<String, Double> motes,
            Map<String, Double> lowestBin) {
        Quotes {
            bazaarBuy = Map.copyOf(bazaarBuy == null ? Map.of() : bazaarBuy);
            bazaarSell = Map.copyOf(bazaarSell == null ? Map.of() : bazaarSell);
            npcCoins = Map.copyOf(npcCoins == null ? Map.of() : npcCoins);
            motes = Map.copyOf(motes == null ? Map.of() : motes);
            lowestBin = Map.copyOf(lowestBin == null ? Map.of() : lowestBin);
        }

        PriceTooltipsPolicy.Quote quote(String itemId) {
            String id = itemId == null ? "" : itemId.trim().toUpperCase(Locale.ROOT);
            return new PriceTooltipsPolicy.Quote(
                    value(lowestBin, id),
                    value(bazaarBuy, id),
                    value(bazaarSell, id),
                    value(npcCoins, id),
                    value(motes, id));
        }

        private static double value(Map<String, Double> map, String id) {
            if (id.isBlank()) {
                return 0.0D;
            }
            Double direct = map.get(id);
            if (direct != null) {
                return direct;
            }
            Double starred = map.get(id.replace("STARRED_", ""));
            return starred == null ? 0.0D : starred;
        }
    }

    private static volatile Quotes snapshot = new Quotes(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "RotClient-MarketQuotes");
                thread.setDaemon(true);
                return thread;
            });
    private static volatile boolean started;

    private SkyBlockMarketQuoteService() {
    }

    static void start() {
        if (started) {
            return;
        }
        started = true;
        EXECUTOR.scheduleWithFixedDelay(SkyBlockMarketQuoteService::refresh, 0, 45, TimeUnit.SECONDS);
    }

    static Quotes current() {
        return snapshot;
    }

    private static void refresh() {
        Map<String, Double> buy = new HashMap<>();
        Map<String, Double> sell = new HashMap<>();
        Map<String, Double> npc = new HashMap<>();
        Map<String, Double> motes = new HashMap<>();
        Map<String, Double> bin = new HashMap<>();
        fetchBazaar(buy, sell);
        fetchItems(npc, motes);
        fetchLowestBin(bin);
        snapshot = new Quotes(
                merge(snapshot.bazaarBuy(), buy),
                merge(snapshot.bazaarSell(), sell),
                merge(snapshot.npcCoins(), npc),
                merge(snapshot.motes(), motes),
                merge(snapshot.lowestBin(), bin));
    }

    private static Map<String, Double> merge(Map<String, Double> previous, Map<String, Double> next) {
        if (next == null || next.isEmpty()) {
            return previous;
        }
        return next;
    }

    private static void fetchBazaar(Map<String, Double> buy, Map<String, Double> sell) {
        JsonObject root = getJson(BAZAAR);
        if (root == null || !root.has("products") || !root.get("products").isJsonObject()) {
            return;
        }
        JsonObject products = root.getAsJsonObject("products");
        for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
            if (entry.getValue() == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject product = entry.getValue().getAsJsonObject();
            JsonObject status = product.has("quick_status") && product.get("quick_status").isJsonObject()
                    ? product.getAsJsonObject("quick_status")
                    : null;
            if (status == null) {
                continue;
            }
            String id = entry.getKey() == null ? "" : entry.getKey().toUpperCase(Locale.ROOT);
            putPositive(buy, id, number(status, "buyPrice"));
            putPositive(sell, id, number(status, "sellPrice"));
        }
    }

    private static void fetchItems(Map<String, Double> npc, Map<String, Double> motes) {
        JsonObject root = getJson(ITEMS);
        if (root == null || !root.has("items") || !root.get("items").isJsonArray()) {
            return;
        }
        for (JsonElement element : root.getAsJsonArray("items")) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String id = text(item, "id").toUpperCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }
            putPositive(npc, id, number(item, "npc_sell_price"));
            JsonObject rift = item.has("rift") && item.get("rift").isJsonObject()
                    ? item.getAsJsonObject("rift")
                    : null;
            if (rift != null) {
                putPositive(motes, id, number(rift, "motes_sell_price"));
            }
            putPositive(motes, id, number(item, "motes_sell_price"));
        }
    }

    private static void fetchLowestBin(Map<String, Double> bin) {
        for (URI uri : LOWEST_BIN) {
            JsonObject root = getJson(uri);
            if (root == null || root.entrySet().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String id = entry.getKey() == null ? "" : entry.getKey().toUpperCase(Locale.ROOT);
                putPositive(bin, id, number(entry.getValue()));
            }
            if (!bin.isEmpty()) {
                return;
            }
        }
    }

    private static JsonObject getJson(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "RotClient/2.0.0+mc26.2")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonElement parsed = JsonParser.parseString(response.body());
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static void putPositive(Map<String, Double> map, String id, double value) {
        if (id.isBlank() || !Double.isFinite(value) || value <= 0.0D) {
            return;
        }
        map.put(id, value);
    }

    private static String text(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static double number(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0.0D;
        }
        return number(object.get(key));
    }

    private static double number(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return 0.0D;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return 0.0D;
        }
    }
}
