package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrackerConfigHudVisibilityTest {
    @Test
    void resetHudVisibilityRestoresAllShowFlagsToDefaults() {
        TrackerConfig config = new TrackerConfig();
        config.showBlocks = false;
        config.showRawMaterial = false;
        config.showEnchantedMaterial = false;
        config.showSessionProfit = false;
        config.showUnsoldValue = false;
        config.showCoinsPerHour = false;
        config.showMaterialPerHour = false;
        config.showSessionTime = false;
        config.showActiveTool = false;
        config.showArea = false;
        config.showRateGraph = false;
        config.showDropAndFortune = false;
        config.showBazaarPrices = false;
        config.showValuePanel = false;
        config.showHudTitle = false;
        config.showHudStatus = false;
        config.showHudVersion = false;
        config.showHudAutoPause = false;
        config.hudShowBackground = false;
        config.powderChestHudShowBackground = false;
        config.showTargetHeading = false;
        config.showOtherSection = false;
        config.showTargetValue = false;
        config.showOtherValue = false;
        config.showTotalMinedValue = false;
        config.x = 99.0F;
        config.enabled = true;
        config.bazaarTaxPercent = 5.0;

        config.resetHudVisibility();

        assertTrue(config.showBlocks);
        assertTrue(config.showRawMaterial);
        assertTrue(config.showEnchantedMaterial);
        assertTrue(config.showSessionProfit);
        assertTrue(config.showUnsoldValue);
        assertTrue(config.showCoinsPerHour);
        assertTrue(config.showMaterialPerHour);
        assertTrue(config.showSessionTime);
        assertTrue(config.showActiveTool);
        assertTrue(config.showArea);
        assertTrue(config.showRateGraph);
        assertTrue(config.showDropAndFortune);
        assertTrue(config.showBazaarPrices);
        assertTrue(config.showValuePanel);
        assertTrue(config.showHudTitle);
        assertTrue(config.showHudStatus);
        assertTrue(config.showHudVersion);
        assertTrue(config.showHudAutoPause);
        assertTrue(config.hudShowBackground);
        assertTrue(config.powderChestHudShowBackground);
        assertTrue(config.showTargetHeading);
        assertTrue(config.showOtherSection);
        assertTrue(config.showTargetValue);
        assertTrue(config.showOtherValue);
        assertTrue(config.showTotalMinedValue);
        assertTrue(config.enabled);
        assertTrue(Math.abs(config.x - 99.0F) < 0.001F);
        assertTrue(Math.abs(config.bazaarTaxPercent - 5.0) < 0.001);
    }

    @Test
    void miningUiScreenExposesValuePanelAndOtherSectionToggles() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("VALUE PANEL"));
        assertTrue(source.contains("OTHERS SECTION"));
        assertTrue(source.contains("AREA / LOCATION"));
        assertTrue(source.contains("OTHERS VALUE"));
        assertTrue(source.contains("showValuePanel"));
        assertTrue(source.contains("showOtherSection"));
        assertTrue(source.contains("showArea"));
        assertTrue(source.contains("showOtherValue"));
        assertTrue(source.contains("settings.size() + \" OPTIONS\""));
        assertFalse(source.contains("\"12 OPTIONS\""));
    }

    @Test
    void appearanceResetWiresHudVisibilityReset() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientAppearanceScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("Reset HUD Visibility"));
        assertTrue(source.contains("resetHudVisibility"));
        assertTrue(source.contains("awaitingHudVisibilityResetConfirm"));
    }
}
