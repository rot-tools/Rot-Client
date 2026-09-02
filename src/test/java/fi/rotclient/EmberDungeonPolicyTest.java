package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EmberDungeonPolicyTest {
    @Test
    void lividWoolMapsToColorNames() {
        assertEquals("Hockey Livid", EmberDungeonPolicy.lividFromWool("minecraft:red_wool").orElseThrow());
        assertEquals("Vendetta Livid", EmberDungeonPolicy.lividFromWool("white_wool").orElseThrow());
        assertTrue(EmberDungeonPolicy.hologramMatchesLivid("Hockey Livid 2M❤", "Hockey Livid"));
        assertFalse(EmberDungeonPolicy.hologramMatchesLivid("Arcade Livid", "Hockey Livid"));
        assertTrue(EmberDungeonPolicy.isLividStart(
                "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid."));
    }

    @Test
    void thornWatcherAndBloodNames() {
        assertTrue(EmberDungeonPolicy.isThornHologram("Thorn 50M❤"));
        assertTrue(EmberDungeonPolicy.isWatcherHologram("The Watcher"));
        assertTrue(EmberDungeonPolicy.isBloodMobName("Revoker"));
        assertTrue(EmberDungeonPolicy.isBloodMobName("Mr. Deadly"));
        assertEquals(DungeonPolicy.EspKind.THORN, DungeonPolicy.classifyHologram("Thorn"));
        assertEquals(DungeonPolicy.EspKind.LIVID, DungeonPolicy.classifyHologram("Hockey Livid"));
    }

    @Test
    void terminalHitboxesAndI4Coords() {
        assertEquals(1, EmberDungeonPolicy.terminalSection(110, 113, 73));
        assertEquals(2, EmberDungeonPolicy.terminalSection(68, 109, 122));
        assertEquals(0, EmberDungeonPolicy.terminalSection(0, 0, 0));
        assertTrue(EmberDungeonPolicy.isInactiveTerminal("Inactive Terminal"));
        assertTrue(EmberDungeonPolicy.isActiveTerminal("Terminal Active"));
        assertTrue(EmberDungeonPolicy.isI4Block(66, 128, 50));
        assertTrue(EmberDungeonPolicy.isI4Lit("sea_lantern"));
        assertTrue(EmberDungeonPolicy.isDeviceDoneChat("Henri completed a device! (1/7)"));
    }

    @Test
    void arrowAlignMatchesKnownSolutions() {
        int[] rotations = {
                7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, -1, -1, 7, 0
        };
        int[] solution = EmberDungeonPolicy.matchArrowSolution(rotations).orElseThrow();
        assertEquals(1, solution[24]);
        List<EmberDungeonPolicy.ArrowClicks> remaining =
                EmberDungeonPolicy.remainingArrowClicks(rotations, solution);
        assertEquals(1, remaining.size());
        assertEquals(24, remaining.getFirst().index());
        assertEquals(1, remaining.getFirst().clicks());
        assertEquals(0, EmberDungeonPolicy.arrowIndex(-2, 120, 75));
        assertEquals(1, EmberDungeonPolicy.arrowClicks(7, 0));
        assertEquals(0xFF22C55E, EmberDungeonPolicy.arrowColor(1));
    }

    @Test
    void relicsGatesLeapAndUltimates() {
        assertEquals("Red", EmberDungeonPolicy.relicPickup(
                "Henri picked the Corrupted Red Relic!").orElseThrow());
        assertEquals(5, EmberDungeonPolicy.relics().size());
        assertTrue(EmberDungeonPolicy.gateForSection(1).isPresent());
        assertEquals(1, EmberDungeonPolicy.p3Section(110, 120, 80));
        assertEquals("SS", EmberDungeonPolicy.leapRegionAt(107, 120, 94).orElseThrow().id());
        assertEquals("§41§9/3 Players Leaped", EmberDungeonPolicy.leapCounterText(1, 3));
        assertTrue(EmberDungeonPolicy.shouldFireUltimate(
                "⚠ Maxor is enraged! ⚠", "F7", DungeonPolicy.DungeonClass.HEALER));
        assertFalse(EmberDungeonPolicy.shouldFireUltimate(
                "⚠ Maxor is enraged! ⚠", "F7", DungeonPolicy.DungeonClass.MAGE));
        assertTrue(EmberDungeonPolicy.isClassUltimateItem("Wish", List.of("Ultimate Ability")));
        assertTrue(EmberDungeonPolicy.usesVanillaDropForClassAbility());
        assertTrue(EmberDungeonPolicy.isLeapMotionPacket("ClientboundTeleportEntityPacket"));
        assertTrue(EmberDungeonPolicy.isIceSprayItem("Ice Spray Wand", List.of()));
        assertTrue(EmberDungeonPolicy.isGravityWandItem("Gyrokinetic Wand", List.of()));
        assertTrue(EmberDungeonPolicy.isPuzzleTimerEnd("You completed the puzzle!"));
        assertEquals("minecraft:black_stained_glass",
                EmberDungeonPolicy.glassBlockId(EmberDungeonPolicy.GlassTint.BLACK));
        assertEquals(EmberDungeonPolicy.DebuffPhase.P3,
                EmberDungeonPolicy.debuffPhase("[BOSS] Goldor: Who dares trespass into my domain?"));
        assertEquals("Debuff P3", EmberDungeonPolicy.debuffLabel(EmberDungeonPolicy.DebuffPhase.P3));
    }

    @Test
    void hateDoorsGlassAndClassIcons() {
        assertEquals(EmberDungeonPolicy.GlassTint.BLACK, EmberDungeonPolicy.glassTint("Black"));
        assertTrue(EmberDungeonPolicy.isHateDoorBlock("coal_block", true, false, false));
        assertTrue(EmberDungeonPolicy.isHateDoorBlock("red_stained_glass", false, true, false));
        assertEquals("A", EmberDungeonPolicy.classLetter(DungeonPolicy.DungeonClass.ARCHER));
        assertEquals(0xFF55FFFF, EmberDungeonPolicy.classColor(DungeonPolicy.DungeonClass.MAGE));
        var classes = EmberDungeonPolicy.teammateClasses(List.of("[Archer] Henri", "Bob Mage"));
        assertEquals(DungeonPolicy.DungeonClass.ARCHER, classes.get("Henri"));
        assertEquals(DungeonPolicy.DungeonClass.MAGE, classes.get("Bob"));
        assertEquals(7, EmberDungeonPolicy.floorNumber("M7"));
        var roster = EmberDungeonPolicy.teammateClasses(List.of(
                "[Archer] Henri",
                "[Archer] Alice",
                "[Mage] Bob"));
        assertEquals(
                List.of(DungeonPolicy.DungeonClass.ARCHER),
                EmberDungeonPolicy.duplicateClasses(roster));
        assertEquals("Duplicate Archer", EmberDungeonPolicy.duplicateClassTitle(
                EmberDungeonPolicy.duplicateClasses(roster)));
        assertTrue(EmberDungeonPolicy.duplicateClasses(Map.of()).isEmpty());
    }
}
