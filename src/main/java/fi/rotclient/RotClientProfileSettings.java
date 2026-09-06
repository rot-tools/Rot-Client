package fi.rotclient;

import com.google.gson.Gson;

/**
 * Settings owned by a user-created Rot Client profile.
 *
 * This deliberately contains preferences/configuration only. Accumulated
 * tracker statistics, session history, caches and detected runtime state stay
 * outside profiles.
 */
final class RotClientProfileSettings {
    private static final Gson COPY_GSON = new Gson();
    private static final String DEFAULT_SELECTED_TARGET_ID = "GOLD";

    // Standalone tracker / HUD preferences.
    boolean powderChestTrackerEnabled = true;
    boolean miningTrackerEnabled;
    boolean powderChestHudEnabled = true;

    float powderChestHudX = 292.0F;
    float powderChestHudY = 12.0F;
    float powderChestHudScale = 1.0F;

    // Mining Tracker HUD layout.
    float miningHudX = 12.0F;
    float miningHudY = 12.0F;
    float miningHudScale = 1.0F;

    // Mining Tracker HUD visibility.
    boolean showBlocks = true;
    boolean showRawMaterial = true;
    boolean showEnchantedMaterial = true;
    boolean showSessionProfit = true;
    boolean showUnsoldValue = true;
    boolean showCoinsPerHour = true;
    boolean showMaterialPerHour = true;
    boolean showSessionTime = true;
    boolean showActiveTool = true;
    boolean showArea = true;
    boolean showRateGraph = true;
    boolean showDropAndFortune = true;
    boolean showBazaarPrices = true;
    boolean showValuePanel = true;

    boolean showHudTitle = true;
    boolean showHudStatus = true;
    boolean showHudVersion = true;
    boolean showHudAutoPause = true;
    boolean hudShowBackground = true;
    boolean powderChestHudShowBackground = true;

    boolean showTargetHeading = true;
    boolean showOtherSection = true;
    boolean showTargetValue = true;
    boolean showOtherValue = true;
    boolean showTotalMinedValue = true;

    // General runtime feature preferences.
    boolean fullbrightEnabled;
    boolean autoSprintEnabled;
    boolean cameraEnabled;

    // Mining configuration preferences.
    String selectedTargetId = DEFAULT_SELECTED_TARGET_ID;
    double bazaarTaxPercent = 1.25D;
    boolean fortuneAuto = true;

    /*
     * QoL contains module toggles, module-specific settings, keybinds and the
     * movable QoL HUD poses. This makes those settings profile-specific as a
     * single unit rather than maintaining a second parallel list.
     */
    QolUtilityConfig qolUtilities = new QolUtilityConfig();

    static RotClientProfileSettings defaults() {
        RotClientProfileSettings settings =
                new RotClientProfileSettings();

        settings.normalize();
        return settings;
    }

    RotClientProfileSettings copy() {
        /*
         * QolUtilityConfig contains nested lists/maps, so a shallow assignment
         * would make two profiles modify each other. Gson gives us a complete
         * deep copy here. This only runs during profile management, never on
         * the render/tick hot path.
         */
        RotClientProfileSettings copy =
                COPY_GSON.fromJson(
                        COPY_GSON.toJson(this),
                        RotClientProfileSettings.class);

        if (copy == null) {
            return defaults();
        }

        copy.normalize();
        return copy;
    }

    void normalize() {
        miningHudScale =
                finite(miningHudScale)
                        ? clamp(miningHudScale, 0.5F, 2.5F)
                        : 1.0F;

        miningHudX =
                finite(miningHudX)
                        ? Math.max(0.0F, miningHudX)
                        : 12.0F;

        miningHudY =
                finite(miningHudY)
                        ? Math.max(0.0F, miningHudY)
                        : 12.0F;

        powderChestHudScale =
                finite(powderChestHudScale)
                        ? clamp(powderChestHudScale, 0.5F, 2.5F)
                        : 1.0F;

        powderChestHudX =
                finite(powderChestHudX)
                        ? Math.max(0.0F, powderChestHudX)
                        : 292.0F;

        powderChestHudY =
                finite(powderChestHudY)
                        ? Math.max(0.0F, powderChestHudY)
                        : 12.0F;

        bazaarTaxPercent =
                Double.isFinite(bazaarTaxPercent)
                        ? Math.max(
                        0.0D,
                        Math.min(100.0D, bazaarTaxPercent))
                        : 1.25D;

        if (selectedTargetId == null
                || selectedTargetId.isBlank()) {
            selectedTargetId = DEFAULT_SELECTED_TARGET_ID;
        }

        if (qolUtilities == null) {
            qolUtilities = new QolUtilityConfig();
        }

        qolUtilities.normalizeHudPoses();
    }

    private static boolean finite(float value) {
        return Float.isFinite(value);
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum) {

        return Math.max(
                minimum,
                Math.min(maximum, value));
    }
}