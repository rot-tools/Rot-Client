package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HudLayoutMathTest {
    @Test
    void topMetricsRemoveTheirOwnSpaceAndReflow() {
        assertEquals(167, HudLayoutMath.topCardHeight(true, true, true, true));
        assertEquals(139, HudLayoutMath.topCardHeight(false, true, true, true));
        assertEquals(128, HudLayoutMath.topCardHeight(true, false, true, true));
        assertEquals(138, HudLayoutMath.topCardHeight(true, true, false, false));
        assertEquals(71, HudLayoutMath.topCardHeight(false, false, false, false));
    }

    @Test
    void topCardChromeFlagsShrinkHeightWhenDisabled() {
        int full = HudLayoutMath.topCardHeight(
                true, true, true, true, true, true, true, true, true, true);
        assertEquals(167, full);
        int withArea = HudLayoutMath.topCardHeight(
                true, true, true, true, true, true, true, true, true, true, true);
        assertEquals(167 + HudLayoutMath.AREA_ROW_HEIGHT, withArea);
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
        assertEquals(162, HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false));
        assertEquals(162, HudLayoutMath.profitCardHeight(
                false, true, false, false, false, false));
        assertEquals(180, HudLayoutMath.profitCardHeight(
                true, true, false, false, false, false));
        assertEquals(134, HudLayoutMath.profitCardHeight(
                false, false, true, false, false, false));
        assertEquals(131, HudLayoutMath.profitCardHeight(
                false, false, false, false, false, true));
    }

    @Test
    void combinedTargetAllocatesRowsForBothMaterialLedgers() {
        assertEquals(216, HudLayoutMath.profitCardHeight(
                true, true, false, false, false, false, 2));
        assertEquals(288, HudLayoutMath.profitCardHeight(
                true, true, true, true, true, true, 2));
    }

    @Test
    void gemstoneTopCardHeightTracksBlocksAndGraphOnly() {
        assertEquals(
                101,
                HudLayoutMath.gemstoneTopCardHeight(false, false));
        assertEquals(
                129,
                HudLayoutMath.gemstoneTopCardHeight(true, false));
        assertEquals(
                140,
                HudLayoutMath.gemstoneTopCardHeight(false, true));
        assertEquals(
                168,
                HudLayoutMath.gemstoneTopCardHeight(true, true));
    }

    @Test
    void gemstoneLedgerAndFullHudHeightsStayInSync() {
        assertEquals(150, HudLayoutMath.gemstoneLedgerCardHeight());
        assertEquals(257, HudLayoutMath.gemstoneHudHeight(false, false));
        assertEquals(285, HudLayoutMath.gemstoneHudHeight(true, false));
        assertEquals(296, HudLayoutMath.gemstoneHudHeight(false, true));
        assertEquals(324, HudLayoutMath.gemstoneHudHeight(true, true));
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
        assertEquals(full - 32, noOther);

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
        assertEquals(14, withHeading - withoutHeading);
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
    void gemstoneHeightAndScaleClampToLogicalScreenBounds() {
        int gemstoneHeight =
                HudLayoutMath.gemstoneHudHeight(true, true);

        assertEquals(
                276.0,
                HudLayoutMath.clampOrigin(
                        500.0,
                        600,
                        gemstoneHeight,
                        1.0F));
        assertEquals(
                114.0,
                HudLayoutMath.clampOrigin(
                        500.0,
                        600,
                        gemstoneHeight,
                        1.5F));
        assertEquals(
                0.0,
                HudLayoutMath.clampOrigin(
                        500.0,
                        400,
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
