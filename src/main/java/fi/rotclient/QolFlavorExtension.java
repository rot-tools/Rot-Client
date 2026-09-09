package fi.rotclient;

import java.util.List;

/**
 * Plus-only catalog, settings, and client hooks. The legit JAR has no
 * ServiceLoader provider, so every default here is a no-op.
 */
public interface QolFlavorExtension {
    QolFlavorExtension NONE = new QolFlavorExtension() {};

    default boolean isPlus() {
        return false;
    }

    default String productName() {
        return "Rot Client";
    }

    default String productHeader() {
        return "ROT CLIENT";
    }

    default String modId() {
        return "rotclient";
    }

    default String configFileName() {
        return "rotclient.json";
    }

    default List<QolUtilityCatalog.ModuleDef> extraModules() {
        return List.of();
    }

    default List<QolUtilityCatalog.SettingDef> extraSettings(String moduleId) {
        return List.of();
    }

    default Boolean readModuleEnabled(QolUtilityConfig config, String moduleId) {
        return null;
    }

    default boolean writeModuleEnabled(
            QolUtilityConfig config, String moduleId, boolean enabled) {
        return false;
    }

    default Boolean readBoolean(QolUtilityConfig config, String settingId) {
        return null;
    }

    default boolean writeBoolean(
            QolUtilityConfig config, String settingId, boolean value) {
        return false;
    }

    default Double readNumber(QolUtilityConfig config, String settingId) {
        return null;
    }

    default boolean writeNumber(
            QolUtilityConfig config, String settingId, double value) {
        return false;
    }

    default String readKeybind(QolUtilityConfig config, String settingId) {
        return null;
    }

    default boolean writeKeybind(
            QolUtilityConfig config, String settingId, String value) {
        return false;
    }

    default String readText(QolUtilityConfig config, String settingId) {
        return null;
    }

    default boolean writeText(
            QolUtilityConfig config, String settingId, String value) {
        return false;
    }

    default String readEnum(QolUtilityConfig config, String settingId) {
        return null;
    }

    default boolean writeEnum(
            QolUtilityConfig config, String settingId, String value) {
        return false;
    }

    default Integer readColor(QolUtilityConfig config, String settingId) {
        return null;
    }

    default boolean writeColor(
            QolUtilityConfig config, String settingId, int value) {
        return false;
    }

    default float[] readPose(QolUtilityConfig config, String poseId) {
        return null;
    }

    default boolean writePose(
            QolUtilityConfig config, String poseId, float x, float y, float scale) {
        return false;
    }

    default void loadPersistence() {
    }

    default void savePersistence() {
    }

    default List<HudLayerCatalog.Layer> extraHudLayers() {
        return List.of();
    }

    default QolNumberSettings.Spec numberSpec(String settingId) {
        return null;
    }

    default String hudFocusId(String settingId) {
        return "";
    }

    default List<HudElementCatalog.InspectorToggle> inspectorToggles(String poseId) {
        return List.of();
    }

    default String disableSettingId(String poseId) {
        return "";
    }

    default boolean resetModule(QolUtilityConfig config, String moduleId) {
        return false;
    }
}
