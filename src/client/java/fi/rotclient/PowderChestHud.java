package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Independent, movable HUD projection for the Powder Chest Tracker. */
final class PowderChestHud {
    static final int WIDTH = 268;
    private static final int MAX_ROWS = 6;
    private static final int BASE_HEIGHT = 80;
    private static final int ROW_HEIGHT = 14;

    private final TrackerConfig config;
    private boolean editorOpen;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    PowderChestHud(TrackerConfig config) {
        this.config = config;
    }

    void render(GuiGraphicsExtractor graphics) {
        if (!config.powderChestHudEnabled && !editorOpen) return;
        PowderChestTrackerPresentation presentation =
                RotClientClient.powderChestTrackerPresentation();
        List<PowderChestTrackerPresentation.RewardRow> rows = rows(presentation);
        int height = currentHeight(presentation);

        graphics.pose().pushMatrix();
        graphics.pose().translate(config.powderChestHudX, config.powderChestHudY);
        graphics.pose().scale(
                config.powderChestHudScale,
                config.powderChestHudScale);

        if (config.powderChestHudShowBackground) {
            RotClientUiDraw.roundedFill(
                    graphics,
                    0,
                    0,
                    WIDTH,
                    height,
                    RotClientUiDraw.withAlpha(
                            RotClientTheme.HUD_BACKGROUND, 0xE8));
            RotClientUiDraw.roundedOutline(
                    graphics, 0, 0, WIDTH, height, RotClientTheme.HUD_BORDER);
            graphics.fill(0, 5, 3, height - 5,
                    presentation.enabled()
                            ? RotClientTheme.HUD_ACCENT
                            : RotClientTheme.HUD_TEXT_DIM);
        } else if (editorOpen) {
            RotClientUiDraw.roundedOutline(
                    graphics, 0, 0, WIDTH, height, RotClientTheme.HUD_ACCENT);
        }
        RotClientUiDraw.text(graphics, font(), "POWDER CHEST TRACKER", 10, 8,
                RotClientTheme.HUD_TEXT, true);
        drawRight(
                graphics,
                presentation.statusLabel(),
                WIDTH - 10,
                8,
                presentation.enabled()
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.HUD_TEXT_DIM);
        RotClientUiDraw.text(graphics, font(),
                "CHESTS  " + format(presentation.chestsOpened()),
                10, 26, RotClientTheme.HUD_TEXT_DIM, true);
        drawRight(
                graphics,
                formatRate(presentation.chestsPerHour())
                        + (presentation.enchantedHardStone() > 0L
                        ? "  HS " + format(presentation.enchantedHardStone())
                        : ""),
                WIDTH - 10,
                26,
                RotClientTheme.HUD_TEXT_DIM);
        RotClientUiDraw.text(graphics, font(),
                "GEM  " + format(presentation.gemstonePowder())
                        + "  " + formatRate(presentation.gemstonePowderPerHour()),
                10, 40, RotClientTheme.HUD_ACCENT, true);
        drawRight(
                graphics,
                "MITH  " + format(presentation.mithrilPowder())
                        + "  " + formatRate(presentation.mithrilPowderPerHour()),
                WIDTH - 10,
                40,
                RotClientTheme.HUD_ACCENT);
        graphics.fill(10, 57, WIDTH - 10, 58, RotClientTheme.HUD_BORDER);

        if (rows.isEmpty()) {
            RotClientUiDraw.text(graphics, font(), "No chest rewards yet", 10, 65,
                    RotClientTheme.HUD_TEXT_DIM, false);
        } else {
            int y = 65;
            for (int i = 0; i < Math.min(MAX_ROWS, rows.size()); i++) {
                PowderChestTrackerPresentation.RewardRow row = rows.get(i);
                String label = RotClientUiDraw.ellipsize(
                        font(), row.displayName(), WIDTH - 82);
                RotClientUiDraw.text(graphics, font(), label, 10, y,
                        RotClientTheme.HUD_TEXT, false);
                drawRight(graphics, format(row.quantity()), WIDTH - 10, y,
                        RotClientTheme.HUD_ACCENT);
                y += ROW_HEIGHT;
            }
            if (rows.size() > MAX_ROWS) {
                RotClientUiDraw.text(graphics, font(),
                        "+" + (rows.size() - MAX_ROWS) + " more in dashboard",
                        10, y, RotClientTheme.HUD_TEXT_DIM, false);
            }
        }
        graphics.pose().popMatrix();
    }

    int currentHeight() {
        return currentHeight(RotClientClient.powderChestTrackerPresentation());
    }

