package fi.rotclient;

import java.util.Optional;
import java.util.Set;

/** Verified first-slice mapping from shadow resources to Bazaar product IDs. */
final class MiningSessionResourcePriceMapping {
    private static final Set<String> PRICED_GEMSTONE_PRODUCT_IDS =
            buildPricedGemstoneProductIds();

    private MiningSessionResourcePriceMapping() {
    }

    static Set<String> pricedGemstoneProductIds() {
        return PRICED_GEMSTONE_PRODUCT_IDS;
    }

    static boolean isPricedCategory(MiningSessionCategory category) {
        return category == MiningSessionCategory.TARGET_MINED
                || category == MiningSessionCategory.OTHER_MINED
                || category == MiningSessionCategory.CHEST_LOOT;
    }

    static Optional<String> bazaarProductId(
            MiningSessionResource resource) {
        if (resource == null
                || resource.kind()
                != MiningSessionResource.ResourceKind.ITEM) {
            return Optional.empty();
        }

        TrackedMaterial material = resource.material();
        if (material != null) {
            String resourceId = resource.resourceId();
            if (resourceId.equals(material.rawBazaarId())) {
                return Optional.of(resourceId);
            }
            if (resourceId.equals(material.enchantedBazaarId())) {
                return Optional.of(resourceId);
            }
            return Optional.empty();
        }

        String resourceId = resource.resourceId();
        if (!PRICED_GEMSTONE_PRODUCT_IDS.contains(resourceId)) {
            return Optional.empty();
        }

        GemstoneType gemstone = resource.gemstone();
        GemstoneTier tier = resource.gemstoneTier();
        if (gemstone != null && tier != null) {
            if (!resourceId.equals(gemstone.bazaarId(tier))) {
                return Optional.empty();
            }
        }

        return Optional.of(resourceId);
    }

    static boolean isSupportedItemResource(
            MiningSessionCategory category,
            MiningSessionResource resource) {
        if (category == MiningSessionCategory.CURRENCY
                || resource.kind()
                == MiningSessionResource.ResourceKind.CURRENCY) {
            return false;
        }
        if (!isPricedCategory(category)) {
            return false;
        }
        return bazaarProductId(resource).isPresent();
    }

    private static Set<String> buildPricedGemstoneProductIds() {
        java.util.LinkedHashSet<String> productIds =
                new java.util.LinkedHashSet<>();
        for (GemstoneType gemstone : GemstoneType.values()) {
            productIds.add(gemstone.bazaarId(GemstoneTier.ROUGH));
            productIds.add(gemstone.bazaarId(GemstoneTier.FLAWED));
        }
        return Set.copyOf(productIds);
    }
}
