package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

final class DashboardModuleTest {
    @Test
    void missingModuleUsesLegacyTrackerPanelState() {
        assertSame(DashboardModule.MINING_TRACKER,
                DashboardModule.fromId(null, true));
        assertSame(DashboardModule.NONE,
                DashboardModule.fromId(null, false));
    }

    @Test
    void idsAreNormalizedAndUnknownModulesCloseThePanel() {
        assertSame(DashboardModule.MINING_TRACKER,
                DashboardModule.fromId(" MINING_TRACKER ", false));
        assertSame(DashboardModule.QOL_SETTINGS,
                DashboardModule.fromId("QOL_SETTINGS", false));
        assertSame(DashboardModule.NONE,
                DashboardModule.fromId("future-module", true));
    }

    @Test
    void earlyFullbrightModuleIdMigratesToQolSettings() {
        assertSame(DashboardModule.QOL_SETTINGS,
                DashboardModule.fromId("fullbright", false));
    }

    @Test
    void sessionAnalyticsModuleIdIsRecognized() {
        assertSame(DashboardModule.SESSION_ANALYTICS,
                DashboardModule.fromId("session_analytics", false));
        assertSame(DashboardModule.SESSION_ANALYTICS,
                DashboardModule.fromId(" SESSION_ANALYTICS ", true));
    }

    @Test
    void sessionHistoryModuleIdIsRecognized() {
        assertSame(DashboardModule.SESSION_HISTORY,
                DashboardModule.fromId("session_history", false));
        assertSame(DashboardModule.SESSION_HISTORY,
                DashboardModule.fromId(" SESSION_HISTORY ", true));
    }

    @Test
    void powderChestTrackerHasItsOwnStableModuleId() {
        assertSame(DashboardModule.POWDER_CHEST_TRACKER,
                DashboardModule.fromId("powder_chest_tracker", false));
    }
}
