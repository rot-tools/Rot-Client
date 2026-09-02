package fi.rotclient;

/** Pure dimensions and work estimates for the nearby block fallback scan. */
final class MiningBreakScanPlan {
    static final int HORIZONTAL_RADIUS = 7;
    static final int VERTICAL_RADIUS = 5;
    static final int WIDTH = HORIZONTAL_RADIUS * 2 + 1;
    static final int HEIGHT = VERTICAL_RADIUS * 2 + 1;
    static final int POSITIONS_PER_FRAME = WIDTH * HEIGHT * WIDTH;
    static final int MAX_DETECTORS = 64;

    private MiningBreakScanPlan() {
    }

    static int legacyWorldReads(int detectorCount) {
        return Math.multiplyExact(POSITIONS_PER_FRAME, bounded(detectorCount));
    }

    static int sharedWorldReads(int detectorCount) {
        return bounded(detectorCount) == 0 ? 0 : POSITIONS_PER_FRAME;
    }

    private static int bounded(int detectorCount) {
        if (detectorCount < 0 || detectorCount > MAX_DETECTORS) {
            throw new IllegalArgumentException(
                    "detectorCount must be between 0 and " + MAX_DETECTORS);
        }
        return detectorCount;
    }
}
