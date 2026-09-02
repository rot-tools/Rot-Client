package fi.rotclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps sack notifications tied to real mining and makes packet/event delivery
 * idempotent. Some client mod combinations can expose the same server component
 * more than once. The currently open screen is intentionally irrelevant: a
 * legitimate delayed sack summary may arrive while Bazaar or Sell Sacks is open.
 */
final class SackObservationGate {
    private static final long DUPLICATE_WINDOW_MILLIS = 2_000L;

    private final Map<String, Long> recentFingerprints = new HashMap<>();
    private long lastCreditedSessionBlocks;

    boolean shouldCredit(String visibleSummary,
                         long rawGold,
                         long enchantedGold,
                         long sessionBlocks,
                         long now) {
        if (sessionBlocks <= 0 || (rawGold <= 0 && enchantedGold <= 0)) {
            return false;
        }

        String fingerprint = visibleSummary + '|' + rawGold + '|' + enchantedGold;
        recentFingerprints.entrySet().removeIf(entry ->
                now >= entry.getValue()
                        && now - entry.getValue() > DUPLICATE_WINDOW_MILLIS);
        Long previousDelivery = recentFingerprints.get(fingerprint);
        if (previousDelivery != null
                && now >= previousDelivery
                && now - previousDelivery <= DUPLICATE_WINDOW_MILLIS) {
            return false;
        }

        // Every later summary must follow at least one newly verified matching
        // material
        // block. This rejects replays even outside the short fingerprint window.
        if (sessionBlocks <= lastCreditedSessionBlocks) {
            return false;
        }

        recentFingerprints.put(fingerprint, now);
        lastCreditedSessionBlocks = Math.max(lastCreditedSessionBlocks, sessionBlocks);
        return true;
    }

    void reset(long sessionBlocks) {
        recentFingerprints.clear();
        lastCreditedSessionBlocks = Math.max(0, sessionBlocks);
    }
}
