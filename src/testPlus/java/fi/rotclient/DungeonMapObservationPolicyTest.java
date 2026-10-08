package fi.rotclient;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonMapObservationPolicyTest {
    @Test void packetCentreAndNegativeCoordinatesLandOnTheirActualPixels() {
        assertEquals(64,DungeonMapObservationPolicy.pixel(0));
        assertEquals(32,DungeonMapObservationPolicy.pixel(-64));
        assertEquals(96,DungeonMapObservationPolicy.pixel(64));
        assertEquals(0,DungeonMapObservationPolicy.pixel(-128));
        assertEquals(127,DungeonMapObservationPolicy.pixel(127));
        assertEquals(63,DungeonMapObservationPolicy.pixel(-1));
        assertEquals(-1,DungeonMapObservationPolicy.pixel(128));
    }
    @Test void deadTeammatesCannotShiftUnnamedLiveMapMarkers() {
        assertEquals(List.of("Alive","Other"),DungeonMapObservationPolicy.livingRoster(
                List.of("[30] DeadPlayer (Mage 30) (DEAD)","OtherDeadPlayer (DEAD)"),
                List.of("Self","DeadPlayer","Alive","Other","Alive"),"Self"));
    }
    @Test void incompleteMarkerPacketsCannotGiveTheWrongPlayerAHead() {
        var anonymous = new DungeonMapPolicy.MapDecorationHint(20, 30, 0, false, "");
        var dead = new DungeonMapPolicy.MapDecorationHint(40, 50, 0, false, "Dead");
        assertEquals(List.of(anonymous), DungeonMapObservationPolicy.knownMarkers(
                List.of(anonymous, dead), List.of("Alive", "Other")));
        assertEquals(List.of(), DungeonMapObservationPolicy.unambiguousOrder(
                List.of(anonymous), List.of("Alive", "Other")));
        assertEquals(List.of("Alive"), DungeonMapObservationPolicy.unambiguousOrder(
                List.of(anonymous), List.of("Alive")));
        var named = new DungeonMapPolicy.MapDecorationHint(40, 50, 0, false, "Alive");
        assertEquals(List.of("Other"), DungeonMapObservationPolicy.unambiguousOrder(
                List.of(anonymous, named), List.of("Alive", "Other")));
        var icons = DungeonMapPolicy.assignTeammateIcons(List.of(anonymous, named),
                DungeonMapObservationPolicy.unambiguousOrder(List.of(anonymous, named), List.of("Alive", "Other")),
                "Self", java.util.Map.of(), false, true);
        assertEquals(List.of("Other", "Alive"), icons.stream().map(DungeonMapPolicy.PlayerIcon::name).toList());
    }
    @Test void aPartialOrOversizedMapCannotReuseTheOldBoard() {
        assertTrue(DungeonMapObservationPolicy.buffer(new byte[16384]));
        assertFalse(DungeonMapObservationPolicy.buffer(new byte[16383]));
        assertFalse(DungeonMapObservationPolicy.buffer(new byte[16385]));
        assertFalse(DungeonMapObservationPolicy.buffer(null));
    }
}
