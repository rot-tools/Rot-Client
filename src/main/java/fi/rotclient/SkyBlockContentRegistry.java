package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Data-driven SkyBlock item registry. Unknown lookups return empty Optionals
 * and never throw. Duplicate ids/aliases are rejected at construction.
 * Catalog enrichment is never an observation allow-list.
 */
final class SkyBlockContentRegistry {
    record KnownItem(
            String id,
            String displayName,
            List<String> aliases,
            String bazaarProductId,
            List<String> areas,
            String sourceHint,
            String hypixelItemId) {
        KnownItem(
                String id,
                String displayName,
                List<String> aliases,
                String bazaarProductId,
                List<String> areas,
                String sourceHint) {
            this(id, displayName, aliases, bazaarProductId, areas, sourceHint, id);
        }

        KnownItem {
            id = SkyBlockItemId.normalize(id);
            if (id.isEmpty()) {
                throw new IllegalArgumentException("KnownItem id cannot be blank");
            }
            displayName = displayName == null || displayName.isBlank()
                    ? id
                    : displayName.trim();
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            bazaarProductId = bazaarProductId == null || bazaarProductId.isBlank()
                    ? null
                    : SkyBlockItemId.normalize(bazaarProductId);
            areas = areas == null ? List.of() : List.copyOf(areas);
            sourceHint = sourceHint == null || sourceHint.isBlank()
                    ? null
                    : sourceHint.trim().toUpperCase(Locale.ROOT);
            hypixelItemId = hypixelItemId == null || hypixelItemId.isBlank()
                    ? id
                    : SkyBlockItemId.normalize(hypixelItemId);
        }
    }

    private static final SkyBlockContentRegistry BUILTIN = createBuiltin();

    private final Map<String, KnownItem> byId;
    private final Map<String, KnownItem> byAlias;

