package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HudLayoutMathTest {
    @Test
    void topMetricsRemoveTheirOwnSpaceAndReflow() {
        assertEquals(78, HudLayoutMath.topCardHeight(true, true, true, true));
        assertEquals(67, HudLayoutMath.topCardHeight(false, true, true, true));
        assertEquals(59, HudLayoutMath.topCardHeight(true, false, true, true));
        assertEquals(67, HudLayoutMath.topCardHeight(true, true, false, false));
        assertEquals(37, HudLayoutMath.topCardHeight(false, false, false, false));
    }

    @Test
    void topCardChromeFlagsShrinkHeightWhenDisabled() {
        int full = HudLayoutMath.topCardHeight(
                true, true, true, true, true, true, true, true, true, true);
        assertEquals(78, full);
        int withArea = HudLayoutMath.topCardHeight(
                true, true, true, true, true, true, true, true, true, true, true);
        assertEquals(78 + HudLayoutMath.AREA_ROW_HEIGHT, withArea);
        int noChrome = HudLayoutMath.topCardHeight(
                true, true, true, true, false, true, true, true, false, false);
        assertTrue(full > noChrome);
        int noAutoPause = HudLayoutMath.topCardHeight(
                true, true, true, true, true, false, true, true, true, true);
        assertTrue(full > noAutoPause);
    }

    @Test
    void everyProfitToggleCanCreateTheCardIndependently() {
        assertFalse(HudLayoutMath.hasProfitCard(
                false, false, false, false, false, false));
        assertTrue(HudLayoutMath.hasProfitCard(
                true, false, false, false, false, false));
        assertTrue(HudLayoutMath.hasProfitCard(
                false, true, false, false, false, false));
        assertTrue(HudLayoutMath.hasProfitCard(
                false, false, true, false, false, false));
        assertTrue(HudLayoutMath.hasProfitCard(
                false, false, false, true, false, false));
        assertTrue(HudLayoutMath.hasProfitCard(
                false, false, false, false, true, false));
        assertTrue(HudLayoutMath.hasProfitCard(
                false, false, false, false, false, true));
    }

    @Test
    void itemAndValueRowsHaveMatchingDynamicHeights() {
        assertEquals(75, HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false));
        assertEquals(75, HudLayoutMath.profitCardHeight(
                false, true, false, false, false, false));
        assertEquals(87, HudLayoutMath.profitCardHeight(
                true, true, false, false, false, false));
        assertEquals(64, HudLayoutMath.profitCardHeight(
                false, false, true, false, false, false));
        assertEquals(63, HudLayoutMath.profitCardHeight(
                false, false, false, false, false, true));
    }

    @Test
    void combinedTargetAllocatesRowsForBothMaterialLedgers() {
        assertEquals(111, HudLayoutMath.profitCardHeight(
                true, true, false, false, false, false, 2));
        assertEquals(164, HudLayoutMath.profitCardHeight(
                true, true, true, true, true, true, 2));
    }

    @Test
    void gemstoneTopCardHeightTracksBlocksAndGraphOnly() {
        assertEquals(
                48,
                HudLayoutMath.gemstoneTopCardHeight(false, false));
        assertEquals(
                59,
                HudLayoutMath.gemstoneTopCardHeight(true, false));
        assertEquals(
                67,
                HudLayoutMath.gemstoneTopCardHeight(false, true));
        assertEquals(
                78,
                HudLayoutMath.gemstoneTopCardHeight(true, true));
    }

    @Test
    void gemstoneLedgerAndFullHudHeightsStayInSync() {
        assertEquals(95, HudLayoutMath.gemstoneLedgerCardHeight());
        assertEquals(147, HudLayoutMath.gemstoneHudHeight(false, false));
        assertEquals(158, HudLayoutMath.gemstoneHudHeight(true, false));
        assertEquals(166, HudLayoutMath.gemstoneHudHeight(false, true));
        assertEquals(177, HudLayoutMath.gemstoneHudHeight(true, true));
    }

    @Test
    void otherSectionAndValueFlagsShrinkProfitCard() {
        int full = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, true, true, true);
        int noOther = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                false, true, true, true);
        assertTrue(full > noOther);
        assertEquals(full - HudLayoutMath.ITEM_ROW, noOther);

        int noValues = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, false, false, false);
        assertTrue(full > noValues);
    }

    @Test
    void targetHeadingOffShrinksProfitCardHeader() {
        int withHeading = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, true, true, true, true);
        int withoutHeading = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, true, true, true, false);
        assertEquals(HudLayoutMath.HEADING_ROW, withHeading - withoutHeading);
    }

    @Test
    void valuePanelMasterCanHideProfitCard() {
        assertFalse(HudLayoutMath.hasProfitCard(
                true, true, true, true, true, true,
                false, true, true, true, true));
        assertTrue(HudLayoutMath.hasProfitCard(
                false, false, false, false, false, false,
                true, true, false, false, false));
    }

    @Test
    void allGemstonesLedgerGrowsPerGemstoneThenCapsWithAMoreRow() {
        int empty = HudLayoutMath.gemstoneAllLedgerHeight(0);
        int one = HudLayoutMath.gemstoneAllLedgerHeight(1);
        // Nothing gained yet still reserves one placeholder row.
        assertEquals(one, empty);
        assertEquals(51, one);
        assertEquals(one + HudLayoutMath.VALUE_ROW,
                HudLayoutMath.gemstoneAllLedgerHeight(2));

        int capped = HudLayoutMath.gemstoneAllLedgerHeight(
                HudLayoutMath.MAX_ALL_GEMSTONE_ROWS);
        assertEquals(one + (HudLayoutMath.MAX_ALL_GEMSTONE_ROWS - 1)
                * HudLayoutMath.VALUE_ROW, capped);
        // One more gemstone than fits adds only the "+N more" row.
        assertEquals(capped + HudLayoutMath.VALUE_ROW,
                HudLayoutMath.gemstoneAllLedgerHeight(
                        HudLayoutMath.MAX_ALL_GEMSTONE_ROWS + 1));
        // ...and stays put no matter how many more there are.
        assertEquals(HudLayoutMath.gemstoneAllLedgerHeight(
                        HudLayoutMath.MAX_ALL_GEMSTONE_ROWS + 1),
                HudLayoutMath.gemstoneAllLedgerHeight(12));
        assertEquals(one, HudLayoutMath.gemstoneAllLedgerHeight(-3));
    }

    @Test
    void allGemstonesIsShorterThanTheFullSingleGemstoneLedgerUntilMany() {
        assertTrue(HudLayoutMath.gemstoneAllLedgerHeight(3)
                < HudLayoutMath.gemstoneLedgerCardHeight());
        assertEquals(
                HudLayoutMath.gemstoneTopCardHeight(true, true, false)
                        + HudLayoutMath.SECTION_GAP
                        + HudLayoutMath.gemstoneAllLedgerHeight(2),
                HudLayoutMath.gemstoneAllHudHeight(
                        true, true, false,
                        true, true, true, true, true, true, 2));
    }

    @Test
    void gemstoneHeightAndScaleClampToLogicalScreenBounds() {
        int gemstoneHeight =
                HudLayoutMath.gemstoneHudHeight(true, true);

        assertEquals(
                423.0,
                HudLayoutMath.clampOrigin(
                        500.0,
                        600,
                        gemstoneHeight,
                        1.0F));
        assertEquals(
                334.5,
                HudLayoutMath.clampOrigin(
                        500.0,
                        600,
                        gemstoneHeight,
                        1.5F));
        assertEquals(
                0.0,
                HudLayoutMath.clampOrigin(
                        500.0,
                        200,
                        gemstoneHeight,
                        1.5F));
        assertEquals(
                0.0,
                HudLayoutMath.clampOrigin(
                        -50.0,
                        600,
                        gemstoneHeight,
                        1.0F));
    }
}
