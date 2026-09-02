package fi.rotclient;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Credits combat-loot sack gains as MOB while a recent player-attributed kill
 * window is open. Mining and gemstone sacks stay with the mining pipeline.
 */
final class MobLootSackEvaluator {
    private static final Set<String> MOB_LOOT_SACKS = Set.of(
            "combat sack",
            "slayer sack",
            "dungeon sack");
    private static final Set<String> MINING_PROTECTED_SACKS = Set.of(
            "mining sack",
            "enchanted mining sack",
            "gemstone sack",
            "gemstones sack",
            "dwarven sack");
    private MobLootSackEvaluator() {
    }

    record Credit(String itemId, String displayName, long quantity) {
        Credit {
            itemId = SkyBlockItemId.normalize(itemId);
            displayName = displayName == null || displayName.isBlank()
                    ? itemId
                    : displayName.trim();
            quantity = Math.max(0L, quantity);
        }
    }

    static Optional<Credit> evaluate(
            MobLootAttribution attribution,
            MiningResourceCatalog catalog,
            TrackerSelection selection,
            SackChangeParser.Change change,
            long nowMillis) {
        if (attribution == null || change == null || change.delta() <= 0L) {
            return Optional.empty();
        }
        if (isMiningProtectedSack(change) && !isMobLootSack(change)) {
            return Optional.empty();
        }
        return evaluateNamed(
                attribution,
                catalog,
                selection,
                change.itemName(),
                change.delta(),
                nowMillis);
    }

    static Optional<Credit> evaluateNamed(
            MobLootAttribution attribution,
            MiningResourceCatalog catalog,
            TrackerSelection selection,
            String itemName,
            long quantity,
            long nowMillis) {
        if (attribution == null
                || itemName == null
                || itemName.isBlank()
                || quantity <= 0L) {
            return Optional.empty();
        }
        if (!attribution.allowsSackGain(nowMillis)) {
            return Optional.empty();
        }
        if (isCoinName(itemName)) {
            return Optional.of(new Credit("COINS", "Coins", quantity));
        }
        String cleaned = MobLootNoiseFilter.lootItemName(itemName);
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        itemName = cleaned;
        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                catalog == null
                        ? Optional.empty()
                        : catalog.fromExactSackItem(itemName);
        if (definition.isPresent()) {
            return Optional.empty();
        }
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromTextToken(itemName);
        if (resolved.stableId().isEmpty()
                || SkyBlockItemIdentityResolver.UNKNOWN.equals(
                        resolved.stableId())) {
            return Optional.empty();
        }
        return Optional.of(new Credit(
                resolved.stableId(),
                resolved.displayName(),
                quantity));
    }

    static boolean isCoinName(String itemName) {
        if (itemName == null) {
            return false;
        }
        String trimmed = itemName.trim();
        return "coins".equalsIgnoreCase(trimmed)
                || "coin".equalsIgnoreCase(trimmed);
    }

    static boolean isMobLootSack(SackChangeParser.Change change) {
        if (change == null || change.sacks() == null) {
            return false;
        }
        for (String sack : change.sacks()) {
            if (sack == null) {
                continue;
            }
            if (MOB_LOOT_SACKS.contains(
                    sack.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static boolean isMiningProtectedSack(SackChangeParser.Change change) {
        if (change == null || change.sacks() == null) {
            return false;
        }
        for (String sack : change.sacks()) {
            if (sack == null) {
                continue;
            }
            if (MINING_PROTECTED_SACKS.contains(
                    sack.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
