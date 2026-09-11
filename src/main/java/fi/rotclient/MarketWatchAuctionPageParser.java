package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class MarketWatchAuctionPageParser {
    private MarketWatchAuctionPageParser() {
    }

    static MarketWatchAuctionPage parse(JsonObject root) {
        if (root == null) {
            return new MarketWatchAuctionPage(0, 0, 0, -1L, List.of());
        }

        List<MarketWatchAuction> auctions = new ArrayList<>();
        JsonArray array = array(root, "auctions");

        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }

                JsonObject auction = element.getAsJsonObject();

                auctions.add(new MarketWatchAuction(
                        text(auction, "uuid"),
                        text(auction, "item_name"),
                        text(auction, "category"),
                        text(auction, "tier"),
                        longNumber(auction, "start", 0L),
                        longNumber(auction, "end", 0L),
                        longNumber(auction, "starting_bid", 0L),
                        longNumber(auction, "highest_bid_amount", 0L),
                        bool(auction, "bin"),
                        itemBytes(auction),
                        text(auction, "auctioneer")));
            }
        }

        return new MarketWatchAuctionPage(
                intNumber(root, "page", 0),
                intNumber(root, "totalPages", 0),
                intNumber(root, "totalAuctions", auctions.size()),
                longNumber(root, "lastUpdated", -1L),
                auctions);
    }

    private static JsonArray array(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonArray()
                    ? object.getAsJsonArray(key)
                    : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String itemBytes(
            JsonObject auction) {

        if (auction == null
                || !auction.has("item_bytes")) {

            return "";
        }

        com.google.gson.JsonElement value =
                auction.get("item_bytes");

        if (value == null
                || value.isJsonNull()) {

            return "";
        }

        if (value.isJsonPrimitive()) {
            try {
                return value
                        .getAsString()
                        .trim();
            } catch (RuntimeException ignored) {
                return "";
            }
        }

        if (!value.isJsonObject()) {
            return "";
        }

        JsonObject object =
                value.getAsJsonObject();

        if (!object.has("data")) {
            return "";
        }

        com.google.gson.JsonElement data =
                object.get("data");

        if (data == null
                || data.isJsonNull()
                || !data.isJsonPrimitive()) {

            return "";
        }

        try {
            return data
                    .getAsString()
                    .trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
    private static String text(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString()
                    : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int intNumber(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long longNumber(JsonObject object, String key, long fallback) {
        try {
            return object.has(key) ? object.get(key).getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}