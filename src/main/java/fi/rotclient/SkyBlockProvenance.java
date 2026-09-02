package fi.rotclient;

import java.util.Locale;
import java.util.Objects;

/**
 * Provenance for a non-trivial enrichment or override. Every material change
 * that is not a pure official passthrough should carry one of these.
 */
record SkyBlockProvenance(
        String url,
        String sourceName,
        String sourceType,
        String retrievedAt,
        String confidence,
        String notes) {
    SkyBlockProvenance {
        url = blankToEmpty(url);
        sourceName = requireNonBlank(sourceName, "sourceName");
        sourceType = requireNonBlank(sourceType, "sourceType")
                .toUpperCase(Locale.ROOT);
        retrievedAt = requireNonBlank(retrievedAt, "retrievedAt");
        confidence = requireNonBlank(confidence, "confidence")
                .toUpperCase(Locale.ROOT);
        notes = blankToEmpty(notes);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim();
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    static SkyBlockProvenance hypixel(String retrievedAt, String notes) {
        return new SkyBlockProvenance(
                "https://api.hypixel.net/v2/resources/skyblock/items",
                "Hypixel SkyBlock Items",
                "OFFICIAL_HYPIXEL",
                retrievedAt,
                "VERIFIED_CURRENT",
                notes);
    }

    static SkyBlockProvenance rotDomain(String retrievedAt, String notes) {
        return new SkyBlockProvenance(
                "rotclient://domain-rules",
                "Rot Client domain rules",
                "ROT_DOMAIN_RULE",
                retrievedAt,
                "HIGH",
                notes);
    }

    static SkyBlockProvenance community(
            String url,
            String sourceName,
            String retrievedAt,
            String confidence,
            String notes) {
        Objects.requireNonNull(url, "url");
        return new SkyBlockProvenance(
                url,
                sourceName,
                "PUBLIC_MOD",
                retrievedAt,
                confidence,
                notes);
    }
}
