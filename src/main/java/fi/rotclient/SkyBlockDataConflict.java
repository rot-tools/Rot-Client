package fi.rotclient;

/**
 * Auditable disagreement between authority layers. Conflicts are never
 * silently resolved by replacing primary Hypixel facts.
 */
record SkyBlockDataConflict(
        String stableId,
        String field,
        String officialValue,
        String otherValue,
        String otherAuthority,
        String resolution,
        String notes) {
    SkyBlockDataConflict {
        stableId = SkyBlockItemId.normalize(stableId);
        if (stableId.isEmpty()) {
            throw new IllegalArgumentException("stableId cannot be blank");
        }
        field = require(field, "field");
        officialValue = officialValue == null ? "" : officialValue;
        otherValue = otherValue == null ? "" : otherValue;
        otherAuthority = require(otherAuthority, "otherAuthority");
        resolution = require(resolution, "resolution");
        notes = notes == null ? "" : notes;
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
