package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catacombs puzzle solvers that work from scanned blocks and chat — Ice Fill,
 * Water Board, Boulder, TP Maze, Three Weirdos chests and the dungeon map
 * item — without a full room-core database.
 */
public final class DungeonPuzzlePolicy {
    public record Cell(int x, int z) {
    }

    public record WorldCell(int x, int y, int z) {
    }

    public record NamedPos(String name, int x, int y, int z) {
    }

    public record BoulderStep(int x, int z, boolean push) {
    }

    public record MapPreview(int width, int height, int[] argb, int playerX, int playerZ, String summary) {
        public MapPreview {
            argb = argb == null ? new int[0] : argb.clone();
            summary = summary == null ? "" : summary;
        }
    }

    public enum MapKind {
        EMPTY,
        ROOM,
        PUZZLE,
        BLOOD,
        WITHER,
        ENTRANCE,
        PLAYER
    }

    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};
    private static final int BOULDER_STATE_CAP = 20_000;
    private static final int ICE_CELL_CAP = 64;

    private DungeonPuzzlePolicy() {
    }

    public static List<Cell> iceFillPath(boolean[][] walkable, int startX, int startZ) {
        if (walkable == null || walkable.length == 0 || walkable[0].length == 0) {
            return List.of();
        }
        int height = walkable.length;
        int width = walkable[0].length;
        if (!inBounds(startX, startZ, width, height) || !walkable[startZ][startX]) {
            return List.of();
        }
        int total = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                if (walkable[z][x]) {
                    total++;
                }
            }
        }
        if (total == 0 || total > ICE_CELL_CAP) {
            return List.of();
        }
        boolean[][] seen = new boolean[height][width];
        List<Cell> path = new ArrayList<>();
        if (iceDfs(walkable, seen, startX, startZ, total, path)) {
            return List.copyOf(path);
        }
        return List.of();
    }

    public static List<Integer> waterLeversToToggle(char[][] grid, boolean[] currentlyOpen) {
        if (grid == null || currentlyOpen == null || currentlyOpen.length == 0) {
            return List.of();
        }
        int n = currentlyOpen.length;
        int currentMask = 0;
        for (int i = 0; i < n; i++) {
            if (currentlyOpen[i]) {
                currentMask |= 1 << i;
            }
        }
        int bestMask = -1;
        int bestToggles = Integer.MAX_VALUE;
        int limit = 1 << n;
        for (int mask = 0; mask < limit; mask++) {
            if (!waterReachesGoals(grid, mask, n)) {
                continue;
            }
            int toggles = Integer.bitCount(mask ^ currentMask);
            if (toggles < bestToggles) {
                bestToggles = toggles;
                bestMask = mask;
            }
        }
        if (bestMask < 0) {
            return List.of();
        }
        List<Integer> clicks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            boolean want = ((bestMask >> i) & 1) != 0;
            if (want != currentlyOpen[i]) {
                clicks.add(i);
            }
        }
        return List.copyOf(clicks);
    }

    public static boolean waterReachesGoals(char[][] grid, int openMask, int leverCount) {
        if (grid == null || grid.length == 0) {
            return false;
        }
        int height = grid.length;
        int width = grid[0].length;
        boolean[][] wet = new boolean[height][width];
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        int goals = 0;
        int wetGoals = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                char cell = grid[z][x];
                if (cell == 'C') {
                    goals++;
                }
                if (cell == 'S') {
                    wet[z][x] = true;
                    queue.add(new Cell(x, z));
                }
            }
        }
        if (goals == 0) {
            return false;
        }
        while (!queue.isEmpty()) {
            Cell cur = queue.removeFirst();
            if (grid[cur.z()][cur.x()] == 'C') {
                wetGoals++;
                continue;
            }
            for (int dir = 0; dir < 4; dir++) {
                int nx = cur.x() + DX[dir];
                int nz = cur.z() + DZ[dir];
                if (!inBounds(nx, nz, width, height) || wet[nz][nx]) {
                    continue;
                }
                if (!waterPassable(grid[nz][nx], openMask, leverCount)) {
                    continue;
                }
                wet[nz][nx] = true;
                queue.add(new Cell(nx, nz));
            }
        }
        return wetGoals >= goals;
    }

    public static List<BoulderStep> boulderPath(char[][] grid) {
        if (grid == null || grid.length == 0) {
            return List.of();
        }
        int height = grid.length;
        int width = grid[0].length;
        int player = -1;
        int chest = -1;
        long boulders = 0L;
        long walls = 0L;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int idx = z * width + x;
                char cell = grid[z][x];
                switch (cell) {
                    case 'P' -> player = idx;
                    case 'C' -> chest = idx;
                    case 'O' -> boulders |= 1L << idx;
                    case '#' -> walls |= 1L << idx;
                    default -> {
                    }
                }
            }
        }
        if (player < 0 || chest < 0 || width * height > 49) {
            return List.of();
        }
        record State(int player, long boulders) {
        }
        ArrayDeque<State> queue = new ArrayDeque<>();
        Map<State, State> prev = new HashMap<>();
        Map<State, BoulderStep> via = new HashMap<>();
        State start = new State(player, boulders);
        queue.add(start);
        prev.put(start, start);
        State goal = null;
        int visited = 0;
        while (!queue.isEmpty() && visited < BOULDER_STATE_CAP) {
            State cur = queue.removeFirst();
            visited++;
            if (adjacentIndex(cur.player(), chest, width, height)) {
                goal = cur;
                break;
            }
            int px = cur.player() % width;
            int pz = cur.player() / width;
            for (int dir = 0; dir < 4; dir++) {
                int nx = px + DX[dir];
                int nz = pz + DZ[dir];
                if (!inBounds(nx, nz, width, height)) {
                    continue;
                }
                int nIdx = nz * width + nx;
                if (((walls >> nIdx) & 1L) != 0 || nIdx == chest) {
                    continue;
                }
                long nextBoulders = cur.boulders();
                boolean push = false;
                if (((cur.boulders() >> nIdx) & 1L) != 0) {
                    int bx = nx + DX[dir];
                    int bz = nz + DZ[dir];
                    if (!inBounds(bx, bz, width, height)) {
                        continue;
                    }
                    int bIdx = bz * width + bx;
                    if (((walls >> bIdx) & 1L) != 0
                            || ((cur.boulders() >> bIdx) & 1L) != 0
                            || bIdx == chest) {
                        continue;
                    }
                    nextBoulders = (cur.boulders() & ~(1L << nIdx)) | (1L << bIdx);
                    push = true;
                }
                State next = new State(nIdx, nextBoulders);
                if (prev.containsKey(next)) {
                    continue;
                }
                prev.put(next, cur);
                via.put(next, new BoulderStep(nx, nz, push));
                queue.add(next);
            }
        }
        if (goal == null) {
            return List.of();
        }
        List<BoulderStep> steps = new ArrayList<>();
        State cursor = goal;
        while (!cursor.equals(start)) {
            BoulderStep step = via.get(cursor);
            if (step != null) {
                steps.add(step);
            }
            cursor = prev.get(cursor);
            if (cursor == null) {
                return List.of();
            }
        }
        java.util.Collections.reverse(steps);
        return List.copyOf(steps);
    }

    public static List<WorldCell> teleportPath(
            List<WorldCell> pads,
            Map<String, String> padToPad,
            WorldCell start,
            WorldCell goal) {
        if (pads == null || pads.isEmpty() || start == null) {
            return List.of();
        }
        WorldCell from = nearestPad(pads, start);
        WorldCell to = goal == null ? null : nearestPad(pads, goal);
        if (from == null) {
            return List.of();
        }
        Map<String, WorldCell> byKey = new HashMap<>();
        for (WorldCell pad : pads) {
            byKey.put(cellKey(pad), pad);
        }
        ArrayDeque<WorldCell> queue = new ArrayDeque<>();
        Map<String, String> prev = new HashMap<>();
        queue.add(from);
        prev.put(cellKey(from), cellKey(from));
        String goalKey = to == null ? "" : cellKey(to);
        WorldCell found = null;
        while (!queue.isEmpty()) {
            WorldCell cur = queue.removeFirst();
            if (!goalKey.isEmpty() && cellKey(cur).equals(goalKey)) {
                found = cur;
                break;
            }
            String destKey = padToPad == null ? null : padToPad.get(cellKey(cur));
            if (destKey != null && byKey.containsKey(destKey) && !prev.containsKey(destKey)) {
                prev.put(destKey, cellKey(cur));
                queue.add(byKey.get(destKey));
            }
            for (WorldCell pad : pads) {
                if (prev.containsKey(cellKey(pad))) {
                    continue;
                }
                if (Math.abs(pad.x() - cur.x()) + Math.abs(pad.z() - cur.z()) > 6
                        || Math.abs(pad.y() - cur.y()) > 2) {
                    continue;
                }
                prev.put(cellKey(pad), cellKey(cur));
                queue.add(pad);
            }
        }
        if (found == null) {
            return to == null ? List.of(from) : List.of(from, to);
        }
        List<WorldCell> path = new ArrayList<>();
        String cursor = cellKey(found);
        String startKey = cellKey(from);
        while (cursor != null) {
            WorldCell pad = byKey.get(cursor);
            if (pad != null) {
                path.add(pad);
            }
            if (cursor.equals(startKey)) {
                break;
            }
            cursor = prev.get(cursor);
        }
        java.util.Collections.reverse(path);
        return List.copyOf(path);
    }

    public static WorldCell nearestPad(List<WorldCell> pads, WorldCell point) {
        if (pads == null || point == null) {
            return null;
        }
        WorldCell best = null;
        int bestDist = Integer.MAX_VALUE;
        for (WorldCell pad : pads) {
            int dist = Math.abs(pad.x() - point.x())
                    + Math.abs(pad.y() - point.y())
                    + Math.abs(pad.z() - point.z());
            if (dist < bestDist) {
                bestDist = dist;
                best = pad;
            }
        }
        return best;
    }

    public static Optional<WorldCell> weirdoChest(List<NamedPos> npcs, List<WorldCell> chests, String truthName) {
        if (npcs == null || chests == null || truthName == null || truthName.isBlank()) {
            return Optional.empty();
        }
        String want = truthName.toLowerCase(Locale.ROOT);
        NamedPos npc = null;
        for (NamedPos candidate : npcs) {
            if (candidate.name() != null && candidate.name().toLowerCase(Locale.ROOT).contains(want)) {
                npc = candidate;
                break;
            }
        }
        if (npc == null) {
            return Optional.empty();
        }
        WorldCell best = null;
        int bestDist = Integer.MAX_VALUE;
        for (WorldCell chest : chests) {
            int dist = Math.abs(chest.x() - npc.x())
                    + Math.abs(chest.y() - npc.y())
                    + Math.abs(chest.z() - npc.z());
            if (dist < bestDist) {
                bestDist = dist;
                best = chest;
            }
        }
        return Optional.ofNullable(best);
    }

    public static String cellKey(WorldCell cell) {
        return cell == null ? "" : cell.x() + "," + cell.y() + "," + cell.z();
    }

    public static MapKind classifyDungeonMapColor(int colorByte) {
        int value = colorByte & 0xFF;
        if (value == 0) {
            return MapKind.EMPTY;
        }
        int id = value / 4;
        return switch (id) {
            case 4, 28 -> MapKind.BLOOD;
            case 1, 7, 19, 27, 33 -> MapKind.ENTRANCE;
            case 5, 17, 23 -> MapKind.PUZZLE;
            case 26, 34, 10 -> MapKind.WITHER;
            case 15, 16, 25, 30, 31 -> MapKind.PLAYER;
            default -> id == 29 ? MapKind.EMPTY : MapKind.ROOM;
        };
    }

    public static MapPreview previewDungeonMap(byte[] colors, int stride, int crop) {
        int width = stride <= 0 ? 128 : stride;
        if (colors == null || colors.length < width) {
            return new MapPreview(0, 0, new int[0], -1, -1, "");
        }
        int height = Math.max(1, colors.length / width);
        int playerX = -1;
        int playerZ = -1;
        int rooms = 0;
        int puzzles = 0;
        int blood = 0;
        int minX = width;
        int minZ = height;
        int maxX = 0;
        int maxZ = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int idx = z * width + x;
                if (idx >= colors.length) {
                    continue;
                }
                MapKind kind = classifyDungeonMapColor(colors[idx]);
                if (kind == MapKind.EMPTY) {
                    continue;
                }
                minX = Math.min(minX, x);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxZ = Math.max(maxZ, z);
                switch (kind) {
                    case PLAYER -> {
                        playerX = x;
                        playerZ = z;
                    }
                    case ROOM -> rooms++;
                    case PUZZLE -> puzzles++;
                    case BLOOD -> blood++;
                    default -> {
                    }
                }
            }
        }
        if (playerX < 0) {
            playerX = (minX + maxX) / 2;
            playerZ = (minZ + maxZ) / 2;
        }
        int size = crop <= 0 ? 21 : crop;
        int half = size / 2;
        int[] argb = new int[size * size];
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                int x = playerX - half + dx;
                int z = playerZ - half + dz;
                int color = 0xFF111111;
                if (x >= 0 && z >= 0 && x < width && z < height) {
                    int idx = z * width + x;
                    if (idx < colors.length) {
                        color = mapArgb(classifyDungeonMapColor(colors[idx]));
                    }
                }
                if (dx == half && dz == half) {
                    color = 0xFF22C55E;
                }
                argb[dz * size + dx] = color;
            }
        }
        String summary = "Map " + rooms + " rooms";
        if (puzzles > 0) {
            summary += " · " + puzzles + " puzzle";
        }
        if (blood > 0) {
            summary += " · blood";
        }
        return new MapPreview(size, size, argb, half, half, summary);
    }

    public static int mapArgb(MapKind kind) {
        return switch (kind) {
            case EMPTY -> 0xFF111111;
            case ROOM -> 0xFFD6C7A1;
            case PUZZLE -> 0xFF38BDF8;
            case BLOOD -> 0xFFDC2626;
            case WITHER -> 0xFF44403C;
            case ENTRANCE -> 0xFF22C55E;
            case PLAYER -> 0xFFFACC15;
        };
    }

    public static boolean isIceWalkable(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.equals("ice") || id.equals("frosted_ice");
    }

    public static boolean isPackedIce(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("packed_ice") || id.contains("blue_ice");
    }

    public static boolean isBoulderBlock(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.equals("cobblestone") || id.equals("stone") || id.equals("andesite");
    }

    public static boolean isBoulderWall(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("obsidian") || id.contains("bedrock") || id.contains("bricks");
    }

    public static boolean isPressurePad(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("pressure_plate");
    }

    public static boolean isChestBlock(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("chest") && !id.contains("ender");
    }

    public static boolean isSimonWall(int x, int y, int z) {
        return x >= 109 && x <= 111 && y >= 120 && y <= 123 && z >= 91 && z <= 95;
    }

    public static int leverColorIndex(String blockId, List<String> colors) {
        if (colors == null || colors.isEmpty()) {
            return -1;
        }
        String blob = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        for (int i = 0; i < colors.size(); i++) {
            String color = colors.get(i).toLowerCase(Locale.ROOT);
            if (!color.isBlank() && blob.contains(color)) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> defaultWaterColors() {
        return List.of("orange", "green", "red", "blue", "white", "purple", "yellow", "lime");
    }

    private static boolean iceDfs(
            boolean[][] walkable,
            boolean[][] seen,
            int x,
            int z,
            int remaining,
            List<Cell> path) {
        seen[z][x] = true;
        path.add(new Cell(x, z));
        if (remaining == 1) {
            return true;
        }
        int width = walkable[0].length;
        int height = walkable.length;
        for (int dir = 0; dir < 4; dir++) {
            int nx = x + DX[dir];
            int nz = z + DZ[dir];
            if (!inBounds(nx, nz, width, height) || seen[nz][nx] || !walkable[nz][nx]) {
                continue;
            }
            if (iceDfs(walkable, seen, nx, nz, remaining - 1, path)) {
                return true;
            }
        }
        seen[z][x] = false;
        path.remove(path.size() - 1);
        return false;
    }

    private static boolean waterPassable(char cell, int openMask, int leverCount) {
        if (cell == '.' || cell == 'S' || cell == 'C') {
            return true;
        }
        if (cell >= '0' && cell <= '9') {
            int idx = cell - '0';
            return idx < leverCount && ((openMask >> idx) & 1) != 0;
        }
        return false;
    }

    private static boolean inBounds(int x, int z, int width, int height) {
        return x >= 0 && z >= 0 && x < width && z < height;
    }

    private static boolean adjacentIndex(int from, int to, int width, int height) {
        int fx = from % width;
        int fz = from / width;
        int tx = to % width;
        int tz = to / width;
        return Math.abs(fx - tx) + Math.abs(fz - tz) == 1 && fz >= 0 && tz < height;
    }
}
