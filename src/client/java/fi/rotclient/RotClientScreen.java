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
    /*
     * Dashboard placement is handled by the dashboard itself. It is not a
     * world HUD element and should not appear in the world HUD editor.
     */
    private static final boolean CLIENT_UI_EDITOR_TARGET_ENABLED = false;

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
            minecraft.setScreen(parent);
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
        boolean hoverUi =
                CLIENT_UI_EDITOR_TARGET_ENABLED
                        && RotClientUiDraw.inside(
                        mx,
                        my,
                        clientUiGhostX(),
                        clientUiGhostY(),
                        CLIENT_UI_GHOST_WIDTH,
                        CLIENT_UI_GHOST_HEIGHT);

        if (CLIENT_UI_EDITOR_TARGET_ENABLED) {
            drawClientUiGhost(
                    graphics,
                    hoverUi);
        }
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

    private HudEditorChromePolicy.Rect chromeRect(
            HudEditorChromePolicy.Panel panel) {

        QolSkyblockExtras extras =
                extras();

        /*
         * The visible editor inspector uses its persisted position so it can
         * be dragged out of the way and remains there on the next open.
         */
        return switch (panel) {
            case TITLE ->
                    HudEditorChromePolicy.resolve(
                            panel,
                            extras.hudEditorTitleX,
                            extras.hudEditorTitleY,
                            this.width,
                            this.height);

            case HELP ->
                    HudEditorChromePolicy.resolve(
                            panel,
                            extras.hudEditorHelpX,
                            extras.hudEditorHelpY,
                            this.width,
                            this.height);

            case INSPECTOR ->
                    HudEditorChromePolicy.resolve(
                            panel,
                            extras.hudEditorInspectorX,
                            extras.hudEditorInspectorY,
                            this.width,
                            this.height);
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

        int x = rect.x();
        int y = rect.y();
        int w = rect.w();
        int h = rect.h();

        RotClientUiDraw.roundedFill(
                graphics,
                x + 2,
                y + 3,
                x + w + 2,
                y + h + 3,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SHADOW,
                        0x58),
                6);

        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + w,
                y + h,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SURFACE,
                        0xF4),
                6);

        RotClientUiDraw.roundedOutline(
                graphics,
                x,
                y,
                x + w,
                y + h,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.BORDER,
                        0xD0),
                6);

        RotClientUiDraw.roundedFill(
                graphics,
                x + 10,
                y,
                x + 72,
                y + 2,
                RotClientTheme.HUD_ACCENT,
                1);

        RotClientUiDraw.text(
                graphics,
                font,
                "HUD EDITOR",
                x + 10,
                y + 8,
                RotClientTheme.TEXT,
                true);

        String selectedText =
                selected == null
                        || selected.isBlank()
                        || selected.startsWith(
                                "No QoL")
                        ? "Click a HUD to edit it"
                        : selected;

        selectedText =
                RotClientUiDraw.ellipsize(
                        font,
                        selectedText,
                        w - 20);

        RotClientUiDraw.text(
                graphics,
                font,
                selectedText,
                x + 10,
                y + 22,
                RotClientTheme.TEXT_DIM,
                false);

        RotClientUiDraw.text(
                graphics,
                font,
                "Drag directly  -  wheel scales",
                x + 10,
                y + 37,
                RotClientTheme.TEXT_MUTED,
                false);

        graphics.fill(
                x + 10,
                y + 52,
                x + w - 10,
                y + 53,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.DIVIDER,
                        0xC0));

        boolean canScale =
                selectedSupportsScale();

        boolean canBackground =
                selectedSupportsBackground();

        boolean canTitle =
                selectedSupportsTitle();

        boolean canTextColor =
                selectedSupportsTextColor();

        boolean canPanelColor =
                selectedSupportsPanelColor();

        boolean canHide =
                selectedCanHide();

        RotClientUiDraw.text(
                graphics,
                font,
                "SCALE",
                x + 10,
                y + 59,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 70,
                30,
                20,
                "-",
                false,
                canScale);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 44,
                y + 70,
                66,
                20,
                Math.round(
                        selectedScale()
                                * 100.0F)
                        + "%",
                false,
                canScale);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 114,
                y + 70,
                30,
                20,
                "+",
                false,
                canScale);

        RotClientUiDraw.text(
                graphics,
                font,
                "POSITION",
                x + 10,
                y + 96,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 107,
                96,
                20,
                "Center",
                false,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 112,
                y + 107,
                96,
                20,
                "Reset",
                false,
                true);

        RotClientUiDraw.text(
                graphics,
                font,
                "STYLE",
                x + 10,
                y + 133,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 144,
                48,
                20,
                "BG",
                selectedBackgroundOn(),
                canBackground);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 62,
                y + 144,
                48,
                20,
                "Title",
                selectedTitleOn(),
                canTitle);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 114,
                y + 144,
                48,
                20,
                "Text",
                false,
                canTextColor);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 166,
                y + 144,
                48,
                20,
                "Panel",
                false,
                canPanelColor);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 171,
                64,
                20,
                "Hide",
                false,
                canHide);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 78,
                y + 171,
                64,
                20,
                "Undo",
                false,
                visibilityUndo.canUndo());

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 146,
                y + 171,
                64,
                20,
                "Redo",
                false,
                visibilityUndo.canRedo());

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 197,
                100,
                20,
                "Reset all",
                false,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 114,
                y + 197,
                100,
                20,
                "Done",
                true,
                true);
    }

    private static boolean editorButtonHit(
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height) {

        return RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x,
                y,
                width,
                height);
    }

    private boolean handleEditorPanelClick(
            int mouseX,
            int mouseY) {

        HudEditorChromePolicy.Rect rect =
                chromeRect(
                        HudEditorChromePolicy.Panel.TITLE);

        if (!rect.contains(
                mouseX,
                mouseY)) {

            return false;
        }

        int x = rect.x();
        int y = rect.y();

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 10,
                y + 70,
                30,
                20)
                && selectedSupportsScale()) {

            adjustSelectedScale(
                    -0.1F);

            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 44,
                y + 70,
                66,
                20)
                && selectedSupportsScale()) {

            adjustSelectedScale(
                    1.0F
                            - selectedScale());

            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 114,
                y + 70,
                30,
                20)
                && selectedSupportsScale()) {

            adjustSelectedScale(
                    0.1F);

            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 10,
                y + 107,
                96,
                20)) {

            centerSelectedHorizontally();
            centerSelectedVertically();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 112,
                y + 107,
                96,
                20)) {

            resetSelectedPosition();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 10,
                y + 144,
                48,
                20)
                && selectedSupportsBackground()) {

            toggleSelectedBackground();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 62,
                y + 144,
                48,
                20)
                && selectedSupportsTitle()) {

            toggleSelectedTitleFromPanel();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 114,
                y + 144,
                48,
                20)
                && selectedSupportsTextColor()) {

            openSelectedHudColor(
                    false);

            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 166,
                y + 144,
                48,
                20)
                && selectedSupportsPanelColor()) {

            openSelectedHudColor(
                    true);

            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 10,
                y + 171,
                64,
                20)
                && selectedCanHide()) {

            hideSelected();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 78,
                y + 171,
                64,
                20)
                && visibilityUndo.canUndo()) {

            applyVisibilityUndo();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 146,
                y + 171,
                64,
                20)
                && visibilityUndo.canRedo()) {

            applyVisibilityRedo();
            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 10,
                y + 197,
                100,
                20)) {

            RotClientClient
                    .resetLayoutPositionsFromUi(
                            true,
                            true);

            hud.clampToScreen();
            powderHud.clampToScreen();
            RotClientClient.save();

            return true;
        }

        if (editorButtonHit(
                mouseX,
                mouseY,
                x + 114,
                y + 197,
                100,
                20)) {

            onClose();
            return true;
        }

        /*
         * The inspector itself always consumes clicks so a HUD underneath the
         * card cannot accidentally start moving.
         */
        return true;
    }

    private boolean selectedSupportsScale() {
        if (target == LayoutTarget.CLIENT_UI) {
            return false;
        }

        if (target != LayoutTarget.QOL_HUD) {
            return true;
        }

        String id =
                qolHud.selectedId();

        return id != null
                && !id.isBlank();
    }

    private float selectedScale() {
        if (target == LayoutTarget.MINING_HUD) {
            return RotClientClient
                    .trackerConfig()
                    .scale;
        }

        if (target
                == LayoutTarget.POWDER_CHEST_HUD) {

            return RotClientClient
                    .trackerConfig()
                    .powderChestHudScale;
        }

        if (target == LayoutTarget.QOL_HUD) {
            String id =
                    qolHud.selectedId();

            if (id != null
                    && !id.isBlank()) {

                return RotClientClient
                        .qolConfigPublic()
                        .extras()
                        .resolvedHudStyle(
                                id)
                        .scale;
            }
        }

        return 1.0F;
    }

    private void adjustSelectedScale(
            float delta) {

        if (!selectedSupportsScale()
                || Math.abs(delta)
                < 0.0001F) {

            return;
        }

        if (target == LayoutTarget.MINING_HUD) {
            hud.nudgeScale(
                    delta);
        } else if (target
                == LayoutTarget.POWDER_CHEST_HUD) {

            powderHud.nudgeScale(
                    delta);
        } else if (target
                == LayoutTarget.QOL_HUD) {

            qolHud.nudgeSelectedScale(
                    delta);
        }

        RotClientClient.save();
    }

    private boolean selectedSupportsBackground() {
        if (target == LayoutTarget.MINING_HUD
                || target
                == LayoutTarget.POWDER_CHEST_HUD) {

            return true;
        }

        if (target != LayoutTarget.QOL_HUD) {
            return false;
        }

        String id =
                qolHud.selectedId();

        return id != null
                && !id.isBlank()
                && !CustomScoreboardPolicy.POSE_ID
                .equals(id);
    }

    private boolean selectedBackgroundOn() {
        if (target == LayoutTarget.MINING_HUD) {
            return RotClientClient
                    .trackerConfig()
                    .hudShowBackground;
        }

        if (target
                == LayoutTarget.POWDER_CHEST_HUD) {

            return RotClientClient
                    .trackerConfig()
                    .powderChestHudShowBackground;
        }

        if (target == LayoutTarget.QOL_HUD) {
            String id =
                    qolHud.selectedId();

            if (id != null
                    && !id.isBlank()) {

                return RotClientClient
                        .qolConfigPublic()
                        .extras()
                        .resolvedHudStyle(
                                id)
                        .showBackground;
            }
        }

        return false;
    }

    private boolean selectedSupportsTitle() {
        if (target == LayoutTarget.MINING_HUD) {
            return true;
        }

        if (target != LayoutTarget.QOL_HUD) {
            return false;
        }

        String id =
                qolHud.selectedId();

        if (id == null
                || id.isBlank()) {

            return false;
        }

        return switch (id) {
            case "commission",
                    "fishing",
                    "mining",
                    "diana",
                    "foraging",
                    "iota_arrows",
                    "kuudra_alerts",
                    "stall_bin" ->
                    true;

            default ->
                    id.startsWith(
                            "slayer")
                            || id.startsWith(
                            "dungeon");
        };
    }

    private boolean selectedTitleOn() {
        if (target == LayoutTarget.MINING_HUD) {
            return RotClientClient
                    .trackerConfig()
                    .showHudTitle;
        }

        if (target == LayoutTarget.QOL_HUD
                && selectedSupportsTitle()) {

            String id =
                    qolHud.selectedId();

            return HudStylePolicy
                    .titleVisible(
                            RotClientClient
                                    .qolConfigPublic()
                                    .extras()
                                    .resolvedHudStyle(
                                            id));
        }

        return false;
    }

    private void toggleSelectedTitleFromPanel() {
        if (target == LayoutTarget.MINING_HUD) {
            TrackerConfig config =
                    RotClientClient
                            .trackerConfig();

            config.showHudTitle =
                    !config.showHudTitle;

            RotClientClient.save();
            return;
        }

        if (target == LayoutTarget.QOL_HUD
                && selectedSupportsTitle()) {

            qolHud.toggleSelectedTitle();
            RotClientClient.save();
        }
    }

    private boolean selectedSupportsTextColor() {
        if (target != LayoutTarget.QOL_HUD) {
            return false;
        }

        String id =
                qolHud.selectedId();

        if (id == null
                || id.isBlank()) {

            return false;
        }

        return switch (id) {
            case "performance",
                    "health",
                    "mana",
                    "overflow",
                    "defense",
                    "vitality",
                    "ehp",
                    "speed",
                    "pet",
                    "custom_scoreboard" ->
                    false;

            default ->
                    true;
        };
    }

    private boolean selectedSupportsPanelColor() {
        if (target != LayoutTarget.QOL_HUD) {
            return false;
        }

        String id =
                qolHud.selectedId();

        return id != null
                && !id.isBlank()
                && !CustomScoreboardPolicy.POSE_ID
                .equals(id);
    }

    private boolean selectedCanHide() {
        if (target == LayoutTarget.MINING_HUD) {
            return RotClientClient
                    .trackerConfig()
                    .enabled;
        }

        if (target
                == LayoutTarget.POWDER_CHEST_HUD) {

            return RotClientClient
                    .trackerConfig()
                    .powderChestHudEnabled;
        }

        if (target == LayoutTarget.QOL_HUD) {
            String id =
                    qolHud.selectedId();

            return id != null
                    && !id.isBlank()
                    && HudLayoutLandingPolicy
                    .disableForPose(
                            id)
                    != null;
        }

        return false;
    }

    private void hideSelected() {
        if (target == LayoutTarget.MINING_HUD) {
            if (RotClientClient
                    .trackerConfig()
                    .enabled) {

                visibilityUndo.pushHide(
                        HudEditorVisibilityUndo
                                .Entry
                                .miningTracker());

                RotClientClient
                        .setTrackerEnabled(
                                false);
            }

            return;
        }

        if (target
                == LayoutTarget.POWDER_CHEST_HUD) {

            if (RotClientClient
                    .trackerConfig()
                    .powderChestHudEnabled) {

                visibilityUndo.pushHide(
                        HudEditorVisibilityUndo
                                .Entry
                                .powderChest());

                RotClientClient
                        .setPowderChestHudEnabled(
                                false);
            }

            return;
        }

        if (target != LayoutTarget.QOL_HUD) {
            return;
        }

        String id =
                qolHud.selectedId();

        if (id == null
                || id.isBlank()) {

            return;
        }

        HudLayoutLandingPolicy.Disable spec =
                HudLayoutLandingPolicy
                        .disableForPose(
                                id);

        if (spec == null) {
            return;
        }

        if (HudLayoutLandingPolicy.hide(
                RotClientClient.qolConfigPublic(),
                spec.settingId(),
                spec.moduleToggle())) {

            visibilityUndo.pushHide(
                    HudEditorVisibilityUndo
                            .Entry
                            .qol(
                                    spec.settingId(),
                                    spec.moduleToggle()));

            RotClientClient.save();

            /*
             * Select the next visible QoL element rather than leaving controls
             * attached to an overlay that just disappeared.
             */
            qolHud.setEditorOpen(
                    true);
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

    private void drawClientUiGhost(
            GuiGraphicsExtractor graphics,
            boolean hovered) {

        int ghostX =
                clientUiGhostX();

        int ghostY =
                clientUiGhostY();

        boolean selected =
                target
                        == LayoutTarget.CLIENT_UI;

        RotClientUiDraw.roundedFill(
                graphics,
                ghostX,
                ghostY,
                ghostX + CLIENT_UI_GHOST_WIDTH,
                ghostY + CLIENT_UI_GHOST_HEIGHT,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SURFACE,
                        selected
                                ? 0x34
                                : 0x1C),
                6);

        RotClientUiDraw.roundedFill(
                graphics,
                ghostX,
                ghostY,
                ghostX + CLIENT_UI_GHOST_WIDTH,
                ghostY + 34,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.HUD_ACCENT,
                        selected
                                ? 0x58
                                : 0x2C),
                6);

        RotClientUiDraw.text(
                graphics,
                font,
                "Client UI",
                ghostX + 12,
                ghostY + 12,
                selected
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_MUTED,
                true);

        int outline =
                selected
                        ? RotClientTheme.HUD_ACCENT
                        : hovered
                        ? RotClientUiDraw.withAlpha(
                                RotClientTheme.BORDER_BRIGHT,
                                0xB0)
                        : RotClientUiDraw.withAlpha(
                                RotClientTheme.BORDER,
                                0x58);

        RotClientUiDraw.roundedOutline(
                graphics,
                ghostX - 1,
                ghostY - 1,
                ghostX
                        + CLIENT_UI_GHOST_WIDTH
                        + 1,
                ghostY
                        + CLIENT_UI_GHOST_HEIGHT
                        + 1,
                outline,
                6);

        if (selected) {
            drawSelectionTag(
                    graphics,
                    "Client UI",
                    ghostX,
                    ghostY,
                    CLIENT_UI_GHOST_WIDTH);
        }
    }

    private void drawSelectionTag(
            GuiGraphicsExtractor graphics,
            String label,
            int left,
            int top,
            int maxWidth) {

        String text =
                RotClientUiDraw.ellipsize(
                        font,
                        label == null
                                ? ""
                                : label,
                        Math.max(
                                40,
                                maxWidth - 12));

        int textWidth =
                RotClientFonts.width(
                        font,
                        text);

        int tagTop =
                Math.max(
                        2,
                        top - 13);

        RotClientUiDraw.roundedFill(
                graphics,
                left,
                tagTop,
                left + textWidth + 10,
                tagTop + 11,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SURFACE,
                        0xEE),
                3);

        RotClientUiDraw.roundedOutline(
                graphics,
                left,
                tagTop,
                left + textWidth + 10,
                tagTop + 11,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.HUD_ACCENT,
                        0xB8),
                3);

        RotClientUiDraw.text(
                graphics,
                font,
                text,
                left + 5,
                tagTop + 2,
                RotClientTheme.TEXT,
                true);
    }
    private void drawHudEditorChrome(
            GuiGraphicsExtractor graphics,
            boolean hovered) {

        TrackerConfig config =
                RotClientClient
                        .trackerConfig();

        int left =
                Math.round(
                        config.x);

        int top =
                Math.round(
                        config.y);

        int right =
                Math.round(
                        config.x
                                + RotClientHud.WIDTH
                                * config.scale);

        int bottom =
                Math.round(
                        config.y
                                + hud.currentHeight()
                                * config.scale);

        boolean selected =
                target
                        == LayoutTarget.MINING_HUD;

        int outline =
                selected
                        ? RotClientTheme.HUD_ACCENT
                        : hovered
                        ? RotClientUiDraw.withAlpha(
                                RotClientTheme.BORDER_BRIGHT,
                                0xB0)
                        : RotClientUiDraw.withAlpha(
                                RotClientTheme.BORDER,
                                0x58);

        RotClientUiDraw.roundedOutline(
                graphics,
                left - 2,
                top - 2,
                right + 2,
                bottom + 2,
                outline,
                5);

        if (selected) {
            drawSelectionTag(
                    graphics,
                    "Mining HUD",
                    left,
                    top,
                    Math.max(
                            80,
                            right - left));
        }
    }
    private void drawPowderHudEditorChrome(
            GuiGraphicsExtractor graphics,
            boolean hovered) {

        TrackerConfig config =
                RotClientClient
                        .trackerConfig();

        int left =
                Math.round(
                        config.powderChestHudX);

        int top =
                Math.round(
                        config.powderChestHudY);

        int right =
                Math.round(
                        left
                                + PowderChestHud.WIDTH
                                * config.powderChestHudScale);

        int bottom =
                Math.round(
                        top
                                + powderHud.currentHeight()
                                * config.powderChestHudScale);

        boolean selected =
                target
                        == LayoutTarget.POWDER_CHEST_HUD;

        int outline =
                selected
                        ? RotClientTheme.HUD_ACCENT
                        : hovered
                        ? RotClientUiDraw.withAlpha(
                                RotClientTheme.BORDER_BRIGHT,
                                0xB0)
                        : RotClientUiDraw.withAlpha(
                                RotClientTheme.BORDER,
                                0x58);

        RotClientUiDraw.roundedOutline(
                graphics,
                left - 2,
                top - 2,
                right + 2,
                bottom + 2,
                outline,
                5);

        if (selected) {
            drawSelectionTag(
                    graphics,
                    "Powder Chest HUD",
                    left,
                    top,
                    Math.max(
                            100,
                            right - left));
        }
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
        if (key == GLFW.GLFW_KEY_3
                || key == GLFW.GLFW_KEY_4) {

            target =
                    LayoutTarget.QOL_HUD;

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
        this.minecraft.setScreen(new RotClientColorPickerScreen(
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
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick) {

        int mx =
                (int) Math.round(
                        event.x());

        int my =
                (int) Math.round(
                        event.y());

        if (event.button()
                == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {

            return handleEditorRightClick(
                    mx,
                    my);
        }

        if (event.button()
                != GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            return super.mouseClicked(
                    event,
                    doubleClick);
        }

        if (RotClientUiDraw.hitBackButton(
                mx,
                my,
                12,
                12)) {

            onClose();
            return true;
        }

        HudEditorChromePolicy.Rect editorPanel =
                chromeRect(
                        HudEditorChromePolicy.Panel.TITLE);

        /*
         * The top 54px contain only title/help text and therefore form a safe
         * drag handle. Controls start below this area.
         */
        if (editorPanel.containsHeader(
                mx,
                my,
                54)) {

            beginChromeDrag(
                    HudEditorChromePolicy.Panel.TITLE,
                    mx,
                    my);

            return true;
        }

        if (handleEditorPanelClick(
                mx,
                my)) {

            return true;
        }

        /*
         * Direct manipulation is the primary interaction now: clicking the
         * actual HUD selects it and immediately starts a drag.
         */
        if (qolHud.beginDrag(
                event.x(),
                event.y())) {

            target =
                    LayoutTarget.QOL_HUD;

            return true;
        }

        if (powderHud.beginDrag(
                event.x(),
                event.y())) {

            target =
                    LayoutTarget.POWDER_CHEST_HUD;

            return true;
        }

        boolean onHud =
                hud.containsScreen(
                        event.x(),
                        event.y());

        boolean onUi =
                CLIENT_UI_EDITOR_TARGET_ENABLED
                        && RotClientUiDraw.inside(
                        mx,
                        my,
                        clientUiGhostX(),
                        clientUiGhostY(),
                        CLIENT_UI_GHOST_WIDTH,
                        CLIENT_UI_GHOST_HEIGHT);

        if (onHud) {
            target =
                    LayoutTarget.MINING_HUD;

            hud.beginDrag(
                    event.x(),
                    event.y());

            return true;
        }

        if (onUi) {
            target =
                    LayoutTarget.CLIENT_UI;

            clientUiDragging =
                    true;

            clientUiDragOriginMouseX =
                    mx;

            clientUiDragOriginMouseY =
                    my;

            clientUiDragOriginPanelX =
                    clientUiGhostX();

            clientUiDragOriginPanelY =
                    clientUiGhostY();

            return true;
        }

        return super.mouseClicked(
                event,
                doubleClick);
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
        if (CLIENT_UI_EDITOR_TARGET_ENABLED
                && RotClientUiDraw.inside(
                mx,
                my,
                clientUiGhostX(),
                clientUiGhostY(),
                CLIENT_UI_GHOST_WIDTH,
                CLIENT_UI_GHOST_HEIGHT)) {

            target =
                    LayoutTarget.CLIENT_UI;

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
        boolean overClientUi =
                CLIENT_UI_EDITOR_TARGET_ENABLED
                        && RotClientUiDraw.inside(
                        mx,
                        my,
                        clientUiGhostX(),
                        clientUiGhostY(),
                        CLIENT_UI_GHOST_WIDTH,
                        CLIENT_UI_GHOST_HEIGHT);

        if (chromeRect(
                HudEditorChromePolicy.Panel.TITLE)
                .contains(
                        mx,
                        my)
                || overClientUi) {

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
