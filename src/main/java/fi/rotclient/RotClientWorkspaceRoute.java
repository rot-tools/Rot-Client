package fi.rotclient;

import java.util.Locale;

/**
 * Stable view routes for Client UI workspace tabs.
 * Application version is intentionally independent of this enum's ids.
 */
enum RotClientWorkspaceRoute {
    OVERVIEW("overview", "Overview"),
    MINING_TRACKER("mining_tracker", "Mining Tracker"),
    POWDER_CHEST_TRACKER("powder_chest_tracker", "Powder Chest Tracker"),
    SESSION_ANALYTICS("session_analytics", "Session Analytics"),
    SESSION_HISTORY("session_history", "Session History"),
    QOL_SETTINGS("qol_settings", "QoL & Settings"),
    PROFILES("profiles", "Profiles"),
    LOADOUTS("loadouts", "Loadouts"),
    MARKET_WATCH("market_watch", "Market Watch"),
    APPEARANCE_OVERVIEW("appearance_overview", "Appearance"),
    APPEARANCE_DASHBOARD("appearance_dashboard", "Appearance"),
    APPEARANCE_MINING_HUD("appearance_mining_hud", "Appearance"),
    APPEARANCE_COLORS("appearance_colors", "Appearance"),
    APPEARANCE_BACKGROUND("appearance_background", "Appearance"),
    APPEARANCE_CHARTS("appearance_charts", "Appearance"),
    APPEARANCE_RESET("appearance_reset", "Appearance");

    private final String id;
    private final String title;

    RotClientWorkspaceRoute(String id, String title) {
        this.id = id;
        this.title = title;
    }

    String id() {
        return id;
    }

    String title() {
        return title;
    }

    boolean isAppearance() {
        return name().startsWith("APPEARANCE_");
    }

    DashboardModule toDashboardModule() {
        return switch (this) {
            case OVERVIEW -> DashboardModule.NONE;
            case MINING_TRACKER -> DashboardModule.MINING_TRACKER;
            case POWDER_CHEST_TRACKER -> DashboardModule.POWDER_CHEST_TRACKER;
            case SESSION_ANALYTICS -> DashboardModule.SESSION_ANALYTICS;
            case SESSION_HISTORY -> DashboardModule.SESSION_HISTORY;
            case QOL_SETTINGS -> DashboardModule.QOL_SETTINGS;
            default -> DashboardModule.NONE;
        };
    }

    static RotClientWorkspaceRoute fromDashboardModule(DashboardModule module) {
        if (module == null) {
            return OVERVIEW;
        }
        return switch (module) {
            case NONE -> OVERVIEW;
            case MINING_TRACKER -> MINING_TRACKER;
            case POWDER_CHEST_TRACKER -> POWDER_CHEST_TRACKER;
            case SESSION_ANALYTICS -> SESSION_ANALYTICS;
            case SESSION_HISTORY -> SESSION_HISTORY;
            case QOL_SETTINGS -> QOL_SETTINGS;
        };
    }

    static RotClientWorkspaceRoute fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return OVERVIEW;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (RotClientWorkspaceRoute route : values()) {
            if (route.id.equals(normalized)) {
                return route;
            }
        }
        // Legacy / unknown → safe Overview fallback (never wipe workspace).
        return OVERVIEW;
    }
}
