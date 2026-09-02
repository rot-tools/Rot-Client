package fi.rotclient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory Rot Client canonical SkyBlock dataset. Runtime code reasons against
 * this normalized representation, never against live upstream repositories.
 */
final class SkyBlockCanonicalDataset {
    private final String schemaVersion;
    private final String generatedAt;
    private final Map<String, SkyBlockCanonicalItem> itemsById;
    private final Set<String> bazaarProductIds;
    private final List<SkyBlockDataConflict> conflicts;
    private final List<SkyBlockProvenance> datasetProvenance;

    SkyBlockCanonicalDataset(
            String schemaVersion,
            String generatedAt,
            List<SkyBlockCanonicalItem> items,
            Set<String> bazaarProductIds,
            List<SkyBlockDataConflict> conflicts,
            List<SkyBlockProvenance> datasetProvenance) {
        this.schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? "canonical-items.v1"
                : schemaVersion.trim();
        this.generatedAt = generatedAt == null ? "" : generatedAt.trim();
        Map<String, SkyBlockCanonicalItem> map = new LinkedHashMap<>();
        if (items != null) {
            for (SkyBlockCanonicalItem item : items) {
                if (item == null) {
                    throw new IllegalArgumentException("item cannot be null");
                }
                if (map.put(item.stableId(), item) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate canonical item: " + item.stableId());
                }
            }
        }
        this.itemsById = Collections.unmodifiableMap(map);
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
        this.conflicts = conflicts == null
                ? List.of()
                : List.copyOf(conflicts);
        this.datasetProvenance = datasetProvenance == null
                ? List.of()
                : List.copyOf(datasetProvenance);
    }

    String schemaVersion() {
        return schemaVersion;
    }

    String generatedAt() {
        return generatedAt;
    }

    Map<String, SkyBlockCanonicalItem> itemsById() {
        return itemsById;
    }

    List<SkyBlockCanonicalItem> items() {
        return List.copyOf(itemsById.values());
    }

    Set<String> bazaarProductIds() {
        return bazaarProductIds;
    }

    List<SkyBlockDataConflict> conflicts() {
        return conflicts;
    }

    List<SkyBlockProvenance> datasetProvenance() {
        return datasetProvenance;
    }

    Optional<SkyBlockCanonicalItem> lookup(String id) {
        String normalized = SkyBlockItemId.normalize(id);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemsById.get(normalized));
    }

    boolean isKnownBazaarProduct(String productId) {
        String normalized = SkyBlockItemId.normalize(productId);
        return !normalized.isEmpty() && bazaarProductIds.contains(normalized);
    }

    boolean isEmpty() {
        return itemsById.isEmpty();
    }
}
