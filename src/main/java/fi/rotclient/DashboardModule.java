package fi.rotclient;

import java.util.Locale;

enum DashboardModule {
    NONE("none"),
    MINING_TRACKER("mining_tracker"),
    POWDER_CHEST_TRACKER("powder_chest_tracker"),
    QOL_SETTINGS("qol_settings"),
    SESSION_ANALYTICS("session_analytics"),
    SESSION_HISTORY("session_history");

    private final String id;

    DashboardModule(String id) {
        this.id = id;
    }

    String id() {
        return id;
    }

    static DashboardModule fromId(
            String id, boolean legacyPanelOpen) {
        if (id == null || id.isBlank()) {
            return legacyPanelOpen
                    ? MINING_TRACKER
                    : NONE;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if ("fullbright".equals(normalized)) return QOL_SETTINGS;
        for (DashboardModule module : values()) {
            if (module.id.equals(normalized)) return module;
        }
        return NONE;
    }
}
