package fi.rotclient;

import java.util.List;
import java.util.Locale;

/**
 * Handwritten mechanics identity for one mining resource or collection.
 * Physical block evidence is deliberately separate from gameplay and API IDs.
 */
record SkyBlockMiningResource(
        String canonicalResourceId,
        String displayName,
        String kind,
        String collectionId,
        String hypixelItemId,
        String bazaarProductId,
        String droppedItemId,
        List<String> aliases,
        List<String> sackAliases,
        List<String> physicalBlockTokens,
        String enchantedItemId,
        String enchantedBlockItemId,
        List<String> areas,
        MiningFortuneCategory fortuneCategory,
        Integer breakingPower,
        Integer blockStrength,
        Long baseYieldMin,
        Long baseYieldMax,
        boolean requiresAreaContext,
        String gemstoneFamily,
        String gemstoneTier,
        String confidence,
        String evidenceState,
        List<String> provenanceRefs,
        List<String> notes) {
    SkyBlockMiningResource {
        canonicalResourceId = requiredId(canonicalResourceId, "canonicalResourceId");
        displayName = displayName == null || displayName.isBlank()
                ? canonicalResourceId
                : displayName.trim();
        kind = normalizedWord(kind, "UNKNOWN");
        collectionId = nullableId(collectionId);
        hypixelItemId = nullableId(hypixelItemId);
        bazaarProductId = nullableId(bazaarProductId);
        droppedItemId = nullableId(droppedItemId);
        aliases = copy(aliases);
        sackAliases = copy(sackAliases);
        physicalBlockTokens = copy(physicalBlockTokens);
        enchantedItemId = nullableId(enchantedItemId);
        enchantedBlockItemId = nullableId(enchantedBlockItemId);
        areas = copy(areas);
        fortuneCategory = fortuneCategory == null
                ? MiningFortuneCategory.UNKNOWN
                : fortuneCategory;
        breakingPower = nonNegative(breakingPower, "breakingPower");
        blockStrength = nonNegative(blockStrength, "blockStrength");
        baseYieldMin = nonNegative(baseYieldMin, "baseYieldMin");
        baseYieldMax = nonNegative(baseYieldMax, "baseYieldMax");
        if (baseYieldMin != null && baseYieldMax != null
                && baseYieldMin > baseYieldMax) {
            throw new IllegalArgumentException("baseYieldMin exceeds baseYieldMax");
        }
        gemstoneFamily = nullableId(gemstoneFamily);
        gemstoneTier = nullableId(gemstoneTier);
        confidence = normalizedWord(confidence, "UNKNOWN");
        evidenceState = normalizedWord(evidenceState, "RESEARCHED");
        provenanceRefs = copy(provenanceRefs);
        notes = copy(notes);
    }

    boolean isCollection() {
        return "COLLECTION".equals(kind);
    }

    boolean allowsArea(SkyBlockArea area) {
        if (!requiresAreaContext) {
            return true;
        }
        if (area == null || area == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) {
            return false;
        }
        return areas.stream().anyMatch(area.id()::equalsIgnoreCase);
    }

    private static String requiredId(String value, String field) {
        String normalized = nullableId(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return normalized;
    }

    private static String nullableId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String normalizedWord(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private static <T extends Number> T nonNegative(T value, String field) {
        if (value != null && value.longValue() < 0L) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
