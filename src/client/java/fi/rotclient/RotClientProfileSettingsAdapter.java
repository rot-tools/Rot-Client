package fi.rotclient;

import com.google.gson.Gson;

/**
 * Maps profile-owned preferences between the existing TrackerConfig and a
 * RotClientProfileSettings snapshot.
 *
 * This class deliberately does not touch accumulated tracker statistics,
 * session state, caches, detected fortune values or dashboard/workspace state.
 *
 * Applying settings only updates configuration fields. Runtime side effects
 * caused by switching certain features are reconciled separately by the
 * profile-switching layer.
 */
final class RotClientProfileSettingsAdapter {
    private static final Gson COPY_GSON = new Gson();

    private RotClientProfileSettingsAdapter() {
    }

    /**
     * Captures the profile-owned portion of the current Rot Client settings.
     */
    static RotClientProfileSettings capture(TrackerConfig config) {
        if (config == null) {
            return RotClientProfileSettings.defaults();
        }

        RotClientProfileSettings settings =
                RotClientProfileSettings.defaults();

        settings.powderChestTrackerEnabled =
                config.powderChestTrackerEnabled;

        settings.miningTrackerEnabled =
                config.enabled;

        settings.powderChestHudEnabled =
                config.powderChestHudEnabled;

        settings.powderChestHudX =
                config.powderChestHudX;

        settings.powderChestHudY =
                config.powderChestHudY;

        settings.powderChestHudScale =
                config.powderChestHudScale;

        settings.miningHudX =
                config.x;

        settings.miningHudY =
                config.y;

        settings.miningHudScale =
                config.scale;

        settings.showBlocks =
                config.showBlocks;

        settings.showRawMaterial =
                config.showRawMaterial;

        settings.showEnchantedMaterial =
                config.showEnchantedMaterial;

        settings.showSessionProfit =
                config.showSessionProfit;

        settings.showUnsoldValue =
                config.showUnsoldValue;

        settings.showCoinsPerHour =
                config.showCoinsPerHour;

        settings.showMaterialPerHour =
                config.showMaterialPerHour;

        settings.showSessionTime =
                config.showSessionTime;

        settings.showActiveTool =
                config.showActiveTool;

        settings.showArea =
                config.showArea;

        settings.showRateGraph =
                config.showRateGraph;

        settings.showDropAndFortune =
                config.showDropAndFortune;

        settings.showBazaarPrices =
                config.showBazaarPrices;

        settings.showValuePanel =
                config.showValuePanel;

        settings.showHudTitle =
                config.showHudTitle;

        settings.showHudStatus =
                config.showHudStatus;

        settings.showHudVersion =
                config.showHudVersion;

        settings.showHudAutoPause =
                config.showHudAutoPause;

        settings.hudShowBackground =
                config.hudShowBackground;

        settings.powderChestHudShowBackground =
                config.powderChestHudShowBackground;

        settings.showTargetHeading =
                config.showTargetHeading;

        settings.showOtherSection =
                config.showOtherSection;

        settings.showTargetValue =
                config.showTargetValue;

        settings.showOtherValue =
                config.showOtherValue;

        settings.showTotalMinedValue =
                config.showTotalMinedValue;

        settings.fullbrightEnabled =
                config.fullbrightEnabled;

        settings.autoSprintEnabled =
                config.autoSprintEnabled;

        settings.cameraEnabled =
                config.cameraEnabled;

        settings.selectedTargetId =
                config.selectedSelection().id();

        settings.bazaarTaxPercent =
                config.bazaarTaxPercent;

        settings.fortuneAuto =
                config.fortuneAuto;

        settings.qolUtilities =
                copyQol(config.qolUtilities);

        settings.normalize();

        return settings;
    }

    /**
     * Applies the profile-owned settings to the existing TrackerConfig.
     *
     * Global statistics and runtime/domain state are intentionally left alone.
     */
    static void apply(
            RotClientProfileSettings settings,
            TrackerConfig config) {

        if (config == null) {
            return;
        }

        RotClientProfileSettings safe =
                settings == null
                        ? RotClientProfileSettings.defaults()
                        : settings.copy();

        safe.normalize();

        config.powderChestTrackerEnabled =
                safe.powderChestTrackerEnabled;

        config.powderChestHudEnabled =
                safe.powderChestHudEnabled;

        config.powderChestHudX =
                safe.powderChestHudX;

        config.powderChestHudY =
                safe.powderChestHudY;

        config.powderChestHudScale =
                safe.powderChestHudScale;

        config.x =
                safe.miningHudX;

        config.y =
                safe.miningHudY;

        config.scale =
                safe.miningHudScale;

        config.showBlocks =
                safe.showBlocks;

        config.showRawMaterial =
                safe.showRawMaterial;

        config.showEnchantedMaterial =
                safe.showEnchantedMaterial;

        config.showSessionProfit =
                safe.showSessionProfit;

        config.showUnsoldValue =
                safe.showUnsoldValue;

        config.showCoinsPerHour =
                safe.showCoinsPerHour;

        config.showMaterialPerHour =
                safe.showMaterialPerHour;

        config.showSessionTime =
                safe.showSessionTime;

        config.showActiveTool =
                safe.showActiveTool;

        config.showArea =
                safe.showArea;

        config.showRateGraph =
                safe.showRateGraph;

        config.showDropAndFortune =
                safe.showDropAndFortune;

        config.showBazaarPrices =
                safe.showBazaarPrices;

        config.showValuePanel =
                safe.showValuePanel;

        config.showHudTitle =
                safe.showHudTitle;

        config.showHudStatus =
                safe.showHudStatus;

        config.showHudVersion =
                safe.showHudVersion;

        config.showHudAutoPause =
                safe.showHudAutoPause;

        config.hudShowBackground =
                safe.hudShowBackground;

        config.powderChestHudShowBackground =
                safe.powderChestHudShowBackground;

        config.showTargetHeading =
                safe.showTargetHeading;

        config.showOtherSection =
                safe.showOtherSection;

        config.showTargetValue =
                safe.showTargetValue;

        config.showOtherValue =
                safe.showOtherValue;

        config.showTotalMinedValue =
                safe.showTotalMinedValue;

        config.fullbrightEnabled =
                safe.fullbrightEnabled;

        config.autoSprintEnabled =
                safe.autoSprintEnabled;

        config.cameraEnabled =
                safe.cameraEnabled;

        /*
         * TrackerSelection performs validation for us. Unknown or corrupted
         * persisted IDs safely fall back to GOLD.
         */
        config.setSelectedSelection(
                TrackerSelection.fromId(
                        safe.selectedTargetId));

        config.bazaarTaxPercent =
                safe.bazaarTaxPercent;

        config.fortuneAuto =
                safe.fortuneAuto;

        config.qolUtilities =
                copyQol(safe.qolUtilities);

        config.normalize();
    }

    private static QolUtilityConfig copyQol(
            QolUtilityConfig source) {

        if (source == null) {
            return new QolUtilityConfig();
        }

        QolUtilityConfig copy =
                COPY_GSON.fromJson(
                        COPY_GSON.toJson(source),
                        QolUtilityConfig.class);

        if (copy == null) {
            return new QolUtilityConfig();
        }

        copy.normalizeHudPoses();

        return copy;
    }
}