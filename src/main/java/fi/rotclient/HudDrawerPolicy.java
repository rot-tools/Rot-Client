package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared HUD-settings drawer contract: which catalog rows belong in the HUD
 * drawer versus module Settings, plus synthetic look/scale rows.
 */
public final class HudDrawerPolicy {
    public static final String HUD_VISIBLE = "rotclient.hud_style.visible";
    public static final String STYLE_TITLE = "rotclient.hud_style.title";
    public static final String STYLE_BACKGROUND = "rotclient.hud_style.background";
    public static final String STYLE_TEXT = "rotclient.hud_style.text_color";
    public static final String STYLE_PANEL = "rotclient.hud_style.panel_color";
    public static final String STYLE_SCALE = "rotclient.hud_style.scale";
    public static final String STYLE_RESET = "rotclient.hud_style.reset";

    private HudDrawerPolicy() {}

    public static boolean isStyleSetting(String settingId) {
        String id = normalize(settingId);
        return STYLE_TITLE.equals(id)
                || STYLE_BACKGROUND.equals(id)
                || STYLE_TEXT.equals(id)
                || STYLE_PANEL.equals(id)
                || STYLE_SCALE.equals(id)
                || STYLE_RESET.equals(id)
                || HUD_VISIBLE.equals(id);
    }

    public static boolean hudVisibilityUsesModuleEnable(HudElementCatalog.HudPiece piece) {
        return piece == null || !piece.hasToggle();
    }

    public static String styleFocusId(HudElementCatalog.HudPiece piece) {
        if (piece == null) {
            return "";
        }
        if (piece.focusId() != null && !piece.focusId().isBlank()) {
            return piece.focusId().trim();
        }
        return HudElementCatalog.focusIdForHudEditorSetting(piece.editorId());
    }

    public static List<QolUtilityCatalog.SettingDef> styleSettings() {
        return List.of(
                row(STYLE_TITLE, "Title", "Show the HUD header line.",
                        QolUtilityCatalog.SettingType.TOGGLE),
                row(STYLE_BACKGROUND, "Background", "Draw a panel behind HUD text.",
                        QolUtilityCatalog.SettingType.TOGGLE),
                row(STYLE_TEXT, "Text color", "HUD text color.",
                        QolUtilityCatalog.SettingType.COLOR),
                row(STYLE_PANEL, "Panel color", "HUD panel fill, including alpha.",
                        QolUtilityCatalog.SettingType.COLOR),
                row(STYLE_SCALE, "Scale", "Size of this HUD on screen.",
                        QolUtilityCatalog.SettingType.NUMBER));
    }

