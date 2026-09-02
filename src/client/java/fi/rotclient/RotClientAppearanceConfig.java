package fi.rotclient;

import java.util.Locale;

/**
 * Persisted Appearance customizer colors and optional dashboard background.
 */
final class RotClientAppearanceConfig {
    static final int SCHEMA_VERSION = 4;
    /** Pre-brand slate surface that must migrate to {@link RotClientTheme#DEFAULT_SURFACE}. */
    private static final int LEGACY_SURFACE = 0xEE120909;
    private static final int LEGACY_ACCENT = 0xFFE33B3B;
    private static final int LEGACY_GOLD_ACCENT = 0xFFFFAA00;
    private static final int LEGACY_SLATE_BACKDROP = 0xF2111118;
    private static final int LEGACY_SLATE_SURFACE = 0xF51C1C24;

    int schemaVersion = SCHEMA_VERSION;
    int dashboardBackdrop = RotClientTheme.DEFAULT_BACKDROP;
    int dashboardSurface = RotClientTheme.DEFAULT_SURFACE;
    int dashboardSurfaceAlt = RotClientTheme.DEFAULT_SURFACE_ALT;
    int dashboardHeader = RotClientTheme.DEFAULT_HUD_HEADER;
    int dashboardSidebar = RotClientTheme.DEFAULT_SURFACE_ALT;
    int dashboardBorder = RotClientTheme.DEFAULT_BORDER;
    int dashboardAccent = RotClientTheme.DEFAULT_BORDER_BRIGHT;
    int dashboardSectionTitle = RotClientTheme.DEFAULT_TEXT_MUTED;
    int dashboardTextPrimary = RotClientTheme.DEFAULT_TEXT;
    int dashboardTextSecondary = RotClientTheme.DEFAULT_TEXT_DIM;
    int dashboardTextSuccess = RotClientTheme.DEFAULT_SUCCESS;
    int dashboardTextWarning = RotClientTheme.DEFAULT_WARNING;
    int dashboardButton = RotClientTheme.DEFAULT_BUTTON;
    int dashboardButtonHover = RotClientTheme.DEFAULT_BUTTON_HOVER;
    int dashboardButtonSelected = RotClientTheme.DEFAULT_SELECTED_ROW;
    int dashboardButtonText = RotClientTheme.DEFAULT_TEXT;
    int dashboardField = RotClientTheme.DEFAULT_FIELD;
    int dashboardFieldActive = RotClientTheme.DEFAULT_FIELD_ACTIVE;
    int dashboardHoverRow = RotClientTheme.DEFAULT_HOVER_ROW;
    int dashboardDivider = RotClientTheme.DEFAULT_DIVIDER;
    int dashboardShadow = RotClientTheme.DEFAULT_SHADOW;
    int dashboardError = RotClientTheme.DEFAULT_ERROR;

    int hudBackground = RotClientTheme.DEFAULT_HUD_BACKGROUND;
    int hudPanel = RotClientTheme.DEFAULT_HUD_PANEL;
    int hudPanelAlt = RotClientTheme.DEFAULT_HUD_PANEL_ALT;
    int hudHeader = RotClientTheme.DEFAULT_HUD_HEADER;
    int hudBorder = RotClientTheme.DEFAULT_BORDER;
    int hudAccent = RotClientTheme.DEFAULT_HUD_ACCENT;
    int hudTitle = RotClientTheme.DEFAULT_TEXT;
    int hudTextPrimary = RotClientTheme.DEFAULT_TEXT;
    int hudTextSecondary = RotClientTheme.DEFAULT_TEXT_DIM;
    int hudChartLine = RotClientTheme.DEFAULT_CHART_LINE;
    int hudChartGlow = RotClientTheme.DEFAULT_CHART_GLOW;
    int hudChartFill = RotClientTheme.DEFAULT_CHART_FILL;
    int hudChartGrid = RotClientTheme.DEFAULT_CHART_GRID;
    int hudChartWell = RotClientTheme.DEFAULT_CHART_WELL;

    boolean customBackgroundEnabled;
    String customBackgroundFile = "";
    int customBackgroundOpacity = 180;
    int customBackgroundDim = 80;
    String customBackgroundFit = "fill";

    static RotClientAppearanceConfig defaults() {
        return new RotClientAppearanceConfig();
    }

