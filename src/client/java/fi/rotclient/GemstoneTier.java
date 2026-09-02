package fi.rotclient;

import java.util.Locale;

enum GemstoneTier {
    ROUGH(
            "ROUGH",
            "Rough",
            1L),
    FLAWED(
            "FLAWED",
            "Flawed",
            80L),
    FINE(
            "FINE",
            "Fine",
            6_400L),
    FLAWLESS(
            "FLAWLESS",
            "Flawless",
            512_000L),
    PERFECT(
            "PERFECT",
            "Perfect",
            2_560_000L);

    private final String id;
    private final String displayName;
    private final long roughEquivalent;

    GemstoneTier(
            String id,
            String displayName,
            long roughEquivalent) {
        this.id = id;
        this.displayName = displayName;
        this.roughEquivalent = roughEquivalent;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    long roughEquivalent() {
        return roughEquivalent;
    }

    String bazaarId(String gemstoneId) {
        if (gemstoneId == null || gemstoneId.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemstone id cannot be blank");
        }

        return id
                + "_"
                + gemstoneId.trim().toUpperCase(Locale.ROOT)
                + "_GEM";
    }

    String itemName(String gemstoneName) {
        if (gemstoneName == null || gemstoneName.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemstone name cannot be blank");
        }

        return displayName
                + " "
                + gemstoneName.trim()
                + " Gemstone";
    }

    static GemstoneTier fromItemName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }

        String normalized = itemName
                .trim()
                .toLowerCase(Locale.ROOT);

        for (GemstoneTier tier : values()) {
            String prefix = tier.displayName
                    .toLowerCase(Locale.ROOT)
                    + " ";

            if (normalized.startsWith(prefix)
                    && normalized.endsWith(" gemstone")) {
                return tier;
            }
        }

        return null;
    }
}
