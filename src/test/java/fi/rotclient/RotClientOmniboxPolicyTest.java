package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientOmniboxPolicyTest {
    @Test
    void addressShowsRotSchemeWhenIdle() {
        assertEquals(
                "rot://mining-tracker",
                RotClientOmniboxPolicy.addressLabel(
                        RotClientWorkspaceRoute.MINING_TRACKER, "", false));
        assertEquals(
                "auto click",
                RotClientOmniboxPolicy.addressLabel(
                        RotClientWorkspaceRoute.OVERVIEW, "auto click", true));
    }

    @Test
    void suggestionsStayInsideTheModCatalog() {
        List<RotClientSettingsIndex.Entry> results =
                RotClientOmniboxPolicy.suggest("auto sprint");
        assertFalse(results.isEmpty());
        assertEquals("qol.auto_sprint", results.get(0).id());
        assertTrue(RotClientOmniboxPolicy.suggest("https://google.com").isEmpty());
    }

    @Test
    void tokenOrderDoesNotMatter() {
        List<RotClientSettingsIndex.Entry> a =
                RotClientSettingsIndex.search("pause auto");
        List<RotClientSettingsIndex.Entry> b =
                RotClientSettingsIndex.search("auto pause");
        assertFalse(a.isEmpty());
        assertEquals(b.get(0).id(), a.get(0).id());
    }

    @Test
    void alreadyOpenPageSwitchesTab() {
        RotClientSettingsIndex.Entry tracker =
                RotClientSettingsIndex.findById("nav.mining_tracker");
        RotClientOmniboxPolicy.Decision decision = RotClientOmniboxPolicy.decide(
                tracker,
                List.of("overview", "mining_tracker"),
                0,
                false,
                2,
                RotClientWorkspaceConfig.MAX_TABS);
        assertEquals(RotClientOmniboxPolicy.Action.FOCUS_EXISTING_TAB, decision.action());
        assertEquals(1, decision.existingTabIndex());
    }

    @Test
    void modifierOpensNewTabUntilCap() {
        RotClientSettingsIndex.Entry history =
                RotClientSettingsIndex.findById("nav.session_history");
        RotClientOmniboxPolicy.Decision open = RotClientOmniboxPolicy.decide(
                history,
                List.of("overview"),
                0,
                true,
                1,
                RotClientWorkspaceConfig.MAX_TABS);
        assertEquals(RotClientOmniboxPolicy.Action.OPEN_NEW_TAB, open.action());

        RotClientOmniboxPolicy.Decision full = RotClientOmniboxPolicy.decide(
                history,
                List.of("overview"),
                0,
                true,
                RotClientWorkspaceConfig.MAX_TABS,
                RotClientWorkspaceConfig.MAX_TABS);
        assertEquals(RotClientOmniboxPolicy.Action.NAVIGATE_CURRENT, full.action());
    }
}