    public static boolean isHudDrawerSetting(
            QolUtilityCatalog.SettingDef setting,
            QolUtilityCatalog.ModuleDef module,
            HudElementCatalog.HudPiece piece) {
        if (setting == null || module == null || piece == null) {
            return false;
        }
        if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
            return false;
        }
        String id = setting.id();
        if (id.equals(piece.toggleId()) || id.equals(piece.editorId())) {
            return true;
        }
        Set<String> otherToggles = otherPieceToggles(module, piece);
        if (otherToggles.contains(id)) {
            return false;
        }
        for (HudElementCatalog.InspectorToggle toggle
                : HudElementCatalog.inspectorToggles(styleFocusId(piece))) {
            if (id.equals(toggle.settingId())) {
                return true;
            }
        }
        return isHudContentExtra(id, setting.type());
    }

    public static boolean usesQolHudStyle(HudElementCatalog.HudPiece piece) {
        return MiningTrackerCatalogPolicy.usesQolHudStyle(styleFocusId(piece));
    }

    public static boolean isModuleDrawerSetting(
            QolUtilityCatalog.SettingDef setting,
            QolUtilityCatalog.ModuleDef module) {
        if (setting == null || module == null) {
            return true;
        }
        if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
            return hasModuleDrawerChild(module, setting.id());
        }
        for (HudElementCatalog.HudPiece piece : HudElementCatalog.hudPieces(module)) {
            if (isHudDrawerSetting(setting, module, piece)) {
                return false;
            }
        }
        return true;
    }

    public static List<QolUtilityCatalog.SettingDef> hudCatalogSettings(
            QolUtilityCatalog.ModuleDef module,
            HudElementCatalog.HudPiece piece) {
        if (module == null || piece == null) {
            return List.of();
        }
        List<QolUtilityCatalog.SettingDef> out = new ArrayList<>();
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (isHudDrawerSetting(setting, module, piece)) {
                out.add(setting);
            }
        }
        return List.copyOf(out);
    }

    public static List<QolUtilityCatalog.SettingDef> moduleCatalogSettings(
            QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return List.of();
        }
        List<QolUtilityCatalog.SettingDef> out = new ArrayList<>();
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (isModuleDrawerSetting(setting, module)) {
                out.add(setting);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Every HUD-owned catalog row for a module, in catalog order. Used when the
     * card HUD button opens one combined drawer instead of a per-piece popup.
     */
    public static List<QolUtilityCatalog.SettingDef> allHudCatalogSettings(
            QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return List.of();
        }
        List<HudElementCatalog.HudPiece> pieces = HudElementCatalog.hudPieces(module);
        Set<String> seen = new HashSet<>();
        List<QolUtilityCatalog.SettingDef> out = new ArrayList<>();
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                if (sectionHasCombinedHudChild(module, setting.id(), pieces) && seen.add(setting.id())) {
                    out.add(setting);
                }
                continue;
            }
            String id = setting.id();
            if (id == null || id.isBlank() || seen.contains(id)) {
                continue;
            }
            if (!isCombinedHudSetting(setting, module, pieces)) {
                continue;
            }
            seen.add(id);
            out.add(setting);
        }
        return List.copyOf(out);
    }

    /**
     * Shared look/scale focus when this module is one overlay. Blank when
     * several standalone HUD pieces would disagree (Player Display).
     */
    public static String uniqueStyleFocus(QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return "";
        }
        int standalone = 0;
        String found = "";
        for (HudElementCatalog.HudPiece piece : HudElementCatalog.hudPieces(module)) {
            if (piece.hasToggle() && isStandaloneHudToggle(piece.toggleId())) {
                standalone++;
            }
            if (!usesQolHudStyle(piece)) {
                continue;
            }
            String focus = styleFocusId(piece);
            if (focus.isBlank()) {
                continue;
            }
            if (found.isBlank()) {
                found = focus;
            } else if (!found.equals(focus)) {
                return "";
            }
        }
        if (standalone > 1) {
            return "";
        }
        return found;
    }

    public static boolean hudDrawerShowsModuleEnableRow(
            QolUtilityCatalog.ModuleDef module) {
        List<HudElementCatalog.HudPiece> pieces = HudElementCatalog.hudPieces(module);
        if (pieces.isEmpty()) {
            return false;
        }
        for (HudElementCatalog.HudPiece piece : pieces) {
            if (!hudVisibilityUsesModuleEnable(piece)) {
                return false;
            }
        }
        return true;
    }

    public static HudElementCatalog.HudPiece resolvePiece(
            QolUtilityCatalog.ModuleDef module,
            String toggleId,
            String editorId) {
        List<HudElementCatalog.HudPiece> pieces = HudElementCatalog.hudPieces(module);
        if (pieces.isEmpty()) {
            return null;
        }
        String toggle = toggleId == null ? "" : toggleId;
        String editor = editorId == null ? "" : editorId;
        if (!toggle.isBlank() || !editor.isBlank()) {
            for (HudElementCatalog.HudPiece piece : pieces) {
                if (toggle.equals(piece.toggleId()) && editor.equals(piece.editorId())) {
                    return piece;
                }
            }
            for (HudElementCatalog.HudPiece piece : pieces) {
                if ((!toggle.isBlank() && toggle.equals(piece.toggleId()))
                        || (!editor.isBlank() && editor.equals(piece.editorId()))) {
                    return piece;
                }
            }
        }
        return pieces.get(0);
    }

    private static boolean isCombinedHudSetting(
            QolUtilityCatalog.SettingDef setting,
            QolUtilityCatalog.ModuleDef module,
            List<HudElementCatalog.HudPiece> pieces) {
        if (HudElementCatalog.isHudVisibilityToggle(setting)) {
            return true;
        }
        String id = setting.id();
        if (setting.type() == QolUtilityCatalog.SettingType.ACTION
                && id != null
                && id.endsWith("_hud_editor")) {
            return true;
        }
        for (HudElementCatalog.HudPiece piece : pieces) {
            if (isHudDrawerSetting(setting, module, piece)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sectionHasCombinedHudChild(
            QolUtilityCatalog.ModuleDef module,
            String sectionId,
            List<HudElementCatalog.HudPiece> pieces) {
        boolean inSection = false;
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                inSection = setting.id().equals(sectionId);
                continue;
            }
            if (inSection && isCombinedHudSetting(setting, module, pieces)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStandaloneHudToggle(String id) {
        String key = normalize(id);
        return key.endsWith(".hud") || key.endsWith("_hud");
    }

    private static boolean hasModuleDrawerChild(
            QolUtilityCatalog.ModuleDef module,
            String sectionId) {
        boolean inSection = false;
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                inSection = setting.id().equals(sectionId);
                continue;
            }
            if (inSection && isModuleDrawerSetting(setting, module)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> otherPieceToggles(
            QolUtilityCatalog.ModuleDef module,
            HudElementCatalog.HudPiece current) {
        Set<String> ids = new HashSet<>();
        for (HudElementCatalog.HudPiece piece : HudElementCatalog.hudPieces(module)) {
            if (piece == current) {
                continue;
            }
            if (samePiece(piece, current)) {
                continue;
            }
            if (piece.hasToggle() && !isSharedHudExtra(piece.toggleId())) {
                ids.add(piece.toggleId());
            }
        }
        return ids;
    }

    private static boolean samePiece(
            HudElementCatalog.HudPiece left,
            HudElementCatalog.HudPiece right) {
        return left.toggleId().equals(right.toggleId())
                && left.editorId().equals(right.editorId())
                && left.focusId().equals(right.focusId());
    }

    private static boolean isHudContentExtra(String id, QolUtilityCatalog.SettingType type) {
        String key = normalize(id);
        if (isSharedHudExtra(key)) {
            return true;
        }
        if (MiningTrackerCatalogPolicy.isHudContentSetting(key)) {
            return true;
        }
        return type == QolUtilityCatalog.SettingType.TOGGLE
                && key.startsWith("qol.slayer_stats.")
                && !key.endsWith(".reset_session");
    }

    private static boolean isSharedHudExtra(String id) {
        String key = normalize(id);
        return key.contains("show_icons")
                || key.contains("show_labels")
                || key.contains("show_max")
                || key.endsWith(".dynamic_size")
                || key.endsWith(".kill_time");
    }

    private static QolUtilityCatalog.SettingDef row(
            String id,
            String label,
            String description,
            QolUtilityCatalog.SettingType type) {
        return new QolUtilityCatalog.SettingDef(
                id, label, description, type, List.of(), List.of());
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
