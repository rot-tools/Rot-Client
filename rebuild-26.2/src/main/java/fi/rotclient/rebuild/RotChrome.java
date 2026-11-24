package fi.rotclient.rebuild;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Dashboard and HUD editor surfaces in the original Rot palette. */
final class RotChrome {
    static final int TEXT = 0xFFF8F5FF;
    static final int MUTED = 0xFFC8BFD8;
    static final int SIDEBAR = 0xFF211A28;
    static final int BORDER = 0xFF4A3858;
    static final int VIOLET = 0xFFA855F7;

    private RotChrome() {}

    static int accent() {
        return 0xFF000000 | RebuildClient.settings().accent;
    }

    static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        if (w <= 0 || h <= 0) return;
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        rounded(g, x, y, w, h, 6, border);
        rounded(g, x + 1, y + 1, w - 2, h - 2, 5, fill);
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        rounded(g, x + 2, y + 4, w, h, 7, 0x99000000);
        surface(g, x, y, w, h, 0xFF000000 | RebuildClient.settings().panel, BORDER);
    }

    static void text(GuiGraphicsExtractor g, Font font, String label, int x, int y, int width, int color) {
        if (width > 0) g.text(font, font.plainSubstrByWidth(label, width), x, y, color, false);
    }

    static Button button(Font font, String label, int x, int y, int w, int h, boolean selected, Runnable click) {
        return new RotButton(font, label, x, y, w, h, selected, click);
    }

    abstract static class ControlButton extends Button {
        ControlButton(String label, int x, int y, int w, int h, Runnable click) {
            super(x, y, w, h, Component.literal(label), ignored -> click.run(), DEFAULT_NARRATION);
        }
    }

    private static final class RotButton extends ControlButton {
        private final Font font;
        private final boolean selected;

        RotButton(Font font, String label, int x, int y, int w, int h, boolean selected, Runnable click) {
            super(label, x, y, w, h, click);
            this.font = font;
            this.selected = selected;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int fill = !active ? 0xFF140E16 : selected ? 0xFF3B1530 : isHoveredOrFocused() ? 0xFF3D2048 : 0xFF2A1828;
            surface(g, getX(), getY(), getWidth(), getHeight(), fill, selected ? accent() : BORDER);
            String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, getWidth() - 10));
            g.centeredText(font, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2,
                    active ? TEXT : MUTED);
        }
    }
}
