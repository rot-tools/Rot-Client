package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * QoL & Settings utility-suite dashboard: page-filtered module cards + drawer.
 * Pages map to {@link QolUtilityCatalog.Group} categories such as Combat,
 * Fishing, Dungeons, and Utilities.
 */
final class QolUtilityDashboard {
    private final TrackerConfig config;
    private final Screen host;
    private final RotClientScrollState listScroll = new RotClientScrollState();
    private final RotClientScrollState drawerScroll = new RotClientScrollState();

    private QolUtilityCatalog.Group activePage = QolUtilityCatalog.Group.COMBAT;
    private QolUtilityUiMath.PageFilter pageFilter = QolUtilityUiMath.PageFilter.ALL;
    private String openModuleId = "";
    private String pendingColorSettingId = "";
    private String openEnumSettingId = "";
    private String enumQuery = "";
    private boolean enumSearchFocused;
    private String listeningKeybindSettingId = "";
    private String listeningTextSettingId = "";
    private String draggingNumberSettingId = "";
    private int draggingSliderRowX;
    private int draggingSliderRowWidth;
    private final java.util.LinkedHashSet<String> collapsedDrawerSections =
            new java.util.LinkedHashSet<>();
    private final RotClientExpandState drawerExpand = new RotClientExpandState();
    private final java.util.HashMap<String, Double> moduleCardHoverAmounts =
            new java.util.HashMap<>();

    /*
     * Slider rendering deliberately has its own visual state.
     *
     * The underlying setting changes immediately so gameplay/config behavior
     * remains precise. Only the rendered position eases toward that value.
     * This removes the stepped/snappy appearance caused by number specs while
     * retaining their exact min/max/step semantics.
     */
    private final java.util.HashMap<String, Double> sliderVisualFractions =
            new java.util.HashMap<>();

    private final java.util.HashMap<String, Double> sliderInteractionAmounts =
            new java.util.HashMap<>();

    private static final double MODULE_ACCORDION_SECONDS = 0.22D;
    private double moduleAccordionProgress;
    private long moduleAccordionLastNanos;
    private boolean moduleAccordionClosing;
    private boolean moduleInlineBodyRendering;
    private boolean pendingModuleResetConfirm;
    private boolean pendingHudStyleResetConfirm;
    private enum DrawerKind {
        MODULE,
        HUD
    }
    private DrawerKind drawerKind = DrawerKind.MODULE;
    private int listContentHeight;
    private int drawerContentHeight;
    private String headerHoverTip = "";
    private boolean appearanceLanding;
    private boolean hudLayoutLanding;

    QolUtilityDashboard(TrackerConfig config, Screen host) {
        this.config = config;
        this.host = host;
    }

    QolUtilityCatalog.Group activePage() {
        return activePage;
    }

    void setActivePage(QolUtilityCatalog.Group page) {
        if (page == null) {
            return;
        }
        boolean changed = activePage != page;
        boolean closedLanding = appearanceLanding || hudLayoutLanding;
        appearanceLanding = false;
        hudLayoutLanding = false;
        if (changed) {
            activePage = page;
            closeDrawer();
            listScroll.reset();
        }
        if (changed || closedLanding) {
            persistWorkspaceView();
        }
    }

    void restoreFromWorkspace(RotClientWorkspaceTab tab) {
        if (tab == null) {
            return;
        }
        QolWorkspaceView view = QolWorkspaceView.fromTab(tab);
        if (!view.groupId().isBlank()) {
            activePage = QolUtilityCatalog.Group.fromId(view.groupId());
        }
        if (view.appearanceLanding()) {
            openAppearanceLanding();
        } else if (view.hudLayoutLanding()) {
            openHudLayoutLanding();
        }
        if (view.hasDrawerModule()) {
            if (view.hudDrawer()) {
                openHudSettings(view.moduleId());
            } else {
                openModule(view.moduleId());
            }
        }
    }

    void persistWorkspaceView() {
        RotClientWorkspace workspace = RotClientClient.workspace();
        if (workspace == null) {
            return;
        }
        workspace.setQolView(QolWorkspaceView.capture(
                activePage.name(),
                appearanceLanding,
                hudLayoutLanding,
                isDrawerOpen(),
                drawerKind == DrawerKind.HUD,
                openModuleId));
    }

    boolean isDrawerOpen() {
        return openModuleId != null && !openModuleId.isBlank();
    }

    void closeDrawer() {
        openModuleId = "";
        drawerKind = DrawerKind.MODULE;
        pendingModuleResetConfirm = false;
        pendingHudStyleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        draggingNumberSettingId = "";
        drawerScroll.reset();
        resetModuleAccordionAnimation();
        persistWorkspaceView();
    }

    void openModule(String moduleId) {
        if (MiningTrackerCatalogPolicy.APPEARANCE.equals(moduleId)) {
            openAppearanceLanding();
            return;
        }
        if (MiningTrackerCatalogPolicy.HUD_LAYOUT.equals(moduleId)) {
            openHudLayoutLanding();
            return;
        }
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(moduleId);
        if (module == null) {
            return;
        }
        appearanceLanding = false;
        hudLayoutLanding = false;
        if (activePage != module.group()) {
            activePage = module.group();
            listScroll.reset();
        }
        openModuleId = module.id();
        drawerKind = DrawerKind.MODULE;
        pendingModuleResetConfirm = false;
        pendingHudStyleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        collapsedDrawerSections.clear();
        drawerScroll.reset();
        moduleAccordionProgress = 0.0D;
        moduleAccordionLastNanos = System.nanoTime();
        moduleAccordionClosing = false;
        persistWorkspaceView();
    }

