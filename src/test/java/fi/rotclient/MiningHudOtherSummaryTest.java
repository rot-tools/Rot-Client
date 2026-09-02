package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MiningHudOtherSummaryTest {
    @Test
    void emptyOtherShowsZeroQuantityAndValue() {
        MiningHudOtherSummary summary =
                MiningHudOtherSummary.available(0L, 0.0, 0);
        assertTrue(summary.analyticsActive());
        assertTrue(summary.isEmpty());
        assertEquals(0L, summary.totalQuantity());
        assertEquals(0.0, summary.resolvedGrossValue(), 0.0001);
        assertFalse(summary.hasUnresolved());
        assertEquals("0", summary.valueDisplayCompact("0"));
    }

    @Test
    void sumsMultipleOtherMaterialsForQuantityOnly() {
        Map<String, Long> other = new LinkedHashMap<>();
        other.put("MITHRIL", 1_000L);
        other.put("TITANIUM", 50L);
        other.put("HARD_STONE", 11_750L);
        assertEquals(12_800L, MiningHudOtherSummary.sumQuantityMap(other));

        MiningHudOtherSummary summary = MiningHudOtherSummary.available(
                MiningHudOtherSummary.sumQuantityMap(other),
                33_300.0,
                0);
        assertEquals(12_800L, summary.totalQuantity());
        assertEquals(33_300.0, summary.resolvedGrossValue(), 0.0001);
        assertTrue(summary.singleSummaryRow());
    }

    @Test
    void resolvedValueSumsDoNotTreatUnresolvedAsZero() {
        MiningHudOtherSummary summary = MiningHudOtherSummary.available(
                1_250L,
                21_000.0,
                2);
        assertEquals(1_250L, summary.totalQuantity());
        assertEquals(21_000.0, summary.resolvedGrossValue(), 0.0001);
        assertTrue(summary.hasUnresolved());
        assertEquals("21k+", summary.valueDisplayCompact("21k"));
    }

    @Test
    void unresolvedQuantityStillIncludedInTotal() {
        // Quantity includes all OTHER_MINED units; value only resolved portion.
        MiningHudOtherSummary summary = MiningHudOtherSummary.available(
                500L + 40L,
                100.0,
                1);
        assertEquals(540L, summary.totalQuantity());
        assertEquals(100.0, summary.resolvedGrossValue(), 0.0001);
        assertTrue(summary.hasUnresolved());
    }

    @Test
    void targetCurrencyAndUnattributedMustNotBePassedUnfiltered() {
        // Documented caller contract: only eligible HUD OTHERS rows.
        // TARGET / CURRENCY / UNATTRIBUTED are filtered by the projection.
        Map<String, Long> targetOnly = Map.of("GOLD_INGOT", 40_500L);
        Map<String, Long> currencyOnly = Map.of("GEMSTONE_POWDER", 291L);
        Map<String, Long> otherOnly = Map.of("MITHRIL", 840L, "TITANIUM", 52L);

        long otherQty = MiningHudOtherSummary.sumQuantityMap(otherOnly);
        assertEquals(892L, otherQty);
        assertEquals(40_500L, MiningHudOtherSummary.sumQuantityMap(targetOnly));
        assertEquals(291L, MiningHudOtherSummary.sumQuantityMap(currencyOnly));
        MiningHudOtherSummary summary =
                MiningHudOtherSummary.available(otherQty, 19_700.0, 0);
        assertEquals(892L, summary.totalQuantity());
        assertFalse(summary.totalQuantity() >= 40_500L);
    }

    @Test
    void hudRowLabelIsOthers() {
        assertEquals("Others", MiningHudOtherSummary.hudRowLabel());
    }

    @Test
    void analyticsInactiveDoesNotFabricateZerosAsTracked() {
        MiningHudOtherSummary summary =
                MiningHudOtherSummary.analyticsInactive();
        assertFalse(summary.analyticsActive());
        assertEquals("—", summary.valueDisplayCompact("0"));
        assertEquals(
                MiningHudOtherSummary.Availability.ANALYTICS_INACTIVE,
                summary.availability());
    }

    @Test
    void netValueAppliesExistingTaxHelper() {
        MiningHudOtherSummary summary =
                MiningHudOtherSummary.available(100L, 1000.0, 0);
        assertEquals(
                SessionAccounting.afterTax(1000.0, 1.25),
                summary.resolvedNetValue(1.25),
                0.0001);
    }

    @Test
    void hudModelIsSingleAggregateRowOnly() {
        MiningHudOtherSummary summary =
                MiningHudOtherSummary.available(12_800L, 33_300.0, 0);
        assertTrue(summary.singleSummaryRow());
        assertEquals(12_800L, summary.totalQuantity());
        assertEquals(33_300.0, summary.resolvedGrossValue(), 0.0001);
    }

    @Test
    void profitCardHeightGrowsByFixedOtherBlockNotPerMaterial() {
        int oneMaterial = HudLayoutMath.profitCardHeight(
                true, true, true, false, false, false, 1, true);
        int twoMaterials = HudLayoutMath.profitCardHeight(
                true, true, true, false, false, false, 2, true);
        // Target rows grow with material count; OTHER block stays fixed size.
        int delta = twoMaterials - oneMaterial;
        assertEquals(36, delta); // 18 raw + 18 enchanted for second material
        int withoutOther = HudLayoutMath.profitCardHeight(
                true, true, true, false, false, false, 1, false);
        assertTrue(oneMaterial > withoutOther);
        // OTHER heading+row+totals; divider exists in both item-row layouts.
        assertEquals(14 + 18 + 48, oneMaterial - withoutOther);
    }

    @Test
    void expandedProfitCardIncludedInHudBoundsMath() {
        int top = HudLayoutMath.topCardHeight(true, true, true, true);
        int profit = HudLayoutMath.profitCardHeight(
                true, true, true, true, true, true, 1, true);
        int full = top + 6 + profit;
        assertTrue(full > top + 6 + 86);
        assertEquals(
                full,
                top + 6 + profit);
    }
}
