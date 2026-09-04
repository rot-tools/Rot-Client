package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientRuntimePolishTest {
    @Test
    void dashboardProductHeaderIsRotClient() throws IOException {
        String ui = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(ui.contains("\"ROT CLIENT\""));
        assertFalse(ui.contains("\"ROT CLIENT · ALPHA\""));
        assertFalse(ui.contains("\"ALPHA · by RotTools\""));
        assertTrue(ui.contains("\"by OgRudolf\""));
        // Product-level header must not identify the app as Mining Tracker.
        assertFalse(ui.contains("graphics.text(font, \"MINING TRACKER\", panelX + 16"));
        assertTrue(ui.contains("\"Modules\""));
        assertTrue(ui.contains("\"HUD Elements Editor\""));
        assertFalse(ui.contains("\"HUD Layout\""));
        assertTrue(ui.contains("VisualsLandingNavPolicy.dismissOnUnhandledClick"));
        assertTrue(ui.contains("goHome()"));
        assertFalse(ui.contains("\"Look & HUD\""));
        assertTrue(ui.contains("\"Mining\""));
        assertTrue(ui.contains("\"Events\""));
        assertTrue(ui.contains("OverviewLandingPolicy"));
        String dashboard = Files.readString(
                Path.of("src/client/java/fi/rotclient/QolUtilityDashboard.java"),
                StandardCharsets.UTF_8);
        int landingMiss = dashboard.indexOf(
                "// Misses must not eat sidebar clicks or hit HUD & Display cards underneath.");
        assertTrue(landingMiss >= 0);
        String missBlock = dashboard.substring(landingMiss, landingMiss + 160);
        assertTrue(missBlock.contains("return false;"));
        assertFalse(missBlock.contains("return true;"));
        assertTrue(ui.contains("DashboardModule.MINING_TRACKER")
                || ui.contains("selectedModule == DashboardModule.MINING_TRACKER"));
        int lookHeader = ui.indexOf("case SECTION_SETTINGS");
        int qolHeader = ui.indexOf("case SECTION_QOL");
        assertTrue(lookHeader >= 0 && qolHeader > lookHeader);
        String lookCase = ui.substring(lookHeader, qolHeader);
        assertTrue(lookCase.contains("toggleSidebarSection"));
        assertFalse(lookCase.contains("openAppearanceCustomizer"));
    }

    @Test
    void aliasDeprecationNoticesAreAliasSpecific() {
        assertEquals(
                "Rot Client: /miningtracker is deprecated; use /rot.",
                RotClientCommandNotices.deprecationNotice("miningtracker"));
        assertEquals(
                "Rot Client: /MiningTracker is deprecated; use /rot.",
                RotClientCommandNotices.deprecationNotice("MiningTracker"));
        assertEquals(
                "Rot Client: /miningui is deprecated; use /rot.",
                RotClientCommandNotices.deprecationNotice("miningui"));
        assertEquals(
                "Rot Client: /rotclient is deprecated; use /rot.",
                RotClientCommandNotices.deprecationNotice("rotclient"));
        assertEquals("", RotClientCommandNotices.deprecationNotice(null));
        assertEquals("", RotClientCommandNotices.deprecationNotice(""));
    }

    @Test
    void readmeDocumentsAuthorAndRepositoryLinks() throws IOException {
        String readme = Files.readString(
                Path.of("README.md"),
                StandardCharsets.UTF_8);
        assertTrue(readme.contains("Rot Tools"));
        assertTrue(readme.contains("https://github.com/rot-tools/Rot-Client"));
        assertTrue(readme.contains(
                "https://github.com/rot-tools/Rot-Client/issues"));
        assertTrue(readme.contains(
                "https://github.com/rot-tools/Rot-Client/releases/tag/playtest"));
        assertTrue(readme.contains("HUD Elements Editor"));
        assertTrue(readme.contains("Visuals"));
        assertTrue(readme.contains("THIRD_PARTY.md"));
        assertTrue(readme.contains("NOTICE"));
        assertFalse(readme.contains("Look & HUD"));
        assertFalse(readme.contains("Rot Client contributors"));
        String workflow = Files.readString(
                Path.of(".github/workflows/build.yml"),
                StandardCharsets.UTF_8);
        assertTrue(workflow.contains("name: RotClient-playable"));
        assertTrue(workflow.contains("gh release create playtest"));
        assertTrue(workflow.contains("refs/heads/development"));
    }

    @Test
    void commandAndDiagnosticLifecycleUseCanonicalSessionBoundaries()
            throws IOException {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        String start = section(source, "private static int sessionStart(",
                "private static int sessionStop(");
        String stop = section(source, "private static int sessionStop(",
                "private static int sessionReset(");
        String status = section(source, "private static int sessionStatus(",
                "private static int sessionCopy(");
        String copy = section(source, "private static int sessionCopy(",
                "private static int sessionSave(");
        String recording = section(source, "private static int startRecording(",
                "private static int stopRecording(");
        String reset = section(source, "resetSessionAnalytics() {",
                "static MiningSessionAnalyticsController.CopyResult");

        assertTrue(start.contains("startSessionAnalytics()"));
        assertFalse(start.contains("SESSION_ANALYTICS.start("));
        assertTrue(stop.contains("stopSessionAnalytics()"));
        assertFalse(stop.contains("SESSION_ANALYTICS.stop("));
        assertTrue(status.contains("CURRENT_SESSION.snapshotConfig()"));
        assertTrue(copy.contains("CURRENT_SESSION.snapshotConfig()"));
        assertTrue(recording.contains("CURRENT_SESSION.isPaused()"));
        assertTrue(reset.contains("clearedCanonical"));
        assertTrue(reset.contains("ResetResult.CLEARED"));
        assertTrue(source.contains("TargetItemGainPipeline.creditMaterial("));
        assertTrue(source.contains("TargetItemGainPipeline.creditGemstone("));
        assertTrue(source.contains(
                "CurrentSessionStartNewCoordinator.startDurably("));
        assertTrue(source.contains(
                "CurrentSessionStartNewCoordinator.recoverPending("));
        assertFalse(source.contains(
                "CurrentSessionStartNewCoordinator.start(\n"
                        + "                        CURRENT_SESSION"));
    }

    @Test
    void worldChangeClearsCanonicalAreaBeforeOfflineResume()
            throws IOException {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        String join = section(
                source,
                "ClientPlayConnectionEvents.JOIN.register",
                "ClientPlayConnectionEvents.DISCONNECT.register");
        assertTrue(join.indexOf("onShadowWorldChanged();")
                < join.indexOf("CURRENT_SESSION.resumeAfterOffline(now)"));

        String worldChanged = section(
                source,
                "private static void onShadowWorldChanged()",
                "private static long lastAreaCheckMillis");
        assertTrue(worldChanged.contains("CURRENT_SESSION.onAreaChanged("));
        assertFalse(worldChanged.contains("CURRENT_SESSION.isActive()"));
    }

    private static String section(
            String source,
            String startMarker,
            String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, () -> "Missing marker: " + startMarker);
        assertTrue(end > start, () -> "Missing marker: " + endMarker);
        return source.substring(start, end);
    }
}
