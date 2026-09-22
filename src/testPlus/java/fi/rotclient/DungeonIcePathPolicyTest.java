package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonIcePathPolicyTest {
    @Test
    void emptyIceSlidesSouthFromTheNorthEdgeToTheHole() {
        boolean[][] ice = emptyIce();
        List<DungeonIcePathPolicy.GridPos> path =
                DungeonIcePathPolicy.solve(ice, 7, 0);
        assertEquals(2, path.size());
        assertEquals(new DungeonIcePathPolicy.GridPos(7, 0), path.getFirst());
        assertEquals(new DungeonIcePathPolicy.GridPos(
                DungeonIcePathPolicy.GOAL_X, DungeonIcePathPolicy.GOAL_Z), path.getLast());
        List<DungeonIcePathPolicy.GridPos> cells = DungeonIcePathPolicy.expandPath(path);
        assertEquals(19, cells.size());
        assertEquals(new DungeonIcePathPolicy.GridPos(7, 18), cells.getLast());
    }

    @Test
    void wallOnTheSouthLaneForcesADetourThenTheHole() {
        boolean[][] ice = emptyIce();
        ice[10][7] = true;
        ice[18][6] = true;
        List<DungeonIcePathPolicy.GridPos> path =
                DungeonIcePathPolicy.solve(ice, 7, 0);
        assertTrue(path.size() >= 3);
        assertEquals(new DungeonIcePathPolicy.GridPos(7, 0), path.getFirst());
        assertEquals(new DungeonIcePathPolicy.GridPos(
                DungeonIcePathPolicy.GOAL_X, DungeonIcePathPolicy.GOAL_Z), path.getLast());
        assertTrue(DungeonIcePathPolicy.expandPath(path).stream()
                .noneMatch(cell -> cell.x() == 7 && cell.z() == 10));
    }

    @Test
    void clayRelativeCellsMatchTheHashedRoomGrid() {
        assertEquals(
                new DungeonPuzzleBoardPolicy.RelPos(7, 67, 7),
                DungeonIcePathPolicy.relPos(0, 0));
        assertEquals(
                new DungeonPuzzleBoardPolicy.RelPos(14, 67, 25),
                DungeonIcePathPolicy.relPos(7, 18));
        assertEquals(
                new DungeonIcePathPolicy.GridPos(7, 18),
                DungeonIcePathPolicy.gridFromRel(14, 25).orElseThrow());
        assertTrue(DungeonIcePathPolicy.gridFromRel(0, 0).isEmpty());
    }

    private static boolean[][] emptyIce() {
        boolean[][] ice = new boolean[DungeonIcePathPolicy.GRID][DungeonIcePathPolicy.GRID];
        return ice;
    }
}
