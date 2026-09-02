package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientAppearanceNavTest {
    @Test
    void defaultsExpandAllKnownGroups() {
        assertEquals(5, RotClientAppearanceNav.defaultExpandedSections().size());
    }

    @Test
    void multiExpandToggleIsIndependent() {
        List<String> after = RotClientAppearanceNav.toggleSection(
                RotClientAppearanceNav.defaultExpandedSections(),
                RotClientAppearanceNav.DASHBOARD_BASICS);
        assertFalse(RotClientAppearanceNav.isExpanded(
                after, RotClientAppearanceNav.DASHBOARD_BASICS));
        assertTrue(RotClientAppearanceNav.isExpanded(
                after, RotClientAppearanceNav.HUD_BASICS));
        assertTrue(RotClientAppearanceNav.isExpanded(
                after, RotClientAppearanceNav.BUTTONS_BORDERS));
    }

    @Test
    void unknownFutureIdsAreIgnored() {
        List<String> normalized = RotClientAppearanceNav.normalizeExpandedSections(
                List.of(
                        RotClientAppearanceNav.HUD_TEXT,
                        "appearance.future_group",
                        "not-a-section"));
        assertEquals(List.of(RotClientAppearanceNav.HUD_TEXT), normalized);
    }

    @Test
    void workspaceRoundTripPreservesAppearanceExpandState() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.expandedAppearanceSections = List.of(
                RotClientAppearanceNav.DASHBOARD_TEXT);
        config.normalize();
        String json = RotClientWorkspaceStore.toJson(config);
        RotClientWorkspaceConfig loaded =
                RotClientWorkspaceStore.parseJson(json);
        assertEquals(
                List.of(RotClientAppearanceNav.DASHBOARD_TEXT),
                loaded.expandedAppearanceSections);
        assertTrue(RotClientAppearanceNav.isExpanded(
                loaded.expandedAppearanceSections,
                RotClientAppearanceNav.DASHBOARD_TEXT));
        assertFalse(RotClientAppearanceNav.isExpanded(
                loaded.expandedAppearanceSections,
                RotClientAppearanceNav.HUD_BASICS));
    }
}
