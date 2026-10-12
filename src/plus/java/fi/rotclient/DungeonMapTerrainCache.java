package fi.rotclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps one immutable terrain observation while players are refreshed separately.
 * The terrain/player update split is inspired by Odin MapScan.kt, revision
 * b0058000511ef755dfc6379330a1d23404e08df2 (BSD-3-Clause, Copyright (c) 2025,
 * odtheking). Full notice: docs/third-party/Odin-LICENSE.txt.
 * Terrain decoding remains in {@link DungeonMapPolicy}.
 */
public final class DungeonMapTerrainCache {
    private static final int MAP_SIDE = 128;
    private static final int MAP_PIXELS = MAP_SIDE * MAP_SIDE;
    private static final int MAX_CANDIDATES = 4;
    private static final DungeonMapPolicy.Board EMPTY = new DungeonMapPolicy.Board(
            DungeonMapPolicy.Calibration.none(), List.of(), List.of(), List.of(), "");

    private Object cachedIdentity;
    private byte[] cachedColors;
    private int cachedFloor;
    private DungeonMapPolicy.Board cachedTerrain;
    private long decodeCount;
    private final List<CandidateProbe> candidateProbes = new ArrayList<>();
    private long probeDecodeCount;

    private record CandidateProbe(Object identity, DungeonMapTerrainCache terrain) {
    }

    /** Checks inventory candidates without replacing the selected map's terrain. */
    public boolean isDungeonMap(Object mapIdentity, byte[] colors, int floor) {
        if (cachedTerrain != null && cachedIdentity == mapIdentity) {
            return terrain(mapIdentity, colors, floor).calibration().ok();
        }
        if (mapIdentity == null || colors == null || colors.length != MAP_PIXELS) {
            return false;
        }

        CandidateProbe probe = null;
        for (int i = 0; i < candidateProbes.size(); i++) {
            if (candidateProbes.get(i).identity() == mapIdentity) {
                probe = candidateProbes.remove(i);
                break;
            }
        }
        if (probe == null) {
            if (candidateProbes.size() == MAX_CANDIDATES) {
                candidateProbes.removeFirst();
            }
            probe = new CandidateProbe(mapIdentity, new DungeonMapTerrainCache());
        }
        candidateProbes.add(probe);
        long previousCount = probe.terrain().decodeCount();
        DungeonMapPolicy.Board board = probe.terrain().terrain(mapIdentity, colors, floor);
        probeDecodeCount += probe.terrain().decodeCount() - previousCount;
        return board.calibration().ok();
    }

    /**
     * Returns terrain without decorations or the local player. Identity is compared
     * by reference; copied buffers with unchanged pixels reuse the same board.
     * A missing map or malformed buffer clears the previous observation.
     */
    public DungeonMapPolicy.Board terrain(Object mapIdentity, byte[] colors, int floor) {
        if (mapIdentity == null || colors == null || colors.length != MAP_PIXELS) {
            clear();
            return EMPTY;
        }
        if (cachedTerrain != null
                && cachedIdentity == mapIdentity
                && cachedFloor == floor
                && Arrays.equals(cachedColors, colors)) {
            return cachedTerrain;
        }

        // Minecraft updates map colors in place. Decode owned bytes so the
        // snapshot matches the board and detects later writes.
        byte[] snapshot = colors.clone();
        DungeonMapPolicy.Board decoded = DungeonMapPolicy.scan(
                snapshot, MAP_SIDE, List.of(), null, null, 0.0F, "", "", floor);
        cachedIdentity = mapIdentity;
        cachedColors = snapshot;
        cachedFloor = floor;
        cachedTerrain = decoded;
        decodeCount++;
        return decoded;
    }

    /** Releases the current map; the lifetime decode counter is preserved. */
    public void clear() {
        cachedIdentity = null;
        cachedColors = null;
        cachedFloor = 0;
        cachedTerrain = null;
        candidateProbes.clear();
    }

    long decodeCount() {
        return decodeCount;
    }

    int candidateCount() {
        return candidateProbes.size();
    }

    long probeDecodeCount() {
        return probeDecodeCount;
    }
}