    void openHudSettings(String moduleId) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(moduleId);
        if (module == null) {
            return;
        }
        if (HudElementCatalog.hudPieces(module).isEmpty()) {
            openModule(module.id());
            return;
        }
        boolean keepHudLayout = hudLayoutLanding;
        appearanceLanding = false;
        hudLayoutLanding = keepHudLayout;
        if (!keepHudLayout && activePage != module.group()) {
            activePage = module.group();
            listScroll.reset();
        }
        openModuleId = module.id();
        drawerKind = DrawerKind.HUD;
        pendingModuleResetConfirm = false;
        pendingHudStyleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        collapsedDrawerSections.clear();
        drawerScroll.reset();
        resetModuleAccordionAnimation();
        persistWorkspaceView();
    }

    void openAppearanceLanding() {
        appearanceLanding = true;
        hudLayoutLanding = false;
        activePage = QolUtilityCatalog.Group.HUD_DISPLAY;
        closeDrawer();
        listScroll.reset();
        persistWorkspaceView();
    }

    void openHudLayoutLanding() {
        hudLayoutLanding = true;
        appearanceLanding = false;
        activePage = QolUtilityCatalog.Group.HUD_DISPLAY;
        closeDrawer();
        listScroll.reset();
        persistWorkspaceView();
    }

    void closeLandings() {
        appearanceLanding = false;
        hudLayoutLanding = false;
        persistWorkspaceView();
    }

    boolean appearanceLanding() {
        return appearanceLanding;
    }

    boolean hudLayoutLanding() {
        return hudLayoutLanding;
    }

    private int pageContentListTop(int contentTop) {
        return hudLayoutLanding || appearanceLanding
                ? contentTop
                : QolUtilityUiMath.pageListTop(contentTop);
    }

    private int pageScrollTrackTop(int contentTop) {
        if (hudLayoutLanding) {
            return contentTop + HudLayoutLandingPolicy.headerHeight();
        }
        return pageContentListTop(contentTop);
    }

    String openModuleId() {
        return openModuleId == null ? "" : openModuleId;
    }

    boolean focusingModule(String moduleId) {
        return moduleId != null
                && moduleId.equals(openModuleId)
                && drawerKind == DrawerKind.MODULE;
    }

    boolean focusingHud(String moduleId) {
        return moduleId != null
                && moduleId.equals(openModuleId)
                && drawerKind == DrawerKind.HUD;
    }

    boolean hudDrawerOpen() {
        return isDrawerOpen() && drawerKind == DrawerKind.HUD;
    }

    private String hudDrawerTitle(QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return "";
        }
        if (!hudDrawerOpen()) {
            return module.name();
        }
        String name = module.name() == null ? "" : module.name();
        if (name.toUpperCase(Locale.ROOT).endsWith(" HUD")) {
            return name;
        }
        return name + " HUD";
    }

    private java.util.List<QolUtilityCatalog.SettingDef> currentDrawerSettings(
            QolUtilityCatalog.ModuleDef module) {
        if (hudDrawerOpen()) {
            java.util.ArrayList<QolUtilityCatalog.SettingDef> rows = new java.util.ArrayList<>();
            if (!HudDrawerPolicy.uniqueStyleFocus(module).isBlank()) {
                rows.addAll(HudDrawerPolicy.styleSettings());
            }
            rows.addAll(HudDrawerPolicy.allHudCatalogSettings(module));
            return rows;
        }
        return HudDrawerPolicy.moduleCatalogSettings(module);
    }

    private boolean hudDrawerShowsEnableRow(QolUtilityCatalog.ModuleDef module) {
        return hudDrawerOpen() && HudDrawerPolicy.hudDrawerShowsModuleEnableRow(module);
    }

    private String hudStyleFocus(QolUtilityCatalog.ModuleDef module) {
        return HudDrawerPolicy.uniqueStyleFocus(module);
    }

    private HudStyleState hudStyle(QolUtilityCatalog.ModuleDef module) {
        String focus = hudStyleFocus(module);
        if (focus.isBlank()) {
            return new HudStyleState();
        }
        return qol().extras().resolvedHudStyle(focus);
    }

    private void putHudStyle(QolUtilityCatalog.ModuleDef module, HudStyleState style) {
        String focus = hudStyleFocus(module);
        if (focus.isBlank() || style == null) {
            return;
        }
        qol().extras().putHudStyle(focus, style);
        TrackerStore.save(config);
    }

    private void resetHudStyle(QolUtilityCatalog.ModuleDef module) {
        String focus = hudStyleFocus(module);
        if (focus.isBlank()) {
            return;
        }
        qol().extras().resetHudStyle(focus);
        TrackerStore.save(config);
    }

    private boolean isHudStyleSetting(String settingId) {
        return hudDrawerOpen() && HudDrawerPolicy.isStyleSetting(settingId);
    }

    private Boolean readDrawerBoolean(String settingId) {
        Boolean lighting = RotClientClient.readLightingSetting(settingId);
        if (lighting != null) {
            return lighting;
        }
        if (isHudStyleSetting(settingId)) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
            HudStyleState style = hudStyle(module);
            if (HudDrawerPolicy.STYLE_TITLE.equals(settingId)) {
                return HudStylePolicy.titleVisible(style);
            }
            if (HudDrawerPolicy.STYLE_BACKGROUND.equals(settingId)) {
                return style.showBackground;
            }
            return qol().readBoolean(settingId);
        }
        Boolean tracker = MiningTrackerConfigBridge.readBoolean(config, settingId);
        if (tracker != null) {
            return tracker;
        }
        return qol().readBoolean(settingId);
    }

    private Integer readDrawerColor(String settingId) {
        if (!isHudStyleSetting(settingId)) {
            return qol().readColor(settingId);
        }
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
        HudStyleState style = hudStyle(module);
        if (HudDrawerPolicy.STYLE_TEXT.equals(settingId)) {
            return style.textColor;
        }
        if (HudDrawerPolicy.STYLE_PANEL.equals(settingId)) {
            return style.backgroundColor;
        }
        return qol().readColor(settingId);
    }

    private Double readDrawerNumber(String settingId) {
        if (!isHudStyleSetting(settingId)) {
            return qol().readNumber(settingId);
        }
        if (HudDrawerPolicy.STYLE_SCALE.equals(settingId)) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
            return (double) hudStyle(module).scale;
        }
        return qol().readNumber(settingId);
    }

    private void writeDrawerBoolean(String settingId, boolean value) {
        if (RotClientClient.writeLightingSetting(settingId, value)) {
            return;
        }
        if (isHudStyleSetting(settingId)) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
            HudStyleState style = hudStyle(module);
            if (HudDrawerPolicy.STYLE_TITLE.equals(settingId)) {
                style.showTitle = value;
            } else if (HudDrawerPolicy.STYLE_BACKGROUND.equals(settingId)) {
                style.showBackground = value;
            }
            putHudStyle(module, style);
            return;
        }
        if (MiningTrackerConfigBridge.writeBoolean(config, settingId, value)) {
            TrackerStore.save(config);
            return;
        }
        qol().writeBoolean(settingId, value);
        TrackerStore.save(config);
    }

    private void writeDrawerColor(String settingId, int color) {
        if (!isHudStyleSetting(settingId)) {
            qol().writeColor(settingId, color);
            TrackerStore.save(config);
            return;
        }
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
        HudStyleState style = hudStyle(module);
        if (HudDrawerPolicy.STYLE_TEXT.equals(settingId)) {
            style.textColor = color;
        } else if (HudDrawerPolicy.STYLE_PANEL.equals(settingId)) {
            style.backgroundColor = color;
        }
        putHudStyle(module, style);
    }

    private void writeDrawerNumber(String settingId, double value) {
        if (!isHudStyleSetting(settingId)) {
            qol().writeNumber(settingId, value);
            return;
        }
        if (HudDrawerPolicy.STYLE_SCALE.equals(settingId)) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
            HudStyleState style = hudStyle(module);
            style.scale = HudStylePolicy.clampScale((float) value);
            putHudStyle(module, style);
        }
    }

    void openFromSearchId(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return;
        }
        if ("qol.market_watch".equals(entryId)
                || "qol.market_watch.open_dashboard".equals(entryId)) {
            if (host instanceof MiningUiScreen screen) {
                screen.openMarketWatchPage();
            }
            return;
        }
        if ("nav.mining_tracker".equals(entryId)
                || MiningTrackerCatalogPolicy.TRACKER.equals(entryId)) {
            openModule(MiningTrackerCatalogPolicy.TRACKER);
            return;
        }
        if (MiningTrackerCatalogPolicy.APPEARANCE.equals(entryId)
                || "appearance.open".equals(entryId)) {
            openAppearanceLanding();
            return;
        }
        if (AppearanceLandingPolicy.isAppearanceAction(entryId)) {
            openAppearanceLanding();
            handleAction(entryId);
            return;
        }
        String legacyHud = MiningTrackerCatalogPolicy.catalogIdForLegacyHudToggle(entryId);
        if (!legacyHud.isBlank()) {
            openHudSettings(MiningTrackerCatalogPolicy.TRACKER);
            return;
        }
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(entryId);
        if (module == null) {
            return;
        }
        if (MiningTrackerCatalogPolicy.isHudContentSetting(entryId)
                || (entryId.endsWith("_hud_editor") && !entryId.equals(module.id()))) {
            openHudSettings(module.id());
            return;
        }
        openModule(module.id());
    }

    private QolUtilityConfig qol() {
        if (config.qolUtilities == null) {
            config.qolUtilities = new QolUtilityConfig();
        }
        return config.qolUtilities;
    }

    private boolean isEnabled(QolUtilityCatalog.ModuleDef module) {
        return switch (module.id()) {
            case "qol.fullbright" -> config.fullbrightEnabled || config.alwaysNightEnabled;
            case "qol.auto_sprint" -> config.autoSprintEnabled;
            case "qol.camera" -> config.cameraEnabled;
            case "qol.mining_tracker" -> config.enabled;
            case "qol.powder_chest" -> config.powderChestTrackerEnabled;
            case "qol.market_watch" -> MarketWatchRuntime.enabled();
            case "qol.mining_session", "qol.mining_history", "qol.appearance" -> true;
            default -> qol().isModuleEnabled(module.id());
        };
    }

    private void setEnabled(QolUtilityCatalog.ModuleDef module, boolean enabled) {
        switch (module.id()) {
            case "qol.fullbright" -> {
                boolean was = isEnabled(module);
                RotClientClient.setLightingCardEnabled(enabled);
                if (was != enabled) {
                    RotClientClient.notifyQolModuleToggled(module.name(), enabled);
                }
            }
            case "qol.auto_sprint" -> RotClientClient.setAutoSprintEnabled(enabled);
            case "qol.camera" -> RotClientClient.setCameraEnabled(enabled);
            case "qol.mining_tracker" -> {
                boolean was = config.enabled;
                config.enabled = enabled;
                TrackerStore.save(config);
                if (was != enabled) {
                    RotClientClient.notifyQolModuleToggled(module.name(), enabled);
                }
            }
            case "qol.powder_chest" -> {
                boolean was = config.powderChestTrackerEnabled;
                config.powderChestTrackerEnabled = enabled;
                TrackerStore.save(config);
                if (was != enabled) {
                    RotClientClient.notifyQolModuleToggled(module.name(), enabled);
                }
            }
            case "qol.market_watch" -> {
                boolean was =
                        MarketWatchRuntime.enabled();

                if (MarketWatchRuntime.setEnabled(enabled)
                        && was != enabled) {
                    RotClientClient.notifyQolModuleToggled(
                            module.name(),
                            enabled);
                }
            }
            default -> {
                boolean was = qol().isModuleEnabled(module.id());
                qol().setModuleEnabled(module.id(), enabled);
                TrackerStore.save(config);
                if (was != enabled) {
                    RotClientClient.notifyQolModuleToggled(module.name(), enabled);
                }
            }
        }
    }

    private static boolean runtimeAvailable(QolUtilityCatalog.ModuleDef module) {
        return module != null && !module.wip() && module.runtimeReady();
    }

    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int contentLeft,
            int contentTop,
            int contentRight,
            int contentBottom,
            int mouseX,
            int mouseY) {
        advanceModuleAccordionAnimation();

        int contentWidth = contentRight - contentLeft;
        boolean drawerOpen = isDrawerOpen();
        boolean hudSideDrawer =
                drawerOpen && drawerKind == DrawerKind.HUD;
        boolean moduleAccordion =
                drawerOpen && drawerKind == DrawerKind.MODULE;
        boolean modal = hudSideDrawer;
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);
        boolean overlay = hudSideDrawer;
        int listW = contentWidth;
        int listRight = contentLeft + listW;
        int listMouseX = modal ? Integer.MIN_VALUE : mouseX;
        int listMouseY = modal ? Integer.MIN_VALUE : mouseY;

        if (!hudLayoutLanding && !appearanceLanding) {
            roundedHeader(
                    graphics,
                    font,
                    contentLeft,
                    contentTop,
                    contentWidth,
                    listMouseX,
                    listMouseY);
        }

        int listTop = pageContentListTop(contentTop);
        int listBottom = contentBottom;
        if (hudLayoutLanding) {
            drawHudLayoutLanding(
                    graphics,
                    font,
                    contentLeft,
                    listTop,
                    contentRight,
                    listBottom,
                    listMouseX,
                    listMouseY);
            if (hudSideDrawer) {
                RotClientUiDraw.drawScrim(
                        graphics,
                        contentLeft,
                        contentTop,
                        contentWidth,
                        Math.max(1, contentBottom - contentTop));
                int drawerX = contentRight - drawerW;
                drawDrawer(
                        graphics,
                        font,
                        drawerX,
                        contentTop,
                        drawerW,
                        contentBottom - contentTop,
                        mouseX,
                        mouseY,
                        true);
            }
            if (headerHoverTip != null && !headerHoverTip.isBlank()) {
                RotClientUiDraw.noteHoverTip(headerHoverTip);
            }
            return;
        }
        if (appearanceLanding) {
            drawAppearanceLanding(
                    graphics,
                    font,
                    contentLeft,
                    listTop,
                    contentRight,
                    listBottom,
                    listMouseX,
                    listMouseY);
            if (headerHoverTip != null && !headerHoverTip.isBlank()) {
                RotClientUiDraw.noteHoverTip(headerHoverTip);
            }
            return;
        }
        List<QolUtilityCatalog.ModuleDef> pageModules =
                QolUtilityCatalog.modulesOnGroupPage(activePage);
        List<QolUtilityCatalog.ModuleDef> modules = QolUtilityUiMath.filterPageModules(
                pageModules,
                pageFilter,
                this::isEnabled);
        int gridWidth = Math.max(
                1,
                listW - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4);
        QolUtilityCatalog.ModuleDef accordionModule =
                moduleAccordion
                        ? moduleAccordionModule()
                        : null;

        if (accordionModule != null) {
            tickDrawerExpand(
                    accordionModule);
        }

        int accordionVisibleHeight =
                accordionModule == null
                        ? 0
                        : moduleAccordionVisibleHeight(
                                accordionModule);

        String expandedModuleId =
                accordionModule == null
                        ? ""
                        : accordionModule.id();

        QolUtilityUiMath.PageLayout layout =
                QolUtilityUiMath.layoutPage(
                        modules,
                        contentLeft,
                        gridWidth,
                        expandedModuleId,
                        accordionVisibleHeight);

        int accordionRowY =
                moduleAccordionRowY(
                        layout,
                        accordionModule);

        if (moduleAccordion
                && accordionModule != null
                && accordionRowY < 0) {

            completeModuleAccordionClose();

            accordionModule =
                    null;

            accordionVisibleHeight =
                    0;

            layout =
                    QolUtilityUiMath.layoutPage(
                            modules,
                            contentLeft,
                            gridWidth,
                            "",
                            0);
        }

        listContentHeight =
                layout.height();
        listScroll.setBounds(
                listContentHeight,
                Math.max(0, listBottom - listTop));
        listScroll.advanceSeconds(RotClientUiClock.seconds());
        int baseY = listTop - listScroll.scrollPixels();
        String hoverTip = null;

        graphics.enableScissor(contentLeft, listTop, listRight, listBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, listScroll);
        try {
            for (QolUtilityUiMath.PlacedHeader header : layout.headers()) {
                int y =
                        baseY
                                + header.y();
                if (y + QolUtilityUiMath.GROUP_HEADER_HEIGHT < listTop
                        || y > listBottom) {
                    continue;
                }
                String sectionTitle =
                        header.title();

                RotClientUiDraw.sectionLabel(
                        graphics,
                        font,
                        sectionTitle,
                        header.x() + 2,
                        y + 6);

                int dividerX =
                        header.x()
                                + font.width(
                                sectionTitle)
                                + 18;

                if (dividerX
                        < header.x()
                        + header.width()
                        - 8) {

                    graphics.fill(
                            dividerX,
                            y + 11,
                            header.x()
                                    + header.width()
                                    - 8,
                            y + 12,
                            RotClientTheme.DIVIDER);
                }
            }
            for (QolUtilityUiMath.PlacedCard card
                    : layout.cards()) {

                int y =
                        baseY
                                + card.y();

                boolean expandedCard =
                        accordionModule != null
                                && card.module() != null
                                && accordionModule.id()
                                .equals(
                                        card.module().id());

                int cardExtra =
                        expandedCard
                                ? accordionVisibleHeight
                                : 0;

                int cardHeight =
                        QolUtilityUiMath.CARD_HEIGHT
                                + cardExtra;

                if (y + cardHeight >= listTop
                        && y <= listBottom) {

                    String tip =
                            drawModuleCard(
                                    graphics,
                                    font,
                                    card.x(),
                                    y,
                                    card.width(),
                                    card.module(),
                                    listMouseX,
                                    listMouseY);

                    if (tip != null) {
                        hoverTip =
                                tip;
                    }

                    if (expandedCard
                            && cardExtra > 0) {

                        int bodyY =
                                y
                                        + QolUtilityUiMath.CARD_HEIGHT;

                        graphics.fill(
                                card.x() + 12,
                                bodyY,
                                card.x()
                                        + card.width()
                                        - 12,
                                bodyY + 1,
                                RotClientTheme.DIVIDER);

                        int inlineMouseX =
                                moduleAccordionClosing
                                        ? Integer.MIN_VALUE
                                        : listMouseX;

                        int inlineMouseY =
                                moduleAccordionClosing
                                        ? Integer.MIN_VALUE
                                        : listMouseY;

                        drawDrawer(
                                graphics,
                                font,
                                card.x(),
                                bodyY,
                                card.width(),
                                cardExtra,
                                inlineMouseX,
                                inlineMouseY,
                                false);

                        /*
                         * drawDrawer disables its own scissor.
                         * Restore the main module-list clip afterwards.
                         */
                        graphics.enableScissor(
                                contentLeft,
                                listTop,
                                listRight,
                                listBottom);
                    }
                }
            }
            if (layout.cards().isEmpty()) {
                String empty = switch (pageFilter) {
                    case ENABLED -> "Nothing is on in this page. Turn a switch on, or pick All.";
                    case CHEAT -> "No cheat-tagged modules on this page.";
                    case ALL -> "No modules on this page.";
                };
                RotClientUiDraw.text(graphics, font,
                        empty,
                        contentLeft + 4,
                        listTop + 12,
                        RotClientTheme.TEXT_MUTED,
                        false);
            }
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }
        if (listScroll.canScroll()) {
            int scrollbarX = listRight - RotClientUiDraw.SCROLLBAR_WIDTH - 2;
            boolean hovered = scrollbarHovered(
                    listMouseX,
                    listMouseY,
                    listRight - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                    listTop,
                    listBottom);
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    scrollbarX,
                    listTop,
                    listBottom,
                    listScroll.contentHeight(),
                    listScroll.scrollPixels(),
                    hovered,
                    listScroll.isThumbDragging());
        }
        if (modal) {
            RotClientUiDraw.drawScrim(
                    graphics,
                    contentLeft,
                    contentTop,
                    contentWidth,
                    Math.max(1, contentBottom - contentTop));
        }

        if (hudSideDrawer) {
            int drawerX = contentRight - drawerW;
            int drawerY = contentTop;
            drawDrawer(
                    graphics,
                    font,
                    drawerX,
                    drawerY,
                    drawerW,
                    contentBottom - drawerY,
                    mouseX,
                    mouseY,
                    overlay);
        }
        if (headerHoverTip != null && !headerHoverTip.isBlank()) {
            hoverTip = headerHoverTip;
        }
        if (hoverTip != null) {
            RotClientUiDraw.noteHoverTip(hoverTip);
        }
    }

    private void roundedHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                QolUtilityUiMath.PAGE_HEADER_HEIGHT);

        graphics.fill(
                x + 1,
                y + 9,
                x + 4,
                y + 42,
                accentColor());

        RotClientUiDraw.pageTitle(
                graphics,
                font,
                activePage.title(),
                x + 14,
                y + 8);

        List<QolUtilityCatalog.ModuleDef> pageModules =
                QolUtilityCatalog.modulesOnGroupPage(
                        activePage);

        int enabledCount =
                0;

        int cheatCount =
                0;

        for (QolUtilityCatalog.ModuleDef module
                : pageModules) {

            if (runtimeAvailable(module)
                    && isEnabled(module)) {

                enabledCount++;
            }

            if (QolUtilityCatalog.hasCheatTag(
                    module)) {

                cheatCount++;
            }
        }

        if (!isDrawerOpen()) {

            String countLabel =
                    enabledCount
                            + " ON  /  "
                            + pageModules.size();

            RotClientUiDraw.drawStatusPill(
                    graphics,
                    font,
                    x + width - 12,
                    y + 7,
                    countLabel,
                    enabledCount > 0
                            ? accentColor()
                            : RotClientTheme.TEXT_MUTED);
        }

        String desc =
                activePage.pageDescription();

        int descMax =
                Math.max(
                        80,
                        width - 28);

        String shownDesc =
                RotClientUiDraw.ellipsizeAndHover(
                        font,
                        desc,
                        descMax,
                        x + 14,
                        y + 25,
                        12);

        RotClientUiDraw.helpText(
                graphics,
                font,
                shownDesc,
                x + 14,
                y + 25);

        this.headerHoverTip =
                null;

        if (RotClientUiDraw.truncated(
                font,
                desc,
                descMax)
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x + 14,
                y + 22,
                descMax,
                14)) {

            this.headerHoverTip =
                    desc;
        }

        graphics.fill(
                x + 12,
                y + 45,
                x + width - 12,
                y + 46,
                RotClientTheme.DIVIDER);

        for (QolUtilityUiMath.FilterChip chip
                : QolUtilityUiMath.pageFilterChips(
                x,
                y)) {

            boolean selected =
                    chip.filter()
                            == pageFilter;

            boolean hover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            chip.x(),
                            chip.y(),
                            chip.width(),
                            chip.height());

            RotClientUiDraw.drawInteractiveSurface(
                    graphics,
                    chip.x(),
                    chip.y(),
                    chip.width(),
                    chip.height(),
                    hover
                            ? 1.0F
                            : 0.0F,
                    selected,
                    accentColor(),
                    RotClientUiDraw.RADIUS_SM);

            String chipLabel;

            if (chip.filter()
                    == QolUtilityUiMath.PageFilter.ALL) {

                chipLabel =
                        "All ["
                                + pageModules.size()
                                + "]";

            } else if (chip.filter()
                    == QolUtilityUiMath.PageFilter.ENABLED) {

                chipLabel =
                        "On ["
                                + enabledCount
                                + "]";

            } else {
                chipLabel =
                        QolUtilityUiMath.cheatFilterLabel(cheatCount);
            }

            int labelX =
                    chip.x()
                            + (
                            chip.width()
                                    - font.width(
                                    chipLabel))
                            / 2;

            RotClientUiDraw.text(
                    graphics,
                    font,
                    chipLabel,
                    labelX,
                    chip.y() + 5,
                    selected
                            ? RotClientTheme.TEXT
                            : RotClientTheme.TEXT_DIM,
                    selected || hover);
        }
    }
    private int accentColor() {
        return RotClientClient.clickGuiAccentColor();
    }

    private static int evidenceColor(QolModuleEvidence.Status status) {
        return switch (status) {
            case READY -> RotClientTheme.STATUS_READY;
            case NEEDS_TESTING -> RotClientTheme.STATUS_NEEDS_TESTING;
            case WORK_IN_PROGRESS -> RotClientTheme.STATUS_WORK_IN_PROGRESS;
            case UPCOMING -> RotClientTheme.STATUS_UPCOMING;
        };
    }

    private int panelRadius() {
        return RotClientClient.clickGuiRoundedBottoms()
                ? RotClientUiDraw.RADIUS_MD
                : 0;
    }

    private boolean anyUtilityActive() {
        if (config.fullbrightEnabled
                || config.alwaysNightEnabled
                || config.autoSprintEnabled
                || config.cameraEnabled) {
            return true;
        }
        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if (runtimeAvailable(module) && isEnabled(module)) {
                return true;
            }
        }
        return false;
    }

    private float moduleCardHoverAmount(
            String moduleId,
            boolean hovered) {

        String key =
                moduleId == null
                        ? ""
                        : moduleId;

        double current =
                moduleCardHoverAmounts
                        .getOrDefault(
                                key,
                                0.0D);

        double next =
                RotClientEase.expToward(
                        current,
                        hovered
                                ? 1.0D
                                : 0.0D,
                        RotClientUiClock.seconds(),
                        16.0D);

        if (next <= 0.0001D
                && !hovered) {

            moduleCardHoverAmounts.remove(
                    key);

        } else {
            moduleCardHoverAmounts.put(
                    key,
                    next);
        }

        return (float) RotClientEase.smoothstep(
                next);
    }

    private boolean moduleAccordionOpen() {
        return isDrawerOpen() && drawerKind == DrawerKind.MODULE;
    }

    private QolUtilityCatalog.ModuleDef moduleAccordionModule() {
        if (!moduleAccordionOpen()) {
            return null;
        }
        return QolUtilityCatalog.findById(openModuleId);
    }

    private void resetModuleAccordionAnimation() {
        moduleAccordionProgress = 0.0D;
        moduleAccordionLastNanos = 0L;
        moduleAccordionClosing = false;
    }

    private void requestModuleAccordionClose() {
        if (!moduleAccordionOpen()) {
            closeDrawer();
            return;
        }

        pendingModuleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        draggingNumberSettingId = "";

        moduleAccordionClosing = true;
        moduleAccordionLastNanos = System.nanoTime();

        if (moduleAccordionProgress <= 0.0001D) {
            completeModuleAccordionClose();
        }
    }

    private void completeModuleAccordionClose() {
        openModuleId = "";
        drawerKind = DrawerKind.MODULE;
        pendingModuleResetConfirm = false;
        pendingHudStyleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        draggingNumberSettingId = "";
        drawerScroll.reset();
        resetModuleAccordionAnimation();
        persistWorkspaceView();
    }

    private void toggleModuleAccordion(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return;
        }

        if (focusingModule(moduleId)) {
            if (moduleAccordionClosing) {
                moduleAccordionClosing = false;
                moduleAccordionLastNanos = System.nanoTime();
            } else {
                requestModuleAccordionClose();
            }
            return;
        }

        openModule(moduleId);
    }

    private void advanceModuleAccordionAnimation() {
        if (!moduleAccordionOpen()) {
            resetModuleAccordionAnimation();
            return;
        }

        long now = System.nanoTime();

        if (moduleAccordionLastNanos == 0L) {
            moduleAccordionLastNanos = now;
            return;
        }

        double elapsed = Math.max(
                0.0D,
                Math.min(
                        0.05D,
                        (now - moduleAccordionLastNanos) / 1_000_000_000.0D));

        moduleAccordionLastNanos = now;

        double step = elapsed / MODULE_ACCORDION_SECONDS;

        if (moduleAccordionClosing) {
            moduleAccordionProgress = Math.max(
                    0.0D,
                    moduleAccordionProgress - step);

            if (moduleAccordionProgress <= 0.0D) {
                completeModuleAccordionClose();
            }
            return;
        }

        moduleAccordionProgress = Math.min(
                1.0D,
                moduleAccordionProgress + step);
    }

    private int moduleAccordionTargetHeight(
            QolUtilityCatalog.ModuleDef module) {

        if (module == null) {
            return 0;
        }

        int bodyHeight =
                0;

        if (module.wip()
                || !module.runtimeReady()) {

            bodyHeight +=
                    QolUtilityUiMath.DRAWER_ROW_HEIGHT
                            + 4;
        }

        String sectionId =
                "";

        for (QolUtilityCatalog.SettingDef setting
                : currentDrawerSettings(
                        module)) {

            if (setting.type()
                    == QolUtilityCatalog.SettingType.SECTION) {

                sectionId =
                        setting.id();

                bodyHeight +=
                        QolUtilityUiMath.DRAWER_SECTION_HEIGHT;

                continue;
            }

            double open =
                    drawerSectionAmount(
                            sectionId);

            if (open <= 0.02D) {
                continue;
            }

            int step =
                    QolUtilityUiMath.settingRowHeight(
                            setting)
                            + 4;

            int shown =
                    sectionId.isEmpty()
                            ? step
                            : RotClientEase.shownPixels(
                                    step,
                                    open);

            bodyHeight +=
                    Math.max(
                            0,
                            shown);

            if (runtimeAvailable(module)
                    && setting.type()
                    == QolUtilityCatalog.SettingType.ENUM
                    && setting.id()
                    .equals(
                            openEnumSettingId)) {

                if (OverflowListPolicy.needsSearch(
                        setting.enumOptions()
                                .size())) {

                    int searchShown =
                            sectionId.isEmpty()
                                    ? QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT
                                    : RotClientEase.shownPixels(
                                            QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT,
                                            open);

                    bodyHeight +=
                            Math.max(
                                    0,
                                    searchShown);
                }

                for (String ignored
                        : visibleEnumOptions(
                                setting)) {

                    int optionShown =
                            sectionId.isEmpty()
                                    ? QolUtilityUiMath.ENUM_OPTION_HEIGHT
                                    : RotClientEase.shownPixels(
                                            QolUtilityUiMath.ENUM_OPTION_HEIGHT,
                                            open);

                    bodyHeight +=
                            Math.max(
                                    0,
                                    optionShown);
                }
            }
        }

        if (!module.settings().isEmpty()
                && runtimeAvailable(module)) {

            bodyHeight +=
                    8
                            + QolUtilityUiMath.DRAWER_ROW_HEIGHT
                            + 4;
        }

        /*
         * Eight pixels above the settings and eight below them.
         */
        return Math.max(
                48,
                bodyHeight + 16);
    }
    private int moduleAccordionVisibleHeight(
            QolUtilityCatalog.ModuleDef module) {
        if (module == null) {
            return 0;
        }

        double eased = RotClientEase.smoothstep(moduleAccordionProgress);

        return (int) Math.round(
                moduleAccordionTargetHeight(module) * eased);
    }

    private static int moduleAccordionRowY(
            QolUtilityUiMath.PageLayout layout,
            QolUtilityCatalog.ModuleDef module) {
        if (layout == null || module == null) {
            return -1;
        }

        for (QolUtilityUiMath.PlacedCard card : layout.cards()) {
            if (card.module() != null
                    && module.id().equals(card.module().id())) {
                return card.y();
            }
        }

        return -1;
    }

    private static String[] moduleDescriptionLines(
            Font font,
            String value,
            int maxWidth) {

        String description =
                value == null
                        ? ""
                        : value.trim();

        if (description.isEmpty()) {
            return new String[]{
                    "",
                    ""
            };
        }

        int width =
                Math.max(
                        20,
                        maxWidth);

        if (font.width(description)
                <= width) {

            return new String[]{
                    description,
                    ""
            };
        }

        StringBuilder first =
                new StringBuilder();

        StringBuilder second =
                new StringBuilder();

        boolean secondLine =
                false;

        for (String word
                : description.split("\\s+")) {

            if (word.isBlank()) {
                continue;
            }

            if (!secondLine) {

                String candidate =
                        first.length() == 0
                                ? word
                                : first
                                + " "
                                + word;

                if (font.width(candidate)
                        <= width) {

                    if (first.length() > 0) {
                        first.append(' ');
                    }

                    first.append(word);
                    continue;
                }

                secondLine =
                        true;
            }

            if (second.length() > 0) {
                second.append(' ');
            }

            second.append(word);
        }

        return new String[]{
                RotClientUiDraw.ellipsize(
                        font,
                        first.toString(),
                        width),

                RotClientUiDraw.ellipsize(
                        font,
                        second.toString(),
                        width)
        };
    }

    private String drawModuleCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            QolUtilityCatalog.ModuleDef module,
            int mouseX,
            int mouseY) {

        boolean hover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        QolUtilityUiMath.CARD_HEIGHT);

        boolean enabled =
                isEnabled(
                        module);

        boolean available =
                runtimeAvailable(
                        module);

        boolean selected =
                module.id()
                        .equals(
                                openModuleId);

        int expandedCardHeight =
                QolUtilityUiMath.CARD_HEIGHT;

        if (selected
                && moduleAccordionOpen()) {

            expandedCardHeight +=
                    moduleAccordionVisibleHeight(
                            module);
        }

        float hoverAmount =
                moduleCardHoverAmount(
                        module.id(),
                        hover);

        int cardAccent =
                selected
                        ? RotClientTheme.VIOLET
                        : available && enabled
                        ? accentColor()
                        : RotClientTheme.BORDER_BRIGHT;

        RotClientUiDraw.drawInteractiveSurface(
                graphics,
                x,
                y,
                width,
                expandedCardHeight,
                hoverAmount,
                selected,
                cardAccent,
                panelRadius());

        int railColor =
                available && enabled
                        ? accentColor()
                        : selected
                        ? RotClientTheme.VIOLET
                        : RotClientTheme.DIVIDER;

        int railAlpha =
                selected
                        || available && enabled
                        ? 0xFF
                        : Math.min(
                                0xFF,
                                0x70
                                        + Math.round(
                                        0x50 * hoverAmount));

        graphics.fill(
                x + 1,
                y + 10,
                x + 4,
                y + expandedCardHeight - 10,
                RotClientUiDraw.withAlpha(
                        railColor,
                        railAlpha));

        QolModuleEvidence.Status evidence =
                module.evidenceStatus();

        String statusLabel =
                evidence.label();

        int statusColor =
                evidenceColor(
                        evidence);

        int statusWidth =
                QolUtilityUiMath.statusBadgeWidth(
                        font.width(
                                statusLabel));

        int titleMax =
                Math.max(
                        70,
                        width
                                - statusWidth
                                - 44);

        String title =
                module.name();

        String shownTitle =
                RotClientUiDraw.ellipsizeAndHover(
                        font,
                        title,
                        titleMax,
                        x + 14,
                        y + 9,
                        14);

        RotClientUiDraw.text(
                graphics,
                font,
                shownTitle,
                x + 14,
                y + 12,
                available
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawStatusPill(
                graphics,
                font,
                x + width - 12,
                y + 7,
                statusLabel,
                statusColor);

        String hoverTip =
                null;

        if (RotClientUiDraw.truncated(
                font,
                title,
                titleMax)
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x + 14,
                y + 8,
                titleMax,
                16)) {

            hoverTip =
                    title;
        }

        int descriptionWidth =
                Math.max(
                        80,
                        width - 28);

        String[] descriptionLines =
                moduleDescriptionLines(
                        font,
                        module.description(),
                        descriptionWidth);

        RotClientUiDraw.helpText(
                graphics,
                font,
                descriptionLines[0],
                x + 14,
                y + 31);

        if (!descriptionLines[1].isBlank()) {
            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    descriptionLines[1],
                    x + 14,
                    y + 44);
        }

        if (font.width(
                module.description())
                > descriptionWidth
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x + 14,
                y + 28,
                descriptionWidth,
                30)) {

            hoverTip =
                    module.description();
        }

        java.util.List<HudElementCatalog.HudPiece> hudPieces =
                HudElementCatalog.hudPieces(
                        module);

        boolean cheat =
                QolFlavorSupport.isPlus()
                        && QolUtilityCatalog.hasCheatTag(
                        module);

        String metadata =
                module.settings().isEmpty()
                        ? "No settings"
                        : module.settings().size()
                        + (
                        module.settings().size() == 1
                                ? " setting"
                                : " settings");

        if (!hudPieces.isEmpty()) {
            metadata +=
                    " \u00B7 HUD";
        }

        if (cheat) {
            metadata +=
                    " \u00B7 CHEAT";
        }

        RotClientUiDraw.text(
                graphics,
                font,
                RotClientUiDraw.ellipsize(
                        font,
                        metadata,
                        descriptionWidth),
                x + 14,
                y + 59,
                cheat
                        ? RotClientTheme.WARNING
                        : enabled
                        ? RotClientTheme.TEXT_DIM
                        : RotClientTheme.TEXT_MUTED,
                false);

        int footerY =
                QolUtilityUiMath.cardFooterY(
                        y,
                        QolUtilityUiMath.CARD_HEIGHT);

        graphics.fill(
                x + 12,
                footerY - 5,
                x + width - 12,
                footerY - 4,
                selected
                        ? RotClientUiDraw.withAlpha(
                        cardAccent,
                        0x70)
                        : RotClientTheme.DIVIDER);

        if (module.toggleable()
                && available) {

            int toggleX =
                    QolUtilityUiMath.moduleToggleX(
                            x);

            int toggleY =
                    QolUtilityUiMath.moduleToggleY(
                            y,
                            QolUtilityUiMath.CARD_HEIGHT);

            boolean toggleHover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            toggleX,
                            toggleY,
                            QolUtilityUiMath.TOGGLE_WIDTH,
                            QolUtilityUiMath.TOGGLE_HEIGHT);

            RotClientUiDraw.drawAnimatedToggle(
                    graphics,
                    toggleX,
                    toggleY,
                    enabled,
                    toggleHover,
                    "module-toggle:" + module.id(),
                    accentColor());

            RotClientUiDraw.text(
                    graphics,
                    font,
                    enabled
                            ? "ON"
                            : "OFF",
                    toggleX
                            + QolUtilityUiMath.TOGGLE_WIDTH
                            + 6,
                    toggleY + 3,
                    enabled
                            ? accentColor()
                            : RotClientTheme.TEXT_MUTED,
                    true);

        } else if (module.wip()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "COMING SOON",
                    x + 14,
                    footerY + 6,
                    RotClientTheme.WARNING,
                    true);

        } else if (!module.runtimeReady()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "PLANNED",
                    x + 14,
                    footerY + 6,
                    RotClientTheme.WARNING,
                    true);
        }

        if (!hudPieces.isEmpty()
                && available) {

            int hudX =
                    QolUtilityUiMath.hudControlX(
                            x);

            int hudY =
                    QolUtilityUiMath.hudControlY(
                            y,
                            QolUtilityUiMath.CARD_HEIGHT);

            RotClientUiDraw.drawPremiumButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    hudX,
                    hudY,
                    QolUtilityUiMath.HUD_CONTROL_WIDTH,
                    QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                    "HUD",
                    focusingHud(
                            module.id()),
                    true);
        }

        if (!module.settings().isEmpty()) {

            double openAmount =
                    moduleAccordionOpen()
                            && selected
                            ? RotClientEase.smoothstep(
                                    moduleAccordionProgress)
                            : 0.0D;

            String settingsLabel =
                    "Settings";

            int chevronX =
                    x
                            + width
                            - 22;

            int labelX =
                    chevronX
                            - font.width(
                                    settingsLabel)
                            - 8;

            RotClientUiDraw.text(
                    graphics,
                    font,
                    settingsLabel,
                    labelX,
                    footerY + 6,
                    openAmount > 0.45D
                            ? accentColor()
                            : RotClientTheme.TEXT_DIM,
                    openAmount > 0.45D
                            || hover);

            RotClientUiDraw.drawChevron(
                    graphics,
                    chevronX,
                    footerY + 6,
                    openAmount,
                    openAmount > 0.45D
                            || hover
                            ? RotClientTheme.TEXT
                            : RotClientTheme.TEXT_MUTED);
        }
        return hoverTip;
    }
    private void drawDrawer(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            boolean overlay) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
        if (module == null) {
            closeDrawer();
            return;
        }
        boolean inline =
                !overlay
                        && moduleAccordionOpen();

        if (!inline) {
            tickDrawerExpand(
                    module);
        }

        if (!inline) {
        if (overlay) {
            graphics.fill(
                    x - 8,
                    y,
                    x,
                    y + height,
                    0x66000000);
        }

        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + height,
                RotClientTheme.SURFACE,
                panelRadius());

        RotClientUiDraw.roundedFill(
                graphics,
                x + 7,
                y + 6,
                x + width - 7,
                y + 46,
                RotClientTheme.SURFACE_ALT,
                RotClientUiDraw.RADIUS_SM);

        graphics.fill(
                x + 8,
                y + 12,
                x + 11,
                y + 40,
                accentColor());

        String drawerTitle =
                hudDrawerTitle(
                        module);

        int titleMax =
                Math.max(
                        80,
                        width - 90);

        RotClientUiDraw.text(
                graphics,
                font,
                RotClientUiDraw.ellipsize(
                        font,
                        drawerTitle,
                        titleMax),
                x + 18,
                y + 11,
                RotClientTheme.TEXT,
                true);

        String evidenceLabel =
                module.evidenceStatus()
                        .label();

        int evidenceColor =
                evidenceColor(
                        module.evidenceStatus());

        RotClientUiDraw.text(
                graphics,
                font,
                evidenceLabel,
                x + 18
                        + Math.min(
                        titleMax,
                        font.width(
                                drawerTitle))
                        + 10,
                y + 11,
                evidenceColor,
                false);

        boolean cheat =
                QolFlavorSupport.isPlus()
                        && QolUtilityCatalog.hasCheatTag(
                        module)
                        && !hudDrawerOpen();

        if (cheat) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    "CHEAT",
                    x + 18,
                    y + 27,
                    RotClientTheme.WARNING,
                    true);
        }

        int descX =
                cheat
                        ? x + 60
                        : x + 18;

        int descMax =
                Math.max(
                        40,
                        x + width - 48 - descX);

        String explanation =
                hudDrawerOpen()
                        ? "Turn this overlay on, change its look, and open layout edit to move it."
                        : HudElementCatalog.explainedDescription(
                        module);

        String shownDesc =
                RotClientUiDraw.ellipsizeAndHover(
                        font,
                        explanation,
                        descMax,
                        descX,
                        y + 25,
                        12);

        RotClientUiDraw.text(
                graphics,
                font,
                shownDesc,
                descX,
                y + 28,
                RotClientTheme.TEXT_DIM,
                false);

        if (RotClientUiDraw.truncated(
                font,
                explanation,
                descMax)
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                descX,
                y + 25,
                descMax,
                14)) {

            this.headerHoverTip =
                    explanation;
        }

        boolean closeHover = QolUtilityUiMath.hitClose(mouseX, mouseY, x, y, width);
        int closeX = x + width - 34;
        int closeY = y + 14;
        RotClientUiDraw.roundedFill(
                graphics,
                closeX,
                closeY,
                closeX + 16,
                closeY + 16,
                closeHover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON);
        RotClientUiDraw.text(graphics, font, "×", closeX + 4, closeY + 3, RotClientTheme.TEXT, true);

        }

        int bodyTop =
                inline
                        ? y + 8
                        : y + 52;

        int bodyBottom =
                inline
                        ? y + height
                        : y + height - 10;
        drawerScroll.setViewportHeight(Math.max(0, bodyBottom - bodyTop));
        drawerScroll.advanceSeconds(RotClientUiClock.seconds());

        int rowY = bodyTop - drawerScroll.scrollPixels();
        graphics.enableScissor(x, bodyTop, x + width, bodyBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, drawerScroll);
        try {
        if (hudDrawerShowsEnableRow(module)) {
            boolean hudOn = isEnabled(module);
            drawDrawerToggleRow(
                    graphics,
                    font,
                    x + 12,
                    rowY,
                    width - 24,
                    "HUD ON/OFF",
                    hudOn ? "This overlay is visible in the world." : "This overlay is hidden.",
                    hudOn,
                    mouseX,
                    mouseY,
                    runtimeAvailable(module));
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 8;
        } else if (!inline && !hudDrawerOpen() && module.toggleable() && runtimeAvailable(module)) {
            boolean enabled = isEnabled(module);
            drawDrawerToggleRow(
                    graphics,
                    font,
                    x + 12,
                    rowY,
                    width - 24,
                    "Module ON/OFF",
                    enabled ? "This switch turns the whole module on. HUD pieces have their own toggles below." : "Module is OFF. HUD pieces stay hidden until you turn this on.",
                    enabled,
                    mouseX,
                    mouseY,
                    true);
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 8;
        } else if (module.wip()) {
            RotClientUiDraw.text(graphics, font,
                    "Coming Later — settings are previews only",
                    x + 14,
                    rowY + 6,
                    RotClientTheme.WARNING,
                    false);
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 4;
        } else if (!module.runtimeReady()) {
            RotClientUiDraw.text(graphics, font,
                    "Planned for Batch 3 — settings are previews only",
                    x + 14,
                    rowY + 6,
                    RotClientTheme.WARNING,
                    false);
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 4;
        }

        String drawerSectionId = "";
        for (QolUtilityCatalog.SettingDef setting : currentDrawerSettings(module)) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                drawerSectionId = setting.id();
                if (rowY + QolUtilityUiMath.DRAWER_SECTION_HEIGHT >= bodyTop
                        && rowY <= bodyBottom) {
                    drawDrawerSectionHeader(
                            graphics,
                            font,
                            x + 12,
                            rowY,
                            width - 24,
                            setting.label(),
                            drawerExpand.amount(setting.id()),
                            mouseX,
                            mouseY);
                }
                rowY += QolUtilityUiMath.DRAWER_SECTION_HEIGHT;
                continue;
            }
            double open = drawerSectionAmount(drawerSectionId);
            if (open <= 0.02D) {
                continue;
            }
            int rowHeight = QolUtilityUiMath.settingRowHeight(setting);
            int step = rowHeight + 4;
            int shown = drawerSectionId.isEmpty()
                    ? step
                    : RotClientEase.shownPixels(step, open);
            if (shown <= 0) {
                continue;
            }
            if (rowY + shown >= bodyTop
                    && rowY <= bodyBottom) {
                drawSettingRow(
                        graphics,
                        font,
                        x + 12,
                        rowY,
                        width - 24,
                        module,
                        setting,
                        mouseX,
                        mouseY);
            }
            rowY += shown;
            if (runtimeAvailable(module)
                    && setting.type() == QolUtilityCatalog.SettingType.ENUM
                    && setting.id().equals(openEnumSettingId)) {
                boolean search = OverflowListPolicy.needsSearch(setting.enumOptions().size());
                if (search) {
                    int searchShown = drawerSectionId.isEmpty()
                            ? QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT
                            : RotClientEase.shownPixels(
                                    QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT, open);
                    if (searchShown > 0
                            && rowY + searchShown >= bodyTop
                            && rowY <= bodyBottom) {
                        drawEnumSearchField(
                                graphics,
                                font,
                                x + 24,
                                rowY,
                                width - 36,
                                mouseX,
                                mouseY);
                    }
                    rowY += Math.max(0, searchShown);
                }
                for (String option : visibleEnumOptions(setting)) {
                    int optionShown = drawerSectionId.isEmpty()
                            ? QolUtilityUiMath.ENUM_OPTION_HEIGHT
                            : RotClientEase.shownPixels(
                                    QolUtilityUiMath.ENUM_OPTION_HEIGHT, open);
                    if (optionShown <= 0) {
                        continue;
                    }
                    if (rowY + optionShown >= bodyTop
                            && rowY <= bodyBottom) {
                        drawEnumOption(
                                graphics,
                                font,
                                x + 24,
                                rowY,
                                width - 36,
                                option,
                                option.equals(qol().readEnum(setting.id())),
                                mouseX,
                                mouseY);
                    }
                    rowY += optionShown;
                }
            }
        }

        if (hudDrawerOpen()) {
            rowY += 8;
            if (rowY + QolUtilityUiMath.DRAWER_ROW_HEIGHT >= bodyTop
                    && rowY <= bodyBottom) {
                drawHudStyleResetRow(
                        graphics,
                        font,
                        x + 12,
                        rowY,
                        width - 24,
                        mouseX,
                        mouseY);
            }
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 4;
        } else if (!module.settings().isEmpty() && runtimeAvailable(module)) {
            rowY += 8;
            if (rowY + QolUtilityUiMath.DRAWER_ROW_HEIGHT >= bodyTop
                    && rowY <= bodyBottom) {
                drawResetRow(
                        graphics,
                        font,
                        x + 12,
                        rowY,
                        width - 24,
                        mouseX,
                        mouseY);
            }
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 4;
        }
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }

        drawerContentHeight = Math.max(0, rowY + drawerScroll.scrollPixels() - bodyTop);
        drawerScroll.setBounds(
                drawerContentHeight, Math.max(0, bodyBottom - bodyTop));
        if (!inline && drawerScroll.canScroll()) {
            int scrollbarX = x + width - RotClientUiDraw.SCROLLBAR_WIDTH - 2;
            boolean hovered = scrollbarHovered(
                    mouseX,
                    mouseY,
                    x + width - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                    bodyTop,
                    bodyBottom);
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    scrollbarX,
                    bodyTop,
                    bodyBottom,
                    drawerScroll.contentHeight(),
                    drawerScroll.scrollPixels(),
                    hovered,
                    drawerScroll.isThumbDragging());
        }
    }

    private void tickDrawerExpand(QolUtilityCatalog.ModuleDef module) {
        java.util.ArrayList<String> known = new java.util.ArrayList<>();
        java.util.ArrayList<String> open = new java.util.ArrayList<>();
        if (module != null) {
            for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                if (setting.type() != QolUtilityCatalog.SettingType.SECTION) {
                    continue;
                }
                known.add(setting.id());
                if (!collapsedDrawerSections.contains(setting.id())) {
                    open.add(setting.id());
                }
            }
        }
        drawerExpand.syncTargets(open, known);
        drawerExpand.advanceSeconds(RotClientUiClock.seconds());
    }

    private double drawerSectionAmount(String sectionId) {
        if (sectionId == null || sectionId.isBlank()) {
            return 1.0D;
        }
        return drawerExpand.amount(sectionId);
    }

    private void drawDrawerSectionHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String label,
            double openAmount,
            int mouseX,
            int mouseY) {

        boolean hover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        QolUtilityUiMath.DRAWER_SECTION_HEIGHT);

        float open =
                (float) RotClientEase.smoothstep(
                        openAmount);

        RotClientUiDraw.drawInteractiveSurface(
                graphics,
                x,
                y,
                width,
                QolUtilityUiMath.DRAWER_SECTION_HEIGHT,
                hover
                        ? 1.0F
                        : open * 0.22F,
                false,
                accentColor(),
                RotClientUiDraw.RADIUS_SM);

        RotClientUiDraw.drawChevron(
                graphics,
                x + 7,
                y + 6,
                openAmount,
                hover || open > 0.4F
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_MUTED);

        RotClientUiDraw.glyph(
                graphics,
                font,
                label,
                x + 23,
                y + 7,
                hover || open > 0.4F
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_DIM,
                true);

        if (open > 0.05F) {
            graphics.fill(
                    x + 23,
                    y + QolUtilityUiMath.DRAWER_SECTION_HEIGHT - 2,
                    x + 23
                            + Math.max(
                            8,
                            Math.round(
                                    20.0F * open)),
                    y + QolUtilityUiMath.DRAWER_SECTION_HEIGHT - 1,
                    RotClientUiDraw.withAlpha(
                            accentColor(),
                            Math.max(
                                    0x40,
                                    Math.round(
                                            0xB0 * open))));
        }
    }
    private void drawResetRow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.DRAWER_ROW_HEIGHT);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.DRAWER_ROW_HEIGHT,
                hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT,
                RotClientUiDraw.RADIUS_SM);
        String label = pendingModuleResetConfirm
                ? "Click again to confirm reset"
                : "Reset Module to Defaults";
        RotClientUiDraw.text(graphics, font,
                label,
                x + 8,
                y + 9,
                pendingModuleResetConfirm ? RotClientTheme.WARNING : RotClientTheme.TEXT_DIM,
                false);
    }

    private void drawHudStyleResetRow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.DRAWER_ROW_HEIGHT);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.DRAWER_ROW_HEIGHT,
                hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT,
                RotClientUiDraw.RADIUS_SM);
        String label = pendingHudStyleResetConfirm
                ? "Click again to reset this HUD look"
                : "Reset HUD Look";
        RotClientUiDraw.text(graphics, font,
                label,
                x + 8,
                y + 9,
                pendingHudStyleResetConfirm ? RotClientTheme.WARNING : RotClientTheme.TEXT_DIM,
                false);
    }

    private void drawDrawerToggleRow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String label,
            String hint,
            boolean enabled,
            int mouseX,
            int mouseY,
            boolean interactive) {

        boolean hover =
                interactive
                        && RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        QolUtilityUiMath.DRAWER_ROW_HEIGHT);

        RotClientUiDraw.drawInteractiveSurface(
                graphics,
                x,
                y,
                width,
                QolUtilityUiMath.DRAWER_ROW_HEIGHT,
                hover
                        ? 1.0F
                        : 0.0F,
                false,
                accentColor(),
                RotClientUiDraw.RADIUS_SM);

        if (enabled) {
            graphics.fill(
                    x + 1,
                    y + 8,
                    x + 3,
                    y
                            + QolUtilityUiMath.DRAWER_ROW_HEIGHT
                            - 8,
                    accentColor());
        }

        RotClientUiDraw.text(
                graphics,
                font,
                label,
                x + 9,
                y + 6,
                interactive
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_MUTED,
                true);

        String shownHint =
                RotClientUiDraw.ellipsizeAndHover(
                        font,
                        hint,
                        Math.max(
                                40,
                                width
                                        - QolUtilityUiMath.DRAWER_CONTROL_RESERVE
                                        - 18),
                        x + 9,
                        y + 22,
                        12);

        RotClientUiDraw.text(
                graphics,
                font,
                shownHint,
                x + 9,
                y + 24,
                enabled
                        ? accentColor()
                        : RotClientTheme.TEXT_MUTED,
                false);

        RotClientUiDraw.drawAnimatedToggle(
                graphics,
                x
                        + width
                        - QolUtilityUiMath.DRAWER_CONTROL_RESERVE
                        + 8,
                y + 12,
                enabled,
                hover,
                "drawer-toggle:" + drawerKind + ":" + openModuleId,
                accentColor());
    }
    private void drawSettingRow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            QolUtilityCatalog.ModuleDef module,
            QolUtilityCatalog.SettingDef setting,
            int mouseX,
            int mouseY) {

        boolean disabled =
                !runtimeAvailable(
                        module);

        int rowHeight =
                QolUtilityUiMath.settingRowHeight(
                        setting);

        boolean hover =
                !disabled
                        && RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        rowHeight);

        if (hover
                && setting.description() != null
                && !setting.description().isBlank()) {

            this.headerHoverTip =
                    HudElementCatalog.explainedSetting(
                            setting);
        }

        RotClientUiDraw.drawInteractiveSurface(
                graphics,
                x,
                y,
                width,
                rowHeight,
                hover
                        ? 1.0F
                        : 0.0F,
                false,
                accentColor(),
                RotClientUiDraw.RADIUS_SM);

        String trailing =
                settingTrailing(
                        setting);

        int trailingColor =
                disabled
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.TEXT_DIM;

        String label =
                RotClientUiDraw.ellipsizeAndHover(
                        font,
                        setting.label(),
                        QolUtilityUiMath.settingLabelMaxWidth(
                                width,
                                setting.type()),
                        x + 9,
                        y + 4,
                        12);

        RotClientUiDraw.text(
                graphics,
                font,
                label,
                x + 9,
                y + 6,
                disabled
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.TEXT,
                false);

        if (setting.type()
                == QolUtilityCatalog.SettingType.TOGGLE) {

            Boolean value =
                    readDrawerBoolean(
                            setting.id());

            boolean on =
                    value != null
                            && value
                            && !disabled;

            if (on) {
                graphics.fill(
                        x + 1,
                        y + 8,
                        x + 3,
                        y + rowHeight - 8,
                        accentColor());
            }

            RotClientUiDraw.drawAnimatedToggle(
                    graphics,
                    x
                            + width
                            - QolUtilityUiMath.DRAWER_CONTROL_RESERVE
                            + 8,
                    y + 12,
                    on,
                    hover,
                    "setting-toggle:" + setting.id(),
                    accentColor());

        } else if (setting.type()
                == QolUtilityCatalog.SettingType.SQUARE) {

            Boolean value =
                    readDrawerBoolean(
                            setting.id());

            RotClientUiDraw.drawSquareLatch(
                    graphics,
                    x + width - 28,
                    y + 12,
                    value != null
                            && value
                            && !disabled,
                    hover);

        } else if (setting.type()
                == QolUtilityCatalog.SettingType.COLOR) {

            Integer color =
                    readDrawerColor(
                            setting.id());

            int swatch =
                    color == null
                            ? RotClientTheme.HUD_ACCENT
                            : color;

            RotClientUiDraw.roundedFill(
                    graphics,
                    x + width - 30,
                    y + 7,
                    x + width - 8,
                    y + 23,
                    RotClientTheme.FIELD,
                    RotClientUiDraw.RADIUS_XS);

            graphics.fill(
                    x + width - 27,
                    y + 10,
                    x + width - 11,
                    y + 20,
                    swatch);

            RotClientUiDraw.roundedOutline(
                    graphics,
                    x + width - 30,
                    y + 7,
                    x + width - 8,
                    y + 23,
                    hover
                            ? RotClientTheme.BORDER_BRIGHT
                            : RotClientTheme.BORDER,
                    RotClientUiDraw.RADIUS_XS);

        } else if (setting.type()
                == QolUtilityCatalog.SettingType.NUMBER) {

            if (QolNumberSettings.usesSlider(
                    setting.id())) {

                drawNumberSlider(
                        graphics,
                        font,
                        x,
                        y,
                        width,
                        trailing,
                        setting.id(),
                        disabled,
                        mouseX,
                        mouseY);

            } else {
                drawNumberStepper(
                        graphics,
                        font,
                        x,
                        y,
                        width,
                        trailing,
                        disabled);
            }

        } else if (setting.type()
                == QolUtilityCatalog.SettingType.ENUM) {

            drawEnumControl(
                    graphics,
                    font,
                    x,
                    y,
                    width,
                    trailing,
                    disabled,
                    setting.id()
                            .equals(
                                    openEnumSettingId));

        } else if (setting.type()
                == QolUtilityCatalog.SettingType.ACTION
                || (
                setting.type()
                        == QolUtilityCatalog.SettingType.TEXT
                        && CustomScoreboardPolicy.isListTextSetting(
                        setting.id()))) {

            RotClientUiDraw.drawPremiumButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    x + width - 78,
                    y + 9,
                    70,
                    22,
                    trailing,
                    true,
                    !disabled);

        } else if (setting.type()
                == QolUtilityCatalog.SettingType.TEXT) {

            int fieldW =
                    Math.min(
                            188,
                            Math.max(
                                    120,
                                    width / 2));

            int fieldX =
                    x
                            + width
                            - fieldW
                            - 8;

            int fieldY =
                    y + 10;

            boolean focused =
                    setting.id()
                            .equals(
                                    listeningTextSettingId);

            RotClientUiDraw.roundedFill(
                    graphics,
                    fieldX,
                    fieldY,
                    fieldX + fieldW,
                    fieldY + 22,
                    focused
                            ? RotClientTheme.FIELD_ACTIVE
                            : RotClientTheme.FIELD,
                    RotClientUiDraw.RADIUS_SM);

            RotClientUiDraw.roundedOutline(
                    graphics,
                    fieldX,
                    fieldY,
                    fieldX + fieldW,
                    fieldY + 22,
                    focused
                            ? accentColor()
                            : hover
                            ? RotClientTheme.BORDER_BRIGHT
                            : RotClientTheme.BORDER,
                    RotClientUiDraw.RADIUS_SM);

            String shown =
                    trailing == null
                            || trailing.isBlank()
                            ? "Click, type, Enter"
                            : trailing;

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            shown,
                            fieldW - 12),
                    fieldX + 6,
                    fieldY + 6,
                    disabled
                            ? RotClientTheme.TEXT_MUTED
                            : RotClientTheme.TEXT,
                    false);

        } else {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    trailing,
                    x
                            + width
                            - 10
                            - font.width(
                            trailing),
                    y + 9,
                    trailingColor,
                    false);
        }
    }
    private double sliderVisualFraction(
            String settingId,
            double targetFraction) {

        String key =
                settingId == null
                        ? ""
                        : settingId;

        double target =
                RotClientEase.clamp01(
                        targetFraction);

        Double current =
                sliderVisualFractions.get(
                        key);

        /*
         * First render starts at the true value. Only later changes animate.
         */
        if (current == null
                || !Double.isFinite(
                        current)) {

            sliderVisualFractions.put(
                    key,
                    target);

            return target;
        }

        /*
         * Still visibly smooth, but slightly less floaty than the previous
         * 6.5 pass.
         */
        double next =
                RotClientEase.expToward(
                        current,
                        target,
                        RotClientUiClock.seconds(),
                        8.0D);

        sliderVisualFractions.put(
                key,
                next);

        if (sliderVisualFractions.size() > 256) {
            sliderVisualFractions.clear();
            sliderVisualFractions.put(
                    key,
                    next);
        }

        return RotClientEase.clamp01(
                next);
    }

    private float sliderInteractionAmount(
            String settingId,
            boolean active) {

        String key =
                settingId == null
                        ? ""
                        : settingId;

        double current =
                sliderInteractionAmounts.getOrDefault(
                        key,
                        0.0D);

        double next =
                RotClientEase.expToward(
                        current,
                        active
                                ? 1.0D
                                : 0.0D,
                        RotClientUiClock.seconds(),
                        12.0D);

        if (!active
                && next <= 0.0005D) {

            sliderInteractionAmounts.remove(
                    key);

            return 0.0F;
        }

        sliderInteractionAmounts.put(
                key,
                next);

        if (sliderInteractionAmounts.size() > 256) {
            sliderInteractionAmounts.clear();
            sliderInteractionAmounts.put(
                    key,
                    next);
        }

        return (float) RotClientEase.smoothstep(
                RotClientEase.clamp01(
                        next));
    }

    private void drawNumberSlider(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String value,
            String settingId,
            boolean disabled,
            int mouseX,
            int mouseY) {

        Double raw =
                readDrawerNumber(
                        settingId);

        QolNumberSettings.Spec spec =
                QolNumberSettings.spec(
                        settingId);

        double targetFraction =
                spec == null
                        || raw == null
                        ? 0.0D
                        : spec.fraction(
                                raw);

        boolean dragging =
                !disabled
                        && settingId != null
                        && settingId.equals(
                                draggingNumberSettingId);

        boolean hovered =
                !disabled
                        && QolUtilityUiMath.hitSlider(
                                mouseX,
                                mouseY,
                                x,
                                y,
                                width);

        float interaction =
                sliderInteractionAmount(
                        settingId,
                        hovered
                                || dragging);

        double visualFraction =
                sliderVisualFraction(
                        settingId,
                        targetFraction);

        int accent =
                accentColor();


        /*
         * Compact value pill.
         *
         * This stays deliberately understated now; no lift or large accent
         * wash.
         */
        int valueWidth =
                Math.max(
                        36,
                        font.width(
                                value)
                                + 14);

        int valueHeight =
                16;

        int valueX =
                x + width - valueWidth;

        int valueY =
                y + 3;

        RotClientUiDraw.roundedFill(
                graphics,
                valueX,
                valueY,
                valueX + valueWidth,
                valueY + valueHeight,
                disabled
                        ? RotClientTheme.BUTTON_DISABLED
                        : RotClientTheme.FIELD,
                RotClientUiDraw.RADIUS_SM);

        int valueBorder =
                disabled
                        ? RotClientTheme.BORDER
                        : interaction > 0.02F
                        ? RotClientUiDraw.withAlpha(
                                accent,
                                0x60
                                        + Math.round(
                                        0x30
                                                * interaction))
                        : RotClientTheme.BORDER;

        RotClientUiDraw.roundedOutline(
                graphics,
                valueX,
                valueY,
                valueX + valueWidth,
                valueY + valueHeight,
                valueBorder,
                RotClientUiDraw.RADIUS_SM);

        RotClientUiDraw.text(
                graphics,
                font,
                value,
                valueX
                        + Math.max(
                        4,
                        (
                                valueWidth
                                        - font.width(
                                        value))
                                / 2),
                valueY + 4,
                disabled
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.TEXT,
                false);


        /*
         * Clean thin rail.
         */
        int trackX =
                x
                        + QolUtilityUiMath.SLIDER_TRACK_INSET;

        int trackW =
                Math.max(
                        1,
                        width
                                - QolUtilityUiMath.SLIDER_TRACK_INSET
                                * 2);

        int trackCenterY =
                y + 29;

        int trackHeight =
                dragging
                        ? 5
                        : 4;

        int trackY =
                trackCenterY
                        - trackHeight / 2;

        int trackBottom =
                trackY
                        + trackHeight;

        RotClientUiDraw.roundedFill(
                graphics,
                trackX,
                trackY,
                trackX + trackW,
                trackBottom,
                disabled
                        ? RotClientTheme.BUTTON_DISABLED
                        : RotClientTheme.FIELD,
                Math.max(
                        1,
                        trackHeight / 2));


        /*
         * Soft active fill. Position still glides between stepped values.
         */
        int fillW =
                Math.max(
                        0,
                        Math.min(
                                trackW,
                                (int) Math.round(
                                        visualFraction
                                                * trackW)));

        if (fillW > 0) {

            if (!disabled
                    && interaction > 0.08F) {

                int glowAlpha =
                        Math.round(
                                0x20
                                        * interaction);

                RotClientUiDraw.roundedFill(
                        graphics,
                        trackX,
                        trackCenterY - 3,
                        trackX + fillW,
                        trackCenterY + 3,
                        RotClientUiDraw.withAlpha(
                                accent,
                                glowAlpha),
                        3);
            }

            int fillAlpha =
                    disabled
                            ? 0x70
                            : 0xC8
                                    + Math.round(
                                    0x22
                                            * interaction);

            RotClientUiDraw.roundedFill(
                    graphics,
                    trackX,
                    trackY,
                    trackX + fillW,
                    trackBottom,
                    disabled
                            ? RotClientTheme.BUTTON_DISABLED
                            : RotClientUiDraw.withAlpha(
                                    accent,
                                    Math.min(
                                            0xEA,
                                            fillAlpha)),
                    Math.max(
                            1,
                            trackHeight / 2));
        }


        /*
         * Small circular thumb: still polished, but no miniature-button
         * treatment anymore.
         */
        int thumbSize =
                QolUtilityUiMath.SLIDER_THUMB_SIZE
                        + Math.round(
                        interaction);

        int thumbCenterX =
                trackX + fillW;

        int thumbX =
                thumbCenterX
                        - thumbSize / 2;

        int thumbY =
                trackCenterY
                        - thumbSize / 2;

        if (!disabled
                && interaction > 0.02F) {

            int shadowAlpha =
                    0x28
                            + Math.round(
                            0x18
                                    * interaction);

            RotClientUiDraw.roundedFill(
                    graphics,
                    thumbX + 1,
                    thumbY + 1,
                    thumbX + thumbSize + 1,
                    thumbY + thumbSize + 2,
                    RotClientUiDraw.withAlpha(
                            RotClientTheme.SHADOW,
                            shadowAlpha),
                    thumbSize / 2);
        }

        RotClientUiDraw.roundedFill(
                graphics,
                thumbX,
                thumbY,
                thumbX + thumbSize,
                thumbY + thumbSize,
                disabled
                        ? RotClientTheme.TEXT_MUTED
                        : accent,
                thumbSize / 2);

        int innerInset =
                2;

        if (thumbSize > innerInset * 2) {

            RotClientUiDraw.roundedFill(
                    graphics,
                    thumbX + innerInset,
                    thumbY + innerInset,
                    thumbX + thumbSize - innerInset,
                    thumbY + thumbSize - innerInset,
                    disabled
                            ? RotClientTheme.BUTTON_DISABLED
                            : RotClientTheme.TEXT,
                    Math.max(
                            1,
                            (
                                    thumbSize
                                            - innerInset * 2)
                                    / 2));
        }
    }
    private void drawNumberStepper(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String value,
            boolean disabled) {
        int controlX = x + width - QolUtilityUiMath.NUMBER_CONTROL_WIDTH;
        int controlRight = x + width;
        int color = disabled ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT;
        RotClientUiDraw.roundedFill(
                graphics,
                controlX,
                y + 4,
                controlRight,
                y + QolUtilityUiMath.DRAWER_ROW_HEIGHT - 4,
                RotClientTheme.FIELD,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.text(graphics, font, "−", controlX + 7, y + 9, color, true);
        RotClientUiDraw.text(graphics, font,
                value,
                controlX + (QolUtilityUiMath.NUMBER_CONTROL_WIDTH - font.width(value)) / 2,
                y + 9,
                color,
                false);
        RotClientUiDraw.text(graphics, font, "+", controlRight - 15, y + 9, color, true);
    }

    private void drawEnumControl(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String value,
            boolean disabled,
            boolean open) {
        int controlWidth = Math.min(150, Math.max(100, width / 2));
        int controlX = x + width - controlWidth;
        RotClientUiDraw.roundedFill(
                graphics,
                controlX,
                y + 4,
                x + width,
                y + QolUtilityUiMath.DRAWER_ROW_HEIGHT - 4,
                RotClientTheme.FIELD,
                RotClientUiDraw.RADIUS_SM);
        int color = disabled ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT;
        RotClientUiDraw.text(graphics, font,
                RotClientUiDraw.ellipsizeAndHover(
                        font, value, controlWidth - 28, controlX + 8, y + 7, 12),
                controlX + 8,
                y + 9,
                color,
                false);
        RotClientUiDraw.text(graphics, font,
                open ? "^" : "v",
                x + width - 15,
                y + 9,
                color,
                true);
    }

    private void drawEnumOption(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String option,
            boolean selected,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.ENUM_OPTION_HEIGHT);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.ENUM_OPTION_HEIGHT,
                selected ? RotClientTheme.SELECTED_ROW
                        : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.FIELD),
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.text(graphics, font,
                (selected ? "•  " : "   ") + option,
                x + 8,
                y + 7,
                selected ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM,
                false);
    }

    private void drawEnumSearchField(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT - 2,
                enumSearchFocused || hover ? RotClientTheme.FIELD : RotClientTheme.BUTTON,
                RotClientUiDraw.RADIUS_SM);
        String shown = enumQuery.isBlank() ? "Search…" : enumQuery;
        RotClientUiDraw.text(graphics, font,
                RotClientUiDraw.ellipsizeAndHover(
                        font, shown, width - 16, x + 8, y + 5, 12),
                x + 8,
                y + 5,
                enumQuery.isBlank() ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT,
                false);
    }

    private List<String> visibleEnumOptions(QolUtilityCatalog.SettingDef setting) {
        List<String> options = setting.enumOptions();
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        if (!OverflowListPolicy.needsSearch(options.size()) || enumQuery.isBlank()) {
            return options;
        }
        List<Integer> hits = OverflowListPolicy.matchingIndices(options, enumQuery);
        List<String> out = new ArrayList<>();
        for (int index : hits) {
            out.add(options.get(index));
        }
        return out;
    }

    private String settingTrailing(QolUtilityCatalog.SettingDef setting) {
        return switch (setting.type()) {
            case ENUM -> {
                String value = qol().readEnum(setting.id());
                yield value.isBlank() ? "—" : value;
            }
            case NUMBER -> {
                Double value = readDrawerNumber(setting.id());
                if (value == null) {
                    yield "—";
                }
                if (setting.id().contains("timeout")
                        || setting.id().contains("ping")) {
                    yield ((int) Math.round(value)) + "ms";
                }
                if (setting.id().contains("cps")) {
                    yield String.format(Locale.ROOT, "%.1f", value);
                }
                if (setting.id().equals("qol.zoom.amount")) {
                    yield String.format(Locale.ROOT, "%.2fx", value);
                }
                if (setting.id().equals("qol.zoom.speed")) {
                    yield String.format(Locale.ROOT, "%.1f", value);
                }
                if (setting.id().contains("alpha")
                        || setting.id().contains("opacity")
                        || setting.id().contains("line_width")
                        || setting.id().contains("player_size")
                        || setting.id().contains("name_scale")
                        || setting.id().contains("box_size")
                        || HudDrawerPolicy.STYLE_SCALE.equals(setting.id())) {
                    yield String.format(Locale.ROOT, "%.2f", value);
                }
                yield String.format(Locale.ROOT, "%.0f", value);
            }
            case KEYBIND -> {
                if (setting.id().equals(listeningKeybindSettingId)) {
                    yield "Press a key…";
                }
                yield qol().displayKeybind(setting.id());
            }
            case TEXT -> {
                if (CustomScoreboardPolicy.isListTextSetting(setting.id())) {
                    yield "Edit →";
                }
                String value = qol().readText(setting.id());
                if (setting.id().equals(listeningTextSettingId)) {
                    yield (value.isEmpty() ? "" : value) + "▌";
                }
                if (value.isBlank()) {
                    yield "Click, type, Enter";
                }
                yield ellipsize(value.replace('\n', ' '), 28);
            }
            case ACTION -> setting.id().contains(".reset_") ? "Reset" : "Open →";
            default -> "";
        };
    }

    private static String ellipsize(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    boolean capturingKeybind() {
        return (listeningKeybindSettingId != null && !listeningKeybindSettingId.isBlank())
                || capturingText();
    }

    boolean capturingText() {
        return (listeningTextSettingId != null && !listeningTextSettingId.isBlank())
                || enumSearchFocused;
    }

    boolean captureKey(int glfwKey) {
        if (enumSearchFocused
                && openEnumSettingId != null
                && !openEnumSettingId.isBlank()) {
            if (glfwKey == GLFW.GLFW_KEY_ESCAPE) {
                openEnumSettingId = "";
                enumQuery = "";
                enumSearchFocused = false;
                return true;
            }
            if (glfwKey == GLFW.GLFW_KEY_BACKSPACE && !enumQuery.isEmpty()) {
                enumQuery = enumQuery.substring(
                        0, enumQuery.offsetByCodePoints(enumQuery.length(), -1));
                return true;
            }
            return glfwKey == GLFW.GLFW_KEY_BACKSPACE;
        }
        if (capturingText()) {
            if (glfwKey == GLFW.GLFW_KEY_ESCAPE || glfwKey == GLFW.GLFW_KEY_ENTER
                    || glfwKey == GLFW.GLFW_KEY_KP_ENTER) {
                listeningTextSettingId = "";
                TrackerStore.save(config);
                return true;
            }
            if (glfwKey == GLFW.GLFW_KEY_BACKSPACE) {
                String current = qol().readText(listeningTextSettingId);
                if (!current.isEmpty()) {
                    int next = current.offsetByCodePoints(current.length(), -1);
                    qol().writeText(listeningTextSettingId, current.substring(0, next));
                    TrackerStore.save(config);
                }
                return true;
            }
            return true;
        }
        if (listeningKeybindSettingId == null || listeningKeybindSettingId.isBlank()) {
            return false;
        }
        if (glfwKey == GLFW.GLFW_KEY_ESCAPE) {
            finishKeybindCapture("");
            return true;
        }
        String name = QolKeybindNames.formatGlfwKey(glfwKey);
        if (!name.isBlank()) {
            finishKeybindCapture(name);
        }
        return true;
    }

    boolean captureChar(int codePoint, boolean allowedChatCharacter) {
        if (enumSearchFocused
                && openEnumSettingId != null
                && !openEnumSettingId.isBlank()) {
            if (!allowedChatCharacter || codePoint < 32) {
                return true;
            }
            if (enumQuery.length() >= 32) {
                return true;
            }
            enumQuery += Character.toString(codePoint);
            return true;
        }
        if (!capturingText()) {
            return false;
        }
        if (!allowedChatCharacter || codePoint < 32) {
            return true;
        }
        String current = qol().readText(listeningTextSettingId);
        if (current.length() >= NameHiderPolicy.MAX_CUSTOM_LENGTH) {
            return true;
        }
        qol().writeText(listeningTextSettingId, current + Character.toString(codePoint));
        TrackerStore.save(config);
        return true;
    }

    private boolean captureMouseButton(int button) {
        if (listeningKeybindSettingId == null || listeningKeybindSettingId.isBlank()) {
            return false;
        }
        String name = QolKeybindNames.formatMouseButton(button);
        if (!name.isBlank()) {
            finishKeybindCapture(name);
        }
        return true;
    }

    private void finishKeybindCapture(String value) {
        if (qol().writeKeybind(listeningKeybindSettingId, value)) {
            TrackerStore.save(config);
        }
        listeningKeybindSettingId = "";
    }

    boolean mouseClicked(
            int button,
            double mouseX,
            double mouseY,
            int contentLeft,
            int contentTop,
            int contentRight,
            int contentBottom) {

        if (captureMouseButton(
                button)) {

            return true;
        }

        int mx =
                (int) Math.round(
                        mouseX);

        int my =
                (int) Math.round(
                        mouseY);

        int contentWidth =
                contentRight
                        - contentLeft;

        int listW =
                contentWidth;

        if (hudDrawerOpen()) {
            int drawerW =
                    QolUtilityUiMath.drawerWidth(
                            contentWidth);

            int drawerX =
                    contentRight
                            - drawerW;

            int drawerY =
                    contentTop;

            int drawerH =
                    contentBottom
                            - drawerY;

            if (RotClientUiDraw.inside(
                    mx,
                    my,
                    drawerX,
                    drawerY,
                    drawerW,
                    drawerH)) {

                if (handleScrollbarPress(
                        button,
                        mx,
                        my,
                        drawerScroll,
                        drawerX
                                + drawerW
                                - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                        drawerY + 52,
                        drawerY
                                + drawerH
                                - 10)) {

                    return true;
                }

                return handleDrawerClick(
                        button,
                        mx,
                        my,
                        drawerX,
                        drawerY,
                        drawerW,
                        drawerH);
            }

            if (button
                    == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

                closeDrawer();

                return true;
            }
        }

        int listTop =
                pageContentListTop(
                        contentTop);

        int scrollTrackTop =
                pageScrollTrackTop(
                        contentTop);

        int listBottom =
                contentBottom;

        int listRight =
                contentLeft
                        + listW;

        QolUtilityUiMath.PageFilter chipHit =
                QolUtilityUiMath.hitPageFilter(
                        mx,
                        my,
                        contentLeft,
                        contentTop);

        if (hudLayoutLanding
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && handleHudLayoutLandingClick(
                        mx,
                        my,
                        contentLeft,
                        listTop,
                        listW,
                        contentBottom)) {

            return true;
        }

        if (appearanceLanding
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            return handleAppearanceLandingClick(
                    mx,
                    my,
                    contentLeft,
                    contentTop,
                    listW,
                    contentBottom,
                    listTop);
        }

        if (chipHit != null
                && !hudLayoutLanding
                && !appearanceLanding
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            if (pageFilter
                    != chipHit) {

                pageFilter =
                        chipHit;

                listScroll.reset();
            }

            return true;
        }

        if (handleScrollbarPress(
                button,
                mx,
                my,
                listScroll,
                listRight
                        - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                scrollTrackTop,
                listBottom)) {

            return true;
        }

        if (hudLayoutLanding
                || appearanceLanding) {

            // Misses must not eat sidebar clicks or hit HUD & Display cards underneath.
            return false;
        }

        if (!RotClientUiDraw.inside(
                mx,
                my,
                contentLeft,
                listTop,
                listW,
                Math.max(
                        0,
                        listBottom
                                - listTop))) {

            return false;
        }

        int gridWidth =
                Math.max(
                        1,
                        listW
                                - RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                                - 4);

        int baseY =
                listTop
                        - listScroll.scrollPixels();

        java.util.List<QolUtilityCatalog.ModuleDef> modules =
                QolUtilityUiMath.filterPageModules(
                        QolUtilityCatalog.modulesOnGroupPage(
                                activePage),
                        pageFilter,
                        this::isEnabled);

        QolUtilityCatalog.ModuleDef accordionModule =
                moduleAccordionOpen()
                        ? moduleAccordionModule()
                        : null;

        int accordionVisibleHeight =
                accordionModule == null
                        ? 0
                        : moduleAccordionVisibleHeight(
                                accordionModule);

        String expandedModuleId =
                accordionModule == null
                        ? ""
                        : accordionModule.id();

        QolUtilityUiMath.PageLayout layout =
                QolUtilityUiMath.layoutPage(
                        modules,
                        contentLeft,
                        gridWidth,
                        expandedModuleId,
                        accordionVisibleHeight);

        if (accordionModule != null
                && moduleAccordionRowY(
                        layout,
                        accordionModule) < 0) {

            completeModuleAccordionClose();

            accordionModule =
                    null;

            accordionVisibleHeight =
                    0;

            layout =
                    QolUtilityUiMath.layoutPage(
                            modules,
                            contentLeft,
                            gridWidth,
                            "",
                            0);
        }

        for (QolUtilityUiMath.PlacedCard card
                : layout.cards()) {

            int cardY =
                    baseY
                            + card.y();

            boolean expandedCard =
                    accordionModule != null
                            && card.module() != null
                            && accordionModule.id()
                            .equals(
                                    card.module().id());

            int cardExtra =
                    expandedCard
                            ? accordionVisibleHeight
                            : 0;

            if (RotClientUiDraw.inside(
                    mx,
                    my,
                    card.x(),
                    cardY,
                    card.width(),
                    QolUtilityUiMath.CARD_HEIGHT)) {

                return handleCardClick(
                        button,
                        mx,
                        my,
                        card.x(),
                        cardY,
                        card.width(),
                        card.module());
            }

            if (expandedCard
                    && cardExtra > 0) {

                int bodyY =
                        cardY
                                + QolUtilityUiMath.CARD_HEIGHT;

                if (RotClientUiDraw.inside(
                        mx,
                        my,
                        card.x(),
                        bodyY,
                        card.width(),
                        cardExtra)) {

                    if (moduleAccordionClosing) {
                        return true;
                    }

                    moduleInlineBodyRendering =
                            true;

                    try {
                        /*
                         * Synthetic drawer geometry:
                         *
                         * handleDrawerClick normally starts its body 52 px
                         * below drawerY. Inline rendering starts 8 px below
                         * bodyY, therefore bodyY - 44 maps both coordinate
                         * systems exactly.
                         */
                        return handleDrawerClick(
                                button,
                                mx,
                                my,
                                card.x(),
                                bodyY - 44,
                                card.width(),
                                cardExtra + 54);

                    } finally {
                        moduleInlineBodyRendering =
                                false;
                    }
                }
            }
        }

        return false;
    }
    private boolean handleCardClick(
            int button,
            int mx,
            int my,
            int cardX,
            int cardY,
            int cardW,
            QolUtilityCatalog.ModuleDef module) {
        java.util.List<HudElementCatalog.HudPiece> hudPieces =
                HudElementCatalog.hudPieces(module);
        boolean hasHud = !hudPieces.isEmpty() && runtimeAvailable(module);
        boolean settingsHit = !module.settings().isEmpty()
                && QolUtilityUiMath.hitSettingsButton(
                        mx, my, cardX, cardY, cardW, QolUtilityUiMath.CARD_HEIGHT);
        boolean toggleHit = QolUtilityUiMath.hitModuleToggle(
                mx, my, cardX, cardY, QolUtilityUiMath.CARD_HEIGHT);
        boolean hudHit = hasHud && QolUtilityUiMath.hitHudControl(
                mx, my, cardX, cardY, QolUtilityUiMath.CARD_HEIGHT);
        QolUtilityUiMath.CardAction action = QolUtilityUiMath.moduleCardAction(
                button == GLFW.GLFW_MOUSE_BUTTON_LEFT,
                button == GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                toggleHit,
                hudHit,
                settingsHit,
                !module.settings().isEmpty(),
                module.toggleable() && runtimeAvailable(module),
                hasHud,
                false);
        if (action == QolUtilityUiMath.CardAction.OPEN_SETTINGS) {
            if (MiningTrackerCatalogPolicy.APPEARANCE.equals(module.id())) {
                openAppearanceLanding();
                return true;
            }
            toggleModuleAccordion(module.id());
            return true;
        }
        if (action == QolUtilityUiMath.CardAction.TOGGLE) {
            setEnabled(module, !isEnabled(module));
            return true;
        }
        if (action == QolUtilityUiMath.CardAction.OPEN_HUD_SETTINGS) {
            openHudSettings(module.id());
            return true;
        }

        /*
         * Profit Finder behavior: clicking the ordinary card surface also
         * expands/collapses it. Toggle and HUD controls above have already
         * consumed their clicks.
         */
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && !module.settings().isEmpty()) {

            if (MiningTrackerCatalogPolicy.APPEARANCE.equals(module.id())) {
                openAppearanceLanding();
            } else {
                toggleModuleAccordion(
                        module.id());
            }

            return true;
        }

        return true;
    }

    private boolean handleDrawerClick(
            int button,
            int mx,
            int my,
            int drawerX,
            int drawerY,
            int drawerW,
            int drawerH) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return true;
        }
        if (!moduleInlineBodyRendering && QolUtilityUiMath.hitClose(mx, my, drawerX, drawerY, drawerW)) {
            if (drawerKind == DrawerKind.MODULE) {
                requestModuleAccordionClose();
            } else {
                closeDrawer();
            }
            return true;
        }
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openModuleId);
        if (module == null) {
            return true;
        }
        int bodyTop = drawerY + 52;
        int bodyBottom = drawerY + drawerH - 10;
        if (my < bodyTop || my >= bodyBottom) {
            return true;
        }
        int rowY = bodyTop - drawerScroll.scrollPixels();
        int rowX = drawerX + 12;
        int rowW = drawerW - 24;

        if (hudDrawerShowsEnableRow(module)) {
            if (RotClientUiDraw.inside(
                    mx, my, rowX, rowY, rowW, QolUtilityUiMath.DRAWER_ROW_HEIGHT)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    setEnabled(module, !isEnabled(module));
                }
                return true;
            }
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 8;
        } else if (!moduleInlineBodyRendering && !hudDrawerOpen() && module.toggleable() && runtimeAvailable(module)) {
            if (RotClientUiDraw.inside(
                    mx, my, rowX, rowY, rowW, QolUtilityUiMath.DRAWER_ROW_HEIGHT)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    setEnabled(module, !isEnabled(module));
                }
                return true;
            }
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 8;
        } else if (module.wip() || !module.runtimeReady()) {
            rowY += QolUtilityUiMath.DRAWER_ROW_HEIGHT + 4;
        }

        String drawerSectionId = "";
        for (QolUtilityCatalog.SettingDef setting : currentDrawerSettings(module)) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                if (RotClientUiDraw.inside(
                        mx, my, rowX, rowY, rowW, QolUtilityUiMath.DRAWER_SECTION_HEIGHT)) {
                    if (!collapsedDrawerSections.add(setting.id())) {
                        collapsedDrawerSections.remove(setting.id());
                    }
                    return true;
                }
                drawerSectionId = setting.id();
                rowY += QolUtilityUiMath.DRAWER_SECTION_HEIGHT;
                continue;
            }
            double open = drawerSectionAmount(drawerSectionId);
            if (open <= 0.02D) {
                continue;
            }
            int rowHeight = QolUtilityUiMath.settingRowHeight(setting);
            int step = rowHeight + 4;
            int shown = drawerSectionId.isEmpty()
                    ? step
                    : RotClientEase.shownPixels(step, open);
            if (shown <= 0) {
                continue;
            }
            if (RotClientUiDraw.inside(
                    mx, my, rowX, rowY, rowW, Math.min(rowHeight, shown))) {
                if (runtimeAvailable(module)) {
                    if (setting.type() == QolUtilityCatalog.SettingType.KEYBIND) {
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                            if (qol().writeKeybind(setting.id(), "")) {
                                TrackerStore.save(config);
                            }
                            listeningKeybindSettingId = "";
                        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            listeningTextSettingId = "";
                            listeningKeybindSettingId = setting.id();
                        }
                    } else if (setting.type() == QolUtilityCatalog.SettingType.TEXT) {
                        if (CustomScoreboardPolicy.isListTextSetting(setting.id())) {
                            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                listeningTextSettingId = "";
                                openBoardListEditor(setting.id());
                            }
                        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                            if (qol().writeText(setting.id(), "")) {
                                TrackerStore.save(config);
                            }
                            listeningTextSettingId = "";
                        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            listeningKeybindSettingId = "";
                            listeningTextSettingId = setting.id();
                        }
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        if (setting.type() == QolUtilityCatalog.SettingType.ENUM) {
                            if (setting.id().equals(openEnumSettingId)) {
                                openEnumSettingId = "";
                                enumQuery = "";
                                enumSearchFocused = false;
                            } else {
                                openEnumSettingId = setting.id();
                                enumQuery = "";
                                enumSearchFocused = OverflowListPolicy.needsSearch(
                                        setting.enumOptions().size());
                            }
                        } else if (setting.type() == QolUtilityCatalog.SettingType.NUMBER) {
                            if (QolNumberSettings.usesSlider(setting.id())
                                    && QolUtilityUiMath.hitSlider(mx, my, rowX, rowY, rowW)) {
                                beginSliderDrag(setting.id(), rowX, rowW, mx);
                            } else {
                                boolean decrease = QolUtilityUiMath.hitNumberDecrease(
                                        mx, rowX, rowW);
                                boolean increase = QolUtilityUiMath.hitNumberIncrease(
                                        mx, rowX, rowW);
                                if ((decrease || increase)
                                        && qol().nudgeNumber(setting.id(), increase)) {
                                    TrackerStore.save(config);
                                }
                            }
                        } else {
                            activateSetting(module, setting);
                        }
                    }
                }
                return true;
            }
            rowY += shown;
            if (runtimeAvailable(module)
                    && setting.type() == QolUtilityCatalog.SettingType.ENUM
                    && setting.id().equals(openEnumSettingId)) {
                boolean search = OverflowListPolicy.needsSearch(setting.enumOptions().size());
                if (search) {
                    int searchShown = drawerSectionId.isEmpty()
                            ? QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT
                            : RotClientEase.shownPixels(
                                    QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT, open);
                    if (searchShown > 0
                            && RotClientUiDraw.inside(
                                    mx, my, rowX + 12, rowY, rowW - 12, searchShown)) {
                        enumSearchFocused = true;
                        return true;
                    }
                    rowY += Math.max(0, searchShown);
                }
                for (String option : visibleEnumOptions(setting)) {
                    int optionShown = drawerSectionId.isEmpty()
                            ? QolUtilityUiMath.ENUM_OPTION_HEIGHT
                            : RotClientEase.shownPixels(
                                    QolUtilityUiMath.ENUM_OPTION_HEIGHT, open);
                    if (optionShown <= 0) {
                        continue;
                    }
                    if (RotClientUiDraw.inside(
                            mx,
                            my,
                            rowX + 12,
                            rowY,
                            rowW - 12,
                            optionShown)) {
                        if (qol().writeEnum(setting.id(), option)) {
                            TrackerStore.save(config);
                        }
                        openEnumSettingId = "";
                        enumQuery = "";
                        enumSearchFocused = false;
                        return true;
                    }
                    rowY += optionShown;
                }
            }
        }

        if (hudDrawerOpen()) {
            rowY += 8;
            if (RotClientUiDraw.inside(
                    mx, my, rowX, rowY, rowW, QolUtilityUiMath.DRAWER_ROW_HEIGHT)) {
                if (!pendingHudStyleResetConfirm) {
                    pendingHudStyleResetConfirm = true;
                } else {
                    resetHudStyle(module);
                    pendingHudStyleResetConfirm = false;
                }
                return true;
            }
        } else if (!module.settings().isEmpty()
                && runtimeAvailable(module)) {
            rowY += 8;
            if (RotClientUiDraw.inside(
                    mx, my, rowX, rowY, rowW, QolUtilityUiMath.DRAWER_ROW_HEIGHT)) {
                if (!pendingModuleResetConfirm) {
                    pendingModuleResetConfirm = true;
                } else if (resetModuleToDefaults(module)) {
                    pendingModuleResetConfirm = false;
                    TrackerStore.save(config);
                }
                return true;
            }
        }
        return true;
    }

    private boolean resetModuleToDefaults(QolUtilityCatalog.ModuleDef module) {
        if (MiningTrackerConfigBridge.resetModule(config, module.id())) {
            TrackerStore.save(config);
            return true;
        }
        boolean reset = qol().resetModuleToDefaults(module.id());
        if ("qol.fullbright".equals(module.id())) {
            RotClientClient.resetLightingModule();
            reset = true;
        }
        if ("qol.auto_sprint".equals(module.id())
                || "qol.camera".equals(module.id())) {
            setEnabled(module, false);
            reset = true;
        }
        return reset;
    }

    private void activateSetting(
            QolUtilityCatalog.ModuleDef module,
            QolUtilityCatalog.SettingDef setting) {
        switch (setting.type()) {
            case TOGGLE, SQUARE -> {
                Boolean current = readDrawerBoolean(setting.id());
                writeDrawerBoolean(setting.id(), current == null || !current);
            }
            case COLOR -> openColorPicker(setting);
            case ACTION -> handleAction(setting.id());
            case ENUM, NUMBER, KEYBIND, TEXT, SECTION -> {
                // ENUM, NUMBER, KEYBIND, and TEXT have explicit controls.
            }
            default -> {
            }
        }
    }

    private void openBoardListEditor(String settingId) {
        Minecraft.getInstance().setScreen(new QolListEditorScreen(host, settingId));
    }

    private void handleAction(String settingId) {
        if (settingId == null) {
            return;
        }
        if (settingId.endsWith("_hud_editor")) {
            RotClientClient.openHudEditor(
                    host, HudElementCatalog.focusIdForHudEditorSetting(settingId));
            return;
        }
        if ("qol.custom_scoreboard.reset_appearance".equals(settingId)) {
            qol().extras().board().resetAppearance();
            TrackerStore.save(config);
            openBoardListEditor("qol.custom_scoreboard.appearance");
            return;
        }
        if ("qol.custom_scoreboard.reset_events".equals(settingId)) {
            qol().extras().board().resetEvents();
            TrackerStore.save(config);
            openBoardListEditor("qol.custom_scoreboard.event_priority");
            return;
        }
        if (host instanceof MiningUiScreen screen) {
            if ("qol.market_watch.open_dashboard".equals(settingId)) {
                screen.openMarketWatchPage();
                return;
            }
            if (MiningTrackerCatalogPolicy.TRACKER_OPEN_PAGE.equals(settingId)) {
                screen.openTrackedPage(DashboardModule.MINING_TRACKER);
                return;
            }
            if (MiningTrackerCatalogPolicy.POWDER_OPEN_PAGE.equals(settingId)) {
                screen.openTrackedPage(DashboardModule.POWDER_CHEST_TRACKER);
                return;
            }
            if (MiningTrackerCatalogPolicy.SESSION_OPEN_PAGE.equals(settingId)) {
                screen.openTrackedPage(DashboardModule.SESSION_ANALYTICS);
                return;
            }
            if (MiningTrackerCatalogPolicy.HISTORY_OPEN_PAGE.equals(settingId)) {
                screen.openTrackedPage(DashboardModule.SESSION_HISTORY);
                return;
            }
            if (AppearanceLandingPolicy.isAppearanceAction(settingId)) {
                screen.openAppearanceCustomizer(appearanceSection(settingId));
                return;
            }
        }
        if ("qol.auto_sell.add_defaults".equals(settingId)) {
            if (QolClientFlavorSupport.hooks().handleDashboardAction(settingId)) {
                TrackerStore.save(config);
            }
            return;
        }
        if ("qol.command_keybinds.open_sequence_editor".equals(settingId)) {
            Minecraft.getInstance().setScreen(new HotkeySequenceEditorScreen(host));
            return;
        }
        if ("qol.storage_overlay.open_item_search".equals(settingId)) {
            Minecraft.getInstance().setScreen(new ItemSearchScreen(host, ""));
            return;
        }
        if ("qol.dungeon_carry.open_manager".equals(settingId)) {
            Minecraft.getInstance().setScreen(new DungeonCarryManagerScreen(host));
            return;
        }
        if ("qol.slayer_carry.open_manager".equals(settingId)) {
            Minecraft.getInstance().setScreen(new SlayerCarryManagerScreen(host));
            return;
        }
        if ("qol.slayer_stats.reset_session".equals(settingId)) {
            SlayerRuntime.engine().resetStats(System.currentTimeMillis());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message(
                        "Slayer session reset. Fresh start, same sharp tools."));
            }
            return;
        }
        if ("qol.storage_overlay.clear_cache".equals(settingId) || "qol.storage_overlay.reload_pages".equals(settingId)) {
            if ("qol.storage_overlay.reload_pages".equals(settingId)) {
                StorageOverlayRuntime.requestReloadAll();
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    client.player.sendSystemMessage(RotClientChat.message(
                            "Reloading Ender Chests and Backpacks from the server."));
                }
                return;
            }
            StorageOverlayRuntime.clearObservedPages();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message(
                        "Storage previews cleared. Open a page to refresh it."));
            }
            return;
        }
        if ("qol.storage_overlay.clear_search".equals(settingId)) {
            qol().extras().storageOverlaySearchQuery = "";
            TrackerStore.save(config);
            return;
        }
        if (QolClientFlavorSupport.hooks().handleDashboardAction(settingId)) {
            TrackerStore.save(config);
            return;
        }
        if ("qol.dungeon_hud.reset_split_pbs".equals(settingId)) {
            DungeonRuntime.resetSplitPersonalBests();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message(
                        "Dungeon split personal bests cleared."));
            }
            return;
        }
        if ("qol.dungeon_hud.reset_kuudra_pbs".equals(settingId)) {
            DungeonRuntime.resetKuudraPersonalBests();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message(
                        "Kuudra split personal bests cleared."));
            }
            return;
        }
        if ("qol.dungeon_f7.reset_term_pbs".equals(settingId)) {
            DungeonRuntime.resetTerminalPersonalBests();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message(
                        "Goldor terminal personal bests cleared."));
            }
            return;
        }
        if ("qol.dungeon_f7.reset_predev_pb".equals(settingId)) {
            DungeonRuntime.resetPredevPersonalBest();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message(
                        "Predev personal best cleared."));
            }
            return;
        }
        if ("qol.slayer_drops.open_filter_editor".equals(settingId)) {
            Minecraft.getInstance().setScreen(new SlayerDropFilterScreen(host));
            return;
        }
        if ("qol.inventory_overlay.open_colors".equals(settingId)) {
            Minecraft.getInstance().setScreen(new InventoryChromeColorsScreen(host));
            return;
        }
        if ("qol.inventory_buttons.open_editor".equals(settingId)) {
            Minecraft.getInstance().setScreen(new InventoryButtonsEditorScreen(host));
            return;
        }
        if ("qol.inventory_buttons.save_preset".equals(settingId)) {
            var extras = qol().extras();
            extras.inventoryButtonsSavedPreset = copyInventoryButtons(extras.inventoryButtons);
            TrackerStore.save(config);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message("Inventory button preset saved."));
            }
            return;
        }
        if ("qol.inventory_buttons.load_preset".equals(settingId)) {
            var extras = qol().extras();
            Minecraft client = Minecraft.getInstance();
            if (extras.inventoryButtonsSavedPreset == null || extras.inventoryButtonsSavedPreset.isEmpty()) {
                if (client.player != null) {
                    client.player.sendSystemMessage(RotClientChat.message("No saved inventory button preset yet."));
                }
                return;
            }
            extras.inventoryButtons = copyInventoryButtons(extras.inventoryButtonsSavedPreset);
            TrackerStore.save(config);
            if (client.player != null) {
                client.player.sendSystemMessage(RotClientChat.message("Inventory button preset restored."));
            }
            return;
        }
        if ("qol.inventory_buttons.simple_preset".equals(settingId)
                || "qol.inventory_buttons.warps_preset".equals(settingId)) {
            java.util.List<InventoryButtonsPolicy.Button> preset =
                    settingId.endsWith("warps_preset")
                            ? InventoryButtonsPolicy.allWarpsPreset()
                            : InventoryButtonsPolicy.simplePreset();
            qol().extras().inventoryButtons = copyInventoryButtons(preset);
            TrackerStore.save(config);
        }
    }

    private static RotClientAppearanceScreen.Section appearanceSection(String actionId) {
        return switch (AppearanceLandingPolicy.sectionId(actionId)) {
            case "dashboard" -> RotClientAppearanceScreen.Section.DASHBOARD;
            case "colors" -> RotClientAppearanceScreen.Section.COLORS;
            case "background" -> RotClientAppearanceScreen.Section.BACKGROUND;
            case "charts" -> RotClientAppearanceScreen.Section.CHARTS;
            case "reset" -> RotClientAppearanceScreen.Section.RESET;
            default -> RotClientAppearanceScreen.Section.OVERVIEW;
        };
    }

    private void drawAppearanceLanding(
            GuiGraphicsExtractor graphics,
            Font font,
            int contentLeft,
            int listTop,
            int contentRight,
            int listBottom,
            int mouseX,
            int mouseY) {

        boolean backHover =
                AppearanceLandingPolicy.hitBack(
                        mouseX,
                        mouseY,
                        contentLeft,
                        listTop);

        RotClientUiDraw.text(
                graphics,
                font,
                "\u2190 Back",
                contentLeft,
                listTop + 4,
                backHover
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_MUTED,
                backHover);

        RotClientUiDraw.pageTitle(
                graphics,
                font,
                "Appearance",
                contentLeft + 80,
                listTop + 1);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Shape the dashboard, colors, background and data visuals.",
                contentLeft + 80,
                listTop + 16);

        int gridWidth =
                Math.max(
                        1,
                        contentRight
                                - contentLeft
                                - RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                                - 4);

        java.util.List<AppearanceLandingPolicy.Card> cards =
                AppearanceLandingPolicy.cards();

        for (int i = 0;
             i < cards.size();
             i++) {

            AppearanceLandingPolicy.Card card =
                    cards.get(i);

            int[] rect =
                    AppearanceLandingPolicy.cardRect(
                            i,
                            contentLeft,
                            listTop,
                            gridWidth);

            boolean hover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            rect[0],
                            rect[1],
                            rect[2],
                            rect[3]);

            float hoverAmount =
                    moduleCardHoverAmount(
                            "appearance:"
                                    + card.actionId(),
                            hover);

            RotClientUiDraw.drawInteractiveSurface(
                    graphics,
                    rect[0],
                    rect[1],
                    rect[2],
                    rect[3],
                    hoverAmount,
                    false,
                    accentColor(),
                    panelRadius());

            graphics.fill(
                    rect[0] + 1,
                    rect[1] + 10,
                    rect[0] + 4,
                    rect[1] + rect[3] - 10,
                    accentColor());

            String category =
                    switch (
                            AppearanceLandingPolicy.sectionId(
                                    card.actionId())) {

                        case "dashboard" ->
                                "INTERFACE";

                        case "colors" ->
                                "THEME";

                        case "background" ->
                                "CANVAS";

                        case "charts" ->
                                "DATA";

                        case "reset" ->
                                "DEFAULTS";

                        default ->
                                "VISUALS";
                    };

            RotClientUiDraw.text(
                    graphics,
                    font,
                    category,
                    rect[0] + 16,
                    rect[1] + 11,
                    RotClientTheme.VIOLET,
                    true);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    card.title(),
                    rect[0] + 16,
                    rect[1] + 27,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsizeAndHover(
                            font,
                            card.subtitle(),
                            Math.max(
                                    20,
                                    rect[2] - 32),
                            rect[0] + 16,
                            rect[1] + 41,
                            12),
                    rect[0] + 16,
                    rect[1] + 43);

            int buttonX =
                    rect[0]
                            + rect[2]
                            - QolUtilityUiMath.SETTINGS_BUTTON_WIDTH
                            - 14;

            int buttonY =
                    rect[1]
                            + rect[3]
                            - QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT
                            - 12;

            RotClientUiDraw.drawPremiumButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    buttonX,
                    buttonY,
                    QolUtilityUiMath.SETTINGS_BUTTON_WIDTH,
                    QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                    "Open",
                    false,
                    true);
        }
    }
    private void drawHudLayoutLanding(
            GuiGraphicsExtractor graphics,
            Font font,
            int contentLeft,
            int listTop,
            int contentRight,
            int listBottom,
            int mouseX,
            int mouseY) {

        int listW =
                contentRight - contentLeft;

        int gridWidth =
                Math.max(
                        1,
                        listW
                                - RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                                - 4);

        int headerBottom =
                listTop
                        + HudLayoutLandingPolicy.headerHeight();

        listContentHeight =
                HudLayoutLandingPolicy.contentHeight();

        listScroll.setBounds(
                listContentHeight,
                Math.max(
                        0,
                        listBottom - headerBottom));

        listScroll.advanceSeconds(
                RotClientUiClock.seconds());

        int scroll =
                listScroll.scrollPixels();

        boolean backHover =
                AppearanceLandingPolicy.hitBack(
                        mouseX,
                        mouseY,
                        contentLeft,
                        listTop);

        RotClientUiDraw.text(
                graphics,
                font,
                "\u2190 Back",
                contentLeft,
                listTop + 4,
                backHover
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_MUTED,
                backHover);

        RotClientUiDraw.pageTitle(
                graphics,
                font,
                HudLayoutLandingPolicy.TITLE,
                contentLeft + 80,
                listTop + 1);

        RotClientUiDraw.helpText(
                graphics,
                font,
                HudLayoutLandingPolicy.SUBTITLE,
                contentLeft + 80,
                listTop + 16);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                contentLeft,
                listTop
                        + HudLayoutLandingPolicy.EDITOR_TOP,
                168,
                HudLayoutLandingPolicy.EDITOR_HEIGHT,
                "Open layout editor",
                true,
                true);

        java.util.List<HudLayoutLandingPolicy.Row> allRows =
                HudLayoutLandingPolicy.rows();

        int enabledRows = 0;

        for (HudLayoutLandingPolicy.Row row
                : allRows) {

            if (hudLayoutRowOn(row)) {
                enabledRows++;
            }
        }

        RotClientUiDraw.helpText(
                graphics,
                font,
                enabledRows
                        + " of "
                        + allRows.size()
                        + " elements enabled",
                contentLeft + 180,
                listTop
                        + HudLayoutLandingPolicy.EDITOR_TOP
                        + 9);

        graphics.enableScissor(
                contentLeft,
                headerBottom,
                contentRight,
                listBottom);

        RotClientUiMotion.pushFractionalScroll(
                graphics,
                listScroll);

        try {
            int y =
                    headerBottom
                            + HudLayoutLandingPolicy.SECTION_GAP
                            - scroll;

            for (HudLayoutLandingPolicy.Section section
                    : HudLayoutLandingPolicy.sections()) {

                String sectionTitle =
                        section.title()
                                + "  \u00B7  "
                                + section.rows().size();

                RotClientUiDraw.text(
                        graphics,
                        font,
                        sectionTitle,
                        contentLeft + 2,
                        y + 2,
                        RotClientTheme.TEXT_DIM,
                        true);

                y +=
                        HudLayoutLandingPolicy.SECTION_LABEL_HEIGHT;

                for (HudLayoutLandingPolicy.Row row
                        : section.rows()) {

                    boolean on =
                            hudLayoutRowOn(
                                    row);

                    boolean hover =
                            RotClientUiDraw.inside(
                                    mouseX,
                                    mouseY,
                                    contentLeft,
                                    y,
                                    gridWidth,
                                    HudLayoutLandingPolicy.ROW_HEIGHT);

                    float hoverAmount =
                            moduleCardHoverAmount(
                                    "hud:"
                                            + row.settingId(),
                                    hover);

                    RotClientUiDraw.drawInteractiveSurface(
                            graphics,
                            contentLeft,
                            y,
                            gridWidth,
                            HudLayoutLandingPolicy.ROW_HEIGHT,
                            hoverAmount,
                            false,
                            on
                                    ? accentColor()
                                    : RotClientTheme.BORDER_BRIGHT,
                            panelRadius());

                    if (on) {
                        graphics.fill(
                                contentLeft + 1,
                                y + 8,
                                contentLeft + 4,
                                y
                                        + HudLayoutLandingPolicy.ROW_HEIGHT
                                        - 8,
                                accentColor());
                    }

                    RotClientUiDraw.text(
                            graphics,
                            font,
                            row.label(),
                            contentLeft + 12,
                            y + 8,
                            on
                                    ? RotClientTheme.TEXT
                                    : RotClientTheme.TEXT_DIM,
                            true);

                    RotClientUiDraw.text(
                            graphics,
                            font,
                            RotClientUiDraw.ellipsizeAndHover(
                                    font,
                                    row.description(),
                                    Math.max(
                                            40,
                                            gridWidth - 170),
                                    contentLeft + 12,
                                    y + 24,
                                    12),
                            contentLeft + 12,
                            y + 26,
                            RotClientTheme.TEXT_MUTED,
                            false);

                    int toggleX =
                            contentLeft
                                    + gridWidth
                                    - HudLayoutLandingPolicy.TOGGLE_WIDTH
                                    - 12;

                    if (row.hasSettings()) {

                        int settingsX =
                                toggleX
                                        - HudLayoutLandingPolicy.SETTINGS_WIDTH
                                        - 8;

                        RotClientUiDraw.drawPremiumButton(
                                graphics,
                                font,
                                mouseX,
                                mouseY,
                                settingsX,
                                y + 12,
                                HudLayoutLandingPolicy.SETTINGS_WIDTH,
                                QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                                "Settings",
                                false,
                                true);
                    }

                    RotClientUiDraw.drawAnimatedToggle(
                            graphics,
                            toggleX,
                            y + 16,
                            on,
                            hover,
                            "hud-layout-toggle:"
                                    + row.settingId(),
                            accentColor());

                    y +=
                            HudLayoutLandingPolicy.ROW_HEIGHT
                                    + HudLayoutLandingPolicy.ROW_GAP;
                }

                y += 8;
            }

        } finally {
            RotClientUiMotion.pop(
                    graphics);

            graphics.disableScissor();
        }

        if (listScroll.canScroll()) {
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    contentRight
                            - RotClientUiDraw.SCROLLBAR_WIDTH
                            - 2,
                    headerBottom,
                    listBottom,
                    listContentHeight,
                    scroll);
        }
    }
    private boolean handleHudLayoutLandingClick(
            int mx,
            int my,
            int contentLeft,
            int listTop,
            int listW,
            int contentBottom) {
        int gridWidth = Math.max(1, listW - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4);
        HudLayoutLandingPolicy.Hit hit = HudLayoutLandingPolicy.hit(
                mx, my, contentLeft, listTop, gridWidth, listScroll.scrollPixels());
        if (hit.action() == HudLayoutLandingPolicy.Action.BACK) {
            return popHostDashboard();
        }
        if (hit.action() == HudLayoutLandingPolicy.Action.OPEN_EDITOR) {
            RotClientClient.openHudEditor(host);
            return true;
        }
        if (hit.action() == HudLayoutLandingPolicy.Action.TOGGLE
                || hit.action() == HudLayoutLandingPolicy.Action.SETTINGS) {
            java.util.List<HudLayoutLandingPolicy.Row> rows = HudLayoutLandingPolicy.rows();
            if (hit.rowIndex() < 0 || hit.rowIndex() >= rows.size()) {
                return true;
            }
            HudLayoutLandingPolicy.Row row = rows.get(hit.rowIndex());
            if (hit.action() == HudLayoutLandingPolicy.Action.SETTINGS && row.hasSettings()) {
                openHudSettings(row.settingsModuleId());
                return true;
            }
            if (MiningTrackerCatalogPolicy.TRACKER.equals(row.settingId())) {
                RotClientClient.setTrackerEnabled(!config.enabled);
                return true;
            }
            if (MiningTrackerCatalogPolicy.POWDER_HUD.equals(row.settingId())) {
                RotClientClient.setPowderChestHudEnabled(!config.powderChestHudEnabled);
                return true;
            }
            HudLayerTogglePolicy.toggle(qol(), row.settingId(), row.moduleToggle());
            if ("qol.wardrobe_keybinds".equals(row.settingId())
                    && !HudLayerTogglePolicy.isOn(qol(), row.settingId(), true)) {
                QolClientFlavorSupport.hooks().disablePlusWardrobe();
            }
            TrackerStore.save(config);
            return true;
        }
        return false;
    }

    private boolean hudLayoutRowOn(HudLayoutLandingPolicy.Row row) {
        if (MiningTrackerCatalogPolicy.TRACKER.equals(row.settingId())) {
            return config.enabled;
        }
        if (MiningTrackerCatalogPolicy.POWDER_HUD.equals(row.settingId())) {
            return config.powderChestHudEnabled;
        }
        if ("qol.wardrobe_keybinds".equals(row.settingId())) {
            return HudLayerTogglePolicy.isOn(qol(), row.settingId(), true)
                    || QolClientFlavorSupport.hooks().plusWardrobeEnabled();
        }
        return HudLayerTogglePolicy.isOn(qol(), row.settingId(), row.moduleToggle());
    }

    private boolean popHostDashboard() {
        if (host instanceof MiningUiScreen screen) {
            screen.navigateDashboardBack();
            return true;
        }
        closeLandings();
        return true;
    }

    private boolean handleAppearanceLandingClick(
            int mx,
            int my,
            int contentLeft,
            int contentTop,
            int listW,
            int contentBottom,
            int listTop) {
        int gridWidth = Math.max(1, listW - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4);
        AppearanceLandingPolicy.LandingHit kind = AppearanceLandingPolicy.hitKind(
                mx, my, contentLeft, contentTop, listW, contentBottom, listTop, gridWidth);
        if (kind == AppearanceLandingPolicy.LandingHit.CARD) {
            int hit = AppearanceLandingPolicy.hitIndex(mx, my, contentLeft, listTop, gridWidth);
            if (hit >= 0) {
                handleAction(AppearanceLandingPolicy.cards().get(hit).actionId());
            }
            return true;
        }
        if (kind == AppearanceLandingPolicy.LandingHit.OUTSIDE) {
            popHostDashboard();
            return false;
        }
        return popHostDashboard();
    }

    private void closeAppearanceLanding() {
        if (!appearanceLanding) {
            return;
        }
        appearanceLanding = false;
        persistWorkspaceView();
    }

    private static java.util.ArrayList<InventoryButtonsPolicy.Button> copyInventoryButtons(
            java.util.List<InventoryButtonsPolicy.Button> source) {
        java.util.ArrayList<InventoryButtonsPolicy.Button> copy = new java.util.ArrayList<>();
        if (source != null) {
            for (InventoryButtonsPolicy.Button button : source) {
                if (button != null) copy.add(button.copy());
            }
        }
        return copy;
    }

    private void openColorPicker(QolUtilityCatalog.SettingDef setting) {
        Integer current = readDrawerColor(setting.id());
        if (current == null) {
            current = RotClientTheme.HUD_ACCENT;
        }
        pendingColorSettingId = setting.id();
        int initial = current;
        Minecraft.getInstance().setScreen(
                new RotClientColorPickerScreen(
                        host,
                        setting.label(),
                        initial,
                        color -> writeDrawerColor(pendingColorSettingId, color)));
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double verticalAmount,
            int contentLeft,
            int contentTop,
            int contentRight,
            int contentBottom) {
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        int contentWidth = contentRight - contentLeft;
        boolean drawerOpen = isDrawerOpen();
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);

        if (drawerOpen && drawerKind == DrawerKind.HUD) {
            int drawerX = contentRight - drawerW;
            int drawerY = contentTop;
            int drawerH = contentBottom - drawerY;
            if (RotClientUiDraw.inside(mx, my, drawerX, drawerY, drawerW, drawerH)) {
                if (!drawerScroll.canScroll()) {
                    return true;
                }
                drawerScroll.scrollBySteps(verticalAmount, 40);
                return true;
            }
            return true;
        }

        int listTop = pageScrollTrackTop(contentTop);
        if (RotClientUiDraw.inside(
                mx, my, contentLeft, listTop, contentWidth, contentBottom - listTop)) {
            if (!listScroll.canScroll()) {
                return false;
            }
            listScroll.scrollBySteps(verticalAmount, 40);
            return true;
        }
        return false;
    }

    boolean mouseDragged(
            double mouseX,
            double mouseY,
            int contentTop,
            int contentBottom) {
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        if (draggingNumberSettingId != null && !draggingNumberSettingId.isBlank()) {
            applySliderDrag(mx);
            return true;
        }
        if (drawerScroll.dragThumbTo(
                my,
                contentTop + 52,
                contentBottom - 10,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {
            return true;
        }
        int listTop = pageScrollTrackTop(contentTop);
        return listScroll.dragThumbTo(
                my,
                listTop,
                contentBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT);
    }

    boolean mouseReleased() {
        boolean sliderReleased = false;
        if (draggingNumberSettingId != null && !draggingNumberSettingId.isBlank()) {
            draggingNumberSettingId = "";
            TrackerStore.save(config);
            sliderReleased = true;
        }
        boolean drawerReleased = drawerScroll.endThumbDrag();
        boolean listReleased = listScroll.endThumbDrag();
        return sliderReleased || drawerReleased || listReleased;
    }

    private void beginSliderDrag(String settingId, int rowX, int rowWidth, int mouseX) {
        draggingNumberSettingId = settingId;
        draggingSliderRowX = rowX;
        draggingSliderRowWidth = rowWidth;
        applySliderDrag(mouseX);
    }

    private void applySliderDrag(int mouseX) {
        QolNumberSettings.Spec spec = QolNumberSettings.spec(draggingNumberSettingId);
        if (spec == null) {
            return;
        }
        double next = spec.fromFraction(
                QolUtilityUiMath.sliderFraction(
                        mouseX, draggingSliderRowX, draggingSliderRowWidth));
        writeDrawerNumber(draggingNumberSettingId, next);
    }

    private static boolean handleScrollbarPress(
            int button,
            int mouseX,
            int mouseY,
            RotClientScrollState scroll,
            int hitX,
            int trackTop,
            int trackBottom) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || !scroll.canScroll()
                || !scrollbarHovered(mouseX, mouseY, hitX, trackTop, trackBottom)) {
            return false;
        }
        if (scroll.beginThumbDrag(
                mouseY,
                trackTop,
                trackBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {
            return true;
        }
        return scroll.clickTrack(
                mouseY,
                trackTop,
                trackBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT);
    }

    private static boolean scrollbarHovered(
            int mouseX,
            int mouseY,
            int hitX,
            int trackTop,
            int trackBottom) {
        return mouseX >= hitX
                && mouseX < hitX + RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                && mouseY >= trackTop
                && mouseY < trackBottom;
    }

    String statusLine() {
        if (capturingKeybind()) {
            return "Press a key…  ·  ESC clears";
        }
        List<String> on = new ArrayList<>();
        if (config.fullbrightEnabled && config.alwaysNightEnabled) {
            on.add("FB+NIGHT");
        } else if (config.fullbrightEnabled) {
            on.add("FB");
        } else if (config.alwaysNightEnabled) {
            on.add("NIGHT");
        }
        if (config.autoSprintEnabled) {
            on.add("SPRINT");
        }
        if (config.cameraEnabled) {
            on.add("CAM");
        }
        if (qol().performanceHudEnabled) {
            on.add("PERF");
        }
        if (qol().renderOptimizerEnabled) {
            on.add("RENDER");
        }
        if (on.isEmpty()) {
            return "UTILITIES  ·  OFF";
        }
        if (on.size() == 1) {
            return on.get(0) + "  ·  ON";
        }
        return on.size() + " UTILITIES  ·  ON";
    }
}
