package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Named HUD visibility combination cases — height must stay positive,
 * reflow without phantom gaps, and clamp to screen bounds.
 */
final class HudVisibilityCombinationTest {
    private static final int CARD_GAP = 6;

    @Test
    void allOnProducesPositiveStackedHeight() {
        int height = materialHudHeight(
                true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true,
                true, true);
        assertTrue(height > 100);
        assertEquals(0.0, HudLayoutMath.clampOrigin(0, 1080, height, 1.0F));
    }

    @Test
    void minimalTopTitleAndSessionOnly() {
        // Title on, session total on, everything else top off.
        int top = HudLayoutMath.topCardHeight(
                false, false, false, false,
                true, false, false, true, false, false);
        assertTrue(top > 0);
        assertTrue(top < HudLayoutMath.topCardHeight(
                true, true, true, true, true, true, true, true, true, true));
    }

    @Test
    void noGraphShrinksTopCard() {
        int with = HudLayoutMath.topCardHeight(true, true, true, true);
        int without = HudLayoutMath.topCardHeight(true, false, true, true);
        assertEquals(with - 39, without);
    }

    @Test
    void noToolShrinksTopCardWhenItIsTheOnlyChrome() {
        int withToolOnly = HudLayoutMath.topCardHeight(
                false, false, false, false,
                false, false, false, false, false, true);
        int without = HudLayoutMath.topCardHeight(
                false, false, false, false,
                false, false, false, false, false, false);
        assertTrue(withToolOnly > without);
    }

    @Test
    void noFooterChromeStillPositive() {
        int height = HudLayoutMath.topCardHeight(
                true, true, true, true,
                false, false, false, false, false, false);
        assertTrue(height > 0);
    }

    @Test
    void noMarketInfoShrinksProfitCard() {
        int with = HudLayoutMath.profitCardHeight(
                true, true, true, true, true, true);
        int without = HudLayoutMath.profitCardHeight(
                true, true, true, true, true, false);
        assertTrue(with > without);
    }

    @Test
    void valuePanelMinimalStillPositive() {
        assertTrue(HudLayoutMath.hasProfitCard(
                false, false, false, false, false, false,
                true, true, false, false, false));
        int height = HudLayoutMath.profitCardHeight(
                false, false, false, false, false, false, 1,
                true, false, false, false, true);
        assertTrue(height > 0);
    }

    @Test
    void otherOffRemovesOtherBlockSpace() {
        int with = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, true, true, true);
        int without = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                false, true, true, true);
        assertEquals(32, with - without);
    }

    @Test
    void targetHeadingOffRemovesHeaderSpace() {
        int with = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, true, true, true, true);
        int without = HudLayoutMath.profitCardHeight(
                true, false, false, false, false, false, 1,
                true, true, true, true, false);
        assertEquals(14, with - without);
    }

    @Test
    void valuePanelOffRemovesProfitCardEntirely() {
        assertFalse(HudLayoutMath.hasProfitCard(
                true, true, true, true, true, true,
                false, true, true, true, true));
        int top = HudLayoutMath.topCardHeight(true, true, true, true);
        int full = materialHudHeight(
                true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true,
                true, false);
        assertEquals(top, full);
    }

    @Test
    void combinationHeightsClampToViewport() {
        int height = materialHudHeight(
                true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true,
                true, true);
        assertEquals(
                0.0,
                HudLayoutMath.clampOrigin(5000, 400, height, 1.5F));
        assertTrue(HudLayoutMath.clampOrigin(12, 1080, height, 1.0F) >= 0.0);
    }

    @Test
    void areaRowReflowInCombinations() {
        int without = HudLayoutMath.topCardHeight(
                true, true, true, true,
                true, true, true, true, true, true, false);
        int with = HudLayoutMath.topCardHeight(
                true, true, true, true,
                true, true, true, true, true, true, true);
        assertEquals(HudLayoutMath.AREA_ROW_HEIGHT, with - without);
    }

    private static int materialHudHeight(
            boolean showBlocks,
            boolean showRateGraph,
            boolean showMaterialPerHour,
            boolean showDropAndFortune,
            boolean showHudTitle,
            boolean showHudAutoPause,
            boolean showHudVersion,
            boolean showSessionTime,
            boolean showHudStatus,
            boolean showActiveTool,
            boolean showRawMaterial,
            boolean showEnchantedMaterial,
            boolean showSessionProfit,
            boolean showCoinsPerHour,
            boolean showUnsoldValue,
            boolean showBazaarPrices,
            boolean showOtherSection,
            boolean showTargetValue,
            boolean showOtherValue,
            boolean showTotalMinedValue,
            boolean showTargetHeading,
            boolean showValuePanel) {
        int top = HudLayoutMath.topCardHeight(
                showBlocks,
                showRateGraph,
                showMaterialPerHour,
                showDropAndFortune,
                showHudTitle,
                showHudAutoPause,
                showHudVersion,
                showSessionTime,
                showHudStatus,
                showActiveTool);
        boolean profit = HudLayoutMath.hasProfitCard(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices,
                showValuePanel,
                showOtherSection,
                showTargetValue,
                showOtherValue,
                showTotalMinedValue);
        if (!profit) {
            return top;
        }
        int card = HudLayoutMath.profitCardHeight(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices,
                1,
                showOtherSection,
                showTargetValue,
                showOtherValue,
                showTotalMinedValue,
                showTargetHeading);
        return top + CARD_GAP + card;
    }
}
