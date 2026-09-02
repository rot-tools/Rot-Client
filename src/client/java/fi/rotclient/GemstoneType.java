package fi.rotclient;

import java.util.Locale;

enum GemstoneType {
    RUBY(
            "RUBY",
            "Ruby"),
    AMBER(
            "AMBER",
            "Amber"),
    SAPPHIRE(
            "SAPPHIRE",
            "Sapphire"),
    JADE(
            "JADE",
            "Jade"),
    AMETHYST(
            "AMETHYST",
            "Amethyst"),
    TOPAZ(
            "TOPAZ",
            "Topaz"),
    JASPER(
            "JASPER",
            "Jasper"),
    OPAL(
            "OPAL",
            "Opal"),
    ONYX(
            "ONYX",
            "Onyx"),
    AQUAMARINE(
            "AQUAMARINE",
            "Aquamarine"),
    CITRINE(
            "CITRINE",
            "Citrine"),
    PERIDOT(
            "PERIDOT",
            "Peridot");

    private final String id;
    private final String displayName;

    GemstoneType(
            String id,
            String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    String itemName(GemstoneTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier cannot be null");
        }

        return tier.itemName(displayName);
    }

    String bazaarId(GemstoneTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier cannot be null");
        }

        return tier.bazaarId(id);
    }

    boolean matchesItemName(String itemName) {
        if (itemName == null) {
            return false;
        }

        String normalized = normalize(itemName);

        for (GemstoneTier tier : GemstoneTier.values()) {
            if (normalized.equals(
                    normalize(this.itemName(tier)))) {
                return true;
            }
        }

        return false;
    }

    static GemstoneType fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        String normalized = id.trim();

        for (GemstoneType gemstone : values()) {
            if (gemstone.id.equalsIgnoreCase(normalized)) {
                return gemstone;
            }
        }

        return null;
    }

    static GemstoneType fromItemName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }

        for (GemstoneType gemstone : values()) {
            if (gemstone.matchesItemName(itemName)) {
                return gemstone;
            }
        }

        return null;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
