package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loads and writes Rot Client canonical SkyBlock data snapshots. Runtime uses
 * classpath resources only; refresh tooling writes files under resources/.
 */
final class SkyBlockCanonicalDatasetLoader {
    static final String DATA_ROOT = "assets/rotclient/data/";
    static final String MANIFEST = DATA_ROOT + "manifest.v1.json";
    static final String DOMAIN_RULES = DATA_ROOT + "domain-rules.v1.json";
    static final String CANONICAL_ITEMS = DATA_ROOT + "canonical-items.v1.json";
    static final String BAZAAR_INDEX = DATA_ROOT + "bazaar-product-index.v1.json";
    static final String CONFLICTS = DATA_ROOT + "conflicts.v1.json";
    static final String PROVENANCE = DATA_ROOT + "provenance.v1.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SkyBlockCanonicalDatasetLoader() {
    }

    static Optional<SkyBlockCanonicalDataset> loadBundled() {
        try (InputStream in = open(CANONICAL_ITEMS)) {
            if (in == null) {
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                SkyBlockCanonicalDataset dataset = parseCanonical(reader);
                Set<String> bazaar = loadBundledBazaarIndex().orElse(Set.of());
                List<SkyBlockDataConflict> conflicts =
                        loadBundledConflicts().orElse(List.of());
                if (!bazaar.isEmpty() || !conflicts.isEmpty()) {
                    return Optional.of(new SkyBlockCanonicalDataset(
                            dataset.schemaVersion(),
                            dataset.generatedAt(),
                            dataset.items(),
                            bazaar.isEmpty() ? dataset.bazaarProductIds() : bazaar,
                            conflicts.isEmpty() ? dataset.conflicts() : conflicts,
                            dataset.datasetProvenance()));
                }
                return Optional.of(dataset);
            }
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    static SkyBlockDomainRules loadDomainRulesOrSeed() {
        try (InputStream in = open(DOMAIN_RULES)) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return parseDomainRules(reader);
                }
            }
        } catch (IOException ignored) {
            // Fall through to seed.
        }
        return SkyBlockDomainRules.fromBuiltinRegistry();
    }

    static SkyBlockDomainRules parseDomainRules(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        String schema = text(root, "schemaVersion", "domain-rules.v1");
        List<SkyBlockDomainRules.Rule> rules = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("rules");
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                rules.add(new SkyBlockDomainRules.Rule(
                        text(obj, "stableId", ""),
                        text(obj, "displayName", ""),
                        stringList(obj, "aliases"),
                        nullableText(obj, "hypixelItemIdHint"),
                        nullableText(obj, "bazaarProductIdHint"),
                        stringList(obj, "areas"),
                        nullableText(obj, "sourceHint"),
                        stringList(obj, "notes")));
            }
        }
        return new SkyBlockDomainRules(schema, rules);
    }

    static SkyBlockCanonicalDataset parseCanonical(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        String schema = text(root, "schemaVersion", "canonical-items.v1");
        String generatedAt = text(root, "generatedAt", "");
        List<SkyBlockCanonicalItem> items = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("items");
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                items.add(parseItem(element.getAsJsonObject()));
            }
        }
        Set<String> bazaar = new LinkedHashSet<>(stringList(root, "bazaarProductIds"));
        List<SkyBlockDataConflict> conflicts = parseConflicts(root.getAsJsonArray("conflicts"));
        List<SkyBlockProvenance> provenance = parseProvenance(root.getAsJsonArray("provenance"));
        return new SkyBlockCanonicalDataset(
                schema, generatedAt, items, bazaar, conflicts, provenance);
    }

    static void writeDatasetBundle(Path dataDir, SkyBlockCanonicalDataset dataset)
            throws IOException {
        Files.createDirectories(dataDir);
        SkyBlockDomainRules domain = SkyBlockDomainRules.fromBuiltinRegistry();
        writeJson(dataDir.resolve("domain-rules.v1.json"), toDomainJson(domain));
        writeJson(dataDir.resolve("canonical-items.v1.json"), toCanonicalJson(dataset));
        writeJson(dataDir.resolve("bazaar-product-index.v1.json"), toBazaarIndexJson(dataset));
        writeJson(dataDir.resolve("conflicts.v1.json"), toConflictsJson(dataset));
        writeJson(dataDir.resolve("provenance.v1.json"), toProvenanceJson(dataset));
        writeJson(dataDir.resolve("manifest.v1.json"), toManifestJson(dataset));
    }

    private static Optional<Set<String>> loadBundledBazaarIndex() throws IOException {
        try (InputStream in = open(BAZAAR_INDEX)) {
            if (in == null) {
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return Optional.of(new LinkedHashSet<>(stringList(root, "productIds")));
            }
        }
    }

    private static Optional<List<SkyBlockDataConflict>> loadBundledConflicts()
            throws IOException {
        try (InputStream in = open(CONFLICTS)) {
            if (in == null) {
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return Optional.of(parseConflicts(root.getAsJsonArray("conflicts")));
            }
        }
    }

    private static SkyBlockCanonicalItem parseItem(JsonObject obj) {
        return new SkyBlockCanonicalItem(
                text(obj, "stableId", ""),
                text(obj, "displayName", ""),
                stringList(obj, "aliases"),
                nullableText(obj, "hypixelItemId"),
                nullableText(obj, "bazaarProductId"),
                stringList(obj, "areas"),
                nullableText(obj, "sourceHint"),
                text(obj, "idAuthority", "ROT_DOMAIN_RULE"),
                text(obj, "bazaarAuthority", "ROT_DOMAIN_RULE"),
                text(obj, "confidence", "MEDIUM"),
                parseProvenance(obj.getAsJsonArray("provenance")),
                stringList(obj, "notes"));
    }

    private static List<SkyBlockDataConflict> parseConflicts(JsonArray array) {
        List<SkyBlockDataConflict> conflicts = new ArrayList<>();
        if (array == null) {
            return conflicts;
        }
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            conflicts.add(new SkyBlockDataConflict(
                    text(obj, "stableId", ""),
                    text(obj, "field", ""),
                    text(obj, "officialValue", ""),
                    text(obj, "otherValue", ""),
                    text(obj, "otherAuthority", "UNKNOWN"),
                    text(obj, "resolution", "SURFACED"),
                    text(obj, "notes", "")));
        }
        return conflicts;
    }

    private static List<SkyBlockProvenance> parseProvenance(JsonArray array) {
        List<SkyBlockProvenance> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            list.add(new SkyBlockProvenance(
                    text(obj, "url", ""),
                    text(obj, "sourceName", "unknown"),
                    text(obj, "sourceType", "OTHER"),
                    text(obj, "retrievedAt", "unknown"),
                    text(obj, "confidence", "UNKNOWN"),
                    text(obj, "notes", "")));
        }
        return list;
    }

    private static JsonObject toDomainJson(SkyBlockDomainRules domain) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", domain.schemaVersion());
        root.addProperty(
                "description",
                "Handwritten Rot Client domain rules. Separate from generated upstream metadata.");
        JsonArray rules = new JsonArray();
        for (SkyBlockDomainRules.Rule rule : domain.rules()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("stableId", rule.stableId());
            obj.addProperty("displayName", rule.displayName());
            obj.add("aliases", toStringArray(rule.aliases()));
            if (rule.hypixelItemIdHint() != null) {
                obj.addProperty("hypixelItemIdHint", rule.hypixelItemIdHint());
            }
            if (rule.bazaarProductIdHint() != null) {
                obj.addProperty("bazaarProductIdHint", rule.bazaarProductIdHint());
            }
            obj.add("areas", toStringArray(rule.areas()));
            if (rule.sourceHint() != null) {
                obj.addProperty("sourceHint", rule.sourceHint());
            }
            obj.add("notes", toStringArray(rule.notes()));
            rules.add(obj);
        }
        root.add("rules", rules);
        return root;
    }

    private static JsonObject toCanonicalJson(SkyBlockCanonicalDataset dataset) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", dataset.schemaVersion());
        root.addProperty("generatedAt", dataset.generatedAt());
        JsonArray items = new JsonArray();
        for (SkyBlockCanonicalItem item : dataset.items()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("stableId", item.stableId());
            obj.addProperty("displayName", item.displayName());
            obj.add("aliases", toStringArray(item.aliases()));
            if (item.hypixelItemId() != null) {
                obj.addProperty("hypixelItemId", item.hypixelItemId());
            }
            if (item.bazaarProductId() != null) {
                obj.addProperty("bazaarProductId", item.bazaarProductId());
            }
            obj.add("areas", toStringArray(item.areas()));
            if (item.sourceHint() != null) {
                obj.addProperty("sourceHint", item.sourceHint());
            }
            obj.addProperty("idAuthority", item.idAuthority());
            obj.addProperty("bazaarAuthority", item.bazaarAuthority());
            obj.addProperty("confidence", item.confidence());
            obj.add("provenance", toProvenanceArray(item.provenance()));
            obj.add("notes", toStringArray(item.notes()));
            items.add(obj);
        }
        root.add("items", items);
        return root;
    }

    private static JsonObject toBazaarIndexJson(SkyBlockCanonicalDataset dataset) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", "bazaar-product-index.v1");
        root.addProperty("generatedAt", dataset.generatedAt());
        root.addProperty(
                "description",
                "Static Bazaar product ID presence at refresh time. Not market prices.");
        root.add("productIds", toStringArray(dataset.bazaarProductIds()));
        return root;
    }

    private static JsonObject toConflictsJson(SkyBlockCanonicalDataset dataset) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", "conflicts.v1");
        root.addProperty("generatedAt", dataset.generatedAt());
        JsonArray array = new JsonArray();
        for (SkyBlockDataConflict conflict : dataset.conflicts()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("stableId", conflict.stableId());
            obj.addProperty("field", conflict.field());
            obj.addProperty("officialValue", conflict.officialValue());
            obj.addProperty("otherValue", conflict.otherValue());
            obj.addProperty("otherAuthority", conflict.otherAuthority());
            obj.addProperty("resolution", conflict.resolution());
            obj.addProperty("notes", conflict.notes());
            array.add(obj);
        }
        root.add("conflicts", array);
        return root;
    }

    private static JsonObject toProvenanceJson(SkyBlockCanonicalDataset dataset) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", "provenance.v1");
        root.addProperty("generatedAt", dataset.generatedAt());
        root.add("dataset", toProvenanceArray(dataset.datasetProvenance()));
        return root;
    }

    private static JsonObject toManifestJson(SkyBlockCanonicalDataset dataset) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", "manifest.v1");
        root.addProperty("generatedAt", dataset.generatedAt());
        root.addProperty("canonicalItems", "canonical-items.v1.json");
        root.addProperty("domainRules", "domain-rules.v1.json");
        root.addProperty("bazaarProductIndex", "bazaar-product-index.v1.json");
        root.addProperty("conflicts", "conflicts.v1.json");
        root.addProperty("provenance", "provenance.v1.json");
        root.addProperty("miningResources", "mining-resources.v2.json");
        root.addProperty("itemCount", dataset.items().size());
        root.addProperty("conflictCount", dataset.conflicts().size());
        root.addProperty("bazaarProductCount", dataset.bazaarProductIds().size());
        root.addProperty(
                "valuationPolicy",
                "BAZAAR_INSTANT_SELL_GROSS_NO_BUY_SIDE");
        root.addProperty(
                "observationPolicy",
                "CATALOG_IS_NOT_AN_ALLOW_LIST");
        return root;
    }

    private static JsonArray toProvenanceArray(List<SkyBlockProvenance> list) {
        JsonArray array = new JsonArray();
        for (SkyBlockProvenance provenance : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("url", provenance.url());
            obj.addProperty("sourceName", provenance.sourceName());
            obj.addProperty("sourceType", provenance.sourceType());
            obj.addProperty("retrievedAt", provenance.retrievedAt());
            obj.addProperty("confidence", provenance.confidence());
            obj.addProperty("notes", provenance.notes());
            array.add(obj);
        }
        return array;
    }

    private static JsonArray toStringArray(Iterable<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            if (value != null) {
                array.add(value);
            }
        }
        return array;
    }

    private static void writeJson(Path path, JsonObject root) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("JSON target has no parent: " + path);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(
                parent, absolute.getFileName().toString() + ".", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(
                    temporary,
                    GSON.toJson(root) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        absolute,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporary,
                        absolute,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static InputStream open(String classpath) {
        ClassLoader loader = SkyBlockCanonicalDatasetLoader.class.getClassLoader();
        return loader.getResourceAsStream(classpath);
    }

    private static String text(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        return obj.get(key).getAsString();
    }

    private static String nullableText(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        String value = obj.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static List<String> stringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return list;
        }
        for (JsonElement element : obj.getAsJsonArray(key)) {
            if (element != null && element.isJsonPrimitive()) {
                list.add(element.getAsString());
            }
        }
        return list;
    }
}
