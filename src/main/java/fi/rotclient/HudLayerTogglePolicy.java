package fi.rotclient;

/**
 * Inspector on/off for HUD Layout: vanilla layers are boolean hides,
 * Rot overlays are parent module switches.
 */
public final class HudLayerTogglePolicy {
    private HudLayerTogglePolicy() {
    }

    public static boolean isOn(QolUtilityConfig qol, String id, boolean moduleToggle) {
        if (qol == null || id == null || id.isBlank()) {
            return false;
        }
        if (moduleToggle) {
            return qol.isModuleEnabled(id);
        }
        Boolean value = qol.readBoolean(id);
        return value != null && value;
    }

    public static boolean toggle(QolUtilityConfig qol, String id, boolean moduleToggle) {
        if (qol == null || id == null || id.isBlank()) {
            return false;
        }
        if (moduleToggle) {
            qol.setModuleEnabled(id, !qol.isModuleEnabled(id));
            return true;
        }
        return qol.toggleBooleanSetting(id);
    }
}
