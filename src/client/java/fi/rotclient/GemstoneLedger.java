package fi.rotclient;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stores independent item quantities for every tier of one gemstone type.
 *
 * <p>The ledger never allows negative quantities. It can also convert all
 * stored tiers into one common Rough Gemstone equivalent for rate and session
 * calculations.</p>
 */
final class GemstoneLedger {
    private Map<GemstoneTier, Long> quantities =
            new EnumMap<>(GemstoneTier.class);

    GemstoneLedger() {
        for (GemstoneTier tier : GemstoneTier.values()) {
            quantities.put(tier, 0L);
        }
    }

    long quantity(GemstoneTier tier) {
        requireTier(tier);
        Long value = quantities.get(tier);
        return value == null ? 0L : value;
    }

    void add(GemstoneTier tier, long amount) {
        requireTier(tier);

        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Gemstone amount cannot be negative");
        }

        if (amount == 0) {
            return;
        }

        long current = quantity(tier);
        quantities.put(tier, Math.addExact(current, amount));
    }

    long removeUpTo(GemstoneTier tier, long requestedAmount) {
        requireTier(tier);

        if (requestedAmount < 0) {
            throw new IllegalArgumentException(
                    "Requested gemstone amount cannot be negative");
        }

        long current = quantity(tier);
        long removed = Math.min(current, requestedAmount);

        quantities.put(tier, current - removed);
        return removed;
    }

    long totalItemCount() {
        long total = 0L;

        for (GemstoneTier tier : GemstoneTier.values()) {
            total = Math.addExact(total, quantity(tier));
        }

        return total;
    }

    long totalRoughEquivalent() {
        long total = 0L;

        for (GemstoneTier tier : GemstoneTier.values()) {
            long tierEquivalent = Math.multiplyExact(
                    quantity(tier),
                    tier.roughEquivalent());

            total = Math.addExact(total, tierEquivalent);
        }

        return total;
    }

    boolean isEmpty() {
        return totalItemCount() == 0L;
    }

    void clear() {
        for (GemstoneTier tier : GemstoneTier.values()) {
            quantities.put(tier, 0L);
        }
    }

    void normalize() {
        if (quantities == null) {
            quantities = new EnumMap<>(GemstoneTier.class);
        }

        Map<GemstoneTier, Long> normalized =
                new EnumMap<>(GemstoneTier.class);

        for (GemstoneTier tier : GemstoneTier.values()) {
            Long value = quantities.get(tier);
            if (value == null || value < 0L) {
                normalized.put(tier, 0L);
            } else {
                normalized.put(tier, value);
            }
        }

        quantities = normalized;
    }

    private static void requireTier(GemstoneTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier cannot be null");
        }
    }
}
