package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static fi.rotclient.DungeonRoomDataPolicy.*;

final class DungeonModernRoomPolicyTest {
    @Test void usesNoammModernBlockNameFoldInsteadOfLegacyNumericCore() {
        var stone = Collections.nCopies(129, "minecraft:stone");
        assertEquals(-1199202185, DungeonModernRoomPolicy.hashColumn(stone).orElseThrow());
        assertNotEquals(DungeonRoomDataPolicy.hashColumn(stone), DungeonModernRoomPolicy.hashColumn(stone).orElseThrow());
        var ignored = new ArrayList<>(Collections.nCopies(129, "minecraft:air"));
        ignored.set(0, "minecraft:chest"); ignored.set(1, "minecraft:spruce_planks");
        ignored.set(2, "minecraft:water"); ignored.set(3, "minecraft:moving_piston");
        assertEquals(DungeonModernRoomPolicy.hashColumn(Collections.nCopies(129, "minecraft:air")),
                DungeonModernRoomPolicy.hashColumn(ignored));
        assertEquals(DungeonModernRoomPolicy.hashColumn(stone),
                DungeonModernRoomPolicy.hashColumn(Collections.nCopies(129, "stone[variant=smooth]")));
    }

    @Test void rejectsIncompleteColumnsAndLeavesUnknownCoreUnidentified() {
        assertTrue(DungeonModernRoomPolicy.hashColumn(Collections.nCopies(128, "minecraft:stone")).isEmpty());
        var bad = new ArrayList<>(Collections.nCopies(129, "minecraft:stone"));
        bad.set(0, null);
        assertTrue(DungeonModernRoomPolicy.hashColumn(bad).isEmpty());
        assertTrue(DungeonModernRoomPolicy.identify(Collections.nCopies(129, "minecraft:air")).isEmpty());
    }

    @Test void shipsPinned140RoomDatasetAndTypedSecretCoordinates() {
        var catalog = DungeonModernRoomPolicy.catalog();
        assertEquals(140, catalog.rooms().size());
        var room = catalog.byCore().get(210009410);
        assertEquals("Criss Cross", room.meta().name());
        assertEquals(List.of(new SecretWaypoint(SecretKind.CHEST, 15, 66, 6)), room.secrets());
        assertTrue(DungeonModernRoomPolicy.owns(room.meta()));
        assertFalse(DungeonModernRoomPolicy.owns(new RoomMeta("Old", "NORMAL", 1, List.of(), 1, "1x1")));
        assertTrue(catalog.rooms().stream().anyMatch(r -> r.secrets().stream().anyMatch(s -> s.kind() == SecretKind.BAT)));
        assertTrue(catalog.rooms().stream().anyMatch(r -> r.secrets().stream().anyMatch(s -> s.kind() == SecretKind.ESSENCE)));
    }

    @Test void doesNotAssignCollidingCoreToArbitraryRoom() {
        var catalog = DungeonModernRoomPolicy.parse("""
                [{"name":"A","type":"NORMAL","shape":"1x1","cores":[123]},
                 {"name":"B","type":"NORMAL","shape":"1x1","cores":[123]}]
                """);
        assertFalse(catalog.byCore().containsKey(123));
    }

    @Test void checksAllFourOuterCornersOfMultiTileRoom() {
        var room = new RoomMeta("Multi", "NORMAL", 1, List.of(), -1, "1x2");
        var tiles = List.of(new MapTile(0, 0), new MapTile(1, 0));
        List<Rotation> expected = List.of(new Rotation(0, -200, -200), new Rotation(90, -138, -200),
                new Rotation(180, -138, -170), new Rotation(270, -200, -170));
        for (var rotation : expected) {
            assertEquals(rotation, DungeonModernRoomPolicy.resolveRotation(room, tiles, 100, (x,y,z) ->
                    x == rotation.cornerX() && y == 100 && z == rotation.cornerZ()
                            ? "minecraft:blue_terracotta" : "minecraft:air").orElseThrow());
        }
    }

    @Test void waitsForCompleteShapeAndLoadedUnambiguousCorners() {
        var room = new RoomMeta("Multi", "NORMAL", 1, List.of(), -1, "2x2");
        var full = List.of(new MapTile(0,0), new MapTile(0,1), new MapTile(1,0), new MapTile(1,1));
        assertTrue(DungeonModernRoomPolicy.resolveRotation(room, full.subList(0,3), 100,
                (x,y,z) -> "minecraft:blue_terracotta").isEmpty());
        assertTrue(DungeonModernRoomPolicy.resolveRotation(room, full, 100,
                (x,y,z) -> "minecraft:blue_terracotta").isEmpty());
        assertTrue(DungeonModernRoomPolicy.resolveRotation(room, full, 100,
                (x,y,z) -> x == -200 ? "minecraft:blue_terracotta" : "").isEmpty());
        assertTrue(DungeonModernRoomPolicy.resolveRotation(room,
                List.of(new MapTile(0,0),new MapTile(2,0),new MapTile(0,2),new MapTile(2,2)), 100,
                (x,y,z) -> "minecraft:blue_terracotta").isEmpty());
    }

    @Test void lRoomChecksCornersInsideItsBoundingRectangle() {
        var room = new RoomMeta("L", "NORMAL", 1, List.of(), -1, "L");
        var tiles = List.of(new MapTile(0,0),new MapTile(1,0),new MapTile(0,1));
        assertEquals(new Rotation(180,-170,-170), DungeonModernRoomPolicy.resolveRotation(room, tiles, 100,
                (x,y,z) -> x == -170 && z == -170 ? "minecraft:blue_terracotta" : "minecraft:air").orElseThrow());
    }

    @Test void heightScanCanFindClayAboveHashRangeAndIgnoresAllAirAndGold() {
        assertEquals(160, DungeonModernRoomPolicy.highestY(-185,-185,
                (x,y,z) -> y == 256 ? "minecraft:gold_block" : y == 160 ? "minecraft:stone" : "minecraft:cave_air"));
        assertEquals(0, DungeonModernRoomPolicy.highestY(-185,-185,(x,y,z) -> ""));
    }

    @Test void modernSecretPlacementUsesTheSameFourRotationsAsNoamm() {
        var room = DungeonModernRoomPolicy.catalog().byCore().get(210009410).meta();
        int[][] offsets = {{15,6},{-6,15},{-15,-6},{6,-15}};
        for (int i=0;i<4;i++) {
            var waypoint = DungeonModernRoomPolicy.placeSecrets(room, new Rotation(i*90,-200,-200)).getFirst();
            assertEquals(-200+offsets[i][0], waypoint.x());
            assertEquals(66, waypoint.y());
            assertEquals(-200+offsets[i][1], waypoint.z());
        }
    }
}
