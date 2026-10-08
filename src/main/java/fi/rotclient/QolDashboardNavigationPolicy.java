package fi.rotclient;

import java.util.List;

/** Presentation grouping only: original catalog, feature IDs and profile group IDs remain readable. */
public final class QolDashboardNavigationPolicy {
    public static QolUtilityCatalog.Group page(QolUtilityCatalog.Group group) {
        if (group == null) return QolUtilityCatalog.Group.COMBAT;
        return switch (group) {
            case GUI, HUD_DISPLAY, RENDER, INTERFACE -> QolUtilityCatalog.Group.GUI;
            default -> group;
        };
    }
    public static List<QolUtilityCatalog.Group> pages() {
        return QolUtilityCatalog.sidebarPages().stream().map(QolDashboardNavigationPolicy::page).distinct().toList();
    }
    public static List<QolUtilityCatalog.ModuleDef> modules(QolUtilityCatalog.Group group) {
        var target = page(group);
        return QolUtilityCatalog.modules().stream().filter(m -> page(m.group()) == target)
                .filter(m -> !QolUtilityCatalog.hiddenFromGroupPage(m)).toList();
    }
    public static String title(QolUtilityCatalog.Group group) {
        return page(group) == QolUtilityCatalog.Group.GUI ? "Display & Interface" : page(group).title();
    }
    public static String subtitle(QolUtilityCatalog.Group group) {
        return page(group) == QolUtilityCatalog.Group.GUI ? "HUD, rendering, inventory, menus" : page(group).sidebarSubtitle();
    }
    public static String description(QolUtilityCatalog.Group group) {
        return page(group) == QolUtilityCatalog.Group.GUI ? "Related display, HUD, render and inventory tools in one place." : page(group).pageDescription();
    }
    private QolDashboardNavigationPolicy() {}
}
