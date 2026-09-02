package fi.rotclient;

/**
 * Hypixel's resource-specific Mining Fortune domains.
 *
 * <p>This is mechanics metadata, not a promise that Rot Client currently
 * detects or applies every category at runtime.</p>
 */
enum MiningFortuneCategory {
    BLOCK("Block Fortune"),
    ORE("Ore Fortune"),
    DWARVEN_METAL("Dwarven Metal Fortune"),
    GEMSTONE("Gemstone Fortune"),
    NONE("No Mining Fortune"),
    UNKNOWN("Unknown");

    private final String displayName;

    MiningFortuneCategory(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }
}
