package fi.rotclient;

/**
 * Pure Inventory Walk gates: movement keys may be applied while a
 * GUI is open unless a text field is focused. After a container click,
 * walking resumes only when a keepalive/ping is recent or a keepalive arrived
 * more than {@link #CLICK_RESUME_MS} after the click.
 */
public final class InventoryWalkPolicy {
    public static final int MIN_PING_MS = 1;
    public static final int MAX_PING_MS = 500;
    public static final int DEFAULT_PING_MS = 200;
    public static final int CLICK_RESUME_MS = 500;

    private InventoryWalkPolicy() {
    }

    public static int clampPingMs(int pingMs) {
        if (pingMs < MIN_PING_MS) {
            return MIN_PING_MS;
        }
        if (pingMs > MAX_PING_MS) {
            return MAX_PING_MS;
        }
        return pingMs;
    }

    /**
     * Average-preserving ±20% jitter so a displayed ping window is not a
     * perfect metronome. {@code random01 = 0.5} keeps the configured ping.
     */
    public static int jitteredPingMs(int pingMs, double random01) {
        int base = clampPingMs(pingMs);
        int jittered = (int) Math.round(base * AutoClickerPolicy.humanizeFactor(random01));
        return clampPingMs(jittered);
    }

    /**
     * Slot clicks are allowed only while WASD is up. Walking plus a container
     * click is what Hypixel (and the Serveri) treats as invalid and limbos for.
     */
    public static boolean shouldAllowContainerClick(boolean movementKeysHeld) {
        return !movementKeysHeld;
    }

    /**
     * Freeze walking only when a click slipped through while WASD was down.
     * Standing still and moving items must not interrupt later walking.
     */
    public static boolean shouldFreezeWalkOnClick(boolean movementKeysHeld) {
        return movementKeysHeld;
    }

    public static boolean shouldApplyMouseLook(
            boolean moduleEnabled,
            boolean screenOpen,
            boolean pauseScreen,
            boolean textFieldFocused,
            boolean movementKeysHeld) {
        return moduleEnabled
                && screenOpen
                && !pauseScreen
                && !textFieldFocused
                && movementKeysHeld;
    }

    /**
     * Every container click that happens while walking starts or extends the
     * walk freeze. Releasing keys again when already frozen is unnecessary.
     */
    public static boolean shouldReleaseKeysOnClick(boolean alreadyFrozen) {
        return !alreadyFrozen;
    }

    /**
     * Slot-change packets are not a completed click. Resume walking only from
     * ping/keepalive plus {@link #CLICK_RESUME_MS} after the last click.
     */
    public static boolean shouldAcknowledgeClickFromSlotUpdate() {
        return false;
    }

    /**
     * Movement gate: {@code (!clicked && now - lastPing < ping) || lastPing > clickTime + 500}.
     */
    public static boolean shouldApplyMovement(
            boolean moduleEnabled,
            boolean screenOpen,
            boolean pauseScreen,
            boolean textFieldFocused,
            boolean clicked,
            long nowMs,
            long lastPingMs,
            long clickTimeMs,
            int pingMs) {
        if (!moduleEnabled || !screenOpen || pauseScreen || textFieldFocused) {
            return false;
        }
        if (!clicked && nowMs - lastPingMs < clampPingMs(pingMs)) {
            return true;
        }
        return lastPingMs > clickTimeMs + CLICK_RESUME_MS;
    }
}
