package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonPuzzlePolicyTest {
    @Test
    void iceFillCoversEveryTileOnce() {
        boolean[][] ice = {
                {true, true, false},
                {false, true, true}
        };
        List<DungeonPuzzlePolicy.Cell> path = DungeonPuzzlePolicy.iceFillPath(ice, 0, 0);
        assertEquals(4, path.size());
        assertEquals(new DungeonPuzzlePolicy.Cell(0, 0), path.getFirst());
        assertEquals(new DungeonPuzzlePolicy.Cell(2, 1), path.getLast());
    }

    @Test
    void waterBoardTogglesLeversUntilGoalsFlood() {
        char[][] grid = {
                {'S', '0', 'C'},
                {'#', '1', 'C'}
        };
        boolean[] closed = {false, false};
        List<Integer> clicks = DungeonPuzzlePolicy.waterLeversToToggle(grid, closed);
        assertEquals(List.of(0, 1), clicks);
        assertTrue(DungeonPuzzlePolicy.waterReachesGoals(grid, 0b11, 2));
        assertFalse(DungeonPuzzlePolicy.waterReachesGoals(grid, 0, 2));
    }

    @Test
    void boulderPushOpensAPathToTheChest() {
        char[][] grid = {
                {'#', '#', '#', '#', '#'},
                {'#', 'P', 'O', '.', '#'},
                {'#', '#', '.', 'C', '#'}
        };
        List<DungeonPuzzlePolicy.BoulderStep> steps = DungeonPuzzlePolicy.boulderPath(grid);
        assertFalse(steps.isEmpty());
        assertTrue(steps.getFirst().push());
        assertEquals(2, steps.getFirst().x());
    }

    @Test
    void teleportPathFollowsObservedPadPairs() {
        DungeonPuzzlePolicy.WorldCell a = new DungeonPuzzlePolicy.WorldCell(0, 70, 0);
        DungeonPuzzlePolicy.WorldCell b = new DungeonPuzzlePolicy.WorldCell(20, 70, 0);
        DungeonPuzzlePolicy.WorldCell c = new DungeonPuzzlePolicy.WorldCell(40, 70, 0);
        Map<String, String> hops = new LinkedHashMap<>();
        hops.put(DungeonPuzzlePolicy.cellKey(a), DungeonPuzzlePolicy.cellKey(b));
        hops.put(DungeonPuzzlePolicy.cellKey(b), DungeonPuzzlePolicy.cellKey(c));
        List<DungeonPuzzlePolicy.WorldCell> path = DungeonPuzzlePolicy.teleportPath(
                List.of(a, b, c), hops, a, c);
        assertEquals(List.of(a, b, c), path);
    }

    @Test
    void weirdoChestPicksTheChestBesideTheTruthNpc() {
        List<DungeonPuzzlePolicy.NamedPos> npcs = List.of(
                new DungeonPuzzlePolicy.NamedPos("Liar", 1, 70, 1),
                new DungeonPuzzlePolicy.NamedPos("Relieved", 8, 70, 1));
        List<DungeonPuzzlePolicy.WorldCell> chests = List.of(
                new DungeonPuzzlePolicy.WorldCell(1, 70, 2),
                new DungeonPuzzlePolicy.WorldCell(8, 70, 2));
        assertEquals(new DungeonPuzzlePolicy.WorldCell(8, 70, 2),
                DungeonPuzzlePolicy.weirdoChest(npcs, chests, "Relieved").orElseThrow());
    }

    @Test
    void dungeonMapPreviewMarksThePlayerCell() {
        byte[] colors = new byte[128 * 128];
        colors[10 * 128 + 10] = (byte) (3 * 4); // wool / room
        colors[12 * 128 + 12] = (byte) (25 * 4); // blue / player
        DungeonPuzzlePolicy.MapPreview preview = DungeonPuzzlePolicy.previewDungeonMap(colors, 128, 5);
        assertEquals(5, preview.width());
        assertTrue(preview.summary().contains("rooms"));
        assertEquals(0xFF22C55E, preview.argb()[preview.playerZ() * 5 + preview.playerX()]);
        assertEquals(DungeonPuzzlePolicy.MapKind.BLOOD, DungeonPuzzlePolicy.classifyDungeonMapColor(28 * 4));
        assertEquals(DungeonPuzzlePolicy.MapKind.PUZZLE, DungeonPuzzlePolicy.classifyDungeonMapColor(17 * 4));
    }

    @Test
    void simonWallKeepsTheKnownStartButton() {
        assertTrue(DungeonPuzzlePolicy.isSimonWall(110, 121, 91));
        assertTrue(DungeonPolicy.isSimonStart(110, 121, 91));
        assertFalse(DungeonPuzzlePolicy.isSimonWall(0, 70, 0));
    }
}
