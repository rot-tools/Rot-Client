package fi.rotclient;

final class SessionAccounting {
    private SessionAccounting() {
    }

    static long unsold(long earned, long sold) {
        return Math.max(0, earned - sold);
    }

    static SaleCredit creditSale(long available, long soldAmount, double grossCoins) {
        if (available <= 0 || soldAmount <= 0 || grossCoins <= 0) {
            return new SaleCredit(0, 0);
        }
        long creditedAmount = Math.min(available, soldAmount);
        double creditedGrossCoins = grossCoins * creditedAmount / soldAmount;
        return new SaleCredit(creditedAmount, creditedGrossCoins);
    }

    static double afterTax(double grossCoins, double taxPercent) {
        return grossCoins * Math.max(0, 1.0 - taxPercent / 100.0);
    }

    static double marketGross(long rawItems,
                              long enchantedItems,
                              double rawItemPrice,
                              double enchantedItemPrice) {
        return Math.max(0, rawItems) * Math.max(0, rawItemPrice)
                + Math.max(0, enchantedItems) * Math.max(0, enchantedItemPrice);
    }

    static ResourceReconciliation reconcileResource(long inventoryRawItems,
                                                     long compactedEnchantedItems,
                                                     long compactBonusEnchantedItems,
                                                     long sackRawItems,
                                                     long sackEnchantedItems,
                                                     long rawPerEnchanted) {
        long inventoryRaw = Math.max(0, inventoryRawItems);
        long compacted = Math.max(0, compactedEnchantedItems);
        long compactBonus = Math.max(0, compactBonusEnchantedItems);
        long sackRaw = Math.max(0, sackRawItems);
        long sackEnchanted = Math.max(0, sackEnchantedItems);
        long conversion = Math.max(1, rawPerEnchanted);

        long observedEnchanted = compacted + compactBonus;
        long unseenSackEnchanted = Math.max(0, sackEnchanted - observedEnchanted);
        long inventoryRawRemainder = Math.max(0, inventoryRaw - compacted * conversion);
        long unseenSackRaw = Math.max(0, sackRaw - inventoryRawRemainder);
        long rawEquivalent = inventoryRaw
                + unseenSackRaw
                + compactBonus * conversion
                + unseenSackEnchanted * conversion;
        long enchantedItems = Math.max(observedEnchanted, sackEnchanted);
        long rawItems = Math.max(0, rawEquivalent - enchantedItems * conversion);
        return new ResourceReconciliation(rawEquivalent, rawItems, enchantedItems);
    }

    /**
     * Compatibility bridge for callers compiled around the original Gold-only accounting model.
     */
    @Deprecated(forRemoval = false)
    static GoldReconciliation reconcileGold(long inventoryRawGold,
                                            long compactedGold,
                                            long compactBonusEnchanted,
                                            long sackRawGold,
                                            long sackEnchantedGold) {
        ResourceReconciliation result = reconcileResource(
                inventoryRawGold,
                compactedGold,
                compactBonusEnchanted,
                sackRawGold,
                sackEnchantedGold,
                160L);
        return new GoldReconciliation(
                result.rawEquivalent(),
                result.rawItems(),
                result.enchantedItems());
    }

    static double sessionNet(double realizedGrossCoins, double unsoldGrossCoins, double taxPercent) {
        return afterTax(realizedGrossCoins + unsoldGrossCoins, taxPercent);
    }

    record SaleCredit(long amount, double grossCoins) {
    }

    record ResourceReconciliation(long rawEquivalent, long rawItems, long enchantedItems) {
    }

    /**
     * @deprecated Use {@link ResourceReconciliation} through
     * {@link #reconcileResource(long, long, long, long, long, long)}.
     */
    @Deprecated(forRemoval = false)
    record GoldReconciliation(long rawEquivalent, long rawItems, long enchantedItems) {
    }
}
