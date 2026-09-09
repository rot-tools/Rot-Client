package fi.rotclient;

import java.util.Locale;
import java.util.Objects;

/**
 * Last QoL dashboard page plus optional settings/HUD drawer. Minecraft-free
 * so reopen restore can be unit-tested without the Click GUI.
 */
final class QolWorkspaceView {
    static final String DRAWER_MODULE = "module";
    static final String DRAWER_HUD = "hud";
    static final String LANDING_APPEARANCE = "appearance";
    static final String LANDING_HUD_LAYOUT = "hud_layout";

    private final String groupId;
    private final String moduleId;
    private final String drawerKind;
    private final String landing;

    private QolWorkspaceView(
            String groupId,
            String moduleId,
            String drawerKind,
            String landing) {
        this.groupId = groupId;
        this.moduleId = moduleId;
        this.drawerKind = drawerKind;
        this.landing = landing;
    }

    public static QolWorkspaceView empty() {
        return new QolWorkspaceView("", "", "", "");
    }

    /**
     * Click GUI key and default dashboard open keep the last view.
     * Only a named module argument replaces the saved route.
     */
    public static boolean shouldNavigateTo(DashboardModule module) {
        return module != null && module != DashboardModule.NONE;
    }

    public static QolWorkspaceView capture(
            String groupId,
            boolean appearanceLanding,
            boolean hudLayoutLanding,
            boolean drawerOpen,
            boolean hudDrawer,
            String openModuleId) {
        String landing = appearanceLanding
                ? LANDING_APPEARANCE
                : (hudLayoutLanding ? LANDING_HUD_LAYOUT : "");
        String module = "";
        String kind = "";
        String openId = safe(openModuleId);
        if (drawerOpen && !openId.isEmpty() && !isLandingCatalogId(openId)) {
            module = openId;
            kind = hudDrawer ? DRAWER_HUD : DRAWER_MODULE;
        } else if (LANDING_APPEARANCE.equals(landing)) {
            module = MiningTrackerCatalogPolicy.APPEARANCE;
        } else if (LANDING_HUD_LAYOUT.equals(landing)) {
            module = MiningTrackerCatalogPolicy.HUD_LAYOUT;
        }
        return parse(groupId, module, kind, landing);
    }

    public static QolWorkspaceView fromTab(RotClientWorkspaceTab tab) {
        if (tab == null) {
            return empty();
        }
        return parse(tab.qolGroup, tab.qolModuleId, tab.qolDrawerKind, tab.qolLanding);
    }

    public static QolWorkspaceView parse(
            String groupId,
            String moduleId,
            String drawerKind,
            String landing) {
        String group = safe(groupId);
        String module = safe(moduleId);
        String kind = normalizeDrawerKind(drawerKind);
        String land = normalizeLanding(landing);
        if (land.isEmpty()) {
            if (MiningTrackerCatalogPolicy.APPEARANCE.equals(module)) {
                land = LANDING_APPEARANCE;
            } else if (MiningTrackerCatalogPolicy.HUD_LAYOUT.equals(module)) {
                land = LANDING_HUD_LAYOUT;
            }
        }
        if (isLandingCatalogId(module) || module.isEmpty()) {
            kind = "";
            if (isLandingCatalogId(module) && land.isEmpty()) {
                land = MiningTrackerCatalogPolicy.APPEARANCE.equals(module)
                        ? LANDING_APPEARANCE
                        : LANDING_HUD_LAYOUT;
            }
        } else if (!DRAWER_HUD.equals(kind)) {
            kind = DRAWER_MODULE;
        }
        return new QolWorkspaceView(group, module, kind, land);
    }

    public String groupId() {
        return groupId;
    }

    public String moduleId() {
        return moduleId;
    }

    public String drawerKind() {
        return drawerKind;
    }

    public String landing() {
        return landing;
    }

    public boolean hasDrawerModule() {
        return !moduleId.isEmpty() && !isLandingCatalogId(moduleId);
    }

    public boolean hudDrawer() {
        return hasDrawerModule() && DRAWER_HUD.equals(drawerKind);
    }

    public boolean appearanceLanding() {
        return LANDING_APPEARANCE.equals(landing);
    }

    public boolean hudLayoutLanding() {
        return LANDING_HUD_LAYOUT.equals(landing);
    }

    public void applyTo(RotClientWorkspaceTab tab) {
        if (tab == null) {
            return;
        }
        tab.qolGroup = groupId;
        tab.qolModuleId = moduleId;
        tab.qolDrawerKind = drawerKind;
        tab.qolLanding = landing;
    }

    public boolean sameAs(RotClientWorkspaceTab tab) {
        return equals(fromTab(tab));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof QolWorkspaceView other)) {
            return false;
        }
        return groupId.equals(other.groupId)
                && moduleId.equals(other.moduleId)
                && drawerKind.equals(other.drawerKind)
                && landing.equals(other.landing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, moduleId, drawerKind, landing);
    }

    private static boolean isLandingCatalogId(String moduleId) {
        return MiningTrackerCatalogPolicy.APPEARANCE.equals(moduleId)
                || MiningTrackerCatalogPolicy.HUD_LAYOUT.equals(moduleId);
    }

    private static String normalizeLanding(String raw) {
        String value = safe(raw).toLowerCase(Locale.ROOT);
        if (LANDING_APPEARANCE.equals(value) || LANDING_HUD_LAYOUT.equals(value)) {
            return value;
        }
        return "";
    }

    private static String normalizeDrawerKind(String raw) {
        String value = safe(raw).toLowerCase(Locale.ROOT);
        if (DRAWER_HUD.equals(value) || DRAWER_MODULE.equals(value)) {
            return value;
        }
        return "";
    }

    private static String safe(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
