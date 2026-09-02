package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class MiningUiScreen extends Screen {
    private static final String PRODUCT_HEADER = "ROT CLIENT";
    private static final int CHROME_HEIGHT = RotClientDashboardLayout.chromeHeight();
    private static final int HEADER_HEIGHT = CHROME_HEIGHT;
    private static final int TAB_BAR_HEIGHT = RotClientDashboardLayout.TAB_STRIP_HEIGHT;
    private static final int SIDEBAR_WIDTH = RotClientDashboardLayout.SIDEBAR_WIDTH;
    private static final int MODULE_X_OFFSET = 10;
    private static final int MODULE_WIDTH = SIDEBAR_WIDTH - 20;
    private static final int NAV_ITEM_HEIGHT = RotClientSidebarNav.ITEM_HEIGHT;
    private static final int OVERVIEW_MODULE_Y = CHROME_HEIGHT + 14;
    private static final int HOME_BUTTON_Y_OFFSET =
            RotClientDashboardLayout.HOME_BUTTON_Y_OFFSET;
    private static final int ANALYTICS_MAX_RESOURCE_ROWS = 8;
    private static final int HISTORY_MAX_VISIBLE_ROWS = 7;
    private static final int HISTORY_ROW_HEIGHT = 36;

    private static final int CONTENT_INSET = RotClientDashboardLayout.CONTENT_INSET;
    private static final int MASTER_Y = CHROME_HEIGHT + 12;
    private static final int MASTER_HEIGHT = 42;
    private static final int METRICS_LABEL_Y = CHROME_HEIGHT + 64;
    private static final int SETTINGS_GRID_Y = CHROME_HEIGHT + 76;
    private static final int SETTING_HEIGHT = 22;
    private static final int SETTING_PITCH = 26;
    private static final int SETTING_COLUMN_GAP = 10;
    private static final int BUTTON_HEIGHT = 26;
    private static final int BUTTON_GAP = 8;

    private static final List<TrackerSelection> TRACKER_SELECTIONS =
        List.of(TrackerSelection.values());

    private static final int MASTER_SWITCH_WIDTH = 36;
    private static final int MASTER_CONTROL_GAP = 8;
    private static final int MASTER_CONTROL_RIGHT_INSET = 8;

    private final TrackerConfig config;
    private final RotClientHud hud;
    private final Screen parent;
    private final List<ToggleSetting> settings;
    private final float[] animations;
    private float fullbrightAnimation;
    private float autoSprintAnimation;
    private float cameraAnimation;
    private final QolUtilityDashboard qolDashboard;
    private DashboardModule selectedModule;

    private boolean trackerDropdownOpen;
    private String trackerSearch = "";
    private String settingsQuery = "";
    private boolean settingsSearchFocused;
    private String highlightSettingId = "";
    private long highlightUntilMillis;
    private int trackerDropdownScroll;
    private int trackerDropdownHighlight;
    private final RotClientScrollState analyticsScroll = new RotClientScrollState();
    private final RotClientScrollState sidebarScroll = new RotClientScrollState();
    private final RotClientExpandState sidebarExpand = new RotClientExpandState();

    private int tabStripScroll = 0;
    private Integer draggingTabIndex = null;
    private int dragStartX;
    private int dragStartY;
    private boolean tabDragging = false;
    private boolean panelDragging = false;
    private boolean openInNewTabGesture;
    private RotClientWindowPlacementPolicy.ResizeEdge resizeEdge =
            RotClientWindowPlacementPolicy.ResizeEdge.NONE;
    private String snapPreview = RotClientWindowPlacementPolicy.FLOATING;
    private String chromeHoverTip = "";
    private int omniboxHighlight;
    private boolean awaitingNewSessionConfirm = false;
    private double panelDragOriginMouseX;
    private double panelDragOriginMouseY;
    private double panelDragOriginPanelX;
    private double panelDragOriginPanelY;
    private double livePanelX = Double.NaN;
    private double livePanelY = Double.NaN;

    MiningUiScreen(TrackerConfig config, RotClientHud hud) {
        this(config, hud, null, null, true);
    }

    MiningUiScreen(TrackerConfig config, RotClientHud hud, Screen parent) {
        this(config, hud, parent, null, true);
    }

    MiningUiScreen(
            TrackerConfig config,
            RotClientHud hud,
            Screen parent,
            DashboardModule initialModule) {
        this(config, hud, parent, initialModule, true);
    }

    MiningUiScreen(
            TrackerConfig config,
            RotClientHud hud,
            Screen parent,
            DashboardModule initialModule,
            boolean useWorkspace) {
        super(Component.literal(PRODUCT_HEADER));
        this.config = config;
        this.hud = hud;
        this.parent = parent;
        this.qolDashboard = new QolUtilityDashboard(config, this);
        this.settings = List.of(
                setting("showBlocks", () -> selectedTrackerLabel() + " BLOCKS",
                        () -> config.showBlocks,
                        value -> config.showBlocks = value),
                setting("showRawMaterial", () -> rawMetricLabel(),
                        () -> config.showRawMaterial,
                        value -> config.showRawMaterial = value),
                setting("showEnchantedMaterial", () -> enchantedMetricLabel(),
                        () -> config.showEnchantedMaterial,
                        value -> config.showEnchantedMaterial = value),
                setting("showSessionProfit", () -> "EST. SESSION VALUE",
                        () -> config.showSessionProfit,
                        value -> config.showSessionProfit = value),
                setting("showUnsoldValue", () -> "UNSOLD VALUE (EST.)",
                        () -> config.showUnsoldValue,
                        value -> config.showUnsoldValue = value),
                setting("showCoinsPerHour", () -> "COINS / HOUR",
                        () -> config.showCoinsPerHour,
                        value -> config.showCoinsPerHour = value),
                setting("showMaterialPerHour", () -> resourcePerHourLabel(),
                        () -> config.showMaterialPerHour,
                        value -> config.showMaterialPerHour = value),
                setting("showSessionTime", () -> "SESSION TIME",
                        () -> config.showSessionTime,
                        value -> config.showSessionTime = value),
                setting("showActiveTool", () -> "MINING TOOL",
                        () -> config.showActiveTool,
                        value -> config.showActiveTool = value),
                setting("showArea", () -> "AREA / LOCATION",
                        () -> config.showArea,
                        value -> config.showArea = value),
                setting("showRateGraph", () -> "RATE GRAPH",
                        () -> config.showRateGraph,
                        value -> config.showRateGraph = value),
                setting("showDropAndFortune", () -> "DROP + FORTUNE",
                        () -> config.showDropAndFortune,
                        value -> config.showDropAndFortune = value),
                setting("showBazaarPrices", () -> "BAZAAR + TAX",
                        () -> config.showBazaarPrices,
                        value -> config.showBazaarPrices = value),
                setting("showValuePanel", () -> "VALUE PANEL",
                        () -> config.showValuePanel,
                        value -> config.showValuePanel = value),
                setting("showOtherSection", () -> "OTHERS SECTION",
                        () -> config.showOtherSection,
                        value -> config.showOtherSection = value),
                setting("showTargetValue", () -> "TARGET VALUE",
                        () -> config.showTargetValue,
                        value -> config.showTargetValue = value),
                setting("showOtherValue", () -> "OTHERS VALUE",
                        () -> config.showOtherValue,
                        value -> config.showOtherValue = value),
                setting("showTotalMinedValue", () -> "TOTAL MINED VALUE",
                        () -> config.showTotalMinedValue,
                        value -> config.showTotalMinedValue = value),
                setting("showHudTitle", () -> "HUD TITLE",
                        () -> config.showHudTitle,
                        value -> config.showHudTitle = value),
                setting("showHudStatus", () -> "STATUS PILL",
                        () -> config.showHudStatus,
                        value -> config.showHudStatus = value),
                setting("showHudVersion", () -> "VERSION",
                        () -> config.showHudVersion,
                        value -> config.showHudVersion = value),
                setting("showHudAutoPause", () -> "AUTO-PAUSE LINE",
                        () -> config.showHudAutoPause,
                        value -> config.showHudAutoPause = value),
                setting("showTargetHeading", () -> "TARGET HEADING",
                        () -> config.showTargetHeading,
                        value -> config.showTargetHeading = value)
        );
        this.animations = new float[settings.size() + 1];
        animations[0] = config.enabled ? 1.0F : 0.0F;
        for (int i = 0; i < settings.size(); i++) {
            animations[i + 1] = settings.get(i).getter().getAsBoolean() ? 1.0F : 0.0F;
        }
        fullbrightAnimation = config.fullbrightEnabled ? 1.0F : 0.0F;
        autoSprintAnimation = config.autoSprintEnabled ? 1.0F : 0.0F;
        cameraAnimation = config.cameraEnabled ? 1.0F : 0.0F;
        RotClientWorkspace workspace = RotClientClient.workspace();
        if (useWorkspace) {
            if (initialModule != null) {
                workspace.navigateActive(
                        RotClientWorkspaceRoute.fromDashboardModule(initialModule));
            }
            selectedModule = workspace.activeRoute().toDashboardModule();
        } else {
            DashboardModule resolved = initialModule != null
                    ? initialModule
                    : config.selectedDashboardModule();
            if (resolved == null) {
                resolved = DashboardModule.NONE;
            }
            selectedModule = resolved;
            workspace.navigateActive(
                    RotClientWorkspaceRoute.fromDashboardModule(selectedModule));
        }
        if (config.selectedDashboardModule() != selectedModule) {
            config.setSelectedDashboardModule(selectedModule);
            RotClientClient.save();
        }
        qolDashboard.restoreFromWorkspace(workspace.activeTab());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        RotClientUiClock.beginFrame(minecraft);
        float uiScale = uiScale();
        int logicalMouseX = Math.round(mouseX / uiScale);
        int logicalMouseY = Math.round(mouseY / uiScale);
        graphics.pose().pushMatrix();
        graphics.pose().scale(uiScale, uiScale);
        graphics.pose().translate(
                RotClientUiMotion.fractionalPixel(displayedPanelX()),
                RotClientUiMotion.fractionalPixel(displayedPanelY()));
        RotClientUiDraw.beginHoverFrame(logicalMouseX, logicalMouseY);

        int panelX = panelX();
        int panelY = panelY();
        drawShell(graphics, panelX, panelY, logicalMouseX, logicalMouseY);
        drawSidebar(graphics, panelX, panelY, logicalMouseX, logicalMouseY);
        if (RotClientClient.workspace().activeRoute().isAppearance()) {
            drawAppearanceTabPlaceholder(
                    graphics, panelX, panelY, logicalMouseX, logicalMouseY);
        } else {
            switch (selectedModule) {
                case MINING_TRACKER ->
                        drawTrackerSettings(
                                graphics, panelX, panelY,
                                logicalMouseX, logicalMouseY);
                case POWDER_CHEST_TRACKER ->
                        drawPowderChestTracker(
                                graphics, panelX, panelY,
                                logicalMouseX, logicalMouseY);
                case QOL_SETTINGS ->
                        drawQolSettings(
                                graphics, panelX, panelY,
                                logicalMouseX, logicalMouseY);
                case SESSION_ANALYTICS ->
                        drawSessionAnalytics(
                                graphics, panelX, panelY,
                                logicalMouseX, logicalMouseY);
                case SESSION_HISTORY ->
                        drawSessionHistory(
                                graphics, panelX, panelY,
                                logicalMouseX, logicalMouseY);
                case NONE ->
                        drawModuleLanding(
                                graphics, panelX, panelY,
                                logicalMouseX, logicalMouseY);
            }
        }
        drawOmniboxSuggestions(graphics, panelX, panelY, logicalMouseX, logicalMouseY);
        drawSnapPreview(graphics, logicalMouseX, logicalMouseY);
        if (!RotClientWindowPlacementPolicy.isSnapped(
                RotClientClient.workspace().config().windowPlacement)) {
            CustomCursorRuntime.setResizeKind(CustomCursorPolicy.fromWindowEdge(
                    RotClientWindowPlacementPolicy.hitResizeEdge(
                            windowRect(), logicalMouseX, logicalMouseY)));
        } else {
            CustomCursorRuntime.setResizeKind(CustomCursorPolicy.ResizeKind.NONE);
        }
        if (chromeHoverTip != null && !chromeHoverTip.isBlank()) {
            RotClientUiDraw.noteHoverTip(chromeHoverTip);
        }
        RotClientUiDraw.drawHoverFrame(
                graphics,
                font,
                panelX + panelW(),
                panelY + panelH());

        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawShell(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        RotClientBackgroundManager.drawIfEnabled(
                graphics,
                panelX,
                panelY,
                panelW(),
                panelH(),
                RotClientTheme.activeConfig());
        RotClientUiDraw.drawShadowedPanel(
                graphics, panelX, panelY, panelW(), panelH());


        RotClientWorkspace workspace = RotClientClient.workspace();
        List<RotClientWorkspaceTab> tabs = workspace.tabsView();
        RotClientTabStrip.Layout tabLayout = RotClientTabStrip.compute(
                panelX, panelY, panelW(), tabs.size());
        this.chromeHoverTip = RotClientTabStrip.draw(
                graphics,
                font,
                tabLayout,
                tabs,
                workspace.config().activeTabId,
                tabStripScroll,
                mouseX,
                mouseY,
                draggingTabIndex,
                RotClientWindowPlacementPolicy.MAXIMIZED.equals(
                        RotClientWindowPlacementPolicy.normalize(
                                workspace.config().windowPlacement)));

        drawOmnibox(graphics, panelX, panelY, mouseX, mouseY);

        int contentTop = RotClientDashboardLayout.contentTop(panelY);
        graphics.fill(panelX + 1, contentTop,
                panelX + SIDEBAR_WIDTH, panelY + panelH() - 1,
                RotClientTheme.DASHBOARD_SIDEBAR);
        graphics.fill(panelX + SIDEBAR_WIDTH, contentTop,
                panelX + SIDEBAR_WIDTH + 1, panelY + panelH() - 1, RotClientTheme.DIVIDER);
        // Soft content-area wash for clearer hierarchy vs sidebar.
        RotClientUiDraw.roundedFill(
                graphics,
                panelX + SIDEBAR_WIDTH + 1,
                contentTop,
                panelX + panelW() - 1,
                panelY + panelH() - 1,
                RotClientUiDraw.withAlpha(RotClientTheme.SURFACE, 0x55),
                RotClientUiDraw.RADIUS_SM);
    }

    private String categoryHeaderLabel() {
        if (RotClientClient.workspace().activeRoute().isAppearance()) {
            return "Appearance";
        }
        return switch (selectedModule) {
            case NONE -> "Overview";
            case MINING_TRACKER -> "Mining Tracker";
            case POWDER_CHEST_TRACKER -> "Powder Chest Tracker";
            case SESSION_ANALYTICS -> "Session Analytics";
            case SESSION_HISTORY -> "Session History";
            case QOL_SETTINGS -> qolDashboard.activePage().title();
        };
    }

    private RotClientSidebarNav.Layout sidebarLayout(boolean advanceMotion) {
        RotClientWorkspace workspace = RotClientClient.workspace();
        sidebarExpand.syncTargets(
                workspace.expandedSidebarSections(),
                RotClientSidebarNav.knownSections());
        if (advanceMotion) {
            sidebarExpand.advanceSeconds(RotClientUiClock.seconds());
        }
        return RotClientSidebarNav.layout(
                OVERVIEW_MODULE_Y,
                workspace.expandedSidebarSections(),
                sidebarExpand::amount);
    }

    private void drawSidebar(GuiGraphicsExtractor graphics, int panelX, int panelY,
                             int mouseX, int mouseY) {
        RotClientWorkspace workspace = RotClientClient.workspace();
        RotClientSidebarNav.Layout layout = sidebarLayout(true);
        int sidebarTop = panelY + CHROME_HEIGHT;
        int homeY = panelY + panelH() - HOME_BUTTON_Y_OFFSET;
        int sidebarBottom = homeY - 10;
        int viewportHeight = Math.max(0, sidebarBottom - sidebarTop);
        int contentHeight = layout.contentHeight(OVERVIEW_MODULE_Y);
        sidebarScroll.setBounds(contentHeight, viewportHeight);
        sidebarScroll.advanceSeconds(RotClientUiClock.seconds());

        int scroll = sidebarScroll.scrollPixels();
        int headerX = panelX + 12;
        int itemX = panelX + MODULE_X_OFFSET;
        boolean appearanceSelected =
                workspace.activeRoute().isAppearance();
        boolean qolSelected = selectedModule == DashboardModule.QOL_SETTINGS
                && !appearanceSelected;

        graphics.enableScissor(panelX + 1, sidebarTop, panelX + SIDEBAR_WIDTH - 1, sidebarBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, sidebarScroll);
        try {
            int contentOrigin = OVERVIEW_MODULE_Y;
            java.util.function.IntUnaryOperator mapY = layoutY ->
                    sidebarTop + (layoutY - contentOrigin) - scroll;

            drawModuleEntry(graphics, mouseX, mouseY,
                    itemX, mapY.applyAsInt(layout.overviewY()),
                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                    "Overview",
                    "Status at a glance",
                    selectedModule == DashboardModule.NONE && !appearanceSelected,
                    false, false);

            drawSidebarSectionHeader(
                    graphics, mouseX, mouseY,
                    headerX, mapY.applyAsInt(layout.miningHeaderY()),
                    "Mining",
                    layout.miningOpen());
            drawSidebarChildren(
                    graphics,
                    panelX,
                    sidebarTop,
                    sidebarBottom,
                    mapY.applyAsInt(layout.trackerY()),
                    layout.miningClipHeight(),
                    () -> {
                        if (layout.trackerVisible()) {
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.trackerY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "Mining Tracker",
                                    config.selectedSelection()
                                            .displayName()
                                            + (config.enabled ? "  ·  ON" : "  ·  OFF"),
                                    selectedModule == DashboardModule.MINING_TRACKER
                                            && !appearanceSelected,
                                    false, config.enabled);
                        }
                        if (layout.miningHudVisible()) {
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.miningHudY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "Mining HUD",
                                    "Layout & colors",
                                    appearanceSelected
                                            && workspace.activeRoute()
                                            == RotClientWorkspaceRoute.APPEARANCE_MINING_HUD,
                                    false, false);
                        }
                        if (layout.powderChestTrackerVisible()) {
                            PowderChestTrackerPresentation powder =
                                    RotClientClient.powderChestTrackerPresentation();
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.powderChestTrackerY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "Powder Chest",
                                    powder.chestsOpened() + " chests  |  "
                                            + powder.statusLabel(),
                                    selectedModule
                                            == DashboardModule.POWDER_CHEST_TRACKER
                                            && !appearanceSelected,
                                    false, powder.enabled());
                        }
                    });

            drawSidebarSectionHeader(
                    graphics, mouseX, mouseY,
                    headerX, mapY.applyAsInt(layout.sessionsHeaderY()),
                    "Sessions",
                    layout.sessionsOpen());
            drawSidebarChildren(
                    graphics,
                    panelX,
                    sidebarTop,
                    sidebarBottom,
                    mapY.applyAsInt(layout.analyticsY()),
                    layout.sessionsClipHeight(),
                    () -> {
                        MiningSessionAnalyticsPresentation analytics =
                                RotClientClient.sessionAnalyticsPresentation();
                        boolean analyticsActive = analytics.currentRunning();
                        if (layout.analyticsVisible()) {
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.analyticsY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "Session Analytics",
                                    analytics.statusBadge(),
                                    selectedModule == DashboardModule.SESSION_ANALYTICS
                                            && !appearanceSelected,
                                    false, analyticsActive);
                        }
                        if (layout.historyVisible()) {
                            MiningSessionHistoryPresentation history =
                                    RotClientClient.sessionHistoryPresentation();
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.historyY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "Session History",
                                    history.available()
                                            ? (history.empty()
                                            ? "Local  ·  empty"
                                            : "Local  ·  " + history.sessions().size())
                                            : "Unavailable",
                                    selectedModule == DashboardModule.SESSION_HISTORY
                                            && !appearanceSelected,
                                    false, false);
                        }
                    });

            drawSidebarSectionHeader(
                    graphics, mouseX, mouseY,
                    headerX, mapY.applyAsInt(layout.settingsHeaderY()),
                    "SETTINGS",
                    layout.settingsOpen());
            drawSidebarChildren(
                    graphics,
                    panelX,
                    sidebarTop,
                    sidebarBottom,
                    mapY.applyAsInt(layout.appearanceY()),
                    layout.settingsClipHeight(),
                    () -> {
                        if (layout.appearanceVisible()) {
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.appearanceY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "Appearance",
                                    "Theme & colors",
                                    appearanceSelected,
                                    false, false);
                        }
                        if (layout.hudLayoutVisible()) {
                            drawModuleEntry(graphics, mouseX, mouseY,
                                    itemX, mapY.applyAsInt(layout.hudLayoutY()),
                                    MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                    "HUD Layout",
                                    "Move & align overlays",
                                    false,
                                    false, false);
                        }
                    });

            drawSidebarSectionHeader(
                    graphics, mouseX, mouseY,
                    headerX, mapY.applyAsInt(layout.qolHeaderY()),
                    "QoL & Settings",
                    layout.qolOpen());
            int qolClipTop = layout.qolPageYs() != null && layout.qolPageYs().length > 0
                    ? mapY.applyAsInt(layout.qolPageYs()[0])
                    : mapY.applyAsInt(layout.qolHeaderY());
            drawSidebarChildren(
                    graphics,
                    panelX,
                    sidebarTop,
                    sidebarBottom,
                    qolClipTop,
                    layout.qolClipHeight(),
                    () -> {
                        if (layout.qolChildrenVisible()) {
                            for (QolUtilityCatalog.Group group : QolUtilityCatalog.sidebarPages()) {
                                drawModuleEntry(graphics, mouseX, mouseY,
                                        itemX, mapY.applyAsInt(layout.qolPageY(group)),
                                        MODULE_WIDTH, NAV_ITEM_HEIGHT,
                                        group.title(),
                                        group.sidebarSubtitle(),
                                        qolSelected && qolDashboard.activePage() == group,
                                        false, false);
                            }
                        }
                    });
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }

        if (sidebarScroll.canScroll()) {
            int scrollbarHitX = panelX + SIDEBAR_WIDTH
                    - RotClientUiDraw.SCROLLBAR_HIT_WIDTH;
            boolean scrollbarHovered = mouseX >= scrollbarHitX
                    && mouseX < scrollbarHitX + RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                    && mouseY >= sidebarTop
                    && mouseY < sidebarBottom;
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    panelX + SIDEBAR_WIDTH - RotClientUiDraw.SCROLLBAR_WIDTH - 2,
                    sidebarTop,
                    sidebarBottom,
                    contentHeight,
                    scroll,
                    scrollbarHovered,
                    sidebarScroll.isThumbDragging());
        }

        graphics.fill(panelX + 10, homeY - 8,
                panelX + SIDEBAR_WIDTH - 10, homeY - 7, RotClientTheme.DIVIDER);
        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                panelX + MODULE_X_OFFSET,
                homeY,
                MODULE_WIDTH,
                selectedModule == DashboardModule.NONE ? "Close" : "Overview",
                selectedModule != DashboardModule.NONE,
                true);
    }

    private void drawSidebarChildren(
            GuiGraphicsExtractor graphics,
            int panelX,
            int sidebarTop,
            int sidebarBottom,
            int clipTop,
            int clipHeight,
            Runnable children) {
        if (clipHeight <= 0 || children == null) {
            return;
        }
        int left = panelX + 1;
        int right = panelX + SIDEBAR_WIDTH - 1;
        int top = Math.max(sidebarTop, clipTop);
        int bottom = Math.min(sidebarBottom, clipTop + clipHeight);
        if (bottom <= top) {
            return;
        }
        graphics.enableScissor(left, top, right, bottom);
        try {
            children.run();
        } finally {
            // GuiGraphics scissor is a stack. Re-enabling the parent rect would
            // push a second clip and leave the main pane empty after the sidebar.
            graphics.disableScissor();
        }
    }

    private void drawSidebarSectionHeader(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            String label,
            double openAmount) {
        boolean hovered = mouseX >= x
                && mouseX < x + MODULE_WIDTH
                && mouseY >= y
                && mouseY < y + RotClientSidebarNav.SECTION_LABEL_HEIGHT + 2;
        RotClientUiDraw.drawChevron(
                graphics,
                x,
                y,
                openAmount,
                hovered ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED);
        RotClientUiDraw.glyph(
                graphics,
                font,
                label,
                x + 14,
                y,
                hovered ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                true);
        if (hovered) {
            graphics.fill(
                    x,
                    y + RotClientSidebarNav.SECTION_LABEL_HEIGHT + 1,
                    x + MODULE_WIDTH - 8,
                    y + RotClientSidebarNav.SECTION_LABEL_HEIGHT + 2,
                    RotClientTheme.VIOLET);
        }
    }

    private void drawModuleEntry(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                 int x, int y, int width, int height,
                                 String label, String subtitle,
                                 boolean selected, boolean wip,
                                 boolean active) {
        RotClientUiDraw.drawNavItem(
                graphics,
                font,
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                label,
                subtitle,
                selected && !wip,
                active && !wip);
        if (wip) {
            RotClientUiDraw.roundedOutline(
                    graphics,
                    x + width - 18,
                    y + 8,
                    x + width - 8,
                    y + 18,
                    RotClientTheme.TEXT_MUTED,
                    RotClientUiDraw.RADIUS_XS);
            graphics.fill(
                    x + width - 16,
                    y + 6,
                    x + width - 10,
                    y + 9,
                    RotClientTheme.TEXT_MUTED);
        }
    }

    private void drawModuleLanding(GuiGraphicsExtractor graphics, int panelX, int panelY,
                                   int mouseX, int mouseY) {
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int contentWidth = contentRight - contentLeft;

        RotClientUiDraw.text(graphics, font, "Overview", contentLeft, panelY + CHROME_HEIGHT + 12,
                RotClientTheme.TEXT_DIM, true);
        RotClientUiDraw.text(graphics, font,
                "Right Shift opens this dashboard. Search the address bar, or type /rot qol.",
                contentLeft, panelY + CHROME_HEIGHT + 28, RotClientTheme.TEXT_MUTED, false);

        int cardY = panelY + CHROME_HEIGHT + 52;
        int gap = 10;
        int cardWidth = (contentWidth - gap) / 2;
        int row2Y = cardY + 84 + gap;
        drawOverviewCard(graphics, mouseX, mouseY,
                contentLeft, cardY, cardWidth,
                "Quality of life",
                QolUtilityCatalog.modules().size() + " modules",
                "Combat, dungeons, utilities…",
                true);
        drawOverviewCard(graphics, mouseX, mouseY,
                contentLeft + cardWidth + gap, cardY, cardWidth,
                "Look & HUD",
                "Appearance",
                "Colors, background, HUD layout",
                false);
        drawOverviewCard(graphics, mouseX, mouseY,
                contentLeft, row2Y, cardWidth,
                "Mining tracker",
                config.selectedSelection().displayName(),
                config.enabled ? "Tracking ON" : "Tracking OFF",
                config.enabled);
        MiningSessionAnalyticsPresentation analytics =
                RotClientClient.sessionAnalyticsPresentation();
        boolean analyticsActive = analytics.currentRunning();
        MiningSessionHistoryPresentation history =
                RotClientClient.sessionHistoryPresentation();
        String historyDetail = history.available()
                ? (history.empty() ? "No saved sessions" : history.sessions().size() + " saved")
                : "History unavailable";
        drawOverviewCard(graphics, mouseX, mouseY,
                contentLeft + cardWidth + gap, row2Y, cardWidth,
                "Sessions",
                analytics.statusBadge(),
                historyDetail,
                analyticsActive);

        int linkY = row2Y + 100;
        RotClientUiDraw.text(graphics, font, "Community", contentLeft, linkY, RotClientTheme.TEXT_DIM, false);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                contentLeft, linkY + 14, 148,
                RotClientLinks.DISCORD_LABEL, true, true);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                contentLeft + 158, linkY + 14, 128,
                RotClientLinks.HOMEPAGE_LABEL, false, true);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                contentLeft + 296, linkY + 14, 110,
                RotClientLinks.ISSUES_LABEL, false, true);

        drawButton(graphics, mouseX, mouseY,
                contentLeft, panelY + actionRowY(), 148,
                "QoL modules", RotClientTheme.TEXT_DIM, false);
        drawButton(graphics, mouseX, mouseY,
                contentLeft + 158, panelY + actionRowY(), 148,
                "Edit HUD", RotClientTheme.TEXT_DIM, false);
        drawButton(graphics, mouseX, mouseY,
                contentLeft + 316, panelY + actionRowY(), 148,
                "Mining", RotClientTheme.TEXT_DIM, false);
        drawButton(graphics, mouseX, mouseY,
                contentRight - 148, panelY + actionRowY(), 148,
                "Done", RotClientTheme.HUD_ACCENT, true);
    }

    private void drawSettingsSearchField(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width) {
        boolean hover = inside(mouseX, mouseY, x, y, width, 20);
        int fill = settingsSearchFocused || hover
                ? RotClientTheme.FIELD_ACTIVE
                : RotClientTheme.FIELD;
        roundedFill(graphics, x, y, x + width, y + 20, fill);
        roundedOutline(graphics, x, y, x + width, y + 20,
                settingsSearchFocused
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.BORDER);
        String text;
        int color;
        if (!settingsQuery.isEmpty()) {
            text = settingsQuery;
            color = RotClientTheme.TEXT;
        } else if (settingsSearchFocused) {
            text = "Search settings…";
            color = RotClientTheme.TEXT_MUTED;
        } else {
            text = "Search settings, tools, and pages…";
            color = RotClientTheme.TEXT_MUTED;
        }
        RotClientUiDraw.text(graphics, font, RotClientUiDraw.ellipsizeAndHover(font, text, width - 16, x + 8, y + 4, 12),
                x + 8, y + 6, color, false);
        if (settingsSearchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = x + 8 + font.width(
                    RotClientUiDraw.ellipsize(font, settingsQuery, width - 16));
            graphics.fill(caretX, y + 5, caretX + 1, y + 15, RotClientTheme.TEXT);
        }
    }

    private void drawOverviewCard(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            String title,
            String status,
            String detail,
            boolean active) {
        boolean hover = inside(mouseX, mouseY, x, y, width, 84);
        RotClientUiDraw.drawAccentCard(graphics, x, y, width, 84, active);
        if (hover) {
            roundedOutline(graphics, x, y, x + width, y + 84,
                    RotClientTheme.BORDER_BRIGHT);
        }
        RotClientUiDraw.text(graphics, font, title, x + 12, y + 10, RotClientTheme.TEXT_DIM, true);
        RotClientUiDraw.text(graphics, font, status, x + 12, y + 30, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, detail, x + 12, y + 48,
                active ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_MUTED, false);
        RotClientUiDraw.text(graphics, font, "Open →",
                x + width - 12 - font.width("Open →"),
                y + 64, RotClientTheme.TEXT_DIM, false);
    }

    private void drawTrackerSettings(GuiGraphicsExtractor graphics, int panelX, int panelY,
                                     int mouseX, int mouseY) {
        int dropdownMouseX = mouseX;
        int dropdownMouseY = mouseY;

        if (trackerDropdownOpen) {
            mouseX = Integer.MIN_VALUE;
            mouseY = Integer.MIN_VALUE;
        }
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int masterY = panelY + MASTER_Y;

        boolean masterHover = inside(mouseX, mouseY,
                contentLeft, masterY, contentRight - contentLeft, MASTER_HEIGHT);
        roundedFill(graphics, contentLeft, masterY,
                contentRight, masterY + MASTER_HEIGHT,
                masterHover ? RotClientTheme.HOVER_ROW : RotClientTheme.SELECTED_ROW);
        graphics.fill(contentLeft, masterY + 5, contentLeft + 3,
                masterY + MASTER_HEIGHT - 5,
                config.enabled ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_DIM);
        TrackerSelection selection = config.selectedSelection();
        RotClientUiDraw.text(graphics, font,
                selection.displayName().toUpperCase() + " TRACKER",
                contentLeft + 12, masterY + 6, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font,
                config.enabled
                        ? (selection.isGemstone()
                        ? "Live gemstone drops + tier ledger"
                        : "Server drops + live Bazaar pricing")
                        : "Select a target, then enable tracking",
                contentLeft + 12, masterY + 22,
                config.enabled ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_DIM, false);
        int switchX = contentRight - MASTER_CONTROL_RIGHT_INSET
                - MASTER_SWITCH_WIDTH;
int selectorX = switchX - MASTER_CONTROL_GAP - 180;
int selectorY = masterY + 10;

        animations[0] = animate(animations[0], config.enabled);
        drawSwitch(graphics, switchX, selectorY,
                MASTER_SWITCH_WIDTH, 20, animations[0]);

        RotClientUiDraw.text(graphics, font, "HUD METRICS",
                contentLeft + 2, panelY + METRICS_LABEL_Y, RotClientTheme.TEXT_DIM, true);
        drawRight(graphics,
                selection.isGemstone()
                        ? "VALUE / PROFIT NOT AVAILABLE"
                        : settings.size() + " OPTIONS",
                contentRight - 2, panelY + METRICS_LABEL_Y, RotClientTheme.TEXT_DIM);

        int gridY = panelY + SETTINGS_GRID_Y;
        int availableWidth = contentRight - contentLeft;
        int columnWidth = (availableWidth - SETTING_COLUMN_GAP) / 2;
        for (int i = 0; i < settings.size(); i++) {
            int column = i % 2;
            int row = i / 2;
            int x = contentLeft + column * (columnWidth + SETTING_COLUMN_GAP);
            int y = gridY + row * SETTING_PITCH;
            ToggleSetting setting = settings.get(i);
            boolean available = isHudSettingAvailable(i);
            boolean enabled = available
                    && setting.getter().getAsBoolean();
            boolean highlighted = isSettingHighlighted(setting.id());
            boolean hover = available && inside(mouseX, mouseY,
                    x, y, columnWidth, SETTING_HEIGHT);
            int rowFill = highlighted
                    ? RotClientTheme.SELECTED_ROW
                    : hover
                    ? RotClientTheme.HOVER_ROW
                    : (enabled ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT);
            roundedFill(graphics, x, y, x + columnWidth, y + SETTING_HEIGHT, rowFill);
            if (highlighted) {
                roundedOutline(graphics, x, y, x + columnWidth, y + SETTING_HEIGHT,
                        RotClientTheme.HUD_ACCENT);
            }
            if (enabled) {
                graphics.fill(x, y + 4, x + 2,
                        y + SETTING_HEIGHT - 4, RotClientTheme.HUD_ACCENT);
            }
            RotClientUiDraw.text(graphics, font, hudSettingLabel(i, setting), x + 9, y + 6,
                    enabled ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM, false);
            if (available) {
                animations[i + 1] = animate(animations[i + 1], enabled);
                drawSwitch(graphics, x + columnWidth - 35, y + 3,
                        30, 15, animations[i + 1]);
            } else {
                animations[i + 1] = animate(animations[i + 1], false);
                drawRight(graphics, gemstoneHudSettingStatus(i),
                        x + columnWidth - 8, y + 6, RotClientTheme.TEXT_DIM);
            }
        }

        int buttonY = panelY + actionRowY();
        int buttonWidth = (availableWidth - BUTTON_GAP * 2) / 3;
        int secondX = contentLeft + buttonWidth + BUTTON_GAP;
        int thirdX = secondX + buttonWidth + BUTTON_GAP;
        drawButton(graphics, mouseX, mouseY,
                contentLeft, buttonY, buttonWidth,
                "EDIT HUD LAYOUT", RotClientTheme.TEXT_DIM, false);
        drawButton(graphics, mouseX, mouseY,
                secondX, buttonY, buttonWidth,
                "RESET", RotClientTheme.WARNING, false);
        drawButton(graphics, mouseX, mouseY,
                thirdX, buttonY, contentRight - thirdX,
                "DONE", RotClientTheme.HUD_ACCENT, true);

        // Draw the dropdown last so it stays above the settings.
        drawTrackerDropdown(
                graphics,
                dropdownMouseX,
                dropdownMouseY,
                selectorX,
                selectorY,
                180,
                20);
    }

    private void drawPowderChestTracker(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        PowderChestTrackerPresentation presentation =
                RotClientClient.powderChestTrackerPresentation();
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int contentWidth = contentRight - contentLeft;
        int masterY = panelY + MASTER_Y;
        boolean hover = inside(mouseX, mouseY,
                contentLeft, masterY, contentWidth, MASTER_HEIGHT);
        roundedFill(graphics, contentLeft, masterY,
                contentRight, masterY + MASTER_HEIGHT,
                hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SELECTED_ROW);
        graphics.fill(contentLeft, masterY + 5, contentLeft + 3,
                masterY + MASTER_HEIGHT - 5,
                presentation.enabled()
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_DIM);
        RotClientUiDraw.text(graphics, font, "POWDER CHEST TRACKER",
                contentLeft + 12, masterY + 6, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font,
                "Standalone Crystal Hollows chest rewards  |  Current Session",
                contentLeft + 12, masterY + 22,
                presentation.enabled()
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_DIM,
                false);
        animations[0] = animate(
                animations[0], presentation.enabled());
        drawSwitch(graphics,
                contentRight - MASTER_CONTROL_RIGHT_INSET
                        - MASTER_SWITCH_WIDTH,
                masterY + 10,
                MASTER_SWITCH_WIDTH,
                20,
                animations[0]);

        int metricY = masterY + MASTER_HEIGHT + 12;
        int gap = 10;
        int metricWidth = (contentWidth - gap * 2) / 3;
        RotClientUiDraw.drawMetricCard(
                graphics, font, contentLeft, metricY, metricWidth,
                "Chests Opened",
                formatPowderCount(presentation.chestsOpened()),
                formatPowderRate(presentation.chestsPerHour()),
                RotClientTheme.TEXT);
        RotClientUiDraw.drawMetricCard(
                graphics, font,
                contentLeft + metricWidth + gap,
                metricY, metricWidth,
                "Gemstone Powder",
                formatPowderCount(presentation.gemstonePowder()),
                formatPowderRate(presentation.gemstonePowderPerHour()),
                RotClientTheme.HUD_ACCENT);
        RotClientUiDraw.drawMetricCard(
                graphics, font,
                contentLeft + (metricWidth + gap) * 2,
                metricY, contentRight
                        - (contentLeft + (metricWidth + gap) * 2),
                "Mithril Powder",
                formatPowderCount(presentation.mithrilPowder()),
                formatPowderRate(presentation.mithrilPowderPerHour()),
                RotClientTheme.HUD_ACCENT);

        int listY = metricY + RotClientUiDraw.METRIC_CARD_HEIGHT + 12;
        RotClientUiDraw.text(graphics, font,
                "Enc. / compacted Hard Stone  "
                        + formatPowderCount(presentation.enchantedHardStone()),
                contentLeft, listY, RotClientTheme.TEXT_MUTED, false);
        listY += 18;
        int listHeight = Math.max(120, panelY + actionRowY() - 12 - listY);
        int columnWidth = (contentWidth - gap) / 2;
        drawPowderRewardColumn(
                graphics,
                contentLeft,
                listY,
                columnWidth,
                listHeight,
                "CHEST REWARDS",
                presentation.lootRows());
        drawPowderRewardColumn(
                graphics,
                contentLeft + columnWidth + gap,
                listY,
                contentRight - (contentLeft + columnWidth + gap),
                listHeight,
                "POWDER & CURRENCY",
                presentation.currencyRows());

        drawButton(graphics, mouseX, mouseY,
                contentLeft, panelY + actionRowY(), 148,
                config.powderChestHudEnabled ? "HUD: ON" : "HUD: OFF",
                config.powderChestHudEnabled
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_DIM,
                true);
        drawButton(graphics, mouseX, mouseY,
                contentLeft + 158, panelY + actionRowY(), 148,
                "EDIT HUD", RotClientTheme.HUD_ACCENT, true);
        drawButton(graphics, mouseX, mouseY,
                contentRight - 148, panelY + actionRowY(), 148,
                "DONE", RotClientTheme.HUD_ACCENT, true);
    }

    private void drawPowderRewardColumn(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String title,
            List<PowderChestTrackerPresentation.RewardRow> rows) {
        RotClientUiDraw.drawElevatedCard(graphics, x, y, width, height);
        RotClientUiDraw.text(graphics, font, title, x + 12, y + 10,
                RotClientTheme.TEXT_DIM, true);
        RotClientUiDraw.text(graphics, font, "ITEM", x + 12, y + 28,
                RotClientTheme.TEXT_MUTED, false);
        drawRight(graphics, "QTY", x + width - 12, y + 28,
                RotClientTheme.TEXT_MUTED);
        if (rows.isEmpty()) {
            RotClientUiDraw.text(graphics, font, "None yet", x + 12, y + 46,
                    RotClientTheme.TEXT_DIM, false);
            return;
        }
        int rowY = y + 46;
        int maxRows = Math.max(1, (height - 54) / 18);
        for (int i = 0; i < Math.min(rows.size(), maxRows); i++) {
            PowderChestTrackerPresentation.RewardRow row = rows.get(i);
            String name = RotClientUiDraw.ellipsizeAndHover(
                    font, row.displayName(), width - 92, x + 12, rowY, 14);
            RotClientUiDraw.text(graphics, font, name, x + 12, rowY,
                    RotClientTheme.TEXT, false);
            drawRight(graphics, formatPowderCount(row.quantity()),
                    x + width - 12, rowY, RotClientTheme.HUD_ACCENT);
            rowY += 18;
        }
    }

    private static String formatPowderCount(long value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0L, value));
    }

    private static String formatPowderRate(double perHour) {
        if (perHour <= 0.0) {
            return "0/h";
        }
        if (perHour >= 100.0) {
            return String.format(Locale.ROOT, "%,.0f/h", perHour);
        }
        return String.format(Locale.ROOT, "%,.1f/h", perHour);
    }

    private void drawQolSettings(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int contentTop = panelY + MASTER_Y;
        int contentBottom = panelY + actionRowY() - 10;
        qolDashboard.draw(
                graphics,
                font,
                contentLeft,
                contentTop,
                contentRight,
                contentBottom,
                mouseX,
                mouseY);

        drawButton(graphics, mouseX, mouseY,
                contentRight - 148, panelY + actionRowY(), 148,
                "DONE", RotClientTheme.HUD_ACCENT, true);
    }

    private void drawQolToggleRow(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int contentLeft,
            int contentRight,
            int settingY,
            int settingHeight,
            String title,
            String subtitle,
            boolean enabled,
            float switchAnimation) {
        // Retained for compile safety if referenced elsewhere; unused by new cards.
        boolean hover = inside(mouseX, mouseY,
                contentLeft, settingY,
                contentRight - contentLeft, settingHeight);
        roundedFill(graphics, contentLeft, settingY,
                contentRight, settingY + settingHeight,
                hover ? RotClientTheme.HOVER_ROW
                        : (enabled ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT));
        RotClientUiDraw.text(graphics, font, title,
                contentLeft + 12, settingY + 7,
                enabled ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM, true);
        RotClientUiDraw.text(graphics, font, subtitle,
                contentLeft + 12, settingY + 23,
                enabled ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_DIM, false);
        drawSwitch(graphics,
                contentRight - MASTER_CONTROL_RIGHT_INSET
                        - MASTER_SWITCH_WIDTH,
                settingY + 10,
                MASTER_SWITCH_WIDTH, 20,
                switchAnimation);
    }

    private String qolStatusLine() {
        return qolDashboard.statusLine();
    }

    private void drawSessionAnalytics(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        MiningSessionAnalyticsPresentation presentation =
                RotClientClient.sessionAnalyticsPresentation();
        MiningSessionAnalyticsViewModel model = presentation.viewModel();
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int availableWidth = contentRight - contentLeft;
        int clipTop = panelY + MASTER_Y;
        int clipBottom = panelY + actionRowY() - 10;
        analyticsScroll.setViewportHeight(clipBottom - clipTop);
        analyticsScroll.advanceSeconds(RotClientUiClock.seconds());

        graphics.enableScissor(contentLeft, clipTop, contentRight + 6, clipBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, analyticsScroll);
        int y = clipTop - analyticsScroll.scrollPixels();
        int measuredStart = y;
        try {
        boolean active = presentation.currentRunning();

        // Header
        if (analyticsScroll.intersects(y, 44, clipTop, clipBottom)) {
            RotClientUiDraw.drawElevatedCard(
                    graphics, contentLeft, y, availableWidth, 44);
            graphics.fill(
                    contentLeft,
                    y + 10,
                    contentLeft + 3,
                    y + 34,
                    active ? RotClientTheme.SUCCESS : RotClientTheme.HUD_ACCENT);
            RotClientUiDraw.pageTitle(
                    graphics, font, "Session Analytics", contentLeft + 14, y + 9);
            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Persistent Current Session · local only",
                    contentLeft + 14,
                    y + 25);
            RotClientUiDraw.drawStatusPill(
                    graphics,
                    font,
                    contentRight - 12,
                    y + 14,
                    presentation.statusBadge(),
                    active ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_DIM);
        }
        y += 52;

        // Priority performance metrics
        int chipGap = 8;
        int chipWidth = (availableWidth - chipGap * 3) / 4;
        String sessionTime = formatAnalyticsSessionTime(model);
        String sessionValue = model.resolvedValueAvailable()
                ? MiningSessionPriceBook.formatCoinAmount(model.resolvedItemValue())
                : "—";
        String coinsPerHour = formatAnalyticsCoinsPerHour(model);
        if (analyticsScroll.intersects(
                y, RotClientUiDraw.METRIC_CARD_HEIGHT, clipTop, clipBottom)) {
            RotClientUiDraw.drawMetricCard(
                    graphics, font, contentLeft, y, chipWidth,
                    "Session Time", sessionTime, RotClientTheme.TEXT);
            RotClientUiDraw.drawMetricCard(
                    graphics, font, contentLeft + chipWidth + chipGap, y, chipWidth,
                    "Session Value", sessionValue,
                    model.resolvedValueAvailable()
                            ? RotClientTheme.TEXT
                            : RotClientTheme.TEXT_DIM);
            RotClientUiDraw.drawMetricCard(
                    graphics, font,
                    contentLeft + (chipWidth + chipGap) * 2, y, chipWidth,
                    "Coins / Hour", coinsPerHour, RotClientTheme.TEXT);
            int parityColor = model.mismatchCount() > 0
                    ? RotClientTheme.WARNING
                    : RotClientTheme.SUCCESS;
            RotClientUiDraw.drawMetricCard(
                    graphics, font,
                    contentLeft + (chipWidth + chipGap) * 3, y, chipWidth,
                    "Parity", model.parityStatusLabel(), parityColor);
        }
        y += RotClientUiDraw.METRIC_CARD_HEIGHT + 10;

        // Secondary status chips
        if (analyticsScroll.intersects(
                y, RotClientUiDraw.METRIC_CARD_HEIGHT, clipTop, clipBottom)) {
            RotClientUiDraw.drawMetricCard(
                    graphics, font, contentLeft, y, chipWidth,
                    "Target", model.selectedTargetDisplayName(), RotClientTheme.TEXT);
            RotClientUiDraw.drawMetricCard(
                    graphics, font, contentLeft + chipWidth + chipGap, y, chipWidth,
                    "Tracker", model.trackerEnabled() ? "On" : "Off",
                    model.trackerEnabled()
                            ? RotClientTheme.SUCCESS
                            : RotClientTheme.TEXT_DIM);
            RotClientUiDraw.drawMetricCard(
                    graphics, font,
                    contentLeft + (chipWidth + chipGap) * 2, y, chipWidth,
                    "Entries", Long.toString(model.entryCount()), RotClientTheme.TEXT);
            RotClientUiDraw.drawMetricCard(
                    graphics, font,
                    contentLeft + (chipWidth + chipGap) * 3, y, chipWidth,
                    "Mismatches", Long.toString(model.mismatchCount()),
                    model.mismatchCount() > 0
                            ? RotClientTheme.WARNING
                            : RotClientTheme.SUCCESS);
        }
        y += RotClientUiDraw.METRIC_CARD_HEIGHT + 10;

        if (presentation.emptyStateMessage() != null) {
            if (analyticsScroll.intersects(y, 40, clipTop, clipBottom)) {
                RotClientUiDraw.drawElevatedCard(
                        graphics, contentLeft, y, availableWidth, 40);
                RotClientUiDraw.sectionLabel(
                        graphics, font, "NOTICE", contentLeft + 12, y + 8);
                RotClientUiDraw.bodyText(
                        graphics,
                        font,
                        RotClientUiDraw.ellipsizeAndHover(
                                font,
                                presentation.emptyStateMessage(),
                                availableWidth - 24,
                                contentLeft + 12,
                                y + 20,
                                12),
                        contentLeft + 12,
                        y + 22);
            }
            y += 48;
        }

        // Two-column Entries / Values
        int colGap = 10;
        int colWidth = (availableWidth - colGap) / 2;
        int summaryHeight = 78;
        if (analyticsScroll.intersects(y, summaryHeight, clipTop, clipBottom)) {
            drawAnalyticsSummaryCard(
                    graphics,
                    contentLeft,
                    y,
                    colWidth,
                    summaryHeight,
                    "Entries",
                    Long.toString(model.entryCount()),
                    "total credited",
                    "Target " + model.targetEntryCount()
                            + "  ·  Other Mined " + model.otherEntryCount()
                            + "  ·  Chest " + model.chestLootEntryCount()
                            + "  ·  Mob "
                            + currentSessionMobLootQuantities().size()
                            + "  ·  Currency " + model.currencyEntryCount());

            String value = model.resolvedValueAvailable()
                    ? MiningSessionPriceBook.formatCoinAmount(
                            model.resolvedItemValue())
                    : "—";
            String valueUnit = model.resolvedValueAvailable() ? "coins (gross)" : "unavailable";
            drawAnalyticsSummaryCard(
                    graphics,
                    contentLeft + colWidth + colGap,
                    y,
                    colWidth,
                    summaryHeight,
                    "Values",
                    value,
                    valueUnit,
                    "Resolved " + model.resolvedEntryCount()
                            + "  ·  Open "
                            + (model.unresolvedEntryCount()
                            + model.staleEntryCount()
                            + model.unavailableEntryCount()
                            + model.unsupportedEntryCount())
                            + "  ·  Excluded "
                            + model.excludedCurrencyEntryCount());
        }
        y += summaryHeight + 10;

        // Resource breakdown — Analytics keeps precise source separation.
        // HUD may roll non-target item sources into OTHERS; Analytics does not.
        int resourceWidth = (availableWidth - SETTING_COLUMN_GAP * 3) / 4;
        int resourceTop = y;
        int resourceHeight = measureResourceCardHeight(model.targetQuantities());
        resourceHeight = Math.max(resourceHeight,
                measureResourceCardHeight(model.otherMinedQuantities()));
        resourceHeight = Math.max(resourceHeight,
                measureResourceCardHeight(model.chestLootQuantities()));
        resourceHeight = Math.max(resourceHeight,
                measureResourceCardHeight(model.currencyQuantities()));
        if (analyticsScroll.intersects(resourceTop, resourceHeight, clipTop, clipBottom)) {
            drawResourceCard(graphics, contentLeft, resourceTop, resourceWidth,
                    "Target Mined", model.targetQuantities());
            drawResourceCard(graphics,
                    contentLeft + resourceWidth + SETTING_COLUMN_GAP,
                    resourceTop, resourceWidth,
                    "Other Mined", model.otherMinedQuantities());
            drawResourceCard(graphics,
                    contentLeft + (resourceWidth + SETTING_COLUMN_GAP) * 2,
                    resourceTop, resourceWidth,
                    "Chest / Rewards", model.chestLootQuantities());
            drawResourceCard(graphics,
                    contentLeft + (resourceWidth + SETTING_COLUMN_GAP) * 3,
                    resourceTop, resourceWidth,
                    "Currency", model.currencyQuantities());
        }
        y = resourceTop + resourceHeight + 8;

        // Mob loot lives on Current Session (engine snapshot has no MOB category).
        java.util.Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                mobQuantities = currentSessionMobLootQuantities();
        int mobHeight = measureResourceCardHeight(mobQuantities);
        if (analyticsScroll.intersects(y, mobHeight, clipTop, clipBottom)) {
            drawResourceCard(
                    graphics,
                    contentLeft,
                    y,
                    resourceWidth,
                    mobLootCardTitle(),
                    mobQuantities);
        }
        y += mobHeight + 8;
        analyticsScroll.setBounds(
                RotClientScrollState.measureContentHeight(measuredStart, y),
                clipBottom - clipTop);
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }

        if (analyticsScroll.canScroll()) {
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    contentRight + 2,
                    clipTop,
                    clipBottom,
                    analyticsScroll.contentHeight(),
                    analyticsScroll.scrollPixels());
        }

        int buttonY = panelY + actionRowY();
        int buttonWidth = (availableWidth - BUTTON_GAP * 4) / 5;
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft, buttonY, buttonWidth,
                "Pause", presentation.pauseEnabled());
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft + buttonWidth + BUTTON_GAP, buttonY, buttonWidth,
                "Resume", presentation.resumeEnabled());
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft + (buttonWidth + BUTTON_GAP) * 2, buttonY,
                buttonWidth,
                awaitingNewSessionConfirm ? "Confirm?" : "Start New",
                presentation.startNewEnabled());
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft + (buttonWidth + BUTTON_GAP) * 3, buttonY,
                buttonWidth, "Copy", presentation.copyEnabled());
        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                contentLeft + (buttonWidth + BUTTON_GAP) * 4,
                buttonY,
                contentRight - (contentLeft + (buttonWidth + BUTTON_GAP) * 4),
                "Done",
                true,
                true);
    }

    private void drawAnalyticsSummaryCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String title,
            String metric,
            String metricHint,
            String detail) {
        RotClientUiDraw.drawElevatedCard(graphics, x, y, width, height);
        graphics.fill(
                x,
                y + 10,
                x + 3,
                y + height - 10,
                RotClientTheme.HUD_ACCENT);
        RotClientUiDraw.sectionLabel(graphics, font, title, x + 12, y + 8);
        RotClientUiDraw.metricValue(
                graphics,
                font,
                RotClientUiDraw.ellipsizeAndHover(font, metric, width - 24, x + 12, y + 22, 14),
                x + 12,
                y + 24,
                RotClientTheme.TEXT);
        RotClientUiDraw.helpText(
                graphics,
                font,
                RotClientUiDraw.ellipsizeAndHover(font, metricHint, width - 24, x + 12, y + 38, 12),
                x + 12,
                y + 40);
        RotClientUiDraw.bodyText(
                graphics,
                font,
                RotClientUiDraw.ellipsizeAndHover(font, detail, width - 24, x + 12, y + 54, 12),
                x + 12,
                y + 56);
    }

    private int measureResourceCardHeight(
            java.util.Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    quantities) {
        int rows = quantities == null || quantities.isEmpty()
                ? 1
                : Math.min(ANALYTICS_MAX_RESOURCE_ROWS, quantities.size());
        int omitted = quantities == null ? 0
                : Math.max(0, quantities.size() - ANALYTICS_MAX_RESOURCE_ROWS);
        // Header + column labels + rows (+ optional omitted line).
        return 40 + rows * 16 + (omitted > 0 ? 14 : 0);
    }

    private String mobLootCardTitle() {
        int magicFind = RotClientClient.currentSessionSnapshot().lastMagicFind;
        long observedAt =
                RotClientClient.currentSessionSnapshot().lastMagicFindAtMillis;
        if (magicFind < 0 || observedAt <= 0L) {
            return "Mob Loot";
        }
        return "Mob Loot · MF " + magicFind;
    }

    /**
     * Mob loot item quantities from the canonical Current Session ledger.
     * The mining-engine analytics snapshot has no MOB category — Session
     * Analytics therefore reads MOB rows from Current Session so source
     * separation stays intact while HUD OTHERS can still roll them up.
     */
    private java.util.Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            currentSessionMobLootQuantities() {
        java.util.LinkedHashMap<String,
                MiningSessionAnalyticsViewModel.ResourceQuantity> result =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, RotClientCurrentSessionMath.ProjectedQuantity>
                projected = RotClientCurrentSessionMath.quantitiesForSource(
                        RotClientClient.currentSessionSnapshot().items,
                        SessionSourceType.MOB);
        for (RotClientCurrentSessionMath.ProjectedQuantity quantity
                : projected.values()) {
            if (quantity.quantity() <= 0L || quantity.itemId().isBlank()) {
                continue;
            }
            result.put(
                    quantity.itemId(),
                    new MiningSessionAnalyticsViewModel.ResourceQuantity(
                            quantity.itemId(),
                            quantity.displayName().isBlank()
                                    ? quantity.itemId()
                                    : quantity.displayName(),
                            quantity.quantity()));
        }
        return result;
    }

    private void drawResourceCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            String title,
            java.util.Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    quantities) {
        int height = measureResourceCardHeight(quantities);
        RotClientUiDraw.drawElevatedCard(graphics, x, y, width, height);
        RotClientUiDraw.sectionLabel(graphics, font, title, x + 10, y + 8);
        boolean wide = width >= 150;
        if (wide) {
            RotClientUiDraw.helpText(graphics, font, "ITEM", x + 10, y + 22);
            drawRight(graphics, "QTY", x + width - 10, y + 22, RotClientTheme.TEXT_DIM);
        }
        int rowY = y + 36;
        if (quantities == null || quantities.isEmpty()) {
            RotClientUiDraw.helpText(graphics, font, "None yet", x + 10, rowY);
            return;
        }
        int shown = 0;
        int omitted = 0;
        java.util.Map<String, RotClientCurrentSessionConfig.SessionItemRecord>
                sessionItems = indexCurrentSessionItems();
        for (MiningSessionAnalyticsViewModel.ResourceQuantity quantity
                : quantities.values()) {
            if (shown >= ANALYTICS_MAX_RESOURCE_ROWS) {
                omitted++;
                continue;
            }
            String fullName = quantity.displayName();
            String label = fullName;
            int qtyWidth = font.width(Long.toString(quantity.quantity())) + 8;
            int nameBudget = Math.max(40, width - qtyWidth - 20);
            if (font.width(label) > nameBudget) {
                label = quantity.resourceId();
            }
            label = RotClientUiDraw.ellipsizeAndHover(font, fullName, nameBudget, x + 10, rowY, 14);
            RotClientUiDraw.bodyText(graphics, font, label, x + 10, rowY);
            String qtyLabel = Long.toString(quantity.quantity());
            RotClientCurrentSessionConfig.SessionItemRecord sessionItem =
                    sessionItems.get(quantity.resourceId());
            if (sessionItem != null
                    && sessionItem.price()
                    != RotClientCurrentSessionConfig.PriceStatus
                    .RESOLVED_BAZAAR) {
                qtyLabel = qtyLabel + " · ?";
            }
            drawRight(graphics, qtyLabel,
                    x + width - 10, rowY, RotClientTheme.TEXT);
            rowY += 16;
            shown++;
        }
        if (omitted > 0) {
            RotClientUiDraw.helpText(
                    graphics, font, "+" + omitted + " more", x + 10, rowY);
        }
    }

    private java.util.Map<String, RotClientCurrentSessionConfig.SessionItemRecord>
            indexCurrentSessionItems() {
        java.util.LinkedHashMap<String,
                RotClientCurrentSessionConfig.SessionItemRecord> map =
                new java.util.LinkedHashMap<>();
        java.util.List<RotClientCurrentSessionConfig.SessionItemRecord> items =
                RotClientClient.currentSessionSnapshot().items;
        if (items == null) {
            return map;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || item.itemId().isBlank()) {
                continue;
            }
            map.putIfAbsent(item.itemId(), item);
        }
        return map;
    }

    private void drawAnalyticsCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            String title,
            String body) {
        roundedFill(graphics, x, y, x + width, y + 46, RotClientTheme.SURFACE_ALT);
        RotClientUiDraw.text(graphics, font, title, x + 8, y + 5, RotClientTheme.TEXT_DIM, true);
        RotClientUiDraw.text(graphics, font, body, x + 8, y + 22, RotClientTheme.TEXT, false);
    }

    private void drawAnalyticsValuesCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            MiningSessionAnalyticsViewModel model) {
        roundedFill(graphics, x, y, x + width, y + 46, RotClientTheme.SURFACE_ALT);
        RotClientUiDraw.text(graphics, font, "VALUES", x + 8, y + 3, RotClientTheme.TEXT_DIM, true);
        String value = model.resolvedValueAvailable()
                ? MiningSessionPriceBook.formatCoinAmount(
                        model.resolvedItemValue())
                + " coins"
                : "unavailable";
        RotClientUiDraw.text(graphics, font,
                "Price basis: Bazaar instant sell (gross)  |  Value: "
                        + value,
                x + 8, y + 14, RotClientTheme.TEXT, false);
        RotClientUiDraw.text(graphics, font,
                "Resolved " + model.resolvedEntryCount()
                        + " | Unresolved " + model.unresolvedEntryCount()
                        + " | Stale " + model.staleEntryCount(),
                x + 8, y + 25, RotClientTheme.TEXT_DIM, false);
        String age = model.priceBookAgeMillis().isPresent()
                ? " | Age "
                + (model.priceBookAgeMillis().getAsLong() / 1_000L)
                + "s"
                : "";
        RotClientUiDraw.text(graphics, font,
                "Unavailable " + model.unavailableEntryCount()
                        + " | Unsupported " + model.unsupportedEntryCount()
                        + " | Excluded currency "
                        + model.excludedCurrencyEntryCount()
                        + age,
                x + 8, y + 35, RotClientTheme.TEXT_DIM, false);
    }

    private int drawResourceColumn(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            String title,
            java.util.Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    quantities) {
        RotClientUiDraw.text(graphics, font, title, x + 2, y, RotClientTheme.TEXT_DIM, true);
        int rowY = y + 12;
        if (quantities == null || quantities.isEmpty()) {
            RotClientUiDraw.text(graphics, font, "(none)", x + 2, rowY, RotClientTheme.TEXT_MUTED, false);
            return y;
        }
        int shown = 0;
        int omitted = 0;
        for (MiningSessionAnalyticsViewModel.ResourceQuantity quantity
                : quantities.values()) {
            if (shown >= ANALYTICS_MAX_RESOURCE_ROWS) {
                omitted++;
                continue;
            }
            String label = quantity.displayName();
            if (font.width(label) > width - 28) {
                label = quantity.resourceId();
            }
            RotClientUiDraw.text(graphics, font, label, x + 2, rowY, RotClientTheme.TEXT_DIM, false);
            drawRight(graphics, Long.toString(quantity.quantity()),
                    x + width - 2, rowY, RotClientTheme.TEXT);
            rowY += 11;
            shown++;
        }
        if (omitted > 0) {
            RotClientUiDraw.text(graphics, font, "+" + omitted + " more",
                    x + 2, rowY, RotClientTheme.WARNING, false);
        }
        return y;
    }

    private void drawActionButton(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            String label,
            boolean enabled) {
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x, y, width, label, false, enabled);
    }

    private String formatAnalyticsSessionTime(MiningSessionAnalyticsViewModel model) {
        long now = System.currentTimeMillis();
        long millis = RotClientClient.currentSessionActiveDurationMillis(now);
        if (millis <= 0L && model.sessionStartedMillis().isEmpty()) {
            return "—";
        }
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }

    private String formatAnalyticsCoinsPerHour(MiningSessionAnalyticsViewModel model) {
        if (!model.resolvedValueAvailable()) {
            return "—";
        }
        long now = System.currentTimeMillis();
        long millis = RotClientClient.currentSessionActiveDurationMillis(now);
        if (millis < 1_000L) {
            return "—";
        }
        try {
            double value = model.resolvedItemValue().doubleValue();
            if (!Double.isFinite(value) || value < 0) {
                return "—";
            }
            double perHour = value * 3_600_000.0 / millis;
            return MiningSessionPriceBook.formatCoinAmount(
                    java.math.BigDecimal.valueOf(perHour));
        } catch (Exception ignored) {
            return "—";
        }
    }

    private void drawSessionHistory(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        MiningSessionHistoryPresentation presentation =
                RotClientClient.sessionHistoryPresentation();
        if (presentation.detailOpen()) {
            drawSessionHistoryDetail(
                    graphics,
                    panelX,
                    panelY,
                    mouseX,
                    mouseY,
                    presentation);
            return;
        }

        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int availableWidth = contentRight - contentLeft;
        int y = panelY + MASTER_Y;

        roundedFill(graphics, contentLeft, y,
                contentRight, y + 34, RotClientTheme.SELECTED_ROW);
        graphics.fill(contentLeft, y + 4, contentLeft + 3, y + 30, RotClientTheme.HUD_ACCENT);
        RotClientUiDraw.text(graphics, font, "SESSION HISTORY",
                contentLeft + 10, y + 5, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, "Local saved sessions",
                contentLeft + 10, y + 18, RotClientTheme.TEXT_DIM, false);
        drawRight(graphics,
                presentation.available()
                        ? presentation.sessions().size()
                        + " / "
                        + MiningSessionHistoryStore.MAX_RECORDS
                        : "UNAVAILABLE",
                contentRight - 10, y + 12, RotClientTheme.TEXT_DIM);

        y += 40;
        if (presentation.emptyStateMessage() != null) {
            RotClientUiDraw.text(graphics, font, presentation.emptyStateMessage(),
                    contentLeft + 2, y, RotClientTheme.WARNING, false);
            y += 14;
        }

        List<MiningSessionHistoryRecord> sessions = presentation.sessions();
        int visible = Math.min(HISTORY_MAX_VISIBLE_ROWS, sessions.size());
        for (int i = 0; i < visible; i++) {
            MiningSessionHistoryRecord record = sessions.get(i);
            int rowY = y + i * HISTORY_ROW_HEIGHT;
            roundedFill(graphics, contentLeft, rowY,
                    contentRight, rowY + HISTORY_ROW_HEIGHT - 2, RotClientTheme.SURFACE_ALT);
            RotClientUiDraw.text(graphics, font,
                    MiningSessionHistorySummaries.listTitle(record, i + 1),
                    contentLeft + 8, rowY + 4, RotClientTheme.TEXT, false);
            RotClientUiDraw.text(graphics, font,
                    MiningSessionHistorySummaries.listSubtitle(record),
                    contentLeft + 8, rowY + 18, RotClientTheme.TEXT_DIM, false);

            int actionWidth = 52;
            int actionGap = 4;
            int actionX = contentRight - (actionWidth * 3 + actionGap * 2) - 6;
            drawActionButton(graphics, mouseX, mouseY,
                    actionX, rowY + 3, actionWidth, "OPEN", true);
            drawActionButton(graphics, mouseX, mouseY,
                    actionX + actionWidth + actionGap, rowY + 3, actionWidth,
                    "COPY", true);
            drawActionButton(graphics, mouseX, mouseY,
                    actionX + (actionWidth + actionGap) * 2, rowY + 3,
                    actionWidth, "DELETE", true);
        }
        if (sessions.size() > visible) {
            RotClientUiDraw.text(graphics, font,
                    "+" + (sessions.size() - visible)
                            + " more saved locally",
                    contentLeft + 2,
                    y + visible * HISTORY_ROW_HEIGHT,
                    RotClientTheme.WARNING,
                    false);
        }

        int buttonY = panelY + actionRowY();
        int buttonWidth = (availableWidth - BUTTON_GAP * 3) / 4;
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft, buttonY, buttonWidth,
                "SAVE SESSION", presentation.saveEnabled());
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft + buttonWidth + BUTTON_GAP, buttonY, buttonWidth,
                presentation.awaitingClearConfirm()
                        ? "CONFIRM CLEAR"
                        : "CLEAR HISTORY",
                presentation.clearEnabled()
                        || presentation.awaitingClearConfirm());
        drawButton(graphics, mouseX, mouseY,
                contentLeft + (buttonWidth + BUTTON_GAP) * 2, buttonY,
                contentRight - (contentLeft + (buttonWidth + BUTTON_GAP) * 2),
                "DONE", RotClientTheme.HUD_ACCENT, true);
    }

    private void drawSessionHistoryDetail(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY,
            MiningSessionHistoryPresentation presentation) {
        MiningSessionAnalyticsViewModel model =
                presentation.selectedViewModel();
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int availableWidth = contentRight - contentLeft;
        int y = panelY + MASTER_Y;

        roundedFill(graphics, contentLeft, y,
                contentRight, y + 34, RotClientTheme.SELECTED_ROW);
        graphics.fill(contentLeft, y + 4, contentLeft + 3, y + 30, RotClientTheme.TEXT_DIM);
        RotClientUiDraw.text(graphics, font, "SAVED SESSION",
                contentLeft + 10, y + 5, RotClientTheme.TEXT, true);
        String detailMeta = MiningSessionHistorySummaries.detailBadge();
        if (presentation.selected().isPresent()) {
            MiningSessionHistoryRecord selected =
                    presentation.selected().orElseThrow();
            detailMeta = MiningSessionHistorySummaries.whenLabel(selected)
                    + "  ·  active "
                    + MiningSessionHistorySummaries.durationLabel(selected)
                    + "  ·  paused "
                    + MiningSessionHistorySummaries.pausedDurationLabel(selected)
                    + "  ·  "
                    + MiningSessionHistorySummaries.detailBadge();
        }
        RotClientUiDraw.text(graphics, font, detailMeta,
                contentLeft + 10, y + 18, RotClientTheme.TEXT_DIM, false);
        drawRight(graphics, "FROZEN",
                contentRight - 10, y + 12, RotClientTheme.WARNING);

        y += 40;
        RotClientUiDraw.text(graphics, font,
                "Target: " + model.selectedTargetDisplayName()
                        + "  |  Tracker: "
                        + (model.trackerEnabled() ? "ON" : "OFF")
                        + "  |  Parity: "
                        + model.parityStatusLabel()
                        + "  |  Mismatches: "
                        + model.mismatchCount(),
                contentLeft + 2, y, RotClientTheme.TEXT_DIM, false);

        Optional<MiningSessionHistoryRecord> selectedRecord =
                presentation.selected();
        if (selectedRecord.isPresent()
                && selectedRecord.get().currentSessionFreeze().isPresent()) {
            RotClientSessionFreeze frozen = selectedRecord.get()
                    .currentSessionFreeze().orElseThrow();
            y += 12;
            RotClientUiDraw.text(graphics, font,
                    "Target segments: " + frozen.targetSegments().size()
                            + "  |  Area segments: "
                            + frozen.areaSegments().size()
                            + "  |  Item rows: "
                            + frozen.itemRows().size(),
                    contentLeft + 2,
                    y,
                    RotClientTheme.TEXT_DIM,
                    false);
            y += 10;
            RotClientUiDraw.text(graphics, font,
                    "MOB/CHEST/CURRENCY: SCHEMA READY · LIVE INGRESS GATED",
                    contentLeft + 2,
                    y,
                    RotClientTheme.TEXT_DIM,
                    false);
        }

        y += 16;
        int columnWidth = (availableWidth - SETTING_COLUMN_GAP) / 2;
        drawAnalyticsCard(
                graphics,
                contentLeft,
                y,
                columnWidth,
                "ENTRIES",
                "TARGET " + model.targetEntryCount()
                        + "  OTHER MINED " + model.otherEntryCount()
                        + "  CHEST " + model.chestLootEntryCount()
                        + "  MOB "
                        + selectedRecord.map(record ->
                        MiningSessionHistorySummaries.sourceRowCount(
                                record, SessionSourceType.MOB)).orElse(0)
                        + "  CURRENCY " + model.currencyEntryCount()
                        + "  TOTAL " + model.entryCount());
        drawAnalyticsValuesCard(
                graphics,
                contentLeft + columnWidth + SETTING_COLUMN_GAP,
                y,
                columnWidth,
                model);

        y += 52;
        int resourceWidth = (availableWidth - SETTING_COLUMN_GAP * 3) / 4;
        drawResourceColumn(
                graphics,
                contentLeft,
                y,
                resourceWidth,
                "TARGET MINED",
                model.targetQuantities());
        drawResourceColumn(
                graphics,
                contentLeft + resourceWidth + SETTING_COLUMN_GAP,
                y,
                resourceWidth,
                "OTHER MINED",
                model.otherMinedQuantities());
        drawResourceColumn(
                graphics,
                contentLeft + (resourceWidth + SETTING_COLUMN_GAP) * 2,
                y,
                resourceWidth,
                "CHEST / MOB",
                selectedRecord.map(
                        MiningSessionHistorySummaries::chestAndMobQuantities)
                        .orElse(model.chestLootQuantities()));
        drawResourceColumn(
                graphics,
                contentLeft + (resourceWidth + SETTING_COLUMN_GAP) * 3,
                y,
                resourceWidth,
                "CURRENCY",
                model.currencyQuantities());

        int buttonY = panelY + actionRowY();
        int buttonWidth = (availableWidth - BUTTON_GAP * 3) / 4;
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft, buttonY, buttonWidth, "BACK", true);
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft + buttonWidth + BUTTON_GAP, buttonY, buttonWidth,
                "COPY", true);
        drawActionButton(graphics, mouseX, mouseY,
                contentLeft + (buttonWidth + BUTTON_GAP) * 2, buttonY,
                buttonWidth, "DELETE", true);
        drawButton(graphics, mouseX, mouseY,
                contentLeft + (buttonWidth + BUTTON_GAP) * 3, buttonY,
                contentRight - (contentLeft + (buttonWidth + BUTTON_GAP) * 3),
                "DONE", RotClientTheme.HUD_ACCENT, true);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (selectedModule == DashboardModule.QOL_SETTINGS
                && qolDashboard.captureChar(event.codepoint(), event.isAllowedChatCharacter())) {
            return true;
        }
        if (settingsSearchFocused
                && !trackerDropdownOpen) {
            if (!event.isAllowedChatCharacter()) {
                return true;
            }
            if (settingsQuery.codePointCount(0, settingsQuery.length()) >= 48) {
                return true;
            }
            settingsQuery += event.codepointAsString();
            omniboxHighlight = 0;
            return true;
        }
        if (!trackerDropdownOpen) {
            return super.charTyped(event);
        }

        if (!event.isAllowedChatCharacter()) {
            return true;
        }

        if (trackerSearch.codePointCount(
                0,
                trackerSearch.length()) >= 32) {
            return true;
        }

        trackerSearch += event.codepointAsString();
        trackerDropdownHighlight = 0;
        trackerDropdownScroll = 0;

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (selectedModule == DashboardModule.QOL_SETTINGS
                && qolDashboard.captureKey(key)) {
            return true;
        }
        if (controlDown(event)) {
            if (key == GLFW.GLFW_KEY_T) {
                RotClientClient.workspace().addOverviewTab();
                syncSelectedModuleFromWorkspace();
                return true;
            }
            if (key == GLFW.GLFW_KEY_W) {
                RotClientWorkspace workspace = RotClientClient.workspace();
                workspace.closeTab(workspace.config().activeTabId);
                syncSelectedModuleFromWorkspace();
                return true;
            }
            if (key == GLFW.GLFW_KEY_TAB) {
                cycleWorkspaceTab(shiftDown(event) ? -1 : 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_L) {
                settingsSearchFocused = true;
                return true;
            }
        }

        if (settingsSearchFocused
                && !trackerDropdownOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                settingsSearchFocused = false;
                settingsQuery = "";
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!settingsQuery.isEmpty()) {
                    int newLength = settingsQuery.offsetByCodePoints(
                            settingsQuery.length(), -1);
                    settingsQuery = settingsQuery.substring(0, newLength);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                List<RotClientSettingsIndex.Entry> results =
                        RotClientOmniboxPolicy.suggest(settingsQuery);
                if (!results.isEmpty()) {
                    int index = Math.max(0, Math.min(omniboxHighlight, results.size() - 1));
                    activateSettingsEntry(results.get(index), controlDown(event));
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DOWN) {
                omniboxHighlight++;
                return true;
            }
            if (key == GLFW.GLFW_KEY_UP) {
                omniboxHighlight = Math.max(0, omniboxHighlight - 1);
                return true;
            }
            return true;
        }

        if (selectedModule == DashboardModule.QOL_SETTINGS
                && key == GLFW.GLFW_KEY_ESCAPE
                && qolDashboard.isDrawerOpen()) {
            qolDashboard.closeDrawer();
            return true;
        }

        if (!trackerDropdownOpen) {
            return super.keyPressed(event);
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            trackerDropdownOpen = false;
            trackerSearch = "";
            trackerDropdownHighlight = 0;
            trackerDropdownScroll = 0;
            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!trackerSearch.isEmpty()) {
                int newLength = trackerSearch.offsetByCodePoints(
                        trackerSearch.length(),
                        -1);

                trackerSearch = trackerSearch.substring(
                        0,
                        newLength);
            }

            trackerDropdownHighlight = 0;
            trackerDropdownScroll = 0;
            return true;
        }

        List<TrackerSelection> options =
                filteredTrackerSelections();

        if (key == GLFW.GLFW_KEY_UP) {
            if (!options.isEmpty()) {
                trackerDropdownHighlight = Math.max(
                        0,
                        trackerDropdownHighlight - 1);

                keepDropdownHighlightVisible(
                        options.size());
            }

            return true;
        }

        if (key == GLFW.GLFW_KEY_DOWN) {
            if (!options.isEmpty()) {
                trackerDropdownHighlight = Math.min(
                        options.size() - 1,
                        trackerDropdownHighlight + 1);

                keepDropdownHighlightVisible(
                        options.size());
            }

            return true;
        }

        if (
            key == GLFW.GLFW_KEY_ENTER ||
            key == GLFW.GLFW_KEY_KP_ENTER
        ) {
            if (!options.isEmpty()) {
                int selectedIndex = Math.max(
                        0,
                        Math.min(
                                trackerDropdownHighlight,
                                options.size() - 1));

                RotClientClient.setTrackingSelection(
                        options.get(selectedIndex));
            }

            trackerDropdownOpen = false;
            trackerSearch = "";
            trackerDropdownHighlight = 0;
            trackerDropdownScroll = 0;

            return true;
        }

        return super.keyPressed(event);
    }

    private void keepDropdownHighlightVisible(
            int optionCount) {
        if (optionCount <= 0) {
            trackerDropdownHighlight = 0;
            trackerDropdownScroll = 0;
            return;
        }

        trackerDropdownHighlight = Math.max(
                0,
                Math.min(
                        trackerDropdownHighlight,
                        optionCount - 1));

        if (
            trackerDropdownHighlight <
            trackerDropdownScroll
        ) {
            trackerDropdownScroll =
                    trackerDropdownHighlight;
        }
        else if (
            trackerDropdownHighlight >=
            trackerDropdownScroll + 6
        ) {
            trackerDropdownScroll =
                    trackerDropdownHighlight - 5;
        }

        int maxScroll = Math.max(
                0,
                optionCount - 6);

        trackerDropdownScroll = Math.max(
                0,
                Math.min(
                        trackerDropdownScroll,
                        maxScroll));
    }

    private void closeTrackerDropdown() {
        trackerDropdownOpen = false;
        trackerSearch = "";
        trackerDropdownHighlight = 0;
        trackerDropdownScroll = 0;
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
        RotClientTabStrip.Layout tabLayout = tabStripLayout(panelX, panelY);
        if (RotClientUiDraw.inside(
                logicalX,
                logicalY,
                tabLayout.x(),
                tabLayout.y(),
                tabLayout.width() + RotClientTabStrip.NEW_TAB_WIDTH + 10,
                RotClientTabStrip.HEIGHT)
                && tabLayout.maxScroll() > 0) {
            int next = tabStripScroll - (int) Math.round(verticalAmount * 16.0D);
            tabStripScroll = Math.max(0, Math.min(tabLayout.maxScroll(), next));
            return true;
        }
        int sidebarTop = panelY + CHROME_HEIGHT;
        int homeY = panelY + panelH() - HOME_BUTTON_Y_OFFSET;
        int sidebarBottom = homeY - 10;
        if (RotClientUiDraw.inside(
                logicalX,
                logicalY,
                panelX,
                sidebarTop,
                SIDEBAR_WIDTH,
                Math.max(0, sidebarBottom - sidebarTop))) {
            RotClientSidebarNav.Layout layout = sidebarLayout(false);
            sidebarScroll.setBounds(
                    layout.contentHeight(OVERVIEW_MODULE_Y),
                    Math.max(0, sidebarBottom - sidebarTop));
            if (!sidebarScroll.canScroll()) {
                return false;
            }
            sidebarScroll.scrollBySteps(verticalAmount, 40);
            return true;
        }
        if (selectedModule == DashboardModule.SESSION_ANALYTICS
                && !trackerDropdownOpen
                && !RotClientClient.workspace().activeRoute().isAppearance()) {
            int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
            int contentRight = panelX + panelW() - CONTENT_INSET;
            int clipTop = panelY + MASTER_Y;
            int clipBottom = panelY + actionRowY() - 10;
            if (!inside(
                    logicalX,
                    logicalY,
                    contentLeft,
                    clipTop,
                    contentRight - contentLeft,
                    clipBottom - clipTop)) {
                return false;
            }
            if (!analyticsScroll.canScroll()) {
                return false;
            }
            analyticsScroll.scrollBySteps(verticalAmount, 40);
            return true;
        }
        if (selectedModule == DashboardModule.QOL_SETTINGS
                && !trackerDropdownOpen) {
            int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
            int contentRight = panelX + panelW() - CONTENT_INSET;
            int contentTop = panelY + MASTER_Y;
            int contentBottom = panelY + actionRowY() - 10;
            if (qolDashboard.mouseScrolled(
                    logicalX,
                    logicalY,
                    verticalAmount,
                    contentLeft,
                    contentTop,
                    contentRight,
                    contentBottom)) {
                return true;
            }
        }
        if (!trackerDropdownOpen) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    horizontalAmount,
                    verticalAmount);
        }

        int contentRight = panelX + panelW() - CONTENT_INSET;
        int masterY = panelY + MASTER_Y;
        int switchX = contentRight - MASTER_SWITCH_WIDTH - 12;
        int selectorX = switchX - MASTER_CONTROL_GAP - 180;
        int selectorY = masterY + 10;
        int dropdownHeight = 20 + 4 + 6 * 22;
        if (!inside(logicalX, logicalY, selectorX, selectorY, 180, dropdownHeight)) {
            return false;
        }

        List<TrackerSelection> options =
                filteredTrackerSelections();

        int maxScroll = Math.max(
                0,
                options.size() - 6);

        if (verticalAmount > 0.0D) {
            trackerDropdownScroll = Math.max(
                    0,
                    trackerDropdownScroll - 1);
        }
        else if (verticalAmount < 0.0D) {
            trackerDropdownScroll = Math.min(
                    maxScroll,
                    trackerDropdownScroll + 1);
        }

        trackerDropdownHighlight = Math.max(
                trackerDropdownScroll,
                Math.min(
                        trackerDropdownHighlight,
                        Math.max(0, options.size() - 1)));

        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float uiScale = uiScale();
        double logicalMouseX = event.x() / uiScale;
        double logicalMouseY = event.y() / uiScale;
        int panelX = panelX();
        int panelY = panelY();
        openInNewTabGesture = controlDown(event)
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

        if (handleDashboardWindowClick(
                event, doubleClick, logicalMouseX, logicalMouseY, panelX, panelY)) {
            return true;
        }

        if (selectedModule == DashboardModule.QOL_SETTINGS
                && (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
            int contentRight = panelX + panelW() - CONTENT_INSET;
            int contentTop = panelY + MASTER_Y;
            int contentBottom = panelY + actionRowY() - 10;
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    && inside(logicalMouseX, logicalMouseY,
                    contentRight - 148, panelY + actionRowY(),
                    148, BUTTON_HEIGHT)) {
                onClose();
                return true;
            }
            if (qolDashboard.mouseClicked(
                    event.button(),
                    logicalMouseX,
                    logicalMouseY,
                    contentLeft,
                    contentTop,
                    contentRight,
                    contentBottom)) {
                return true;
            }
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                return true;
            }
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            return super.mouseClicked(event, doubleClick);
        }

        // Handle the open tracker dropdown before other controls.
        if (trackerDropdownOpen && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int dropdownContentRight =
                    panelX + panelW() - CONTENT_INSET;
            int dropdownMasterY =
                    panelY + MASTER_Y;
            int dropdownSwitchX =
                    dropdownContentRight
                            - MASTER_CONTROL_RIGHT_INSET
                            - MASTER_SWITCH_WIDTH;
            int dropdownSelectorX =
                    dropdownSwitchX
                            - MASTER_CONTROL_GAP
                            - 180;
            int dropdownSelectorY =
                    dropdownMasterY + 10;

            if (inside(
                    logicalMouseX,
                    logicalMouseY,
                    dropdownSelectorX,
                    dropdownSelectorY,
                    180,
                    20)) {
                closeTrackerDropdown();
                return true;
            }

            List<TrackerSelection> dropdownOptions =
                    filteredTrackerSelections();

            int visibleCount = Math.min(
                    Math.max(
                            dropdownOptions.size()
                                    - trackerDropdownScroll,
                            0),
                    6);

            int dropdownRowY =
                    dropdownSelectorY + 24;

            for (int i = 0; i < visibleCount; i++) {
                int optionIndex =
                        trackerDropdownScroll + i;

                if (inside(
                        logicalMouseX,
                        logicalMouseY,
                        dropdownSelectorX,
                        dropdownRowY,
                        180,
                        20)) {
                    RotClientClient.setTrackingSelection(
                            dropdownOptions.get(optionIndex));

                    closeTrackerDropdown();
                    return true;
                }

                dropdownRowY += 22;
            }

            closeTrackerDropdown();
            return true;
        }

        if (inside(logicalMouseX, logicalMouseY,
                panelX + MODULE_X_OFFSET,
                panelY + panelH() - HOME_BUTTON_Y_OFFSET,
                MODULE_WIDTH,
                BUTTON_HEIGHT)) {
            openOverview();
            return true;
        }

        RotClientWorkspace workspace = RotClientClient.workspace();
        RotClientSidebarNav.Layout layout = sidebarLayout(false);
        int sidebarTop = panelY + CHROME_HEIGHT;
        int homeY = panelY + panelH() - HOME_BUTTON_Y_OFFSET;
        int sidebarBottom = homeY - 10;
        int viewportHeight = Math.max(0, sidebarBottom - sidebarTop);
        sidebarScroll.setBounds(
                layout.contentHeight(OVERVIEW_MODULE_Y),
                viewportHeight);
        int sidebarScrollbarX = panelX + SIDEBAR_WIDTH
                - RotClientUiDraw.SCROLLBAR_HIT_WIDTH;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && sidebarScroll.canScroll()
                && logicalMouseX >= sidebarScrollbarX
                && logicalMouseX < sidebarScrollbarX
                + RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                && logicalMouseY >= sidebarTop
                && logicalMouseY < sidebarBottom) {
            if (sidebarScroll.beginThumbDrag(
                    (int) Math.round(logicalMouseY),
                    sidebarTop,
                    sidebarBottom,
                    RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)
                    || sidebarScroll.clickTrack(
                    (int) Math.round(logicalMouseY),
                    sidebarTop,
                    sidebarBottom,
                    RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {
                return true;
            }
        }
        int localX = (int) Math.round(logicalMouseX) - panelX;
        // Map screen Y into layout/content coordinates (origin at OVERVIEW_MODULE_Y).
        int localY = (int) Math.round(logicalMouseY) - sidebarTop
                + sidebarScroll.scrollPixels()
                + OVERVIEW_MODULE_Y;
        boolean overSidebarViewport = RotClientUiDraw.inside(
                (int) Math.round(logicalMouseX),
                (int) Math.round(logicalMouseY),
                panelX,
                sidebarTop,
                SIDEBAR_WIDTH,
                viewportHeight);
        RotClientSidebarNav.HitTarget hit = overSidebarViewport
                ? RotClientSidebarNav.hitTest(
                        layout,
                        localX,
                        localY,
                        MODULE_X_OFFSET,
                        MODULE_WIDTH,
                        12,
                        MODULE_WIDTH)
                : RotClientSidebarNav.HitTarget.NONE;
        switch (hit) {
            case OVERVIEW -> {
                selectModule(DashboardModule.NONE);
                return true;
            }
            case TRACKER -> {
                selectModule(DashboardModule.MINING_TRACKER);
                return true;
            }
            case POWDER_CHEST_TRACKER -> {
                selectModule(DashboardModule.POWDER_CHEST_TRACKER);
                return true;
            }
            case MINING_HUD -> {
                openAppearanceCustomizer(
                        RotClientAppearanceScreen.Section.MINING_HUD);
                return true;
            }
            case ANALYTICS -> {
                selectModule(DashboardModule.SESSION_ANALYTICS);
                return true;
            }
            case HISTORY -> {
                selectModule(DashboardModule.SESSION_HISTORY);
                return true;
            }
            case APPEARANCE -> {
                openAppearanceCustomizer();
                return true;
            }
            case HUD_LAYOUT -> {
                RotClientClient.openHudEditor(this);
                return true;
            }
            case QOL_COMBAT, QOL_SLAYER, QOL_FISHING, QOL_FORAGING, QOL_DUNGEONS, QOL_MINING,
                 QOL_UTILITIES, QOL_HUD_DISPLAY, QOL_RENDER, QOL_INTERFACE -> {
                QolUtilityCatalog.Group group =
                        RotClientSidebarNav.groupForHitTarget(hit);
                if (group != null) {
                    selectQolPage(group);
                }
                return true;
            }
            case SECTION_MINING -> {
                workspace.toggleSidebarSection(RotClientSidebarNav.SECTION_MINING);
                reclampSidebarScroll();
                return true;
            }
            case SECTION_SESSIONS -> {
                workspace.toggleSidebarSection(RotClientSidebarNav.SECTION_SESSIONS);
                reclampSidebarScroll();
                return true;
            }
            case SECTION_SETTINGS -> {
                workspace.toggleSidebarSection(RotClientSidebarNav.SECTION_SETTINGS);
                reclampSidebarScroll();
                return true;
            }
            case SECTION_QOL -> {
                workspace.toggleSidebarSection(RotClientSidebarNav.SECTION_QOL);
                reclampSidebarScroll();
                return true;
            }
            case NONE -> {
            }
        }

        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        if (workspace.activeRoute().isAppearance()) {
            int cardY = panelY + MASTER_Y;
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + 16, cardY + 70, 168, BUTTON_HEIGHT)) {
                openAppearanceCustomizer();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (selectedModule == DashboardModule.NONE) {
            int contentWidth = contentRight - contentLeft;
            int cardY = panelY + CHROME_HEIGHT + 52;
            int gap = 10;
            int cardWidth = (contentWidth - gap) / 2;
            int row2Y = cardY + 84 + gap;
            settingsSearchFocused = false;
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft, cardY, cardWidth, 84)
                    || inside(logicalMouseX, logicalMouseY,
                    contentLeft, panelY + actionRowY(), 148, BUTTON_HEIGHT)) {
                selectModule(DashboardModule.QOL_SETTINGS);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + cardWidth + gap, cardY, cardWidth, 84)) {
                openAppearanceCustomizer();
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft, row2Y, cardWidth, 84)
                    || inside(logicalMouseX, logicalMouseY,
                    contentLeft + 316, panelY + actionRowY(), 148, BUTTON_HEIGHT)) {
                selectModule(DashboardModule.MINING_TRACKER);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + cardWidth + gap, row2Y, cardWidth, 84)) {
                selectModule(DashboardModule.SESSION_ANALYTICS);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + 158, panelY + actionRowY(), 148, BUTTON_HEIGHT)) {
                RotClientClient.openHudEditor(this);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentRight - 148, panelY + actionRowY(),
                    148, BUTTON_HEIGHT)) {
                onClose();
                return true;
            }
            int linkY = row2Y + 100;
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft, linkY + 14, 148, BUTTON_HEIGHT)) {
                RotClientLinkOpener.openConfirmed(this, RotClientLinks.DISCORD);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + 158, linkY + 14, 128, BUTTON_HEIGHT)) {
                RotClientLinkOpener.openConfirmed(this, RotClientLinks.SOURCE);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + 296, linkY + 14, 110, BUTTON_HEIGHT)) {
                RotClientLinkOpener.openConfirmed(this, RotClientLinks.ISSUES);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (selectedModule == DashboardModule.QOL_SETTINGS) {
            // Handled at the top of mouseClicked (supports left + right).
            return super.mouseClicked(event, doubleClick);
        }
        if (selectedModule == DashboardModule.POWDER_CHEST_TRACKER) {
            int masterY = panelY + MASTER_Y;
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft, masterY,
                    contentRight - contentLeft, MASTER_HEIGHT)) {
                RotClientClient.setPowderChestTrackerEnabled(
                        !config.powderChestTrackerEnabled);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft, panelY + actionRowY(),
                    148, BUTTON_HEIGHT)) {
                RotClientClient.setPowderChestHudEnabled(
                        !config.powderChestHudEnabled);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft + 158, panelY + actionRowY(),
                    148, BUTTON_HEIGHT)) {
                RotClientClient.openHudEditor(this);
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    contentRight - 148, panelY + actionRowY(),
                    148, BUTTON_HEIGHT)) {
                onClose();
                return true;
            }
            return true;
        }
        if (selectedModule == DashboardModule.SESSION_ANALYTICS) {
            return handleSessionAnalyticsClick(
                    logicalMouseX,
                    logicalMouseY,
                    contentLeft,
                    contentRight,
                    panelY);
        }
        if (selectedModule == DashboardModule.SESSION_HISTORY) {
            return handleSessionHistoryClick(
                    logicalMouseX,
                    logicalMouseY,
                    contentLeft,
                    contentRight,
                    panelY);
        }

        int masterY = panelY + MASTER_Y;
        int switchX = contentRight - MASTER_CONTROL_RIGHT_INSET
                - MASTER_SWITCH_WIDTH;
