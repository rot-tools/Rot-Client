package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

/** Bounded, fair block-volume traversal; walking cannot restart an unfinished scan. */
public final class DungeonBlockScanPolicy {
    public static final int TICK_BUDGET = 384;

    public record Region(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        public Region {
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0
                    || sizeX > 65_536 || sizeY > 65_536 || sizeZ > 65_536
                    || (long) sizeX * sizeY * sizeZ > 65_536
                    || (long) x + sizeX > Integer.MAX_VALUE
                    || (long) y + sizeY > Integer.MAX_VALUE
                    || (long) z + sizeZ > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid scan region");
            }
        }

        int volume() { return sizeX * sizeY * sizeZ; }

        public boolean contains(int px, int py, int pz) {
            return px >= x && (long) px < (long) x + sizeX
                    && py >= y && (long) py < (long) y + sizeY
                    && pz >= z && (long) pz < (long) z + sizeZ;
        }

        boolean overlaps(Region other) {
            return (long) x < (long) other.x + other.sizeX && (long) other.x < (long) x + sizeX
                    && (long) y < (long) other.y + other.sizeY && (long) other.y < (long) y + sizeY
                    && (long) z < (long) other.z + other.sizeZ && (long) other.z < (long) z + sizeZ;
        }
    }

    public record Slice(Region region, List<DungeonPuzzlePolicy.WorldCell> cells, boolean started, boolean complete) {
        public Slice { cells = List.copyOf(cells); }
    }

    private Region active;
    private int cursor;

    public Slice next(Region requested) {
        boolean started = active == null || cursor == active.volume() || !active.overlaps(requested);
        if (started) {
            active = requested;
            cursor = 0;
        }
        List<DungeonPuzzlePolicy.WorldCell> cells = new ArrayList<>();
        int limit = Math.min(active.volume(), cursor + TICK_BUDGET);
        while (cursor < limit) {
            int index = cursor++;
            int dz = index % active.sizeZ;
            int dy = index / active.sizeZ % active.sizeY;
            int dx = index / (active.sizeZ * active.sizeY);
            cells.add(new DungeonPuzzlePolicy.WorldCell(active.x + dx, active.y + dy, active.z + dz));
        }
        return new Slice(active, cells, started, cursor == active.volume());
    }

    public void clear() { active = null; cursor = 0; }
}
