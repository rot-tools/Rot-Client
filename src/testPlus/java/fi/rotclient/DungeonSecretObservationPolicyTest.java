package fi.rotclient;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static fi.rotclient.DungeonRoomDataPolicy.*;

final class DungeonSecretObservationPolicyTest {
    @Test void parsesServerRoomCountAmongFormattedHealthAndMana() {
        var count = DungeonSecretObservationPolicy.roomCount("§c1200/1200❤   §76/10 Secrets§r   §b200/300✎").orElseThrow();
        assertEquals(new DungeonSecretObservationPolicy.RoomCount(6,10), count);
        assertTrue(DungeonSecretObservationPolicy.belongsToRoom(count,
                new DungeonMapPolicy.RoomIdentity("Lava",10,DungeonMapPolicy.RoomType.NORMAL)));
        assertFalse(DungeonSecretObservationPolicy.belongsToRoom(count,
                new DungeonMapPolicy.RoomIdentity("Other",8,DungeonMapPolicy.RoomType.NORMAL)));
    }

    @Test void malformedAndPartyWideCountersCannotBecomeRoomCounts() {
        for (String text : List.of("Secrets Found: 30/100", "200/300❤", "11/10 Secrets", "0/0 Secrets",
                "999/10 Secrets", "1/2 Secrets 2/2 Secrets", "x".repeat(5000))) {
            assertTrue(DungeonSecretObservationPolicy.roomCount(text).isEmpty(),text);
        }
        assertTrue(DungeonSecretObservationPolicy.roomCount(null).isEmpty());
    }

    @Test void pickupMatchesOnlyTheRightKindAndRoomWithinRadius() {
        var item = new PlacedWaypoint(SecretKind.ITEM,-200,70,-200,"Lava");
        var bat = new PlacedWaypoint(SecretKind.BAT,-200,70,-200,"Lava");
        var other = new PlacedWaypoint(SecretKind.ITEM,-200,70,-200,"Other");
        var points = List.of(other,bat,item);
        assertEquals(item, DungeonSecretObservationPolicy.nearest(points,Set.of(),"Lava",SecretKind.ITEM,
                -199.5,70,-200,3).orElseThrow());
        assertTrue(DungeonSecretObservationPolicy.nearest(points,Set.of(),"Lava",SecretKind.ITEM,-190,70,-200,3).isEmpty());
        assertTrue(DungeonSecretObservationPolicy.nearest(points,Set.of(secretKey(-200,70,-200)),"Lava",SecretKind.ITEM,
                -200,70,-200,3).isEmpty());
        assertTrue(DungeonSecretObservationPolicy.nearest(points,Set.of(),"Lava",SecretKind.ITEM,Double.NaN,70,-200,3).isEmpty());
    }

    @Test void arbitraryMobLootDoesNotHideAnItemSecret() {
        assertTrue(DungeonSecretObservationPolicy.isSecretItem("§5Revive Stone"));
        assertTrue(DungeonSecretObservationPolicy.isSecretItem("Spirit Leap"));
        assertFalse(DungeonSecretObservationPolicy.isSecretItem("Rotten Flesh"));
        assertFalse(DungeonSecretObservationPolicy.isSecretItem("Fake Spirit Leap"));
    }
}
