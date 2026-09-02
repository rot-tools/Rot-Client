package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Layout editor for Mining/Powder HUDs, QoL overlays and Client UI ghost panel.
 * Visibility toggles on TrackerConfig / QolUtilityConfig are reflected by live
 * HUD previews.
 */
final class RotClientScreen extends Screen {
    private static final int CLIENT_UI_GHOST_WIDTH = 820;
    private static final int CLIENT_UI_GHOST_HEIGHT = 450;

    private enum LayoutTarget {
        MINING_HUD,
        POWDER_CHEST_HUD,
        QOL_HUD,
        CLIENT_UI
    }

    private final RotClientHud hud;
    private final PowderChestHud powderHud;
    private final QolOverlayHud qolHud;
    private final Screen parent;
    private final String initialFocusId;
    private LayoutTarget target = LayoutTarget.MINING_HUD;
    private boolean clientUiDragging;
    private int clientUiDragOriginMouseX;
    private int clientUiDragOriginMouseY;
    private int clientUiDragOriginPanelX;
    private int clientUiDragOriginPanelY;
    private HudEditorChromePolicy.Panel chromeDragging;
    private int chromeDragOffsetX;
    private int chromeDragOffsetY;
    private int inspectorScroll;

    RotClientScreen(RotClientHud hud) {
        this(hud, null, "");
    }

    RotClientScreen(RotClientHud hud, Screen parent) {
        this(hud, parent, "");
    }

    RotClientScreen(RotClientHud hud, Screen parent, String focusId) {
        super(Component.literal("Rot Client editor"));
        this.hud = hud;
        this.powderHud = RotClientClient.powderChestHud();
        this.qolHud = RotClientClient.qolHud();
        this.parent = parent;
        this.initialFocusId = focusId == null ? "" : focusId.trim();
        if (!this.initialFocusId.isEmpty()) {
            this.target = LayoutTarget.QOL_HUD;
        }
    }

    @Override
    protected void init() {
        hud.setEditorOpen(true);
        powderHud.setEditorOpen(true);
        qolHud.setEditorOpen(true);
        qolHud.setFocusId(initialFocusId);
        hud.clampToScreen();
        powderHud.clampToScreen();
    }

