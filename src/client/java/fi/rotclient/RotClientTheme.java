package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared Rot Client black / red / violet visual palette. {@link #DEFAULT_*}
 * constants are the brand defaults; mutable fields are the active runtime
 * appearance and are updated through {@link #apply(RotClientAppearanceConfig)}.
 */
final class RotClientTheme {
    static final int DEFAULT_BACKDROP = 0xF208080A;
    static final int DEFAULT_SURFACE = 0xF518141C;
    static final int DEFAULT_SURFACE_ALT = 0xF5221A28;
    static final int DEFAULT_FIELD = 0xF50C0A10;
    static final int DEFAULT_FIELD_ACTIVE = 0xF5322040;
    static final int DEFAULT_BORDER = 0xFF4A3858;
    static final int DEFAULT_BORDER_BRIGHT = 0xFFE11D48;
    static final int DEFAULT_VIOLET = 0xFFA855F7;
    static final int DEFAULT_TEXT = 0xFFF8F5FF;
    static final int DEFAULT_TEXT_DIM = 0xFFE6DDF0;
    static final int DEFAULT_TEXT_MUTED = 0xFFC8BFD8;
    static final int DEFAULT_SUCCESS = 0xFFC4B5FD;
    static final int DEFAULT_WARNING = 0xFFF4C06A;
    static final int DEFAULT_ERROR = 0xFFFF4D6D;
    static final int DEFAULT_STATUS_READY = 0xFF22C55E;
    static final int DEFAULT_STATUS_NEEDS_TESTING = 0xFFFACC15;
    static final int DEFAULT_STATUS_WORK_IN_PROGRESS = 0xFFF97316;
    static final int DEFAULT_STATUS_UPCOMING = 0xFFEF4444;
    static final int DEFAULT_HUD_BACKGROUND = 0xF5100C12;
    static final int DEFAULT_HUD_HEADER = 0xF524182C;
    static final int DEFAULT_HUD_PANEL = 0xFF18141C;
    static final int DEFAULT_HUD_PANEL_ALT = 0xFF221A28;
    static final int DEFAULT_HUD_ACCENT = DEFAULT_BORDER_BRIGHT;
    static final int DEFAULT_DIVIDER = 0xFF3A2A48;
    static final int DEFAULT_SELECTED_ROW = 0xFF3B1530;
    static final int DEFAULT_HOVER_ROW = 0xFF2A1A32;
    static final int DEFAULT_BUTTON = 0xFF2A1828;
    static final int DEFAULT_BUTTON_HOVER = 0xFF3D2048;
    static final int DEFAULT_BUTTON_DISABLED = 0xFF140E16;
    static final int DEFAULT_SHADOW = 0x99000000;
    static final int DEFAULT_CHART_LINE = DEFAULT_BORDER_BRIGHT;
    /** Soft stroke halo for rate charts. */
    static final int DEFAULT_CHART_GLOW = 0x55E11D48;
    /** Quiet grid lines with low visual emphasis. */
    static final int DEFAULT_CHART_GRID = 0x284A3858;
    /** Chart plot well — deep black with light translucency. */
    static final int DEFAULT_CHART_WELL = 0x6608080A;
    /** Soft area fill under chart curves. */
    static final int DEFAULT_CHART_FILL = 0x38A855F7;
    static final int DEFAULT_TOGGLE_OFF = 0xFF2A2434;

    /** Near-black blue-slate backdrop with translucent coverage. */
    static int BACKDROP = DEFAULT_BACKDROP;
    /** Primary surface/panel fill. */
    static int SURFACE = DEFAULT_SURFACE;
    /** Alternate card/surface fill. */
    static int SURFACE_ALT = DEFAULT_SURFACE_ALT;
    /** Dashboard header strip. */
    static int DASHBOARD_HEADER = DEFAULT_HUD_HEADER;
    /** Dashboard sidebar fill. */
    static int DASHBOARD_SIDEBAR = DEFAULT_SURFACE_ALT;
    /** Input/field background. */
    static int FIELD = DEFAULT_FIELD;
    /** Focused input/field background. */
    static int FIELD_ACTIVE = DEFAULT_FIELD_ACTIVE;
    /** Standard border. */
    static int BORDER = DEFAULT_BORDER;
    /** Selected/focused bright border accent. */
    static int BORDER_BRIGHT = DEFAULT_BORDER_BRIGHT;
    /** Primary off-white text. */
    static int TEXT = DEFAULT_TEXT;
    /** Secondary cool-gray text. */
    static int TEXT_DIM = DEFAULT_TEXT_DIM;
    /** Muted blue-gray text. */
    static int TEXT_MUTED = DEFAULT_TEXT_MUTED;
    /** Semantic success (not brand). */
    static int SUCCESS = DEFAULT_SUCCESS;
    /** Semantic warning (not brand). */
    static int WARNING = DEFAULT_WARNING;
    /** Semantic error (not brand). */
    static int ERROR = DEFAULT_ERROR;
    static int STATUS_READY = DEFAULT_STATUS_READY;
    static int STATUS_NEEDS_TESTING = DEFAULT_STATUS_NEEDS_TESTING;
    static int STATUS_WORK_IN_PROGRESS = DEFAULT_STATUS_WORK_IN_PROGRESS;
    static int STATUS_UPCOMING = DEFAULT_STATUS_UPCOMING;

    /** Translucent HUD card background derived from SURFACE. */
    static int HUD_BACKGROUND = DEFAULT_HUD_BACKGROUND;
    /** HUD header strip derived from SURFACE_ALT. */
    static int HUD_HEADER = DEFAULT_HUD_HEADER;
    /** HUD panel fill. */
    static int HUD_PANEL = DEFAULT_HUD_PANEL;
    /** HUD alternate panel fill. */
    static int HUD_PANEL_ALT = DEFAULT_HUD_PANEL_ALT;
    /** HUD border. */
    static int HUD_BORDER = DEFAULT_BORDER;
    /** HUD title text. */
    static int HUD_TITLE = DEFAULT_TEXT;
    /** HUD primary text. */
    static int HUD_TEXT = DEFAULT_TEXT;
    /** HUD secondary text. */
    static int HUD_TEXT_DIM = DEFAULT_TEXT_DIM;
    /** HUD title/accent line uses the red interaction accent. */
    static int HUD_ACCENT = DEFAULT_HUD_ACCENT;
    /** Secondary violet used for HUD controls and selected chips. */
    static int VIOLET = DEFAULT_VIOLET;
    /** Divider derived from BORDER at reduced emphasis. */
    static int DIVIDER = DEFAULT_DIVIDER;
    /** Selected module/row fill. */
    static int SELECTED_ROW = DEFAULT_SELECTED_ROW;
    /** Hover row fill. */
    static int HOVER_ROW = DEFAULT_HOVER_ROW;
    /** Primary button fill. */
    static int BUTTON = DEFAULT_BUTTON;
    /** Primary button hover/focus fill. */
    static int BUTTON_HOVER = DEFAULT_BUTTON_HOVER;
    /** Disabled button fill. */
    static int BUTTON_DISABLED = DEFAULT_BUTTON_DISABLED;
    /** Button label text. */
    static int BUTTON_TEXT = DEFAULT_TEXT;
    /** Soft drop-shadow used behind dashboard panels. */
    static int SHADOW = DEFAULT_SHADOW;
    /** Chart/accent line for rate graphs. */
    static int CHART_LINE = DEFAULT_CHART_LINE;
    /** Soft chart glow derived from CHART_LINE. */
    static int CHART_GLOW = DEFAULT_CHART_GLOW;
    /** Graph grid line. */
    static int CHART_GRID = DEFAULT_CHART_GRID;
    /** Translucent rate-graph well fill derived from BACKDROP RGB. */
    static int CHART_WELL = DEFAULT_CHART_WELL;
    /** Soft area fill under chart curves. */
    static int CHART_FILL = DEFAULT_CHART_FILL;
    /** Toggle track when off. */
    static int TOGGLE_OFF = DEFAULT_TOGGLE_OFF;
    /** Toggle knob. */
    static int TOGGLE_KNOB = DEFAULT_TEXT;

    private static RotClientAppearanceConfig activeConfig =
            RotClientAppearanceConfig.defaults();

    private RotClientTheme() {
    }

    static RotClientAppearanceConfig activeConfig() {
        return activeConfig;
    }

    static void apply(RotClientAppearanceConfig config) {
        RotClientAppearanceConfig safe = config == null
                ? RotClientAppearanceConfig.defaults()
                : config.copy();
        safe.normalize();
        activeConfig = safe;

        BACKDROP = safe.dashboardBackdrop;
        SURFACE = safe.dashboardSurface;
        SURFACE_ALT = safe.dashboardSurfaceAlt;
        DASHBOARD_HEADER = safe.dashboardHeader;
        DASHBOARD_SIDEBAR = safe.dashboardSidebar;
        FIELD = safe.dashboardField;
        FIELD_ACTIVE = safe.dashboardFieldActive;
        BORDER = safe.dashboardBorder;
        BORDER_BRIGHT = safe.dashboardAccent;
        TEXT = safe.dashboardTextPrimary;
        TEXT_DIM = safe.dashboardTextSecondary;
        TEXT_MUTED = safe.dashboardSectionTitle;
        SUCCESS = safe.dashboardTextSuccess;
        WARNING = safe.dashboardTextWarning;
        ERROR = safe.dashboardError;
        HUD_BACKGROUND = safe.hudBackground;
        HUD_HEADER = safe.hudHeader;
        HUD_PANEL = safe.hudPanel;
        HUD_PANEL_ALT = safe.hudPanelAlt;
        HUD_BORDER = safe.hudBorder;
        HUD_TITLE = safe.hudTitle;
        HUD_TEXT = safe.hudTextPrimary;
        HUD_TEXT_DIM = safe.hudTextSecondary;
        HUD_ACCENT = safe.hudAccent;
        VIOLET = DEFAULT_VIOLET;
        DIVIDER = safe.dashboardDivider;
        SELECTED_ROW = safe.dashboardButtonSelected;
        HOVER_ROW = safe.dashboardHoverRow;
        BUTTON = safe.dashboardButton;
        BUTTON_HOVER = safe.dashboardButtonHover;
        BUTTON_DISABLED = DEFAULT_BUTTON_DISABLED;
        BUTTON_TEXT = safe.dashboardButtonText;
        SHADOW = safe.dashboardShadow;
        CHART_LINE = safe.hudChartLine;
        CHART_GLOW = safe.hudChartGlow;
        CHART_GRID = safe.hudChartGrid;
        CHART_WELL = safe.hudChartWell;
        CHART_FILL = safe.hudChartFill;
        TOGGLE_OFF = DEFAULT_TOGGLE_OFF;
        TOGGLE_KNOB = safe.dashboardTextPrimary;
    }

    static void resetToDefaults() {
        apply(RotClientAppearanceConfig.defaults());
    }

    static void drawBackdrop(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height) {
        graphics.fill(x, y, x + width, y + height, BACKDROP);
    }

    static void drawPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height) {
        graphics.fill(x, y, x + width, y + height, SURFACE);
        drawOutline(graphics, x, y, width, height, BORDER);
    }

    static void drawInset(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean focused) {
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                focused ? FIELD_ACTIVE : FIELD);
        drawOutline(
                graphics,
                x,
                y,
                width,
                height,
                focused ? BORDER_BRIGHT : BORDER);
    }

    static void drawHeader(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height) {
        graphics.fill(x, y, x + width, y + height, HUD_HEADER);
        drawOutline(graphics, x, y, width, height, BORDER);
    }

    static void drawDivider(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width) {
        graphics.fill(x, y, x + width, y + 1, DIVIDER);
    }

    static void drawOutline(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
