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
    private final HudEditorVisibilityUndo visibilityUndo = new HudEditorVisibilityUndo();

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
        if ("mining_tracker".equals(this.initialFocusId)) {
            this.target = LayoutTarget.MINING_HUD;
        } else if ("powder_chest".equals(this.initialFocusId)) {
            this.target = LayoutTarget.POWDER_CHEST_HUD;
        } else if (!this.initialFocusId.isEmpty()) {
            this.target = LayoutTarget.QOL_HUD;
        }
    }

    @Override
    protected void init() {
        hud.setEditorOpen(true);
        powderHud.setEditorOpen(true);
        qolHud.setEditorOpen(true);
        if (target == LayoutTarget.QOL_HUD) {
            qolHud.setFocusId(initialFocusId);
        } else {
            qolHud.setFocusId("");
        }
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
        boolean hoverHud = HudEditorPreviewPolicy.showMiningTracker(
                RotClientClient.trackerConfig().enabled)
                && hud.containsScreen(mouseX, mouseY);
        boolean hoverPowderHud = HudEditorPreviewPolicy.showPowderChest(
                RotClientClient.trackerConfig().powderChestHudEnabled)
                && powderHud.containsScreen(mouseX, mouseY);
        boolean hoverUi = RotClientUiDraw.inside(
                mx, my, clientUiGhostX(), clientUiGhostY(),
                CLIENT_UI_GHOST_WIDTH, CLIENT_UI_GHOST_HEIGHT);

        drawClientUiGhost(graphics, hoverUi);
        TrackerConfig tracker = RotClientClient.trackerConfig();
        if (HudEditorPreviewPolicy.showMiningTracker(tracker.enabled)) {
            drawHudEditorChrome(graphics, hoverHud);
        }
        if (HudEditorPreviewPolicy.showPowderChest(tracker.powderChestHudEnabled)) {
            drawPowderHudEditorChrome(graphics, hoverPowderHud);
        }
        qolHud.render(graphics);

        RotClientUiDraw.drawBackButton(graphics, font, mx, my, 12, 12);

        String selected = selectedLabel();
        HudEditorChromePolicy.Rect title = chromeRect(HudEditorChromePolicy.Panel.TITLE);
        drawEditorChromeCard(graphics, title, selected, mx, my);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

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

    private void drawEditorChromeCard(
            GuiGraphicsExtractor graphics,
            HudEditorChromePolicy.Rect rect,
            String selected,
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
        RotClientUiDraw.text(
                graphics,
                font,
                "HUD ELEMENTS EDITOR",
                rect.x() + 10,
                rect.y() + 8,
                RotClientTheme.TEXT,
                true);
        RotClientUiDraw.text(
                graphics,
                font,
                "Selected: " + selected,
                rect.x() + 10,
                rect.y() + 22,
                RotClientTheme.TEXT_DIM,
                false);
        RotClientUiDraw.text(
                graphics,
                font,
                "How to edit",
                rect.x() + 10,
                rect.y() + 38,
                RotClientTheme.TEXT,
                true);
        int row = rect.y() + 52;
        for (String line : HudEditorChromePolicy.helpLines()) {
            RotClientUiDraw.text(
                    graphics, font, line, rect.x() + 10, row, RotClientTheme.TEXT_DIM, false);
            row += 12;
        }
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
        if (event.hasControlDown() && key == GLFW.GLFW_KEY_Z && event.hasShiftDown()) {
            applyVisibilityRedo();
            return true;
        }
        if (event.hasControlDown() && key == GLFW.GLFW_KEY_Z) {
            applyVisibilityUndo();
            return true;
        }
        if (event.hasControlDown() && key == GLFW.GLFW_KEY_Y) {
            applyVisibilityRedo();
            return true;
        }
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
        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return handleEditorRightClick(mx, my);
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        if (RotClientUiDraw.hitBackButton(mx, my, 12, 12)) {
            onClose();
            return true;
        }
        HudEditorChromePolicy.Rect title = chromeRect(HudEditorChromePolicy.Panel.TITLE);
        if (title.contains(mx, my)) {
            beginChromeDrag(HudEditorChromePolicy.Panel.TITLE, mx, my);
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

    private boolean handleEditorRightClick(int mx, int my) {
        if (RotClientUiDraw.hitBackButton(mx, my, 12, 12)) {
            onClose();
            return true;
        }
        HudEditorChromePolicy.Rect title = chromeRect(HudEditorChromePolicy.Panel.TITLE);
        if (title.contains(mx, my)) {
            return true;
        }
        if (RotClientUiDraw.inside(
                mx, my, clientUiGhostX(), clientUiGhostY(),
                CLIENT_UI_GHOST_WIDTH, CLIENT_UI_GHOST_HEIGHT)) {
            target = LayoutTarget.CLIENT_UI;
            return true;
        }
        String qolId = qolHud.elementAt(mx, my);
        if (qolId != null && !qolId.isBlank()) {
            HudLayoutLandingPolicy.Disable spec = HudLayoutLandingPolicy.disableForPose(qolId);
            if (spec != null
                    && HudLayoutLandingPolicy.hide(
                            RotClientClient.qolConfigPublic(), spec.settingId(), spec.moduleToggle())) {
                visibilityUndo.pushHide(
                        HudEditorVisibilityUndo.Entry.qol(spec.settingId(), spec.moduleToggle()));
                RotClientClient.save();
            }
            target = LayoutTarget.QOL_HUD;
            return true;
        }
        if (powderHud.containsScreen(mx, my)) {
            if (RotClientClient.trackerConfig().powderChestHudEnabled) {
                visibilityUndo.pushHide(HudEditorVisibilityUndo.Entry.powderChest());
                RotClientClient.setPowderChestHudEnabled(false);
            }
            target = LayoutTarget.POWDER_CHEST_HUD;
            return true;
        }
        if (hud.containsScreen(mx, my)) {
            if (RotClientClient.trackerConfig().enabled) {
                visibilityUndo.pushHide(HudEditorVisibilityUndo.Entry.miningTracker());
                RotClientClient.setTrackerEnabled(false);
            }
            target = LayoutTarget.MINING_HUD;
            return true;
        }
        return true;
    }

    private void applyVisibilityUndo() {
        applyVisibilityEntry(visibilityUndo.undo(), true);
    }

    private void applyVisibilityRedo() {
        applyVisibilityEntry(visibilityUndo.redo(), false);
    }

    private void applyVisibilityEntry(HudEditorVisibilityUndo.Entry entry, boolean restore) {
        if (entry == null) {
            return;
        }
        switch (entry.kind()) {
            case MINING_TRACKER -> RotClientClient.setTrackerEnabled(restore);
            case POWDER_CHEST -> RotClientClient.setPowderChestHudEnabled(restore);
            case QOL -> {
                if (restore) {
                    HudLayerTogglePolicy.enable(
                            RotClientClient.qolConfigPublic(),
                            entry.settingId(),
                            entry.moduleToggle());
                } else {
                    HudLayerTogglePolicy.disable(
                            RotClientClient.qolConfigPublic(),
                            entry.settingId(),
                            entry.moduleToggle());
                }
                RotClientClient.save();
            }
        }
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
        if (chromeRect(HudEditorChromePolicy.Panel.TITLE).contains(mx, my)
                || RotClientUiDraw.inside(
                        mx, my, clientUiGhostX(), clientUiGhostY(),
                        CLIENT_UI_GHOST_WIDTH, CLIENT_UI_GHOST_HEIGHT)) {
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