int selectorX = switchX - MASTER_CONTROL_GAP - 180;
int selectorY = masterY + 10;

if (inside(
        logicalMouseX,
        logicalMouseY,
        selectorX,
        selectorY,
        180,
        20)) {

    trackerDropdownOpen = !trackerDropdownOpen;
    trackerDropdownHighlight = 0;
    trackerDropdownScroll = 0;
    return true;
}

if (trackerDropdownOpen) {
    List<TrackerSelection> options = filteredTrackerSelections();

    int rowY = selectorY + 24;

    for (int i = 0;
         i < Math.min(
                 Math.max(
                         options.size()
                                 - trackerDropdownScroll,
                         0),
                 6);
         i++) {
        int optionIndex =
                trackerDropdownScroll + i;

        if (inside(
                logicalMouseX,
                logicalMouseY,
                selectorX,
                rowY,
                180,
                20)) {

            RotClientClient.setTrackingSelection(
                    options.get(optionIndex));

            closeTrackerDropdown();
            trackerSearch = "";
            trackerDropdownHighlight = 0;
            trackerDropdownScroll = 0;
            return true;
        }

        rowY += 22;
    }
}
        if (inside(logicalMouseX, logicalMouseY,
                contentLeft, masterY,
                contentRight - contentLeft, MASTER_HEIGHT)) {
            RotClientClient.setTrackerEnabled(!config.enabled);
            return true;
        }

        int gridY = panelY + SETTINGS_GRID_Y;
        int availableWidth = contentRight - contentLeft;
        int columnWidth = (availableWidth - SETTING_COLUMN_GAP) / 2;
        for (int i = 0; i < settings.size(); i++) {
            int column = i % 2;
            int row = i / 2;
            int x = contentLeft + column * (columnWidth + SETTING_COLUMN_GAP);
            int y = gridY + row * SETTING_PITCH;
            if (inside(logicalMouseX, logicalMouseY,
                    x, y, columnWidth, SETTING_HEIGHT)) {
                if (!isHudSettingAvailable(i)) {
                    return true;
                }
                ToggleSetting setting = settings.get(i);
                setting.setter().accept(!setting.getter().getAsBoolean());
                RotClientClient.save();
                hud.clampToScreen();
                return true;
            }
        }

        int buttonY = panelY + actionRowY();
        int buttonWidth = (availableWidth - BUTTON_GAP * 2) / 3;
        int secondX = contentLeft + buttonWidth + BUTTON_GAP;
        int thirdX = secondX + buttonWidth + BUTTON_GAP;
        if (inside(logicalMouseX, logicalMouseY,
                contentLeft, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            RotClientClient.openHudEditor(this);
            return true;
        }
        if (inside(logicalMouseX, logicalMouseY,
                secondX, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            RotClientClient.resetSessionData();
            return true;
        }
        if (inside(logicalMouseX, logicalMouseY,
                thirdX, buttonY, contentRight - thirdX, BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
        double logicalMouseX = event.x() / uiScale;
        double logicalMouseY = event.y() / uiScale;
        int snappedMouseX = (int) Math.round(logicalMouseX);
        int snappedMouseY = (int) Math.round(logicalMouseY);
        int logicalWidth = Math.round(width / uiScale);
        int logicalHeight = Math.round(height / uiScale);

        int panelX = panelX();
        int panelY = panelY();
        int sidebarTop = panelY + CHROME_HEIGHT;
        int sidebarBottom = panelY + panelH() - HOME_BUTTON_Y_OFFSET - 10;
        if (sidebarScroll.dragThumbTo(
                snappedMouseY,
                sidebarTop,
                sidebarBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {
            return true;
        }
        if (selectedModule == DashboardModule.QOL_SETTINGS) {
            int contentTop = panelY + MASTER_Y;
            int contentBottom = panelY + actionRowY() - 10;
            if (qolDashboard.mouseDragged(
                    snappedMouseX,
                    snappedMouseY,
                    contentTop,
                    contentBottom)) {
                return true;
            }
        }

        if (resizeEdge != RotClientWindowPlacementPolicy.ResizeEdge.NONE) {
            RotClientWindowPlacementPolicy.Rect next =
                    RotClientWindowPlacementPolicy.resize(
                            windowRect(),
                            resizeEdge,
                            snappedMouseX,
                            snappedMouseY,
                            logicalWidth,
                            logicalHeight);
            persistFloatingRect(next, logicalWidth, logicalHeight);
            return true;
        }

        if (panelDragging) {
            RotClientWorkspace workspace = RotClientClient.workspace();
            if (RotClientWindowPlacementPolicy.isSnapped(workspace.config().windowPlacement)) {
                RotClientWindowPlacementPolicy.Rect restored =
                        RotClientWindowPlacementPolicy.restoreUnderCursor(
                                RotClientWindowPlacementPolicy.floatingRect(
                                        logicalWidth,
                                        logicalHeight,
                                        workspace.config().clientUiNormX,
                                        workspace.config().clientUiNormY,
                                        workspace.config().clientUiNormW,
                                        workspace.config().clientUiNormH),
                                snappedMouseX,
                                logicalWidth,
                                logicalHeight);
                persistFloatingRect(restored, logicalWidth, logicalHeight);
                panelDragOriginPanelX = restored.x();
                panelDragOriginPanelY = restored.y();
                panelDragOriginMouseX = logicalMouseX;
                panelDragOriginMouseY = logicalMouseY;
            }
            applyLivePanelDrag(
                    panelDragOriginPanelX + (logicalMouseX - panelDragOriginMouseX),
                    panelDragOriginPanelY + (logicalMouseY - panelDragOriginMouseY),
                    logicalWidth,
                    logicalHeight);
            snapPreview = RotClientWindowPlacementPolicy.snapPreview(
                    logicalWidth, logicalHeight, snappedMouseX, snappedMouseY);
            return true;
        }

        if (draggingTabIndex != null) {
            int dx = (int) Math.abs(logicalMouseX - dragStartX);
            int dy = (int) Math.abs(logicalMouseY - dragStartY);
            if (!tabDragging
                    && (dx >= RotClientTabStrip.DRAG_THRESHOLD
                    || dy >= RotClientTabStrip.DRAG_THRESHOLD)) {
                tabDragging = true;
            }
            if (tabDragging) {
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseReleased(event);
        }
        boolean scrollbarReleased = sidebarScroll.endThumbDrag();
        if (selectedModule == DashboardModule.QOL_SETTINGS) {
            scrollbarReleased = qolDashboard.mouseReleased() || scrollbarReleased;
        }
        if (scrollbarReleased) {
            return true;
        }
        RotClientWorkspace workspace = RotClientClient.workspace();
        if (resizeEdge != RotClientWindowPlacementPolicy.ResizeEdge.NONE) {
            resizeEdge = RotClientWindowPlacementPolicy.ResizeEdge.NONE;
            workspace.saveNow();
            return true;
        }
        if (panelDragging) {
            panelDragging = false;
            livePanelX = Double.NaN;
            livePanelY = Double.NaN;
            if (!RotClientWindowPlacementPolicy.FLOATING.equals(snapPreview)) {
                workspace.setWindowPlacement(snapPreview);
            }
            snapPreview = RotClientWindowPlacementPolicy.FLOATING;
            workspace.saveNow();
            return true;
        }
        if (draggingTabIndex != null) {
            float uiScale = uiScale();
            int logicalMouseX = (int) Math.round(event.x() / uiScale);
            if (tabDragging) {
                RotClientTabStrip.Layout layout = tabStripLayout(panelX(), panelY());
                int to = RotClientTabStrip.dropIndex(
                        layout,
                        workspace.tabsView().size(),
                        tabStripScroll,
                        logicalMouseX);
                workspace.reorder(draggingTabIndex, to);
                workspace.saveNow();
            }
            draggingTabIndex = null;
            tabDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        qolDashboard.persistWorkspaceView();
        RotClientClient.workspace().saveNow();
        config.setSelectedDashboardModule(selectedModule);
        RotClientClient.save();
        if (parent != null) {
            minecraft.gui.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private boolean handleSessionAnalyticsClick(
            double logicalMouseX,
            double logicalMouseY,
            int contentLeft,
            int contentRight,
            int panelY) {
        MiningSessionAnalyticsPresentation presentation =
                RotClientClient.sessionAnalyticsPresentation();
        int availableWidth = contentRight - contentLeft;
        int buttonY = panelY + actionRowY();
        int buttonWidth = (availableWidth - BUTTON_GAP * 4) / 5;
        int gap = buttonWidth + BUTTON_GAP;

        if (presentation.pauseEnabled()
                && inside(logicalMouseX, logicalMouseY,
                contentLeft, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            awaitingNewSessionConfirm = false;
            RotClientClient.pauseCurrentSession();
            return true;
        }
        if (presentation.resumeEnabled()
                && inside(logicalMouseX, logicalMouseY,
                contentLeft + gap, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            awaitingNewSessionConfirm = false;
            RotClientClient.resumeCurrentSession();
            return true;
        }
        if (presentation.startNewEnabled()
                && inside(logicalMouseX, logicalMouseY,
                contentLeft + gap * 2, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            if (!awaitingNewSessionConfirm) {
                awaitingNewSessionConfirm = true;
                return true;
            }
            awaitingNewSessionConfirm = false;
            RotClientClient.startNewCurrentSession();
            return true;
        }
        if (presentation.copyEnabled()
                && inside(logicalMouseX, logicalMouseY,
                contentLeft + gap * 3, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            awaitingNewSessionConfirm = false;
            RotClientClient.copySessionAnalyticsSummary();
            return true;
        }
        int doneX = contentLeft + gap * 4;
        if (inside(logicalMouseX, logicalMouseY,
                doneX, buttonY, contentRight - doneX, BUTTON_HEIGHT)) {
            awaitingNewSessionConfirm = false;
            onClose();
            return true;
        }
        awaitingNewSessionConfirm = false;
        return true;
    }

    private boolean handleSessionHistoryClick(
            double logicalMouseX,
            double logicalMouseY,
            int contentLeft,
            int contentRight,
            int panelY) {
        MiningSessionHistoryPresentation presentation =
                RotClientClient.sessionHistoryPresentation();
        int availableWidth = contentRight - contentLeft;
        int buttonY = panelY + actionRowY();

        if (presentation.detailOpen()) {
            int buttonWidth = (availableWidth - BUTTON_GAP * 3) / 4;
            int gap = buttonWidth + BUTTON_GAP;
            Optional<MiningSessionHistoryRecord> selected =
                    presentation.selected();
            if (inside(logicalMouseX, logicalMouseY,
                    contentLeft, buttonY, buttonWidth, BUTTON_HEIGHT)) {
                RotClientClient.clearSessionHistorySelection();
                return true;
            }
            if (selected.isPresent()
                    && inside(logicalMouseX, logicalMouseY,
                    contentLeft + gap, buttonY, buttonWidth, BUTTON_HEIGHT)) {
                RotClientClient.copySessionHistory(
                        selected.get().recordId());
                return true;
            }
            if (selected.isPresent()
                    && inside(logicalMouseX, logicalMouseY,
                    contentLeft + gap * 2, buttonY, buttonWidth,
                    BUTTON_HEIGHT)) {
                RotClientClient.deleteSessionHistory(
                        selected.get().recordId());
                return true;
            }
            int doneX = contentLeft + gap * 3;
            if (inside(logicalMouseX, logicalMouseY,
                    doneX, buttonY, contentRight - doneX, BUTTON_HEIGHT)) {
                onClose();
                return true;
            }
            return true;
        }

        int listY = panelY + MASTER_Y + 40;
        if (presentation.emptyStateMessage() != null) {
            listY += 14;
        }
        List<MiningSessionHistoryRecord> sessions = presentation.sessions();
        int visible = Math.min(HISTORY_MAX_VISIBLE_ROWS, sessions.size());
        int actionWidth = 52;
        int actionGap = 4;
        for (int i = 0; i < visible; i++) {
            MiningSessionHistoryRecord record = sessions.get(i);
            int rowY = listY + i * HISTORY_ROW_HEIGHT;
            int actionX = contentRight - (actionWidth * 3 + actionGap * 2) - 6;
            if (inside(logicalMouseX, logicalMouseY,
                    actionX, rowY + 3, actionWidth, BUTTON_HEIGHT)) {
                RotClientClient.openSessionHistory(record.recordId());
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    actionX + actionWidth + actionGap,
                    rowY + 3, actionWidth, BUTTON_HEIGHT)) {
                RotClientClient.copySessionHistory(record.recordId());
                return true;
            }
            if (inside(logicalMouseX, logicalMouseY,
                    actionX + (actionWidth + actionGap) * 2,
                    rowY + 3, actionWidth, BUTTON_HEIGHT)) {
                RotClientClient.deleteSessionHistory(record.recordId());
                return true;
            }
        }

        int buttonWidth = (availableWidth - BUTTON_GAP * 3) / 4;
        if (presentation.saveEnabled()
                && inside(logicalMouseX, logicalMouseY,
                contentLeft, buttonY, buttonWidth, BUTTON_HEIGHT)) {
            RotClientClient.saveSessionHistory();
            return true;
        }
        boolean clearClickable = presentation.clearEnabled()
                || presentation.awaitingClearConfirm();
        if (clearClickable
                && inside(logicalMouseX, logicalMouseY,
                contentLeft + buttonWidth + BUTTON_GAP, buttonY,
                buttonWidth, BUTTON_HEIGHT)) {
            if (presentation.awaitingClearConfirm()) {
                RotClientClient.confirmClearSessionHistory();
            } else {
                RotClientClient.requestClearSessionHistory();
            }
            return true;
        }
        int doneX = contentLeft + (buttonWidth + BUTTON_GAP) * 2;
        if (inside(logicalMouseX, logicalMouseY,
                doneX, buttonY, contentRight - doneX, BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        return true;
    }

    private void selectModule(DashboardModule module) {
        if (module == null) {
            module = DashboardModule.NONE;
        }
        selectedModule = module;
        if (selectedModule != DashboardModule.MINING_TRACKER) {
            highlightSettingId = "";
            highlightUntilMillis = 0L;
        }
        if (selectedModule == DashboardModule.SESSION_ANALYTICS) {
            analyticsScroll.reset();
        }
        ensureSidebarReveals(selectedModule);
        config.setSelectedDashboardModule(selectedModule);
        RotClientWorkspace workspace = RotClientClient.workspace();
        RotClientWorkspaceRoute route =
                RotClientWorkspaceRoute.fromDashboardModule(selectedModule);
        if (openInNewTabGesture) {
            workspace.addTab(route);
            syncSelectedModuleFromWorkspace();
        } else {
            workspace.navigateActive(route);
        }
        RotClientClient.save();
    }

    private void selectQolPage(QolUtilityCatalog.Group group) {
        selectModule(DashboardModule.QOL_SETTINGS);
        qolDashboard.setActivePage(group);
        ensureSidebarRevealsQol();
    }

    private void ensureSidebarRevealsQol() {
        RotClientWorkspace workspace = RotClientClient.workspace();
        if (!RotClientSidebarNav.isExpanded(
                workspace.expandedSidebarSections(),
                RotClientSidebarNav.SECTION_QOL)) {
            workspace.toggleSidebarSection(RotClientSidebarNav.SECTION_QOL);
        }
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                OVERVIEW_MODULE_Y,
                workspace.expandedSidebarSections());
        int viewportHeight = Math.max(0,
                (panelH() - HOME_BUTTON_Y_OFFSET - 10)
                        - (CHROME_HEIGHT));
        RotClientSidebarNav.HitTarget target =
                RotClientSidebarNav.hitTargetForQolGroup(qolDashboard.activePage());
        int top = RotClientSidebarNav.targetTop(layout, target);
        int bottom = top + RotClientSidebarNav.targetHeight(target);
        int next = RotClientSidebarNav.scrollToReveal(
                sidebarScroll.scrollPixels(),
                viewportHeight,
                layout.contentHeight(OVERVIEW_MODULE_Y),
                Math.max(0, top - OVERVIEW_MODULE_Y),
                Math.max(0, bottom - OVERVIEW_MODULE_Y));
        sidebarScroll.setScrollPixels(next);
    }

    private void reclampSidebarScroll() {
        RotClientSidebarNav.Layout layout = sidebarLayout(false);
        int viewportHeight = Math.max(0,
                (panelH() - HOME_BUTTON_Y_OFFSET - 10)
                        - (CHROME_HEIGHT));
        sidebarScroll.setBounds(
                layout.contentHeight(OVERVIEW_MODULE_Y),
                viewportHeight);
    }

    private void ensureSidebarReveals(DashboardModule module) {
        RotClientWorkspace workspace = RotClientClient.workspace();
        String section = RotClientSidebarNav.sectionForModule(module);
        if (section != null
                && !RotClientSidebarNav.isExpanded(
                        workspace.expandedSidebarSections(), section)) {
            workspace.toggleSidebarSection(section);
        }
        RotClientSidebarNav.Layout layout = RotClientSidebarNav.layout(
                OVERVIEW_MODULE_Y,
                workspace.expandedSidebarSections());
        int viewportHeight = Math.max(0,
                (panelH() - HOME_BUTTON_Y_OFFSET - 10)
                        - (CHROME_HEIGHT));
        int contentHeight = layout.contentHeight(OVERVIEW_MODULE_Y);
        sidebarScroll.setBounds(contentHeight, viewportHeight);
        RotClientSidebarNav.HitTarget target =
                RotClientSidebarNav.hitTargetForModule(module);
        int top = RotClientSidebarNav.targetTop(layout, target);
        if (top < 0) {
            return;
        }
        int bottom = top + RotClientSidebarNav.targetHeight(target);
        int next = RotClientSidebarNav.scrollToReveal(
                sidebarScroll.scrollPixels(),
                viewportHeight,
                contentHeight,
                top - OVERVIEW_MODULE_Y,
                bottom - OVERVIEW_MODULE_Y);
        sidebarScroll.setScrollPixels(next);
    }

    private void syncSelectedModuleFromWorkspace() {
        selectedModule = RotClientClient.workspace().activeRoute().toDashboardModule();
        if (selectedModule == DashboardModule.SESSION_ANALYTICS) {
            analyticsScroll.reset();
        }
        if (config.selectedDashboardModule() != selectedModule) {
            config.setSelectedDashboardModule(selectedModule);
            RotClientClient.save();
        }
    }

    private void openAppearanceCustomizer() {
        openAppearanceCustomizer(RotClientAppearanceScreen.Section.OVERVIEW);
    }

    private void openAppearanceCustomizer(RotClientAppearanceScreen.Section section) {
        RotClientClient.workspace().navigateActive(
                RotClientWorkspaceRoute.APPEARANCE_OVERVIEW);
        Minecraft.getInstance().gui.setScreen(
                new RotClientAppearanceScreen(this, config, hud, section));
    }

    private void activateSettingsEntry(RotClientSettingsIndex.Entry entry) {
        activateSettingsEntry(entry, false);
    }

    private void activateSettingsEntry(RotClientSettingsIndex.Entry entry, boolean newTab) {
        if (entry == null) {
            return;
        }
        settingsSearchFocused = false;
        settingsQuery = "";
        omniboxHighlight = 0;
        if (RotClientOmniboxPolicy.opensHudEditor(entry)) {
            highlightSettingId = "";
            highlightUntilMillis = 0L;
            RotClientClient.openHudEditor(this);
            return;
        }
        RotClientWorkspace workspace = RotClientClient.workspace();
        List<RotClientWorkspaceTab> tabs = workspace.tabsView();
        RotClientOmniboxPolicy.Decision decision = RotClientOmniboxPolicy.decide(
                entry,
                RotClientOmniboxPolicy.routeIds(tabs),
                workspace.config().activeIndex(),
                newTab,
                tabs.size(),
                RotClientWorkspaceConfig.MAX_TABS);
        switch (decision.action()) {
            case FOCUS_EXISTING_TAB -> {
                if (decision.existingTabIndex() >= 0
                        && decision.existingTabIndex() < tabs.size()) {
                    workspace.activate(tabs.get(decision.existingTabIndex()).id);
                }
            }
            case OPEN_NEW_TAB -> workspace.addTab(decision.route());
            case NAVIGATE_CURRENT -> workspace.navigateActive(decision.route());
        }
        syncSelectedModuleFromWorkspace();
        switch (entry.destination()) {
            case HUD_EDITOR -> {
            }
            case APPEARANCE -> {
                highlightSettingId = "";
                highlightUntilMillis = 0L;
                if ("appearance.reset".equals(entry.id())) {
                    openAppearanceCustomizer(
                            RotClientAppearanceScreen.Section.RESET);
                } else {
                    openAppearanceCustomizer();
                }
            }
            case MINING_TRACKER -> {
                if (RotClientSettingsIndex.isHudVisibilityToggle(entry.id())) {
                    highlightSettingId = entry.id();
                    highlightUntilMillis = System.currentTimeMillis() + 4_000L;
                } else {
                    highlightSettingId = "";
                    highlightUntilMillis = 0L;
                }
            }
            case QOL_SETTINGS -> qolDashboard.openFromSearchId(entry.id());
            default -> {
            }
        }
    }

    private boolean isSettingHighlighted(String settingId) {
        if (settingId == null
                || highlightSettingId == null
                || highlightSettingId.isBlank()) {
            return false;
        }
        if (!settingId.equals(highlightSettingId)) {
            return false;
        }
        if (System.currentTimeMillis() > highlightUntilMillis) {
            highlightSettingId = "";
            return false;
        }
        return true;
    }

    private void drawAppearanceTabPlaceholder(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        int contentLeft = panelX + SIDEBAR_WIDTH + CONTENT_INSET;
        int contentRight = panelX + panelW() - CONTENT_INSET;
        int cardY = panelY + MASTER_Y;
        int cardHeight = 120;
        RotClientUiDraw.drawAccentCard(
                graphics, contentLeft, cardY, contentRight - contentLeft, cardHeight, true);
        RotClientUiDraw.text(graphics, font, "Appearance",
                contentLeft + 16, cardY + 18, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, "Open the Appearance customizer for this tab.",
                contentLeft + 16, cardY + 38, RotClientTheme.TEXT_MUTED, false);
        drawButton(graphics, mouseX, mouseY,
                contentLeft + 16, cardY + 70, 168,
                "Open Appearance", RotClientTheme.HUD_ACCENT, true);
    }

    private RotClientTabStrip.Layout tabStripLayout(int panelX, int panelY) {
        return RotClientTabStrip.compute(
                panelX,
                panelY,
                panelW(),
                RotClientClient.workspace().tabsView().size());
    }

    private void cycleWorkspaceTab(int delta) {
        RotClientWorkspace workspace = RotClientClient.workspace();
        List<RotClientWorkspaceTab> tabs = workspace.tabsView();
        if (tabs.isEmpty()) {
            return;
        }
        int index = workspace.config().activeIndex();
        int next = Math.floorMod(index + delta, tabs.size());
        workspace.activate(tabs.get(next).id);
        syncSelectedModuleFromWorkspace();
    }

    private boolean controlDown(KeyEvent event) {
        return event.hasControlDown();
    }

    private boolean controlDown(MouseButtonEvent event) {
        return event.hasControlDown();
    }

    private boolean shiftDown(KeyEvent event) {
        return event.hasShiftDown();
    }

    private void openOverview() {
        if (selectedModule == DashboardModule.NONE) {
            onClose();
            return;
        }
        selectModule(DashboardModule.NONE);
    }

    private void openHome() {
        openOverview();
    }

    private void drawButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                            int x, int y, int width, String label, int accent, boolean primary) {
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x, y, width, label, primary, true);
    }
private void drawTrackerDropdown(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        int x,
        int y,
        int width,
        int height) {

    boolean hover = inside(mouseX, mouseY, x, y, width, height);

    roundedFill(
            graphics,
            x,
            y,
            x + width,
            y + height,
            hover || trackerDropdownOpen ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT);

    roundedOutline(
            graphics,
            x,
            y,
            x + width,
            y + height,
            RotClientTheme.BORDER);

    String dropdownText;

    if (trackerDropdownOpen) {
        dropdownText = trackerSearch.isEmpty()
                ? "TYPE TO SEARCH..."
                : trackerSearch + "_";
    }
    else {
        dropdownText = config.selectedSelection()
                .displayName()
                .toUpperCase(Locale.ROOT);
    }

    RotClientUiDraw.text(graphics, font,
            dropdownText,
            x + 8,
            y + 6,
            RotClientTheme.TEXT,
            false);

    RotClientUiDraw.text(graphics, font,
            trackerDropdownOpen ? "^" : "v",
            x + width - 14,
            y + 6,
            RotClientTheme.HUD_ACCENT,
            true);

    if (!trackerDropdownOpen) {
        return;
    }

    List<TrackerSelection> options = filteredTrackerSelections();

    int rowY = y + height + 4;

    for (int i = 0;
         i < Math.min(
                 Math.max(
                         options.size()
                                 - trackerDropdownScroll,
                         0),
                 6);
         i++) {
        int optionIndex =
                trackerDropdownScroll + i;
        TrackerSelection selection = options.get(optionIndex);

        boolean selected =
                selection == config.selectedSelection();
        boolean highlighted =
                optionIndex == trackerDropdownHighlight;

        roundedFill(
                graphics,
                x,
                rowY,
                x + width,
                rowY + 20,
                highlighted ? RotClientTheme.HOVER_ROW : selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT);

        RotClientUiDraw.text(graphics, font,
                selection.displayName(),
                x + 8,
                rowY + 6,
                RotClientTheme.TEXT,
                selected || highlighted);

        rowY += 22;
    }
}
    private void drawRight(GuiGraphicsExtractor graphics, String text,
                           int right, int y, int color) {
        RotClientUiDraw.text(graphics, font, text, right - font.width(text), y, color, false);
    }

    private static void drawSwitch(GuiGraphicsExtractor graphics, int x, int y,
                                   int width, int height, float progress) {
        int background = blend(RotClientTheme.TOGGLE_OFF, RotClientTheme.SUCCESS, progress);
        roundedFill(graphics, x, y, x + width, y + height, background);
        int knobSize = height - 4;
        int knobX = x + 2 + Math.round((width - knobSize - 4) * progress);
        roundedFill(graphics, knobX, y + 2,
                knobX + knobSize, y + 2 + knobSize, RotClientTheme.TOGGLE_KNOB);
    }

    private static float animate(float current, boolean enabled) {
        float target = enabled ? 1.0F : 0.0F;
        return (float) RotClientEase.expToward(
                current, target, RotClientUiClock.seconds(), RotClientEase.TOGGLE_STIFFNESS);
    }

    private int panelX() {
        return windowRect().x();
    }

    private int panelY() {
        return windowRect().y();
    }

    private float uiScale() {
        return 1.0F;
    }

    private RotClientDashboardLayout.PanelSize resolvedPanelSize() {
        RotClientWindowPlacementPolicy.Rect rect = windowRect();
        return new RotClientDashboardLayout.PanelSize(rect.width(), rect.height());
    }

    private int panelW() {
        return windowRect().width();
    }

    private int panelH() {
        return windowRect().height();
    }

    private RotClientWindowPlacementPolicy.Rect windowRect() {
        RotClientWorkspaceConfig cfg = RotClientClient.workspace().config();
        RotClientWindowPlacementPolicy.Rect floating =
                RotClientWindowPlacementPolicy.floatingRect(
                        width,
                        height,
                        cfg.clientUiNormX,
                        cfg.clientUiNormY,
                        cfg.clientUiNormW,
                        cfg.clientUiNormH);
        if (Double.isFinite(livePanelX) && Double.isFinite(livePanelY)) {
            floating = new RotClientWindowPlacementPolicy.Rect(
                    (int) Math.round(livePanelX),
                    (int) Math.round(livePanelY),
                    floating.width(),
                    floating.height());
        }
        if (panelDragging) {
            return floating;
        }
        return RotClientWindowPlacementPolicy.apply(
                cfg.windowPlacement, width, height, floating);
    }

    private void persistFloatingRect(
            RotClientWindowPlacementPolicy.Rect rect,
            int viewportW,
            int viewportH) {
        RotClientClient.workspace().setFloatingWindow(
                RotClientClientUiLayout.normXFromPanelX(viewportW, rect.width(), rect.x()),
                RotClientClientUiLayout.normYFromPanelY(viewportH, rect.height(), rect.y()),
                RotClientWindowPlacementPolicy.normW(viewportW, rect.width()),
                RotClientWindowPlacementPolicy.normH(viewportH, rect.height()));
    }

    private boolean handleDashboardWindowClick(
            MouseButtonEvent event,
            boolean doubleClick,
            double logicalMouseX,
            double logicalMouseY,
            int panelX,
            int panelY) {
        int mx = (int) Math.round(logicalMouseX);
        int my = (int) Math.round(logicalMouseY);
        RotClientWorkspace workspace = RotClientClient.workspace();
        List<RotClientWorkspaceTab> tabs = workspace.tabsView();
        RotClientTabStrip.Layout tabLayout = tabStripLayout(panelX, panelY);

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            int hitTab = RotClientTabStrip.hitTabIndex(
                    tabLayout, tabs.size(), tabStripScroll, mx, my);
            if (hitTab >= 0) {
                workspace.closeTab(tabs.get(hitTab).id);
                syncSelectedModuleFromWorkspace();
                return true;
            }
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (!RotClientWindowPlacementPolicy.isSnapped(workspace.config().windowPlacement)) {
            RotClientWindowPlacementPolicy.ResizeEdge edge =
                    RotClientWindowPlacementPolicy.hitResizeEdge(windowRect(), mx, my);
            if (edge != RotClientWindowPlacementPolicy.ResizeEdge.NONE) {
                resizeEdge = edge;
                return true;
            }
        }

        if (RotClientTabStrip.hitWindowClose(tabLayout, mx, my)) {
            onClose();
            return true;
        }
        if (RotClientTabStrip.hitMaximize(tabLayout, mx, my)) {
            if (!doubleClick) {
                workspace.toggleMaximize();
            }
            return true;
        }
        if (doubleClick && RotClientTabStrip.hitCaptionDrag(
                tabLayout, panelX, panelW(), tabs.size(), tabStripScroll, mx, my)) {
            workspace.toggleMaximize();
            return true;
        }
        if (RotClientTabStrip.hitNewTab(tabLayout, mx, my)) {
            workspace.addOverviewTab();
            syncSelectedModuleFromWorkspace();
            return true;
        }
        int hitTab = RotClientTabStrip.hitTabIndex(
                tabLayout, tabs.size(), tabStripScroll, mx, my);
        if (hitTab >= 0) {
            if (RotClientTabStrip.hitClose(tabLayout, hitTab, tabStripScroll, mx, my)) {
                workspace.closeTab(tabs.get(hitTab).id);
                syncSelectedModuleFromWorkspace();
                return true;
            }
            workspace.activate(tabs.get(hitTab).id);
            syncSelectedModuleFromWorkspace();
            draggingTabIndex = hitTab;
            dragStartX = mx;
            dragStartY = my;
            tabDragging = false;
            return true;
        }
        if (RotClientTabStrip.hitCaptionDrag(
                tabLayout, panelX, panelW(), tabs.size(), tabStripScroll, mx, my)) {
            beginWindowDrag(logicalMouseX, logicalMouseY, panelX, panelY);
            return true;
        }

        int[] box = omniboxBounds(panelX, panelY);
        if (inside(logicalMouseX, logicalMouseY, box[0], box[1], box[2], box[3])) {
            settingsSearchFocused = true;
            return true;
        }
        if (settingsSearchFocused && !settingsQuery.isBlank()) {
            List<RotClientSettingsIndex.Entry> results =
                    RotClientOmniboxPolicy.suggest(settingsQuery);
            int rowY = box[1] + box[3] + 4;
            for (int i = 0; i < results.size(); i++) {
                if (inside(logicalMouseX, logicalMouseY, box[0], rowY, box[2], 16)) {
                    activateSettingsEntry(results.get(i), openInNewTabGesture);
                    return true;
                }
                rowY += 18;
            }
        }
        if (settingsSearchFocused
                && !inside(logicalMouseX, logicalMouseY, box[0], box[1], box[2], 220)) {
            settingsSearchFocused = false;
        }
        return false;
    }

    private void beginWindowDrag(double mouseX, double mouseY, int panelX, int panelY) {
        panelDragging = true;
        snapPreview = RotClientWindowPlacementPolicy.FLOATING;
        panelDragOriginMouseX = mouseX;
        panelDragOriginMouseY = mouseY;
        panelDragOriginPanelX = panelX;
        panelDragOriginPanelY = panelY;
        livePanelX = panelX;
        livePanelY = panelY;
    }

    private void applyLivePanelDrag(
            double rawX,
            double rawY,
            int viewportW,
            int viewportH) {
        int roundedX = (int) Math.round(rawX);
        int roundedY = (int) Math.round(rawY);
        int clampedX = RotClientClientUiLayout.clampPanelX(viewportW, panelW(), roundedX);
        int clampedY = RotClientClientUiLayout.clampPanelY(viewportH, panelH(), roundedY);
        livePanelX = clampedX == roundedX ? rawX : clampedX;
        livePanelY = clampedY == roundedY ? rawY : clampedY;
        persistFloatingRect(
                new RotClientWindowPlacementPolicy.Rect(
                        clampedX, clampedY, panelW(), panelH()),
                viewportW,
                viewportH);
    }

    private double displayedPanelX() {
        return Double.isFinite(livePanelX) ? livePanelX : panelX();
    }

    private double displayedPanelY() {
        return Double.isFinite(livePanelY) ? livePanelY : panelY();
    }

    private int[] omniboxBounds(int panelX, int panelY) {
        return new int[] {
                panelX + 216,
                RotClientDashboardLayout.omniboxY(panelY) + 4,
                Math.max(120, panelW() - 228),
                RotClientDashboardLayout.OMNIBOX_HEIGHT - 8
        };
    }

    private void drawOmnibox(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        int[] box = omniboxBounds(panelX, panelY);
        int brandX = panelX + 12;
        int brandY = box[1] + 5;
        RotClientUiDraw.text(graphics, font, "ROT", brandX, brandY, RotClientTheme.HUD_ACCENT, false);
        RotClientUiDraw.text(graphics, font, " CLIENT", brandX + font.width("ROT"), brandY,
                RotClientTheme.TEXT, false);
        RotClientUiDraw.text(graphics, font, "by OgRudolf", brandX, brandY + 10,
                RotClientTheme.TEXT_MUTED, false);
        drawSettingsSearchField(graphics, mouseX, mouseY, box[0], box[1], box[2]);
    }

    private void drawOmniboxSuggestions(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        if (!settingsSearchFocused || settingsQuery.isBlank()) {
            return;
        }
        List<RotClientSettingsIndex.Entry> results =
                RotClientOmniboxPolicy.suggest(settingsQuery);
        if (results.isEmpty()) {
            return;
        }
        if (omniboxHighlight >= results.size()) {
            omniboxHighlight = results.size() - 1;
        }
        int[] box = omniboxBounds(panelX, panelY);
        int height = 8 + results.size() * 18;
        RotClientUiDraw.drawElevatedCard(
                graphics, box[0], box[1] + box[3] + 2, box[2], height);
        int rowY = box[1] + box[3] + 6;
        for (int i = 0; i < results.size(); i++) {
            RotClientSettingsIndex.Entry entry = results.get(i);
            boolean hover = i == omniboxHighlight
                    || inside(mouseX, mouseY, box[0] + 4, rowY - 2, box[2] - 8, 16);
            if (hover) {
                roundedFill(graphics,
                        box[0] + 4, rowY - 2,
                        box[0] + box[2] - 4, rowY + 14,
                        RotClientTheme.HOVER_ROW);
            }
            RotClientUiDraw.text(graphics, font, entry.category(),
                    box[0] + 10, rowY, RotClientTheme.TEXT_DIM, false);
            RotClientUiDraw.text(graphics, font, entry.label(),
                    box[0] + 88, rowY, RotClientTheme.TEXT, false);
            rowY += 18;
        }
    }

    private void drawSnapPreview(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY) {
        if (!panelDragging
                || RotClientWindowPlacementPolicy.FLOATING.equals(snapPreview)) {
            return;
        }
        RotClientWindowPlacementPolicy.Rect preview =
                RotClientWindowPlacementPolicy.apply(
                        snapPreview, width, height, windowRect());
        graphics.fill(
                preview.x(),
                preview.y(),
                preview.right(),
                preview.bottom(),
                RotClientUiDraw.withAlpha(RotClientTheme.HUD_ACCENT, 0x44));
    }

    private int actionRowY() {
        return panelH()
                - HOME_BUTTON_Y_OFFSET
                - RotClientDashboardLayout.ACTION_ROW_HEIGHT
                - RotClientDashboardLayout.ACTION_ROW_GAP;
    }

    private static ToggleSetting setting(
            String id,
            Supplier<String> label,
            BooleanSupplier getter,
            Consumer<Boolean> setter) {
        return new ToggleSetting(id, label, getter, setter);
    }

    private String selectedTrackerLabel() {
        return config.selectedSelection()
                .displayName()
                .toUpperCase();
    }

    private String rawMetricLabel() {
        if (config.selectedSelection().isGemstone()) {
            return "TIER LEDGER";
        }

        return rawMetricLabel(
                config.selectedSelection().materialTarget());
    }

    private String enchantedMetricLabel() {
        if (config.selectedSelection().isGemstone()) {
            return "ROUGH EQUIVALENT";
        }

        return enchantedMetricLabel(
                config.selectedSelection().materialTarget());
    }

    private String resourcePerHourLabel() {
        if (config.selectedSelection().isGemstone()) {
            return "ROUGH EQ / HOUR";
        }

        TrackingTarget target =
                config.selectedSelection().materialTarget();

        return target.isCombined()
                ? "RESOURCES / HOUR"
                : target.primaryMaterial()
                        .displayName()
                        .toUpperCase() + " / HOUR";
    }

    private boolean isHudSettingAvailable(int index) {
        if (!config.selectedSelection().isGemstone()) {
            return true;
        }
        return index == 0
                || index == 7
                || index == 8
                || index == 9
                || index == 17
                || index == 18
                || index == 19
                || index == 20;
    }

    private String hudSettingLabel(
            int index,
            ToggleSetting setting) {
        if (!config.selectedSelection().isGemstone()) {
            return setting.label().get();
        }
        return switch (index) {
            case 3 -> "SESSION VALUE";
            case 4 -> "UNSOLD VALUE";
            case 5 -> "COINS / HOUR";
            case 10 -> "AVERAGE / BLOCK";
            case 11 -> "BAZAAR PRICING";
            default -> setting.label().get();
        };
    }

    private static String gemstoneHudSettingStatus(int index) {
        return switch (index) {
            case 1, 2, 6, 10 -> "FIXED";
            default -> "LATER";
        };
    }
    private static String rawMetricLabel(TrackingTarget target) {
        return target.isCombined()
                ? "RAW MITHRIL + TITANIUM"
                : target.primaryMaterial().rawMetricLabel().toUpperCase();
    }

    private static String enchantedMetricLabel(TrackingTarget target) {
        return target.isCombined()
                ? "ENCHANTED MATERIALS"
                : target.primaryMaterial().enchantedItemName().toUpperCase();
    }

private List<TrackerSelection> filteredTrackerSelections() {
    String query = trackerSearch
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("_", " ");

    if (query.isEmpty()) {
        return TRACKER_SELECTIONS;
    }

    return TRACKER_SELECTIONS.stream()
            .filter(selection ->
                    selection.displayName()
                            .toLowerCase(Locale.ROOT)
                            .contains(query)
                    || selection.id()
                            .toLowerCase(Locale.ROOT)
                            .replace("_", " ")
                            .contains(query))
            .toList();
}
    private static boolean inside(double mouseX, double mouseY,
                                  int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private static int blend(int from, int to, float progress) {
        int a = channel(from, 24)
                + Math.round((channel(to, 24) - channel(from, 24)) * progress);
        int r = channel(from, 16)
                + Math.round((channel(to, 16) - channel(from, 16)) * progress);
        int g = channel(from, 8)
                + Math.round((channel(to, 8) - channel(from, 8)) * progress);
        int b = channel(from, 0)
                + Math.round((channel(to, 0) - channel(from, 0)) * progress);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int channel(int color, int shift) {
        return color >> shift & 0xFF;
    }

    private static void roundedFill(GuiGraphicsExtractor graphics,
                                    int left, int top, int right, int bottom, int color) {
        RotClientUiDraw.roundedFill(graphics, left, top, right, bottom, color);
    }

    private static void roundedOutline(GuiGraphicsExtractor graphics,
                                       int left, int top, int right, int bottom, int color) {
        RotClientUiDraw.roundedOutline(graphics, left, top, right, bottom, color);
    }

    private record ToggleSetting(
            String id,
            Supplier<String> label,
            BooleanSupplier getter,
            Consumer<Boolean> setter
    ) {
    }

}
