package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonPuzzleBoardPolicyTest {
    @Test
    void iceFillPicksTheEasyPathWhenTheIdentifierAirPairMatches() {
        Map<String, String> blocks = new HashMap<>();
        blocks.put(key(16, 70, 7), "air");
        blocks.put(key(16, 70, 9), "packed_ice");
        List<DungeonPuzzleBoardPolicy.IceFillFloor> floors =
                DungeonPuzzleBoardPolicy.iceFillFloors(probe(blocks), false);
        assertFalse(floors.isEmpty());
        assertEquals(70, floors.getFirst().y());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(15, 70, 7), floors.getFirst().path().getFirst());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(15, 70, 10), floors.getFirst().path().getLast());
    }

    @Test
    void boulderSignatureLooksUpTheShippedClickList() {
        String signature = "010000010111101001010011100000101110000111";
        assertEquals(42, signature.length());
        List<DungeonPuzzleBoardPolicy.BoulderClick> clicks =
                DungeonPuzzleBoardPolicy.boulderClicks(signature);
        assertEquals(2, clicks.size());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(22, 65, 15), clicks.getFirst().render());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(23, 65, 15), clicks.getFirst().click());
        assertEquals(signature, DungeonPuzzleBoardPolicy.boulderSignature(rel -> {
            int index = ((24 - rel.z()) / 3) * 7 + ((24 - rel.x()) / 3);
            return signature.charAt(index) == '0';
        }));
    }

    @Test
    void waterBoardLooksUpLeversFromTerracottaPatternAndWoolSlots() {
        Map<String, String> blocks = new HashMap<>();
        blocks.put(key(14, 77, 27), "terracotta");
        blocks.put(key(15, 56, 19), "purple_wool");
        blocks.put(key(15, 56, 18), "orange_wool");
        blocks.put(key(15, 56, 17), "blue_wool");
        DungeonPuzzleBoardPolicy.BlockProbe probe = probe(blocks);
        assertEquals(0, DungeonPuzzleBoardPolicy.waterPatternId(probe).orElseThrow());
        assertEquals("012", DungeonPuzzleBoardPolicy.waterExtendedSlots(probe));
        List<DungeonPuzzleBoardPolicy.WaterLever> levers =
                DungeonPuzzleBoardPolicy.waterLevers(0, "012", false);
        assertTrue(levers.stream().anyMatch(lever ->
                lever.id().equals("gold_block") && lever.times().equals(List.of(0.0D))));
        assertTrue(levers.stream().anyMatch(lever ->
                lever.id().equals("emerald_block") && lever.times().equals(List.of(7.9D))));
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(20, 61, 15),
                levers.stream().filter(lever -> lever.id().equals("gold_block")).findFirst()
                        .orElseThrow().pos());
    }

    @Test
    void teleportMazeShipsThirtyRoomRelativePads() {
        assertEquals(30, DungeonPuzzleBoardPolicy.teleportMazePads().size());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(4, 69, 12),
                DungeonPuzzleBoardPolicy.teleportMazePads().getFirst());
        DungeonRoomDataPolicy.Rotation rot0 = new DungeonRoomDataPolicy.Rotation(0, 10, 20);
        DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(
                new DungeonPuzzleBoardPolicy.RelPos(4, 69, 12), rot0);
        assertEquals(14, world.x());
        assertEquals(69, world.y());
        assertEquals(32, world.z());
    }

    @Test
    void iceFillHardPathDiffersFromEasyOnTheSameIdentifier() {
        Map<String, String> blocks = new HashMap<>();
        blocks.put(key(14, 71, 14), "air");
        blocks.put(key(15, 71, 14), "packed_ice");
        DungeonPuzzleBoardPolicy.BlockProbe probe = probe(blocks);
        List<DungeonPuzzleBoardPolicy.IceFillFloor> easy =
                DungeonPuzzleBoardPolicy.iceFillFloors(probe, false);
        List<DungeonPuzzleBoardPolicy.IceFillFloor> hard =
                DungeonPuzzleBoardPolicy.iceFillFloors(probe, true);
        assertFalse(easy.isEmpty());
        assertFalse(hard.isEmpty());
        DungeonPuzzleBoardPolicy.IceFillFloor easyMid = easy.stream()
                .filter(floor -> floor.y() == 71)
                .findFirst()
                .orElseThrow();
        DungeonPuzzleBoardPolicy.IceFillFloor hardMid = hard.stream()
                .filter(floor -> floor.y() == 71)
                .findFirst()
                .orElseThrow();
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(15, 71, 12), easyMid.path().getFirst());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(15, 71, 11), hardMid.path().getFirst());
        assertFalse(easyMid.path().equals(hardMid.path()));
    }

    @Test
    void creeperBeamsShipsLanternPairsAndQuizOptionRels() {
        List<DungeonPuzzleBoardPolicy.CreeperPair> pairs =
                DungeonPuzzleBoardPolicy.creeperBeamPairs();
        assertFalse(pairs.isEmpty());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(15, 74, 15), pairs.getFirst().a());
        assertEquals(new DungeonPuzzleBoardPolicy.RelPos(15, 84, 13), pairs.getFirst().b());
        assertEquals(List.of(
                new DungeonPuzzleBoardPolicy.RelPos(20, 70, 6),
                new DungeonPuzzleBoardPolicy.RelPos(15, 70, 9),
                new DungeonPuzzleBoardPolicy.RelPos(10, 70, 6)),
                DungeonPuzzleBoardPolicy.quizOptionRels());
        assertTrue(DungeonPuzzleBoardPolicy.isSeaLantern("minecraft:sea_lantern"));
    }

    @Test
    void waterOptimizedSequenceDiffersFromTheDefault() {
        List<DungeonPuzzleBoardPolicy.WaterLever> easy =
                DungeonPuzzleBoardPolicy.waterLevers(0, "012", false);
        List<DungeonPuzzleBoardPolicy.WaterLever> optimized =
                DungeonPuzzleBoardPolicy.waterLevers(0, "012", true);
        assertTrue(easy.stream().anyMatch(lever -> lever.id().equals("gold_block")));
        assertTrue(optimized.stream().noneMatch(lever -> lever.id().equals("gold_block")));
        assertTrue(optimized.stream().anyMatch(lever ->
                lever.id().equals("water") && lever.times().equals(List.of(0.0D))));
    }

    private static DungeonPuzzleBoardPolicy.BlockProbe probe(Map<String, String> blocks) {
        return (x, y, z) -> blocks.getOrDefault(key(x, y, z), "air");
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}
