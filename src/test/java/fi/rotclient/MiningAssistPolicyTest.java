package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MiningAssistPolicyTest {
    @Test
    void metalDetectorFiltersGateOffsets() {
        MiningAssistPolicy.BlockPos center = MiningAssistPolicy.minesCenter("Diamond", 0, 64, 0).orElseThrow();
        assertEquals(33, center.x());
        assertEquals(3, center.z());
        List<MiningAssistPolicy.BlockPos> chests = MiningAssistPolicy.knownChests(center);
        assertEquals(42, chests.size());
        MiningAssistPolicy.BlockPos target = chests.getFirst();
        double dist = target.distance(target.x() + 0.5D, target.y() + 0.5D, target.z() + 0.5D);
        List<MiningAssistPolicy.BlockPos> hit = MiningAssistPolicy.filterByDistance(
                chests, target.x() + 0.5D, target.y() + 0.5D, target.z() + 0.5D, dist);
        assertFalse(hit.isEmpty());
        assertTrue(MiningAssistPolicy.shouldRenderDetector(hit));
        assertTrue(MiningAssistPolicy.parseTreasureMeters("§3§lTREASURE: §b12.5m").isPresent());
        assertTrue(MiningAssistPolicy.isDetectorFoundChat(
                "You found a chest with your Metal Detector!"));
    }

    @Test
    void wishingCompassIntersectsTwoLines() {
        MiningAssistPolicy.Vec3 startA = new MiningAssistPolicy.Vec3(0, 70, 5);
        MiningAssistPolicy.Vec3 startB = new MiningAssistPolicy.Vec3(5, 70, 0);
        MiningAssistPolicy.Vec3 hit = MiningAssistPolicy.intersectCompassLines(
                startA,
                new MiningAssistPolicy.Vec3(1, 0, 0),
                startB,
                new MiningAssistPolicy.Vec3(0, 0, 1)).orElseThrow();
        assertEquals(5.0D, hit.x(), 0.01D);
        assertEquals(5.0D, hit.z(), 0.01D);
        assertEquals(
                MiningAssistPolicy.HollowsZone.JUNGLE,
                MiningAssistPolicy.zoneAt(250, 80, 250));
        assertTrue(MiningAssistPolicy.isCompassFail(
                "The Wishing Compass can't seem to locate anything!"));
        MiningAssistPolicy.Vec3 temple = MiningAssistPolicy.jungleTempleFromJungleGuess(hit);
        assertEquals(hit.x() - 57.0D, temple.x(), 0.01D);
    }

    @Test
    void fossilHeatmapPrefersUnknownCellsOnHelixPercent() {
        MiningAssistPolicy.FossilTile[] board =
                new MiningAssistPolicy.FossilTile[MiningAssistPolicy.FOSSIL_SLOTS];
        for (int i = 0; i < board.length; i++) {
            board[i] = MiningAssistPolicy.FossilTile.UNKNOWN;
        }
        assertEquals(
                MiningAssistPolicy.FossilTile.FOSSIL,
                MiningAssistPolicy.tileFromItem("minecraft:white_stained_glass_pane"));
        assertEquals(
                8,
                MiningAssistPolicy.parseChiselCharges("Chisel Charges Remaining: 8").orElseThrow());
        double[] heat = MiningAssistPolicy.fossilHeatmap(board, "7.1");
        int best = MiningAssistPolicy.bestFossilSlot(heat);
        assertTrue(best >= 0);
        assertTrue(heat[best] > 0.0D);
    }

    @Test
    void commissionOreTintAndGreyscale() {
        assertTrue(MiningAssistPolicy.blocksForCommission("Ruby Gemstone Collector")
                .contains("red_stained_glass"));
        assertTrue(MiningAssistPolicy.isCommissionOre(
                List.of("Ruby Gemstone Collector"), "minecraft:red_stained_glass_pane"));
        assertTrue(MiningAssistPolicy.greyscaleCommissionOre(
                List.of("Ruby Gemstone Collector"), "packed_ice"));
        assertFalse(MiningAssistPolicy.greyscaleCommissionOre(
                List.of("Glacite Collector"), "packed_ice"));
    }

    @Test
    void breakPredictionMatchesGateSoftcap() {
        assertEquals(500, MiningAssistPolicy.oreStrength("gray_wool"));
        assertEquals(2000, MiningAssistPolicy.oreStrength("polished_diorite"));
        assertEquals(6000, MiningAssistPolicy.oreStrength("packed_ice"));
        assertTrue(MiningAssistPolicy.parseMiningSpeed("Mining Speed: 1234").isPresent());
        assertTrue(MiningAssistPolicy.canBreakPredicted("gray_wool", 5));
        assertFalse(MiningAssistPolicy.canBreakPredicted("packed_ice", 5));
        double ms = MiningAssistPolicy.breakTimeMs(500, 500);
        assertTrue(ms > 0.0D);
        assertTrue(MiningAssistPolicy.ticksToBreak(500, 500 * 60) == 1);
    }

    @Test
    void hotmPresetRoundTripsPerkNames() {
        String encoded = MiningAssistPolicy.encodeHotmPreset(List.of("Mining Speed", "Powder Buff"));
        assertTrue(MiningAssistPolicy.isHotmPreset(encoded));
        assertEquals(
                List.of("Mining Speed", "Powder Buff"),
                MiningAssistPolicy.parseHotmPreset(encoded));
        assertTrue(MiningAssistPolicy.highlightHotmPerk(
                Set.copyOf(MiningAssistPolicy.parseHotmPreset(encoded)), "Mining Speed"));
        assertEquals(7, MiningAssistPolicy.parseHotmTier("Tier 7").orElseThrow());
    }

    @Test
    void glaciteTunnelDijkstraAndNodeKinds() {
        assertEquals(
                MiningAssistPolicy.TunnelKind.CAMPFIRE,
                MiningAssistPolicy.tunnelKind("Base Campfire"));
        assertEquals(
                MiningAssistPolicy.TunnelKind.NEW_GEM,
                MiningAssistPolicy.tunnelKind("Aquamarine Vein"));
        assertEquals("Ruby", MiningAssistPolicy.collectorGoal("Ruby Collector").orElseThrow());
        List<MiningAssistPolicy.GraphNode> nodes = List.of(
                new MiningAssistPolicy.GraphNode("a", 0, 0, 0, "Campfire"),
                new MiningAssistPolicy.GraphNode("b", 10, 0, 0, "Ruby"),
                new MiningAssistPolicy.GraphNode("c", 20, 0, 0, "Umber"));
        List<MiningAssistPolicy.GraphEdge> edges = List.of(
                new MiningAssistPolicy.GraphEdge("a", "b", 10),
                new MiningAssistPolicy.GraphEdge("b", "c", 10));
        assertEquals("a", MiningAssistPolicy.nearestNode(nodes, 1, 0, 0));
        assertEquals(List.of("a", "b", "c"), MiningAssistPolicy.shortestPath(nodes, edges, "a", "c"));
    }

    @Test
    void miningIgnoreSkipsGemstoneFlickerButAppliesAir() {
        assertTrue(MiningAssistPolicy.isGemstoneBlock("minecraft:red_stained_glass"));
        assertTrue(MiningAssistPolicy.sameMiningFamily(
                "minecraft:red_stained_glass", "minecraft:purple_stained_glass"));
        assertTrue(MiningAssistPolicy.shouldIgnoreMiningUpdate(
                true, true, true,
                "minecraft:red_stained_glass", "minecraft:red_stained_glass"));
        assertFalse(MiningAssistPolicy.shouldIgnoreMiningUpdate(
                true, true, true,
                "minecraft:red_stained_glass", "minecraft:air"));
        assertFalse(MiningAssistPolicy.shouldIgnoreMiningUpdate(
                true, true, false,
                "minecraft:red_stained_glass", "minecraft:red_stained_glass"));
    }
}
