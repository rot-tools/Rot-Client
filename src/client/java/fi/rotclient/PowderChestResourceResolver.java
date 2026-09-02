package fi.rotclient;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Narrow Powder Chest reward resolution on top of the mining resource catalog.
 *
 * <p>Full {@link MiningResourceCatalog#fromExactSackItem(String)} reuse is
 * intentional for resources already represented by Rot Client with stable
 * canonical IDs. No external reward table was imported. Unknown display names
 * remain diagnostic-only.
 */
final class PowderChestResourceResolver {
    private static final Map<String, ResolvedReward> ADDITIONAL_ALIASES = Map.of(
            normalize("Gold Essence"),
            new ResolvedReward(
                    MiningSessionResource.genericItem(
                            "GOLD_ESSENCE",
                            "Gold Essence"),
                    true),
            normalize("Diamond Essence"),
            new ResolvedReward(
                    MiningSessionResource.genericItem(
                            "DIAMOND_ESSENCE",
                            "Diamond Essence"),
                    true),
            normalize("Wishing Compass"),
            new ResolvedReward(
                    MiningSessionResource.genericItem(
                            "WISHING_COMPASS",
                            "Wishing Compass"),
                    true),
            normalize("Compacted Hard Stone"),
            new ResolvedReward(
                    MiningSessionResource.genericItem(
                            "COMPACTED_HARD_STONE",
                            "Compacted Hard Stone"),
                    true));

    private static final Map<String, ResolvedReward> CURRENCY_ALIASES = Map.of(
            normalize("Gemstone Powder"),
            new ResolvedReward(
                    MiningSessionResource.currency(
                            "GEMSTONE_POWDER",
                            "Gemstone Powder"),
                    true),
            normalize("Mithril Powder"),
            new ResolvedReward(
                    MiningSessionResource.currency(
                            "MITHRIL_POWDER",
                            "Mithril Powder"),
                    true));

    private final MiningResourceCatalog catalog;

    PowderChestResourceResolver(MiningResourceCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }
        this.catalog = catalog;
    }

    ResolvedReward resolveItem(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return unknownItem(displayName);
        }

        Optional<MiningResourceCatalog.ResourceDefinition> catalogMatch =
                catalog.fromExactSackItem(displayName);
        if (catalogMatch.isPresent()) {
            MiningResourceCatalog.ResourceDefinition definition =
                    catalogMatch.get();
            return new ResolvedReward(definition.resource(), true);
        }

        ResolvedReward additional = ADDITIONAL_ALIASES.get(
                normalize(displayName));
        if (additional != null) {
            return additional;
        }

        return unknownItem(displayName);
    }

    ResolvedReward resolveCurrency(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return unknownCurrency(displayName);
        }

        ResolvedReward currency = CURRENCY_ALIASES.get(
                normalize(displayName));
        if (currency != null) {
            return currency;
        }

        return unknownCurrency(displayName);
    }

    private static ResolvedReward unknownItem(String displayName) {
        String safeName = displayName == null ? "" : displayName.trim();
        String resourceId = "CHEST:"
                + safeName.toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_|_$", "");
        if (resourceId.equals("CHEST:") || resourceId.length() > 64) {
            resourceId = "CHEST:UNKNOWN";
        }
        return new ResolvedReward(
                MiningSessionResource.genericItem(
                        resourceId,
                        safeName.isEmpty() ? "Unknown" : safeName),
                false);
    }

    private static ResolvedReward unknownCurrency(String displayName) {
        return new ResolvedReward(
                MiningSessionResource.currency(
                        "CHEST:UNKNOWN_CURRENCY",
                        displayName == null || displayName.isBlank()
                                ? "Unknown"
                                : displayName.trim()),
                false);
    }

    private static String normalize(String value) {
        return MiningResourceCatalog.normalizeAlias(value);
    }

    static String normalizeDisplayKey(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "UNKNOWN";
        }
        return normalize(displayName);
    }

    record ResolvedReward(
            MiningSessionResource resource,
            boolean known) {
    }
}
