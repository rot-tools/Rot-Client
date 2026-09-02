package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders movable Performance HUD + Player Display HUD elements and supports
 * drag inside the existing Rot HUD editor / pause editor mode.
 */
final class QolOverlayHud {
    private static final int PERFORMANCE_EDITOR_WIDTH = 120;
    private static final int PERFORMANCE_EDITOR_HEIGHT = 36;
    private static final int STAT_EDITOR_WIDTH = 110;
    private static final int STAT_EDITOR_HEIGHT = 14;
    private static final int PET_EDITOR_WIDTH = 180;
    private static final int PET_EDITOR_HEIGHT = 36;
    private static final int SLAYER_EDITOR_WIDTH = 156;

    private final TrackerConfig config;
    private final PerformanceMetricsSampler metrics = new PerformanceMetricsSampler();
    private final SkyBlockStatTracker stats = new SkyBlockStatTracker();
    private boolean editorOpen;
    private String draggingId = "";
    private String selectedId = "";
    private String focusId = "";
    private double dragOffsetX;
    private double dragOffsetY;

    QolOverlayHud(TrackerConfig config) {
        this.config = config;
    }

    SkyBlockStatTracker statsTracker() {
        return stats;
    }

    void setEditorOpen(boolean open) {
        editorOpen = open;
        if (!open) {
            draggingId = "";
            focusId = "";
        } else if (!isVisibleElement(selectedId)) {
            selectedId = firstVisibleElementId();
        }
    }

    void setFocusId(String id) {
        focusId = id == null ? "" : id.trim();
        if (!focusId.isEmpty() && isVisibleElement(focusId)) {
            selectedId = focusId;
        }
    }

    boolean editorOpen() {
        return editorOpen;
    }

