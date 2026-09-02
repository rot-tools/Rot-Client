package fi.rotclient;

/**
 * Persistent counters and observations for exactly one tracked material.
 *
 * <p>Keeping these values in separate ledgers prevents a target switch from
 * mixing material totals. UI preferences, Bazaar tax and Fortune are
 * intentionally stored in {@link TrackerConfig}, because they apply globally.</p>
 */
final class MaterialTrackerState {
    long totalBlocks;
    long totalBaseDrops;
    long totalActiveMillis;
    long sessionBlocks;
    long sessionBaseDrops;
    long sessionActiveMillis;
    long lastBreakEpochMillis;

    double lastRawPrice;
    double lastRawSellOfferPrice;
    double lastEnchantedPrice;
    long lastPriceUpdateEpochMillis;

    long sessionActualRawEquivalent;
    long totalActualRawEquivalent;
    long sessionInventoryRaw;
    long totalInventoryRaw;
    long sessionCompactBonusEnchanted;
    long totalCompactBonusEnchanted;
    long sessionSackRaw;
    long totalSackRaw;
    long sessionSackEnchanted;
    long totalSackEnchanted;
    long sessionCompactedEnchanted;
    long totalCompactedEnchanted;
    long sessionSoldRaw;
    long sessionSoldEnchanted;
    double sessionRealizedGrossCoins;
    long compactorRawRemainder;
    long lastActualRawEquivalentEpochMillis;
    String actualRawEquivalentSource = "none";

    void resetSession() {
        sessionBlocks = 0;
        sessionBaseDrops = 0;
        sessionActiveMillis = 0;
        lastBreakEpochMillis = 0;
        sessionActualRawEquivalent = 0;
        sessionInventoryRaw = 0;
        sessionCompactBonusEnchanted = 0;
        sessionSackRaw = 0;
        sessionSackEnchanted = 0;
        sessionCompactedEnchanted = 0;
        sessionSoldRaw = 0;
        sessionSoldEnchanted = 0;
        sessionRealizedGrossCoins = 0;
        compactorRawRemainder = 0;
        lastActualRawEquivalentEpochMillis = 0;
        actualRawEquivalentSource = "calibrating";
    }

    void normalize() {
        totalBlocks = nonNegative(totalBlocks);
        totalBaseDrops = nonNegative(totalBaseDrops);
        totalActiveMillis = nonNegative(totalActiveMillis);
        sessionBlocks = nonNegative(sessionBlocks);
        sessionBaseDrops = nonNegative(sessionBaseDrops);
        sessionActiveMillis = nonNegative(sessionActiveMillis);
        lastBreakEpochMillis = nonNegative(lastBreakEpochMillis);
        lastRawPrice = nonNegativeFinite(lastRawPrice);
        lastRawSellOfferPrice = nonNegativeFinite(lastRawSellOfferPrice);
        lastEnchantedPrice = nonNegativeFinite(lastEnchantedPrice);
        lastPriceUpdateEpochMillis = nonNegative(lastPriceUpdateEpochMillis);
        sessionActualRawEquivalent = nonNegative(sessionActualRawEquivalent);
        totalActualRawEquivalent = nonNegative(totalActualRawEquivalent);
        sessionInventoryRaw = nonNegative(sessionInventoryRaw);
        totalInventoryRaw = nonNegative(totalInventoryRaw);
        sessionCompactBonusEnchanted = nonNegative(sessionCompactBonusEnchanted);
        totalCompactBonusEnchanted = nonNegative(totalCompactBonusEnchanted);
        sessionSackRaw = nonNegative(sessionSackRaw);
        totalSackRaw = nonNegative(totalSackRaw);
        sessionSackEnchanted = nonNegative(sessionSackEnchanted);
        totalSackEnchanted = nonNegative(totalSackEnchanted);
        sessionCompactedEnchanted = nonNegative(sessionCompactedEnchanted);
        totalCompactedEnchanted = nonNegative(totalCompactedEnchanted);
        sessionSoldRaw = nonNegative(sessionSoldRaw);
        sessionSoldEnchanted = nonNegative(sessionSoldEnchanted);
        sessionRealizedGrossCoins = nonNegativeFinite(sessionRealizedGrossCoins);
        compactorRawRemainder = nonNegative(compactorRawRemainder);
        lastActualRawEquivalentEpochMillis =
                nonNegative(lastActualRawEquivalentEpochMillis);
        if (actualRawEquivalentSource == null || actualRawEquivalentSource.isBlank()) {
            actualRawEquivalentSource = "none";
        }
    }

    private static long nonNegative(long value) {
        return Math.max(0, value);
    }

    private static double nonNegativeFinite(double value) {
        return Double.isFinite(value) ? Math.max(0, value) : 0;
    }
}
