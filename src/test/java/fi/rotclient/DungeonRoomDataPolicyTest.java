package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonRoomDataPolicyTest {
    @Test
    void shippedRelicRoomsAndWaypointsLoad() {
        DungeonRoomDataPolicy.Catalog catalog = DungeonRoomDataPolicy.catalog();
        assertTrue(catalog.rooms().size() > 80);
        assertEquals("Waterfall", catalog.rooms().getFirst().name());
        assertTrue(DungeonRoomDataPolicy.roomForCore(740310812).isPresent());
        assertEquals("Waterfall", DungeonRoomDataPolicy.roomForCore(740310812).orElseThrow().name());
        List<DungeonRoomDataPolicy.SecretWaypoint> waterfall =
                catalog.waypointsByRoomId().get(0);
        assertTrue(waterfall.size() >= 8);
        assertTrue(waterfall.stream().anyMatch(w ->
                w.kind() == DungeonRoomDataPolicy.SecretKind.CHEST && w.x() == 34));
    }

    @Test
    void hashColumnMatchesJavaStringHashOfLegacyIds() {
        List<String> column = new ArrayList<>();
        for (int y = DungeonRoomDataPolicy.HASH_Y_TOP; y >= DungeonRoomDataPolicy.HASH_Y_BOTTOM; y--) {
            column.add("minecraft:air");
        }
        assertEquals("0".repeat(column.size()).hashCode(), DungeonRoomDataPolicy.hashColumn(column));
        assertEquals(Integer.valueOf(173), DungeonRoomDataPolicy.legacyId("coal_block"));
        assertEquals(Integer.valueOf(1), DungeonRoomDataPolicy.legacyId("minecraft:stone"));
    }

    @Test
    void fromCompRotatesLikeRelic() {
        DungeonRoomDataPolicy.Rotation rot0 = new DungeonRoomDataPolicy.Rotation(0, 10, 20);
        DungeonRoomDataPolicy.IntVec placed = DungeonRoomDataPolicy.fromComp(4, 70, 6, rot0);
        assertEquals(14, placed.x());
        assertEquals(70, placed.y());
        assertEquals(26, placed.z());
        DungeonRoomDataPolicy.Rotation rot90 = new DungeonRoomDataPolicy.Rotation(90, 0, 0);
        DungeonRoomDataPolicy.IntVec spun = DungeonRoomDataPolicy.fromComp(10, 5, 4, rot90);
        assertEquals(-4, spun.x());
        assertEquals(10, spun.z());
        DungeonRoomDataPolicy.IntVec back = DungeonRoomDataPolicy.toComp(
                spun.x(), spun.y(), spun.z(), rot90);
        assertEquals(10, back.x());
        assertEquals(5, back.y());
        assertEquals(4, back.z());
        assertFalse(DungeonRoomDataPolicy.secretBlockStillPresent(
                DungeonRoomDataPolicy.SecretKind.CHEST, "minecraft:air"));
        assertTrue(DungeonRoomDataPolicy.secretBlockStillPresent(
                DungeonRoomDataPolicy.SecretKind.CHEST, "minecraft:trapped_chest"));
        assertTrue(DungeonRoomDataPolicy.secretBlockStillPresent(
                DungeonRoomDataPolicy.SecretKind.ITEM, "minecraft:air"));
        assertFalse(DungeonRoomDataPolicy.secretBlockStillPresent(
                DungeonRoomDataPolicy.SecretKind.ESSENCE, "minecraft:air"));
        DungeonRoomDataPolicy.PlacedWaypoint chest = new DungeonRoomDataPolicy.PlacedWaypoint(
                DungeonRoomDataPolicy.SecretKind.CHEST, 4, 70, 6, "Crypt");
        assertEquals(chest, DungeonRoomDataPolicy.matchingSecret(
                List.of(chest), Set.of(), 4, 70, 6, 0).orElseThrow());
        assertTrue(DungeonRoomDataPolicy.matchingSecret(
                List.of(chest), Set.of("4,70,6"), 4, 70, 6, 0).isEmpty());
        assertEquals(-200, DungeonRoomDataPolicy.roomOrigin(-185));
        assertEquals(-185, DungeonRoomDataPolicy.roomCenter(-185));
        assertEquals(-168, DungeonRoomDataPolicy.roomOrigin(-153));
        assertEquals(-153, DungeonRoomDataPolicy.roomCenter(-153));
        assertTrue(DungeonRoomDataPolicy.isBlueTerracotta("minecraft:blue_terracotta"));
        assertEquals(4, DungeonRoomDataPolicy.candidateRotations(-185, -185).size());
    }

    @Test
    void placeSecretsUsesRoomIdAndRotation() {
        DungeonRoomDataPolicy.RoomMeta waterfall =
                DungeonRoomDataPolicy.roomForCore(740310812).orElseThrow();
        List<DungeonRoomDataPolicy.PlacedWaypoint> placed = DungeonRoomDataPolicy.placeSecrets(
                waterfall, new DungeonRoomDataPolicy.Rotation(0, 0, 0));
        assertFalse(placed.isEmpty());
        assertEquals("Chest", DungeonRoomDataPolicy.secretLabel(
                DungeonRoomDataPolicy.SecretKind.CHEST));
        assertEquals("Bat", DungeonRoomDataPolicy.secretLabel(
                DungeonRoomDataPolicy.SecretKind.BAT));
    }

    @Test
    void highestBlockSkipsAirAndGoldThenStopsOnStone() {
        List<String> column = airColumn();
        column.set(DungeonRoomDataPolicy.HASH_Y_TOP - 90, "minecraft:gold_block");
        column.set(DungeonRoomDataPolicy.HASH_Y_TOP - 80, "minecraft:stone");
        assertEquals(80, DungeonRoomDataPolicy.highestBlock(column));
    }

    @Test
    void oneByOneClayUsesHighestBlockNotY69() {
        DungeonRoomDataPolicy.RoomMeta room = oneByOneRoom("Crypt");
        int cx = -185;
        int cz = -185;
        DungeonRoomDataPolicy.Rotation expected =
                DungeonRoomDataPolicy.candidateRotations(cx, cz).getFirst();
        DungeonRoomDataPolicy.BlockAt world = (x, y, z) -> {
            if (x == expected.cornerX() && z == expected.cornerZ() && y == 80) {
                return "minecraft:blue_terracotta";
            }
            return "minecraft:air";
        };
        Optional<DungeonRoomDataPolicy.Rotation> found =
                DungeonRoomDataPolicy.resolveRotation(room, cx, cz, 80, List.of(), world);
        assertTrue(found.isPresent());
        assertEquals(expected, found.orElseThrow());
        assertTrue(DungeonRoomDataPolicy.resolveRotation(
                room, cx, cz, 69, List.of(), world).isEmpty());
    }

    @Test
    void missingClayDoesNotInventNorthWestRotation() {
        DungeonRoomDataPolicy.RoomMeta room = oneByOneRoom("Crypt");
        assertTrue(DungeonRoomDataPolicy.resolveRotation(
                room, -185, -185, 80, List.of(), (x, y, z) -> "minecraft:air").isEmpty());
        assertTrue(DungeonRoomDataPolicy.placeSecrets(room, null).isEmpty());
    }

    @Test
    void fairyForcesSouthZeroDegreeCorner() {
        DungeonRoomDataPolicy.RoomMeta fairy = DungeonRoomDataPolicy.catalog().rooms().stream()
                .filter(room -> "Fairy".equals(room.name()))
                .findFirst()
                .orElseThrow();
        DungeonRoomDataPolicy.Rotation expected =
                DungeonRoomDataPolicy.fairyRotation(-185, -185);
        assertEquals(0, expected.degrees());
        assertEquals(-200, expected.cornerX());
        assertEquals(-200, expected.cornerZ());
        Optional<DungeonRoomDataPolicy.Rotation> found = DungeonRoomDataPolicy.resolveRotation(
                fairy, -185, -185, 70, List.of(), (x, y, z) -> "minecraft:air");
        assertEquals(expected, found.orElseThrow());
    }

    @Test
    void oneByTwoSameXUsesWestClayAtHighestBlock() {
        DungeonRoomDataPolicy.RoomMeta room = new DungeonRoomDataPolicy.RoomMeta(
                "Long", "normal", 0, List.of(), 99, "1x2");
        List<DungeonRoomDataPolicy.MapTile> tiles = List.of(
                new DungeonRoomDataPolicy.MapTile(0, 0),
                new DungeonRoomDataPolicy.MapTile(0, 1));
        int highest = 77;
        int clayX = DungeonMapPolicy.roomWorldCenter(0) + DungeonRoomDataPolicy.ROOM_HALF;
        int clayZ = DungeonMapPolicy.roomWorldCenter(0) - DungeonRoomDataPolicy.ROOM_HALF;
        DungeonRoomDataPolicy.BlockAt world = (x, y, z) ->
                x == clayX && y == highest && z == clayZ
                        ? "minecraft:blue_terracotta"
                        : "minecraft:air";
        DungeonRoomDataPolicy.Rotation rotation = DungeonRoomDataPolicy.resolveRotation(
                room, -185, -185, highest, tiles, world).orElseThrow();
        assertEquals(90, rotation.degrees());
        assertEquals(clayX, rotation.cornerX());
        assertEquals(clayZ, rotation.cornerZ());
    }

    private static List<String> airColumn() {
        List<String> column = new ArrayList<>();
        for (int y = DungeonRoomDataPolicy.HASH_Y_TOP; y >= DungeonRoomDataPolicy.HASH_Y_BOTTOM; y--) {
            column.add("minecraft:air");
        }
        return column;
    }

    private static DungeonRoomDataPolicy.RoomMeta oneByOneRoom(String name) {
        return new DungeonRoomDataPolicy.RoomMeta(name, "normal", 0, List.of(), 1, "1x1");
    }
}
