package fi.rotclient;

import java.util.Locale;

/**
 * Known Hypixel SkyBlock mining-related areas. Unknown IDs map to UNKNOWN.
 * Values and scoreboard hints are enriched from the Rot Client research
 * catalog (HIGH-confidence CURRENT records only). Research workspace remains
 * read-only development material — this enum is the runtime subset.
 */
enum SkyBlockArea {
    DEEP_CAVERNS("Deep Caverns"),
    DWARVEN_MINES("Dwarven Mines"),
    CRYSTAL_HOLLOWS("Crystal Hollows"),
    CRYSTAL_NUCLEUS("Crystal Nucleus"),
    DWARVEN_BASE_CAMP("Dwarven Base Camp"),
    GREAT_GLACITE_LAKE("Great Glacite Lake"),
    GLACITE_TUNNELS("Glacite Tunnels"),
    GLACITE_MINESHAFT("Glacite Mineshaft"),
    UNKNOWN_SKYBLOCK_AREA("Unknown");

    private final String displayName;

    SkyBlockArea(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    static SkyBlockArea fromId(String id) {
        if (id == null || id.isBlank()) {
            return UNKNOWN_SKYBLOCK_AREA;
        }
        String normalized = id.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        for (SkyBlockArea area : values()) {
            if (area.name().equals(normalized)) {
                return area;
            }
        }
        return UNKNOWN_SKYBLOCK_AREA;
    }

    String id() {
        return name();
    }

    boolean isGlaciteFamily() {
        return this == GLACITE_TUNNELS
                || this == GLACITE_MINESHAFT
                || this == GREAT_GLACITE_LAKE;
    }
}
