package fi.rotclient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockAreaTest {
    @AfterEach
    void resetDetectorState() {
        SkyBlockAreaDetector.updateCurrentLocation(SkyBlockLocation.UNKNOWN);
        SkyBlockAreaDetector.clearSkyblockPresence();
    }

    @Test
    void fromIdMapsKnownAreas() {
        assertEquals(SkyBlockArea.DEEP_CAVERNS, SkyBlockArea.fromId("deep-caverns"));
        assertEquals(SkyBlockArea.DWARVEN_MINES, SkyBlockArea.fromId("DWARVEN_MINES"));
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS, SkyBlockArea.fromId("crystal-hollows"));
        assertEquals(SkyBlockArea.CRYSTAL_NUCLEUS, SkyBlockArea.fromId("Crystal Nucleus"));
        assertEquals(SkyBlockArea.DWARVEN_BASE_CAMP, SkyBlockArea.fromId("DWARVEN_BASE_CAMP"));
        assertEquals(SkyBlockArea.GLACITE_TUNNELS, SkyBlockArea.fromId("glacite_tunnels"));
        assertEquals(SkyBlockArea.GLACITE_MINESHAFT, SkyBlockArea.fromId("GLACITE-MINESHAFT"));
        assertEquals(
                SkyBlockArea.GREAT_GLACITE_LAKE,
                SkyBlockArea.fromId("Great Glacite Lake"));
    }

    @Test
    void fromIdFallsBackToUnknown() {
        assertEquals(SkyBlockArea.UNKNOWN_SKYBLOCK_AREA, SkyBlockArea.fromId(null));
        assertEquals(SkyBlockArea.UNKNOWN_SKYBLOCK_AREA, SkyBlockArea.fromId(""));
        assertEquals(SkyBlockArea.UNKNOWN_SKYBLOCK_AREA, SkyBlockArea.fromId("hub"));
    }

    @Test
    void scoreboardHeuristics() {
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("  Crystal Hollows  "));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Dwarven Mines"));
        assertEquals(
                SkyBlockArea.GLACITE_TUNNELS,
                SkyBlockAreaDetector.fromScoreboardTitle("⏣ Glacite Tunnels"));
        assertEquals(
                SkyBlockArea.GLACITE_MINESHAFT,
                SkyBlockAreaDetector.fromScoreboardTitle("⏣ Glacite Mineshaft"));
        assertEquals(
                SkyBlockArea.GREAT_GLACITE_LAKE,
                SkyBlockAreaDetector.fromScoreboardTitle("Great Glacite Lake"));
        // Bare ambiguous tokens must not invent a specific Glacite area.
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.fromScoreboardTitle("Glacite"));
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.fromScoreboardTitle("Mineshaft"));
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.fromScoreboardTitle("Spider's Den"));
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.detect());
    }

    @Test
    void dwarvenSubAreasMapToParentNotUnknown() {
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("⏣ Cliffside Veins"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("§aThe Forge"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Lava Springs"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Upper Mines"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Royal Palace"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Rampart's Quarry"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Goblin Burrows"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("The Mist"));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardTitle("Far Reserve"));
    }

    @Test
    void dwarvenSubAreasExposeCanonicalSubArea() {
        assertEquals(
                SkyBlockSubArea.CLIFFSIDE_VEINS,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "⏣ Cliffside Veins").subArea());
        assertEquals(
                SkyBlockSubArea.THE_FORGE,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "§aThe Forge").subArea());
        assertEquals(
                SkyBlockSubArea.RAMPARTS_QUARRY,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Rampart's Quarry").subArea());
    }

    @Test
    void crystalHollowsSubAreasMapToParent() {
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Magma Fields"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Precursor Remnants"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Fairy Grotto"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Goblin Holdout"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Jungle"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Mithril Deposits"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Mines of Divan"));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.fromScoreboardTitle("Khazad-Dum"));
    }

    @Test
    void deepCavernsAndItsOfficialFloorsUseExactCanonicalNames() {
        assertEquals(
                SkyBlockLocation.of(SkyBlockArea.DEEP_CAVERNS),
                SkyBlockAreaDetector.locationFromScoreboardTitle("Deep Caverns"));

        assertEquals(
                SkyBlockSubArea.GUNPOWDER_MINES,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Gunpowder Mines").subArea());
        assertEquals(
                SkyBlockSubArea.LAPIS_QUARRY,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Lapis Quarry").subArea());
        assertEquals(
                SkyBlockSubArea.PIGMENS_DEN,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Pigmen's Den").subArea());
        assertEquals(
                SkyBlockSubArea.SLIMEHILL,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Slimehill").subArea());
        assertEquals(
                SkyBlockSubArea.DIAMOND_RESERVE,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Diamond Reserve").subArea());
        assertEquals(
                SkyBlockSubArea.OBSIDIAN_SANCTUARY,
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Obsidian Sanctuary").subArea());
    }

    @Test
    void deepCavernsNarrativeAndPartialNamesRemainUnknown() {
        for (String line : List.of(
                "Travel to the Deep Caverns",
                "Diamond Reserve unlocked",
                "Gunpowder",
                "Sanctuary")) {
            assertEquals(
                    SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                    SkyBlockAreaDetector.fromScoreboardTitle(line),
                    line);
        }
    }

    @Test
    void minesOfDivanExposesCrystalHollowsParentAndCanonicalSubArea() {
        SkyBlockLocation location = SkyBlockAreaDetector.locationFromScoreboardTitle(
                "Mines of Divan");

        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS, location.parentArea());
        assertEquals(SkyBlockSubArea.MINES_OF_DIVAN, location.subArea());
    }

    @Test
    void formattedVariantsAndNormalization() {
        assertEquals(
                "cliffside veins",
                SkyBlockAreaDetector.normalizeLocationText("  ⏣ Cliffside Veins  "));
        assertEquals(
                "the forge",
                SkyBlockAreaDetector.normalizeLocationText("§b§lThe Forge"));
        assertEquals(
                "khazad dum",
                SkyBlockAreaDetector.normalizeLocationText("Khazad-Dum"));
        assertTrue(SkyBlockAreaDetector.normalizeLocationText("§").isEmpty());
    }

    @Test
    void fromScoreboardLinesScansEveryLineNotJustTheTitle() {
        List<String> lines = Arrays.asList(
                " SKYBLOCK",
                " Wednesday 6th",
                " 7:32am ☀",
                " ⏣ Cliffside Veins",
                "");

        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.fromScoreboardLines(lines));
    }

    @Test
    void fromScoreboardLinesFallsBackToUnknownWhenNoLineMatches() {
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.fromScoreboardLines(
                        List.of(" SKYBLOCK", " ⏣ Spider's Den")));
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.fromScoreboardLines(null));
    }

    @Test
    void narrativeFragmentsCannotCreateMiningAreaAuthority() {
        for (String line : List.of(
                "Return to the Crystal Nucleus",
                "Dwarven event starts soon",
                "Visit the Base Camp",
                "Jungle Temple Key",
                "Rampart objective complete")) {
            assertEquals(
                    SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                    SkyBlockAreaDetector.fromScoreboardTitle(line),
                    line);
        }
    }

    @Test
    void detectReflectsMostRecentlyReportedAreaAndStaysStickyOnMiss() {
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.detect());

        SkyBlockAreaDetector.updateCurrentArea(SkyBlockArea.CRYSTAL_HOLLOWS);
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.detect());

        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.fromScoreboardLines(
                        List.of(" ⏣ Spider's Den")));
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS,
                SkyBlockAreaDetector.detect());

        SkyBlockAreaDetector.updateCurrentArea(null);
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.detect());
    }

    @Test
    void skyblockPresenceFollowsScoreboardTitle() {
        SkyBlockAreaDetector.clearSkyblockPresence();
        assertFalse(SkyBlockAreaDetector.isInSkyblock());
        assertTrue(SkyBlockAreaDetector.looksLikeSkyblock(List.of(" SKYBLOCK")));
        assertTrue(SkyBlockAreaDetector.looksLikeSkyblock(List.of("§aSKYBLOCK")));
        SkyBlockAreaDetector.updateSkyblockPresence(List.of("SKYBLOCK"));
        assertTrue(SkyBlockAreaDetector.isInSkyblock());
        SkyBlockAreaDetector.updateSkyblockPresence(List.of("Hub"));
        assertFalse(SkyBlockAreaDetector.isInSkyblock());
        SkyBlockAreaDetector.updateSkyblockPresence(List.of());
        assertFalse(SkyBlockAreaDetector.isInSkyblock());
    }

    @Test
    void serverTransitionExplicitResetClearsStickyArea() {
        SkyBlockAreaDetector.updateCurrentArea(SkyBlockArea.DWARVEN_MINES);
        assertEquals(SkyBlockArea.DWARVEN_MINES, SkyBlockAreaDetector.detect());
        SkyBlockAreaDetector.updateCurrentArea(SkyBlockArea.UNKNOWN_SKYBLOCK_AREA);
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.detect());
    }
}
