package fi.rotclient;

import java.util.HashSet;
import java.util.Set;

/**
 * Correlates a recently attacked gemstone block with server-side removals of
 * the direct target and immediately adjacent Gemstone Spread blocks. Each
 * physical position is accepted at most once per mining action. This class has
 * no Minecraft runtime dependencies.
 */
final class GemstoneDirectBreakTracker {
    static final long DEFAULT_TARGET_WINDOW_MILLIS =
            2_500L;

    static final long DEFAULT_SPREAD_WINDOW_MILLIS =
            500L;

    /** Current Hypixel sources provide less than 100 Gemstone Spread. */
    static final int MAX_SPREAD_BLOCKS_PER_ACTION =
            1;

    private final long targetWindowMillis;

    private final long spreadWindowMillis;

    private long targetPosition =
            Long.MIN_VALUE;

    private Position targetCoordinates;

    private GemstoneType targetGemstone;

    private long targetEpochMillis =
            -1L;

    private final Set<Position> acceptedPositions =
            new HashSet<>();

    private int acceptedSpreadBlocks;

    GemstoneDirectBreakTracker() {
        this(
                DEFAULT_TARGET_WINDOW_MILLIS,
                DEFAULT_SPREAD_WINDOW_MILLIS);
    }

    GemstoneDirectBreakTracker(
            long targetWindowMillis) {
        this(
                targetWindowMillis,
                Math.min(
                        targetWindowMillis,
                        DEFAULT_SPREAD_WINDOW_MILLIS));
    }

    GemstoneDirectBreakTracker(
            long targetWindowMillis,
            long spreadWindowMillis) {
        if (targetWindowMillis <= 0L
                || spreadWindowMillis <= 0L
                || spreadWindowMillis > targetWindowMillis) {
            throw new IllegalArgumentException(
                    "Spread window must be positive and no longer than target window");
        }

        this.targetWindowMillis =
                targetWindowMillis;

        this.spreadWindowMillis =
                spreadWindowMillis;
    }

    boolean recordTarget(
            long position,
            GemstoneType gemstone,
            long epochMillis) {
        requireTimestamp(epochMillis);

        if (gemstone == null) {
            clear();
            return false;
        }

        targetPosition =
                position;

        targetCoordinates =
                null;

        targetGemstone =
                gemstone;

        targetEpochMillis =
                epochMillis;

        return true;
    }

    boolean recordTarget(
            Position position,
            GemstoneType gemstone,
            long epochMillis) {
        requireTimestamp(epochMillis);

        if (position == null
                || gemstone == null) {
            clear();
            return false;
        }

        boolean sameTarget =
                position.equals(targetCoordinates)
                        && gemstone == targetGemstone
                        && targetEpochMillis >= 0L
                        && (epochMillis < targetEpochMillis
                        || epochMillis - targetEpochMillis
                        <= targetWindowMillis);

        targetPosition =
                Long.MIN_VALUE;

        targetCoordinates =
                position;

        targetGemstone =
                gemstone;

        targetEpochMillis =
                epochMillis;

        if (!sameTarget) {
            acceptedPositions.clear();
            acceptedSpreadBlocks =
                    0;
        }

        return true;
    }

    GemstoneType acceptRemoval(
            long position,
            GemstoneType previousGemstone,
            boolean becameAir,
            long epochMillis) {
        requireTimestamp(epochMillis);

        if (!becameAir
                || previousGemstone == null
                || targetGemstone == null
                || targetEpochMillis < 0L) {
            return null;
        }

        long ageMillis =
                epochMillis >= targetEpochMillis
                        ? epochMillis - targetEpochMillis
                        : 0L;

        boolean accepted =
                ageMillis <= targetWindowMillis
                        && position == targetPosition
                        && previousGemstone == targetGemstone;

        if (!accepted) {
            return null;
        }

        GemstoneType result =
                targetGemstone;

        clear();
        return result;
    }

    Acceptance acceptRemoval(
            Position position,
            GemstoneType previousGemstone,
            boolean becameAir,
            long epochMillis) {
        requireTimestamp(epochMillis);

        if (!becameAir
                || position == null
                || previousGemstone == null
                || targetGemstone == null
                || targetCoordinates == null
                || targetEpochMillis < 0L) {
            return null;
        }

        long ageMillis =
                epochMillis >= targetEpochMillis
                        ? epochMillis - targetEpochMillis
                        : 0L;

        boolean directTarget =
                position.equals(targetCoordinates)
                        && previousGemstone == targetGemstone
                        && ageMillis <= targetWindowMillis;

        boolean spread =
                !directTarget
                        && ageMillis <= spreadWindowMillis
                        && position.isAdjacentTo(targetCoordinates)
                        && acceptedSpreadBlocks
                        < MAX_SPREAD_BLOCKS_PER_ACTION;

        if ((!directTarget && !spread)
                || !acceptedPositions.add(position)) {
            return null;
        }

        if (spread) {
            acceptedSpreadBlocks =
                    Math.addExact(
                            acceptedSpreadBlocks,
                            1);
        }

        return new Acceptance(
                previousGemstone,
                directTarget
                        ? Evidence.DIRECT_TARGET
                        : Evidence.GEMSTONE_SPREAD);
    }

    void clearExpired(
            long epochMillis) {
        requireTimestamp(epochMillis);

        if (targetEpochMillis < 0L) {
            return;
        }

        if (epochMillis >= targetEpochMillis
                && epochMillis - targetEpochMillis
                > targetWindowMillis) {
            clear();
        }
    }

    void clear() {
        targetPosition =
                Long.MIN_VALUE;

        targetCoordinates =
                null;

        targetGemstone =
                null;

        targetEpochMillis =
                -1L;

        acceptedPositions.clear();
        acceptedSpreadBlocks =
                0;
    }

    private static void requireTimestamp(
            long epochMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be negative");
        }
    }

    enum Evidence {
        DIRECT_TARGET,
        GEMSTONE_SPREAD
    }

    record Acceptance(
            GemstoneType gemstone,
            Evidence evidence) {
    }

    record Position(
            int x,
            int y,
            int z) {
        boolean isAdjacentTo(
                Position other) {
            if (other == null) {
                return false;
            }

            int dx = Math.abs(x - other.x);
            int dy = Math.abs(y - other.y);
            int dz = Math.abs(z - other.z);

            return dx <= 1
                    && dy <= 1
                    && dz <= 1
                    && dx + dy + dz > 0;
        }
    }
}
