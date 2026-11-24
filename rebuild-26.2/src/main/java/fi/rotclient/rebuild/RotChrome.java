package fi.rotclient.rebuild;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Dashboard and HUD editor surfaces in the original Rot palette. */
public final class RotChrome {
    public static final int TEXT = 0xFFF8F5FF;
    public static final int MUTED = 0xFFC8BFD8;
    static final int SIDEBAR = 0xFF211A28;
    static final int BORDER = 0xFF4A3858;
    static final int VIOLET = 0xFFA855F7;

    private RotChrome() {}

    public static int accent() {
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

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        rounded(g, x + 2, y + 4, w, h, 7, 0x99000000);
        surface(g, x, y, w, h, 0xFF000000 | RebuildClient.settings().panel, BORDER);
    }

    public static void text(GuiGraphicsExtractor g, Font font, String label, int x, int y, int width, int color) {
        if (width > 0) RotClientUiDraw.text(g, font, RotClientUiDraw.ellipsize(font, label, width), x, y, color, false);
    }

    public static void centered(GuiGraphicsExtractor g, Font font, String label, int x, int y, int color) {
        RotClientUiDraw.centeredText(g, font, label, x, y, color);
    }

    public static void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered) {
        surface(g, x, y, w, h, hovered ? 0xFF2D2034 : SIDEBAR, BORDER);
    }

    public static void slider(GuiGraphicsExtractor g, Font font, String label, int x, int y, int w, int h,
                              double value, boolean focused) {
        surface(g, x, y, w, h, 0xFF25182C, focused ? accent() : BORDER);
        int end = x + 3 + (int) Math.round((w - 6) * value);
        if (end > x + 3) rounded(g, x + 3, y + h - 5, end - x - 3, 2, 1, accent());
        centered(g, font, label, x + w / 2, y + 3, TEXT);
    }

    public static EditBox field(Font font, int x, int y, int width, int height, String narration) {
        var box = new EditBox(font, x + 4, y + 5, width - 8, height - 8, Component.literal(narration)) {
            @Override
            public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
                surface(g, x, y, width, height, 0xFF100C14, isFocused() ? accent() : BORDER);
                super.extractWidgetRenderState(g, mx, my, delta);
            }
        };
        box.setBordered(false);
        box.setTextColor(TEXT);
        return box;
    }

    static Button button(Font font, String label, int x, int y, int w, int h, boolean selected, Runnable click) {
        return new RotButton(font, label, x, y, w, h, selected, click);
    }

    public static Button button(Font font, Component label, int x, int y, int w, int h, boolean primary, Button.OnPress click) {
        return new RotButton(font, label, x, y, w, h, primary, click);
    }

    abstract static class ControlButton extends Button {
        ControlButton(String label, int x, int y, int w, int h, Runnable click) {
            this(Component.literal(label), x, y, w, h, ignored -> click.run());
        }

        ControlButton(Component label, int x, int y, int w, int h, Button.OnPress click) {
            super(x, y, w, h, label, click, DEFAULT_NARRATION);
        }
    }

    private static final class RotButton extends ControlButton {
        private final Font font;
        private final boolean selected;

        RotButton(Font font, String label, int x, int y, int w, int h, boolean selected, Runnable click) {
            this(font, Component.literal(label), x, y, w, h, selected, ignored -> click.run());
        }

        RotButton(Font font, Component label, int x, int y, int w, int h, boolean selected, Button.OnPress click) {
            super(label, x, y, w, h, click);
            this.font = font;
            this.selected = selected;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int fill = !active ? 0xFF140E16 : selected ? 0xFF3B1530 : isHoveredOrFocused() ? 0xFF3D2048 : 0xFF2A1828;
            surface(g, getX(), getY(), getWidth(), getHeight(), fill, selected ? accent() : BORDER);
            String label = RotClientUiDraw.ellipsize(font, getMessage().getString(), Math.max(1, getWidth() - 10));
            centered(g, font, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2,
                    active ? TEXT : MUTED);
        }
    }
}
