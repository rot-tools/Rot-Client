package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class QolUtilityUiMathTest {
    @Test
    void statusBadgeIncludesReadableHorizontalPadding() {
        assertEquals(
                52,
                QolUtilityUiMath.statusBadgeWidth(
                        52 - QolUtilityUiMath.STATUS_BADGE_PAD_X * 2));
        assertTrue(QolUtilityUiMath.STATUS_BADGE_HEIGHT > 0);
        assertTrue(QolUtilityUiMath.STATUS_BADGE_TOP >= 0);
    }

    @Test
    void moduleCardUsesExplicitFooterControls() {
        assertEquals(
                QolUtilityUiMath.CardAction.OPEN_SETTINGS,
                QolUtilityUiMath.moduleCardAction(
                        true, false, false, false, false, true, true, false, false));
        assertEquals(
                QolUtilityUiMath.CardAction.TOGGLE,
                QolUtilityUiMath.moduleCardAction(
                        true, false, true, false, false, true, true, false, false));
        assertEquals(
                QolUtilityUiMath.CardAction.OPEN_SETTINGS,
                QolUtilityUiMath.moduleCardAction(
                        false, true, false, false, false, true, true, false, false));
        assertEquals(
                QolUtilityUiMath.CardAction.OPEN_HUD_SETTINGS,
                QolUtilityUiMath.moduleCardAction(
                        true, false, false, true, false, true, true, true, false));
        assertEquals(
                QolUtilityUiMath.CardAction.OPEN_HUD_SETTINGS,
                QolUtilityUiMath.moduleCardAction(
                        true, false, false, true, false, true, true, true, true));
    }

    @Test
    void drawerWidthStaysInTargetBand() {
        int wide = QolUtilityUiMath.drawerWidth(700);
        assertTrue(wide >= QolUtilityUiMath.DRAWER_MIN_WIDTH);
        assertTrue(wide <= QolUtilityUiMath.DRAWER_WIDTH);
        assertFalse(QolUtilityUiMath.drawerIsOverlay(700));
        assertTrue(QolUtilityUiMath.drawerIsOverlay(400));
    }

    @Test
    void wideModuleListUsesTwoReadableColumns() {
        assertEquals(2, QolUtilityUiMath.gridColumns(870));
        assertEquals(428, QolUtilityUiMath.cardWidth(870));
        assertEquals(442, QolUtilityUiMath.cardX(1, 0, 870));
        assertEquals(2, QolUtilityUiMath.cardRow(4, 2));
    }

    @Test
    void narrowModuleListFallsBackToOneColumn() {
        assertEquals(1, QolUtilityUiMath.gridColumns(620));
        assertEquals(620, QolUtilityUiMath.cardWidth(620));
        assertEquals(3, QolUtilityUiMath.cardRow(3, 1));
    }

    @Test
    void gridHeightCountsRowsRatherThanCards() {
        assertEquals(
                QolUtilityUiMath.CARD_HEIGHT * 2 + QolUtilityUiMath.CARD_GAP,
                QolUtilityUiMath.measureGridHeight(4, 2));
        assertEquals(0, QolUtilityUiMath.measureGridHeight(0, 2));
    }

    @Test
    void numericStepperHasSeparateDecreaseAndIncreaseTargets() {
        assertTrue(QolUtilityUiMath.hitNumberDecrease(215, 10, 300));
        assertTrue(QolUtilityUiMath.hitNumberIncrease(295, 10, 300));
        assertFalse(QolUtilityUiMath.hitNumberDecrease(295, 10, 300));
        assertFalse(QolUtilityUiMath.hitNumberIncrease(215, 10, 300));
    }

    @Test
    void sliderMapsMouseXToZeroOneFraction() {
        assertEquals(0.0D, QolUtilityUiMath.sliderFraction(18, 10, 300), 0.0001D);
        assertEquals(1.0D, QolUtilityUiMath.sliderFraction(302, 10, 300), 0.0001D);
        assertTrue(QolUtilityUiMath.hitSlider(20, 12, 10, 10, 300));
        assertFalse(QolUtilityUiMath.hitSlider(5, 12, 10, 10, 300));
    }

    @Test
    void enumOptionsReserveOneReadableRowPerChoice() {
        assertEquals(78, QolUtilityUiMath.measureEnumOptionsHeight(3));
        assertEquals(0, QolUtilityUiMath.measureEnumOptionsHeight(0));
    }

    @Test
    void settingsButtonHitboxIsOnCardFooter() {
        int h = QolUtilityUiMath.CARD_HEIGHT;
        assertTrue(QolUtilityUiMath.hitSettingsButton(228, 92, 10, 10, 300, h));
        assertFalse(QolUtilityUiMath.hitSettingsButton(20, 30, 10, 10, 300, h));
        assertTrue(QolUtilityUiMath.hitSettingsIcon(228, 92, 10, 10, 300, h));
    }

    @Test
    void moduleAndHudControlsSitOnTheFooter() {
        int h = QolUtilityUiMath.CARD_HEIGHT;
        assertTrue(QolUtilityUiMath.hitModuleToggle(34, 91, 10, 10, h));
        assertTrue(QolUtilityUiMath.hitToggle(34, 91, 10, 10, 300));
        assertFalse(QolUtilityUiMath.hitToggle(228, 92, 10, 10, 300));
        assertTrue(QolUtilityUiMath.hitHudControl(126, 91, 10, 10, h));
        assertTrue(QolUtilityUiMath.hitHudMenuEdit(
                QolUtilityUiMath.hudMenuX(10) + QolUtilityUiMath.HUD_MENU_WIDTH - 20,
                QolUtilityUiMath.hudMenuY(10, h) + 10,
                QolUtilityUiMath.hudMenuX(10),
                QolUtilityUiMath.hudMenuY(10, h),
                0));
    }

    @Test
    void listWidthShrinksWhenDrawerOpenOnWideLayout() {
        int list = QolUtilityUiMath.listWidth(800, true);
        assertTrue(list < 800);
        assertEquals(800, QolUtilityUiMath.listWidth(800, false));
    }

    @Test
    void pageLayoutInsertsSectionHeadersAndResetsColumnsPerSection() {
        QolUtilityCatalog.ModuleDef first = QolUtilityCatalog.findById("qol.slayer_display");
        QolUtilityCatalog.ModuleDef second = QolUtilityCatalog.findById("qol.slayer_vengeance");
        QolUtilityUiMath.PageLayout layout = QolUtilityUiMath.layoutPage(
                List.of(first, second), 10, 870);
        assertEquals(2, layout.headers().size());
        assertEquals("HUD", layout.headers().get(0).title());
        assertEquals("Blaze", layout.headers().get(1).title());
        assertEquals(2, layout.cards().size());
        assertTrue(layout.cards().get(1).y() > layout.cards().get(0).y());
        assertTrue(layout.height() > QolUtilityUiMath.CARD_HEIGHT);
    }

    @Test
    void pageFilterKeepsEnabledAndCheatModules() {
        QolUtilityCatalog.ModuleDef clicker = QolUtilityCatalog.findById("qol.auto_clicker");
        QolUtilityCatalog.ModuleDef share = QolUtilityCatalog.findById("qol.diana_share");
        List<QolUtilityCatalog.ModuleDef> modules = List.of(clicker, share);
        assertEquals(
                2,
                QolUtilityUiMath.filterPageModules(
                        modules, QolUtilityUiMath.PageFilter.ALL, ignored -> true).size());
        assertEquals(
                "qol.auto_clicker",
                QolUtilityUiMath.filterPageModules(
                                modules,
                                QolUtilityUiMath.PageFilter.ENABLED,
                                module -> module.id().equals("qol.auto_clicker"))
                        .get(0)
                        .id());
        assertEquals(
                List.of(clicker, share),
                QolUtilityUiMath.filterPageModules(
                        modules, QolUtilityUiMath.PageFilter.CHEAT, ignored -> false));
        assertEquals(
                QolUtilityUiMath.PageFilter.ALL,
                QolUtilityUiMath.hitPageFilter(20, 55, 8, 4));
        assertEquals(
                QolUtilityUiMath.PageFilter.CHEAT,
                QolUtilityUiMath.hitPageFilter(170, 55, 8, 4));
        assertEquals("Cheats", QolUtilityUiMath.cheatFilterLabel(0));
        assertEquals("Cheats [8]", QolUtilityUiMath.cheatFilterLabel(8));
        assertEquals("Cheats", QolUtilityUiMath.pageFilterChips(8, 4).get(2).label());
        assertEquals(
                QolUtilityUiMath.CHEAT_FILTER_CHIP_WIDTH,
                QolUtilityUiMath.pageFilterChips(8, 4).get(2).width());
        assertEquals(QolUtilityUiMath.PAGE_HEADER_HEIGHT, 80);
    }
}
