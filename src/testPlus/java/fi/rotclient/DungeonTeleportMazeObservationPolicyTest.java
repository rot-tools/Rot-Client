package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.rotclient.DungeonPuzzlePolicy.WorldCell;
import fi.rotclient.DungeonTeleportMazeObservationPolicy.PlayerPosition;
import fi.rotclient.DungeonTeleportMazeObservationPolicy.TeleportPacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonTeleportMazeObservationPolicyTest {
    @Test
    void absoluteMazeTeleportMarksPadsAndRanksFromServerYaw() {
        var policy = entered();
        WorldCell from = pad(4, 6);
        WorldCell to = pad(4, 14);
        WorldCell expected = pad(10, 12);
        assertTrue(policy.candidates().isEmpty());
        assertTrue(policy.recommendedPad().isEmpty());

        assertTrue(policy.observeTeleport(at(from), packet(to, toward(to, expected))));

        assertEquals(List.of(from, to), policy.visitedPads());
        assertEquals(expected, policy.recommendedPad().orElseThrow());
        assertEquals(26, policy.candidates().size());
        assertTrue(policy.candidates().stream().noneMatch(c -> c.pad().equals(from) || c.pad().equals(to)
                || c.pad().equals(pad(15, 12)) || c.pad().equals(pad(15, 14))));
        assertEquals(List.of(new DungeonTeleportMazeObservationPolicy.TeleportLink(from, to)), policy.observedLinks());
    }

    @Test
    void equalDirectionalScoresStayAmbiguous() {
        var policy = entered();
        assertTrue(policy.observeTeleport(at(pad(4, 6)), packet(pad(4, 14), -90)));
        assertEquals(0.0, policy.candidates().get(0).totalAngle(), 1.0E-5);
        assertEquals(0.0, policy.candidates().get(1).totalAngle(), 1.0E-5);
        assertTrue(policy.recommendedPad().isEmpty());
    }

    @Test
    void laterTeleportAddsYawEvidenceAndRemovesItsVisitedDestination() {
        var policy = entered();
        WorldCell destination = pad(4, 14);
        WorldCell candidate = pad(10, 20);
        assertTrue(policy.observeTeleport(at(pad(4, 6)), packet(destination, 0)));
        assertEquals(45.0, score(policy, candidate), 1.0E-5);
        assertTrue(policy.observeTeleport(at(destination), packet(pad(20, 22), 90)));
        assertEquals(45.0 + Math.toDegrees(Math.atan2(2, 10)), score(policy, candidate), 1.0E-5);
        assertFalse(policy.candidates().stream().anyMatch(c -> c.pad().equals(pad(20, 22))));
    }

    @Test
    void duplicatePacketsDoNotMultiplyEvidence() {
        var policy = entered();
        var before = at(pad(4, 6));
        var packet = packet(pad(4, 14), 0);
        assertTrue(policy.observeTeleport(before, packet));
        var candidates = policy.candidates();
        assertFalse(policy.observeTeleport(before, packet));
        assertEquals(candidates, policy.candidates());
        assertEquals(1, policy.observedLinks().size());
    }

    @Test
    void walkingCorrectionsAndTypicalEtherwarpTargetsDoNotCreateLinks() {
        var policy = entered();
        WorldCell from = pad(4, 6);
        // A server correction within the same cell is not a pad activation.
        assertFalse(policy.observeTeleport(at(from), packet(pad(10, 12), 0)));
        // Ability movement lands on the surface, rather than the maze's Y=69.5.
        assertFalse(policy.observeTeleport(at(from), new TeleportPacket(4.5, 69.8125, 14.5, 0, false, false)));
        // Rotation preserved relatively carries no authoritative maze-facing yaw.
        assertFalse(policy.observeTeleport(at(from), new TeleportPacket(4.5, 69.5, 14.5, 0, false, true)));
        assertTrue(policy.observedLinks().isEmpty());
        assertTrue(policy.visitedPads().isEmpty());
        assertTrue(policy.candidates().isEmpty());
    }

    @Test
    void malformedRelativeAndForeignRoomPacketsLeaveEvidenceUntouched() {
        var policy = entered();
        var from = at(pad(4, 6));
        for (TeleportPacket packet : List.of(
                new TeleportPacket(Double.NaN, 69.5, 14.5, 0, false, false),
                new TeleportPacket(4.5, Double.POSITIVE_INFINITY, 14.5, 0, false, false),
                new TeleportPacket(4.5, 69.5, 14.5, Float.NaN, false, false),
                new TeleportPacket(4.25, 69.5, 14.5, 0, false, false),
                new TeleportPacket(4.5, 70.5, 14.5, 0, false, false),
                new TeleportPacket(4.5, 69.5, 14.5, 0, true, false),
                new TeleportPacket(104.5, 69.5, 114.5, 0, false, false))) {
            assertFalse(policy.observeTeleport(from, packet));
        }
        assertFalse(policy.observeTeleport(new PlayerPosition(104.5, 69.5, 106.5), packet(pad(4, 14), 0)));
        assertFalse(policy.observeTeleport(new PlayerPosition(4.5, Double.NaN, 6.5), packet(pad(4, 14), 0)));
        assertFalse(policy.observeTeleport(null, packet(pad(4, 14), 0)));
        assertFalse(policy.observeTeleport(from, null));
        assertTrue(policy.observedLinks().isEmpty());
        assertTrue(policy.candidates().isEmpty());
    }

    @Test
    void sameRoomRefreshRetainsObservationsButRoomFloorAndLeavingReset() {
        var policy = entered();
        var from = at(pad(4, 6));
        var packet = packet(pad(4, 14), 0);
        assertTrue(policy.observeTeleport(from, packet));
        var reordered = new ArrayList<>(layout(0));
        Collections.reverse(reordered);
        policy.enterRoom("run1/F7", "maze1", reordered);
        assertEquals(1, policy.observedLinks().size());

        policy.enterRoom("run1/F7", "maze2", layout(0));
        assertTrue(policy.observedLinks().isEmpty());
        assertTrue(policy.visitedPads().isEmpty());
        assertTrue(policy.candidates().isEmpty());
        assertTrue(policy.observeTeleport(from, packet));
        policy.enterRoom("run2/F6", "maze2", layout(0));
        assertTrue(policy.observedLinks().isEmpty());
        assertTrue(policy.candidates().isEmpty());
        assertTrue(policy.observeTeleport(from, packet));
        policy.enterRoom("run2/F6", "", List.of());
        assertTrue(policy.pads().isEmpty());
        assertTrue(policy.visitedPads().isEmpty());
        assertFalse(policy.observeTeleport(from, packet));
    }

    @Test
    void incompleteUnloadedOrDuplicateLayoutsCannotActivateSolver() {
        var policy = entered();
        var incomplete = layout(0).subList(0, 29);
        policy.enterRoom("run1/F7", "maze1", incomplete);
        assertTrue(policy.pads().isEmpty());
        assertFalse(policy.observeTeleport(at(pad(4, 6)), packet(pad(4, 14), 0)));
        var duplicate = new ArrayList<>(layout(0));
        duplicate.set(1, duplicate.get(0));
        policy.enterRoom("run1/F7", "maze1", duplicate);
        assertTrue(policy.pads().isEmpty());
        duplicate.set(1, null);
        policy.enterRoom("run1/F7", "maze1", duplicate);
        assertTrue(policy.pads().isEmpty());
        policy.enterRoom(null, "maze1", layout(0));
        assertTrue(policy.pads().isEmpty());
    }

    @Test
    void candidatesNeverInventRoutesOrReverseLinks() {
        var policy = entered();
        WorldCell start = pad(4, 6);
        WorldCell via = pad(4, 14);
        WorldCell goal = pad(20, 22);
        assertTrue(policy.observedPath(start, goal).isEmpty());
        assertTrue(policy.observeTeleport(at(start), packet(via, toward(via, pad(10, 12)))));
        assertTrue(policy.recommendedPad().isPresent());
        assertEquals(List.of(start, via), policy.observedPath(start, via));
        assertTrue(policy.observedPath(start, goal).isEmpty());
        assertTrue(policy.observedPath(via, pad(10, 20)).isEmpty());
        assertTrue(policy.observedPath(via, start).isEmpty());
        assertTrue(policy.observeTeleport(at(via), packet(goal, 90)));
        assertEquals(List.of(start, via, goal), policy.observedPath(start, goal));
        assertTrue(policy.observedPath(goal, start).isEmpty());
        assertTrue(policy.observedPath(start, pad(999, 999)).isEmpty());
        assertTrue(policy.observedPath(null, goal).isEmpty());
        assertTrue(policy.observedPath(start, null).isEmpty());
    }

    @Test
    void arrivalAtEndpointClearsHintWithoutCallingItWrong() {
        var policy = entered();
        WorldCell endpoint = pad(15, 12);
        assertTrue(policy.observeTeleport(at(pad(4, 6)), packet(pad(4, 14), 0)));
        assertFalse(policy.candidates().isEmpty());
        assertTrue(policy.observeTeleport(at(pad(4, 14)), packet(endpoint, 0)));
        assertTrue(policy.candidates().isEmpty());
        assertTrue(policy.recommendedPad().isEmpty());
        assertFalse(policy.visitedPads().contains(endpoint));
        assertEquals(List.of(pad(4, 6), pad(4, 14), endpoint), policy.observedPath(pad(4, 6), endpoint));
    }

    @Test
    void negativeWorldCoordinatesAndAllRoomRotationsKeepCandidateGeometry() {
        for (int rotation : List.of(0, 90, 180, 270)) {
            var policy = new DungeonTeleportMazeObservationPolicy();
            var transform = new DungeonRoomDataPolicy.Rotation(rotation, -200, -200);
            List<WorldCell> pads = layout(transform);
            policy.enterRoom("run1/F7", "maze/" + rotation, pads);
            WorldCell from = world(4, 6, transform);
            WorldCell to = world(4, 14, transform);
            WorldCell expected = world(10, 12, transform);
            assertTrue(policy.observeTeleport(at(from), packet(to, toward(to, expected))));
            assertEquals(expected, policy.recommendedPad().orElseThrow());
            assertEquals(List.of(from, to), policy.visitedPads());
        }
    }

    private static DungeonTeleportMazeObservationPolicy entered() {
        var policy = new DungeonTeleportMazeObservationPolicy();
        policy.enterRoom("run1/F7", "maze1", layout(0));
        return policy;
    }

    private static List<WorldCell> layout(int rotation) {
        return layout(new DungeonRoomDataPolicy.Rotation(rotation, 0, 0));
    }

    private static List<WorldCell> layout(DungeonRoomDataPolicy.Rotation rotation) {
        return DungeonPuzzleBoardPolicy.teleportMazePads().stream().map(rel -> {
            var world = DungeonPuzzleBoardPolicy.world(rel, rotation);
            return new WorldCell(world.x(), world.y(), world.z());
        }).toList();
    }

    private static WorldCell world(int x, int z, DungeonRoomDataPolicy.Rotation rotation) {
        var world = DungeonPuzzleBoardPolicy.world(new DungeonPuzzleBoardPolicy.RelPos(x, 69, z), rotation);
        return new WorldCell(world.x(), world.y(), world.z());
    }

    private static WorldCell pad(int x, int z) { return new WorldCell(x, 69, z); }
    private static PlayerPosition at(WorldCell pad) { return new PlayerPosition(pad.x() + 0.5, 69.5, pad.z() + 0.5); }
    private static TeleportPacket packet(WorldCell pad, float yaw) {
        return new TeleportPacket(pad.x() + 0.5, 69.5, pad.z() + 0.5, yaw, false, false);
    }
    private static float toward(WorldCell from, WorldCell to) {
        return (float) Math.toDegrees(Math.atan2(from.x() - to.x(), to.z() - from.z()));
    }
    private static double score(DungeonTeleportMazeObservationPolicy policy, WorldCell pad) {
        return policy.candidates().stream().filter(c -> c.pad().equals(pad)).findFirst().orElseThrow().totalAngle();
    }
}