    boolean containsScreen(double mouseX, double mouseY) {
        return mouseX >= config.powderChestHudX
                && mouseX <= config.powderChestHudX
                + WIDTH * config.powderChestHudScale
                && mouseY >= config.powderChestHudY
                && mouseY <= config.powderChestHudY
                + currentHeight() * config.powderChestHudScale;
    }

    void setEditorOpen(boolean open) {
        editorOpen = open;
        if (!open) {
            dragging = false;
            RotClientClient.save();
        }
    }

    boolean beginDrag(double mouseX, double mouseY) {
        if (!editorOpen || !containsScreen(mouseX, mouseY)) return false;
        dragging = true;
        dragOffsetX = mouseX - config.powderChestHudX;
        dragOffsetY = mouseY - config.powderChestHudY;
        return true;
    }

    boolean dragTo(double mouseX, double mouseY) {
        if (!dragging) return false;
        Minecraft client = Minecraft.getInstance();
        config.powderChestHudX = (float) HudLayoutMath.clampOrigin(
                mouseX - dragOffsetX,
                client.getWindow().getGuiScaledWidth(),
                WIDTH,
                config.powderChestHudScale);
        config.powderChestHudY = (float) HudLayoutMath.clampOrigin(
                mouseY - dragOffsetY,
                client.getWindow().getGuiScaledHeight(),
                currentHeight(),
                config.powderChestHudScale);
        return true;
    }

    boolean endDrag() {
        if (!dragging) return false;
        dragging = false;
        RotClientClient.save();
        return true;
    }

    boolean onScroll(double mouseX, double mouseY, double amount) {
        if (!editorOpen || !containsScreen(mouseX, mouseY)) return false;
        float oldScale = config.powderChestHudScale;
        config.powderChestHudScale = Mth.clamp(
                oldScale + (float) Math.signum(amount) * 0.1F,
                0.5F,
                2.5F);
        config.powderChestHudX += (float) ((mouseX - config.powderChestHudX)
                * (1.0F - config.powderChestHudScale / oldScale));
        config.powderChestHudY += (float) ((mouseY - config.powderChestHudY)
                * (1.0F - config.powderChestHudScale / oldScale));
        clampToScreen();
        RotClientClient.save();
        return true;
    }

    void nudgeScale(float delta) {
        if (!editorOpen) return;
        config.powderChestHudScale = Mth.clamp(
                config.powderChestHudScale + delta, 0.5F, 2.5F);
        clampToScreen();
        RotClientClient.save();
    }

    void clampToScreen() {
        Minecraft client = Minecraft.getInstance();
        config.powderChestHudX = (float) HudLayoutMath.clampOrigin(
                config.powderChestHudX,
                client.getWindow().getGuiScaledWidth(),
                WIDTH,
                config.powderChestHudScale);
        config.powderChestHudY = (float) HudLayoutMath.clampOrigin(
                config.powderChestHudY,
                client.getWindow().getGuiScaledHeight(),
                currentHeight(),
                config.powderChestHudScale);
    }

    private static int currentHeight(PowderChestTrackerPresentation presentation) {
        int count = rows(presentation).size();
        if (count == 0) return BASE_HEIGHT;
        return BASE_HEIGHT + Math.min(MAX_ROWS, count) * ROW_HEIGHT
                + (count > MAX_ROWS ? ROW_HEIGHT : 0);
    }

    private static List<PowderChestTrackerPresentation.RewardRow> rows(
            PowderChestTrackerPresentation presentation) {
        List<PowderChestTrackerPresentation.RewardRow> rows = new ArrayList<>();
        rows.addAll(presentation.lootRows());
        for (PowderChestTrackerPresentation.RewardRow row
                : presentation.currencyRows()) {
            if ("GEMSTONE_POWDER".equals(row.itemId())
                    || "MITHRIL_POWDER".equals(row.itemId())) {
                continue;
            }
            rows.add(row);
        }
        return rows;
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0L, value));
    }

    private static String formatRate(double perHour) {
        if (perHour <= 0.0) {
            return "0/h";
        }
        if (perHour >= 100.0) {
            return String.format(Locale.ROOT, "%,.0f/h", perHour);
        }
        return String.format(Locale.ROOT, "%,.1f/h", perHour);
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static void drawRight(
            GuiGraphicsExtractor graphics,
            String text,
            int right,
            int y,
            int color) {
        RotClientUiDraw.text(graphics, font(), text, right - RotClientFonts.width(font(), text), y, color, false);
    }
}
