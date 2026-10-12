package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import fi.rotclient.DungeonPuzzlePolicy.WorldCell;

/**
 * Teleport Maze cell grouping, visited pads and accumulated yaw-angle hints
 * adapted from NoammAddons TeleportMazeSolver.kt, CC0-1.0, revision
 * 4164abc38500e041a805e03ca0f01b46e37bd8a5. Full license is retained in
 * docs/third-party/NoammAddons-LICENSE.txt. The geometry is the existing
 * DungeonPuzzleBoardPolicy teleportMazePads layout (Odin provenance).
 * Rot requires a complete verified room layout and pre-application server
 * position packets. Candidate hints never create teleport links or routes.
 */
public final class DungeonTeleportMazeObservationPolicy {
    public record PlayerPosition(double x, double y, double z) { }

    /** Raw packet values; relative positions or rotations are not maze evidence. */
    public record TeleportPacket(double x, double y, double z, float yaw,
                                 boolean relativePosition, boolean relativeRotation) { }

    public record Candidate(WorldCell pad, double totalAngle) { }
    public record TeleportLink(WorldCell from, WorldCell to) { }

    private record Cell(int x, int z) { }
    private static final double EPSILON = 1.0E-6;
    private static final Comparator<WorldCell> PAD_ORDER = Comparator
            .comparingInt(WorldCell::x).thenComparingInt(WorldCell::z);

    private String runKey = "";
    private String roomKey = "";
    private List<WorldCell> pads = List.of();
    private Map<WorldCell, Cell> cells = Map.of();
    private Set<WorldCell> endpoints = Set.of();
    private final Set<WorldCell> visited = new LinkedHashSet<>();
    private final Set<TeleportLink> links = new LinkedHashSet<>();
    private final Map<WorldCell, Double> angles = new HashMap<>();
    private int minX;
    private int minZ;
    private boolean hasYawEvidence;

    /**
     * Call when the current identified room or run/floor changes. The caller
     * verifies all thirty expected positions are loaded END_PORTAL_FRAMEs.
     * Repeating the same room/layout keeps observations; an absent room resets.
     */
    public void enterRoom(String runKey, String roomKey, List<WorldCell> verifiedPads) {
        List<WorldCell> layout = layout(verifiedPads);
        if (runKey == null || runKey.isBlank() || roomKey == null || roomKey.isBlank() || layout.isEmpty()) {
            reset();
            return;
        }
        if (this.runKey.equals(runKey) && this.roomKey.equals(roomKey) && pads.equals(layout)) return;
        reset();
        this.runKey = runKey;
        this.roomKey = roomKey;
        pads = layout;
        minX = pads.stream().mapToInt(WorldCell::x).min().orElseThrow();
        minZ = pads.stream().mapToInt(WorldCell::z).min().orElseThrow();
        Map<WorldCell, Cell> grouped = new HashMap<>();
        Map<Cell, Integer> counts = new HashMap<>();
        for (WorldCell pad : pads) {
            Cell cell = new Cell((pad.x() - minX) / 8, (pad.z() - minZ) / 8);
            grouped.put(pad, cell);
            counts.merge(cell, 1, Integer::sum);
            angles.put(pad, 0.0);
        }
        cells = Map.copyOf(grouped);
        Set<WorldCell> terminal = new LinkedHashSet<>();
        for (WorldCell pad : pads) {
            Cell cell = cells.get(pad);
            if (cell.equals(new Cell(1, 1)) || counts.get(cell) == 1) terminal.add(pad);
        }
        endpoints = Set.copyOf(terminal);
    }

    public void reset() {
        runKey = "";
        roomKey = "";
        pads = List.of();
        cells = Map.of();
        endpoints = Set.of();
        visited.clear();
        links.clear();
        angles.clear();
        hasYawEvidence = false;
        minX = minZ = 0;
    }

    /**
     * Call only for ClientboundPlayerPositionPacket, before applying it to the
     * player. Do not feed tick movement or a known player teleport ability.
     * The maze sends absolute half-block coordinates at Y=69.5 plus its yaw.
     */
    public boolean observeTeleport(PlayerPosition before, TeleportPacket packet) {
        if (pads.isEmpty() || before == null || packet == null
                || packet.relativePosition() || packet.relativeRotation()
                || !finite(before.x(), before.y(), before.z())
                || !finite(packet.x(), packet.y(), packet.z()) || !Float.isFinite(packet.yaw())
                || Math.abs(packet.y() - 69.5) > EPSILON
                || Math.abs(before.y() - 69.5) > 2.0
                || !halfBlock(packet.x()) || !halfBlock(packet.z())) return false;
        WorldCell from = padNear(before.x(), before.z());
        WorldCell to = padNear(packet.x(), packet.z());
        if (from == null || to == null || cells.get(from).equals(cells.get(to))) return false;
        TeleportLink link = new TeleportLink(from, to);
        // Retransmitting the same absolute teleport while awaiting acknowledgment
        // must not multiply the angle evidence from one pad activation.
        if (!links.add(link)) return false;
        if (!endpoints.contains(from)) visited.add(from);
        if (!endpoints.contains(to)) visited.add(to);
        if (endpoints.contains(to)) {
            hasYawEvidence = false;
            return true;
        }
        for (WorldCell pad : pads) {
            if (endpoints.contains(pad) || visited.contains(pad)) continue;
            angles.merge(pad, angle(pad.x() + 0.5 - packet.x(), pad.z() + 0.5 - packet.z(), packet.yaw()), Double::sum);
        }
        hasYawEvidence = true;
        return true;
    }

