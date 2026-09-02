package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parsed official Hypixel static snapshots. Dynamic prices are intentionally
 * excluded; only product identity and item metadata belong here.
 */
final class SkyBlockOfficialSnapshot {
    record OfficialItem(
            String id,
            String name,
            String material,
            String category,
            Double npcSellPrice) {
        OfficialItem {
            id = SkyBlockItemId.normalize(id);
            if (id.isEmpty()) {
                throw new IllegalArgumentException("id cannot be blank");
            }
            name = name == null ? "" : name.trim();
            material = material == null ? "" : material.trim();
            category = category == null ? "" : category.trim();
        }
    }

    private final long lastUpdated;
    private final Map<String, OfficialItem> itemsById;
    private final Set<String> bazaarProductIds;

    SkyBlockOfficialSnapshot(
            long lastUpdated,
            Map<String, OfficialItem> itemsById,
            Set<String> bazaarProductIds) {
        this.lastUpdated = lastUpdated;
        Map<String, OfficialItem> items = new LinkedHashMap<>();
        if (itemsById != null) {
            items.putAll(itemsById);
        }
        this.itemsById = Collections.unmodifiableMap(items);
        LinkedHashSet<String> products = new LinkedHashSet<>();
        if (bazaarProductIds != null) {
            for (String id : bazaarProductIds) {
                String normalized = SkyBlockItemId.normalize(id);
                if (!normalized.isEmpty()) {
                    products.add(normalized);
                }
            }
        }
        this.bazaarProductIds = Collections.unmodifiableSet(products);
    }

    long lastUpdated() {
        return lastUpdated;
    }

    Map<String, OfficialItem> itemsById() {
        return itemsById;
    }

    Set<String> bazaarProductIds() {
        return bazaarProductIds;
    }

    Optional<OfficialItem> item(String id) {
        String normalized = SkyBlockItemId.normalize(id);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemsById.get(normalized));
    }

    boolean hasBazaarProduct(String productId) {
        String normalized = SkyBlockItemId.normalize(productId);
        return !normalized.isEmpty() && bazaarProductIds.contains(normalized);
    }

    static SkyBlockOfficialSnapshot parseItemsJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            throw new IllegalArgumentException("Hypixel items snapshot unsuccessful");
        }
        long lastUpdated = root.has("lastUpdated")
                ? root.get("lastUpdated").getAsLong()
                : 0L;
        Map<String, OfficialItem> items = new LinkedHashMap<>();
        JsonArray array = root.getAsJsonArray("items");
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("id")) {
                    continue;
                }
                String id = obj.get("id").getAsString();
                String name = obj.has("name") ? obj.get("name").getAsString() : "";
                String material = obj.has("material")
                        ? obj.get("material").getAsString()
                        : "";
                String category = obj.has("category")
                        ? obj.get("category").getAsString()
                        : "";
                Double npc = null;
                if (obj.has("npc_sell_price") && obj.get("npc_sell_price").isJsonPrimitive()) {
                    npc = obj.get("npc_sell_price").getAsDouble();
                }
                OfficialItem item = new OfficialItem(id, name, material, category, npc);
                items.put(item.id(), item);
            }
        }
        return new SkyBlockOfficialSnapshot(lastUpdated, items, Set.of());
    }

    static Set<String> parseBazaarProductIdsJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (root.has("productIds") && root.get("productIds").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("productIds")) {
                if (element != null && element.isJsonPrimitive()) {
                    String normalized = SkyBlockItemId.normalize(element.getAsString());
                    if (!normalized.isEmpty()) {
                        ids.add(normalized);
                    }
                }
            }
            return ids;
        }
        if (root.has("products") && root.get("products").isJsonObject()) {
            for (String key : root.getAsJsonObject("products").keySet()) {
                String normalized = SkyBlockItemId.normalize(key);
                if (!normalized.isEmpty()) {
                    ids.add(normalized);
                }
            }
        }
        return ids;
    }

    SkyBlockOfficialSnapshot withBazaarProductIds(Set<String> productIds) {
        return new SkyBlockOfficialSnapshot(lastUpdated, itemsById, productIds);
    }
}
