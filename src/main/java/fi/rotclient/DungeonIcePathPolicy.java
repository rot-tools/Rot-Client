package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ice Path sliding BFS on the hashed-room 19×19 ice grid. Relative cells
 * sit at clay-corner (7+gx, 67, 7+gz), matching the same fromComp rotation
 * as other puzzle boards. Goal is the south-edge hole at grid (7, 18).
 */
public final class DungeonIcePathPolicy {
    public static final int GRID = 19;
    public static final int REL_ORIGIN = 7;
    public static final int ICE_Y = 67;
    public static final int GOAL_X = 7;
    public static final int GOAL_Z = 18;

    public record GridPos(int x, int z) {
    }

    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private DungeonIcePathPolicy() {
    }

    public static DungeonPuzzleBoardPolicy.RelPos relPos(int gridX, int gridZ) {
        return new DungeonPuzzleBoardPolicy.RelPos(REL_ORIGIN + gridX, ICE_Y, REL_ORIGIN + gridZ);
    }

    public static Optional<GridPos> gridFromRel(int relX, int relZ) {
        int gx = relX - REL_ORIGIN;
        int gz = relZ - REL_ORIGIN;
        if (!inBounds(gx, gz)) {
            return Optional.empty();
        }
        return Optional.of(new GridPos(gx, gz));
    }

    public static boolean inBounds(int x, int z) {
        return x >= 0 && x < GRID && z >= 0 && z < GRID;
    }

    /**
     * Shortest slide path from the silverfish cell to the goal hole.
     * {@code blocked[z][x]} is true for solid tiles the fish cannot occupy.
     */
    public static List<GridPos> solve(boolean[][] blocked, int startX, int startZ) {
        if (blocked == null || blocked.length != GRID) {
            return List.of();
        }
        for (boolean[] row : blocked) {
            if (row == null || row.length != GRID) {
                return List.of();
            }
        }
        if (!inBounds(startX, startZ) || blocked[startZ][startX]) {
            return List.of();
        }
        if (startX == GOAL_X && startZ == GOAL_Z) {
            return List.of(new GridPos(startX, startZ));
        }
        ArrayDeque<GridPos> queue = new ArrayDeque<>();
        GridPos[][] parent = new GridPos[GRID][GRID];
        GridPos start = new GridPos(startX, startZ);
        queue.add(start);
        parent[startZ][startX] = start;
        while (!queue.isEmpty()) {
            GridPos curr = queue.removeFirst();
            for (int dir = 0; dir < 4; dir++) {
                GridPos next = slide(blocked, parent, curr, DX[dir], DZ[dir]);
                if (next == null) {
                    continue;
                }
                parent[next.z()][next.x()] = curr;
                queue.addLast(next);
                if (next.x() == GOAL_X && next.z() == GOAL_Z) {
                    return reconstruct(parent, start, next);
                }
            }
        }
        return List.of();
    }

    public static List<GridPos> expandPath(List<GridPos> stops) {
        if (stops == null || stops.isEmpty()) {
            return List.of();
        }
        List<GridPos> out = new ArrayList<>();
        out.add(stops.getFirst());
        for (int i = 1; i < stops.size(); i++) {
            GridPos from = stops.get(i - 1);
            GridPos to = stops.get(i);
            int dx = Integer.signum(to.x() - from.x());
            int dz = Integer.signum(to.z() - from.z());
            int x = from.x();
            int z = from.z();
            while (x != to.x() || z != to.z()) {
                x += dx;
                z += dz;
                out.add(new GridPos(x, z));
            }
        }
        return List.copyOf(out);
    }

    private static GridPos slide(
            boolean[][] blocked,
            GridPos[][] parent,
            GridPos from,
            int dx,
            int dz) {
        int steps = 1;
        while (true) {
            int nx = from.x() + steps * dx;
            int nz = from.z() + steps * dz;
            if (!inBounds(nx, nz) || blocked[nz][nx]) {
                steps--;
                if (steps == 0) {
                    return null;
                }
                nx = from.x() + steps * dx;
                nz = from.z() + steps * dz;
                if (parent[nz][nx] != null) {
                    return null;
                }
                return new GridPos(nx, nz);
            }
            steps++;
        }
    }

    private static List<GridPos> reconstruct(GridPos[][] parent, GridPos start, GridPos goal) {
        List<GridPos> path = new ArrayList<>();
        GridPos tmp = goal;
        while (tmp != null && (tmp.x() != start.x() || tmp.z() != start.z())) {
            path.addFirst(tmp);
            tmp = parent[tmp.z()][tmp.x()];
        }
        path.addFirst(start);
        return List.copyOf(path);
    }
}