    public List<WorldCell> pads() { return pads; }
    public List<WorldCell> visitedPads() { return List.copyOf(visited); }
    public List<TeleportLink> observedLinks() { return List.copyOf(links); }

    /** Ranked directional candidates, not a guaranteed path through the maze. */
    public List<Candidate> candidates() {
        if (!hasYawEvidence) return List.of();
        return pads.stream().filter(p -> !endpoints.contains(p) && !visited.contains(p))
                .map(p -> new Candidate(p, angles.get(p)))
                .sorted(Comparator.comparingDouble(Candidate::totalAngle).thenComparing(Candidate::pad, PAD_ORDER))
                .toList();
    }

    /** Match Noamm's tie guard: ambiguous equally ranked pads have no best hint. */
    public Optional<WorldCell> recommendedPad() {
        List<Candidate> candidates = candidates();
        if (candidates.isEmpty() || candidates.size() > 1
                && Math.abs(candidates.get(0).totalAngle() - candidates.get(1).totalAngle()) <= EPSILON) return Optional.empty();
        return Optional.of(candidates.getFirst().pad());
    }

    /** Directed routes contain only server-observed links, without geometric fallbacks. */
    public List<WorldCell> observedPath(WorldCell start, WorldCell goal) {
        if (start == null || goal == null || !pads.contains(start) || !pads.contains(goal)) return List.of();
        ArrayDeque<WorldCell> queue = new ArrayDeque<>();
        Map<WorldCell, WorldCell> parent = new HashMap<>();
        queue.add(start);
        parent.put(start, start);
        while (!queue.isEmpty()) {
            WorldCell from = queue.removeFirst();
            if (from.equals(goal)) {
                List<WorldCell> path = new ArrayList<>();
                for (WorldCell cursor = goal; !cursor.equals(start); cursor = parent.get(cursor)) path.add(cursor);
                path.add(start);
                Collections.reverse(path);
                return List.copyOf(path);
            }
            for (TeleportLink link : links) {
                if (link.from().equals(from) && !parent.containsKey(link.to())) {
                    parent.put(link.to(), from);
                    queue.addLast(link.to());
                }
            }
        }
        return List.of();
    }

    private WorldCell padNear(double x, double z) {
        if (x < minX || x >= (long) minX + 24 || z < minZ || z >= (long) minZ + 24) return null;
        Cell cell = new Cell((int) Math.floor((x - minX) / 8), (int) Math.floor((z - minZ) / 8));
        WorldCell best = null;
        double bestDistance = 3.0 + EPSILON;
        boolean tie = false;
        for (WorldCell pad : pads) {
            if (!cells.get(pad).equals(cell)) continue;
            double distance = Math.abs(x - pad.x() - 0.5) + Math.abs(z - pad.z() - 0.5);
            if (best == null && distance <= 3.0 + EPSILON || distance < bestDistance - EPSILON) {
                best = pad;
                bestDistance = distance;
                tie = false;
            } else if (best != null && Math.abs(distance - bestDistance) <= EPSILON) tie = true;
        }
        return tie ? null : best;
    }

    private static List<WorldCell> layout(List<WorldCell> pads) {
        if (pads == null || pads.size() != DungeonPuzzleBoardPolicy.teleportMazePads().size()
                || pads.stream().anyMatch(p -> p == null || p.y() != 69)) return List.of();
        if (new LinkedHashSet<>(pads).size() != pads.size()) return List.of();
        int minX = pads.stream().mapToInt(WorldCell::x).min().orElseThrow();
        int maxX = pads.stream().mapToInt(WorldCell::x).max().orElseThrow();
        int minZ = pads.stream().mapToInt(WorldCell::z).min().orElseThrow();
        int maxZ = pads.stream().mapToInt(WorldCell::z).max().orElseThrow();
        if ((long) maxX - minX > 23 || (long) maxZ - minZ > 23) return List.of();
        return pads.stream().sorted(PAD_ORDER).toList();
    }

    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    private static boolean halfBlock(double coordinate) {
        return Math.abs(coordinate * 2.0 - Math.rint(coordinate * 2.0)) <= EPSILON;
    }

    private static double angle(double dx, double dz, float yaw) {
        double yawRadians = Math.toRadians(yaw);
        double distance = Math.hypot(dx, dz);
        double dot = (-Math.sin(yawRadians) * dx + Math.cos(yawRadians) * dz) / distance;
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }
}