    @Override
    public void onClose() {
        hud.setEditorOpen(false);
        powderHud.setEditorOpen(false);
        qolHud.setEditorOpen(false);
        RotClientClient.workspace().flushIfDirty();
        RotClientClient.save();
        if (parent != null) {
            minecraft.gui.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void removed() {
        hud.setEditorOpen(false);
        powderHud.setEditorOpen(false);
        qolHud.setEditorOpen(false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // The world is the positioning reference in this editor. Keeping this
        // empty prevents Minecraft's automatic in-game screen dim layer.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        boolean hoverHud = hud.containsScreen(mouseX, mouseY);
        boolean hoverPowderHud = powderHud.containsScreen(mouseX, mouseY);
        boolean hoverUi = RotClientUiDraw.inside(
                mx, my, clientUiGhostX(), clientUiGhostY(),
                CLIENT_UI_GHOST_WIDTH, CLIENT_UI_GHOST_HEIGHT);

        drawClientUiGhost(graphics, hoverUi);
        drawHudEditorChrome(graphics, hoverHud);
        drawPowderHudEditorChrome(graphics, hoverPowderHud);
        qolHud.render(graphics);

        String selected = selectedLabel();
        HudEditorChromePolicy.Rect title = chromeRect(HudEditorChromePolicy.Panel.TITLE);
        drawMovableChromeCard(
                graphics,
                title,
                "LAYOUT EDITOR",
                "Selected: " + selected,
                mx,
                my);
        HudEditorChromePolicy.Rect help = chromeRect(HudEditorChromePolicy.Panel.HELP);
        drawHelpCard(graphics, help, mx, my);
        drawInspector(graphics, mx, my);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private static final int INSPECTOR_WIDTH = HudEditorChromePolicy.INSPECTOR_WIDTH;

    private String selectedLabel() {
        return switch (target) {
            case MINING_HUD -> "Mining HUD";
            case POWDER_CHEST_HUD -> "Powder Chest HUD";
            case QOL_HUD -> qolHud.selectedElementLabel();
            case CLIENT_UI -> "Client UI";
        };
    }

    private QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private HudEditorChromePolicy.Rect chromeRect(HudEditorChromePolicy.Panel panel) {
        QolSkyblockExtras extras = extras();
        return switch (panel) {
            case TITLE -> HudEditorChromePolicy.resolve(
                    panel, extras.hudEditorTitleX, extras.hudEditorTitleY, this.width, this.height);
            case HELP -> HudEditorChromePolicy.resolve(
                    panel, extras.hudEditorHelpX, extras.hudEditorHelpY, this.width, this.height);
            case INSPECTOR -> HudEditorChromePolicy.resolve(
                    panel, extras.hudEditorInspectorX, extras.hudEditorInspectorY, this.width, this.height);
        };
    }

    private void storeChrome(HudEditorChromePolicy.Panel panel, int x, int y) {
        QolSkyblockExtras extras = extras();
        HudEditorChromePolicy.Rect rect = chromeRect(panel);
        int nextX = HudEditorChromePolicy.clampDragX(x, rect.w(), this.width);
        int nextY = HudEditorChromePolicy.clampDragY(y, rect.h(), this.height);
        switch (panel) {
            case TITLE -> {
                extras.hudEditorTitleX = nextX;
                extras.hudEditorTitleY = nextY;
            }
            case HELP -> {
                extras.hudEditorHelpX = nextX;
                extras.hudEditorHelpY = nextY;
            }
            case INSPECTOR -> {
                extras.hudEditorInspectorX = nextX;
                extras.hudEditorInspectorY = nextY;
            }
        }
    }

    private void drawMovableChromeCard(
            GuiGraphicsExtractor graphics,
            HudEditorChromePolicy.Rect rect,
            String title,
            String subtitle,
            int mouseX,
            int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        RotClientUiDraw.roundedFill(
                graphics,
                rect.x(),
                rect.y(),
                rect.x() + rect.w(),
                rect.y() + rect.h(),
                RotClientUiDraw.withAlpha(RotClientTheme.SURFACE, 0xF0));
        RotClientUiDraw.roundedOutline(
                graphics,
                rect.x(),
                rect.y(),
                rect.x() + rect.w(),
                rect.y() + rect.h(),
                hover ? RotClientTheme.HUD_ACCENT : RotClientTheme.BORDER);
        RotClientUiDraw.text(graphics, font, title, rect.x() + 10, rect.y() + 8, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(
                graphics,
                font,
                subtitle,
                rect.x() + 10,
                rect.y() + 22,
                RotClientTheme.TEXT_DIM,
                false);
        RotClientUiDraw.text(
                graphics,
                font,
                "Drag this card",
                rect.x() + 10,
                rect.y() + 36,
                RotClientTheme.TEXT_MUTED,
                false);
    }

    private void drawHelpCard(
            GuiGraphicsExtractor graphics,
            HudEditorChromePolicy.Rect rect,
            int mouseX,
            int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        RotClientUiDraw.roundedFill(
                graphics,
                rect.x(),
                rect.y(),
                rect.x() + rect.w(),
                rect.y() + rect.h(),
                RotClientUiDraw.withAlpha(RotClientTheme.SURFACE, 0xF0));
        RotClientUiDraw.roundedOutline(
                graphics,
                rect.x(),
                rect.y(),
                rect.x() + rect.w(),
                rect.y() + rect.h(),
                hover ? RotClientTheme.HUD_ACCENT : RotClientTheme.BORDER);
        RotClientUiDraw.text(graphics, font, "How to edit", rect.x() + 10, rect.y() + 8, RotClientTheme.TEXT, true);
        int row = rect.y() + 22;
        for (String line : HudEditorChromePolicy.helpLines()) {
            RotClientUiDraw.text(graphics, font, line, rect.x() + 10, row, RotClientTheme.TEXT_DIM, false);
            row += 12;
        }
    }

    private int inspectorX() {
        return chromeRect(HudEditorChromePolicy.Panel.INSPECTOR).x();
    }

    private int inspectorY() {
        return chromeRect(HudEditorChromePolicy.Panel.INSPECTOR).y();
    }

    private int inspectorHeight() {
        return chromeRect(HudEditorChromePolicy.Panel.INSPECTOR).h();
    }

    private void drawInspector(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = inspectorX();
        int y = inspectorY();
        int w = INSPECTOR_WIDTH;
        int h = inspectorHeight();
        RotClientUiDraw.roundedFill(
                graphics, x, y, x + w, y + h, RotClientUiDraw.withAlpha(RotClientTheme.SURFACE, 0xF0));
        RotClientUiDraw.roundedOutline(graphics, x, y, x + w, y + h, RotClientTheme.BORDER);
        RotClientUiDraw.text(graphics, font, "HUD inspector", x + 10, y + 8, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, "Drag header to move", x + 10, y + 20, RotClientTheme.TEXT_MUTED, false);
        String selected = selectedLabel();
        RotClientUiDraw.text(graphics, font, selected, x + 10, y + 32, RotClientTheme.TEXT_DIM, false);
        int row = y + 50;
        String bgLabel = selectedBackgroundOn() ? "Background" : "Text only";
        drawInspectorButton(graphics, x + 10, row, 100, 18, bgLabel, mouseX, mouseY);
        drawInspectorButton(graphics, x + 116, row, 100, 18, "Scale +", mouseX, mouseY);
        row += 22;
        drawInspectorButton(graphics, x + 10, row, 100, 18, "Text color", mouseX, mouseY);
        drawInspectorButton(graphics, x + 116, row, 100, 18, "Scale -", mouseX, mouseY);
        row += 22;
        drawInspectorButton(graphics, x + 10, row, 206, 18, "Panel color", mouseX, mouseY);
        row += 26;
        int listTop = row;
        int listBottom = y + h - 18;
        java.util.List<InspectorRow> rows = inspectorRows();
        int content = rows.size() * 12;
        int viewport = Math.max(12, listBottom - listTop);
        inspectorScroll = Math.max(0, Math.min(inspectorScroll, Math.max(0, content - viewport)));
        int drawY = listTop - inspectorScroll;
        for (InspectorRow entry : rows) {
            if (drawY + 12 > listTop && drawY < listBottom) {
                if (entry.heading()) {
                    RotClientUiDraw.text(graphics, font, entry.label(), x + 10, drawY, RotClientTheme.TEXT_MUTED, false);
                } else {
                    boolean enabled = HudLayerTogglePolicy.isOn(
                            RotClientClient.qolConfigPublic(), entry.settingId(), entry.moduleToggle());
                    RotClientUiDraw.text(
                            graphics,
                            font,
                            (enabled ? "[ON]  " : "[off] ") + entry.label(),
                            x + 10,
                            drawY,
                            enabled ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_MUTED,
                            false);
                }
            }
            drawY += 12;
        }
        RotClientUiDraw.text(graphics, font,
                "Scroll list  ·  drag header to move",
                x + 10,
                y + h - 16,
                RotClientTheme.TEXT_MUTED,
                false);
    }

    private record InspectorRow(String settingId, String label, boolean heading, boolean moduleToggle) {
    }

    private java.util.List<InspectorRow> inspectorRows() {
        java.util.ArrayList<InspectorRow> rows = new java.util.ArrayList<>();
        rows.add(new InspectorRow("", "Vanilla / Hypixel HUD", true, false));
        for (HudLayerCatalog.Layer layer : HudLayerCatalog.vanillaLayers()) {
            rows.add(new InspectorRow(layer.settingId(), layer.label(), false, false));
        }
        rows.add(new InspectorRow("", "Rot overlays", true, false));
        for (HudLayerCatalog.Layer layer : HudLayerCatalog.rotOverlays()) {
            rows.add(new InspectorRow(layer.settingId(), layer.label(), false, layer.moduleToggle()));
        }
        java.util.List<HudElementCatalog.InspectorToggle> bits =
                target == LayoutTarget.QOL_HUD
                        ? HudElementCatalog.inspectorToggles(qolHud.selectedId())
                        : java.util.List.of();
        if (!bits.isEmpty()) {
            rows.add(new InspectorRow("", "Selected HUD bits", true, false));
            for (HudElementCatalog.InspectorToggle toggle : bits) {
                rows.add(new InspectorRow(toggle.settingId(), toggle.label(), false, false));
            }
        }
        return rows;
    }

    private boolean selectedBackgroundOn() {
        if (target == LayoutTarget.MINING_HUD) {
            return RotClientClient.trackerConfig().hudShowBackground;
        }
        if (target == LayoutTarget.POWDER_CHEST_HUD) {
            return RotClientClient.trackerConfig().powderChestHudShowBackground;
        }
        if (target == LayoutTarget.QOL_HUD) {
            return RotClientClient.qolConfigPublic().extras()
                    .resolvedHudStyle(qolHud.selectedId()).showBackground;
        }
        return true;
    }

    private void toggleSelectedBackground() {
        if (target == LayoutTarget.MINING_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            config.hudShowBackground = !config.hudShowBackground;
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.POWDER_CHEST_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            config.powderChestHudShowBackground = !config.powderChestHudShowBackground;
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.QOL_HUD) {
            qolHud.toggleSelectedBackground();
            RotClientClient.save();
        }
    }

    private void drawInspectorButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int w,
            int h,
            String label,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(mouseX, mouseY, x, y, w, h);
        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + w,
                y + h,
                hover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON);
        RotClientUiDraw.text(graphics, font, label, x + 6, y + 5, RotClientTheme.BUTTON_TEXT, false);
    }

    private boolean handleInspectorClick(int mx, int my) {
        int x = inspectorX();
        int y = inspectorY();
        if (!RotClientUiDraw.inside(mx, my, x, y, INSPECTOR_WIDTH, inspectorHeight())) {
            return false;
        }
        int row = y + 50;
        if (RotClientUiDraw.inside(mx, my, x + 10, row, 100, 18)) {
            toggleSelectedBackground();
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + 116, row, 100, 18)) {
            if (target == LayoutTarget.QOL_HUD) {
                qolHud.nudgeSelectedScale(0.1F);
            } else if (target == LayoutTarget.MINING_HUD) {
                hud.nudgeScale(0.1F);
            } else if (target == LayoutTarget.POWDER_CHEST_HUD) {
                powderHud.nudgeScale(0.1F);
            }
            RotClientClient.save();
            return true;
        }
        row += 22;
        if (RotClientUiDraw.inside(mx, my, x + 10, row, 100, 18)) {
            if (target == LayoutTarget.QOL_HUD) {
                openSelectedHudColor(false);
            }
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + 116, row, 100, 18)) {
            if (target == LayoutTarget.QOL_HUD) {
                qolHud.nudgeSelectedScale(-0.1F);
            } else if (target == LayoutTarget.MINING_HUD) {
                hud.nudgeScale(-0.1F);
            } else if (target == LayoutTarget.POWDER_CHEST_HUD) {
                powderHud.nudgeScale(-0.1F);
            }
            RotClientClient.save();
            return true;
        }
        row += 22;
        if (RotClientUiDraw.inside(mx, my, x + 10, row, 206, 18)) {
            if (target == LayoutTarget.QOL_HUD) {
                openSelectedHudColor(true);
            }
            return true;
        }
        row += 26;
        int listTop = row;
        int listBottom = y + inspectorHeight() - 18;
        java.util.List<InspectorRow> rows = inspectorRows();
        int drawY = listTop - inspectorScroll;
        for (InspectorRow entry : rows) {
            if (!entry.heading()
                    && drawY >= listTop
                    && drawY < listBottom
                    && RotClientUiDraw.inside(mx, my, x + 10, drawY, INSPECTOR_WIDTH - 20, 12)
                    && HudLayerTogglePolicy.toggle(
                            RotClientClient.qolConfigPublic(), entry.settingId(), entry.moduleToggle())) {
                RotClientClient.save();
                return true;
            }
            drawY += 12;
        }
        return true;
    }

    private void drawClientUiGhost(GuiGraphicsExtractor graphics, boolean hovered) {
        int ghostX = clientUiGhostX();
        int ghostY = clientUiGhostY();
        graphics.fill(
                ghostX,
                ghostY,
                ghostX + CLIENT_UI_GHOST_WIDTH,
                ghostY + CLIENT_UI_GHOST_HEIGHT,
                RotClientUiDraw.withAlpha(RotClientTheme.SURFACE, 0x66));
        graphics.fill(
                ghostX,
                ghostY,
                ghostX + CLIENT_UI_GHOST_WIDTH,
                ghostY + 44,
                RotClientUiDraw.withAlpha(RotClientTheme.HUD_ACCENT, 0x88));
        RotClientUiDraw.text(graphics, font,
                "Client UI",
                ghostX + 12,
                ghostY + 16,
                RotClientTheme.TEXT,
                true);
        int outline = target == LayoutTarget.CLIENT_UI
                ? RotClientTheme.HUD_ACCENT
                : (hovered ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER);
        RotClientUiDraw.roundedOutline(
                graphics,
                ghostX - 1,
                ghostY - 1,
                ghostX + CLIENT_UI_GHOST_WIDTH + 1,
                ghostY + CLIENT_UI_GHOST_HEIGHT + 1,
                outline);
    }

    private void drawHudEditorChrome(GuiGraphicsExtractor graphics, boolean hovered) {
        TrackerConfig config = RotClientClient.trackerConfig();
        int left = Math.round(config.x);
        int top = Math.round(config.y);
        int right = Math.round(config.x + RotClientHud.WIDTH * config.scale);
        int bottom = Math.round(config.y + hud.currentHeight() * config.scale);
        int outline = target == LayoutTarget.MINING_HUD
                ? RotClientTheme.HUD_ACCENT
                : (hovered ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER);
        RotClientUiDraw.roundedOutline(
                graphics, left - 1, top - 1, right + 1, bottom + 1, outline);
        RotClientUiDraw.text(graphics, font,
                "Mining HUD",
                left,
                Math.max(2, top - 12),
                RotClientTheme.TEXT,
                true);
    }

    private void drawPowderHudEditorChrome(
            GuiGraphicsExtractor graphics,
            boolean hovered) {
        TrackerConfig config = RotClientClient.trackerConfig();
        int left = Math.round(config.powderChestHudX);
        int top = Math.round(config.powderChestHudY);
        int right = Math.round(left
                + PowderChestHud.WIDTH * config.powderChestHudScale);
        int bottom = Math.round(top
                + powderHud.currentHeight() * config.powderChestHudScale);
        int outline = target == LayoutTarget.POWDER_CHEST_HUD
                ? RotClientTheme.HUD_ACCENT
                : (hovered
                ? RotClientTheme.BORDER_BRIGHT
                : RotClientTheme.BORDER);
        RotClientUiDraw.roundedOutline(
                graphics, left - 1, top - 1, right + 1, bottom + 1, outline);
        RotClientUiDraw.text(graphics, font,
                "Powder Chest HUD",
                left,
                Math.max(2, top - 12),
                RotClientTheme.TEXT,
                true);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_1) {
            target = LayoutTarget.MINING_HUD;
            return true;
        }
        if (key == GLFW.GLFW_KEY_2) {
            target = LayoutTarget.POWDER_CHEST_HUD;
            return true;
        }
        if (key == GLFW.GLFW_KEY_3) {
            target = LayoutTarget.CLIENT_UI;
            return true;
        }
        if (key == GLFW.GLFW_KEY_4) {
            target = LayoutTarget.QOL_HUD;
            return true;
        }
        if (key == GLFW.GLFW_KEY_C) {
            if (event.hasShiftDown()) {
                centerSelectedVertically();
            } else if (event.hasControlDown()) {
                centerSelectedHorizontally();
            } else {
                centerSelectedHorizontally();
                centerSelectedVertically();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_H) {
            centerSelectedHorizontally();
            return true;
        }
        if (key == GLFW.GLFW_KEY_V) {
            centerSelectedVertically();
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            if (event.hasShiftDown()) {
                RotClientClient.resetLayoutPositionsFromUi(true, true);
            } else {
                resetSelectedPosition();
            }
            return true;
        }
        if (target == LayoutTarget.MINING_HUD
                || target == LayoutTarget.POWDER_CHEST_HUD
                || target == LayoutTarget.QOL_HUD) {
            if (key == GLFW.GLFW_KEY_LEFT_BRACKET) {
                if (target == LayoutTarget.QOL_HUD) {
                    qolHud.nudgeSelectedScale(-0.1F);
                } else if (target == LayoutTarget.MINING_HUD) {
                    hud.nudgeScale(-0.1F);
                } else {
                    powderHud.nudgeScale(-0.1F);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT_BRACKET) {
                if (target == LayoutTarget.QOL_HUD) {
                    qolHud.nudgeSelectedScale(0.1F);
                } else if (target == LayoutTarget.MINING_HUD) {
                    hud.nudgeScale(0.1F);
                } else {
                    powderHud.nudgeScale(0.1F);
                }
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_B) {
            toggleSelectedBackground();
            return true;
        }
        if (target == LayoutTarget.QOL_HUD && key == GLFW.GLFW_KEY_T) {
            openSelectedHudColor(false);
            return true;
        }
        if (target == LayoutTarget.QOL_HUD && key == GLFW.GLFW_KEY_G) {
            openSelectedHudColor(true);
            return true;
        }
        return super.keyPressed(event);
    }

    private void openSelectedHudColor(boolean background) {
        String id = qolHud.selectedId();
        if (id == null || id.isBlank()) {
            return;
        }
        HudStyleState style = RotClientClient.qolConfigPublic().extras().resolvedHudStyle(id);
        int current = background ? style.backgroundColor : style.textColor;
        String title = background ? "HUD background" : "HUD text";
        this.minecraft.gui.setScreen(new RotClientColorPickerScreen(
                this,
                title,
                current,
                argb -> {
                    HudStyleState next = RotClientClient.qolConfigPublic().extras().resolvedHudStyle(id);
                    if (background) {
                        next.backgroundColor = argb;
                    } else {
                        next.textColor = argb;
                    }
                    RotClientClient.qolConfigPublic().extras().putHudStyle(id, next);
                    RotClientClient.save();
                }));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        HudEditorChromePolicy.Rect inspector = chromeRect(HudEditorChromePolicy.Panel.INSPECTOR);
        if (inspector.containsHeader(mx, my, HudEditorChromePolicy.INSPECTOR_HEADER_HEIGHT)) {
            beginChromeDrag(HudEditorChromePolicy.Panel.INSPECTOR, mx, my);
            return true;
        }
        if (handleInspectorClick(mx, my)) {
            return true;
        }
        HudEditorChromePolicy.Rect title = chromeRect(HudEditorChromePolicy.Panel.TITLE);
        if (title.contains(mx, my)) {
            beginChromeDrag(HudEditorChromePolicy.Panel.TITLE, mx, my);
            return true;
        }
        HudEditorChromePolicy.Rect help = chromeRect(HudEditorChromePolicy.Panel.HELP);
        if (help.contains(mx, my)) {
            beginChromeDrag(HudEditorChromePolicy.Panel.HELP, mx, my);
            return true;
        }
        if (qolHud.beginDrag(event.x(), event.y())) {
            target = LayoutTarget.QOL_HUD;
            return true;
        }
        if (powderHud.beginDrag(event.x(), event.y())) {
            target = LayoutTarget.POWDER_CHEST_HUD;
            return true;
        }
        boolean onHud = hud.containsScreen(event.x(), event.y());
        boolean onUi = RotClientUiDraw.inside(
                mx, my, clientUiGhostX(), clientUiGhostY(),
                CLIENT_UI_GHOST_WIDTH, CLIENT_UI_GHOST_HEIGHT);

        if (onHud) {
            target = LayoutTarget.MINING_HUD;
            if (hud.beginDrag(event.x(), event.y())) {
                return true;
            }
            return true;
        }
        if (onUi) {
            target = LayoutTarget.CLIENT_UI;
            clientUiDragging = true;
            clientUiDragOriginMouseX = mx;
            clientUiDragOriginMouseY = my;
            clientUiDragOriginPanelX = clientUiGhostX();
            clientUiDragOriginPanelY = clientUiGhostY();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void beginChromeDrag(HudEditorChromePolicy.Panel panel, int mouseX, int mouseY) {
        HudEditorChromePolicy.Rect rect = chromeRect(panel);
        chromeDragging = panel;
        chromeDragOffsetX = mouseX - rect.x();
        chromeDragOffsetY = mouseY - rect.y();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && chromeDragging != null) {
            storeChrome(
                    chromeDragging,
                    (int) Math.round(event.x()) - chromeDragOffsetX,
                    (int) Math.round(event.y()) - chromeDragOffsetY);
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && qolHud.dragTo(event.x(), event.y())) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && powderHud.dragTo(event.x(), event.y())) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && hud.dragTo(event.x(), event.y())) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && clientUiDragging) {
            int mouseX = (int) Math.round(event.x());
            int mouseY = (int) Math.round(event.y());
            int nextX = RotClientClientUiLayout.clampPanelX(
                    width,
                    CLIENT_UI_GHOST_WIDTH,
                    clientUiDragOriginPanelX + (mouseX - clientUiDragOriginMouseX));
            int nextY = RotClientClientUiLayout.clampPanelY(
                    height,
                    CLIENT_UI_GHOST_HEIGHT,
                    clientUiDragOriginPanelY + (mouseY - clientUiDragOriginMouseY));
            RotClientClient.workspace().setClientUiNorm(
                    RotClientClientUiLayout.normXFromPanelX(
                            width, CLIENT_UI_GHOST_WIDTH, nextX),
                    RotClientClientUiLayout.normYFromPanelY(
                            height, CLIENT_UI_GHOST_HEIGHT, nextY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && chromeDragging != null) {
            chromeDragging = null;
            RotClientClient.save();
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && qolHud.endDrag()) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && powderHud.endDrag()) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && hud.endDrag()) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && clientUiDragging) {
            clientUiDragging = false;
            RotClientClient.workspace().saveNow();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        HudEditorChromePolicy.Rect inspector = chromeRect(HudEditorChromePolicy.Panel.INSPECTOR);
        if (inspector.contains(mx, my)) {
            inspectorScroll = Math.max(0, inspectorScroll - (int) Math.round(verticalAmount * 18.0D));
            return true;
        }
        if (qolHud.onScroll(mouseX, mouseY, verticalAmount)) {
            target = LayoutTarget.QOL_HUD;
            RotClientClient.save();
            return true;
        }
        if (powderHud.onScroll(mouseX, mouseY, verticalAmount)) {
            target = LayoutTarget.POWDER_CHEST_HUD;
            return true;
        }
        if (hud.onScroll(mouseX, mouseY, verticalAmount)) {
            target = LayoutTarget.MINING_HUD;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }

    private void centerSelectedHorizontally() {
        if (target == LayoutTarget.MINING_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            float scaledW = RotClientHud.WIDTH * config.scale;
            config.x = (width - scaledW) * 0.5F;
            hud.clampToScreen();
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.POWDER_CHEST_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            float scaledW = PowderChestHud.WIDTH
                    * config.powderChestHudScale;
            config.powderChestHudX = (width - scaledW) * 0.5F;
            powderHud.clampToScreen();
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.QOL_HUD) {
            if (qolHud.centerSelectedHorizontally(width)) {
                RotClientClient.save();
            }
            return;
        }
        float normX = RotClientClientUiLayout.normXFromPanelX(
                width,
                CLIENT_UI_GHOST_WIDTH,
                RotClientClientUiLayout.clampPanelX(
                        width,
                        CLIENT_UI_GHOST_WIDTH,
                        (width - CLIENT_UI_GHOST_WIDTH) / 2));
        RotClientClient.workspace().setClientUiNorm(
                normX,
                RotClientClient.workspace().config().clientUiNormY);
        RotClientClient.workspace().saveNow();
    }

    private void centerSelectedVertically() {
        if (target == LayoutTarget.MINING_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            float scaledH = hud.currentHeight() * config.scale;
            config.y = (height - scaledH) * 0.5F;
            hud.clampToScreen();
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.POWDER_CHEST_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            float scaledH = powderHud.currentHeight()
                    * config.powderChestHudScale;
            config.powderChestHudY = (height - scaledH) * 0.5F;
            powderHud.clampToScreen();
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.QOL_HUD) {
            if (qolHud.centerSelectedVertically(height)) {
                RotClientClient.save();
            }
            return;
        }
        float normY = RotClientClientUiLayout.normYFromPanelY(
                height,
                CLIENT_UI_GHOST_HEIGHT,
                RotClientClientUiLayout.clampPanelY(
                        height,
                        CLIENT_UI_GHOST_HEIGHT,
                        (height - CLIENT_UI_GHOST_HEIGHT) / 2));
        RotClientClient.workspace().setClientUiNorm(
                RotClientClient.workspace().config().clientUiNormX,
                normY);
        RotClientClient.workspace().saveNow();
    }

    private void resetSelectedPosition() {
        if (target == LayoutTarget.MINING_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            config.x = 12.0F;
            config.y = 12.0F;
            config.scale = 1.0F;
            hud.clampToScreen();
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.POWDER_CHEST_HUD) {
            TrackerConfig config = RotClientClient.trackerConfig();
            config.powderChestHudX = 292.0F;
            config.powderChestHudY = 12.0F;
            config.powderChestHudScale = 1.0F;
            powderHud.clampToScreen();
            RotClientClient.save();
            return;
        }
        if (target == LayoutTarget.QOL_HUD) {
            if (qolHud.resetSelectedPosition()) {
                RotClientClient.save();
            }
            return;
        }
        RotClientClient.workspace().resetClientUiPosition();
    }

    private int clientUiGhostX() {
        return RotClientClientUiLayout.panelX(
                width,
                CLIENT_UI_GHOST_WIDTH,
                RotClientClient.workspace().config().clientUiNormX);
    }

    private int clientUiGhostY() {
        return RotClientClientUiLayout.panelY(
                height,
                CLIENT_UI_GHOST_HEIGHT,
                RotClientClient.workspace().config().clientUiNormY);
    }
}
