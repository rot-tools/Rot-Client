package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class DungeonMapTerrainCacheTest {
    @Test
    void aRejectedHeldMapCannotEvictTheSelectedDungeonTerrainAcrossTicks() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object selectedMap = new Object();
        Object heldMap = new Object();
        byte[] selectedColors = mapColors();
        byte[] rejectedColors = new byte[128 * 128];
        DungeonMapPolicy.Board selected = cache.terrain(selectedMap, selectedColors, 0);

        for (int tick = 0; tick < 200; tick++) {
            assertFalse(cache.isDungeonMap(heldMap, rejectedColors, 0));
            assertTrue(cache.isDungeonMap(selectedMap, selectedColors, 0));
            assertSame(selected, cache.terrain(selectedMap, selectedColors, 0));
        }

        assertEquals(1, cache.decodeCount());
        assertEquals(1, cache.probeDecodeCount());
        assertEquals(1, cache.candidateCount());
    }

    @Test
    void aRejectedCandidateBecomesValidAfterAnInPlaceEntranceUpdate() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object selectedMap = new Object();
        Object candidateMap = new Object();
        byte[] selectedColors = mapColors();
        byte[] candidateColors = new byte[128 * 128];
        DungeonMapPolicy.Board selected = cache.terrain(selectedMap, selectedColors, 0);

        assertFalse(cache.isDungeonMap(candidateMap, candidateColors, 0));
        paintRoom(candidateColors, 22, 22, DungeonMapPolicy.COLOR_ENTRANCE);
        assertTrue(cache.isDungeonMap(candidateMap, candidateColors, 0));
        assertTrue(cache.isDungeonMap(candidateMap, candidateColors.clone(), 0));

        assertSame(selected, cache.terrain(selectedMap, selectedColors, 0));
        assertEquals(1, cache.decodeCount());
        assertEquals(2, cache.probeDecodeCount());
    }

    @Test
    void candidateProbesUseBoundedLruStorageWithReferenceIdentity() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object selectedMap = new Object();
        byte[] selectedColors = mapColors();
        DungeonMapPolicy.Board selected = cache.terrain(selectedMap, selectedColors, 0);
        byte[] rejectedColors = new byte[128 * 128];
        Object[] candidates = {
                new String("same-value"), new String("same-value"), new String("same-value"),
                new String("same-value"), new String("same-value")};

        for (int i = 0; i < 4; i++) {
            assertFalse(cache.isDungeonMap(candidates[i], rejectedColors, 0));
        }
        assertEquals(4, cache.candidateCount());
        assertEquals(4, cache.probeDecodeCount());
        // Keep the first candidate recent so the second one is evicted next.
        assertFalse(cache.isDungeonMap(candidates[0], rejectedColors, 0));
        assertFalse(cache.isDungeonMap(candidates[4], rejectedColors, 0));
        assertEquals(4, cache.candidateCount());
        assertEquals(5, cache.probeDecodeCount());
        assertFalse(cache.isDungeonMap(candidates[0], rejectedColors, 0));
        assertEquals(5, cache.probeDecodeCount());
        assertFalse(cache.isDungeonMap(candidates[1], rejectedColors, 0));
        assertEquals(4, cache.candidateCount());
        assertEquals(6, cache.probeDecodeCount());

        for (int candidate = 0; candidate < 100; candidate++) {
            assertFalse(cache.isDungeonMap(new Object(), rejectedColors, 0));
            assertEquals(4, cache.candidateCount());
        }
        assertSame(selected, cache.terrain(selectedMap, selectedColors, 0));
        assertEquals(1, cache.decodeCount());
    }

    @Test
    void selectedMapProbesRefreshInPlaceChangesWithoutUsingCandidateStorage() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        colors[30 * 128 + 50] = DungeonMapPolicy.COLOR_WHITE_CHECK;
        assertTrue(cache.isDungeonMap(map, colors, 0));
        DungeonMapPolicy.Board updated = cache.terrain(map, colors, 0);

        assertNotSame(first, updated);
        assertEquals(DungeonMapPolicy.Checkmark.WHITE, room(updated, 1, 0).checkmark());
        assertEquals(2, cache.decodeCount());
        assertEquals(0, cache.probeDecodeCount());
        assertEquals(0, cache.candidateCount());
    }

    @Test
    void malformedCandidateMapsCannotEvictTheSelectedTerrain() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object selectedMap = new Object();
        Object candidateMap = new Object();
        byte[] selectedColors = mapColors();
        DungeonMapPolicy.Board selected = cache.terrain(selectedMap, selectedColors, 0);

        assertFalse(cache.isDungeonMap(candidateMap, null, 0));
        assertFalse(cache.isDungeonMap(candidateMap, new byte[128 * 128 - 1], 0));
        assertFalse(cache.isDungeonMap(candidateMap, new byte[128 * 128 + 1], 0));
        assertFalse(cache.isDungeonMap(null, selectedColors, 0));

        assertSame(selected, cache.terrain(selectedMap, selectedColors, 0));
        assertEquals(1, cache.decodeCount());
        assertEquals(0, cache.probeDecodeCount());
        assertEquals(0, cache.candidateCount());
    }

    @Test
    void clearAlsoDropsCandidateObservations() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object selectedMap = new Object();
        Object candidateMap = new Object();
        byte[] selectedColors = mapColors();
        byte[] rejectedColors = new byte[128 * 128];
        DungeonMapPolicy.Board first = cache.terrain(selectedMap, selectedColors, 0);
        assertFalse(cache.isDungeonMap(candidateMap, rejectedColors, 0));

        cache.clear();
        assertEquals(0, cache.candidateCount());
        assertFalse(cache.isDungeonMap(candidateMap, rejectedColors, 0));
        assertEquals(2, cache.probeDecodeCount());
        assertNotSame(first, cache.terrain(selectedMap, selectedColors, 0));
        assertEquals(2, cache.decodeCount());
    }

    @Test
    void unchangedTerrainReusesOneDecodeAcrossTicksAndCopiedBuffers() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        assertTrue(first.calibration().ok());
        assertEquals(3, first.rooms().size());
        for (int tick = 0; tick < 200; tick++) {
            assertSame(first, cache.terrain(map, colors, 0));
        }
        assertSame(first, cache.terrain(map, colors.clone(), 0));
        assertEquals(1, cache.decodeCount());
    }

    @Test
    void anInPlaceRoomChangeRebuildsTerrainAndKeepsThePreviousBoardStable() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        paintRoom(colors, 42, 22, DungeonMapPolicy.COLOR_PUZZLE);
        DungeonMapPolicy.Board changed = cache.terrain(map, colors, 0);

        assertNotSame(first, changed);
        assertEquals(DungeonMapPolicy.RoomType.NORMAL, room(first, 1, 0).type());
        assertEquals(DungeonMapPolicy.RoomType.PUZZLE, room(changed, 1, 0).type());
        assertTrue(changed.summary().contains("2 puzzle"));
        assertSame(changed, cache.terrain(map, colors, 0));
        assertEquals(2, cache.decodeCount());
    }

    @Test
    void checkmarkChangesAreObservedWithoutAnArrayOrMapSwitch() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        colors[30 * 128 + 50] = DungeonMapPolicy.COLOR_WHITE_CHECK;
        DungeonMapPolicy.Board white = cache.terrain(map, colors, 0);
        colors[30 * 128 + 50] = DungeonMapPolicy.COLOR_ENTRANCE;
        DungeonMapPolicy.Board green = cache.terrain(map, colors, 0);

        assertEquals(DungeonMapPolicy.Checkmark.NONE, room(first, 1, 0).checkmark());
        assertEquals(DungeonMapPolicy.Checkmark.WHITE, room(white, 1, 0).checkmark());
        assertEquals(DungeonMapPolicy.Checkmark.GREEN, room(green, 1, 0).checkmark());
        assertEquals(3, cache.decodeCount());
    }

    @Test
    void openingAndRemovingADoorCannotLeaveTheOldDoorState() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board closed = cache.terrain(map, colors, 0);

        colors[30 * 128 + 40] = DungeonMapPolicy.COLOR_OPENED_WITHER;
        DungeonMapPolicy.Board opened = cache.terrain(map, colors, 0);
        colors[30 * 128 + 40] = 0;
        DungeonMapPolicy.Board removed = cache.terrain(map, colors, 0);

        assertEquals(DungeonMapPolicy.DoorType.WITHER, closed.doors().getFirst().type());
        assertEquals(DungeonMapPolicy.DoorType.OPENED_WITHER, opened.doors().getFirst().type());
        assertTrue(removed.doors().isEmpty());
        assertFalse(removed.summary().contains("doors"));
        assertEquals(3, cache.decodeCount());
    }

    @Test
    void everyPixelParticipatesInTheContentComparison() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        // This pixel is outside the scanned room/door samples. A full snapshot
        // still changes, even though the resulting terrain happens to be equal.
        colors[colors.length - 1] = DungeonMapPolicy.COLOR_NORMAL;
        DungeonMapPolicy.Board changed = cache.terrain(map, colors, 0);

        assertNotSame(first, changed);
        assertEquals(first, changed);
        assertSame(changed, cache.terrain(map, colors, 0));
        assertEquals(2, cache.decodeCount());
    }

    @Test
    void mapIdentityUsesReferencesAndOnlyTheLatestMapIsRetained() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        byte[] colors = mapColors();
        Object firstMap = new String("same-map-value");
        Object secondMap = new String("same-map-value");
        assertEquals(firstMap, secondMap);

        DungeonMapPolicy.Board first = cache.terrain(firstMap, colors, 0);
        DungeonMapPolicy.Board second = cache.terrain(secondMap, colors, 0);
        DungeonMapPolicy.Board returned = cache.terrain(firstMap, colors, 0);

        assertNotSame(first, second);
        assertNotSame(first, returned);
        assertNotSame(second, returned);
        assertEquals(3, cache.decodeCount());
    }

    @Test
    void floorChangesRecalibrateTheSameMapAndSwitchingBackDecodesAgain() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board entrance = cache.terrain(map, colors, 0);
        DungeonMapPolicy.Board laterFloor = cache.terrain(map, colors, 7);
        DungeonMapPolicy.Board returned = cache.terrain(map, colors, 0);

        assertEquals(22, entrance.calibration().startX());
        assertEquals(22, entrance.calibration().startZ());
        assertEquals(2, laterFloor.calibration().startX());
        assertEquals(2, laterFloor.calibration().startZ());
        assertNotSame(entrance, returned);
        assertEquals(entrance, returned);
        assertEquals(3, cache.decodeCount());
    }

    @Test
    void clearDropsThePreviousEntry() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        cache.clear();
        cache.clear();
        DungeonMapPolicy.Board rebuilt = cache.terrain(map, colors, 0);

        assertNotSame(first, rebuilt);
        assertEquals(first, rebuilt);
        assertEquals(2, cache.decodeCount());
    }

    @Test
    void malformedBuffersClearValidTerrainWithoutDecodingOrReturningStaleRooms() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board previous = cache.terrain(map, colors, 0);
        byte[][] invalidBuffers = {null, new byte[0], new byte[128 * 128 - 1], new byte[128 * 128 + 1]};

        for (byte[] invalid : invalidBuffers) {
            long previousCount = cache.decodeCount();
            DungeonMapPolicy.Board empty = cache.terrain(map, invalid, 0);
            assertEmpty(empty);
            assertEquals(previousCount, cache.decodeCount());

            DungeonMapPolicy.Board restored = cache.terrain(map, colors, 0);
            assertNotSame(previous, restored);
            assertEquals(previousCount + 1, cache.decodeCount());
            previous = restored;
        }
    }

    @Test
    void aMissingMapClearsThePreviousObservation() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board first = cache.terrain(map, colors, 0);

        assertEmpty(cache.terrain(null, colors, 0));
        assertEquals(1, cache.decodeCount());
        assertNotSame(first, cache.terrain(map, colors, 0));
        assertEquals(2, cache.decodeCount());
    }

    @Test
    void anUncalibratedMapIsCachedUntilItsPixelsProvideAnEntrance() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = new byte[128 * 128];
        DungeonMapPolicy.Board waiting = cache.terrain(map, colors, 0);

        assertEmpty(waiting);
        assertSame(waiting, cache.terrain(map, colors, 0));
        assertEquals(1, cache.decodeCount());

        paintRoom(colors, 22, 22, DungeonMapPolicy.COLOR_ENTRANCE);
        DungeonMapPolicy.Board discovered = cache.terrain(map, colors, 0);
        assertTrue(discovered.calibration().ok());
        assertEquals(1, discovered.rooms().size());
        assertEquals(2, cache.decodeCount());
    }

    @Test
    void playerObservationsCannotContaminateTheCachedTerrain() {
        DungeonMapTerrainCache cache = new DungeonMapTerrainCache();
        Object map = new Object();
        byte[] colors = mapColors();
        DungeonMapPolicy.Board terrain = cache.terrain(map, colors, 0);
        DungeonMapPolicy.PlayerIcon firstPlayer = new DungeonMapPolicy.PlayerIcon(30, 30, 0.0F, true);
        DungeonMapPolicy.PlayerIcon movedPlayer = new DungeonMapPolicy.PlayerIcon(50, 30, 90.0F, true);
        DungeonMapPolicy.Board firstFrame = withPlayers(terrain, List.of(firstPlayer));
        DungeonMapPolicy.Board movedFrame = withPlayers(cache.terrain(map, colors, 0), List.of(movedPlayer));

        assertEquals(List.of(firstPlayer), firstFrame.players());
        assertEquals(List.of(movedPlayer), movedFrame.players());
        assertTrue(terrain.players().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> terrain.players().add(firstPlayer));
        assertSame(terrain, cache.terrain(map, colors, 0));
        assertEquals(1, cache.decodeCount());
    }

    private static DungeonMapPolicy.Board withPlayers(
            DungeonMapPolicy.Board terrain, List<DungeonMapPolicy.PlayerIcon> players) {
        return new DungeonMapPolicy.Board(
                terrain.calibration(), terrain.rooms(), terrain.doors(), players, terrain.summary());
    }

    private static void assertEmpty(DungeonMapPolicy.Board board) {
        assertFalse(board.calibration().ok());
        assertTrue(board.rooms().isEmpty());
        assertTrue(board.doors().isEmpty());
        assertTrue(board.players().isEmpty());
        assertEquals("", board.summary());
    }

    private static DungeonMapPolicy.RoomTile room(DungeonMapPolicy.Board board, int tileX, int tileZ) {
        return board.rooms().stream()
                .filter(room -> room.tileX() == tileX && room.tileZ() == tileZ)
                .findFirst().orElseThrow();
    }

    private static byte[] mapColors() {
        byte[] colors = new byte[128 * 128];
        paintRoom(colors, 22, 22, DungeonMapPolicy.COLOR_ENTRANCE);
        paintRoom(colors, 42, 22, DungeonMapPolicy.COLOR_NORMAL);
        paintRoom(colors, 62, 22, DungeonMapPolicy.COLOR_PUZZLE);
        colors[30 * 128 + 40] = DungeonMapPolicy.COLOR_WITHER_DOOR;
        return colors;
    }

    private static void paintRoom(byte[] colors, int originX, int originZ, byte color) {
        for (int z = originZ; z < originZ + 16; z++) {
            for (int x = originX; x < originX + 16; x++) {
                colors[z * 128 + x] = color;
            }
        }
    }
}