    void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || RotClientClient.pauseMenuHidesHud()) {
            return;
        }
        PrizeSpinRuntime.renderHud(graphics);
        QolUtilityConfig qol = qol();
        Font font = client.font;
        long now = System.currentTimeMillis();

        if (qol.performanceHudEnabled) {
            renderPerformance(graphics, font, qol, metrics.sample(client, now));
        }
        if (qol.playerDisplayEnabled) {
            renderPlayerDisplay(graphics, font, qol);
        }
        if (qol.petHudEnabled) {
            renderPetHud(graphics, font, qol);
        }
        if (qol.commissionDisplayEnabled) {
            renderCommission(graphics, font, qol);
        }
        if (qol.wardrobeKeybindsEnabled || qol.extras().cheaterWardrobeEnabled) {
            renderWardrobe(graphics, font, qol);
        }
        if (qol.autoClickerCpsHudEnabled) {
            renderAutoClickerHud(graphics, font, qol);
        }
        renderFishing(graphics, font, qol);
        renderMiningLeftover(graphics, font, qol);
        renderDiana(graphics, font, qol);
        renderForaging(graphics, font, qol);
        renderIotaArrows(graphics, font, qol);
        renderKuudraAlerts(graphics, font, qol);
        renderStallBin(graphics, font, qol);
        int coldAlpha = MiningLeftoverRuntime.coldAlpha();
        if (coldAlpha > 0) {
            graphics.fill(
                    0,
                    0,
                    client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight(),
                    RotClientUiDraw.withAlpha(0xFF88CCFF, coldAlpha));
        }
        String reel = FishingSuiteRuntime.overlayTitle();
        if (!reel.isBlank()) {
            int w = font.width(reel);
            int x = (client.getWindow().getGuiScaledWidth() - w) / 2;
            RotClientUiDraw.text(graphics, font, reel, x, 28, 0xFF55FF55, true);
        }
        String miningTitle = MiningLeftoverRuntime.overlayTitle();
        if (!miningTitle.isBlank()) {
            int w = font.width(miningTitle);
            int x = (client.getWindow().getGuiScaledWidth() - w) / 2;
            RotClientUiDraw.text(graphics, font, miningTitle, x, 42, 0xFFFFAA00, true);
        }
        String dianaTitle = DianaRuntime.overlayTitle();
        if (!dianaTitle.isBlank()) {
            int w = font.width(dianaTitle);
            int x = (client.getWindow().getGuiScaledWidth() - w) / 2;
            RotClientUiDraw.text(graphics, font, dianaTitle, x, 56, 0xFFFF55FF, true);
        }
        String foragingTitle = ForagingRuntime.overlayTitle();
        if (!foragingTitle.isBlank()) {
            int w = font.width(foragingTitle);
            int x = (client.getWindow().getGuiScaledWidth() - w) / 2;
            RotClientUiDraw.text(graphics, font, foragingTitle, x, 70, 0xFF55AA55, true);
        }
        String iotaTitle = IotaRuntime.overlayTitle();
        if (iotaTitle.isBlank()) {
            iotaTitle = IotaKuudraRuntime.overlayTitle();
        }
        if (!iotaTitle.isBlank()) {
            int w = font.width(iotaTitle);
            int x = (client.getWindow().getGuiScaledWidth() - w) / 2;
            RotClientUiDraw.text(graphics, font, iotaTitle, x, 84, 0xFFFF5555, true);
        }
        renderSlayerPanel(graphics, font, qol, "slayer", SlayerRuntime.displayLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_progress", SlayerRuntime.progressLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_rng", SlayerRuntime.rngLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_profit", SlayerRuntime.profitLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_stats", SlayerRuntime.statsLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_carry", SlayerRuntime.carryLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_cocoon", SlayerRuntime.cocoonLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_attunement", SlayerRuntime.attunementLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "slayer_vengeance", SlayerRuntime.vengeanceLines(editorOpen));
        renderSlayerPanel(graphics, font, qol, "dungeon", DungeonRuntime.displayLines(editorOpen));
        ItemRarityRuntime.renderHotbar(
                graphics,
                client.getWindow() == null ? 0 : client.getWindow().getGuiScaledWidth(),
                client.getWindow() == null ? 0 : client.getWindow().getGuiScaledHeight());
    }

    private void renderPerformance(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol,
            PerformanceHudLayout.Snapshot snapshot) {
        List<String> lines = PerformanceHudLayout.renderLines(
                snapshot,
                qol.performanceShowFps,
                qol.performanceShowTps,
                qol.performanceShowPing,
                PerformanceHudLayout.Direction.fromConfig(qol.performanceDirection));
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        float[] pose = qol.pose("performance");
        int x = Math.round(pose[0]);
        int y = Math.round(pose[1]);
        int width = Math.max(80, PerformanceHudLayout.estimateWidth(lines, 6) + 8);
        int height = Math.max(18, PerformanceHudLayout.estimateHeight(lines, 10));
        pushHudScale(graphics, "performance", x, y);
        fillHudPanel(graphics, "performance", x, y, width, height);
        if (editorOpen) {
            drawEditorFrame(
                    graphics,
                    font,
                    x,
                    y,
                    Math.max(PERFORMANCE_EDITOR_WIDTH, width),
                    Math.max(PERFORMANCE_EDITOR_HEIGHT, height),
                    "performance");
            RotClientUiDraw.text(graphics, font, "Performance HUD", x + 4, y + 2, RotClientTheme.TEXT_MUTED, false);
            y += 12;
        }
        int rowY = y + 4;
        for (String line : lines) {
            drawNamedValueLine(
                    graphics,
                    font,
                    x + 4,
                    rowY,
                    line,
                    qol.performanceNameColor,
                    qol.performanceValueColor);
            rowY += 10;
        }
        if (lines.isEmpty() && editorOpen) {
            RotClientUiDraw.text(graphics, font, "(no metrics enabled)", x + 4, rowY,
                    RotClientTheme.TEXT_MUTED, false);
        }
        graphics.pose().popMatrix();
    }

    private void renderPlayerDisplay(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<PlayerDisplayMath.HudLine> lines = PlayerDisplayMath.visibleLines(
                stats.stats(),
                qol.playerDisplayHealthHud,
                qol.playerDisplayManaHud,
                qol.playerDisplayOverflowManaHud,
                qol.playerDisplayDefenseHud,
                qol.playerDisplayVitalityHud,
                qol.playerDisplayEhpHud,
                qol.playerDisplaySpeedHud);
        for (PlayerDisplayMath.HudLine line : lines) {
            String id = elementId(line.kind());
            float[] pose = qol.pose(id);
            int x = Math.round(pose[0]);
            int y = Math.round(pose[1]);
            int textWidth;
            pushHudScale(graphics, id, x, y);
            fillHudPanel(
                    graphics,
                    id,
                    x - 3,
                    y - 1,
                    Math.max(STAT_EDITOR_WIDTH, 96),
                    STAT_EDITOR_HEIGHT + 2);
            if (panelOn(id)) {
                graphics.fill(x - 3, y - 1, x, y + STAT_EDITOR_HEIGHT + 1, RotClientTheme.HUD_ACCENT);
            }
            String value = PlayerDisplayMath.withMax(line.value(), qol.playerDisplayShowMax);
            String text = PlayerDisplayMath.composeLine(
                    line.kind(),
                    value,
                    qol.playerDisplayShowIcons,
                    qol.playerDisplayShowLabels);
            int color = textPaint(id, colorFor(line.kind(), qol));
            if (qol.playerDisplayShowIcons) {
                String icon = PlayerDisplayMath.iconFor(line.kind());
                String rest = text.startsWith(icon)
                        ? text.substring(icon.length()).trim()
                        : PlayerDisplayMath.composeLine(
                                line.kind(), value, false, qol.playerDisplayShowLabels);
                RotClientUiDraw.vanillaText(graphics, font, icon, x, y + 2, color);
                int iconWidth = RotClientFonts.vanillaWidth(font, icon + (rest.isBlank() ? "" : " "));
                if (!rest.isBlank()) {
                    RotClientUiDraw.text(graphics, font, rest, x + iconWidth, y + 2, color, true);
                }
                textWidth = iconWidth + (rest.isBlank() ? 0 : RotClientFonts.width(font, rest));
            } else {
                RotClientUiDraw.text(graphics, font, text, x, y + 2, color, true);
                textWidth = RotClientFonts.width(font, text);
            }
            if (editorOpen) {
                drawEditorFrame(
                        graphics,
                        font,
                        x,
                        y,
                        Math.max(STAT_EDITOR_WIDTH, textWidth + 6),
                        STAT_EDITOR_HEIGHT,
                        id);
            }
            graphics.pose().popMatrix();
        }
    }

    private void renderPetHud(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        float[] pose = qol.pose("pet");
        int x = Math.round(pose[0]);
        int y = Math.round(pose[1]);
        pushHudScale(graphics, "pet", x, y);
        if (editorOpen) {
            drawEditorFrame(graphics, font, x, y, PET_EDITOR_WIDTH, PET_EDITOR_HEIGHT, "pet");
        }
        fillHudPanel(graphics, "pet", x, y, PET_EDITOR_WIDTH, PET_EDITOR_HEIGHT);
        ItemStack icon = InventoryChromeRuntime.equippedPet();
        if (!icon.isEmpty()) {
            graphics.item(icon, x + 2, y + 10);
        }
        PetHudPolicy.Snapshot snapshot = InventoryChromeRuntime.petHudSnapshot();
        int textX = x + 22;
        if (snapshot == null) {
            RotClientUiDraw.text(graphics, font, "No pet equipped", textX, y + 12, RotClientTheme.TEXT_MUTED, true);
            graphics.pose().popMatrix();
            return;
        }
        String level = snapshot.levelLabel();
        int nameX = textX;
        if (!level.isEmpty()) {
            RotClientUiDraw.text(graphics, font, level, textX, y + 4, PetHudPolicy.LEVEL_COLOR, true);
            nameX = textX + font.width(level) + 4;
        }
        RotClientUiDraw.text(graphics, font, snapshot.name(), nameX, y + 4, PetHudPolicy.NAME_COLOR, true);
        String held = snapshot.heldItem().isEmpty() ? "None" : snapshot.heldItem();
        RotClientUiDraw.text(graphics, font,
                "Held Item: " + held,
                textX,
                y + 16,
                PetHudPolicy.NAME_COLOR,
                true);
        graphics.pose().popMatrix();
    }

    private void renderCommission(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = CommissionDisplayRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        drawStyledHudLines(graphics, font, "commission", lines, 160);
    }

    private void renderFishing(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = FishingSuiteRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!FishingSuiteRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "fishing", lines, 168);
    }

    private void renderDiana(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = DianaRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!DianaRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "diana", lines, 168);
    }

    private void renderMiningLeftover(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = MiningLeftoverRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!MiningLeftoverRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "mining", lines, 168);
    }

    private void renderForaging(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = ForagingRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!ForagingRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "foraging", lines, 168);
    }

    private void renderIotaArrows(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = IotaRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!IotaRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "iota_arrows", lines, 168);
    }

    private void renderKuudraAlerts(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = IotaKuudraRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!IotaKuudraRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "kuudra_alerts", lines, 168);
    }

    private void renderStallBin(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        List<String> lines = StallMarketRuntime.hudLines(qol);
        if (lines.isEmpty() && !editorOpen) {
            return;
        }
        if (!StallMarketRuntime.hudVisible(qol) && lines.isEmpty()) {
            return;
        }
        drawStyledHudLines(graphics, font, "stall_bin", lines, 168);
    }

    private void renderWardrobe(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        String text = WardrobeAutoEquipRuntime.hudText(editorOpen);
        if (text.isEmpty() && !editorOpen) {
            return;
        }
        float[] pose = qol.pose("wardrobe");
        int x = Math.round(pose[0]);
        int y = Math.round(pose[1]);
        int width = Math.max(110, font.width(text) + 10);
        int height = 16;
        pushHudScale(graphics, "wardrobe", x, y);
        if (editorOpen) {
            drawEditorFrame(graphics, font, x, y, width, height, "wardrobe");
        }
        RotClientUiDraw.text(graphics, font, text, x + 4, y + 4, textPaint("wardrobe", RotClientTheme.TEXT), true);
        graphics.pose().popMatrix();
    }

    private void renderAutoClickerHud(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol) {
        AutoClickerCpsMeter.Snapshot snapshot = AutoClickerRuntime.cpsSnapshot();
        boolean blockHold = AutoClickerRuntime.isHoldingBlockBreak();
        String state = blockHold ? "BLOCK HOLD" : "PULSING";
        String text = "Auto Clicker  " + state + "  L " + snapshot.leftCps()
                + "  R " + snapshot.rightCps() + "  Total " + snapshot.totalCps() + " CPS";
        float[] pose = qol.pose("auto_clicker");
        int x = Math.round(pose[0]);
        int y = Math.round(pose[1]);
        int width = Math.max(190, font.width(text) + 12);
        pushHudScale(graphics, "auto_clicker", x, y);
        if (editorOpen) {
            drawEditorFrame(graphics, font, x, y, width, 16, "auto_clicker");
        }
        fillHudPanel(graphics, "auto_clicker", x, y, width, 16);
        if (panelOn("auto_clicker")) {
            graphics.fill(x, y, x + 3, y + 16,
                    blockHold ? RotClientTheme.WARNING : accentPaint("auto_clicker"));
        }
        RotClientUiDraw.text(graphics, font, text, x + 7, y + 4, textPaint("auto_clicker", RotClientTheme.TEXT), true);
        graphics.pose().popMatrix();
    }

    private void renderSlayerPanel(
            GuiGraphicsExtractor graphics,
            Font font,
            QolUtilityConfig qol,
            String id,
            List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        float[] pose = qol.pose(id);
        int x = Math.round(pose[0]);
        int y = Math.round(pose[1]);
        int width = SLAYER_EDITOR_WIDTH;
        boolean dynamicSlayerText = "slayer".equals(id) && qol.extras().slayerDisplayDynamicSize;
        for (String line : lines) {
            float scale = dynamicSlayerText
                    ? SlayerHudTextPolicy.scaleFor(font.width(line), SLAYER_EDITOR_WIDTH - 12)
                    : 1.0F;
            width = Math.max(width, Math.round(font.width(line) * scale) + 12);
        }
        int height = Math.max(26, 8 + lines.size() * 10);
        pushHudScale(graphics, id, x, y);
        if (editorOpen) {
            drawEditorFrame(graphics, font, x, y, width, height, id);
        }
        fillHudPanel(graphics, id, x, y, width, height);
        if (panelOn(id)) {
            graphics.fill(x, y, x + 3, y + height, accentPaint(id));
        }
        int rowY = y + 4;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? accentPaint(id) : textPaint(id, RotClientTheme.TEXT);
            if ("slayer_profit".equals(id)
                    && lines.get(i).startsWith("Latest · ")
                    && qol.extras().slayerDropsRecentHighlight
                    && !lines.get(i).equals("Latest · none")) {
                color = RotClientTheme.SUCCESS;
            }
            float scale = dynamicSlayerText
                    ? SlayerHudTextPolicy.scaleFor(font.width(lines.get(i)), SLAYER_EDITOR_WIDTH - 12)
                    : 1.0F;
            if (scale < 1.0F) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + 7, rowY);
                graphics.pose().scale(scale, scale);
                RotClientUiDraw.text(graphics, font, lines.get(i), 0, 0, color, true);
                graphics.pose().popMatrix();
            } else {
                RotClientUiDraw.text(graphics, font, lines.get(i), x + 7, rowY, color, true);
            }
            rowY += 10;
        }
        if ("dungeon".equals(id) && qol.extras().dungeonHudMap) {
            drawDungeonMap(graphics, font, x + width + 4, y);
        }
        if ("dungeon".equals(id) && qol.extras().dungeonHudScoreOverlay) {
            int score = DungeonRuntime.overlayScore();
            if (score >= 0) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y + height + 6);
                graphics.pose().scale(1.6F, 1.6F);
                RotClientUiDraw.text(graphics, font, "Score: " + score, 0, 0, 0xFFFF5555, true);
                graphics.pose().popMatrix();
            }
        }
        graphics.pose().popMatrix();
    }

    private static void drawDungeonMap(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y) {
        DungeonPuzzlePolicy.MapPreview preview = DungeonRuntime.mapPreview();
        if (preview.width() <= 0 || preview.height() <= 0 || preview.argb().length == 0) {
            return;
        }
        int scale = preview.width() > 80 ? 1 : preview.width() > 40 ? 2 : 3;
        int width = preview.width() * scale;
        int height = preview.height() * scale;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        for (int row = 0; row < preview.height(); row++) {
            for (int col = 0; col < preview.width(); col++) {
                int index = row * preview.width() + col;
                if (index >= preview.argb().length) {
                    continue;
                }
                graphics.fill(
                        x + col * scale,
                        y + row * scale,
                        x + (col + 1) * scale,
                        y + (row + 1) * scale,
                        preview.argb()[index]);
            }
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (extras.dungeonHudHeadMarkers) {
            for (DungeonMapPolicy.PlayerIcon icon : DungeonRuntime.mapPlayers()) {
                drawMapHead(graphics, Minecraft.getInstance(), x + icon.mapX() * scale - 4, y + icon.mapZ() * scale - 4, icon);
            }
        } else if (extras.dungeonHudClassIcons) {
            for (DungeonMapPolicy.PlayerIcon icon : DungeonRuntime.mapPlayers()) {
                String letter = EmberDungeonPolicy.classLetter(
                        DungeonPolicy.dungeonClass(icon.classKey()));
                if (letter.isEmpty()) {
                    continue;
                }
                RotClientUiDraw.text(graphics, font,
                        letter,
                        x + icon.mapX() * scale - 2,
                        y + icon.mapZ() * scale - 8,
                        EmberDungeonPolicy.classColor(DungeonPolicy.dungeonClass(icon.classKey())),
                        true);
            }
        }
        if (extras.dungeonHudCheaterMap && extras.dungeonHudCheaterNames) {
            DungeonMapPolicy.Board board = DungeonRuntime.mapBoard();
            if (board != null && board.calibration().ok()) {
                for (DungeonMapPolicy.RoomTile room : board.rooms()) {
                    String label = DungeonLeftoverPolicy.cheaterRoomLabel(room);
                    if (label.isEmpty()) {
                        continue;
                    }
                    RotClientUiDraw.text(graphics, font,
                            label,
                            x + room.pixelX() * scale,
                            y + room.pixelZ() * scale,
                            0xFFE2E8F0,
                            true);
                }
            }
        }
        List<String> footer = DungeonRuntime.mapFooterLines();
        int rowY = y + height + 3;
        for (String line : footer) {
            int color = line.startsWith("Score") ? 0xFFFF5555
                    : line.startsWith("Crypts") ? 0xFFFF5555
                    : line.startsWith("Secrets") ? 0xFF55FF55
                    : 0xFFFFFFFF;
            RotClientUiDraw.text(graphics, font, line, x, rowY, color, true);
            rowY += 10;
        }
    }

    private static void drawMapHead(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            int x,
            int y,
            DungeonMapPolicy.PlayerIcon icon) {
        if (blitSkinFace(graphics, client, x, y, icon)) {
            return;
        }
        int color = EmberDungeonPolicy.classColor(DungeonPolicy.dungeonClass(icon.classKey()));
        if (icon.self()) {
            color = 0xFFFFFFFF;
        }
        int hair = darken(color, 0.55F);
        int skin = mix(color, 0xFFE7C6A0, 0.45F);
        int eye = 0xFF111111;
        graphics.fill(x, y, x + 8, y + 8, 0xFF111111);
        graphics.fill(x + 1, y + 1, x + 7, y + 7, skin);
        graphics.fill(x + 1, y + 1, x + 7, y + 3, hair);
        graphics.fill(x + 2, y + 4, x + 3, y + 5, eye);
        graphics.fill(x + 5, y + 4, x + 6, y + 5, eye);
        if (client != null && client.level != null && icon.name() != null && !icon.name().isBlank()) {
            for (var player : client.level.players()) {
                if (!player.getGameProfile().name().equalsIgnoreCase(icon.name())) {
                    continue;
                }
                graphics.fill(x + 3, y + 6, x + 5, y + 7, 0xFF22C55E);
                break;
            }
        }
    }

    private static boolean blitSkinFace(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            int x,
            int y,
            DungeonMapPolicy.PlayerIcon icon) {
        if (client == null || client.level == null || icon.name() == null || icon.name().isBlank()) {
            return false;
        }
        for (var player : client.level.players()) {
            if (!player.getGameProfile().name().equalsIgnoreCase(icon.name())
                    && !(icon.self() && player == client.player)) {
                continue;
            }
            try {
                var skin = player.getSkin();
                var body = skin.body();
                net.minecraft.resources.Identifier texture = body.texturePath();
                graphics.blit(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        texture,
                        x,
                        y,
                        8.0F,
                        8.0F,
                        8,
                        8,
                        8,
                        8,
                        64,
                        64);
                graphics.blit(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        texture,
                        x,
                        y,
                        40.0F,
                        8.0F,
                        8,
                        8,
                        8,
                        8,
                        64,
                        64);
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private static int darken(int argb, float factor) {
        int r = Math.max(0, (int) (((argb >> 16) & 0xFF) * factor));
        int g = Math.max(0, (int) (((argb >> 8) & 0xFF) * factor));
        int b = Math.max(0, (int) ((argb & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static void drawNamedValueLine(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            String line,
            int nameColor,
            int valueColor) {
        int split = line.indexOf(' ');
        if (split <= 0) {
            RotClientUiDraw.text(graphics, font, line, x, y, valueColor, true);
            return;
        }
        String name = line.substring(0, split);
        String value = line.substring(split + 1);
        RotClientUiDraw.text(graphics, font, name, x, y, nameColor, true);
        RotClientUiDraw.text(graphics, font, value, x + font.width(name) + 4, y, valueColor, true);
    }

    private static String elementId(PlayerDisplayMath.StatKind kind) {
        return switch (kind) {
            case HEALTH -> "health";
            case MANA -> "mana";
            case OVERFLOW_MANA -> "overflow";
            case DEFENSE -> "defense";
            case VITALITY -> "vitality";
            case EHP -> "ehp";
            case SPEED -> "speed";
        };
    }

    private static int colorFor(
            PlayerDisplayMath.StatKind kind,
            QolUtilityConfig qol) {
        return switch (kind) {
            case HEALTH -> qol.playerDisplayHealthColor;
            case MANA -> qol.playerDisplayManaColor;
            case OVERFLOW_MANA -> qol.playerDisplayOverflowManaColor;
            case DEFENSE -> qol.playerDisplayDefenseColor;
            case VITALITY -> qol.playerDisplayVitalityColor;
            case EHP -> qol.playerDisplayEhpColor;
            case SPEED -> qol.playerDisplaySpeedColor;
        };
    }

    boolean beginDrag(double mouseX, double mouseY) {
        if (!editorOpen) {
            return false;
        }
        String hit = hitTest(mouseX, mouseY);
        if (hit.isEmpty()) {
            return false;
        }
        float[] pose = qol().pose(hit);
        selectedId = hit;
        draggingId = hit;
        dragOffsetX = mouseX - pose[0];
        dragOffsetY = mouseY - pose[1];
        return true;
    }

    boolean dragTo(double mouseX, double mouseY) {
        if (draggingId.isEmpty()) {
            return false;
        }
        qol().setPose(
                draggingId,
                (float) (mouseX - dragOffsetX),
                (float) (mouseY - dragOffsetY));
        return true;
    }

    boolean endDrag() {
        if (draggingId.isEmpty()) {
            return false;
        }
        draggingId = "";
        TrackerStore.save(config);
        return true;
    }

    boolean onScroll(double mouseX, double mouseY, double amount) {
        if (!editorOpen || amount == 0.0D) {
            return false;
        }
        String hit = hitTest(mouseX, mouseY);
        if (hit.isEmpty()) {
            return false;
        }
        selectedId = hit;
        return nudgeSelectedScale(amount > 0.0D ? 0.1F : -0.1F);
    }

    List<String> editorElementLabels() {
        List<String> labels = new ArrayList<>();
        QolUtilityConfig qol = qol();
        if (qol.performanceHudEnabled) {
            labels.add("Performance HUD");
        }
        if (qol.playerDisplayEnabled) {
            if (qol.playerDisplayHealthHud) labels.add("Health HUD");
            if (qol.playerDisplayManaHud) labels.add("Mana HUD");
            if (qol.playerDisplayOverflowManaHud) labels.add("Overflow Mana HUD");
            if (qol.playerDisplayDefenseHud) labels.add("Defense HUD");
            if (qol.playerDisplayVitalityHud) labels.add("Vitality HUD");
            if (qol.playerDisplayEhpHud) labels.add("EHP HUD");
            if (qol.playerDisplaySpeedHud) labels.add("Speed HUD");
        }
        if (qol.petHudEnabled) {
            labels.add("Pet HUD");
        }
        if (qol.commissionDisplayEnabled) {
            labels.add("Commission Display");
        }
        if (qol.wardrobeKeybindsEnabled || qol.extras().cheaterWardrobeEnabled) {
            labels.add("Wardrobe Equipping");
        }
        if (qol.autoClickerCpsHudEnabled) {
            labels.add("Auto Clicker CPS HUD");
        }
        if (qol.extras().slayerDisplayEnabled) labels.add("Slayer Display");
        if (qol.extras().slayerProgressEnabled) labels.add("Slayer Progress");
        if (qol.extras().slayerDropsEnabled && qol.extras().slayerDropsRngHud) labels.add("RNG Meter");
        if (qol.extras().slayerDropsEnabled && qol.extras().slayerDropsProfitHud) labels.add("Slayer Item Profit");
        if (qol.extras().slayerStatsEnabled) labels.add("Slayer Stats");
        if (qol.extras().slayerCarryEnabled && qol.extras().slayerCarryDisplay) {
            labels.add("Slayer Carry Display");
        }
        if (qol.extras().slayerCocoonAlertEnabled && qol.extras().slayerCocoonTimer) {
            labels.add("Cocoon Timer");
        }
        if (qol.extras().slayerAttunementDisplayEnabled) labels.add("Attunement Display");
        if (qol.extras().slayerVengeanceEnabled) labels.add("Vengeance Timer");
        if (qol.extras().dungeonHudEnabled) labels.add("Dungeon HUD");
        if (FishingSuiteRuntime.hudVisible(qol)) labels.add("Fishing HUD");
        if (MiningLeftoverRuntime.hudVisible(qol)) labels.add("Mining HUD");
        if (DianaRuntime.hudVisible(qol)) labels.add("Diana HUD");
        if (ForagingRuntime.hudVisible(qol)) labels.add("Foraging HUD");
        if (IotaRuntime.hudVisible(qol)) labels.add("Arrow Tracker");
        if (IotaKuudraRuntime.hudVisible(qol)) labels.add("Kuudra Alerts");
        if (StallMarketRuntime.hudVisible(qol)) labels.add("BIN Overlay");
        return labels;
    }

    String selectedElementLabel() {
        return labelFor(selectedId);
    }

    boolean centerSelectedHorizontally(int screenWidth) {
        if (!isVisibleElement(selectedId) || screenWidth <= 0) {
            return false;
        }
        float[] pose = qol().pose(selectedId);
        qol().setPose(
                selectedId,
                Math.max(0.0F, (screenWidth - elementWidth(selectedId)) * 0.5F),
                pose[1]);
        return true;
    }

    boolean centerSelectedVertically(int screenHeight) {
        if (!isVisibleElement(selectedId) || screenHeight <= 0) {
            return false;
        }
        float[] pose = qol().pose(selectedId);
        qol().setPose(
                selectedId,
                pose[0],
                Math.max(0.0F, (screenHeight - elementHeight(selectedId)) * 0.5F));
        return true;
    }

    boolean resetSelectedPosition() {
        if (!isVisibleElement(selectedId)) {
            return false;
        }
        float[] defaults = new QolUtilityConfig().pose(selectedId);
        qol().setPose(selectedId, defaults[0], defaults[1]);
        return true;
    }

    private String hitTest(double mouseX, double mouseY) {
        QolUtilityConfig qol = qol();
        if (qol.performanceHudEnabled
                && inside(mouseX, mouseY, "performance", 120, 36)) {
            return "performance";
        }
        if (qol.playerDisplayEnabled) {
            if (qol.playerDisplayHealthHud && inside(mouseX, mouseY, "health", 110, 14)) {
                return "health";
            }
            if (qol.playerDisplayManaHud && inside(mouseX, mouseY, "mana", 110, 14)) {
                return "mana";
            }
            if (qol.playerDisplayOverflowManaHud
                    && inside(mouseX, mouseY, "overflow", 110, 14)) {
                return "overflow";
            }
            if (qol.playerDisplayDefenseHud && inside(mouseX, mouseY, "defense", 110, 14)) {
                return "defense";
            }
            if (qol.playerDisplayVitalityHud && inside(mouseX, mouseY, "vitality", 110, 14)) {
                return "vitality";
            }
            if (qol.playerDisplayEhpHud && inside(mouseX, mouseY, "ehp", 110, 14)) {
                return "ehp";
            }
            if (qol.playerDisplaySpeedHud && inside(mouseX, mouseY, "speed", 110, 14)) {
                return "speed";
            }
        }
        if (qol.petHudEnabled && inside(mouseX, mouseY, "pet", PET_EDITOR_WIDTH, PET_EDITOR_HEIGHT)) {
            return "pet";
        }
        if (qol.commissionDisplayEnabled && inside(mouseX, mouseY, "commission", 180, 48)) {
            return "commission";
        }
        if ((qol.wardrobeKeybindsEnabled || qol.extras().cheaterWardrobeEnabled)
                && inside(mouseX, mouseY, "wardrobe", 110, 16)) {
            return "wardrobe";
        }
        if (qol.autoClickerCpsHudEnabled && inside(mouseX, mouseY, "auto_clicker", 260, 16)) {
            return "auto_clicker";
        }
        if (FishingSuiteRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "fishing", 180, 64)) {
            return "fishing";
        }
        if (MiningLeftoverRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "mining", 180, 64)) {
            return "mining";
        }
        if (DianaRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "diana", 180, 64)) {
            return "diana";
        }
        if (ForagingRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "foraging", 180, 64)) {
            return "foraging";
        }
        if (IotaRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "iota_arrows", 180, 64)) {
            return "iota_arrows";
        }
        if (IotaKuudraRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "kuudra_alerts", 180, 64)) {
            return "kuudra_alerts";
        }
        if (StallMarketRuntime.hudVisible(qol)
                && inside(mouseX, mouseY, "stall_bin", 180, 64)) {
            return "stall_bin";
        }
        if (qol.extras().slayerDisplayEnabled
                && inside(mouseX, mouseY, "slayer", SLAYER_EDITOR_WIDTH, 58)) {
            return "slayer";
        }
        if (qol.extras().slayerProgressEnabled
                && inside(mouseX, mouseY, "slayer_progress", SLAYER_EDITOR_WIDTH, 48)) {
            return "slayer_progress";
        }
        if (qol.extras().slayerDropsEnabled && qol.extras().slayerDropsRngHud
                && inside(mouseX, mouseY, "slayer_rng", SLAYER_EDITOR_WIDTH, 48)) return "slayer_rng";
        if (qol.extras().slayerDropsEnabled && qol.extras().slayerDropsProfitHud
                && inside(mouseX, mouseY, "slayer_profit", SLAYER_EDITOR_WIDTH, profitPanelHeight())) return "slayer_profit";
        if (qol.extras().slayerStatsEnabled
                && inside(mouseX, mouseY, "slayer_stats", SLAYER_EDITOR_WIDTH, 68)) {
            return "slayer_stats";
        }
        if (qol.extras().slayerCarryEnabled && qol.extras().slayerCarryDisplay
                && inside(mouseX, mouseY, "slayer_carry", SLAYER_EDITOR_WIDTH, 68)) {
            return "slayer_carry";
        }
        if (qol.extras().slayerCocoonAlertEnabled && qol.extras().slayerCocoonTimer
                && inside(mouseX, mouseY, "slayer_cocoon", SLAYER_EDITOR_WIDTH, 36)) {
            return "slayer_cocoon";
        }
        if (qol.extras().slayerAttunementDisplayEnabled
                && inside(mouseX, mouseY, "slayer_attunement", SLAYER_EDITOR_WIDTH, 36)) {
            return "slayer_attunement";
        }
        if (qol.extras().slayerVengeanceEnabled
                && inside(mouseX, mouseY, "slayer_vengeance", SLAYER_EDITOR_WIDTH, 36)) {
            return "slayer_vengeance";
        }
        if (qol.extras().dungeonHudEnabled
                && inside(mouseX, mouseY, "dungeon", SLAYER_EDITOR_WIDTH, 68)) {
            return "dungeon";
        }
        return "";
    }

    private boolean inside(double mx, double my, String id, int w, int h) {
        float[] pose = qol().pose(id);
        float scale = styleScale(id);
        return mx >= pose[0]
                && mx <= pose[0] + w * scale
                && my >= pose[1]
                && my <= pose[1] + h * scale;
    }

    private void pushHudScale(GuiGraphicsExtractor graphics, String id, int x, int y) {
        float scale = styleScale(id);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);
    }

    private void drawEditorFrame(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String id) {
        boolean selected = id.equals(selectedId);
        boolean focused = HudStylePolicy.isFocused(id, focusId);
        HudStyleState style = qol().extras().resolvedHudStyle(id);
        int color = selected
                ? RotClientTheme.HUD_ACCENT
                : HudStylePolicy.dim(
                        RotClientTheme.BORDER_BRIGHT,
                        focused,
                        qol().extras().hudLayoutDimUnfocused);
        RotClientUiDraw.roundedOutline(
                graphics,
                x - 3,
                y - 3,
                x + width + 3,
                y + height + 3,
                color);
        graphics.fill(x - 3, y - 3, x + 1, y + height + 3, color);
        if (selected) {
            String caption = "Selected: " + labelFor(id) + "  -  drag to move";
            if (!focused && !focusId.isBlank()) {
                caption = labelFor(id) + "  (faded, still draggable)";
            }
            RotClientUiDraw.text(graphics, font,
                    caption,
                    x,
                    Math.max(2, y - 13),
                    focused ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                    true);
        } else if (!focused && editorOpen && qol().extras().hudLayoutDimUnfocused) {
            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    HudStylePolicy.withAlpha(0xFF000000, 0x55));
        }
        if (style.showBackground && !editorOpen) {
            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    HudStylePolicy.dim(
                            style.backgroundColor,
                            true,
                            false));
        }
    }

    private boolean panelOn(String id) {
        return qol().extras().resolvedHudStyle(id).showBackground;
    }

    private int panelFill(String id, int color, int alpha) {
        HudStyleState style = qol().extras().resolvedHudStyle(id);
        if (!style.showBackground) {
            return 0x00000000;
        }
        int fill = style.backgroundColor;
        return HudStylePolicy.dim(
                fill,
                HudStylePolicy.isFocused(id, focusId),
                editorOpen && qol().extras().hudLayoutDimUnfocused);
    }

    private int textPaint(String id, int fallback) {
        HudStyleState style = qol().extras().resolvedHudStyle(id);
        int color = style.textColor == 0 ? fallback : style.textColor;
        return HudStylePolicy.dim(
                color,
                HudStylePolicy.isFocused(id, focusId),
                editorOpen && qol().extras().hudLayoutDimUnfocused);
    }

    private int accentPaint(String id) {
        return HudStylePolicy.dim(
                RotClientTheme.HUD_ACCENT,
                HudStylePolicy.isFocused(id, focusId),
                editorOpen && qol().extras().hudLayoutDimUnfocused);
    }

    boolean nudgeSelectedScale(float delta) {
        if (!isVisibleElement(selectedId)) {
            return false;
        }
        HudStyleState style = qol().extras().resolvedHudStyle(selectedId);
        style.scale = HudStylePolicy.clampScale(style.scale + delta);
        qol().extras().putHudStyle(selectedId, style);
        if ("performance".equals(selectedId)) {
            qol().performanceHudScale = style.scale;
        }
        return true;
    }

    boolean toggleSelectedBackground() {
        if (!isVisibleElement(selectedId)) {
            return false;
        }
        HudStyleState style = qol().extras().resolvedHudStyle(selectedId);
        style.showBackground = !style.showBackground;
        qol().extras().putHudStyle(selectedId, style);
        return true;
    }

    private void drawStyledHudLines(
            GuiGraphicsExtractor graphics,
            Font font,
            String id,
            List<String> lines,
            int minWidth) {
        float[] pose = qol().pose(id);
        int x = Math.round(pose[0]);
        int y = Math.round(pose[1]);
        float scale = styleScale(id);
        int width = minWidth;
        int height = Math.max(28, 8 + Math.max(1, lines.size()) * 10);
        for (String line : lines) {
            width = Math.max(width, font.width(line) + 10);
        }
        if (editorOpen) {
            drawEditorFrame(
                    graphics,
                    font,
                    x,
                    y,
                    Math.round(width * scale),
                    Math.round(height * scale),
                    id);
        }
        if (lines.isEmpty()) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        fillHudPanel(graphics, id, 0, 0, width, height);
        int rowY = 4;
        int color = textPaint(id, RotClientTheme.TEXT);
        for (String line : lines) {
            RotClientUiDraw.text(graphics, font, line, 4, rowY, color, true);
            rowY += 10;
        }
        graphics.pose().popMatrix();
    }

    private float styleScale(String id) {
        return qol().extras().resolvedHudStyle(id).scale;
    }

    private void fillHudPanel(
            GuiGraphicsExtractor graphics,
            String id,
            int x,
            int y,
            int width,
            int height) {
        int fill = panelFill(id, RotClientTheme.SURFACE, 0xC0);
        if (((fill >>> 24) & 0xFF) == 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, fill);
    }

    String selectedId() {
        return selectedId;
    }

    private String firstVisibleElementId() {
        QolUtilityConfig qol = qol();
        if (qol.performanceHudEnabled) return "performance";
        if (qol.playerDisplayEnabled) {
            if (qol.playerDisplayHealthHud) return "health";
            if (qol.playerDisplayManaHud) return "mana";
            if (qol.playerDisplayOverflowManaHud) return "overflow";
            if (qol.playerDisplayDefenseHud) return "defense";
            if (qol.playerDisplayVitalityHud) return "vitality";
            if (qol.playerDisplayEhpHud) return "ehp";
            if (qol.playerDisplaySpeedHud) return "speed";
        }
        if (qol.petHudEnabled) return "pet";
        if (qol.commissionDisplayEnabled) return "commission";
        if (qol.wardrobeKeybindsEnabled || qol.extras().cheaterWardrobeEnabled) return "wardrobe";
        if (qol.autoClickerCpsHudEnabled) return "auto_clicker";
        if (qol.extras().slayerDisplayEnabled) return "slayer";
        if (qol.extras().slayerProgressEnabled) return "slayer_progress";
        if (qol.extras().slayerDropsEnabled && qol.extras().slayerDropsRngHud) return "slayer_rng";
        if (qol.extras().slayerDropsEnabled && qol.extras().slayerDropsProfitHud) return "slayer_profit";
        if (qol.extras().slayerStatsEnabled) return "slayer_stats";
        if (qol.extras().slayerCarryEnabled && qol.extras().slayerCarryDisplay) return "slayer_carry";
        if (qol.extras().slayerCocoonAlertEnabled && qol.extras().slayerCocoonTimer) return "slayer_cocoon";
        if (qol.extras().slayerAttunementDisplayEnabled) return "slayer_attunement";
        if (qol.extras().slayerVengeanceEnabled) return "slayer_vengeance";
        if (qol.extras().dungeonHudEnabled) return "dungeon";
        if (FishingSuiteRuntime.hudVisible(qol)) return "fishing";
        if (MiningLeftoverRuntime.hudVisible(qol)) return "mining";
        if (DianaRuntime.hudVisible(qol)) return "diana";
        if (ForagingRuntime.hudVisible(qol)) return "foraging";
        if (IotaRuntime.hudVisible(qol)) return "iota_arrows";
        if (IotaKuudraRuntime.hudVisible(qol)) return "kuudra_alerts";
        if (StallMarketRuntime.hudVisible(qol)) return "stall_bin";
        return "";
    }

    private boolean isVisibleElement(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        QolUtilityConfig qol = qol();
        return switch (id) {
            case "performance" -> qol.performanceHudEnabled;
            case "health" -> qol.playerDisplayEnabled && qol.playerDisplayHealthHud;
            case "mana" -> qol.playerDisplayEnabled && qol.playerDisplayManaHud;
            case "overflow" -> qol.playerDisplayEnabled && qol.playerDisplayOverflowManaHud;
            case "defense" -> qol.playerDisplayEnabled && qol.playerDisplayDefenseHud;
            case "vitality" -> qol.playerDisplayEnabled && qol.playerDisplayVitalityHud;
            case "ehp" -> qol.playerDisplayEnabled && qol.playerDisplayEhpHud;
            case "speed" -> qol.playerDisplayEnabled && qol.playerDisplaySpeedHud;
            case "pet" -> qol.petHudEnabled;
            case "commission" -> qol.commissionDisplayEnabled;
            case "wardrobe" -> qol.wardrobeKeybindsEnabled || qol.extras().cheaterWardrobeEnabled;
            case "auto_clicker" -> qol.autoClickerCpsHudEnabled;
            case "slayer" -> qol.extras().slayerDisplayEnabled;
            case "slayer_progress" -> qol.extras().slayerProgressEnabled;
            case "slayer_rng" -> qol.extras().slayerDropsEnabled && qol.extras().slayerDropsRngHud;
            case "slayer_profit" -> qol.extras().slayerDropsEnabled && qol.extras().slayerDropsProfitHud;
            case "slayer_stats" -> qol.extras().slayerStatsEnabled;
            case "slayer_carry" -> qol.extras().slayerCarryEnabled && qol.extras().slayerCarryDisplay;
            case "slayer_cocoon" -> qol.extras().slayerCocoonAlertEnabled && qol.extras().slayerCocoonTimer;
            case "slayer_attunement" -> qol.extras().slayerAttunementDisplayEnabled;
            case "slayer_vengeance" -> qol.extras().slayerVengeanceEnabled;
            case "dungeon" -> qol.extras().dungeonHudEnabled;
            case "fishing" -> FishingSuiteRuntime.hudVisible(qol);
            case "mining" -> MiningLeftoverRuntime.hudVisible(qol);
            case "diana" -> DianaRuntime.hudVisible(qol);
            case "foraging" -> ForagingRuntime.hudVisible(qol);
            case "iota_arrows" -> IotaRuntime.hudVisible(qol);
            case "kuudra_alerts" -> IotaKuudraRuntime.hudVisible(qol);
            case "stall_bin" -> StallMarketRuntime.hudVisible(qol);
            default -> false;
        };
    }

    private static String labelFor(String id) {
        return switch (id == null ? "" : id) {
            case "performance" -> "Performance HUD";
            case "health" -> "Health HUD";
            case "mana" -> "Mana HUD";
            case "overflow" -> "Overflow Mana HUD";
            case "defense" -> "Defense HUD";
            case "vitality" -> "Vitality HUD";
            case "ehp" -> "EHP HUD";
            case "speed" -> "Speed HUD";
            case "pet" -> "Pet HUD";
            case "commission" -> "Commission Display";
            case "wardrobe" -> "Wardrobe Equipping";
            case "auto_clicker" -> "Auto Clicker CPS HUD";
            case "slayer" -> "Slayer Display";
            case "slayer_progress" -> "Slayer Progress";
            case "slayer_rng" -> "RNG Meter";
            case "slayer_profit" -> "Slayer Item Profit";
            case "slayer_stats" -> "Slayer Stats";
            case "slayer_carry" -> "Slayer Carry Display";
            case "slayer_cocoon" -> "Cocoon Timer";
            case "slayer_attunement" -> "Attunement Display";
            case "slayer_vengeance" -> "Vengeance Timer";
            case "dungeon" -> "Dungeon HUD";
            case "fishing" -> "Fishing HUD";
            case "mining" -> "Mining HUD";
            case "diana" -> "Diana HUD";
            case "foraging" -> "Foraging HUD";
            case "iota_arrows" -> "Arrow Tracker";
            case "kuudra_alerts" -> "Kuudra Alerts";
            case "stall_bin" -> "BIN Overlay";
            default -> "No QoL element selected";
        };
    }

    private static int elementWidth(String id) {
        if ("performance".equals(id)) {
            return PERFORMANCE_EDITOR_WIDTH;
        }
        if ("pet".equals(id)) {
            return PET_EDITOR_WIDTH;
        }
        if ("commission".equals(id)) {
            return 180;
        }
        if ("wardrobe".equals(id)) {
            return 110;
        }
        if ("auto_clicker".equals(id)) {
            return 260;
        }
        if (id != null && (id.startsWith("slayer") || "dungeon".equals(id) || "fishing".equals(id) || "mining".equals(id) || "diana".equals(id) || "foraging".equals(id) || "iota_arrows".equals(id) || "kuudra_alerts".equals(id) || "stall_bin".equals(id))) {
            return SLAYER_EDITOR_WIDTH;
        }
        return STAT_EDITOR_WIDTH;
    }

    private static int elementHeight(String id) {
        if ("performance".equals(id)) {
            return PERFORMANCE_EDITOR_HEIGHT;
        }
        if ("pet".equals(id)) {
            return PET_EDITOR_HEIGHT;
        }
        if ("commission".equals(id)) {
            return 48;
        }
        if ("wardrobe".equals(id)) {
            return 16;
        }
        if ("auto_clicker".equals(id)) {
            return 16;
        }
        if ("slayer".equals(id)) return 58;
        if ("slayer_progress".equals(id)) return 48;
        if ("slayer_rng".equals(id)) return 48;
        if ("slayer_profit".equals(id)) return profitPanelHeight();
        if ("slayer_stats".equals(id) || "slayer_carry".equals(id)) return 68;
        if ("slayer_cocoon".equals(id)
                || "slayer_attunement".equals(id)
                || "slayer_vengeance".equals(id)) return 36;
        if ("dungeon".equals(id) || "fishing".equals(id) || "mining".equals(id) || "diana".equals(id) || "foraging".equals(id) || "iota_arrows".equals(id) || "kuudra_alerts".equals(id) || "stall_bin".equals(id)) return 68;
        return STAT_EDITOR_HEIGHT;
    }

    /**
     * The profit HUD can intentionally show several drop rows. Use the same
     * read-only line projection as rendering so the editor's drag target and
     * screen clamp always cover the visible panel.
     */
    private static int profitPanelHeight() {
        return Math.max(68, 8 + SlayerRuntime.profitLines(true).size() * 10);
    }

    private QolUtilityConfig qol() {
        if (config.qolUtilities == null) {
            config.qolUtilities = new QolUtilityConfig();
        }
        return config.qolUtilities;
    }
}
