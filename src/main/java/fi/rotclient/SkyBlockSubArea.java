package fi.rotclient;

import java.util.Locale;

/**
 * Known Hypixel SkyBlock mining sub-areas shown on the live scoreboard.
 * Each sub-area belongs to exactly one parent {@link SkyBlockArea}. Display
 * names are the canonical player-facing labels — HUD/diagnostics must not
 * invent pretty-printing elsewhere.
 */
enum SkyBlockSubArea {
    // Deep Caverns
    GUNPOWDER_MINES("Gunpowder Mines", SkyBlockArea.DEEP_CAVERNS),
    LAPIS_QUARRY("Lapis Quarry", SkyBlockArea.DEEP_CAVERNS),
    PIGMENS_DEN("Pigmen's Den", SkyBlockArea.DEEP_CAVERNS),
    SLIMEHILL("Slimehill", SkyBlockArea.DEEP_CAVERNS),
    DIAMOND_RESERVE("Diamond Reserve", SkyBlockArea.DEEP_CAVERNS),
    OBSIDIAN_SANCTUARY("Obsidian Sanctuary", SkyBlockArea.DEEP_CAVERNS),

    // Dwarven Mines
    THE_FORGE("The Forge", SkyBlockArea.DWARVEN_MINES),
    FORGE_BASIN("Forge Basin", SkyBlockArea.DWARVEN_MINES),
    CLIFFSIDE_VEINS("Cliffside Veins", SkyBlockArea.DWARVEN_MINES),
    LAVA_SPRINGS("Lava Springs", SkyBlockArea.DWARVEN_MINES),
    UPPER_MINES("Upper Mines", SkyBlockArea.DWARVEN_MINES),
    ROYAL_PALACE("Royal Palace", SkyBlockArea.DWARVEN_MINES),
    ROYAL_MINES("Royal Mines", SkyBlockArea.DWARVEN_MINES),
    PALACE_BRIDGE("Palace Bridge", SkyBlockArea.DWARVEN_MINES),
    RAMPARTS_QUARRY("Rampart's Quarry", SkyBlockArea.DWARVEN_MINES),
    GOBLIN_BURROWS("Goblin Burrows", SkyBlockArea.DWARVEN_MINES),
    THE_MIST("The Mist", SkyBlockArea.DWARVEN_MINES),
    ABANDONED_QUARRY("Abandoned Quarry", SkyBlockArea.DWARVEN_MINES),
    DIVANS_GATEWAY("Divan's Gateway", SkyBlockArea.DWARVEN_MINES),
    GREAT_ICE_WALL("Great Ice Wall", SkyBlockArea.DWARVEN_MINES),
    HANGING_COURT("Hanging Court", SkyBlockArea.DWARVEN_MINES),
    FOSSIL_RESEARCH("Fossil Research Center", SkyBlockArea.DWARVEN_MINES),
    FAR_RESERVE("Far Reserve", SkyBlockArea.DWARVEN_MINES),
    ARISTOCRAT_PASSAGE("Aristocrat Passage", SkyBlockArea.DWARVEN_MINES),
    GATES_TO_THE_MINES("Gates to the Mines", SkyBlockArea.DWARVEN_MINES),
    DWARVEN_VILLAGE("Dwarven Village", SkyBlockArea.DWARVEN_MINES),

    // Crystal Hollows
    MAGMA_FIELDS("Magma Fields", SkyBlockArea.CRYSTAL_HOLLOWS),
    PRECURSOR_REMNANTS("Precursor Remnants", SkyBlockArea.CRYSTAL_HOLLOWS),
    FAIRY_GROTTO("Fairy Grotto", SkyBlockArea.CRYSTAL_HOLLOWS),
    GOBLIN_HOLDOUT("Goblin Holdout", SkyBlockArea.CRYSTAL_HOLLOWS),
    JUNGLE("Jungle", SkyBlockArea.CRYSTAL_HOLLOWS),
    MITHRIL_DEPOSITS("Mithril Deposits", SkyBlockArea.CRYSTAL_HOLLOWS),
    MINES_OF_DIVAN("Mines of Divan", SkyBlockArea.CRYSTAL_HOLLOWS),
    KHAZAD_DUM("Khazad-Dûm", SkyBlockArea.CRYSTAL_HOLLOWS);

    private final String displayName;
    private final SkyBlockArea parentArea;

    SkyBlockSubArea(String displayName, SkyBlockArea parentArea) {
        this.displayName = displayName;
        this.parentArea = parentArea;
    }

    String displayName() {
        return displayName;
    }

    SkyBlockArea parentArea() {
        return parentArea;
    }

    String id() {
        return name();
    }

    static SkyBlockSubArea fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = id.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        for (SkyBlockSubArea sub : values()) {
            if (sub.name().equals(normalized)) {
                return sub;
            }
        }
        return null;
    }
}
