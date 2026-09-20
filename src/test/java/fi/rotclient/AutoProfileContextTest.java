package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class AutoProfileContextTest {

    private static Optional<AutoProfileContext> classify(
            boolean inDungeon,
            String... rows) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("SKYBLOCK");
        lines.addAll(List.of(rows));
        return AutoProfileContext.classify(
                lines,
                SkyBlockAreaDetector.locationFromScoreboardLines(lines),
                inDungeon);
    }

    @Test
    void notSkyblockNeverClassifies() {
        List<String> lobby = List.of("HYPIXEL", "Players: 12", "The Garden");

        assertTrue(AutoProfileContext.classify(
                lobby,
                SkyBlockAreaDetector.locationFromScoreboardLines(lobby),
                false).isEmpty());
        assertTrue(AutoProfileContext.classify(
                List.of(),
                SkyBlockLocation.UNKNOWN,
                true).isEmpty());
        assertTrue(AutoProfileContext.classify(
                null,
                SkyBlockLocation.UNKNOWN,
                false).isEmpty());
    }

    @Test
    void miningSubAreasMapToTheirParentContext() {
        assertEquals(AutoProfileContext.DWARVEN_MINES,
                classify(false, "⏣ Cliffside Veins").orElseThrow());
        assertEquals(AutoProfileContext.DWARVEN_MINES,
                classify(false, "⏣ Dwarven Base Camp").orElseThrow());
        assertEquals(AutoProfileContext.CRYSTAL_HOLLOWS,
                classify(false, "⏣ Magma Fields").orElseThrow());
        assertEquals(AutoProfileContext.CRYSTAL_HOLLOWS,
                classify(false, "⏣ Crystal Nucleus").orElseThrow());
        assertEquals(AutoProfileContext.GLACITE,
                classify(false, "⏣ Glacite Tunnels").orElseThrow());
        assertEquals(AutoProfileContext.GLACITE,
                classify(false, "⏣ Great Glacite Lake").orElseThrow());
        assertEquals(AutoProfileContext.GLACITE_MINESHAFT,
                classify(false, "⏣ Glacite Mineshaft").orElseThrow());
        assertEquals(AutoProfileContext.DEEP_CAVERNS,
                classify(false, "⏣ Gunpowder Mines").orElseThrow());
    }

    @Test
    void everyMiningAreaHasExactlyOneMappingOrIsUnknown() {
        for (SkyBlockArea area : SkyBlockArea.values()) {
            AutoProfileContext mapped = AutoProfileContext.fromArea(area);
            if (area == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) {
                assertEquals(null, mapped);
            } else {
                assertTrue(mapped != null, area + " has no context");
            }
        }
    }

    @Test
    void dungeonBeatsEverythingElse() {
        assertEquals(AutoProfileContext.DUNGEONS,
                classify(true, "⏣ The Catacombs (F7)", "Slayer Quest")
                        .orElseThrow());
        // Even a stale mining location cannot win over a confident dungeon.
        assertEquals(AutoProfileContext.DUNGEONS,
                classify(true, "⏣ Cliffside Veins").orElseThrow());
    }

    @Test
    void kuudraIsRecognisedFromItsHollow() {
        assertEquals(AutoProfileContext.KUUDRA,
                classify(false, "⏣ Kuudra's Hollow").orElseThrow());
        assertEquals(AutoProfileContext.KUUDRA,
                classify(false, "⏣ Kuudra’s Hollow").orElseThrow());
    }

    @Test
    void gardenAndPrivateIslandAreRecognised() {
        assertEquals(AutoProfileContext.GARDEN,
                classify(false, "⏣ The Garden").orElseThrow());
        assertEquals(AutoProfileContext.GARDEN,
                classify(false, "Plot - 3").orElseThrow());
        assertEquals(AutoProfileContext.PRIVATE_ISLAND,
                classify(false, "⏣ Your Island").orElseThrow());
    }

    @Test
    void slayerQuestOnlyAppliesWhenNothingMoreSpecificMatches() {
        assertEquals(AutoProfileContext.SLAYER,
                classify(false, "⏣ Graveyard", "Slayer Quest",
                        "Revenant Horror III").orElseThrow());
        // The quest stays on the sidebar while mining; the area must win.
        assertEquals(AutoProfileContext.DWARVEN_MINES,
                classify(false, "⏣ Cliffside Veins", "Slayer Quest")
                        .orElseThrow());
        assertEquals(AutoProfileContext.PRIVATE_ISLAND,
                classify(false, "⏣ Your Island", "Slayer Quest")
                        .orElseThrow());
    }

    @Test
    void unrecognisedSkyblockPlaceIsOther() {
        assertEquals(AutoProfileContext.OTHER,
                classify(false, "⏣ Village", "Purse: 1,000").orElseThrow());
    }

    @Test
    void idsAreUniqueStableAndRoundTrip() {
        Set<String> seen = new HashSet<>();
        for (AutoProfileContext context : AutoProfileContext.values()) {
            assertTrue(seen.add(context.id()), "duplicate id " + context.id());
            assertEquals(context,
                    AutoProfileContext.fromId(context.id()).orElseThrow());
            assertEquals(context,
                    AutoProfileContext.fromId(
                            "  " + context.id().toUpperCase() + " ")
                            .orElseThrow());
        }
        assertTrue(AutoProfileContext.fromId("nope").isEmpty());
        assertTrue(AutoProfileContext.fromId(null).isEmpty());
        assertFalse(AutoProfileContext.OTHER.hasRuleRow());
        assertTrue(AutoProfileContext.DUNGEONS.hasRuleRow());
    }
}
