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
        if (tab.qolGroup != null && !tab.qolGroup.isBlank()) {
            activePage = QolUtilityCatalog.Group.fromId(tab.qolGroup);
        }
        if (tab.qolModuleId != null && !tab.qolModuleId.isBlank()) {
            if (MiningTrackerCatalogPolicy.APPEARANCE.equals(tab.qolModuleId)) {
                openAppearanceLanding();
            } else if (MiningTrackerCatalogPolicy.HUD_LAYOUT.equals(tab.qolModuleId)) {
                openHudLayoutLanding();
            } else {
                openModule(tab.qolModuleId);
            }
        }
    }

    void persistWorkspaceView() {
        RotClientWorkspace workspace = RotClientClient.workspace();
        if (workspace == null) {
            return;
        }
        workspace.setQolView(
                activePage.name(),
                appearanceLanding
                        ? MiningTrackerCatalogPolicy.APPEARANCE
                        : (hudLayoutLanding
                                ? MiningTrackerCatalogPolicy.HUD_LAYOUT
                                : (isDrawerOpen() ? openModuleId : "")));
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

    private boolean hudDrawerOpen() {
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
        int contentWidth = contentRight - contentLeft;
        boolean drawerOpen = isDrawerOpen();
        boolean enumOpen = openEnumSettingId != null && !openEnumSettingId.isBlank();
        boolean modal = drawerOpen || enumOpen;
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);
        boolean overlay = drawerOpen;
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
            if (drawerOpen) {
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
        QolUtilityUiMath.PageLayout layout = QolUtilityUiMath.layoutPage(
                modules, contentLeft, gridWidth);
        listContentHeight = layout.height();
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
                int y = baseY + header.y();
                if (y + QolUtilityUiMath.GROUP_HEADER_HEIGHT < listTop
                        || y > listBottom) {
                    continue;
                }
                RotClientUiDraw.text(graphics, font,
                        header.title(),
                        header.x(),
                        y + 6,
                        RotClientTheme.TEXT_MUTED,
                        true);
            }
            for (QolUtilityUiMath.PlacedCard card : layout.cards()) {
                int y = baseY + card.y();
                if (y + QolUtilityUiMath.CARD_HEIGHT >= listTop
                        && y <= listBottom) {
                    String tip = drawModuleCard(
                            graphics,
                            font,
                            card.x(),
                            y,
                            card.width(),
                            card.module(),
                            listMouseX,
                            listMouseY);
                    if (tip != null) {
                        hoverTip = tip;
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

        if (drawerOpen) {
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
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.PAGE_HEADER_HEIGHT,
                RotClientTheme.DASHBOARD_HEADER,
                panelRadius());
        graphics.fill(
                x,
                y + 6,
                x + 3,
                y + 38,
                accentColor());
        RotClientUiDraw.text(graphics, font, activePage.title(), x + 12, y + 8, RotClientTheme.TEXT, true);
        List<QolUtilityCatalog.ModuleDef> pageModules =
                QolUtilityCatalog.modulesOnGroupPage(activePage);
        int enabledCount = 0;
        int cheatCount = 0;
        for (QolUtilityCatalog.ModuleDef module : pageModules) {
            if (runtimeAvailable(module) && isEnabled(module)) {
                enabledCount++;
            }
            if (QolUtilityCatalog.hasCheatTag(module)) {
                cheatCount++;
            }
        }
        String countLabel = enabledCount + " / " + pageModules.size() + " on";
        boolean overlayDrawer = isDrawerOpen();
        if (!overlayDrawer) {
            RotClientUiDraw.text(graphics, font,
                    countLabel,
                    x + width - 12 - font.width(countLabel),
                    y + 8,
                    RotClientTheme.TEXT_MUTED,
                    false);
        }
        String desc = activePage.pageDescription();
        int descMax = Math.max(40, width - 24);
        RotClientUiDraw.text(graphics, font,
                RotClientUiDraw.ellipsizeAndHover(font, desc, descMax, x + 12, y + 22, 12),
                x + 12,
                y + 24,
                RotClientTheme.TEXT_DIM,
                false);
        this.headerHoverTip = null;
        for (QolUtilityUiMath.FilterChip chip : QolUtilityUiMath.pageFilterChips(x, y)) {
            boolean selected = chip.filter() == pageFilter;
            boolean hover = RotClientUiDraw.inside(
                    mouseX, mouseY, chip.x(), chip.y(), chip.width(), chip.height());
            int fill = selected
                    ? RotClientTheme.VIOLET
                    : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE);
            RotClientUiDraw.roundedFill(
                    graphics,
                    chip.x(),
                    chip.y(),
                    chip.x() + chip.width(),
                    chip.y() + chip.height(),
                    fill,
                    4);
            String chipLabel = chip.label();
            if (chip.filter() == QolUtilityUiMath.PageFilter.CHEAT) {
                chipLabel = QolUtilityUiMath.cheatFilterLabel(cheatCount);
            }
            int labelX = chip.x() + (chip.width() - font.width(chipLabel)) / 2;
            RotClientUiDraw.text(graphics, font,
                    chipLabel,
                    labelX,
                    chip.y() + 5,
                    selected ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM,
                    selected);
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

    private String drawModuleCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            QolUtilityCatalog.ModuleDef module,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.CARD_HEIGHT);
        boolean enabled = isEnabled(module);
        boolean selected = module.id().equals(openModuleId);
        int fill = selected
                ? RotClientTheme.SELECTED_ROW
                : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT);
        RotClientUiDraw.roundedFill(
                graphics, x, y, x + width, y + QolUtilityUiMath.CARD_HEIGHT, fill, panelRadius());
        int outline = selected
                ? RotClientTheme.VIOLET
                : (enabled && runtimeAvailable(module)
                        ? accentColor()
                        : RotClientTheme.BORDER);
        RotClientUiDraw.roundedOutline(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.CARD_HEIGHT,
                outline,
                panelRadius());
        graphics.fill(
                x,
                y + 8,
                x + 4,
                y + QolUtilityUiMath.CARD_HEIGHT - 8,
                enabled && runtimeAvailable(module)
                        ? accentColor()
                        : RotClientTheme.DIVIDER);

        QolModuleEvidence.Status evidence = module.evidenceStatus();
        String statusLabel = evidence.label();
        int statusColor = evidenceColor(evidence);
        int statusX = x + QolUtilityUiMath.STATUS_BADGE_LEFT;
        int statusY = y + QolUtilityUiMath.STATUS_BADGE_TOP;
        int statusWidth = QolUtilityUiMath.statusBadgeWidth(font.width(statusLabel));
        RotClientUiDraw.roundedFill(
                graphics,
                statusX,
                statusY,
                statusX + statusWidth,
                statusY + QolUtilityUiMath.STATUS_BADGE_HEIGHT,
                RotClientUiDraw.withAlpha(statusColor, 0x34),
                4);
        RotClientUiDraw.roundedOutline(
                graphics,
                statusX,
                statusY,
                statusX + statusWidth,
                statusY + QolUtilityUiMath.STATUS_BADGE_HEIGHT,
                statusColor,
                4);
        RotClientUiDraw.text(
                graphics, font, statusLabel,
                statusX + QolUtilityUiMath.STATUS_BADGE_PAD_X,
                statusY + 3,
                statusColor,
                false);

        String title = module.name();
        boolean cheat = QolUtilityCatalog.hasCheatTag(module);
        int titleMax = Math.max(40, width - (cheat ? 88 : 36));
        String shownTitle = RotClientUiDraw.ellipsizeAndHover(
                font, title, titleMax, x + 16, y + 23, 12);
        RotClientUiDraw.text(graphics, font,
                shownTitle,
                x + 16,
                y + 25,
                runtimeAvailable(module) ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                true);
        String hoverTip = null;
        if (cheat) {
            RotClientUiDraw.text(graphics, font,
                    "CHEAT",
                    x + 16 + font.width(shownTitle) + QolUtilityUiMath.CHEAT_BADGE_GAP,
                    y + 25,
                    RotClientTheme.WARNING,
                    true);
        }

        int descMax = Math.max(40, width - 32);
        String explanation = HudElementCatalog.explainedDescription(module);
        String shownDesc = RotClientUiDraw.ellipsizeAndHover(
                font, explanation, descMax, x + 16, y + 43, 12);
        RotClientUiDraw.text(graphics, font,
                shownDesc,
                x + 16,
                y + 45,
                RotClientTheme.TEXT_DIM,
                false);
        if (RotClientUiDraw.truncated(font, explanation, descMax)
                && RotClientUiDraw.inside(mouseX, mouseY, x + 16, y + 43, descMax, 12)) {
            hoverTip = explanation;
        }

        int footerY = QolUtilityUiMath.cardFooterY(y, QolUtilityUiMath.CARD_HEIGHT);
        graphics.fill(x + 10, footerY - 2, x + width - 10, footerY - 1, RotClientTheme.DIVIDER);

        if (runtimeAvailable(module) && module.toggleable()) {
            int toggleX = QolUtilityUiMath.moduleToggleX(x);
            int toggleY = QolUtilityUiMath.moduleToggleY(y, QolUtilityUiMath.CARD_HEIGHT);
            boolean toggleHover = QolUtilityUiMath.hitModuleToggle(
                    mouseX, mouseY, x, y, QolUtilityUiMath.CARD_HEIGHT);
            RotClientUiDraw.text(graphics, font,
                    "Module",
                    toggleX,
                    footerY - 12,
                    RotClientTheme.TEXT_MUTED,
                    false);
            RotClientUiDraw.drawToggle(graphics, toggleX, toggleY, enabled, toggleHover);
            String onOff = enabled ? "ON" : "OFF";
            RotClientUiDraw.text(graphics, font,
                    onOff,
                    toggleX + QolUtilityUiMath.TOGGLE_WIDTH + 6,
                    toggleY + 3,
                    enabled ? accentColor() : RotClientTheme.TEXT_MUTED,
                    true);
        } else if (module.wip()) {
            RotClientUiDraw.text(graphics, font,
                    "Coming later — preview only",
                    x + 16,
                    footerY + 6,
                    RotClientTheme.WARNING,
                    false);
        } else if (!module.runtimeReady()) {
            RotClientUiDraw.text(graphics, font,
                    "Planned — preview only",
                    x + 16,
                    footerY + 6,
                    RotClientTheme.WARNING,
                    false);
        }

        java.util.List<HudElementCatalog.HudPiece> hudPieces = HudElementCatalog.hudPieces(module);
        if (!hudPieces.isEmpty() && runtimeAvailable(module)) {
            int hudX = QolUtilityUiMath.hudControlX(x);
            int hudY = QolUtilityUiMath.hudControlY(y, QolUtilityUiMath.CARD_HEIGHT);
            RotClientUiDraw.drawButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    hudX,
                    hudY,
                    QolUtilityUiMath.HUD_CONTROL_WIDTH,
                    QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                    "HUD",
                    false,
                    true);
        }

        if (!module.settings().isEmpty()) {
            int buttonX = x + width - QolUtilityUiMath.CARD_PAD
                    - QolUtilityUiMath.SETTINGS_BUTTON_WIDTH;
            int buttonY = QolUtilityUiMath.cardFooterY(y, QolUtilityUiMath.CARD_HEIGHT)
                    + Math.max(0, (QolUtilityUiMath.FOOTER_HEIGHT
                    - QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT) / 2);
            RotClientUiDraw.drawButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    buttonX,
                    buttonY,
                    QolUtilityUiMath.SETTINGS_BUTTON_WIDTH,
                    QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                    "Settings",
                    false,
                    true);
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
        tickDrawerExpand(module);
        if (overlay) {
            graphics.fill(x - 8, y, x, y + height, 0x66000000);
        }
        RotClientUiDraw.roundedFill(
                graphics, x, y, x + width, y + height, RotClientTheme.SURFACE);
        graphics.fill(x, y + 8, x + 3, y + 40, RotClientTheme.HUD_ACCENT);
        String drawerTitle = hudDrawerTitle(module);
        RotClientUiDraw.text(graphics, font, drawerTitle, x + 14, y + 12, RotClientTheme.TEXT, true);
        if (QolUtilityCatalog.hasCheatTag(module) && !hudDrawerOpen()) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    "CHEAT",
                    x + 14 + font.width(drawerTitle) + QolUtilityUiMath.CHEAT_BADGE_GAP,
                    y + 12,
                    RotClientTheme.WARNING,
                    true);
        }
        int descMax = Math.max(40, width - 50);
        String explanation = hudDrawerOpen()
                ? "Turn this overlay on, change its look, and open layout edit to move it."
                : HudElementCatalog.explainedDescription(module);
        String shownDesc = RotClientUiDraw.ellipsizeAndHover(
                font, explanation, descMax, x + 14, y + 26, 12);
        RotClientUiDraw.text(graphics, font,
                shownDesc,
                x + 14,
                y + 28,
                RotClientTheme.TEXT_DIM,
                false);
        if (RotClientUiDraw.truncated(font, explanation, descMax)
                && RotClientUiDraw.inside(mouseX, mouseY, x + 14, y + 26, descMax, 12)) {
            this.headerHoverTip = explanation;
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

        int bodyTop = y + 52;
        int bodyBottom = y + height - 10;
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
        } else if (!hudDrawerOpen() && module.toggleable() && runtimeAvailable(module)) {
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
        if (drawerScroll.canScroll()) {
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
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.DRAWER_SECTION_HEIGHT);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.DRAWER_SECTION_HEIGHT,
                hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.drawChevron(
                graphics,
                x + 6,
                y + 6,
                openAmount,
                hover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED);
        RotClientUiDraw.glyph(
                graphics,
                font,
                label,
                x + 22,
                y + 7,
                RotClientTheme.TEXT,
                true);
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
        boolean hover = interactive && RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, QolUtilityUiMath.DRAWER_ROW_HEIGHT);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + QolUtilityUiMath.DRAWER_ROW_HEIGHT,
                hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT);
        RotClientUiDraw.text(graphics, font, label, x + 8, y + 6, RotClientTheme.TEXT, false);
        String shownHint = RotClientUiDraw.ellipsizeAndHover(
                font,
                hint,
                Math.max(40, width - QolUtilityUiMath.DRAWER_CONTROL_RESERVE - 16),
                x + 8,
                y + 22,
                12);
        RotClientUiDraw.text(graphics, font,
                shownHint,
                x + 8,
                y + 24,
                enabled ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_MUTED,
                false);
        RotClientUiDraw.drawToggle(
                graphics,
                x + width - QolUtilityUiMath.DRAWER_CONTROL_RESERVE + 8,
                y + 12,
                enabled,
                hover);
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
        boolean disabled = !runtimeAvailable(module);
        int rowHeight = QolUtilityUiMath.settingRowHeight(setting);
        boolean hover = !disabled && RotClientUiDraw.inside(
                mouseX, mouseY, x, y, width, rowHeight);
        if (hover && setting.description() != null && !setting.description().isBlank()) {
            this.headerHoverTip = HudElementCatalog.explainedSetting(setting);
        }
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + rowHeight,
                hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT);
        String trailing = settingTrailing(setting);
        int trailingColor = disabled ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT_DIM;
        String label = RotClientUiDraw.ellipsizeAndHover(
                font,
                setting.label(),
                QolUtilityUiMath.settingLabelMaxWidth(width, setting.type()),
                x + 8,
                y + 4,
                12);
        RotClientUiDraw.text(graphics, font,
                label,
                x + 8,
                y + 6,
                disabled ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT,
                false);
        if (setting.type() == QolUtilityCatalog.SettingType.TOGGLE) {
            Boolean value = readDrawerBoolean(setting.id());
            RotClientUiDraw.drawToggle(
                    graphics,
                    x + width - QolUtilityUiMath.DRAWER_CONTROL_RESERVE + 8,
                    y + 12,
                    value != null && value && !disabled,
                    hover);
        } else if (setting.type() == QolUtilityCatalog.SettingType.SQUARE) {
            Boolean value = readDrawerBoolean(setting.id());
            RotClientUiDraw.drawSquareLatch(
                    graphics,
                    x + width - 28,
                    y + 12,
                    value != null && value && !disabled,
                    hover);
        } else if (setting.type() == QolUtilityCatalog.SettingType.COLOR) {
            Integer color = readDrawerColor(setting.id());
            int swatch = color == null ? RotClientTheme.HUD_ACCENT : color;
            graphics.fill(x + width - 28, y + 7, x + width - 10, y + 21, swatch);
        } else if (setting.type() == QolUtilityCatalog.SettingType.NUMBER) {
            if (QolNumberSettings.usesSlider(setting.id())) {
                drawNumberSlider(graphics, font, x, y, width, trailing, setting.id(), disabled);
            } else {
                drawNumberStepper(graphics, font, x, y, width, trailing, disabled);
            }
        } else if (setting.type() == QolUtilityCatalog.SettingType.ENUM) {
            drawEnumControl(
                    graphics,
                    font,
                    x,
                    y,
                    width,
                    trailing,
                    disabled,
                    setting.id().equals(openEnumSettingId));
        } else if (setting.type() == QolUtilityCatalog.SettingType.ACTION) {
            RotClientUiDraw.drawButton(
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
        } else {
            RotClientUiDraw.text(graphics, font,
                    trailing,
                    x + width - 10 - font.width(trailing),
                    y + 9,
                    trailingColor,
                    false);
        }
    }

    private void drawNumberSlider(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String value,
            String settingId,
            boolean disabled) {
        int color = disabled ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT;
        RotClientUiDraw.text(graphics, font,
                value,
                x + width - 10 - font.width(value),
                y + 6,
                color,
                false);
        int trackX = x + QolUtilityUiMath.SLIDER_TRACK_INSET;
        int trackW = Math.max(1, width - QolUtilityUiMath.SLIDER_TRACK_INSET * 2);
        int trackY = y + 24;
        int trackBottom = trackY + QolUtilityUiMath.SLIDER_TRACK_HEIGHT;
        RotClientUiDraw.roundedFill(
                graphics,
                trackX,
                trackY,
                trackX + trackW,
                trackBottom,
                RotClientTheme.FIELD,
                RotClientUiDraw.RADIUS_SM);
        Double raw = readDrawerNumber(settingId);
        QolNumberSettings.Spec spec = QolNumberSettings.spec(settingId);
        double fraction = spec == null || raw == null ? 0.0D : spec.fraction(raw);
        int fillW = Math.max(0, (int) Math.round(fraction * trackW));
        if (fillW > 0) {
            RotClientUiDraw.roundedFill(
                    graphics,
                    trackX,
                    trackY,
                    trackX + fillW,
                    trackBottom,
                    disabled ? RotClientTheme.BUTTON_DISABLED : RotClientTheme.HUD_ACCENT,
                    RotClientUiDraw.RADIUS_SM);
        }
        int thumb = QolUtilityUiMath.SLIDER_THUMB_SIZE;
        int thumbX = trackX + Math.max(0, Math.min(trackW, fillW)) - thumb / 2;
        int thumbY = trackY + QolUtilityUiMath.SLIDER_TRACK_HEIGHT / 2 - thumb / 2;
        RotClientUiDraw.roundedFill(
                graphics,
                thumbX,
                thumbY,
                thumbX + thumb,
                thumbY + thumb,
                disabled ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT,
                thumb / 2);
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
                String value = qol().readText(setting.id());
                if (setting.id().equals(listeningTextSettingId)) {
                    yield (value.isEmpty() ? "" : value) + "▌";
                }
                yield value.isBlank() ? "Click to type" : ellipsize(value, 16);
            }
            case ACTION -> "Open →";
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
        if (captureMouseButton(button)) {
            return true;
        }
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        int contentWidth = contentRight - contentLeft;
        boolean drawerOpen = isDrawerOpen();
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);
        int listW = contentWidth;

        if (drawerOpen) {
            int drawerX = contentRight - drawerW;
            int drawerY = contentTop;
            int drawerH = contentBottom - drawerY;
            if (RotClientUiDraw.inside(mx, my, drawerX, drawerY, drawerW, drawerH)) {
                if (handleScrollbarPress(
                        button,
                        mx,
                        my,
                        drawerScroll,
                        drawerX + drawerW - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                        drawerY + 52,
                        drawerY + drawerH - 10)) {
                    return true;
                }
                return handleDrawerClick(button, mx, my, drawerX, drawerY, drawerW, drawerH);
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                closeDrawer();
                return true;
            }
        }

        int listTop = pageContentListTop(contentTop);
        int scrollTrackTop = pageScrollTrackTop(contentTop);
        int listBottom = contentBottom;
        int listRight = contentLeft + listW;
        QolUtilityUiMath.PageFilter chipHit =
                QolUtilityUiMath.hitPageFilter(mx, my, contentLeft, contentTop);
        if (hudLayoutLanding && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && handleHudLayoutLandingClick(
                        mx, my, contentLeft, listTop, listW, contentBottom)) {
            return true;
        }
        if (appearanceLanding && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return handleAppearanceLandingClick(
                    mx, my, contentLeft, contentTop, listW, contentBottom, listTop);
        }
        if (chipHit != null
                && !hudLayoutLanding
                && !appearanceLanding
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pageFilter != chipHit) {
                pageFilter = chipHit;
                listScroll.reset();
            }
            return true;
        }
        if (handleScrollbarPress(
                button,
                mx,
                my,
                listScroll,
                listRight - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                scrollTrackTop,
                listBottom)) {
            return true;
        }
        if (hudLayoutLanding || appearanceLanding) {
            // Misses must not eat sidebar clicks or hit HUD & Display cards underneath.
            return false;
        }
        if (!RotClientUiDraw.inside(
                mx,
                my,
                contentLeft,
                listTop,
                listW,
                Math.max(0, listBottom - listTop))) {
            return false;
        }
        int gridWidth = Math.max(
                1,
                listW - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4);
        int baseY = listTop - listScroll.scrollPixels();
        QolUtilityUiMath.PageLayout layout = QolUtilityUiMath.layoutPage(
                QolUtilityUiMath.filterPageModules(
                        QolUtilityCatalog.modulesOnGroupPage(activePage),
                        pageFilter,
                        this::isEnabled),
                contentLeft,
                gridWidth);
        for (QolUtilityUiMath.PlacedCard card : layout.cards()) {
            int cardY = baseY + card.y();
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
            openModule(module.id());
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
        if (QolUtilityUiMath.hitClose(mx, my, drawerX, drawerY, drawerW)) {
            closeDrawer();
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
        } else if (!hudDrawerOpen() && module.toggleable() && runtimeAvailable(module)) {
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
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
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
            return;
        }
        if ("qol.custom_scoreboard.reset_events".equals(settingId)) {
            qol().extras().board().resetEvents();
            TrackerStore.save(config);
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
            if (AutoSellRuntime.addDefaults()) {
                TrackerStore.save(config);
            }
            return;
        }
        if ("qol.command_keybinds.open_sequence_editor".equals(settingId)) {
            Minecraft.getInstance().gui.setScreen(new HotkeySequenceEditorScreen(host));
            return;
        }
        if ("qol.storage_overlay.open_item_search".equals(settingId)) {
            Minecraft.getInstance().gui.setScreen(new ItemSearchScreen(host, ""));
            return;
        }
        if ("qol.dungeon_carry.open_manager".equals(settingId)) {
            Minecraft.getInstance().gui.setScreen(new DungeonCarryManagerScreen(host));
            return;
        }
        if ("qol.slayer_carry.open_manager".equals(settingId)) {
            Minecraft.getInstance().gui.setScreen(new SlayerCarryManagerScreen(host));
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
        if ("qol.dungeon_termsim.open".equals(settingId)) {
            TermSimRuntime.openFromCommand(-1);
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
            Minecraft.getInstance().gui.setScreen(new SlayerDropFilterScreen(host));
            return;
        }
        if ("qol.inventory_overlay.open_colors".equals(settingId)) {
            Minecraft.getInstance().gui.setScreen(new InventoryChromeColorsScreen(host));
            return;
        }
        if ("qol.inventory_buttons.open_editor".equals(settingId)) {
            Minecraft.getInstance().gui.setScreen(new InventoryButtonsEditorScreen(host));
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
        boolean backHover = AppearanceLandingPolicy.hitBack(
                mouseX, mouseY, contentLeft, listTop);
        RotClientUiDraw.text(
                graphics,
                font,
                "← Back",
                contentLeft,
                listTop + 4,
                backHover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                backHover);
        RotClientUiDraw.text(
                graphics,
                font,
                "Appearance",
                contentLeft + 80,
                listTop + 4,
                RotClientTheme.TEXT,
                true);
        int gridWidth = Math.max(
                1,
                contentRight - contentLeft - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4);
        java.util.List<AppearanceLandingPolicy.Card> cards = AppearanceLandingPolicy.cards();
        for (int i = 0; i < cards.size(); i++) {
            AppearanceLandingPolicy.Card card = cards.get(i);
            int[] rect = AppearanceLandingPolicy.cardRect(i, contentLeft, listTop, gridWidth);
            boolean hover = RotClientUiDraw.inside(
                    mouseX, mouseY, rect[0], rect[1], rect[2], rect[3]);
            RotClientUiDraw.roundedFill(
                    graphics,
                    rect[0],
                    rect[1],
                    rect[0] + rect[2],
                    rect[1] + rect[3],
                    hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT,
                    panelRadius());
            RotClientUiDraw.roundedOutline(
                    graphics,
                    rect[0],
                    rect[1],
                    rect[0] + rect[2],
                    rect[1] + rect[3],
                    hover ? RotClientTheme.VIOLET : RotClientTheme.BORDER,
                    panelRadius());
            graphics.fill(
                    rect[0],
                    rect[1] + 8,
                    rect[0] + 4,
                    rect[1] + QolUtilityUiMath.CARD_HEIGHT - 8,
                    accentColor());
            RotClientUiDraw.text(
                    graphics, font, card.title(),
                    rect[0] + 16, rect[1] + 28, RotClientTheme.TEXT, true);
            RotClientUiDraw.text(
                    graphics, font, card.subtitle(),
                    rect[0] + 16, rect[1] + 48, RotClientTheme.TEXT_MUTED, false);
            RotClientUiDraw.drawButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    rect[0] + rect[2] - QolUtilityUiMath.SETTINGS_BUTTON_WIDTH - 14,
                    rect[1] + QolUtilityUiMath.CARD_HEIGHT
                            - QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT - 12,
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
        int listW = contentRight - contentLeft;
        int gridWidth = Math.max(
                1,
                listW - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4);
        int headerBottom = listTop + HudLayoutLandingPolicy.headerHeight();
        listContentHeight = HudLayoutLandingPolicy.contentHeight();
        listScroll.setBounds(
                listContentHeight,
                Math.max(0, listBottom - headerBottom));
        listScroll.advanceSeconds(RotClientUiClock.seconds());
        int scroll = listScroll.scrollPixels();
        boolean backHover = AppearanceLandingPolicy.hitBack(
                mouseX, mouseY, contentLeft, listTop);
        RotClientUiDraw.text(
                graphics,
                font,
                "← Back",
                contentLeft,
                listTop + 4,
                backHover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                backHover);
        RotClientUiDraw.text(
                graphics,
                font,
                HudLayoutLandingPolicy.TITLE,
                contentLeft + 80,
                listTop + 4,
                RotClientTheme.TEXT,
                true);
        RotClientUiDraw.text(
                graphics,
                font,
                HudLayoutLandingPolicy.SUBTITLE,
                contentLeft,
                listTop + 16,
                RotClientTheme.TEXT_MUTED,
                false);
        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                contentLeft,
                listTop + HudLayoutLandingPolicy.EDITOR_TOP,
                168,
                HudLayoutLandingPolicy.EDITOR_HEIGHT,
                "Open editor",
                true,
                true);
        graphics.enableScissor(contentLeft, headerBottom, contentRight, listBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, listScroll);
        try {
            int y = headerBottom + HudLayoutLandingPolicy.SECTION_GAP - scroll;
            for (HudLayoutLandingPolicy.Section section : HudLayoutLandingPolicy.sections()) {
                RotClientUiDraw.text(
                        graphics, font, section.title(),
                        contentLeft, y + 2, RotClientTheme.TEXT_DIM, true);
                y += HudLayoutLandingPolicy.SECTION_LABEL_HEIGHT;
                for (HudLayoutLandingPolicy.Row row : section.rows()) {
                    boolean on = hudLayoutRowOn(row);
                    boolean hover = RotClientUiDraw.inside(
                            mouseX, mouseY, contentLeft, y, gridWidth,
                            HudLayoutLandingPolicy.ROW_HEIGHT);
                    RotClientUiDraw.roundedFill(
                            graphics,
                            contentLeft,
                            y,
                            contentLeft + gridWidth,
                            y + HudLayoutLandingPolicy.ROW_HEIGHT,
                            hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT,
                            panelRadius());
                    RotClientUiDraw.text(
                            graphics, font, row.label(),
                            contentLeft + 12, y + 8, RotClientTheme.TEXT, true);
                    RotClientUiDraw.text(
                            graphics,
                            font,
                            RotClientUiDraw.ellipsizeAndHover(
                                    font,
                                    row.description(),
                                    gridWidth - 160,
                                    contentLeft + 12,
                                    y + 24,
                                    12),
                            contentLeft + 12,
                            y + 26,
                            RotClientTheme.TEXT_MUTED,
                            false);
                    int toggleX = contentLeft + gridWidth
                            - HudLayoutLandingPolicy.TOGGLE_WIDTH - 12;
                    if (row.hasSettings()) {
                        int settingsX = toggleX - HudLayoutLandingPolicy.SETTINGS_WIDTH - 8;
                        RotClientUiDraw.drawButton(
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
                    RotClientUiDraw.drawToggle(
                            graphics,
                            toggleX,
                            y + 16,
                            on,
                            hover);
                    y += HudLayoutLandingPolicy.ROW_HEIGHT + HudLayoutLandingPolicy.ROW_GAP;
                }
                y += 8;
            }
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }
        if (listScroll.canScroll()) {
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    contentRight - RotClientUiDraw.SCROLLBAR_WIDTH - 2,
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
                HudLayerTogglePolicy.disable(qol(), "qol.cheater_wardrobe", true);
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
                    || HudLayerTogglePolicy.isOn(qol(), "qol.cheater_wardrobe", true);
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
        Minecraft.getInstance().gui.setScreen(
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

        if (drawerOpen) {
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
