package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientSidebarNavTest {
    @Test
    void defaultExpandedHasFourSections() {
        assertEquals(4, RotClientSidebarNav.defaultExpandedSections().size());
        assertTrue(RotClientSidebarNav.isExpanded(
                RotClientSidebarNav.defaultExpandedSections(),
                RotClientSidebarNav.SECTION_QOL));
    }

    @Test
    void emptyListMeansFullyCollapsed() {
        List<String> normalized = RotClientSidebarNav.normalizeExpandedSections(List.of());
        assertTrue(normalized.isEmpty());
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(58, normalized);
        assertFalse(layout.analyticsVisible());
        assertFalse(layout.qolChildrenVisible());
    }

    @Test
    void unknownIdsFallBackToDefaults() {
        List<String> normalized = RotClientSidebarNav.normalizeExpandedSections(
                List.of("not-a-section"));
        assertEquals(RotClientSidebarNav.defaultExpandedSections(), normalized);
    }

    @Test
    void toggleRemovesAndAddsSection() {
        List<String> expanded = RotClientSidebarNav.defaultExpandedSections();
        List<String> closed = RotClientSidebarNav.toggleSection(
                expanded, RotClientSidebarNav.SECTION_SESSIONS);
        assertFalse(RotClientSidebarNav.isExpanded(
                closed, RotClientSidebarNav.SECTION_SESSIONS));
        List<String> open = RotClientSidebarNav.toggleSection(
                closed, RotClientSidebarNav.SECTION_SESSIONS);
        assertTrue(RotClientSidebarNav.isExpanded(
                open, RotClientSidebarNav.SECTION_SESSIONS));
    }

    @Test
    void animatedOpenAmountReservesSpaceBetweenCollapsedAndFull() {
        RotClientSidebarNav.Layout closed = RotClientSidebarNav.layout(58, List.of());
        RotClientSidebarNav.Layout open = RotClientSidebarNav.layout(
                58, RotClientSidebarNav.defaultExpandedSections());
        RotClientSidebarNav.Layout half = RotClientSidebarNav.layout(
                58,
                RotClientSidebarNav.defaultExpandedSections(),
                id -> 0.5D);
        int closedH = closed.contentHeight(58);
        int openH = open.contentHeight(58);
        int halfH = half.contentHeight(58);
        assertTrue(halfH > closedH);
        assertTrue(halfH < openH);
        assertTrue(half.qolChildrenVisible());
        assertFalse(closed.qolChildrenVisible());
        assertTrue(
                RotClientSidebarNav.hitTest(
                        closed, 20, closed.trackerY() + 8, 8, 180, 8, 180)
                        != RotClientSidebarNav.HitTarget.TRACKER);
        assertEquals(
                RotClientSidebarNav.HitTarget.TRACKER,
                RotClientSidebarNav.hitTest(
                        open, 20, open.trackerY() + 8, 8, 180, 8, 180));
    }

    @Test
    void qolPagesMapToHitTargets() {
        assertEquals(
                QolUtilityCatalog.Group.DUNGEONS,
                RotClientSidebarNav.groupForHitTarget(
                        RotClientSidebarNav.HitTarget.QOL_DUNGEONS));
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_UTILITIES,
                RotClientSidebarNav.hitTargetForQolGroup(
                        QolUtilityCatalog.Group.UTILITIES));
        assertEquals(
                RotClientSidebarNav.SECTION_MINING,
                RotClientSidebarNav.sectionForModule(DashboardModule.MINING_TRACKER));
    }
}
