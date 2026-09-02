package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        assertTrue(DungeonRoomDataPolicy.isBlueTerracotta("minecraft:blue_terracotta"));
        assertEquals(4, DungeonRoomDataPolicy.candidateRotations(40, 8).size());
    }

    @Test
    void placeSecretsUsesRoomIdAndRotation() {
        DungeonRoomDataPolicy.RoomMeta waterfall =
                DungeonRoomDataPolicy.roomForCore(740310812).orElseThrow();
        List<DungeonRoomDataPolicy.PlacedWaypoint> placed = DungeonRoomDataPolicy.placeSecrets(
                waterfall, new DungeonRoomDataPolicy.Rotation(0, 0, 0));
        assertFalse(placed.isEmpty());
        assertEquals(0xFF22C55E, DungeonRoomDataPolicy.secretColor(
                DungeonRoomDataPolicy.SecretKind.CHEST));
    }
}
