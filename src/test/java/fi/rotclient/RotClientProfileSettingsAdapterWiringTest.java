package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientProfileSettingsAdapterWiringTest {

    private static final Path ADAPTER = Path.of(
            "src/client/java/fi/rotclient/"
                    + "RotClientProfileSettingsAdapter.java");

    @Test
    void adapterSupportsCaptureAndApply() throws Exception {
        String source =
                Files.readString(
                        ADAPTER,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "static RotClientProfileSettings capture("));

        assertTrue(source.contains(
                "static void apply("));

        assertTrue(source.contains(
                "TrackerSelection.fromId("));
    }

    @Test
    void miningTrackerEnabledIsCapturedButNotDirectlyApplied()
            throws Exception {

        String source =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/"
                                        + "RotClientProfileSettingsAdapter.java"),
                        StandardCharsets.UTF_8);

        assertTrue(
                source.contains(
                        "settings.miningTrackerEnabled"));

        assertTrue(
                source.contains(
                        "config.enabled"));

        assertFalse(
                source.contains(
                        "config.enabled = settings.miningTrackerEnabled"));
    }


    @Test
    void adapterTransfersProfileOwnedHudSettings() throws Exception {
        String source =
                Files.readString(
                        ADAPTER,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "settings.miningHudX"));

        assertTrue(source.contains(
                "settings.miningHudY"));

        assertTrue(source.contains(
                "settings.miningHudScale"));

        assertTrue(source.contains(
                "settings.powderChestHudX"));

        assertTrue(source.contains(
                "settings.powderChestHudY"));

        assertTrue(source.contains(
                "settings.powderChestHudScale"));

        assertTrue(source.contains(
                "settings.qolUtilities"));

        assertTrue(source.contains(
                "config.qolUtilities"));
    }

    @Test
    void adapterDoesNotTransferPersistentTrackerState()
            throws Exception {

        String source =
                Files.readString(
                        ADAPTER,
                        StandardCharsets.UTF_8);

        assertFalse(source.contains(
                "materialStates"));

        assertFalse(source.contains(
                "gemstoneStates"));

        assertFalse(source.contains(
                "combinedTotalActiveMillis"));

        assertFalse(source.contains(
                "combinedSessionActiveMillis"));

        assertFalse(source.contains(
                "combinedLastBreakEpochMillis"));

        assertFalse(source.contains(
                "fortuneLastDetectedEpochMillis"));

        assertFalse(source.contains(
                "fortuneSource"));

        assertFalse(source.contains(
                "miningTrackerPanelOpen"));

        assertFalse(source.contains(
                "selectedDashboardModuleId"));
    }

    @Test
    void adapterDeepCopiesQolConfiguration()
            throws Exception {

        String source =
                Files.readString(
                        ADAPTER,
                        StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "copyQol(config.qolUtilities)"));

        assertTrue(source.contains(
                "copyQol(safe.qolUtilities)"));

        assertTrue(source.contains(
                "COPY_GSON.toJson(source)"));

        assertTrue(source.contains(
                "QolUtilityConfig.class"));
    }
}