package fi.rotclient;

import java.util.UUID;

/**
 * Lightweight view-state for one Client UI tab. Never holds domain/ledger data.
 */
final class RotClientWorkspaceTab {
    String id = UUID.randomUUID().toString();
    String route = RotClientWorkspaceRoute.OVERVIEW.id();
    int scrollPixels;
    String selectedHistorySessionId = "";
    /** Last QoL dashboard group (HUD, Combat, …) on this tab. */
    String qolGroup = "";
    /**
     * Open settings/HUD drawer module id, Appearance/HUD-layout catalog id
     * when that landing is showing with no drawer, or blank.
     */
    String qolModuleId = "";
    /** {@code module}, {@code hud}, or blank when no drawer is open. */
    String qolDrawerKind = "";
    /** {@code appearance}, {@code hud_layout}, or blank. */
    String qolLanding = "";

    RotClientWorkspaceTab() {
    }

    RotClientWorkspaceTab(RotClientWorkspaceRoute route) {
        this.route = route == null
                ? RotClientWorkspaceRoute.OVERVIEW.id()
                : route.id();
    }

    RotClientWorkspaceRoute resolvedRoute() {
        return RotClientWorkspaceRoute.fromId(route);
    }

    String title() {
        return resolvedRoute().title();
    }

    RotClientWorkspaceTab copy() {
        RotClientWorkspaceTab copy = new RotClientWorkspaceTab();
        copy.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        copy.route = route == null ? RotClientWorkspaceRoute.OVERVIEW.id() : route;
        copy.scrollPixels = Math.max(0, scrollPixels);
        copy.selectedHistorySessionId = selectedHistorySessionId == null
                ? ""
                : selectedHistorySessionId;
        copy.qolGroup = qolGroup == null ? "" : qolGroup;
        copy.qolModuleId = qolModuleId == null ? "" : qolModuleId;
        copy.qolDrawerKind = qolDrawerKind == null ? "" : qolDrawerKind;
        copy.qolLanding = qolLanding == null ? "" : qolLanding;
        return copy;
    }

    void normalize() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        RotClientWorkspaceRoute resolved = RotClientWorkspaceRoute.fromId(route);
        route = resolved.id();
        scrollPixels = Math.max(0, scrollPixels);
        if (selectedHistorySessionId == null) {
            selectedHistorySessionId = "";
        }
        if (qolGroup == null) {
            qolGroup = "";
        }
        if (qolModuleId == null) {
            qolModuleId = "";
        }
        if (qolDrawerKind == null) {
            qolDrawerKind = "";
        }
        if (qolLanding == null) {
            qolLanding = "";
        }
    }

    /** Clamp restored scroll against current content capacity. */
    void clampScroll(int maxScroll) {
        int max = Math.max(0, maxScroll);
        if (scrollPixels < 0) {
            scrollPixels = 0;
        } else if (scrollPixels > max) {
            scrollPixels = max;
        }
    }
}
