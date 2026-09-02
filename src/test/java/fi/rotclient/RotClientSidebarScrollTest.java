package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientSidebarScrollTest {
    private static final int ORIGIN = 58;
    private static final int VIEWPORT = 120;

    @Test
    void shortSidebarMaxScrollIsZero() {
        RotClientSidebarNav.Layout layout =
                RotClientSidebarNav.layout(ORIGIN, List.of());
        int content = layout.contentHeight(ORIGIN);
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(content, 400);
        assertEquals(0, scroll.maxScroll());
        assertFalse(scroll.canScroll());
    }

    @Test
    void multiOpenSidebarExceedsSmallViewport() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                ORIGIN, RotClientSidebarNav.defaultExpandedSections());
        int content = layout.contentHeight(ORIGIN);
        assertTrue(content > VIEWPORT);
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(content, VIEWPORT);
        assertTrue(scroll.maxScroll() > 0);
    }

    @Test
    void scrollClampsTopAndBottom() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                ORIGIN, RotClientSidebarNav.defaultExpandedSections());
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(layout.contentHeight(ORIGIN), VIEWPORT);
        scroll.setScrollPixels(-50);
        assertEquals(0, scroll.scrollPixels());
        scroll.setScrollPixels(Integer.MAX_VALUE);
        assertEquals(scroll.maxScroll(), scroll.scrollPixels());
    }

    @Test
    void repeatedWheelCannotExceedBottom() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                ORIGIN, RotClientSidebarNav.defaultExpandedSections());
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(layout.contentHeight(ORIGIN), VIEWPORT);
        for (int i = 0; i < 200; i++) {
            scroll.scrollBySteps(-1.0D, 24);
            scroll.snap();
        }
        assertEquals(scroll.maxScroll(), scroll.scrollPixels());
    }

    @Test
    void collapseReducesContentAndReclamps() {
        List<String> expanded = RotClientSidebarNav.defaultExpandedSections();
        RotClientSidebarNav.Layout open =
                RotClientSidebarNav.layout(ORIGIN, expanded);
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(open.contentHeight(ORIGIN), VIEWPORT);
        scroll.setScrollPixels(scroll.maxScroll());

        List<String> collapsed = RotClientSidebarNav.toggleSection(
                expanded, RotClientSidebarNav.SECTION_SESSIONS);
        collapsed = RotClientSidebarNav.toggleSection(
                collapsed, RotClientSidebarNav.SECTION_SETTINGS);
        RotClientSidebarNav.Layout closed =
                RotClientSidebarNav.layout(ORIGIN, collapsed);
        scroll.setBounds(closed.contentHeight(ORIGIN), VIEWPORT);
        assertTrue(scroll.scrollPixels() <= scroll.maxScroll());
        assertTrue(closed.contentHeight(ORIGIN) < open.contentHeight(ORIGIN));
    }

    @Test
    void resizeReclamps() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                ORIGIN, RotClientSidebarNav.defaultExpandedSections());
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(layout.contentHeight(ORIGIN), 80);
        scroll.setScrollPixels(scroll.maxScroll());
        scroll.setBounds(layout.contentHeight(ORIGIN), 2000);
        assertEquals(0, scroll.maxScroll());
        assertEquals(0, scroll.scrollPixels());
    }

    @Test
    void autoRevealScrollsSelectedChildIntoView() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                ORIGIN, RotClientSidebarNav.defaultExpandedSections());
        int content = layout.contentHeight(ORIGIN);
        int qolTop = layout.qolUtilitiesY() - ORIGIN;
        int qolBottom = qolTop + RotClientSidebarNav.ITEM_HEIGHT;
        int revealed = RotClientSidebarNav.scrollToReveal(
                0, VIEWPORT, content, qolTop, qolBottom);
        assertTrue(revealed > 0);
        assertTrue(qolTop >= revealed);
        assertTrue(qolBottom <= revealed + VIEWPORT);
    }

    @Test
    void scrolledActiveCategoryCanCollapseWithoutRouteChange() {
        // Pure layout: collapsing sessions hides analytics/history Y but does
        // not alter DashboardModule selection (UI concern). Content shrinks.
        List<String> expanded = RotClientSidebarNav.defaultExpandedSections();
        RotClientSidebarNav.Layout before =
                RotClientSidebarNav.layout(ORIGIN, expanded);
        assertTrue(before.analyticsVisible());
        List<String> after = RotClientSidebarNav.toggleSection(
                expanded, RotClientSidebarNav.SECTION_SESSIONS);
        RotClientSidebarNav.Layout layout =
                RotClientSidebarNav.layout(ORIGIN, after);
        assertFalse(layout.analyticsVisible());
        assertTrue(layout.miningExpanded());
    }
}
