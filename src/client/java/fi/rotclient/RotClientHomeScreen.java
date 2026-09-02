package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Compact Rot Client home hub. Opens the existing Client UI workspace.
 */
final class RotClientHomeScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 248;

    RotClientHomeScreen() {
        super(Component.literal("Rot Client"));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = Math.max(12, (width - PANEL_WIDTH) / 2);
        int y = Math.max(12, (height - PANEL_HEIGHT) / 2);
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics, font, x, y, PANEL_WIDTH, 42, "Rot Client", "Home");
        RotClientUiDraw.text(graphics, font,
                "Open the dashboard for QoL modules, HUD layout, mining, and sessions.",
                x + 20,
                y + 64,
                RotClientTheme.TEXT_DIM,
                false);
        RotClientUiDraw.text(graphics, font,
                "Right Shift opens it in-game. Search the address bar, or type /rot qol.",
                x + 20,
                y + 80,
                RotClientTheme.TEXT_MUTED,
                false);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x + 20, y + 118, 200, "Open dashboard", true, true);
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x + 232, y + 118, 200, "Close", false, true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mouseX = (int) Math.round(event.x());
        int mouseY = (int) Math.round(event.y());
        int x = Math.max(12, (width - PANEL_WIDTH) / 2);
        int y = Math.max(12, (height - PANEL_HEIGHT) / 2);
        if (RotClientUiDraw.inside(mouseX, mouseY, x + 20, y + 118, 200, RotClientUiDraw.BUTTON_HEIGHT)) {
            RotClientClient.openClientUiNavigating(DashboardModule.NONE, null);
            return true;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, x + 232, y + 118, 200, RotClientUiDraw.BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
