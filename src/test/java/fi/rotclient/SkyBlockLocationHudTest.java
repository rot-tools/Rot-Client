package fi.rotclient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockLocationHudTest {
    @AfterEach
    void resetDetectorState() {
        SkyBlockAreaDetector.updateCurrentLocation(SkyBlockLocation.UNKNOWN);
    }

    @Test
    void dwarvenMinesTheForgeHudDisplay() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "§aThe Forge");
        assertEquals(SkyBlockArea.DWARVEN_MINES, location.parentArea());
        assertEquals(SkyBlockSubArea.THE_FORGE, location.subArea());
        assertEquals("Dwarven Mines · The Forge", location.hudDisplay());
    }

    @Test
    void dwarvenMinesCliffsideVeinsHudDisplay() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "⏣ Cliffside Veins");
        assertEquals(SkyBlockArea.DWARVEN_MINES, location.parentArea());
        assertEquals(SkyBlockSubArea.CLIFFSIDE_VEINS, location.subArea());
        assertEquals(
                "Dwarven Mines · Cliffside Veins",
                location.hudDisplay());
    }

    @Test
    void lavaSpringsMapsToDwarvenParentAndSub() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Lava Springs");
        assertEquals(SkyBlockArea.DWARVEN_MINES, location.parentArea());
        assertEquals(SkyBlockSubArea.LAVA_SPRINGS, location.subArea());
        assertEquals("Dwarven Mines · Lava Springs", location.hudDisplay());
    }

    @Test
    void crystalHollowsMagmaFieldsHudDisplay() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Magma Fields");
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS, location.parentArea());
        assertEquals(SkyBlockSubArea.MAGMA_FIELDS, location.subArea());
        assertEquals(
                "Crystal Hollows · Magma Fields",
                location.hudDisplay());
    }

    @Test
    void crystalHollowsPrecursorRemnantsHudDisplay() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Precursor Remnants");
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS, location.parentArea());
        assertEquals(
                SkyBlockSubArea.PRECURSOR_REMNANTS,
                location.subArea());
        assertEquals(
                "Crystal Hollows · Precursor Remnants",
                location.hudDisplay());
    }

    @Test
    void glaciteAreasAreAlreadySpecificWithoutSubArea() {
        SkyBlockLocation tunnels =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "⏣ Glacite Tunnels");
        assertEquals(SkyBlockArea.GLACITE_TUNNELS, tunnels.parentArea());
        assertNull(tunnels.subArea());
        assertEquals("Glacite Tunnels", tunnels.hudDisplay());

        SkyBlockLocation lake =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Great Glacite Lake");
        assertEquals(SkyBlockArea.GREAT_GLACITE_LAKE, lake.parentArea());
        assertEquals("Great Glacite Lake", lake.hudDisplay());
    }

    @Test
    void knownParentUnknownChildShowsParentOnly() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Dwarven Mines");
        assertEquals(SkyBlockArea.DWARVEN_MINES, location.parentArea());
        assertNull(location.subArea());
        assertEquals("Dwarven Mines", location.hudDisplay());
        assertFalse(location.hudDisplay().contains("Unknown"));
    }

    @Test
    void fullUnknownDoesNotFabricateLocation() {
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Spider's Den");
        assertTrue(location.isUnknown());
        assertEquals("Unknown", location.hudDisplay());
        assertEquals(SkyBlockLocation.UNKNOWN, location);
    }

    @Test
    void liveUpdateWithinDwarvenParent() {
        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "The Forge"));
        assertEquals(
                "Dwarven Mines · The Forge",
                SkyBlockAreaDetector.detectLocation().hudDisplay());

        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Cliffside Veins"));
        assertEquals(
                "Dwarven Mines · Cliffside Veins",
                SkyBlockAreaDetector.detectLocation().hudDisplay());
        assertEquals(
                SkyBlockSubArea.CLIFFSIDE_VEINS,
                SkyBlockAreaDetector.detectLocation().subArea());
    }

    @Test
    void warpFromDwarvenToCrystalHollowsUpdatesParentAndChild() {
        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockLocation.of(
                        SkyBlockArea.DWARVEN_MINES,
                        SkyBlockSubArea.THE_FORGE));
        assertEquals(
                "Dwarven Mines · The Forge",
                SkyBlockAreaDetector.detectLocation().hudDisplay());

        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockAreaDetector.locationFromScoreboardTitle(
                        "Magma Fields"));
        SkyBlockLocation after = SkyBlockAreaDetector.detectLocation();
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS, after.parentArea());
        assertEquals(SkyBlockSubArea.MAGMA_FIELDS, after.subArea());
        assertEquals(
                "Crystal Hollows · Magma Fields",
                after.hudDisplay());
        assertFalse(after.hudDisplay().contains("Dwarven"));
        assertFalse(after.hudDisplay().contains("Forge"));
    }

    @Test
    void serverTransitionExplicitResetClearsStaleSubArea() {
        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockLocation.of(
                        SkyBlockArea.DWARVEN_MINES,
                        SkyBlockSubArea.THE_FORGE));
        assertFalse(SkyBlockAreaDetector.detectLocation().isUnknown());

        SkyBlockAreaDetector.updateCurrentLocation(SkyBlockLocation.UNKNOWN);
        assertTrue(SkyBlockAreaDetector.detectLocation().isUnknown());
        assertEquals("Unknown", SkyBlockAreaDetector.detectLocation().hudDisplay());
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                SkyBlockAreaDetector.detect());
    }

    @Test
    void stickyMissDoesNotClearKnownLocation() {
        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockLocation.of(
                        SkyBlockArea.CRYSTAL_HOLLOWS,
                        SkyBlockSubArea.PRECURSOR_REMNANTS));
        assertEquals(
                SkyBlockLocation.UNKNOWN,
                SkyBlockAreaDetector.locationFromScoreboardLines(
                        List.of(" ⏣ Spider's Den")));
        assertEquals(
                "Crystal Hollows · Precursor Remnants",
                SkyBlockAreaDetector.detectLocation().hudDisplay());
    }

    @Test
    void repeatedScoreboardMissEventuallyExpiresStaleLocation() {
        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockLocation.of(
                        SkyBlockArea.DWARVEN_MINES,
                        SkyBlockSubArea.THE_FORGE,
                        1_000L));

        assertFalse(SkyBlockAreaDetector.expireCurrentLocationIfStale(
                5_999L, 5_000L));
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockAreaDetector.detect());

        assertTrue(SkyBlockAreaDetector.expireCurrentLocationIfStale(
                6_000L, 5_000L));
        assertTrue(SkyBlockAreaDetector.detectLocation().isUnknown());
    }

    @Test
    void scoreboardLinesPreferFirstRecognizedSubArea() {
        List<String> lines = Arrays.asList(
                " SKYBLOCK",
                " Wednesday 6th",
                " 7:32am",
                " ⏣ The Forge",
                "");
        SkyBlockLocation location =
                SkyBlockAreaDetector.locationFromScoreboardLines(lines);
        assertEquals(
                "Dwarven Mines · The Forge",
                location.hudDisplay());
    }

    @Test
    void editorPreviewUsesRepresentativeForgeLocation() {
        assertEquals(
                "Dwarven Mines · The Forge",
                SkyBlockLocation.EDITOR_PREVIEW.hudDisplay());
        assertEquals(
                SkyBlockArea.DWARVEN_MINES,
                SkyBlockLocation.EDITOR_PREVIEW.parentArea());
        assertEquals(
                SkyBlockSubArea.THE_FORGE,
                SkyBlockLocation.EDITOR_PREVIEW.subArea());
    }

    @Test
    void displayNamesNeverExposeEnumIds() {
        for (SkyBlockSubArea sub : SkyBlockSubArea.values()) {
            assertFalse(sub.displayName().contains("_"));
            assertFalse(sub.displayName().equals(sub.name()));
        }
        assertEquals("Unknown", SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.displayName());
        assertFalse(
                SkyBlockLocation.UNKNOWN.hudDisplay().contains(
                        "UNKNOWN_SKYBLOCK_AREA"));
    }

    @Test
    void parentOnlyUpdateClearsPreviousSubArea() {
        SkyBlockAreaDetector.updateCurrentLocation(
                SkyBlockLocation.of(
                        SkyBlockArea.DWARVEN_MINES,
                        SkyBlockSubArea.THE_FORGE));
        SkyBlockAreaDetector.updateCurrentArea(SkyBlockArea.DWARVEN_MINES);
        SkyBlockLocation location = SkyBlockAreaDetector.detectLocation();
        assertEquals(SkyBlockArea.DWARVEN_MINES, location.parentArea());
        assertNull(location.subArea());
        assertEquals("Dwarven Mines", location.hudDisplay());
    }
}
