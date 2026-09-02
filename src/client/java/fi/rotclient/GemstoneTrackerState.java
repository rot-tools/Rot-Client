package fi.rotclient;

/**
 * Persistent session and lifetime state for exactly one gemstone type.
 */
final class GemstoneTrackerState {
    private GemstoneLedger sessionLedger =
            new GemstoneLedger();

    private GemstoneLedger totalLedger =
            new GemstoneLedger();

    long sessionBlocks;
    long totalBlocks;

    long sessionActiveMillis;
    long totalActiveMillis;

    long lastBreakEpochMillis;

    GemstoneLedger sessionLedger() {
        return sessionLedger;
    }

    GemstoneLedger totalLedger() {
        return totalLedger;
    }

    void recordGain(
            GemstoneTier tier,
            long amount) {
        if (tier == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier cannot be null");
        }

        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Gemstone gain cannot be negative");
        }

        sessionLedger.add(tier, amount);
        totalLedger.add(tier, amount);
    }

    void recordBlock(long epochMillis) {
        if (epochMillis < 0) {
            throw new IllegalArgumentException(
                    "Break timestamp cannot be negative");
        }

        long nextSessionBlocks = Math.addExact(
                sessionBlocks,
                1L);

        long nextTotalBlocks = Math.addExact(
                totalBlocks,
                1L);

        sessionBlocks = nextSessionBlocks;
        totalBlocks = nextTotalBlocks;
        lastBreakEpochMillis = epochMillis;
    }

    void addActiveMillis(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException(
                    "Active time cannot be negative");
        }

        long nextSessionActiveMillis = Math.addExact(
                sessionActiveMillis,
                millis);

        long nextTotalActiveMillis = Math.addExact(
                totalActiveMillis,
                millis);

        sessionActiveMillis = nextSessionActiveMillis;
        totalActiveMillis = nextTotalActiveMillis;
    }

    long currentSessionActiveMillis(
            long epochMillis,
            long pauseAfterMillis) {
        validateActivityArguments(
                epochMillis,
                pauseAfterMillis);

        if (lastBreakEpochMillis <= 0L) {
            return sessionActiveMillis;
        }

        long elapsed =
                elapsedSinceLastBreak(
                        epochMillis);

        long openWindow =
                Math.min(
                        pauseAfterMillis,
                        elapsed);

        return Math.addExact(
                sessionActiveMillis,
                openWindow);
    }

    long currentTotalActiveMillis(
            long epochMillis,
            long pauseAfterMillis) {
        validateActivityArguments(
                epochMillis,
                pauseAfterMillis);

        if (lastBreakEpochMillis <= 0L) {
            return totalActiveMillis;
        }

        long elapsed =
                elapsedSinceLastBreak(
                        epochMillis);

        long openWindow =
                Math.min(
                        pauseAfterMillis,
                        elapsed);

        return Math.addExact(
                totalActiveMillis,
                openWindow);
    }

    boolean isActive(
            long epochMillis,
            long pauseAfterMillis) {
        validateActivityArguments(
                epochMillis,
                pauseAfterMillis);

        return lastBreakEpochMillis > 0L
                && epochMillis >= lastBreakEpochMillis
                && epochMillis
                - lastBreakEpochMillis
                < pauseAfterMillis;
    }

    void persistActiveTime(
            long epochMillis,
            long pauseAfterMillis) {
        validateActivityArguments(
                epochMillis,
                pauseAfterMillis);

        if (lastBreakEpochMillis <= 0L) {
            return;
        }

        if (epochMillis < lastBreakEpochMillis) {
            return;
        }

        long elapsed =
                elapsedSinceLastBreak(
                        epochMillis);

        long addition =
                Math.min(
                        pauseAfterMillis,
                        elapsed);

        addActiveMillis(
                addition);

        lastBreakEpochMillis =
                elapsed < pauseAfterMillis
                        ? epochMillis
                        : 0L;
    }

    private long elapsedSinceLastBreak(
            long epochMillis) {
        if (epochMillis <= lastBreakEpochMillis) {
            return 0L;
        }
        return epochMillis - lastBreakEpochMillis;
    }

    private static void validateActivityArguments(
            long epochMillis,
            long pauseAfterMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException(
                    "Activity timestamp cannot be negative");
        }

        if (pauseAfterMillis <= 0L) {
            throw new IllegalArgumentException(
                    "Pause window must be positive");
        }
    }

    void resetSession() {
        sessionLedger.clear();
        sessionBlocks = 0L;
        sessionActiveMillis = 0L;
        lastBreakEpochMillis = 0L;
    }

    void normalize() {
        if (sessionLedger == null) {
            sessionLedger = new GemstoneLedger();
        }
        if (totalLedger == null) {
            totalLedger = new GemstoneLedger();
        }

        sessionLedger.normalize();
        totalLedger.normalize();

        sessionBlocks = Math.max(
                0L,
                sessionBlocks);

        totalBlocks = Math.max(
                0L,
                totalBlocks);

        sessionActiveMillis = Math.max(
                0L,
                sessionActiveMillis);

        totalActiveMillis = Math.max(
                0L,
                totalActiveMillis);

        lastBreakEpochMillis = Math.max(
                0L,
                lastBreakEpochMillis);
    }
}
