package fi.rotclient;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TrackerConfig {
    static final int CURRENT_DATA_VERSION = 10;

    int dataVersion = CURRENT_DATA_VERSION;
    boolean enabled;
    /** Standalone Powder Chest Tracker; independent of ore target selection. */
    boolean powderChestTrackerEnabled = true;
    /** Independent in-game HUD for the standalone Powder Chest Tracker. */
    boolean powderChestHudEnabled = true;
    float powderChestHudX = 292.0F;
    float powderChestHudY = 12.0F;
    float powderChestHudScale = 1.0F;
    boolean showBlocks = true;
    @SerializedName(value = "showRawMaterial", alternate = "showRawGold")
    boolean showRawMaterial = true;
    @SerializedName(value = "showEnchantedMaterial", alternate = "showEnchantedGold")
    boolean showEnchantedMaterial = true;
    boolean showSessionProfit = true;
    boolean showUnsoldValue = true;
    boolean showCoinsPerHour = true;
    @SerializedName(value = "showMaterialPerHour", alternate = "showGoldPerHour")
    boolean showMaterialPerHour = true;
    boolean showSessionTime = true;
    boolean showActiveTool = true;
    /** Live SkyBlock parent / sub-area row on the Mining Tracker HUD. */
    boolean showArea = true;
    boolean showRateGraph = true;
    boolean showDropAndFortune = true;
    boolean showBazaarPrices = true;
    /** Master toggle for the Mining HUD profit / session-value card. */
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
    boolean fullbrightEnabled;
    /** Vanilla-style always-night sky and terrain lighting. */
    boolean alwaysNightEnabled;
    /**
     * When true, Fullbright and Always Night may both be on. When false they
     * are exclusive.
     */
    boolean lightingForceBoth;
    /**
     * When enabled, forward movement keeps the local player sprinting via
     * normal vanilla sprint rules (Input.sprint override).
     */
    boolean autoSprintEnabled;
    /**
     * When enabled, perspective cycles between first person and rear third
     * person while front-facing third person is skipped.
     */
    boolean cameraEnabled;
    /**
     * Persisted QoL utility-suite toggles and drawer settings.
     */
    QolUtilityConfig qolUtilities = new QolUtilityConfig();
    /** Persisted JSON key retained for MiningTracker → Rot Client migration. */
    boolean miningTrackerPanelOpen;
    String selectedDashboardModuleId;
    float x = 12.0F;
    float y = 12.0F;
    float scale = 1.0F;
    double bazaarTaxPercent = 1.25;
    double miningFortune;
    double blockFortune;
    double oreFortune;
    double dwarvenMetalFortune;
    double gemstoneFortune;
    boolean fortuneAuto = true;
    long fortuneLastDetectedEpochMillis;
    String fortuneSource = "not detected";
    long combinedTotalActiveMillis;
    long combinedSessionActiveMillis;
    long combinedLastBreakEpochMillis;

    @SerializedName(
            value = "selectedTargetId",
            alternate = "selectedMaterialId")
    String selectedTargetId = TrackingTarget.GOLD.id();
    private Map<String, MaterialTrackerState> materialStates = new LinkedHashMap<>();
    private Map<String, GemstoneTrackerState> gemstoneStates =
            new LinkedHashMap<>();

    TrackerConfig() {
        ensureMaterialStates();
        ensureGemstoneStates();
    }

    TrackerSelection selectedSelection() {
        return TrackerSelection.fromId(selectedTargetId);
    }

    GemstoneType selectedGemstone() {
        TrackerSelection selection = selectedSelection();
        return selection.isGemstone() ? selection.gemstone() : null;
    }

    void setSelectedSelection(TrackerSelection selection) {
        TrackerSelection safeSelection =
                selection == null ? TrackerSelection.GOLD : selection;
        selectedTargetId = safeSelection.id();
    }

    TrackingTarget selectedTarget() {
        TrackerSelection selection = selectedSelection();
        return selection.isMaterial()
                ? selection.materialTarget()
                : TrackingTarget.GOLD;
    }

    TrackedMaterial selectedMaterial() {
        return selectedTarget().primaryMaterial();
    }

    boolean hasMaterialSelection() {
        return selectedSelection().isMaterial();
    }

    TrackingTarget requireMaterialTarget() {
        TrackerSelection selection = selectedSelection();
        if (!selection.isMaterial()) {
            throw new IllegalStateException(
                    "Selected tracker is not a material target: "
                            + selection.id());
        }
        return selection.materialTarget();
    }

    TrackedMaterial requireSelectedMaterial() {
        return requireMaterialTarget().primaryMaterial();
    }

    List<TrackedMaterial> routedMaterials() {
        TrackerSelection selection = selectedSelection();
        return selection.isMaterial()
                ? selection.materialTarget().materials()
                : List.of();
    }

    boolean routesMaterial(TrackedMaterial material) {
        return material != null
                && hasMaterialSelection()
                && requireMaterialTarget().includes(material);
    }

    void resetSelectedSessionState() {
        TrackerSelection selection = selectedSelection();
        if (selection.isGemstone()) {
            gemstoneRegistry().resetSession(selection.gemstone());
            return;
        }
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            state(material).resetSession();
        }
    }

    MaterialTrackerState state(TrackedMaterial material) {
        ensureMaterialStates();
        TrackedMaterial safeMaterial =
                material == null ? TrackedMaterial.GOLD : material;
        return materialStates.get(safeMaterial.id());
    }

    DashboardModule selectedDashboardModule() {
        return DashboardModule.fromId(
                selectedDashboardModuleId,
                miningTrackerPanelOpen);
    }

    void setSelectedDashboardModule(DashboardModule module) {
        DashboardModule safeModule =
                module == null ? DashboardModule.NONE : module;
        selectedDashboardModuleId = safeModule.id();
        miningTrackerPanelOpen =
                safeModule == DashboardModule.MINING_TRACKER;
    }

    Map<String, MaterialTrackerState> states() {
        ensureMaterialStates();
        return materialStates;
    }

    GemstoneTrackerState gemstoneState(GemstoneType gemstone) {
        ensureGemstoneStates();
        return gemstoneRegistry().state(gemstone);
    }

    GemstoneTrackerRegistry gemstoneRegistry() {
        ensureGemstoneStates();
        return new GemstoneTrackerRegistry(gemstoneStates);
    }

    /** Restores all HUD {@code show*} visibility flags to defaults. */
    void resetHudVisibility() {
        showBlocks = true;
        showRawMaterial = true;
        showEnchantedMaterial = true;
        showSessionProfit = true;
        showUnsoldValue = true;
        showCoinsPerHour = true;
        showMaterialPerHour = true;
        showSessionTime = true;
        showActiveTool = true;
        showArea = true;
        showRateGraph = true;
        showDropAndFortune = true;
        showBazaarPrices = true;
        showValuePanel = true;
        showHudTitle = true;
        showHudStatus = true;
        showHudVersion = true;
        showHudAutoPause = true;
        hudShowBackground = true;
        powderChestHudShowBackground = true;
        showTargetHeading = true;
        showOtherSection = true;
        showTargetValue = true;
        showOtherValue = true;
        showTotalMinedValue = true;
    }

    void normalize() {
        dataVersion = CURRENT_DATA_VERSION;
        selectedTargetId = selectedSelection().id();
        setSelectedDashboardModule(selectedDashboardModule());
        scale = finite(scale) ? Math.max(0.5F, Math.min(2.5F, scale)) : 1.0F;
        x = finite(x) ? Math.max(0, x) : 12.0F;
        y = finite(y) ? Math.max(0, y) : 12.0F;
        powderChestHudScale = finite(powderChestHudScale)
                ? Math.max(0.5F, Math.min(2.5F, powderChestHudScale))
                : 1.0F;
        powderChestHudX = finite(powderChestHudX)
                ? Math.max(0, powderChestHudX)
                : 292.0F;
        powderChestHudY = finite(powderChestHudY)
                ? Math.max(0, powderChestHudY)
                : 12.0F;
        bazaarTaxPercent = Double.isFinite(bazaarTaxPercent)
                ? Math.max(0, Math.min(100, bazaarTaxPercent))
                : 1.25;
        miningFortune = nonNegativeFinite(miningFortune);
        blockFortune = nonNegativeFinite(blockFortune);
        oreFortune = nonNegativeFinite(oreFortune);
        dwarvenMetalFortune = nonNegativeFinite(dwarvenMetalFortune);
        gemstoneFortune = nonNegativeFinite(gemstoneFortune);
        fortuneLastDetectedEpochMillis = Math.max(0, fortuneLastDetectedEpochMillis);
        combinedTotalActiveMillis = Math.max(0, combinedTotalActiveMillis);
        combinedSessionActiveMillis = Math.max(0, combinedSessionActiveMillis);
        combinedLastBreakEpochMillis =
                Math.max(0, combinedLastBreakEpochMillis);
        if (fortuneSource == null || fortuneSource.isBlank()) {
            fortuneSource = "not detected";
        }
        if (qolUtilities == null) {
            qolUtilities = new QolUtilityConfig();
        }
        qolUtilities.normalizeHudPoses();
        ensureMaterialStates();
        ensureGemstoneStates();
        for (TrackedMaterial material : TrackedMaterial.values()) {
            MaterialTrackerState state = materialStates.get(material.id());
            if (state == null) continue;
            state.normalize();
            if (state.totalBlocks > 0 && state.totalBaseDrops == 0) {
                state.totalBaseDrops =
                        safeMultiply(state.totalBlocks, material.baseDrop());
            }
            if (state.sessionBlocks > 0 && state.sessionBaseDrops == 0) {
                state.sessionBaseDrops =
                        safeMultiply(state.sessionBlocks, material.baseDrop());
            }
        }
        if (combinedTotalActiveMillis == 0) {
            combinedTotalActiveMillis = Math.max(
                    state(TrackedMaterial.MITHRIL).totalActiveMillis,
                    state(TrackedMaterial.TITANIUM).totalActiveMillis);
        }
        if (combinedSessionActiveMillis == 0) {
            combinedSessionActiveMillis = Math.max(
                    state(TrackedMaterial.MITHRIL).sessionActiveMillis,
                    state(TrackedMaterial.TITANIUM).sessionActiveMillis);
        }
    }

    private void ensureMaterialStates() {
        if (materialStates == null) materialStates = new LinkedHashMap<>();
        for (TrackedMaterial material : TrackedMaterial.values()) {
            MaterialTrackerState state = materialStates.get(material.id());
            if (state == null) {
                String nonCanonicalKey = null;
                for (String key : materialStates.keySet()) {
                    if (material.id().equalsIgnoreCase(key)) {
                        nonCanonicalKey = key;
                        break;
                    }
                }
                if (nonCanonicalKey != null) {
                    state = materialStates.remove(nonCanonicalKey);
                }
            }
            if (state == null) state = new MaterialTrackerState();
            materialStates.put(material.id(), state);
        }
    }

    private void ensureGemstoneStates() {
        if (gemstoneStates == null) {
            gemstoneStates = new LinkedHashMap<>();
        }
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry(gemstoneStates);
        registry.normalize();
    }

    private static boolean finite(float value) {
        return Float.isFinite(value);
    }

    private static double nonNegativeFinite(double value) {
        return Double.isFinite(value) ? Math.max(0, value) : 0;
    }

    private static long safeMultiply(long left, long right) {
        if (left <= 0 || right <= 0) return 0;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }
}
