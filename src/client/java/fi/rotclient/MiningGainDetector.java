package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Measures one material entering the inventory or Mining Sacks. Inventory
 * baselines are maintained even while another material is selected so changing
 * the dashboard target cannot turn an existing stack into a false gain.
 */
final class MiningGainDetector {
    private static final Pattern COMPACT_MESSAGE = Pattern.compile(
            "^COMPACT!\\s+You found an?\\s+(.+)!$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SACK_ITEM = Pattern.compile(
            "\\+([\\d,]+)\\s+([^\\n(]+?)\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BAZAAR_ITEM_SOLD = Pattern.compile(
            "^\\[Bazaar]\\s+Sold\\s+([\\d,]+)x\\s+(.+?)\\s+for\\s+([\\d,.]+)\\s+coins!$",
            Pattern.CASE_INSENSITIVE);

    private final TrackedMaterial material;
    private final SackObservationGate sackObservationGate = new SackObservationGate();
    private long previousRawItems = -1;
    private long previousInventoryEquivalent = -1;

    MiningGainDetector(TrackedMaterial material) {
        this.material = material;
    }

    void tick(Minecraft client) {
        observeInventory(client, "tick");
    }

    void onInventoryPacket(Minecraft client, String packetType) {
        observeInventory(client, packetType);
    }

    private void observeInventory(Minecraft client, String observationSource) {
        if (client.player == null) {
            previousRawItems = -1;
            previousInventoryEquivalent = -1;
            return;
        }

        long rawTotal = 0;
        long equivalentTotal = 0;
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString().trim();
            long multiplier = material.displayMultiplier(name);
            if (multiplier <= 0) continue;
            equivalentTotal += multiplier * stack.getCount();
            if (multiplier == 1) rawTotal += stack.getCount();
        }

        long now = System.currentTimeMillis();
        long rawDelta = previousRawItems >= 0
                ? rawTotal - previousRawItems
                : 0L;
        if (previousRawItems >= 0
                && (rawDelta != 0L
                || equivalentTotal != previousInventoryEquivalent)) {
            DiagnosticRecorder.record("INVENTORY_RESOURCE",
                    "material=" + material.id()
                            + " source=" + observationSource
                            + " rawBefore=" + previousRawItems
                            + " rawAfter=" + rawTotal
                            + " rawDelta=" + rawDelta
                            + " equivalentBefore=" + previousInventoryEquivalent
                            + " equivalentAfter=" + equivalentTotal
                            + " equivalentDelta="
                            + (equivalentTotal - previousInventoryEquivalent));
            TrackingRuntimeTrace.inventoryScan(
                    material.id(),
                    previousRawItems,
                    rawTotal,
                    rawDelta,
                    observationSource,
                    rawDelta == 0L
                            ? TrackingRuntimeTrace.Reason.REJECTED_ZERO_DELTA.name()
                            : "DELTA");
        }

        if (previousRawItems >= 0
                && rawTotal > previousRawItems
                && RotClientClient.isExpecting(material, now)) {
            RotClientClient.onInventoryRawGained(
                    material, rawTotal - previousRawItems);
        } else if (previousRawItems >= 0
                && rawTotal < previousRawItems
                && RotClientClient.isActive(material, now)
                && RotClientClient.isExpecting(material, now)) {
            RotClientClient.onRawConsumedByCompactor(
                    material, previousRawItems - rawTotal);
        } else if (previousRawItems >= 0
                && rawTotal > previousRawItems
                && !RotClientClient.tracksMaterial(material)) {
            // Not the dashboard's live target: this material (e.g. Hard
            // Stone, Cobblestone) has no live accounting to feed, but the
            // gain is still exact and should reach Current Session as
            // OTHER_MINED when it correlates with a confirmed break.
            RotClientClient.onOtherMaterialInventoryGained(
                    material, rawTotal - previousRawItems, now);
        }

        previousRawItems = rawTotal;
        previousInventoryEquivalent = equivalentTotal;
    }

    void inspectMessage(Component message) {
        String plainMessage = message.getString().trim();

        Matcher soldMatcher = BAZAAR_ITEM_SOLD.matcher(plainMessage);
        if (soldMatcher.matches()) {
            long amount = parseLong(soldMatcher.group(1));
            long multiplier = material.displayMultiplier(soldMatcher.group(2));
            double coins = parseDouble(soldMatcher.group(3));
            if (multiplier == 1) {
                RotClientClient.onBazaarSold(
                        material, amount, false, coins);
                return;
            }
            if (multiplier >= material.rawPerEnchanted()
                    && multiplier % material.rawPerEnchanted() == 0) {
                long enchantedUnits = safeMultiply(
                        amount, multiplier / material.rawPerEnchanted());
                RotClientClient.onBazaarSold(
                        material, enchantedUnits, true, coins);
                return;
            }
        }

        Matcher compactMatcher = COMPACT_MESSAGE.matcher(plainMessage);
        if (compactMatcher.matches()) {
            long multiplier = material.displayMultiplier(compactMatcher.group(1));
            if (multiplier >= material.rawPerEnchanted()
                    && multiplier % material.rawPerEnchanted() == 0
                    && RotClientClient.isExpecting(
                    material, System.currentTimeMillis())) {
                RotClientClient.onCompactBonusObserved(
                        material, multiplier / material.rawPerEnchanted());
                return;
            }
        }

        if (!plainMessage.regionMatches(true, 0, "[Sacks]", 0, 7)) return;
        String addedText = firstAddedHoverText(message);
        if (addedText == null) return;

        long sackRaw = 0;
        long sackEnchanted = 0;
        Matcher matcher = SACK_ITEM.matcher(addedText);
        while (matcher.find()) {
            long amount = parseLong(matcher.group(1));
            long multiplier = material.displayMultiplier(matcher.group(2).trim());
            if (multiplier == 1) {
                sackRaw += amount;
            } else if (multiplier >= material.rawPerEnchanted()
                    && multiplier % material.rawPerEnchanted() == 0) {
                sackEnchanted += safeMultiply(
                        amount, multiplier / material.rawPerEnchanted());
            }
        }

        long now = System.currentTimeMillis();
        long sessionBlocks = RotClientClient.sessionBlocks(material);
        boolean accepted = sackObservationGate.shouldCredit(
                plainMessage, sackRaw, sackEnchanted, sessionBlocks, now);
        DiagnosticRecorder.record("SACK_SUMMARY",
                "material=" + material.id()
                        + " raw=" + sackRaw
                        + " enchanted=" + sackEnchanted
                        + " accepted=" + accepted);
        if (!accepted) return;
        RotClientClient.onSackObserved(material, sackRaw, sackEnchanted);
    }

    void resetSession(long sessionBlocks) {
        sackObservationGate.reset(sessionBlocks);
    }

    private static String firstAddedHoverText(Component component) {
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            String text = showText.value().getString();
            if (text.regionMatches(true, 0, "Added items:", 0, 12)) {
                return text;
            }
        }
        for (Component sibling : component.getSiblings()) {
            String found = firstAddedHoverText(sibling);
            if (found != null) return found;
        }
        return null;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static long safeMultiply(long first, long second) {
        if (first <= 0 || second <= 0) return 0;
        if (first > Long.MAX_VALUE / second) return Long.MAX_VALUE;
        return first * second;
    }
}
