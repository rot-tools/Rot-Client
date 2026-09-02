package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Minimal official Mining Collection identity snapshot. */
final class SkyBlockOfficialCollectionsSnapshot {
    private final long lastUpdated;
    private final Set<String> miningCollectionIds;

    private SkyBlockOfficialCollectionsSnapshot(
            long lastUpdated,
            Set<String> miningCollectionIds) {
        this.lastUpdated = Math.max(0L, lastUpdated);
        this.miningCollectionIds = Collections.unmodifiableSet(
                new LinkedHashSet<>(miningCollectionIds));
    }

    static SkyBlockOfficialCollectionsSnapshot parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            throw new IllegalArgumentException(
                    "Hypixel collections snapshot unsuccessful");
        }
        long lastUpdated = root.has("lastUpdated")
                ? root.get("lastUpdated").getAsLong()
                : 0L;
        Set<String> ids = new LinkedHashSet<>();
        JsonObject collections = root.getAsJsonObject("collections");
        JsonObject mining = collections == null
                ? null
                : collections.getAsJsonObject("MINING");
        JsonObject items = mining == null ? null : mining.getAsJsonObject("items");
        if (items != null) {
            for (String key : items.keySet()) {
                JsonElement value = items.get(key);
                if (value != null && value.isJsonObject()) {
                    String normalized = SkyBlockItemId.normalize(key);
                    if (!normalized.isEmpty()) ids.add(normalized);
                }
            }
        }
        return new SkyBlockOfficialCollectionsSnapshot(lastUpdated, ids);
    }

    long lastUpdated() {
        return lastUpdated;
    }

    Set<String> miningCollectionIds() {
        return miningCollectionIds;
    }
}
