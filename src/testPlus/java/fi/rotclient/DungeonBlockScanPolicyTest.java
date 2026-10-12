package fi.rotclient;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonBlockScanPolicyTest {
    @Test void completesEveryPositionOnceWithinTheTickBudget() {
        var scan = new DungeonBlockScanPolicy();
        var region = new DungeonBlockScanPolicy.Region(-8, 65, -8, 17, 13, 17);
        var seen = new HashSet<DungeonPuzzlePolicy.WorldCell>();
        DungeonBlockScanPolicy.Slice slice;
        int ticks = 0;
        do {
            slice = scan.next(region);
            assertTrue(slice.cells().size() <= 384);
            for (var cell : slice.cells()) {
                assertTrue(region.contains(cell.x(), cell.y(), cell.z()));
                assertTrue(seen.add(cell));
            }
            ticks++;
        } while (!slice.complete());
        assertEquals(3757, seen.size());
        assertEquals(10, ticks);
        assertTrue(scan.next(region).started());
    }

    @Test void walkingDoesNotStarveTheRemainingCells() {
        var scan = new DungeonBlockScanPolicy();
        var region = new DungeonBlockScanPolicy.Region(0, 0, 0, 13, 13, 13);
        var seen = new HashSet<DungeonPuzzlePolicy.WorldCell>();
        for (int tick = 0; tick < 6; tick++) {
            var slice = scan.next(new DungeonBlockScanPolicy.Region(tick, 0, 0, 13, 13, 13));
            assertEquals(region, slice.region());
            seen.addAll(slice.cells());
        }
        assertEquals(2197, seen.size());
        assertEquals(6, scan.next(new DungeonBlockScanPolicy.Region(6, 0, 0, 13, 13, 13)).region().x());
    }

    @Test void warpAndClearDiscardAnOldTraversal() {
        var scan = new DungeonBlockScanPolicy();
        scan.next(new DungeonBlockScanPolicy.Region(0, 0, 0, 13, 13, 13));
        var distant = new DungeonBlockScanPolicy.Region(200, 0, 200, 13, 13, 13);
        assertTrue(scan.next(distant).started());
        scan.clear();
        assertTrue(scan.next(distant).started());
    }

    @Test void invalidAndOverflowingRegionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DungeonBlockScanPolicy.Region(0, 0, 0, 0, 13, 13));
        assertThrows(IllegalArgumentException.class, () -> new DungeonBlockScanPolicy.Region(0, 0, 0, 41, 41, 41));
        assertThrows(IllegalArgumentException.class, () -> new DungeonBlockScanPolicy.Region(Integer.MAX_VALUE, 0, 0, 2, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new DungeonBlockScanPolicy.Region(0, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