    RotClientAppearanceConfig copy() {
        RotClientAppearanceConfig copy = new RotClientAppearanceConfig();
        copy.schemaVersion = schemaVersion;
        copy.dashboardBackdrop = dashboardBackdrop;
        copy.dashboardSurface = dashboardSurface;
        copy.dashboardSurfaceAlt = dashboardSurfaceAlt;
        copy.dashboardHeader = dashboardHeader;
        copy.dashboardSidebar = dashboardSidebar;
        copy.dashboardBorder = dashboardBorder;
        copy.dashboardAccent = dashboardAccent;
        copy.dashboardSectionTitle = dashboardSectionTitle;
        copy.dashboardTextPrimary = dashboardTextPrimary;
        copy.dashboardTextSecondary = dashboardTextSecondary;
        copy.dashboardTextSuccess = dashboardTextSuccess;
        copy.dashboardTextWarning = dashboardTextWarning;
        copy.dashboardButton = dashboardButton;
        copy.dashboardButtonHover = dashboardButtonHover;
        copy.dashboardButtonSelected = dashboardButtonSelected;
        copy.dashboardButtonText = dashboardButtonText;
        copy.dashboardField = dashboardField;
        copy.dashboardFieldActive = dashboardFieldActive;
        copy.dashboardHoverRow = dashboardHoverRow;
        copy.dashboardDivider = dashboardDivider;
        copy.dashboardShadow = dashboardShadow;
        copy.dashboardError = dashboardError;
        copy.hudBackground = hudBackground;
        copy.hudPanel = hudPanel;
        copy.hudPanelAlt = hudPanelAlt;
        copy.hudHeader = hudHeader;
        copy.hudBorder = hudBorder;
        copy.hudAccent = hudAccent;
        copy.hudTitle = hudTitle;
        copy.hudTextPrimary = hudTextPrimary;
        copy.hudTextSecondary = hudTextSecondary;
        copy.hudChartLine = hudChartLine;
        copy.hudChartGlow = hudChartGlow;
        copy.hudChartFill = hudChartFill;
        copy.hudChartGrid = hudChartGrid;
        copy.hudChartWell = hudChartWell;
        copy.customBackgroundEnabled = customBackgroundEnabled;
        copy.customBackgroundFile = customBackgroundFile == null ? "" : customBackgroundFile;
        copy.customBackgroundOpacity = customBackgroundOpacity;
        copy.customBackgroundDim = customBackgroundDim;
        copy.customBackgroundFit = customBackgroundFit;
        return copy;
    }

    void normalize() {
        int incomingSchema = schemaVersion;
        if (incomingSchema < 2) {
            if (dashboardSurface == LEGACY_SURFACE) {
                dashboardSurface = RotClientTheme.DEFAULT_SURFACE;
            }
            if (dashboardAccent == LEGACY_ACCENT) {
                dashboardAccent = RotClientTheme.DEFAULT_BORDER_BRIGHT;
            }
        }
        if (incomingSchema < 3) {
            if (dashboardAccent == LEGACY_GOLD_ACCENT) {
                dashboardAccent = RotClientTheme.DEFAULT_BORDER_BRIGHT;
            }
            if (hudAccent == LEGACY_GOLD_ACCENT) {
                hudAccent = RotClientTheme.DEFAULT_HUD_ACCENT;
            }
            if (dashboardBackdrop == LEGACY_SLATE_BACKDROP) {
                dashboardBackdrop = RotClientTheme.DEFAULT_BACKDROP;
            }
            if (dashboardSurface == LEGACY_SLATE_SURFACE) {
                dashboardSurface = RotClientTheme.DEFAULT_SURFACE;
            }
        }
        if (incomingSchema < 4) {
            if (dashboardTextSecondary == 0xFFD4CBE0) {
                dashboardTextSecondary = RotClientTheme.DEFAULT_TEXT_DIM;
            }
            if (dashboardSectionTitle == 0xFF9B90B0) {
                dashboardSectionTitle = RotClientTheme.DEFAULT_TEXT_MUTED;
            }
            if (hudTextSecondary == 0xFFD4CBE0) {
                hudTextSecondary = RotClientTheme.DEFAULT_TEXT_DIM;
            }
        }
        schemaVersion = SCHEMA_VERSION;
        customBackgroundOpacity = clamp(customBackgroundOpacity, 0, 255);
        customBackgroundDim = clamp(customBackgroundDim, 0, 255);
        customBackgroundFit = normalizeFit(customBackgroundFit);
        if (customBackgroundFile == null) {
            customBackgroundFile = "";
        }
        if (!RotClientBackgroundManager.isSafeFileName(customBackgroundFile)
                || !RotClientBackgroundManager.isSupportedImageName(customBackgroundFile)) {
            customBackgroundFile = "";
            customBackgroundEnabled = false;
        }
        if (customBackgroundFile.isBlank()) {
            customBackgroundEnabled = false;
        }
    }

    static int parseChannel(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return clamp(fallback, 0, 255);
        }
        try {
            return clamp(Integer.parseInt(raw.trim()), 0, 255);
        } catch (NumberFormatException ignored) {
            return clamp(fallback, 0, 255);
        }
    }

    boolean equalsNormalized(RotClientAppearanceConfig other) {
        if (other == null) {
            return false;
        }
        RotClientAppearanceConfig a = copy();
        RotClientAppearanceConfig b = other.copy();
        a.normalize();
        b.normalize();
        return a.dashboardBackdrop == b.dashboardBackdrop
                && a.dashboardSurface == b.dashboardSurface
                && a.dashboardAccent == b.dashboardAccent
                && a.dashboardTextPrimary == b.dashboardTextPrimary
                && a.hudChartLine == b.hudChartLine
                && a.customBackgroundEnabled == b.customBackgroundEnabled
                && java.util.Objects.equals(a.customBackgroundFile, b.customBackgroundFile)
                && a.customBackgroundOpacity == b.customBackgroundOpacity
                && java.util.Objects.equals(a.customBackgroundFit, b.customBackgroundFit);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static int alphaOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    static String toHexRgb(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    static int fromHexRgb(String hex, int alpha, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String raw = hex.trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        if (raw.length() != 6) {
            return fallback;
        }
        try {
            int rgb = Integer.parseInt(raw, 16);
            return ((alpha & 0xFF) << 24) | rgb;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalizeFit(String raw) {
        if (raw == null) {
            return "fill";
        }
        String fit = raw.trim().toLowerCase(Locale.ROOT);
        if ("fit".equals(fit) || "stretch".equals(fit) || "fill".equals(fit)) {
            return fit;
        }
        return "fill";
    }
}
