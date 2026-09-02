package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.net.http.HttpClient;

/**
 * In-client daily-reward picker using Rot Client dashboard colors.
 */
final class RewardClaimScreen extends Screen {
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 280;
    private static final int CARD_W = 148;
    private static final int CARD_H = 118;

    private final RewardClaimPolicy.ParsedPage page;
    private final HttpClient http;
    private final long waitUntilMs;
    private int selected;
    private boolean claiming;
    private String status;

    RewardClaimScreen(RewardClaimPolicy.ParsedPage page, HttpClient http) {
        super(Component.literal("Daily Rewards"));
        this.page = page;
        this.http = http;
        this.selected = 0;
        int wait = RewardClaimPolicy.adWaitSeconds(
                page,
                RotClientClient.qolConfigPublic().extras().rewardClaimWaitForAd);
        this.waitUntilMs = wait <= 0 ? 0L : System.currentTimeMillis() + wait * 1000L;
        this.status = wait <= 0
                ? "Pick one reward."
                : "Daily-reward ads still apply. Wait " + wait + "s, then claim.";
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = Math.max(12, (width - PANEL_W) / 2);
        int y = Math.max(12, (height - PANEL_H) / 2);
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, PANEL_W, PANEL_H);
        RotClientUiDraw.drawHeaderBar(
                graphics,
                font,
                x,
                y,
                PANEL_W,
                42,
                "Daily Rewards",
                "Streak " + page.streakValue()
                        + "  ·  best " + page.streakBest()
                        + "  ·  " + page.rewards().size() + " choices");
        int cards = Math.min(3, page.rewards().size());
        int gap = 12;
        int rowX = x + (PANEL_W - (cards * CARD_W + (cards - 1) * gap)) / 2;
        int rowY = y + 58;
        for (int i = 0; i < cards; i++) {
            RewardClaimPolicy.RewardOption reward = page.rewards().get(i);
            int cx = rowX + i * (CARD_W + gap);
            boolean hover = RotClientUiDraw.inside(mouseX, mouseY, cx, rowY, CARD_W, CARD_H);
            int fill = i == selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT;
            graphics.fill(cx, rowY, cx + CARD_W, rowY + CARD_H, fill);
            graphics.fill(cx, rowY, cx + CARD_W, rowY + 3, RewardClaimPolicy.rarityColor(reward.rarity()));
            if (hover || i == selected) {
                RotClientUiDraw.roundedOutline(
                        graphics, cx, rowY, cx + CARD_W, rowY + CARD_H, RotClientTheme.BORDER_BRIGHT);
            }
            RotClientUiDraw.glyph(graphics, font, reward.title(), cx + 8, rowY + 12, RotClientTheme.TEXT, true);
            String rarity = reward.rarity() == null ? "" : reward.rarity();
            RotClientUiDraw.glyph(graphics, font, rarity, cx + 8, rowY + 28, RotClientTheme.VIOLET, false);
            if (reward.subtitle() != null && !reward.subtitle().isBlank()) {
                RotClientUiDraw.glyph(
                        graphics,
                        font,
                        truncate(reward.subtitle(), 22),
                        cx + 8,
                        rowY + 46,
                        RotClientTheme.TEXT_MUTED,
                        false);
            }
        }
        long remain = remainingWaitSeconds();
        String waitLine = remain > 0 ? ("Wait " + remain + "s") : status;
        RotClientUiDraw.glyph(
                graphics, font, waitLine, x + 20, y + PANEL_H - 52, RotClientTheme.TEXT_DIM, false);
        boolean canClaim = remain <= 0 && !claiming;
        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + PANEL_W - 168,
                y + PANEL_H - 58,
                148,
                claiming ? "Claiming..." : "Claim",
                true,
                canClaim);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        int x = Math.max(12, (width - PANEL_W) / 2);
        int y = Math.max(12, (height - PANEL_H) / 2);
        int cards = Math.min(3, page.rewards().size());
        int gap = 12;
        int rowX = x + (PANEL_W - (cards * CARD_W + (cards - 1) * gap)) / 2;
        int rowY = y + 58;
        for (int i = 0; i < cards; i++) {
            int cx = rowX + i * (CARD_W + gap);
            if (RotClientUiDraw.inside(mx, my, cx, rowY, CARD_W, CARD_H)) {
                selected = i;
                return true;
            }
        }
        if (RotClientUiDraw.inside(
                mx, my, x + PANEL_W - 168, y + PANEL_H - 58, 148, RotClientUiDraw.BUTTON_HEIGHT)
                && remainingWaitSeconds() <= 0
                && !claiming) {
            claiming = true;
            status = "Sending claim...";
            RewardClaimRuntime.claim(page, selected, http, () -> claiming = false);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private long remainingWaitSeconds() {
        if (waitUntilMs <= 0L) {
            return 0L;
        }
        return Math.max(0L, (waitUntilMs - System.currentTimeMillis() + 999L) / 1000L);
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
