package fi.rotclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes Bazaar sale chat delivery idempotent without suppressing separate
 * player-initiated sales. Duplicate component delivery happens immediately;
 * a later confirmation is treated as a new transaction.
 */
final class SaleObservationGate {
    private static final long DUPLICATE_WINDOW_MILLIS = 750L;

    private final Map<String, Long> recentSales = new HashMap<>();

    boolean shouldCredit(boolean enchanted, long amount, double grossCoins, long now) {
        if (amount <= 0 || grossCoins <= 0 || !Double.isFinite(grossCoins)) return false;

        recentSales.entrySet().removeIf(entry ->
                now >= entry.getValue()
                        && now - entry.getValue() > DUPLICATE_WINDOW_MILLIS);
        String fingerprint = enchanted + "|" + amount + "|"
                + Double.doubleToLongBits(grossCoins);
        Long previousDelivery = recentSales.get(fingerprint);
        if (previousDelivery != null
                && now >= previousDelivery
                && now - previousDelivery <= DUPLICATE_WINDOW_MILLIS) {
            return false;
        }
        recentSales.put(fingerprint, now);
        return true;
    }

    void reset() {
        recentSales.clear();
    }
}
