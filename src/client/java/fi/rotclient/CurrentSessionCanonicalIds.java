package fi.rotclient;

import java.util.Optional;

/**
 * Maps engine / Hypixel / Bazaar product identities onto the stable Rot Client
 * domain item id used by the canonical Current Session ledger.
 *
 * <p>Canonical rows are keyed by Rot Client stable ids (e.g. {@code TITANIUM}).
 * Hypixel item ids and Bazaar product ids remain separate metadata and are never
 * written back over an already-persisted canonical row identity.
 *
 * <p>Does not alias {@code STONE} → {@code HARD_STONE}.
 */
final class CurrentSessionCanonicalIds {
    private CurrentSessionCanonicalIds() {
    }

    static String canonicalItemId(MiningSessionResource resource) {
        if (resource == null) {
            return "UNKNOWN";
        }
        TrackedMaterial material = resource.material();
        if (material != null) {
            String resourceId = SkyBlockItemId.normalize(resource.resourceId());
            if (resourceId.equals(SkyBlockItemId.normalize(material.enchantedBazaarId()))
                    || resourceId.equals(SkyBlockItemId.normalize(
                    material.enchantedItemName()))) {
                return SkyBlockItemId.normalize(material.enchantedBazaarId());
            }
            if (resourceId.equals(SkyBlockItemId.normalize(
                    material.rawBazaarId()))
                    || resourceId.equals(SkyBlockItemId.normalize(
                    material.rawItemName()))) {
                return SkyBlockItemId.normalize(material.id());
            }
            // Preserve exact material product forms such as
            // ENCHANTED_GOLD_BLOCK. Collapsing every material resource to the
            // raw family id loses quantity units and Bazaar identity.
            return resourceId.isEmpty()
                    ? SkyBlockItemId.normalize(material.id())
                    : resourceId;
        }
        return canonicalItemId(resource.resourceId(), resource.displayName());
    }

    static String canonicalItemId(String rawId, String displayFallback) {
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromKnownId(rawId, displayFallback);
        if (resolved.catalogMatch()) {
            return resolved.stableId();
        }
        String normalized = SkyBlockItemId.normalize(rawId);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return SkyBlockItemIdentityResolver.fromTextToken(displayFallback)
                .stableId();
    }

    static Optional<String> bazaarProductId(String canonicalItemId) {
        if (canonicalItemId == null || canonicalItemId.isBlank()) {
            return Optional.empty();
        }
        Optional<SkyBlockContentRegistry.KnownItem> known =
                SkyBlockContentRegistry.builtin().lookup(canonicalItemId);
        if (known.isPresent()) {
            String product = known.get().bazaarProductId();
            if (product != null && !product.isBlank()
                    && !SkyBlockItemIdentityResolver.UNKNOWN.equals(product)) {
                return Optional.of(SkyBlockItemId.normalize(product));
            }
        }
        for (TrackedMaterial material : TrackedMaterial.values()) {
            if (material.id().equalsIgnoreCase(canonicalItemId)) {
                return Optional.of(SkyBlockItemId.normalize(material.rawBazaarId()));
            }
            if (material.enchantedBazaarId().equalsIgnoreCase(canonicalItemId)) {
                return Optional.of(SkyBlockItemId.normalize(
                        material.enchantedBazaarId()));
            }
            if (material.rawBazaarId().equalsIgnoreCase(canonicalItemId)) {
                return Optional.of(SkyBlockItemId.normalize(material.rawBazaarId()));
            }
            if (material.enchantedBlockItemName() != null) {
                String enchantedBlockId = SkyBlockItemId.normalize(
                        material.enchantedBlockBazaarId());
                if (enchantedBlockId.equalsIgnoreCase(canonicalItemId)) {
                    return Optional.of(enchantedBlockId);
                }
            }
        }
        return Optional.empty();
    }
}
