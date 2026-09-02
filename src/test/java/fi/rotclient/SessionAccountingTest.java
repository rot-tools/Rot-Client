package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SessionAccountingTest {
    private static final double TAX = 1.25;

    @Test
    void soldFirstRunStaysInEarnedButNotInUnsoldSecondRun() {
        long firstRaw = 20_361;
        long firstEnchanted = 345;
        double firstSaleGross = 24_433 + 159_620;

        SessionAccounting.SaleCredit rawSale =
                SessionAccounting.creditSale(firstRaw, 20_361, 24_433);
        SessionAccounting.SaleCredit enchantedSale =
                SessionAccounting.creditSale(firstEnchanted, 345, 159_620);

        assertEquals(0, SessionAccounting.unsold(firstRaw, rawSale.amount()));
        assertEquals(0, SessionAccounting.unsold(firstEnchanted, enchantedSale.amount()));

        long earnedAfterSecondRunRaw = 40_722;
        long earnedAfterSecondRunEnchanted = 715;
        long unsoldRaw = SessionAccounting.unsold(earnedAfterSecondRunRaw, rawSale.amount());
        long unsoldEnchanted =
                SessionAccounting.unsold(earnedAfterSecondRunEnchanted, enchantedSale.amount());
        assertEquals(20_361, unsoldRaw);
        assertEquals(370, unsoldEnchanted);

        double secondRunGross = unsoldRaw * 1.2 + unsoldEnchanted * 462.6;
        assertEquals(193_150.26, SessionAccounting.afterTax(secondRunGross, TAX), 0.01);
        assertEquals(374_902.60,
                SessionAccounting.sessionNet(firstSaleGross, secondRunGross, TAX), 0.1);
    }

    @Test
    void sellingPreexistingSackItemsCannotConsumeMoreThanThisSessionEarned() {
        SessionAccounting.SaleCredit credit =
                SessionAccounting.creditSale(25, 100, 46_260);
        assertEquals(25, credit.amount());
        assertEquals(11_565, credit.grossCoins(), 0.001);
        assertEquals(0, SessionAccounting.unsold(25, credit.amount()));
    }

    @Test
    void confirmedSaleReplacesUnsoldValueInsteadOfDoublingProfit() {
        double minedGoldGrossValue = 172_000;
        double beforeSale = SessionAccounting.sessionNet(0, minedGoldGrossValue, TAX);
        double afterSale = SessionAccounting.sessionNet(minedGoldGrossValue, 0, TAX);

        assertEquals(beforeSale, afterSale, 0.001);
    }

    @Test
    void goldSessionUsesCurrentInstantSellUnitPrices() {
        double gross = SessionAccounting.marketGross(
                20_361, 271,
                1.2, 462.0);

        assertEquals(149_635.2, gross, 0.001);
        assertEquals(147_764.76, SessionAccounting.afterTax(gross, TAX), 0.001);
    }

    @Test
    void diamondSessionUsesItsOwnInstantSellUnitPrices() {
        double gross = SessionAccounting.marketGross(
                8_240, 94,
                8.1, 1_295.0);

        assertEquals(188_474.0, gross, 0.001);
        assertEquals(186_118.075, SessionAccounting.afterTax(gross, TAX), 0.001);
    }

    @Test
    void goldReconcilesInventoryCompactorAndEverySackSummaryAt160ToOne() {
        SessionAccounting.ResourceReconciliation result =
                SessionAccounting.reconcileResource(
                        100_064, 624, 20, 20_361, 678, 160);

        assertEquals(128_841, result.rawEquivalent());
        assertEquals(20_361, result.rawItems());
        assertEquals(678, result.enchantedItems());
        assertEquals(336_107.7,
                SessionAccounting.marketGross(
                        result.rawItems(), result.enchantedItems(), 1.1, 462.7),
                0.0001);
    }

    @Test
    void diamondReconcilesInventoryCompactorAndEverySackSummaryAt160ToOne() {
        SessionAccounting.ResourceReconciliation result =
                SessionAccounting.reconcileResource(
                        32_480, 200, 3, 1_120, 210, 160);

        assertEquals(34_720, result.rawEquivalent());
        assertEquals(1_120, result.rawItems());
        assertEquals(210, result.enchantedItems());
    }

    @Test
    void matchingInventoryAndSackRawTotalsNeverDoubleAcrossMaterials() {
        for (TrackedMaterial material : TrackedMaterial.values()) {
            if (TrackingTarget.forMaterial(material) == null) {
                continue;
            }
            long observed = 3_465L + material.ordinal();
            SessionAccounting.ResourceReconciliation result =
                    SessionAccounting.reconcileResource(
                            observed,
                            0L,
                            0L,
                            observed,
                            0L,
                            material.rawPerEnchanted());

            assertEquals(observed, result.rawEquivalent(), material.id());
            assertEquals(observed, result.rawItems(), material.id());
            assertEquals(0L, result.enchantedItems(), material.id());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyGoldReconciliationRemainsSourceCompatible() {
        SessionAccounting.GoldReconciliation result = SessionAccounting.reconcileGold(
                100_064, 624, 20, 20_361, 678);

        assertEquals(128_841, result.rawEquivalent());
        assertEquals(20_361, result.rawItems());
        assertEquals(678, result.enchantedItems());
    }
}
