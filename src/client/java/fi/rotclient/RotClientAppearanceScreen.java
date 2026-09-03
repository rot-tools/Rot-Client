package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Appearance & GUI Customizer for dashboard and mining HUD visuals.
 */
final class RotClientAppearanceScreen extends Screen {
    private static final int PANEL_WIDTH = RotClientDashboardLayout.BASE_WIDTH;
    private static final int PANEL_HEIGHT = 420;
    private static final int HEADER_HEIGHT = RotClientDashboardLayout.HEADER_HEIGHT;
    private static final int SIDEBAR_WIDTH = RotClientDashboardLayout.SIDEBAR_WIDTH;

    enum Section {
        OVERVIEW,
        DASHBOARD,
        MINING_HUD,
        COLORS,
        BACKGROUND,
        CHARTS,
        RESET
    }

    private final Screen parent;
    private final TrackerConfig trackerConfig;
    private final RotClientAppearanceConfig working;
    private final RotClientAppearanceConfig defaults =
            RotClientAppearanceConfig.defaults();
    private Section section = Section.OVERVIEW;
    private Section resetTargetSection = Section.DASHBOARD;
    private final RotClientScrollState scroll = new RotClientScrollState();
    private final RotClientExpandState accordion = new RotClientExpandState();
    private boolean awaitingResetAllConfirm = false;
    private boolean awaitingLayoutResetConfirm = false;
    private boolean awaitingHudVisibilityResetConfirm = false;
    private String statusMessage = "";

    RotClientAppearanceScreen(
            Screen parent,
            TrackerConfig trackerConfig,
            RotClientHud hud) {
        this(parent, trackerConfig, hud, Section.OVERVIEW);
    }

    RotClientAppearanceScreen(
            Screen parent,
            TrackerConfig trackerConfig,
            RotClientHud hud,
            Section initialSection) {
        super(Component.literal("Appearance & GUI Customizer"));
        this.parent = parent;
        this.trackerConfig = trackerConfig;
        // hud retained in constructor for call-site compatibility; not used yet.
        if (hud == null) {
            throw new IllegalArgumentException("hud cannot be null");
        }
        this.working = RotClientTheme.activeConfig().copy();
        this.working.normalize();
        Section start = initialSection == null ? Section.OVERVIEW : initialSection;
        this.section = start == Section.MINING_HUD ? Section.COLORS : start;
    }

    private boolean expanded(String sectionId) {
        return RotClientClient.workspace().isAppearanceSectionExpanded(sectionId);
    }

    private void toggleExpanded(String sectionId) {
        RotClientClient.workspace().toggleAppearanceSection(sectionId);
        // Collapsing shrinks content; clamp so restore never leaves an
        // invalid scroll past the new max.
        scroll.setBounds(scroll.contentHeight(), scroll.viewportHeight());
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        RotClientUiClock.beginFrame(minecraft);
        float uiScale = uiScale();
        int logicalMouseX = Math.round(mouseX / uiScale);
        int logicalMouseY = Math.round(mouseY / uiScale);
        graphics.pose().pushMatrix();
        graphics.pose().scale(uiScale, uiScale);
        RotClientUiDraw.beginHoverFrame(logicalMouseX, logicalMouseY);
        accordion.syncTargets(
                RotClientClient.workspace().expandedAppearanceSections(),
                RotClientAppearanceNav.defaultExpandedSections());
        accordion.advanceSeconds(RotClientUiClock.seconds());

        int logicalWidth = Math.round(width / uiScale);
        int logicalHeight = Math.round(height / uiScale);
        RotClientUiDraw.drawScrim(graphics, 0, 0, logicalWidth, logicalHeight);

        int panelX = panelX();
        int panelY = panelY();
        RotClientBackgroundManager.drawIfEnabled(
                graphics,
                panelX,
                panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                working);
        RotClientUiDraw.drawShadowedPanel(
                graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics,
                font,
                panelX,
                panelY,
                PANEL_WIDTH,
                HEADER_HEIGHT,
                "Appearance",
                sectionLabel(section),
                true);
        RotClientUiDraw.drawBackButton(
                graphics,
                font,
                logicalMouseX,
                logicalMouseY,
                panelX + 8,
                panelY + 8);

        graphics.fill(
                panelX + 1,
                panelY + HEADER_HEIGHT,
                panelX + SIDEBAR_WIDTH,
                panelY + PANEL_HEIGHT - 1,
                RotClientTheme.DASHBOARD_SIDEBAR);
        graphics.fill(
                panelX + SIDEBAR_WIDTH,
                panelY + HEADER_HEIGHT,
                panelX + SIDEBAR_WIDTH + 1,
                panelY + PANEL_HEIGHT - 1,
                RotClientTheme.DIVIDER);

        drawSidebar(graphics, panelX, panelY, logicalMouseX, logicalMouseY);
        drawContent(graphics, panelX, panelY, logicalMouseX, logicalMouseY);
        RotClientUiDraw.drawHoverFrame(
                graphics, font, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT);

        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSidebar(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        int y = panelY + 56;
        y = drawNav(graphics, mouseX, mouseY, panelX, y, Section.OVERVIEW, "Overview");
        y = drawNav(graphics, mouseX, mouseY, panelX, y, Section.DASHBOARD, "Dashboard");
        y = drawNav(graphics, mouseX, mouseY, panelX, y, Section.COLORS, "Colors");
        y = drawNav(graphics, mouseX, mouseY, panelX, y, Section.BACKGROUND, "Background");
        y = drawNav(graphics, mouseX, mouseY, panelX, y, Section.CHARTS, "Charts");
        drawNav(graphics, mouseX, mouseY, panelX, y, Section.RESET, "Reset & Defaults");

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                panelX + 10,
                panelY + PANEL_HEIGHT - 42,
                SIDEBAR_WIDTH - 20,
                "← Back",
                false,
                true);
    }

