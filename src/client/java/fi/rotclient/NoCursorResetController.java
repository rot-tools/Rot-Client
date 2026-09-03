package fi.rotclient;

/**
 * Runtime state for No Cursor Reset GUI→GUI cursor preservation.
 */
public final class NoCursorResetController {
    private boolean pendingRestore;
    private double savedX;
    private double savedY;
    private long transitionEpochMs;
    private int timeoutMs = NoCursorResetPolicy.DEFAULT_TIMEOUT_MS;
    private int remainingRestores;

    /** Keep the cursor where it is for the next GUI→GUI screen change. */
    public void forcePreserveNext(double currentX, double currentY, long nowMs) {
        rememberForNextScreen(currentX, currentY, nowMs, NoCursorResetPolicy.MAX_TIMEOUT_MS);
    }

    public void onScreenChanging(
            boolean moduleEnabled,
            boolean previousWasScreen,
            boolean nextIsScreen,
            double currentX,
            double currentY,
            long nowMs,
            int configuredTimeoutMs) {
        timeoutMs = NoCursorResetPolicy.clampTimeoutMs(configuredTimeoutMs);
        if (NoCursorResetPolicy.shouldRecaptureOnGameplay(
                previousWasScreen, nextIsScreen)) {
            pendingRestore = false;
            remainingRestores = 0;
            return;
        }
        pendingRestore = false;
        remainingRestores = 0;
        if (NoCursorResetPolicy.shouldPreserveCursor(
                moduleEnabled,
                previousWasScreen,
                nextIsScreen,
                nowMs,
                nowMs,
                timeoutMs)) {
            savedX = currentX;
            savedY = currentY;
            transitionEpochMs = nowMs;
            pendingRestore = true;
            remainingRestores = 6;
        }
    }

    public void rememberForNextScreen(double currentX, double currentY, long nowMs, int timeoutMs) {
        this.timeoutMs = NoCursorResetPolicy.clampTimeoutMs(timeoutMs);
        savedX = currentX;
        savedY = currentY;
        transitionEpochMs = nowMs;
        pendingRestore = true;
        remainingRestores = 6;
    }

    public boolean consumeRestore(long nowMs) {
        if (!shouldRestore(nowMs)) {
            return false;
        }
        remainingRestores--;
        if (remainingRestores <= 0) {
            pendingRestore = false;
        }
        return true;
    }

    /**
     * Allow only the bounded post-transition restores. This covers vanilla's
     * immediate and next-tick recenter without pinning the user's cursor.
     */
    public boolean shouldRestore(long nowMs) {
        if (!pendingRestore) {
            return false;
        }
        if (remainingRestores <= 0) {
            pendingRestore = false;
            return false;
        }
        if (!NoCursorResetPolicy.shouldPreserveCursor(
                true,
                true,
                true,
                transitionEpochMs,
                nowMs,
                timeoutMs)) {
            pendingRestore = false;
            return false;
        }
        return true;
    }

    public double savedX() {
        return savedX;
    }

    public double savedY() {
        return savedY;
    }
}
