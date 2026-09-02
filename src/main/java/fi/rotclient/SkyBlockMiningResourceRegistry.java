package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Loads the reviewed mining-mechanics layer bundled with Rot Client. */
final class SkyBlockMiningResourceRegistry {
    static final String CLASSPATH =
            "assets/rotclient/data/mining-resources.v2.json";

    private static final SkyBlockMiningResourceRegistry BUNDLED = loadBundled();

    private final String schemaVersion;
    private final String reviewedAt;
    private final Map<String, String> sources;
    private final Map<String, SkyBlockMiningResource> resources;
    private final Map<String, SkyBlockMiningResource> aliases;

    private SkyBlockMiningResourceRegistry(
            String schemaVersion,
            String reviewedAt,
            Map<String, String> sources,
            List<SkyBlockMiningResource> resources) {
        this.schemaVersion = schemaVersion;
        this.reviewedAt = reviewedAt;
        this.sources = Map.copyOf(sources);
        Map<String, SkyBlockMiningResource> indexed = new LinkedHashMap<>();
        Map<String, SkyBlockMiningResource> aliasIndex = new LinkedHashMap<>();
        for (SkyBlockMiningResource resource : resources) {
            SkyBlockMiningResource previous = indexed.put(
                    resource.canonicalResourceId(), resource);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate mining resource " + resource.canonicalResourceId());
            }
            for (String ref : resource.provenanceRefs()) {
                if (!this.sources.containsKey(ref)) {
                    throw new IllegalArgumentException(
                            "Unknown provenance ref " + ref + " for "
                                    + resource.canonicalResourceId());
                }
            }
            indexAlias(aliasIndex, resource.canonicalResourceId(), resource);
            indexAlias(aliasIndex, resource.displayName(), resource);
            indexAlias(aliasIndex, resource.hypixelItemId(), resource);
            indexAlias(aliasIndex, resource.bazaarProductId(), resource);
            indexAlias(aliasIndex, resource.droppedItemId(), resource);
            for (String alias : resource.aliases()) {
                indexAlias(aliasIndex, alias, resource);
            }
            for (String alias : resource.sackAliases()) {
                indexAlias(aliasIndex, alias, resource);
            }
        }
        this.resources = Map.copyOf(indexed);
        this.aliases = Map.copyOf(aliasIndex);
    }

    static SkyBlockMiningResourceRegistry bundled() {
        return BUNDLED;
    }

    static SkyBlockMiningResourceRegistry parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        Map<String, String> sources = new LinkedHashMap<>();
        JsonObject sourceObject = root.getAsJsonObject("sources");
        if (sourceObject != null) {
            for (Map.Entry<String, JsonElement> entry : sourceObject.entrySet()) {
                sources.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        List<SkyBlockMiningResource> resources = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("resources");
        if (array != null) {
            for (JsonElement element : array) {
                if (element != null && element.isJsonObject()) {
                    resources.add(parseResource(element.getAsJsonObject()));
                }
            }
        }
        return new SkyBlockMiningResourceRegistry(
                text(root, "schemaVersion", "mining-resources.v2"),
                text(root, "reviewedAt", "unknown"),
                sources,
                resources);
    }

    Optional<SkyBlockMiningResource> lookup(String canonicalResourceId) {
        if (canonicalResourceId == null || canonicalResourceId.isBlank()) {
            return Optional.empty();
        }
        String normalized = canonicalResourceId.trim()
                .toUpperCase(Locale.ROOT).replace(' ', '_');
        return Optional.ofNullable(resources.get(normalized));
    }

    Optional<SkyBlockMiningResource> lookupAlias(String value) {
        String normalized = normalizeAlias(value);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(aliases.get(normalized));
    }

    List<SkyBlockMiningResource> resources() {
        return List.copyOf(resources.values());
    }

    String schemaVersion() {
        return schemaVersion;
    }

    String reviewedAt() {
        return reviewedAt;
    }

    Map<String, String> sources() {
        return sources;
    }

    private static void indexAlias(
            Map<String, SkyBlockMiningResource> index,
            String value,
            SkyBlockMiningResource resource) {
        String normalized = normalizeAlias(value);
        if (!normalized.isEmpty()) {
            index.putIfAbsent(normalized, resource);
        }
    }

    private static String normalizeAlias(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
    }

    private static SkyBlockMiningResourceRegistry loadBundled() {
        ClassLoader loader = SkyBlockMiningResourceRegistry.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(CLASSPATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + CLASSPATH);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(reader);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load " + CLASSPATH, ex);
        }
    }

    private static SkyBlockMiningResource parseResource(JsonObject obj) {
        return new SkyBlockMiningResource(
                text(obj, "canonicalResourceId", ""),
                text(obj, "displayName", ""),
                text(obj, "kind", "UNKNOWN"),
                nullableText(obj, "collectionId"),
                nullableText(obj, "hypixelItemId"),
                nullableText(obj, "bazaarProductId"),
                nullableText(obj, "droppedItemId"),
                stringList(obj, "aliases"),
                stringList(obj, "sackAliases"),
                stringList(obj, "physicalBlockTokens"),
                nullableText(obj, "enchantedItemId"),
                nullableText(obj, "enchantedBlockItemId"),
                stringList(obj, "areas"),
                enumValue(obj, "fortuneCategory", MiningFortuneCategory.UNKNOWN),
                integer(obj, "breakingPower"),
                integer(obj, "blockStrength"),
                longValue(obj, "baseYieldMin"),
                longValue(obj, "baseYieldMax"),
                bool(obj, "requiresAreaContext"),
                nullableText(obj, "gemstoneFamily"),
                nullableText(obj, "gemstoneTier"),
                text(obj, "confidence", "UNKNOWN"),
                text(obj, "evidenceState", "RESEARCHED"),
                stringList(obj, "provenanceRefs"),
                stringList(obj, "notes"));
    }

    private static <E extends Enum<E>> E enumValue(
            JsonObject obj, String key, E fallback) {
        String value = nullableText(obj, key);
        if (value == null) return fallback;
        try {
            return Enum.valueOf(fallback.getDeclaringClass(),
                    value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Integer integer(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsInt() : null;
    }

    private static Long longValue(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsLong() : null;
    }

    private static boolean bool(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                && obj.get(key).getAsBoolean();
    }

    private static String text(JsonObject obj, String key, String fallback) {
        String value = nullableText(obj, key);
        return value == null ? fallback : value;
    }

    private static String nullableText(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        String value = obj.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static List<String> stringList(JsonObject obj, String key) {
        List<String> values = new ArrayList<>();
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return values;
        for (JsonElement element : obj.getAsJsonArray(key)) {
            if (element != null && element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }
}