    private int drawNav(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int panelX,
            int y,
            Section target,
            String label) {
        boolean selected = section == target;
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, panelX + 10, y, SIDEBAR_WIDTH - 20, 28);
        int fill = selected
                ? RotClientTheme.SELECTED_ROW
                : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.DASHBOARD_SIDEBAR);
        RotClientUiDraw.roundedFill(
                graphics, panelX + 10, y, panelX + SIDEBAR_WIDTH - 10, y + 28, fill);
        if (selected) {
            graphics.fill(
                    panelX + 10,
                    y + 5,
                    panelX + 13,
                    y + 23,
                    RotClientTheme.HUD_ACCENT);
        }
        RotClientUiDraw.text(graphics, font,
                label,
                panelX + 20,
                y + 10,
                RotClientTheme.TEXT,
                selected);
        return y + 32;
    }

    private void drawContent(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        int contentLeft = panelX + SIDEBAR_WIDTH + 14;
        int contentRight = panelX + PANEL_WIDTH - 14;
        int contentTop = panelY + HEADER_HEIGHT + 12;
        int contentBottom = panelY + PANEL_HEIGHT - 52;
        int viewport = contentBottom - contentTop;
        scroll.setViewportHeight(viewport);
        scroll.advanceSeconds(RotClientUiClock.seconds());

        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, scroll);
        int y = contentTop - scroll.scrollPixels();
        int startY = y;
        try {
        switch (section) {
            case OVERVIEW -> y = drawOverview(
                    graphics, mouseX, mouseY, contentLeft, contentRight, y);
            case DASHBOARD -> y = drawDashboardSection(
                    graphics, mouseX, mouseY, contentLeft, contentRight, y,
                    contentTop, contentBottom);
            case MINING_HUD, COLORS -> y = drawColorsSection(
                    graphics, mouseX, mouseY, contentLeft, contentRight, y,
                    contentTop, contentBottom);
            case BACKGROUND -> y = drawBackgroundSection(
                    graphics, mouseX, mouseY, contentLeft, contentRight, y,
                    contentTop, contentBottom);
            case CHARTS -> y = drawChartsSection(
                    graphics, mouseX, mouseY, contentLeft, contentRight, y,
                    contentTop, contentBottom);
            case RESET -> y = drawResetSection(
                    graphics, mouseX, mouseY, contentLeft, contentRight, y);
        }
        // Content height = drawn span; must NOT re-add scrollPixels (that caused
        // infinite scroll as offset grew).
        scroll.setBounds(
                RotClientScrollState.measureContentHeight(startY, y + 8),
                viewport);
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }

        if (scroll.canScroll()) {
            int scrollbarX = contentRight + 2;
            boolean hovered = RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    scrollbarX,
                    contentTop,
                    RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                    viewport);
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    scrollbarX,
                    contentTop,
                    contentBottom,
                    scroll.contentHeight(),
                    scroll.scrollPixels(),
                    hovered,
                    scroll.isThumbDragging());
        }
        if (!statusMessage.isBlank()) {
            RotClientUiDraw.text(graphics, font,
                    statusMessage,
                    contentLeft,
                    contentBottom + 4,
                    RotClientTheme.SUCCESS,
                    false);
        }

        int buttonY = panelY + PANEL_HEIGHT - 36;
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                contentRight - 260, buttonY, 80, "Apply", true, true);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                contentRight - 170, buttonY, 80, "Save", true, true);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                contentRight - 80, buttonY, 80, "Close", false, true);
    }

    private int drawOverview(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y) {
        RotClientUiDraw.text(graphics, font, "Customize Rot Client visuals", left, y, RotClientTheme.TEXT, true);
        y += 14;
        RotClientUiDraw.text(graphics, font,
                "Role-based colors cover dashboard panels, text, buttons, and HUD surfaces.",
                left, y, RotClientTheme.TEXT_DIM, false);
        y += 14;
        RotClientUiDraw.text(graphics, font,
                "Default theme uses readable blue-slate surfaces with a teal interaction accent.",
                left, y, RotClientTheme.TEXT_MUTED, false);
        y += 22;
        y = drawInfoCard(graphics, mouseX, mouseY, left, right, y,
                "Dashboard", "Panels, sidebar, headers, borders, buttons");
        y = drawInfoCard(graphics, mouseX, mouseY, left, right, y,
                "Colors", "Dashboard text, HUD surfaces, and accents");
        y = drawInfoCard(graphics, mouseX, mouseY, left, right, y,
                "Charts", "Rate sparkline line, glow, fill, grid, and well");
        y = drawInfoCard(graphics, mouseX, mouseY, left, right, y,
                "Background",
                "Optional images from config/rotclient/backgrounds/");
        return y + 8;
    }

    private int drawDashboardSection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom) {
        y = drawGroupHeader(graphics, mouseX, mouseY, left, right, y,
                "Dashboard surfaces",
                accordion.amount(RotClientAppearanceNav.DASHBOARD_BASICS));
        if (expanded(RotClientAppearanceNav.DASHBOARD_BASICS)) {
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Main background", () -> working.dashboardBackdrop,
                    v -> working.dashboardBackdrop = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Main panel", () -> working.dashboardSurface,
                    v -> working.dashboardSurface = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Secondary panel / card", () -> working.dashboardSurfaceAlt,
                    v -> working.dashboardSurfaceAlt = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Header background", () -> working.dashboardHeader,
                    v -> working.dashboardHeader = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Sidebar background", () -> working.dashboardSidebar,
                    v -> working.dashboardSidebar = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Border", () -> working.dashboardBorder,
                    v -> working.dashboardBorder = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Accent / highlight", () -> working.dashboardAccent,
                    v -> working.dashboardAccent = v);
        }
        return y + 8;
    }

    private int drawHudSection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom) {
        y = drawGroupHeader(graphics, mouseX, mouseY, left, right, y,
                "Mining HUD surfaces",
                accordion.amount(RotClientAppearanceNav.HUD_BASICS));
        if (expanded(RotClientAppearanceNav.HUD_BASICS)) {
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD background", () -> working.hudBackground,
                    v -> working.hudBackground = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD panel / card", () -> working.hudPanel,
                    v -> working.hudPanel = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD alternate panel", () -> working.hudPanelAlt,
                    v -> working.hudPanelAlt = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD header", () -> working.hudHeader,
                    v -> working.hudHeader = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD border", () -> working.hudBorder,
                    v -> working.hudBorder = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD accent", () -> working.hudAccent,
                    v -> working.hudAccent = v);
        }
        return y + 8;
    }

    private int drawColorsSection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom) {
        y = drawGroupHeader(graphics, mouseX, mouseY, left, right, y,
                "HUD surfaces",
                accordion.amount(RotClientAppearanceNav.HUD_BASICS));
        if (expanded(RotClientAppearanceNav.HUD_BASICS)) {
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD background", () -> working.hudBackground,
                    v -> working.hudBackground = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD panel / card", () -> working.hudPanel,
                    v -> working.hudPanel = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD alternate panel", () -> working.hudPanelAlt,
                    v -> working.hudPanelAlt = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD header", () -> working.hudHeader,
                    v -> working.hudHeader = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD border", () -> working.hudBorder,
                    v -> working.hudBorder = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD accent", () -> working.hudAccent,
                    v -> working.hudAccent = v);
        }
        y += 6;
        y = drawGroupHeader(graphics, mouseX, mouseY, left, right, y,
                "Dashboard text",
                accordion.amount(RotClientAppearanceNav.DASHBOARD_TEXT));
        if (expanded(RotClientAppearanceNav.DASHBOARD_TEXT)) {
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Section title", () -> working.dashboardSectionTitle,
                    v -> working.dashboardSectionTitle = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Primary text", () -> working.dashboardTextPrimary,
                    v -> working.dashboardTextPrimary = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Secondary / muted text", () -> working.dashboardTextSecondary,
                    v -> working.dashboardTextSecondary = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Positive / enabled", () -> working.dashboardTextSuccess,
                    v -> working.dashboardTextSuccess = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Warning / experimental", () -> working.dashboardTextWarning,
                    v -> working.dashboardTextWarning = v);
        }
        y += 6;
        y = drawGroupHeader(graphics, mouseX, mouseY, left, right, y,
                "HUD text",
                accordion.amount(RotClientAppearanceNav.HUD_TEXT));
        if (expanded(RotClientAppearanceNav.HUD_TEXT)) {
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD title", () -> working.hudTitle,
                    v -> working.hudTitle = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD primary text", () -> working.hudTextPrimary,
                    v -> working.hudTextPrimary = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "HUD secondary text", () -> working.hudTextSecondary,
                    v -> working.hudTextSecondary = v);
        }
        y += 6;
        y = drawGroupHeader(graphics, mouseX, mouseY, left, right, y,
                "Buttons & borders",
                accordion.amount(RotClientAppearanceNav.BUTTONS_BORDERS));
        if (expanded(RotClientAppearanceNav.BUTTONS_BORDERS)) {
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Button background", () -> working.dashboardButton,
                    v -> working.dashboardButton = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Button hover", () -> working.dashboardButtonHover,
                    v -> working.dashboardButtonHover = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Button selected", () -> working.dashboardButtonSelected,
                    v -> working.dashboardButtonSelected = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Button text", () -> working.dashboardButtonText,
                    v -> working.dashboardButtonText = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Field background", () -> working.dashboardField,
                    v -> working.dashboardField = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Field focused", () -> working.dashboardFieldActive,
                    v -> working.dashboardFieldActive = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Hover row", () -> working.dashboardHoverRow,
                    v -> working.dashboardHoverRow = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Divider", () -> working.dashboardDivider,
                    v -> working.dashboardDivider = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Shadow", () -> working.dashboardShadow,
                    v -> working.dashboardShadow = v);
            y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Error", () -> working.dashboardError,
                    v -> working.dashboardError = v);
        }
        return y + 8;
    }

    private int drawChartsSection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom) {
        if (scroll.intersects(y, 40, clipTop, clipBottom)) {
            RotClientUiDraw.text(graphics, font, "Rate chart colors", left, y, RotClientTheme.TEXT, true);
            RotClientUiDraw.text(graphics, font,
                    "Line and glow form the sparkline; fill shades under it.",
                    left, y + 12, RotClientTheme.TEXT_MUTED, false);
            RotClientUiDraw.text(graphics, font,
                    "Grid guides the plot; well is the chart background.",
                    left, y + 24, RotClientTheme.TEXT_MUTED, false);
        }
        y += 42;

        y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                "Rate chart line", () -> working.hudChartLine,
                v -> working.hudChartLine = v);
        y = helpLine(graphics, left, y, clipTop, clipBottom,
                "Primary stroke of the mining rate sparkline.");
        y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                "Rate chart glow", () -> working.hudChartGlow,
                v -> working.hudChartGlow = v);
        y = helpLine(graphics, left, y, clipTop, clipBottom,
                "Soft bloom drawn around the rate line.");
        y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                "Rate chart fill", () -> working.hudChartFill,
                v -> working.hudChartFill = v);
        y = helpLine(graphics, left, y, clipTop, clipBottom,
                "Area fill beneath the rate line.");
        y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                "Rate chart grid", () -> working.hudChartGrid,
                v -> working.hudChartGrid = v);
        y = helpLine(graphics, left, y, clipTop, clipBottom,
                "Horizontal/vertical guide lines inside the chart.");
        y = colorRow(graphics, mouseX, mouseY, left, right, y, clipTop, clipBottom,
                "Rate chart well", () -> working.hudChartWell,
                v -> working.hudChartWell = v);
        y = helpLine(graphics, left, y, clipTop, clipBottom,
                "Background well behind the rate chart.");
        return y + 8;
    }

    private int drawBackgroundSection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom) {
        if (scroll.intersects(y, 34, clipTop, clipBottom)) {
            boolean hover = RotClientUiDraw.inside(
                    mouseX, mouseY, left, y, right - left, 34);
            RotClientUiDraw.drawCard(graphics, left, y, right - left, 34, hover);
            RotClientUiDraw.text(graphics, font, "Enable custom background",
                    left + 10, y + 6, RotClientTheme.TEXT, true);
            RotClientUiDraw.text(graphics, font,
                    working.customBackgroundEnabled ? "Enabled" : "Disabled",
                    left + 10, y + 18,
                    working.customBackgroundEnabled
                            ? RotClientTheme.SUCCESS
                            : RotClientTheme.TEXT_MUTED,
                    false);
        }
        y += 40;

        if (scroll.intersects(y, 12, clipTop, clipBottom)) {
            RotClientUiDraw.text(graphics, font,
                    RotClientBackgroundManager.backgroundsPathHint(),
                    left, y, RotClientTheme.TEXT_MUTED, false);
        }
        y += 14;

        List<String> files = RotClientBackgroundManager.listBackgroundFiles();
        if (files.isEmpty()) {
            if (scroll.intersects(y, 28, clipTop, clipBottom)) {
                RotClientUiDraw.text(graphics, font, "No images found yet.",
                        left, y, RotClientTheme.WARNING, false);
            }
            y += 24;
        } else {
            for (String file : files) {
                if (scroll.intersects(y, 24, clipTop, clipBottom)) {
                    boolean selected = file.equals(working.customBackgroundFile);
                    RotClientUiDraw.drawCard(
                            graphics, left, y, right - left, 22, selected);
                    RotClientUiDraw.text(graphics, font, file, left + 8, y + 7,
                            selected ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM,
                            selected);
                }
                y += 26;
            }
        }

        if (scroll.intersects(y, 22, clipTop, clipBottom)) {
            RotClientUiDraw.text(graphics, font,
                    "Opacity " + working.customBackgroundOpacity
                            + "  ·  Dim " + working.customBackgroundDim
                            + "  ·  Fit " + working.customBackgroundFit,
                    left, y, RotClientTheme.TEXT_DIM, false);
        }
        y += 20;
        if (scroll.intersects(y, 24, clipTop, clipBottom)) {
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left, y, 120, "Opacity -", false, true);
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left + 128, y, 120, "Opacity +", false, true);
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left + 256, y, 100, "Dim -", false, true);
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left + 364, y, 100, "Dim +", false, true);
        }
        y += 30;
        if (scroll.intersects(y, 24, clipTop, clipBottom)) {
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left, y, 90, "Fill", false, true);
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left + 98, y, 90, "Fit", false, true);
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left + 196, y, 100, "Stretch", false, true);
        }
        y += 30;
        if (scroll.intersects(y, 24, clipTop, clipBottom)) {
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left, y, 140, "Open Folder", false, true);
            RotClientUiDraw.drawButton(
                    graphics, font, mouseX, mouseY,
                    left + 150, y, 120, "Refresh", false, true);
        }
        return y + 36;
    }

    private int drawResetSection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y) {
        RotClientUiDraw.text(graphics, font, "Restore defaults for appearance only.",
                left, y, RotClientTheme.TEXT_DIM, false);
        y += 14;
        RotClientUiDraw.text(graphics, font,
                "Tracker, session, and history settings are never changed here.",
                left, y, RotClientTheme.TEXT_MUTED, false);
        y += 18;
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                left, y, 280,
                "Reset current section (" + sectionLabel(resetTargetSection) + ")",
                false, true);
        y += 34;
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                left, y, 220, "Reset all appearance", true, true);
        y += 34;
        if (awaitingResetAllConfirm) {
            RotClientUiDraw.text(graphics, font,
                    "Click again to confirm reset all",
                    left, y, RotClientTheme.WARNING, false);
            y += 14;
        }
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                left, y, 220,
                awaitingLayoutResetConfirm
                        ? "Confirm reset UI positions"
                        : "Reset UI Positions",
                false, true);
        y += 34;
        if (awaitingLayoutResetConfirm) {
            RotClientUiDraw.text(graphics, font,
                    "Click again to reset Client UI + Mining HUD positions",
                    left, y, RotClientTheme.WARNING, false);
            y += 14;
        }
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                left, y, 220,
                awaitingHudVisibilityResetConfirm
                        ? "Confirm reset HUD visibility"
                        : "Reset HUD Visibility",
                false, true);
        y += 34;
        if (awaitingHudVisibilityResetConfirm) {
            RotClientUiDraw.text(graphics, font,
                    "Click again to restore all HUD show toggles",
                    left, y, RotClientTheme.WARNING, false);
            y += 14;
        }
        RotClientUiDraw.text(graphics, font,
                "Reset all also disables the custom background.",
                left, y, RotClientTheme.TEXT_MUTED, false);
        return y + 20;
    }

    private int drawInfoCard(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            String title,
            String body) {
        int width = right - left;
        boolean hover = RotClientUiDraw.inside(mouseX, mouseY, left, y, width, 40);
        RotClientUiDraw.drawAccentCard(graphics, left, y, width, 40, hover);
        if (hover) {
            RotClientUiDraw.roundedOutline(
                    graphics, left, y, right, y + 40,
                    RotClientTheme.BORDER_BRIGHT, RotClientUiDraw.RADIUS_SM);
        }
        RotClientUiDraw.text(graphics, font, title, left + 12, y + 8, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, body, left + 12, y + 22, RotClientTheme.TEXT_MUTED, false);
        RotClientUiDraw.text(
                graphics,
                font,
                "Open →",
                right - 12 - font.width("Open →"),
                y + 14,
                RotClientTheme.TEXT_DIM,
                false);
        return y + 48;
    }

    private int drawGroupHeader(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            String title,
            double openAmount) {
        boolean hover = RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22);
        RotClientUiDraw.drawCard(graphics, left, y, right - left, 22, hover);
        RotClientUiDraw.drawChevron(
                graphics,
                left + 8,
                y + 5,
                openAmount,
                hover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED);
        RotClientUiDraw.glyph(
                graphics,
                font,
                title,
                left + 24,
                y + 7,
                RotClientTheme.TEXT,
                true);
        return y + 28;
    }

    private int helpLine(
            GuiGraphicsExtractor graphics,
            int left,
            int y,
            int clipTop,
            int clipBottom,
            String text) {
        if (scroll.intersects(y, 12, clipTop, clipBottom)) {
            RotClientUiDraw.text(graphics, font, text, left + 8, y, RotClientTheme.TEXT_MUTED, false);
        }
        return y + 14;
    }

    private int colorRow(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom,
            String label,
            IntSupplier getter,
            IntConsumer ignoredSetter) {
        if (!scroll.intersects(y, RotClientColorRowLayout.ROW_HEIGHT, clipTop, clipBottom)) {
            return y + RotClientColorRowLayout.ROW_STEP;
        }
        int color = getter.getAsInt();
        RotClientColorRowLayout.Columns columns =
                RotClientColorRowLayout.compute(left, right, y);
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, left, y, right - left, RotClientColorRowLayout.ROW_HEIGHT);
        RotClientUiDraw.drawCard(
                graphics, left, y, right - left, RotClientColorRowLayout.ROW_HEIGHT, hover);
        RotClientUiDraw.text(graphics, font,
                RotClientUiDraw.ellipsizeAndHover(
                        font, label, columns.labelMaxWidth(), columns.labelLeft(), y + 6, 12),
                columns.labelLeft(),
                y + 8,
                RotClientTheme.TEXT_DIM,
                false);
        RotClientUiDraw.text(graphics, font,
                RotClientAppearanceConfig.toHexRgb(color),
                columns.hexX(),
                y + 8,
                RotClientTheme.TEXT_MUTED,
                false);
        RotClientUiDraw.drawColorSwatch(
                graphics,
                columns.swatchX(),
                columns.controlTop(),
                RotClientColorRowLayout.SWATCH_WIDTH,
                RotClientColorRowLayout.SWATCH_HEIGHT,
                color,
                hover);
        // Keep swatch height consistent via fill clip of picker size row.
        RotClientUiDraw.drawRainbowSwatch(
                graphics,
                columns.pickerX(),
                columns.controlTop(),
                RotClientColorRowLayout.PICKER_SIZE);
        return y + RotClientColorRowLayout.ROW_STEP;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        float uiScale = uiScale();
        int mouseX = Math.round((float) event.x() / uiScale);
        int mouseY = Math.round((float) event.y() / uiScale);
        int panelX = panelX();
        int panelY = panelY();
        int contentLeft = panelX + SIDEBAR_WIDTH + 14;
        int contentRight = panelX + PANEL_WIDTH - 14;
        int contentTop = panelY + HEADER_HEIGHT + 12;
        int contentBottom = panelY + PANEL_HEIGHT - 52;
        int buttonY = panelY + PANEL_HEIGHT - 36;

        if (!RotClientUiDraw.inside(
                mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
            onClose();
            return true;
        }
        if (RotClientUiDraw.hitBackButton(mouseX, mouseY, panelX + 8, panelY + 8)) {
            onClose();
            return true;
        }
        if (RotClientUiDraw.inside(
                mouseX, mouseY, panelX + 10, panelY + PANEL_HEIGHT - 42,
                SIDEBAR_WIDTH - 20, RotClientUiDraw.BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, contentRight - 260, buttonY, 80, RotClientUiDraw.BUTTON_HEIGHT)) {
            applyWorking();
            statusMessage = "Applied live preview";
            return true;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, contentRight - 170, buttonY, 80, RotClientUiDraw.BUTTON_HEIGHT)) {
            applyWorking();
            if (RotClientAppearanceStore.save(working)) {
                statusMessage = "Saved appearance";
            } else {
                statusMessage = "Save failed";
            }
            return true;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, contentRight - 80, buttonY, 80, RotClientUiDraw.BUTTON_HEIGHT)) {
            onClose();
            return true;
        }

        int navY = panelY + 56;
        for (Section candidate : navSections()) {
            if (RotClientUiDraw.inside(
                    mouseX, mouseY, panelX + 10, navY, SIDEBAR_WIDTH - 20, 28)) {
                if (candidate != section) {
                    if (section != Section.RESET) {
                        resetTargetSection = section == Section.MINING_HUD
                                ? Section.COLORS
                                : section;
                    }
                    section = candidate;
                    scroll.reset();
                    statusMessage = "";
                    awaitingResetAllConfirm = false;
                    awaitingLayoutResetConfirm = false;
                    awaitingHudVisibilityResetConfirm = false;
                }
                return true;
            }
            navY += 32;
        }

        int scrollbarHitX = contentRight + 2;
        if (scroll.canScroll()
                && RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        scrollbarHitX,
                        contentTop,
                        RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                        contentBottom - contentTop)) {
            if (scroll.beginThumbDrag(
                    mouseY,
                    contentTop,
                    contentBottom,
                    RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)
                    || scroll.clickTrack(
                    mouseY,
                    contentTop,
                    contentBottom,
                    RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {
                return true;
            }
        }

        return handleContentClick(mouseX, mouseY, contentLeft, contentRight, contentTop, contentBottom);
    }

    private boolean handleContentClick(
            int mouseX,
            int mouseY,
            int left,
            int right,
            int clipTop,
            int clipBottom) {
        int y = clipTop - scroll.scrollPixels();
        if (section == Section.OVERVIEW) {
            y += 50;
            Section[] cards = {
                    Section.DASHBOARD,
                    Section.COLORS,
                    Section.CHARTS,
                    Section.BACKGROUND
            };
            for (Section target : cards) {
                if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 40)) {
                    section = target;
                    scroll.reset();
                    statusMessage = "";
                    return true;
                }
                y += 48;
            }
            return true;
        }
        if (section == Section.DASHBOARD) {
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22)) {
                toggleExpanded(RotClientAppearanceNav.DASHBOARD_BASICS);
                return true;
            }
            y += 28;
            if (expanded(RotClientAppearanceNav.DASHBOARD_BASICS)) {
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Main background", () -> working.dashboardBackdrop,
                        v -> working.dashboardBackdrop = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Main panel", () -> working.dashboardSurface,
                        v -> working.dashboardSurface = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Secondary panel / card", () -> working.dashboardSurfaceAlt,
                        v -> working.dashboardSurfaceAlt = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Header background", () -> working.dashboardHeader,
                        v -> working.dashboardHeader = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Sidebar background", () -> working.dashboardSidebar,
                        v -> working.dashboardSidebar = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Border", () -> working.dashboardBorder,
                        v -> working.dashboardBorder = v);
                maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Accent / highlight", () -> working.dashboardAccent,
                        v -> working.dashboardAccent = v);
            }
            return true;
        }
        if (section == Section.MINING_HUD || section == Section.COLORS) {
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22)) {
                toggleExpanded(RotClientAppearanceNav.HUD_BASICS);
                return true;
            }
            y += 28;
            if (expanded(RotClientAppearanceNav.HUD_BASICS)) {
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD background", () -> working.hudBackground,
                        v -> working.hudBackground = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD panel / card", () -> working.hudPanel,
                        v -> working.hudPanel = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD alternate panel", () -> working.hudPanelAlt,
                        v -> working.hudPanelAlt = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD header", () -> working.hudHeader,
                        v -> working.hudHeader = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD border", () -> working.hudBorder,
                        v -> working.hudBorder = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD accent", () -> working.hudAccent,
                        v -> working.hudAccent = v);
            }
            y += 6;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22)) {
                toggleExpanded(RotClientAppearanceNav.DASHBOARD_TEXT);
                return true;
            }
            y += 28;
            if (expanded(RotClientAppearanceNav.DASHBOARD_TEXT)) {
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Section title", () -> working.dashboardSectionTitle,
                        v -> working.dashboardSectionTitle = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Primary text", () -> working.dashboardTextPrimary,
                        v -> working.dashboardTextPrimary = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Secondary / muted text", () -> working.dashboardTextSecondary,
                        v -> working.dashboardTextSecondary = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Positive / enabled", () -> working.dashboardTextSuccess,
                        v -> working.dashboardTextSuccess = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Warning / experimental", () -> working.dashboardTextWarning,
                        v -> working.dashboardTextWarning = v);
            }
            y += 6;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22)) {
                toggleExpanded(RotClientAppearanceNav.HUD_TEXT);
                return true;
            }
            y += 28;
            if (expanded(RotClientAppearanceNav.HUD_TEXT)) {
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD title", () -> working.hudTitle,
                        v -> working.hudTitle = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD primary text", () -> working.hudTextPrimary,
                        v -> working.hudTextPrimary = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "HUD secondary text", () -> working.hudTextSecondary,
                        v -> working.hudTextSecondary = v);
            }
            y += 6;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22)) {
                toggleExpanded(RotClientAppearanceNav.BUTTONS_BORDERS);
                return true;
            }
            y += 28;
            if (expanded(RotClientAppearanceNav.BUTTONS_BORDERS)) {
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Button background", () -> working.dashboardButton,
                        v -> working.dashboardButton = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Button hover", () -> working.dashboardButtonHover,
                        v -> working.dashboardButtonHover = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Button selected", () -> working.dashboardButtonSelected,
                        v -> working.dashboardButtonSelected = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Button text", () -> working.dashboardButtonText,
                        v -> working.dashboardButtonText = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Field background", () -> working.dashboardField,
                        v -> working.dashboardField = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Field focused", () -> working.dashboardFieldActive,
                        v -> working.dashboardFieldActive = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Hover row", () -> working.dashboardHoverRow,
                        v -> working.dashboardHoverRow = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Divider", () -> working.dashboardDivider,
                        v -> working.dashboardDivider = v);
                y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Shadow", () -> working.dashboardShadow,
                        v -> working.dashboardShadow = v);
                maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                        "Error", () -> working.dashboardError,
                        v -> working.dashboardError = v);
            }
            return true;
        }
        if (section == Section.CHARTS) {
            y += 42;
            y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Rate chart line", () -> working.hudChartLine,
                    v -> working.hudChartLine = v);
            y += 14;
            y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Rate chart glow", () -> working.hudChartGlow,
                    v -> working.hudChartGlow = v);
            y += 14;
            y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Rate chart fill", () -> working.hudChartFill,
                    v -> working.hudChartFill = v);
            y += 14;
            y = maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Rate chart grid", () -> working.hudChartGrid,
                    v -> working.hudChartGrid = v);
            y += 14;
            maybeOpenColor(mouseX, mouseY, left, right, y, clipTop, clipBottom,
                    "Rate chart well", () -> working.hudChartWell,
                    v -> working.hudChartWell = v);
            return true;
        }
        if (section == Section.BACKGROUND) {
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 34)) {
                working.customBackgroundEnabled = !working.customBackgroundEnabled;
                applyWorking();
                return true;
            }
            y += 54;
            List<String> files = RotClientBackgroundManager.listBackgroundFiles();
            for (String file : files) {
                if (RotClientUiDraw.inside(mouseX, mouseY, left, y, right - left, 22)) {
                    working.customBackgroundFile = file;
                    working.customBackgroundEnabled = true;
                    RotClientBackgroundManager.invalidateCache();
                    applyWorking();
                    return true;
                }
                y += 26;
            }
            if (files.isEmpty()) {
                y += 24;
            }
            y += 20;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 120, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundOpacity = RotClientAppearanceConfig.clamp(
                        working.customBackgroundOpacity - 15, 0, 255);
                applyWorking();
                return true;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left + 128, y, 120, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundOpacity = RotClientAppearanceConfig.clamp(
                        working.customBackgroundOpacity + 15, 0, 255);
                applyWorking();
                return true;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left + 256, y, 100, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundDim = RotClientAppearanceConfig.clamp(
                        working.customBackgroundDim - 15, 0, 255);
                applyWorking();
                return true;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left + 364, y, 100, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundDim = RotClientAppearanceConfig.clamp(
                        working.customBackgroundDim + 15, 0, 255);
                applyWorking();
                return true;
            }
            y += 30;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 90, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundFit = "fill";
                applyWorking();
                return true;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left + 98, y, 90, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundFit = "fit";
                applyWorking();
                return true;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left + 196, y, 100, RotClientUiDraw.BUTTON_HEIGHT)) {
                working.customBackgroundFit = "stretch";
                applyWorking();
                return true;
            }
            y += 30;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 140, RotClientUiDraw.BUTTON_HEIGHT)) {
                if (RotClientBackgroundManager.openBackgroundsFolder()) {
                    statusMessage = "Opened backgrounds folder";
                } else {
                    statusMessage = "Could not open folder";
                }
                return true;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left + 150, y, 120, RotClientUiDraw.BUTTON_HEIGHT)) {
                RotClientBackgroundManager.invalidateCache();
                RotClientBackgroundManager.listBackgroundFiles();
                statusMessage = "Background list refreshed";
                applyWorking();
                return true;
            }
            return true;
        }
        if (section == Section.RESET) {
            y += 32;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 280, RotClientUiDraw.BUTTON_HEIGHT)) {
                resetSectionFields(resetTargetSection);
                applyWorking();
                statusMessage = sectionLabel(resetTargetSection) + " settings reset";
                awaitingResetAllConfirm = false;
                awaitingLayoutResetConfirm = false;
                awaitingHudVisibilityResetConfirm = false;
                return true;
            }
            y += 34;
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 220, RotClientUiDraw.BUTTON_HEIGHT)) {
                if (!awaitingResetAllConfirm) {
                    awaitingResetAllConfirm = true;
                    awaitingLayoutResetConfirm = false;
                    awaitingHudVisibilityResetConfirm = false;
                    statusMessage = "Click again to confirm reset all";
                    return true;
                }
                resetAllAppearanceFields();
                applyWorking();
                RotClientAppearanceStore.save(working);
                awaitingResetAllConfirm = false;
                statusMessage = "All appearance settings reset";
                return true;
            }
            y += 34;
            if (awaitingResetAllConfirm) {
                y += 14;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 220, RotClientUiDraw.BUTTON_HEIGHT)) {
                if (!awaitingLayoutResetConfirm) {
                    awaitingLayoutResetConfirm = true;
                    awaitingResetAllConfirm = false;
                    awaitingHudVisibilityResetConfirm = false;
                    statusMessage = "Click again to confirm reset UI positions";
                    return true;
                }
                RotClientClient.resetLayoutPositionsFromUi(true, true);
                awaitingLayoutResetConfirm = false;
                statusMessage = "Client UI and Mining HUD positions reset";
                return true;
            }
            y += 34;
            if (awaitingLayoutResetConfirm) {
                y += 14;
            }
            if (RotClientUiDraw.inside(mouseX, mouseY, left, y, 220, RotClientUiDraw.BUTTON_HEIGHT)) {
                if (!awaitingHudVisibilityResetConfirm) {
                    awaitingHudVisibilityResetConfirm = true;
                    awaitingResetAllConfirm = false;
                    awaitingLayoutResetConfirm = false;
                    statusMessage = "Click again to confirm reset HUD visibility";
                    return true;
                }
                trackerConfig.resetHudVisibility();
                RotClientClient.save();
                awaitingHudVisibilityResetConfirm = false;
                statusMessage = "HUD visibility restored to defaults";
                return true;
            }
        }
        return true;
    }

    private void resetSectionFields(Section target) {
        RotClientAppearanceConfig fresh = defaults.copy();
        switch (target) {
            case DASHBOARD -> {
                working.dashboardBackdrop = fresh.dashboardBackdrop;
                working.dashboardSurface = fresh.dashboardSurface;
                working.dashboardSurfaceAlt = fresh.dashboardSurfaceAlt;
                working.dashboardHeader = fresh.dashboardHeader;
                working.dashboardSidebar = fresh.dashboardSidebar;
                working.dashboardBorder = fresh.dashboardBorder;
                working.dashboardAccent = fresh.dashboardAccent;
            }
            case MINING_HUD, COLORS -> {
                working.hudBackground = fresh.hudBackground;
                working.hudPanel = fresh.hudPanel;
                working.hudPanelAlt = fresh.hudPanelAlt;
                working.hudHeader = fresh.hudHeader;
                working.hudBorder = fresh.hudBorder;
                working.hudAccent = fresh.hudAccent;
                working.dashboardSectionTitle = fresh.dashboardSectionTitle;
                working.dashboardTextPrimary = fresh.dashboardTextPrimary;
                working.dashboardTextSecondary = fresh.dashboardTextSecondary;
                working.dashboardTextSuccess = fresh.dashboardTextSuccess;
                working.dashboardTextWarning = fresh.dashboardTextWarning;
                working.hudTitle = fresh.hudTitle;
                working.hudTextPrimary = fresh.hudTextPrimary;
                working.hudTextSecondary = fresh.hudTextSecondary;
                working.dashboardButton = fresh.dashboardButton;
                working.dashboardButtonHover = fresh.dashboardButtonHover;
                working.dashboardButtonSelected = fresh.dashboardButtonSelected;
                working.dashboardButtonText = fresh.dashboardButtonText;
                working.dashboardField = fresh.dashboardField;
                working.dashboardFieldActive = fresh.dashboardFieldActive;
                working.dashboardHoverRow = fresh.dashboardHoverRow;
                working.dashboardDivider = fresh.dashboardDivider;
                working.dashboardShadow = fresh.dashboardShadow;
                working.dashboardError = fresh.dashboardError;
            }
            case BACKGROUND -> {
                working.customBackgroundEnabled = false;
                working.customBackgroundFile = "";
                working.customBackgroundOpacity = fresh.customBackgroundOpacity;
                working.customBackgroundDim = fresh.customBackgroundDim;
                working.customBackgroundFit = fresh.customBackgroundFit;
                RotClientBackgroundManager.invalidateCache();
            }
            case CHARTS -> {
                working.hudChartLine = fresh.hudChartLine;
                working.hudChartGlow = fresh.hudChartGlow;
                working.hudChartFill = fresh.hudChartFill;
                working.hudChartGrid = fresh.hudChartGrid;
                working.hudChartWell = fresh.hudChartWell;
            }
            case OVERVIEW, RESET -> {
            }
        }
    }

    private void resetAllAppearanceFields() {
        RotClientAppearanceConfig fresh = defaults.copy();
        working.dashboardBackdrop = fresh.dashboardBackdrop;
        working.dashboardSurface = fresh.dashboardSurface;
        working.dashboardSurfaceAlt = fresh.dashboardSurfaceAlt;
        working.dashboardHeader = fresh.dashboardHeader;
        working.dashboardSidebar = fresh.dashboardSidebar;
        working.dashboardBorder = fresh.dashboardBorder;
        working.dashboardAccent = fresh.dashboardAccent;
        working.dashboardSectionTitle = fresh.dashboardSectionTitle;
        working.dashboardTextPrimary = fresh.dashboardTextPrimary;
        working.dashboardTextSecondary = fresh.dashboardTextSecondary;
        working.dashboardTextSuccess = fresh.dashboardTextSuccess;
        working.dashboardTextWarning = fresh.dashboardTextWarning;
        working.dashboardButton = fresh.dashboardButton;
        working.dashboardButtonHover = fresh.dashboardButtonHover;
        working.dashboardButtonSelected = fresh.dashboardButtonSelected;
        working.dashboardButtonText = fresh.dashboardButtonText;
        working.dashboardField = fresh.dashboardField;
        working.dashboardFieldActive = fresh.dashboardFieldActive;
        working.dashboardHoverRow = fresh.dashboardHoverRow;
        working.dashboardDivider = fresh.dashboardDivider;
        working.dashboardShadow = fresh.dashboardShadow;
        working.dashboardError = fresh.dashboardError;
        working.hudBackground = fresh.hudBackground;
        working.hudPanel = fresh.hudPanel;
        working.hudPanelAlt = fresh.hudPanelAlt;
        working.hudHeader = fresh.hudHeader;
        working.hudBorder = fresh.hudBorder;
        working.hudTitle = fresh.hudTitle;
        working.hudTextPrimary = fresh.hudTextPrimary;
        working.hudTextSecondary = fresh.hudTextSecondary;
        working.hudAccent = fresh.hudAccent;
        working.hudChartLine = fresh.hudChartLine;
        working.hudChartGlow = fresh.hudChartGlow;
        working.hudChartFill = fresh.hudChartFill;
        working.hudChartGrid = fresh.hudChartGrid;
        working.hudChartWell = fresh.hudChartWell;
        working.customBackgroundEnabled = false;
        working.customBackgroundFile = "";
        working.customBackgroundOpacity = fresh.customBackgroundOpacity;
        working.customBackgroundDim = fresh.customBackgroundDim;
        working.customBackgroundFit = fresh.customBackgroundFit;
        RotClientBackgroundManager.invalidateCache();
    }

    private int maybeOpenColor(
            int mouseX,
            int mouseY,
            int left,
            int right,
            int y,
            int clipTop,
            int clipBottom,
            String label,
            IntSupplier getter,
            IntConsumer setter) {
        if (scroll.intersects(y, RotClientColorRowLayout.ROW_HEIGHT, clipTop, clipBottom)
                && RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        left,
                        y,
                        right - left,
                        RotClientColorRowLayout.ROW_HEIGHT)) {
            openPicker(label, getter.getAsInt(), setter);
        }
        return y + RotClientColorRowLayout.ROW_STEP;
    }

    private void openPicker(String label, int initial, IntConsumer setter) {
        Minecraft.getInstance().gui.setScreen(
                new RotClientColorPickerScreen(
                        this,
                        label,
                        initial,
                        live -> {
                            // In-memory live preview only — never persist here.
                            setter.accept(live);
                            applyWorking();
                        },
                        value -> {
                            setter.accept(value);
                            applyWorking();
                            statusMessage = "Color updated";
                        }));
    }

    private void applyWorking() {
        working.normalize();
        RotClientTheme.apply(working);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {
        float uiScale = uiScale();
        int logicalX = Math.round((float) mouseX / uiScale);
        int logicalY = Math.round((float) mouseY / uiScale);
        int panelX = panelX();
        int panelY = panelY();
        int contentLeft = panelX + SIDEBAR_WIDTH + 14;
        int contentRight = panelX + PANEL_WIDTH - 14;
        int contentTop = panelY + HEADER_HEIGHT + 12;
        int contentBottom = panelY + PANEL_HEIGHT - 52;
        if (!RotClientUiDraw.inside(
                logicalX,
                logicalY,
                contentLeft,
                contentTop,
                contentRight - contentLeft,
                contentBottom - contentTop)) {
            return false;
        }
        if (!scroll.canScroll()) {
            return false;
        }
        scroll.scrollBySteps(verticalAmount, 40);
        return true;
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double dragX,
            double dragY) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(event, dragX, dragY);
        }
        float uiScale = uiScale();
        int mouseY = Math.round((float) event.y() / uiScale);
        int panelY = panelY();
        int contentTop = panelY + HEADER_HEIGHT + 12;
        int contentBottom = panelY + PANEL_HEIGHT - 52;
        if (scroll.dragThumbTo(
                mouseY,
                contentTop,
                contentBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scroll.endThumbDrag()) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (RotClientClient.workspace().activeRoute().isAppearance()) {
            RotClientClient.workspace().navigateActive(
                    RotClientWorkspaceRoute.OVERVIEW);
        }
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<Section> navSections() {
        return List.of(
                Section.OVERVIEW,
                Section.DASHBOARD,
                Section.COLORS,
                Section.BACKGROUND,
                Section.CHARTS,
                Section.RESET);
    }

    private static String sectionLabel(Section section) {
        return switch (section) {
            case OVERVIEW -> "Overview";
            case DASHBOARD -> "Dashboard";
            case MINING_HUD -> "Mining HUD";
            case COLORS -> "Colors";
            case BACKGROUND -> "Background";
            case CHARTS -> "Charts";
            case RESET -> "Reset & Defaults";
        };
    }

    private float uiScale() {
        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float fitWidth = scaledWidth / (float) (PANEL_WIDTH + 40);
        float fitHeight = scaledHeight / (float) (PANEL_HEIGHT + 40);
        return Math.max(0.55F, Math.min(1.0F, Math.min(fitWidth, fitHeight)));
    }

    private int panelX() {
        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return Math.round((scaledWidth / uiScale() - PANEL_WIDTH) / 2.0F);
    }

    private int panelY() {
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.round((scaledHeight / uiScale() - PANEL_HEIGHT) / 2.0F);
    }
}
