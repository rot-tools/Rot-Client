package fi.rotclient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Opens a bounded loot window only after a mob that the local player recently
 * attacked emits the vanilla death event. Area names and item allow-lists are
 * deliberately irrelevant here.
 */
final class MobLootAttribution {
    static final long ATTACK_CONTEXT_MILLIS = 10_000L;
    static final long LOOT_WINDOW_MILLIS = 3_000L;
    /** Hypixel Combat Sack batches often arrive after the inventory window. */
    static final long SACK_WINDOW_MILLIS = 10_000L;

    private final Map<Integer, Long> attackedEntities = new HashMap<>();
    private long lootWindowEndsAtMillis = -1L;
    private long sackWindowEndsAtMillis = -1L;

    void recordAttack(int entityId, long nowMillis) {
        if (entityId < 0 || nowMillis < 0L) return;
        expireAttacks(nowMillis);
        attackedEntities.put(entityId, nowMillis);
    }

    boolean recordDeath(int entityId, long nowMillis) {
        if (entityId < 0 || nowMillis < 0L) return false;
        expireAttacks(nowMillis);
        Long attackedAt = attackedEntities.remove(entityId);
        if (attackedAt == null
                || nowMillis - attackedAt > ATTACK_CONTEXT_MILLIS) {
            return false;
        }
        lootWindowEndsAtMillis = Math.max(
                lootWindowEndsAtMillis,
                nowMillis + LOOT_WINDOW_MILLIS);
        sackWindowEndsAtMillis = Math.max(
                sackWindowEndsAtMillis,
                nowMillis + SACK_WINDOW_MILLIS);
        return true;
    }

    boolean allowsInventoryGain(long nowMillis, boolean containerScreenOpen) {
        if (containerScreenOpen || nowMillis < 0L) return false;
        expireAttacks(nowMillis);
        return nowMillis <= lootWindowEndsAtMillis;
    }

    boolean allowsSackGain(long nowMillis) {
        if (nowMillis < 0L) return false;
        expireAttacks(nowMillis);
        return nowMillis <= sackWindowEndsAtMillis;
    }

    void reset() {
        attackedEntities.clear();
        lootWindowEndsAtMillis = -1L;
        sackWindowEndsAtMillis = -1L;
    }

    private void expireAttacks(long nowMillis) {
        Iterator<Map.Entry<Integer, Long>> iterator =
                attackedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            long attackedAt = iterator.next().getValue();
            if (nowMillis - attackedAt > ATTACK_CONTEXT_MILLIS) {
                iterator.remove();
            }
        }
        if (nowMillis > lootWindowEndsAtMillis) {
            lootWindowEndsAtMillis = -1L;
        }
        if (nowMillis > sackWindowEndsAtMillis) {
            sackWindowEndsAtMillis = -1L;
        }
    }
}
