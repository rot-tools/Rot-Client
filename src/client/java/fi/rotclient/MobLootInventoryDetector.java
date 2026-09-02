package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts exact positive player-inventory deltas into MOB rows only while a
 * credible, short-lived combat attribution window is open.
 */
final class MobLootInventoryDetector {
    interface Listener {
        void onMobLoot(String itemId, String displayName, long quantity, long nowMillis);
    }

    private final Listener listener;
    private final MobLootAttribution attribution = new MobLootAttribution();
    private Map<ItemKey, Long> previous = Map.of();
    private boolean initialized;
    private String lastCreditKey = "";
    private long lastCreditAtMillis = -1L;

    MobLootInventoryDetector(Listener listener) {
        this.listener = listener;
    }

    void onPlayerAttack(Entity entity, long nowMillis) {
        if (entity == null) return;
        attribution.recordAttack(entity.getId(), nowMillis);
    }

    void onEntityDeath(Entity entity, byte eventId, long nowMillis) {
        if (entity == null || eventId != 3) return;
        if (attribution.recordDeath(entity.getId(), nowMillis)) {
            DiagnosticRecorder.record(
                    "MOB_KILL_CONTEXT",
                    "entity=" + entity.getType().toString());
        }
    }

    boolean recordAttributedKill(
            int entityId,
            long attackAtMillis,
            long deathAtMillis) {
        attribution.recordAttack(entityId, attackAtMillis);
        return attribution.recordDeath(entityId, deathAtMillis);
    }

    void observe(Minecraft client, boolean collectionAllowed, String source) {
        if (client == null || client.player == null) {
            reset();
            return;
        }
        Map<ItemKey, Long> current = snapshot(client);
        long now = System.currentTimeMillis();
        boolean containerOpen = client.gui != null
                && client.gui.screen() instanceof AbstractContainerScreen<?>;
        if (initialized
                && collectionAllowed
                && attribution.allowsInventoryGain(now, containerOpen)) {
            for (Map.Entry<ItemKey, Long> entry : current.entrySet()) {
                long before = previous.getOrDefault(entry.getKey(), 0L);
                long gain = entry.getValue() - before;
                if (gain <= 0L) continue;
                ItemKey item = entry.getKey();
                emitCredit(
                        item.itemId(),
                        item.displayName(),
                        gain,
                        now,
                        source);
            }
        }
        previous = current;
        initialized = true;
    }

    void reset() {
        attribution.reset();
        previous = Map.of();
        initialized = false;
        lastCreditKey = "";
        lastCreditAtMillis = -1L;
    }

    boolean offerSackGain(
            MiningResourceCatalog catalog,
            TrackerSelection selection,
            SackChangeParser.Change change,
            long nowMillis) {
        Optional<MobLootSackEvaluator.Credit> credit =
                MobLootSackEvaluator.evaluate(
                        attribution, catalog, selection, change, nowMillis);
        if (credit.isEmpty()) {
            return false;
        }
        MobLootSackEvaluator.Credit accepted = credit.get();
        return emitCredit(
                accepted.itemId(),
                accepted.displayName(),
                accepted.quantity(),
                nowMillis,
                "sack");
    }

    boolean offerNamedItem(
            MiningResourceCatalog catalog,
            TrackerSelection selection,
            String itemName,
            long quantity,
            long nowMillis,
            String source) {
        Optional<MobLootSackEvaluator.Credit> credit =
                MobLootSackEvaluator.evaluateNamed(
                        attribution,
                        catalog,
                        selection,
                        itemName,
                        quantity,
                        nowMillis);
        if (credit.isEmpty()) {
            return false;
        }
        MobLootSackEvaluator.Credit accepted = credit.get();
        return emitCredit(
                accepted.itemId(),
                accepted.displayName(),
                accepted.quantity(),
                nowMillis,
                source);
    }

    boolean offerCoinGain(long quantity, long nowMillis) {
        if (quantity <= 0L || !attribution.allowsSackGain(nowMillis)) {
            return false;
        }
        return emitCredit("COINS", "Coins", quantity, nowMillis, "coins");
    }

    boolean offerActionBarGain(
            MiningResourceCatalog catalog,
            TrackerSelection selection,
            String plain,
            long nowMillis) {
        Optional<Long> coins = MobLootCoinParser.parse(plain);
        if (coins.isPresent()) {
            return offerCoinGain(coins.get(), nowMillis);
        }
        Optional<ActionBarGainParser.Gain> gain = ActionBarGainParser.parse(plain);
        if (gain.isEmpty()) {
            return false;
        }
        return offerNamedItem(
                catalog,
                selection,
                gain.get().itemName(),
                gain.get().quantity(),
                nowMillis,
                "actionbar");
    }

    private boolean emitCredit(
            String itemId,
            String displayName,
            long quantity,
            long nowMillis,
            String source) {
        String key = itemId + ":" + quantity;
        if (key.equals(lastCreditKey)
                && nowMillis - lastCreditAtMillis <= 600L) {
            return false;
        }
        lastCreditKey = key;
        lastCreditAtMillis = nowMillis;
        listener.onMobLoot(itemId, displayName, quantity, nowMillis);
        DiagnosticRecorder.record(
                "MOB_LOOT_CREDIT",
                "item=" + itemId
                        + " quantity=" + quantity
                        + " source=" + safeSource(source));
        return true;
    }

    private static Map<ItemKey, Long> snapshot(Minecraft client) {
        Map<ItemKey, Long> quantities = new LinkedHashMap<>();
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
                continue;
            }
            String display = stack.getHoverName().getString().trim();
            if (!MobLootNoiseFilter.isLootName(display)) {
                continue;
            }
            SkyBlockItemIdentityResolver.Resolved resolved =
                    SkyBlockItemIdentityResolver.fromTextToken(display);
            ItemKey key = new ItemKey(
                    resolved.stableId(),
                    resolved.displayName());
            quantities.merge(
                    key,
                    (long) stack.getCount(),
                    MobLootInventoryDetector::safeAdd);
        }
        return Map.copyOf(quantities);
    }

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) return "unknown";
        String trimmed = source.trim();
        return trimmed.length() > 48 ? trimmed.substring(0, 48) : trimmed;
    }

    private record ItemKey(String itemId, String displayName) {
        private ItemKey {
            itemId = SkyBlockItemId.normalize(itemId);
            if (itemId.isEmpty()) itemId = SkyBlockItemIdentityResolver.UNKNOWN;
            displayName = displayName == null || displayName.isBlank()
                    ? itemId
                    : displayName.trim();
        }
    }
}
