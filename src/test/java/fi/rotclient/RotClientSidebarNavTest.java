package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientSidebarNavTest {
    @Test
    void defaultExpandedHasLookAndModules() {
        assertEquals(2, RotClientSidebarNav.defaultExpandedSections().size());
        assertTrue(RotClientSidebarNav.isExpanded(
                RotClientSidebarNav.defaultExpandedSections(),
                RotClientSidebarNav.SECTION_QOL));
        assertTrue(RotClientSidebarNav.isExpanded(
                RotClientSidebarNav.defaultExpandedSections(),
                RotClientSidebarNav.SECTION_SETTINGS));
        assertFalse(RotClientSidebarNav.knownSections().contains(
                RotClientSidebarNav.SECTION_MINING));
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
        int miningY = open.qolPageY(QolUtilityCatalog.Group.MINING);
        assertTrue(miningY > open.qolHeaderY());
        assertTrue(
                RotClientSidebarNav.hitTest(
                        closed, 20, miningY + 8, 8, 180, 8, 180)
                        != RotClientSidebarNav.HitTarget.QOL_MINING);
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_MINING,
                RotClientSidebarNav.hitTest(
                        open, 20, miningY + 8, 8, 180, 8, 180));
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
                RotClientSidebarNav.HitTarget.QOL_EVENTS,
                RotClientSidebarNav.hitTargetForQolGroup(
                        QolUtilityCatalog.Group.EVENTS));
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_KUUDRA,
                RotClientSidebarNav.hitTargetForQolGroup(
                        QolUtilityCatalog.Group.KUUDRA));
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_GARDEN,
                RotClientSidebarNav.hitTargetForQolGroup(
                        QolUtilityCatalog.Group.GARDEN));
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_GUI,
                RotClientSidebarNav.hitTargetForQolGroup(
                        QolUtilityCatalog.Group.GUI));
        assertEquals(
                RotClientSidebarNav.SECTION_QOL,
                RotClientSidebarNav.sectionForModule(DashboardModule.MINING_TRACKER));
        assertEquals(
                RotClientSidebarNav.SECTION_QOL,
                RotClientSidebarNav.sectionForModule(DashboardModule.SESSION_ANALYTICS));
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_MINING,
                RotClientSidebarNav.hitTargetForModule(DashboardModule.MINING_TRACKER));
    }

    @Test
    void visualsChildrenHitWhileModulesCollapsed() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                58, List.of(RotClientSidebarNav.SECTION_SETTINGS));
        assertTrue(layout.hudLayoutVisible());
        assertTrue(layout.profilesVisible());
        assertFalse(layout.qolChildrenVisible());
        assertEquals(
                RotClientSidebarNav.HitTarget.HUD_LAYOUT,
                RotClientSidebarNav.hitTest(
                        layout, 20, layout.hudLayoutY() + 8, 8, 180, 8, 180));
        assertEquals(
                RotClientSidebarNav.HitTarget.APPEARANCE,
                RotClientSidebarNav.hitTest(
                        layout, 20, layout.appearanceY() + 8, 8, 180, 8, 180));
        assertEquals(
                RotClientSidebarNav.HitTarget.PROFILES,
                RotClientSidebarNav.hitTest(
                        layout, 20, layout.profilesY() + 8, 8, 180, 8, 180));
        assertEquals(
                RotClientSidebarNav.HitTarget.SECTION_QOL,
                RotClientSidebarNav.hitTest(
                        layout, 20, layout.qolHeaderY() + 1, 8, 180, 8, 180));
        assertTrue(layout.hudLayoutY() + RotClientSidebarNav.ITEM_HEIGHT
                <= layout.profilesY());
        assertTrue(layout.profilesY() + RotClientSidebarNav.ITEM_HEIGHT
                <= layout.qolHeaderY());
    }

    @Test
    void miningIsNotATopLevelSidebarSection() {
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                58, RotClientSidebarNav.defaultExpandedSections());
        assertTrue(layout.miningHeaderY() < 0);
        assertFalse(layout.trackerVisible());
        assertFalse(layout.analyticsVisible());
        assertEquals(
                RotClientSidebarNav.SECTION_QOL,
                RotClientSidebarNav.normalizeSectionId(RotClientSidebarNav.SECTION_MINING));
        assertEquals(
                RotClientSidebarNav.SECTION_QOL,
                RotClientSidebarNav.normalizeSectionId(RotClientSidebarNav.SECTION_SESSIONS));
        assertEquals(
                RotClientSidebarNav.HitTarget.QOL_MINING,
                RotClientSidebarNav.hitTest(
                        layout, 20, layout.qolPageY(QolUtilityCatalog.Group.MINING) + 8,
                        8, 180, 8, 180));
        assertEquals(
                RotClientSidebarNav.HitTarget.SECTION_QOL,
                RotClientSidebarNav.hitTest(
                        layout, 20, layout.qolHeaderY() + 1, 8, 180, 8, 180));
    }
}
