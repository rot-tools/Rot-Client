package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable exact-identity catalog for the diagnostic mining shadow. */
final class MiningResourceCatalog {
    private final List<ResourceDefinition> definitions;
    private final Map<TrackedMaterial, ResourceDefinition> rawMaterials;
    private final Map<GemstoneKey, ResourceDefinition> gemstones;
    private final Map<String, ResourceDefinition> exactAliases;

    MiningResourceCatalog() {
        this(defaultDefinitions());
    }

    MiningResourceCatalog(List<ResourceDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("Definitions cannot be null");
        }

        List<ResourceDefinition> copied = List.copyOf(definitions);
        Map<TrackedMaterial, ResourceDefinition> materialMap =
                new EnumMap<>(TrackedMaterial.class);
        Map<GemstoneKey, ResourceDefinition> gemstoneMap = new HashMap<>();
        Map<String, ResourceDefinition> aliasMap = new HashMap<>();

        for (ResourceDefinition definition : copied) {
            if (definition == null) {
                throw new IllegalArgumentException(
                        "Resource definition cannot be null");
            }
            if (definition.material() != null && definition.rawMaterialIdentity()) {
                ResourceDefinition previous = materialMap.put(
                        definition.material(), definition);
                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Duplicate raw material identity: "
                                    + definition.material().id());
                }
            }
            if (definition.gemstone() != null) {
                GemstoneKey key = new GemstoneKey(
                        definition.gemstone(), definition.gemstoneTier());
                if (gemstoneMap.put(key, definition) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate gemstone identity: "
                                    + definition.resource().resourceId());
                }
            }
            for (String alias : definition.aliases()) {
                String normalized = normalizeAlias(alias);
                if (normalized.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Resource alias cannot be blank");
                }
                ResourceDefinition previous = aliasMap.put(
                        normalized, definition);
                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Duplicate resource alias: " + alias);
                }
            }
        }

        this.definitions = copied;
        this.rawMaterials = Collections.unmodifiableMap(materialMap);
        this.gemstones = Collections.unmodifiableMap(gemstoneMap);
        this.exactAliases = Collections.unmodifiableMap(aliasMap);
    }

    Optional<ResourceDefinition> fromMaterial(TrackedMaterial material) {
        return Optional.ofNullable(rawMaterials.get(material));
    }

    Optional<ResourceDefinition> fromGemstone(
            GemstoneType gemstone,
            GemstoneTier tier) {
        if (gemstone == null || tier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(gemstones.get(new GemstoneKey(gemstone, tier)));
    }

    Optional<ResourceDefinition> fromExactSackItem(String itemName) {
        String normalized = normalizeAlias(itemName);
        return normalized.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(exactAliases.get(normalized));
    }

    Optional<MiningSessionDirectBreakTracker.FamilyKey> familyFor(
            TrackedMaterial material) {
        return fromMaterial(material)
                .filter(ResourceDefinition::directBreakFamilySupported)
                .map(ResourceDefinition::family);
    }

    Optional<MiningSessionDirectBreakTracker.FamilyKey> familyFor(
            GemstoneType gemstone) {
        if (gemstone == null) {
            return Optional.empty();
        }
        return Optional.of(
                MiningSessionDirectBreakTracker.FamilyKey.gemstone(gemstone));
    }

    List<ResourceDefinition> definitions() {
        return definitions;
    }

    private static List<ResourceDefinition> defaultDefinitions() {
        List<ResourceDefinition> result = new ArrayList<>();

        addMaterial(result, TrackedMaterial.COAL, true);
        addMaterial(result, TrackedMaterial.IRON, true);
        addMaterial(result, TrackedMaterial.GOLD, true);
        addMaterial(result, TrackedMaterial.LAPIS, true);
        addMaterial(result, TrackedMaterial.REDSTONE, true);
        addMaterial(result, TrackedMaterial.EMERALD, true);
        addMaterial(result, TrackedMaterial.DIAMOND, true);
        addMaterial(result, TrackedMaterial.QUARTZ, true);
        addMaterial(result, TrackedMaterial.MITHRIL, true);
        addMaterial(result, TrackedMaterial.TITANIUM, true);
        addMaterial(result, TrackedMaterial.TUNGSTEN, true);
        addMaterial(result, TrackedMaterial.UMBER, true);
        addMaterial(result, TrackedMaterial.HARD_STONE, true);
        addMaterial(result, TrackedMaterial.COBBLESTONE, true);

        for (GemstoneType gemstone : GemstoneType.values()) {
            for (GemstoneTier tier : GemstoneTier.values()) {
                boolean accepted = tier == GemstoneTier.ROUGH
                        || tier == GemstoneTier.FLAWED;
                MiningSessionResource resource = MiningSessionResource.gemstone(
                        gemstone.bazaarId(tier),
                        gemstone.itemName(tier),
                        gemstone,
                        tier);
                result.add(new ResourceDefinition(
                        resource,
                        null,
                        gemstone,
                        tier,
                        MiningSessionDirectBreakTracker.FamilyKey.gemstone(gemstone),
                        true,
                        true,
                        accepted,
                        false,
                        Set.of(gemstone.itemName(tier))));
            }
        }

        return List.copyOf(result);
    }

    private static void addMaterial(
            List<ResourceDefinition> result,
            TrackedMaterial material,
            boolean runtimeSupported) {
        MiningSessionDirectBreakTracker.FamilyKey family =
                MiningSessionDirectBreakTracker.FamilyKey.material(material);

        result.add(new ResourceDefinition(
                MiningSessionResource.material(
                        material.rawBazaarId(),
                        material.rawItemName(),
                        material),
                material,
                null,
                null,
                family,
                runtimeSupported,
                runtimeSupported,
                runtimeSupported,
                true,
                aliasesFor(material, TrackedMaterial.ItemTier.RAW)));

        result.add(new ResourceDefinition(
                MiningSessionResource.material(
                        material.enchantedBazaarId(),
                        material.enchantedItemName(),
                        material),
                material,
                null,
                null,
                family,
                runtimeSupported,
                runtimeSupported,
                runtimeSupported,
                false,
                aliasesFor(material, TrackedMaterial.ItemTier.ENCHANTED)));

        if (material.enchantedBlockItemName() != null) {
            result.add(new ResourceDefinition(
                MiningSessionResource.material(
                            material.enchantedBlockBazaarId(),
                            material.enchantedBlockItemName(),
                            material),
                    material,
                    null,
                    null,
                    family,
                    runtimeSupported,
                    runtimeSupported,
                    runtimeSupported,
                    false,
                    Set.of(material.enchantedBlockItemName())));
        }
    }

    private static Set<String> aliasesFor(
            TrackedMaterial material,
            TrackedMaterial.ItemTier tier) {
        return material.exactItemNames(tier);
    }

    static String normalizeAlias(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    record ResourceDefinition(
            MiningSessionResource resource,
            TrackedMaterial material,
            GemstoneType gemstone,
            GemstoneTier gemstoneTier,
            MiningSessionDirectBreakTracker.FamilyKey family,
            boolean exactQuantitySupported,
            boolean directBreakFamilySupported,
            boolean shadowAcceptanceSupported,
            boolean rawMaterialIdentity,
            Set<String> aliases) {
        ResourceDefinition {
            if (resource == null || family == null || aliases == null) {
                throw new IllegalArgumentException(
                        "Resource definition fields cannot be null");
            }
            if ((material == null) == (gemstone == null)) {
                throw new IllegalArgumentException(
                        "Definition must identify one material or gemstone");
            }
            if ((gemstone == null) != (gemstoneTier == null)) {
                throw new IllegalArgumentException(
                        "Gemstone type and tier must be paired");
            }
            aliases = Set.copyOf(aliases);
        }

        boolean known() {
            return true;
        }
    }

    private record GemstoneKey(
            GemstoneType gemstone,
            GemstoneTier tier) {
    }
}