    SkyBlockContentRegistry(List<KnownItem> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("Definitions cannot be null");
        }
        Map<String, KnownItem> ids = new LinkedHashMap<>();
        Map<String, KnownItem> aliases = new LinkedHashMap<>();
        for (KnownItem item : definitions) {
            if (item == null) {
                throw new IllegalArgumentException("KnownItem cannot be null");
            }
            if (ids.put(item.id(), item) != null) {
                throw new IllegalArgumentException(
                        "Duplicate item id: " + item.id());
            }
            Set<String> seenAliases = new LinkedHashSet<>();
            for (String alias : item.aliases()) {
                String normalized = normalizeAlias(alias);
                if (normalized.isEmpty()) {
                    continue;
                }
                if (!seenAliases.add(normalized)) {
                    throw new IllegalArgumentException(
                            "Duplicate alias within item " + item.id()
                                    + ": " + alias);
                }
                if (aliases.put(normalized, item) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate item alias: " + alias);
                }
            }
            // Id itself is also a lookup key via lookup(), not alias map.
        }
        this.byId = Collections.unmodifiableMap(ids);
        this.byAlias = Collections.unmodifiableMap(aliases);
    }

    static SkyBlockContentRegistry builtin() {
        return BUILTIN;
    }

    /**
     * Handwritten builtin definitions used as domain-rule seed and offline
     * fallback when the bundled canonical dataset is absent.
     */
    static List<KnownItem> builtinKnownItems() {
        return builtInDefinitions();
    }

    static SkyBlockContentRegistry fromCanonical(SkyBlockCanonicalDataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("dataset cannot be null");
        }
        List<KnownItem> items = new ArrayList<>();
        for (SkyBlockCanonicalItem item : dataset.items()) {
            items.add(item.toKnownItem());
        }
        return new SkyBlockContentRegistry(items);
    }

    private static SkyBlockContentRegistry createBuiltin() {
        Optional<SkyBlockCanonicalDataset> bundled =
                SkyBlockCanonicalDatasetLoader.loadBundled();
        if (bundled.isPresent() && !bundled.get().isEmpty()) {
            return fromCanonical(bundled.get());
        }
        return new SkyBlockContentRegistry(builtInDefinitions());
    }

    Optional<KnownItem> lookup(String id) {
        String normalized = SkyBlockItemId.normalize(id);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(normalized));
    }

    Optional<KnownItem> lookupAlias(String name) {
        String normalized = normalizeAlias(name);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        KnownItem byAliasHit = byAlias.get(normalized);
        if (byAliasHit != null) {
            return Optional.of(byAliasHit);
        }
        return lookup(normalized);
    }

    boolean isKnown(String id) {
        return lookup(id).isPresent();
    }

    Set<String> allIds() {
        return byId.keySet();
    }

    private static String normalizeAlias(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
    }

    private static List<KnownItem> builtInDefinitions() {
        List<KnownItem> items = new ArrayList<>();
        addMaterial(items, "GOLD_INGOT", "Gold Ingot", "GOLD_INGOT",
                List.of("Gold Ingot", "Gold Ingots", "Pure Gold"));
        addMaterial(items, "ENCHANTED_GOLD", "Enchanted Gold", "ENCHANTED_GOLD",
                List.of("Enchanted Gold", "Enchanted Gold Ingot"));
        addMaterial(items, "DIAMOND", "Diamond", "DIAMOND",
                List.of("Diamond", "Diamonds", "Pure Diamond"));
        addMaterial(items, "ENCHANTED_DIAMOND", "Enchanted Diamond",
                "ENCHANTED_DIAMOND", List.of("Enchanted Diamond"));
        addMaterial(items, "MITHRIL_ORE", "Mithril", "MITHRIL_ORE",
                List.of("Mithril"));
        addMaterial(items, "ENCHANTED_MITHRIL", "Enchanted Mithril",
                "ENCHANTED_MITHRIL", List.of("Enchanted Mithril"));
        addMaterial(items, "TITANIUM", "Titanium", "TITANIUM_ORE",
                List.of("Titanium"),
                List.of(
                        SkyBlockArea.DWARVEN_MINES.id(),
                        SkyBlockArea.CRYSTAL_HOLLOWS.id()),
                "TITANIUM_ORE");
        addMaterial(items, "ENCHANTED_TITANIUM", "Enchanted Titanium",
                "ENCHANTED_TITANIUM", List.of("Enchanted Titanium"));
        addMaterial(items, "HARD_STONE", "Hard Stone", "HARD_STONE",
                List.of("Hard Stone"));
        addMaterial(items, "ENCHANTED_HARD_STONE", "Enchanted Hard Stone",
                "ENCHANTED_HARD_STONE", List.of("Enchanted Hard Stone"));
        addMaterial(items, "COBBLESTONE", "Cobblestone", "COBBLESTONE",
                List.of("Cobblestone"));
        addMaterial(items, "ENCHANTED_COBBLESTONE", "Enchanted Cobblestone",
                "ENCHANTED_COBBLESTONE", List.of("Enchanted Cobblestone"));
        // Deep Caverns / vanilla-looking ores — research valuation-index
        // uses COAL_ORE / IRON_ORE as SkyBlock item ids with bazaar product
        // aliases COAL / IRON_INGOT. Do NOT treat vanilla registry ids alone
        // as Hypixel canonical identity.
        addMaterial(items, "COAL_ORE", "Coal", "COAL",
                List.of("Coal", "Coal Ore"));
        addMaterial(items, "ENCHANTED_COAL", "Enchanted Coal",
                "ENCHANTED_COAL", List.of("Enchanted Coal"));
        addMaterial(items, "IRON_ORE", "Iron Ingot", "IRON_INGOT",
                List.of("Iron", "Iron Ingot", "Iron Ore"));
        addMaterial(items, "ENCHANTED_IRON", "Enchanted Iron",
                "ENCHANTED_IRON", List.of("Enchanted Iron", "Enchanted Iron Ingot"));
        addMaterial(items, "LAPIS_LAZULI", "Lapis Lazuli", "INK_SACK:4",
                List.of("Lapis Lazuli", "Lapis"),
                List.of(
                        SkyBlockArea.DWARVEN_MINES.id(),
                        SkyBlockArea.CRYSTAL_HOLLOWS.id()),
                "INK_SACK:4");
        addMaterial(items, "ENCHANTED_LAPIS_LAZULI", "Enchanted Lapis Lazuli",
                "ENCHANTED_LAPIS_LAZULI",
                List.of("Enchanted Lapis Lazuli", "Enchanted Lapis"));
        addMaterial(items, "REDSTONE", "Redstone", "REDSTONE",
                List.of("Redstone", "Redstone Dust"));
        addMaterial(items, "ENCHANTED_REDSTONE", "Enchanted Redstone",
                "ENCHANTED_REDSTONE", List.of("Enchanted Redstone"));
        // STONE as a bare token stays unresolved for Hard Stone — Hard Stone
        // has its own verified id. Do not alias STONE → HARD_STONE.
        addMaterial(items, "TUNGSTEN", "Tungsten", "TUNGSTEN",
                List.of("Tungsten"),
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id()));
        addMaterial(items, "ENCHANTED_TUNGSTEN", "Enchanted Tungsten",
                "ENCHANTED_TUNGSTEN", List.of("Enchanted Tungsten"),
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id()));
        addMaterial(items, "UMBER", "Umber", "UMBER",
                List.of("Umber"),
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id()));
        addMaterial(items, "ENCHANTED_UMBER", "Enchanted Umber",
                "ENCHANTED_UMBER", List.of("Enchanted Umber"),
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id()));
        addMaterial(items, "GLACITE", "Glacite", "GLACITE",
                List.of("Glacite"),
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id(),
                        SkyBlockArea.GREAT_GLACITE_LAKE.id()));
        addMaterial(items, "ENCHANTED_GLACITE", "Enchanted Glacite",
                "ENCHANTED_GLACITE", List.of("Enchanted Glacite"),
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id(),
                        SkyBlockArea.GREAT_GLACITE_LAKE.id()));

        String[] gems = {
                "RUBY", "AMBER", "SAPPHIRE", "JADE", "AMETHYST", "TOPAZ",
                "JASPER", "OPAL", "ONYX", "AQUAMARINE", "CITRINE", "PERIDOT"
        };
        String[] gemNames = {
                "Ruby", "Amber", "Sapphire", "Jade", "Amethyst", "Topaz",
                "Jasper", "Opal", "Onyx", "Aquamarine", "Citrine", "Peridot"
        };
        String[] tiers = {"ROUGH", "FLAWED", "FINE", "FLAWLESS", "PERFECT"};
        String[] tierNames = {
                "Rough", "Flawed", "Fine", "Flawless", "Perfect"
        };
        for (int g = 0; g < gems.length; g++) {
            for (int t = 0; t < tiers.length; t++) {
                String id = tiers[t] + "_" + gems[g] + "_GEM";
                String display = tierNames[t] + " " + gemNames[g] + " Gemstone";
                items.add(new KnownItem(
                        id,
                        display,
                        List.of(display, tierNames[t] + " " + gemNames[g]),
                        id,
                        List.of(
                                SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                                SkyBlockArea.DWARVEN_MINES.id()),
                        SessionSourceType.MINING.name()));
            }
        }

        items.add(new KnownItem(
                "MITHRIL_POWDER",
                "Mithril Powder",
                List.of("Mithril Powder"),
                null,
                List.of(SkyBlockArea.DWARVEN_MINES.id()),
                SessionSourceType.CURRENCY.name()));
        items.add(new KnownItem(
                "GEMSTONE_POWDER",
                "Gemstone Powder",
                List.of("Gemstone Powder"),
                null,
                List.of(SkyBlockArea.CRYSTAL_HOLLOWS.id()),
                SessionSourceType.CURRENCY.name()));
        items.add(new KnownItem(
                "GLACITE_POWDER",
                "Glacite Powder",
                List.of("Glacite Powder"),
                null,
                List.of(
                        SkyBlockArea.GLACITE_TUNNELS.id(),
                        SkyBlockArea.GLACITE_MINESHAFT.id()),
                SessionSourceType.CURRENCY.name()));
        return List.copyOf(items);
    }

    private static void addMaterial(
            List<KnownItem> items,
            String id,
            String display,
            String bazaarId,
            List<String> aliases) {
        addMaterial(
                items,
                id,
                display,
                bazaarId,
                aliases,
                List.of(
                        SkyBlockArea.DWARVEN_MINES.id(),
                        SkyBlockArea.CRYSTAL_HOLLOWS.id()),
                id);
    }

    private static void addMaterial(
            List<KnownItem> items,
            String id,
            String display,
            String bazaarId,
            List<String> aliases,
            List<String> areas) {
        addMaterial(items, id, display, bazaarId, aliases, areas, id);
    }

    private static void addMaterial(
            List<KnownItem> items,
            String id,
            String display,
            String bazaarId,
            List<String> aliases,
            List<String> areas,
            String hypixelItemId) {
        items.add(new KnownItem(
                id,
                display,
                aliases,
                bazaarId,
                areas,
                SessionSourceType.MINING.name(),
                hypixelItemId));
    }
}
