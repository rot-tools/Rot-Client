package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * One normalized item row in the Rot Client canonical dataset.
 */
record SkyBlockCanonicalItem(
        String stableId,
        String displayName,
        List<String> aliases,
        String hypixelItemId,
        String bazaarProductId,
        List<String> areas,
        String sourceHint,
        String idAuthority,
        String bazaarAuthority,
        String confidence,
        List<SkyBlockProvenance> provenance,
        List<String> notes) {
    SkyBlockCanonicalItem {
        stableId = SkyBlockItemId.normalize(stableId);
        if (stableId.isEmpty()) {
            throw new IllegalArgumentException("stableId cannot be blank");
        }
        displayName = displayName == null || displayName.isBlank()
                ? stableId
                : displayName.trim();
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        hypixelItemId = normalizeNullableId(hypixelItemId);
        bazaarProductId = normalizeNullableId(bazaarProductId);
        areas = areas == null ? List.of() : List.copyOf(areas);
        sourceHint = sourceHint == null || sourceHint.isBlank()
                ? null
                : sourceHint.trim().toUpperCase(Locale.ROOT);
        idAuthority = requireAuthority(idAuthority, "idAuthority");
        bazaarAuthority = bazaarAuthority == null || bazaarAuthority.isBlank()
                ? idAuthority
                : bazaarAuthority.trim().toUpperCase(Locale.ROOT);
        confidence = confidence == null || confidence.isBlank()
                ? "MEDIUM"
                : confidence.trim().toUpperCase(Locale.ROOT);
        provenance = provenance == null ? List.of() : List.copyOf(provenance);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    private static String normalizeNullableId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = SkyBlockItemId.normalize(id);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String requireAuthority(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    SkyBlockContentRegistry.KnownItem toKnownItem() {
        return new SkyBlockContentRegistry.KnownItem(
                stableId,
                displayName,
                aliases,
                bazaarProductId,
                areas,
                sourceHint,
                hypixelItemId == null ? stableId : hypixelItemId);
    }
}
