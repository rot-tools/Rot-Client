package fi.rotclient;

/**
 * Pure decision helpers for No Cursor Reset.
 */
public final class NoCursorResetPolicy {
    public static final int MIN_TIMEOUT_MS = 0;
    public static final int MAX_TIMEOUT_MS = 1000;
    public static final int DEFAULT_TIMEOUT_MS = 150;

    private NoCursorResetPolicy() {
    }

    public static int clampTimeoutMs(int timeoutMs) {
        if (timeoutMs < MIN_TIMEOUT_MS) {
            return MIN_TIMEOUT_MS;
        }
        if (timeoutMs > MAX_TIMEOUT_MS) {
            return MAX_TIMEOUT_MS;
        }
        return timeoutMs;
    }

    /**
     * Preserve cursor only for supported GUI→GUI transitions while enabled and
     * within the unhook timeout window.
     */
    public static boolean shouldPreserveCursor(
            boolean moduleEnabled,
            boolean previousWasScreen,
            boolean nextIsScreen,
            long transitionEpochMs,
            long nowEpochMs,
            int timeoutMs) {
        if (!moduleEnabled) {
            return false;
        }
        if (!previousWasScreen || !nextIsScreen) {
            return false;
        }
        int safeTimeout = clampTimeoutMs(timeoutMs);
        return nowEpochMs - transitionEpochMs <= safeTimeout;
    }

    public static boolean shouldRecaptureOnGameplay(
            boolean previousWasScreen,
            boolean nextIsScreen) {
        return previousWasScreen && !nextIsScreen;
    }
}
