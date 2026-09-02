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
    private String openHudMenuModuleId = "";
    private int hudMenuCardX;
    private int hudMenuCardY;
    private final RotClientScrollState hudMenuScroll = new RotClientScrollState();
    private String hudMenuQuery = "";
    private boolean hudMenuSearchFocused;
    private int listContentHeight;
    private int drawerContentHeight;
    private String headerHoverTip = "";

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
        if (activePage != page) {
            activePage = page;
            closeDrawer();
            closeHudMenu();
            listScroll.reset();
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
            openModule(tab.qolModuleId);
        }
    }

    void persistWorkspaceView() {
        RotClientWorkspace workspace = RotClientClient.workspace();
        if (workspace == null) {
            return;
        }
        workspace.setQolView(
                activePage.name(),
                isDrawerOpen() ? openModuleId : "");
    }

    boolean isDrawerOpen() {
        return openModuleId != null && !openModuleId.isBlank();
    }

    void closeDrawer() {
        openModuleId = "";
        pendingModuleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        draggingNumberSettingId = "";
        drawerScroll.reset();
        closeHudMenu();
        persistWorkspaceView();
    }

    private void closeHudMenu() {
        openHudMenuModuleId = "";
        hudMenuQuery = "";
        hudMenuSearchFocused = false;
        hudMenuScroll.reset();
    }

    void openModule(String moduleId) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(moduleId);
        if (module == null) {
            return;
        }
        if (activePage != module.group()) {
            activePage = module.group();
            listScroll.reset();
        }
        openModuleId = module.id();
        pendingModuleResetConfirm = false;
        openEnumSettingId = "";
        enumQuery = "";
        enumSearchFocused = false;
        listeningKeybindSettingId = "";
        listeningTextSettingId = "";
        collapsedDrawerSections.clear();
        drawerScroll.reset();
        closeHudMenu();
        persistWorkspaceView();
    }

    void openFromSearchId(String entryId) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(entryId);
        if (module != null) {
            openModule(module.id());
        }
    }

    private QolUtilityConfig qol() {
        if (config.qolUtilities == null) {
            config.qolUtilities = new QolUtilityConfig();
        }
        return config.qolUtilities;
    }

    private boolean isEnabled(QolUtilityCatalog.ModuleDef module) {
        return switch (module.id()) {
            case "qol.fullbright" -> config.fullbrightEnabled;
            case "qol.auto_sprint" -> config.autoSprintEnabled;
            case "qol.camera" -> config.cameraEnabled;
            default -> qol().isModuleEnabled(module.id());
        };
    }

    private void setEnabled(QolUtilityCatalog.ModuleDef module, boolean enabled) {
        switch (module.id()) {
            case "qol.fullbright" -> RotClientClient.setFullbrightEnabled(enabled);
            case "qol.auto_sprint" -> RotClientClient.setAutoSprintEnabled(enabled);
            case "qol.camera" -> RotClientClient.setCameraEnabled(enabled);
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
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);
        boolean overlay = QolUtilityUiMath.drawerIsOverlay(contentWidth);
        int listW = QolUtilityUiMath.listWidth(contentWidth, drawerOpen);
        int listRight = contentLeft + listW;

        roundedHeader(
                graphics,
                font,
                contentLeft,
                contentTop,
                contentWidth,
                mouseX,
                mouseY);

        int listTop = QolUtilityUiMath.pageListTop(contentTop);
        int listBottom = contentBottom;
        List<QolUtilityCatalog.ModuleDef> pageModules =
                QolUtilityCatalog.modulesInGroup(activePage);
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
                            mouseX,
                            mouseY);
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
                    mouseX,
                    mouseY,
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
        if (openHudMenuModuleId != null && !openHudMenuModuleId.isBlank()) {
            drawHudMenu(graphics, font, mouseX, mouseY);
        }

        if (drawerOpen) {
            int drawerX = overlay
                    ? contentRight - drawerW
                    : contentLeft + listW + 12;
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
                QolUtilityCatalog.modulesInGroup(activePage);
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
        boolean overlayDrawer = isDrawerOpen()
                && QolUtilityUiMath.drawerIsOverlay(width);
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
            if (chip.filter() == QolUtilityUiMath.PageFilter.CHEAT && cheatCount > 0) {
                chipLabel = "Cheat " + cheatCount;
            }
            if (font.width(chipLabel) > chip.width() - 4) {
                chipLabel = chip.label();
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
        if (config.fullbrightEnabled || config.autoSprintEnabled || config.cameraEnabled) {
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
        boolean hudMenuOpen = module.id().equals(openHudMenuModuleId);
        int fill = selected || hudMenuOpen
                ? RotClientTheme.SELECTED_ROW
                : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT);
        RotClientUiDraw.roundedFill(
                graphics, x, y, x + width, y + QolUtilityUiMath.CARD_HEIGHT, fill, panelRadius());
        int outline = selected || hudMenuOpen
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
            boolean hudHover = QolUtilityUiMath.hitHudControl(
                    mouseX, mouseY, x, y, QolUtilityUiMath.CARD_HEIGHT);
            boolean hudOn = hudPiecesOn(module, hudPieces);
            boolean menu = HudElementCatalog.hudControlOpensMenu(module);
            RotClientUiDraw.text(graphics, font,
                    menu ? "HUD ▾" : "HUD",
                    hudX,
                    footerY - 12,
                    RotClientTheme.VIOLET,
                    false);
            RotClientUiDraw.drawToggle(graphics, hudX, hudY, hudOn, hudHover);
            RotClientUiDraw.text(graphics, font,
                    hudOn ? "ON" : "OFF",
                    hudX + QolUtilityUiMath.TOGGLE_WIDTH + 6,
                    hudY + 3,
                    hudOn ? RotClientTheme.VIOLET : RotClientTheme.TEXT_MUTED,
                    true);
        }

        if (!module.settings().isEmpty()) {
            int buttonX = x + width - QolUtilityUiMath.CARD_PAD
                    - QolUtilityUiMath.SETTINGS_BUTTON_WIDTH;
            int buttonY = QolUtilityUiMath.cardFooterY(y, QolUtilityUiMath.CARD_HEIGHT)
                    + Math.max(0, (QolUtilityUiMath.FOOTER_HEIGHT
                    - QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT) / 2);
            boolean buttonHover = QolUtilityUiMath.hitSettingsButton(
                    mouseX, mouseY, x, y, width, QolUtilityUiMath.CARD_HEIGHT);
            RotClientUiDraw.roundedFill(
                    graphics,
                    buttonX,
                    buttonY,
                    buttonX + QolUtilityUiMath.SETTINGS_BUTTON_WIDTH,
                    buttonY + QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                    buttonHover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON,
                    RotClientUiDraw.RADIUS_SM);
            RotClientUiDraw.roundedOutline(
                    graphics,
                    buttonX,
                    buttonY,
                    buttonX + QolUtilityUiMath.SETTINGS_BUTTON_WIDTH,
                    buttonY + QolUtilityUiMath.SETTINGS_BUTTON_HEIGHT,
                    buttonHover ? RotClientTheme.VIOLET : RotClientTheme.BORDER,
                    RotClientUiDraw.RADIUS_SM);
            RotClientUiDraw.text(graphics, font,
                    "Settings",
                    buttonX + (QolUtilityUiMath.SETTINGS_BUTTON_WIDTH
                            - font.width("Settings")) / 2,
                    buttonY + 7,
                    RotClientTheme.BUTTON_TEXT,
                    false);
        }
        return hoverTip;
    }

    private boolean hudPiecesOn(
            QolUtilityCatalog.ModuleDef module,
            java.util.List<HudElementCatalog.HudPiece> pieces) {
        boolean sawToggle = false;
        for (HudElementCatalog.HudPiece piece : pieces) {
            if (!piece.hasToggle()) {
                continue;
            }
            sawToggle = true;
            if (Boolean.TRUE.equals(qol().readBoolean(piece.toggleId()))) {
                return true;
            }
        }
        return !sawToggle && module != null && isEnabled(module);
    }

    private void setHudPieces(
            java.util.List<HudElementCatalog.HudPiece> pieces, boolean enabled) {
        boolean wrote = false;
        for (HudElementCatalog.HudPiece piece : pieces) {
            if (piece.hasToggle()) {
                qol().writeBoolean(piece.toggleId(), enabled);
                wrote = true;
            }
        }
        if (wrote) {
            TrackerStore.save(config);
        }
    }

    private void drawHudMenu(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY) {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openHudMenuModuleId);
        if (module == null) {
            closeHudMenu();
            return;
        }
        java.util.List<HudElementCatalog.HudPiece> pieces = HudElementCatalog.hudPieces(module);
        if (pieces.isEmpty()) {
            closeHudMenu();
            return;
        }
        java.util.List<Integer> matches = hudMenuMatches(pieces);
        boolean search = OverflowListPolicy.needsSearch(pieces.size());
        int menuX = QolUtilityUiMath.hudMenuX(hudMenuCardX);
        int preferredY = QolUtilityUiMath.hudMenuY(hudMenuCardY, QolUtilityUiMath.CARD_HEIGHT);
        int menuW = QolUtilityUiMath.HUD_MENU_WIDTH;
        int screenH = host == null ? 480 : Math.max(64, host.height);
        int preferredH = OverflowListPolicy.menuHeight(
                Math.max(1, matches.size()),
                QolUtilityUiMath.HUD_MENU_ROW_HEIGHT,
                search);
        int menuH = OverflowListPolicy.clampedHeight(preferredH, screenH, preferredY);
        int menuY = OverflowListPolicy.clampY(preferredY, menuH, screenH);
        int bodyTop = QolUtilityUiMath.hudMenuBodyTop(menuY, search);
        int bodyBottom = menuY + menuH - 4;
        hudMenuScroll.setBounds(
                matches.size() * QolUtilityUiMath.HUD_MENU_ROW_HEIGHT,
                Math.max(1, bodyBottom - bodyTop));
        hudMenuScroll.advanceSeconds(RotClientUiClock.seconds());

        RotClientUiDraw.roundedFill(
                graphics,
                menuX,
                menuY,
                menuX + menuW,
                menuY + menuH,
                RotClientTheme.SURFACE_ALT,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.roundedOutline(
                graphics,
                menuX,
                menuY,
                menuX + menuW,
                menuY + menuH,
                RotClientTheme.VIOLET,
                RotClientUiDraw.RADIUS_SM);
        if (search) {
            int fieldY = menuY + 4;
            RotClientUiDraw.roundedFill(
                    graphics,
                    menuX + 6,
                    fieldY,
                    menuX + menuW - 6,
                    fieldY + QolUtilityUiMath.HUD_MENU_SEARCH_HEIGHT - 4,
                    hudMenuSearchFocused ? RotClientTheme.FIELD : RotClientTheme.BUTTON,
                    RotClientUiDraw.RADIUS_SM);
            String shown = hudMenuQuery.isBlank() ? "Search…" : hudMenuQuery;
            RotClientUiDraw.text(graphics, font,
                    RotClientUiDraw.ellipsizeAndHover(
                            font, shown, menuW - 20, menuX + 12, fieldY + 4, 12),
                    menuX + 12,
                    fieldY + 4,
                    hudMenuQuery.isBlank() ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT,
                    false);
        }
        graphics.enableScissor(menuX, bodyTop, menuX + menuW, bodyBottom);
        RotClientUiMotion.pushFractionalScroll(graphics, hudMenuScroll);
        try {
            int scroll = hudMenuScroll.scrollPixels();
            for (int visible = 0; visible < matches.size(); visible++) {
                HudElementCatalog.HudPiece piece = pieces.get(matches.get(visible));
                int rowY = bodyTop + visible * QolUtilityUiMath.HUD_MENU_ROW_HEIGHT - scroll;
                if (rowY + QolUtilityUiMath.HUD_MENU_ROW_HEIGHT < bodyTop
                        || rowY > bodyBottom) {
                    continue;
                }
                boolean rowHover = QolUtilityUiMath.hitHudMenuRow(
                        mouseX, mouseY, menuX, menuY, visible, search, scroll);
                if (rowHover) {
                    RotClientUiDraw.roundedFill(
                            graphics,
                            menuX + 4,
                            rowY,
                            menuX + menuW - 4,
                            rowY + QolUtilityUiMath.HUD_MENU_ROW_HEIGHT,
                            RotClientTheme.HOVER_ROW,
                            RotClientUiDraw.RADIUS_SM);
                }
                RotClientUiDraw.text(graphics, font,
                        RotClientUiDraw.ellipsizeAndHover(
                                font,
                                piece.label(),
                                menuW - 120,
                                menuX + 10,
                                rowY + 8,
                                12),
                        menuX + 10,
                        rowY + 8,
                        RotClientTheme.TEXT,
                        false);
                if (piece.hasToggle()) {
                    boolean on = Boolean.TRUE.equals(qol().readBoolean(piece.toggleId()));
                    RotClientUiDraw.drawToggle(
                            graphics,
                            menuX + menuW - 8 - QolUtilityUiMath.HUD_MENU_EDIT_WIDTH
                                    - 8 - QolUtilityUiMath.TOGGLE_WIDTH,
                            rowY + 4,
                            on,
                            rowHover);
                }
                if (piece.hasEditor()) {
                    int editX = menuX + menuW - 8 - QolUtilityUiMath.HUD_MENU_EDIT_WIDTH;
                    boolean editHover = QolUtilityUiMath.hitHudMenuEdit(
                            mouseX, mouseY, menuX, menuY, visible, search, scroll);
                    RotClientUiDraw.roundedFill(
                            graphics,
                            editX,
                            rowY + 3,
                            editX + QolUtilityUiMath.HUD_MENU_EDIT_WIDTH,
                            rowY + QolUtilityUiMath.HUD_MENU_ROW_HEIGHT - 3,
                            editHover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON,
                            RotClientUiDraw.RADIUS_SM);
                    RotClientUiDraw.text(graphics, font,
                            "Edit",
                            editX + 12,
                            rowY + 8,
                            RotClientTheme.VIOLET,
                            false);
                }
            }
        } finally {
            RotClientUiMotion.pop(graphics);
            graphics.disableScissor();
        }
        if (hudMenuScroll.canScroll()) {
            RotClientUiDraw.drawScrollbar(
                    graphics,
                    menuX + menuW - RotClientUiDraw.SCROLLBAR_WIDTH - 2,
                    bodyTop,
                    bodyBottom,
                    hudMenuScroll.contentHeight(),
                    hudMenuScroll.scrollPixels(),
                    false,
                    hudMenuScroll.isThumbDragging());
        }
    }

    private java.util.List<Integer> hudMenuMatches(
            java.util.List<HudElementCatalog.HudPiece> pieces) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (HudElementCatalog.HudPiece piece : pieces) {
            labels.add(piece.label());
        }
        return OverflowListPolicy.matchingIndices(labels, hudMenuQuery);
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
        RotClientUiDraw.text(graphics, font, module.name(), x + 14, y + 12, RotClientTheme.TEXT, true);
        int descMax = Math.max(40, width - 50);
        String explanation = HudElementCatalog.explainedDescription(module);
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
        if (module.toggleable() && runtimeAvailable(module)) {
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
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
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

        if (!module.settings().isEmpty() && runtimeAvailable(module)) {
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
        drawMiniSwitch(
                graphics,
                x + width - QolUtilityUiMath.DRAWER_CONTROL_RESERVE + 8,
                y + 8,
                enabled);
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
            Boolean value = qol().readBoolean(setting.id());
            drawMiniSwitch(
                    graphics,
                    x + width - QolUtilityUiMath.DRAWER_CONTROL_RESERVE + 8,
                    y + 10,
                    value != null && value && !disabled);
        } else if (setting.type() == QolUtilityCatalog.SettingType.COLOR) {
            Integer color = qol().readColor(setting.id());
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
        Double raw = qol().readNumber(settingId);
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
                Double value = qol().readNumber(setting.id());
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
                        || setting.id().contains("box_size")) {
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

    private static void drawMiniSwitch(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            boolean on) {
        int w = 28;
        int h = 14;
        graphics.fill(
                x,
                y,
                x + w,
                y + h,
                on ? RotClientTheme.HUD_ACCENT : RotClientTheme.TOGGLE_OFF);
        int knob = on ? x + w - 12 : x + 2;
        graphics.fill(knob, y + 2, knob + 10, y + h - 2, RotClientTheme.TEXT);
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
                || enumSearchFocused
                || hudMenuSearchFocused;
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
        if (hudMenuSearchFocused
                && openHudMenuModuleId != null
                && !openHudMenuModuleId.isBlank()) {
            if (glfwKey == GLFW.GLFW_KEY_ESCAPE) {
                closeHudMenu();
                return true;
            }
            if (glfwKey == GLFW.GLFW_KEY_BACKSPACE && !hudMenuQuery.isEmpty()) {
                hudMenuQuery = hudMenuQuery.substring(
                        0, hudMenuQuery.offsetByCodePoints(hudMenuQuery.length(), -1));
                hudMenuScroll.reset();
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
        if (hudMenuSearchFocused
                && openHudMenuModuleId != null
                && !openHudMenuModuleId.isBlank()) {
            if (!allowedChatCharacter || codePoint < 32) {
                return true;
            }
            if (hudMenuQuery.length() >= 32) {
                return true;
            }
            hudMenuQuery += Character.toString(codePoint);
            hudMenuScroll.reset();
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
        if (handleHudMenuClick(button, mx, my)) {
            return true;
        }
        int contentWidth = contentRight - contentLeft;
        boolean drawerOpen = isDrawerOpen();
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);
        boolean overlay = QolUtilityUiMath.drawerIsOverlay(contentWidth);
        int listW = QolUtilityUiMath.listWidth(contentWidth, drawerOpen);

        if (drawerOpen) {
            int drawerX = overlay
                    ? contentRight - drawerW
                    : contentLeft + listW + 12;
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
            if (overlay && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                // click outside overlay drawer closes it
                closeDrawer();
                return true;
            }
        }

        int listTop = QolUtilityUiMath.pageListTop(contentTop);
        int listBottom = contentBottom;
        int listRight = contentLeft + listW;
        QolUtilityUiMath.PageFilter chipHit =
                QolUtilityUiMath.hitPageFilter(mx, my, contentLeft, contentTop);
        if (chipHit != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pageFilter != chipHit) {
                pageFilter = chipHit;
                listScroll.reset();
                closeHudMenu();
            }
            return true;
        }
        if (handleScrollbarPress(
                button,
                mx,
                my,
                listScroll,
                listRight - RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                listTop,
                listBottom)) {
            return true;
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
                        QolUtilityCatalog.modulesInGroup(activePage),
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
        boolean hudOpensMenu = HudElementCatalog.hudControlOpensMenu(module);
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
                hudOpensMenu);
        if (action == QolUtilityUiMath.CardAction.OPEN_SETTINGS) {
            closeHudMenu();
            openModule(module.id());
            return true;
        }
        if (action == QolUtilityUiMath.CardAction.TOGGLE) {
            closeHudMenu();
            setEnabled(module, !isEnabled(module));
            return true;
        }
        if (action == QolUtilityUiMath.CardAction.TOGGLE_HUD) {
            closeHudMenu();
            if (hudPieces.stream().anyMatch(HudElementCatalog.HudPiece::hasToggle)) {
                setHudPieces(hudPieces, !hudPiecesOn(module, hudPieces));
            } else if (hudPieces.size() == 1 && hudPieces.get(0).hasEditor()) {
                RotClientClient.openHudEditor(
                        host,
                        HudElementCatalog.focusIdForHudEditorSetting(
                                hudPieces.get(0).editorId()));
            }
            return true;
        }
        if (action == QolUtilityUiMath.CardAction.OPEN_HUD_MENU) {
            if (module.id().equals(openHudMenuModuleId)) {
                closeHudMenu();
            } else {
                openHudMenuModuleId = module.id();
                hudMenuCardX = cardX;
                hudMenuCardY = cardY;
                hudMenuQuery = "";
                hudMenuSearchFocused = OverflowListPolicy.needsSearch(
                        HudElementCatalog.hudPieces(module).size());
                hudMenuScroll.reset();
            }
            return true;
        }
        closeHudMenu();
        return true;
    }

    private int[] hudMenuGeometry(java.util.List<HudElementCatalog.HudPiece> pieces) {
        java.util.List<Integer> matches = hudMenuMatches(pieces);
        boolean search = OverflowListPolicy.needsSearch(pieces.size());
        int menuX = QolUtilityUiMath.hudMenuX(hudMenuCardX);
        int preferredY = QolUtilityUiMath.hudMenuY(hudMenuCardY, QolUtilityUiMath.CARD_HEIGHT);
        int screenH = host == null ? 480 : Math.max(64, host.height);
        int preferredH = OverflowListPolicy.menuHeight(
                Math.max(1, matches.size()),
                QolUtilityUiMath.HUD_MENU_ROW_HEIGHT,
                search);
        int menuH = OverflowListPolicy.clampedHeight(preferredH, screenH, preferredY);
        int menuY = OverflowListPolicy.clampY(preferredY, menuH, screenH);
        return new int[] {menuX, menuY, menuH, search ? 1 : 0};
    }

    private boolean handleHudMenuClick(int button, int mx, int my) {
        if (openHudMenuModuleId == null || openHudMenuModuleId.isBlank()) {
            return false;
        }
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openHudMenuModuleId);
        java.util.List<HudElementCatalog.HudPiece> pieces =
                module == null ? java.util.List.of() : HudElementCatalog.hudPieces(module);
        int[] geo = hudMenuGeometry(pieces);
        int menuX = geo[0];
        int menuY = geo[1];
        int menuH = geo[2];
        boolean search = geo[3] == 1;
        boolean inside = RotClientUiDraw.inside(
                mx, my, menuX, menuY, QolUtilityUiMath.HUD_MENU_WIDTH, menuH);
        if (!inside) {
            closeHudMenu();
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || module == null) {
            return true;
        }
        if (search && my < QolUtilityUiMath.hudMenuBodyTop(menuY, true)) {
            hudMenuSearchFocused = true;
            return true;
        }
        hudMenuSearchFocused = false;
        java.util.List<Integer> matches = hudMenuMatches(pieces);
        int scroll = hudMenuScroll.scrollPixels();
        for (int visible = 0; visible < matches.size(); visible++) {
            HudElementCatalog.HudPiece piece = pieces.get(matches.get(visible));
            if (piece.hasEditor()
                    && QolUtilityUiMath.hitHudMenuEdit(
                            mx, my, menuX, menuY, visible, search, scroll)) {
                closeHudMenu();
                RotClientClient.openHudEditor(
                        host,
                        HudElementCatalog.focusIdForHudEditorSetting(piece.editorId()));
                return true;
            }
            if (QolUtilityUiMath.hitHudMenuToggle(
                    mx, my, menuX, menuY, visible, search, scroll)
                    && piece.hasToggle()) {
                Boolean current = qol().readBoolean(piece.toggleId());
                qol().writeBoolean(piece.toggleId(), current == null || !current);
                TrackerStore.save(config);
                return true;
            }
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

        if (module.toggleable() && runtimeAvailable(module)) {
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
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
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

        if (!module.settings().isEmpty() && runtimeAvailable(module)) {
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
        boolean reset = qol().resetModuleToDefaults(module.id());
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
            case TOGGLE -> {
                if (qol().toggleBooleanSetting(setting.id())) {
                    TrackerStore.save(config);
                }
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
        if ("qol.storage_overlay.clear_cache".equals(settingId)) {
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
        Integer current = qol().readColor(setting.id());
        if (current == null) {
            return;
        }
        pendingColorSettingId = setting.id();
        int initial = current;
        Minecraft.getInstance().gui.setScreen(
                new RotClientColorPickerScreen(
                        host,
                        setting.label(),
                        initial,
                        color -> {
                            if (qol().writeColor(pendingColorSettingId, color)) {
                                TrackerStore.save(config);
                            }
                        }));
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
        if (openHudMenuModuleId != null && !openHudMenuModuleId.isBlank()) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(openHudMenuModuleId);
            java.util.List<HudElementCatalog.HudPiece> pieces =
                    module == null ? java.util.List.of() : HudElementCatalog.hudPieces(module);
            int[] geo = hudMenuGeometry(pieces);
            if (RotClientUiDraw.inside(
                    mx, my, geo[0], geo[1], QolUtilityUiMath.HUD_MENU_WIDTH, geo[2])) {
                if (hudMenuScroll.canScroll()) {
                    hudMenuScroll.scrollBySteps(verticalAmount, 28);
                }
                return true;
            }
        }
        int contentWidth = contentRight - contentLeft;
        boolean drawerOpen = isDrawerOpen();
        int drawerW = QolUtilityUiMath.drawerWidth(contentWidth);
        boolean overlay = QolUtilityUiMath.drawerIsOverlay(contentWidth);
        int listW = QolUtilityUiMath.listWidth(contentWidth, drawerOpen);

        if (drawerOpen) {
            int drawerX = overlay
                    ? contentRight - drawerW
                    : contentLeft + listW + 12;
            int drawerY = contentTop;
            int drawerH = contentBottom - drawerY;
            if (RotClientUiDraw.inside(mx, my, drawerX, drawerY, drawerW, drawerH)) {
                if (!drawerScroll.canScroll()) {
                    return false;
                }
                drawerScroll.scrollBySteps(verticalAmount, 40);
                return true;
            }
        }

        int listTop = QolUtilityUiMath.pageListTop(contentTop);
        if (RotClientUiDraw.inside(
                mx, my, contentLeft, listTop, listW, contentBottom - listTop)) {
            if (!listScroll.canScroll()) {
                return false;
            }
            closeHudMenu();
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
        return listScroll.dragThumbTo(
                my,
                QolUtilityUiMath.pageListTop(contentTop),
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
        qol().writeNumber(draggingNumberSettingId, next);
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
        if (config.fullbrightEnabled) {
            on.add("FB");
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
