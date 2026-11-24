// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** The editor and the in-game HUD use exactly the same layout. */
public final class PopupRenderer {
    public record Bounds(float left, float top, float right, float bottom, float scale,
                         float centerX, float anchorY) {
        public boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }

    private PopupRenderer() { }

    public static Bounds bounds(int width, int height, PopupConfig config, CommandShortcut.Notice notice) {
        int halfWidth = halfWidth(notice);
        float scale = Math.min((float) config.scale, Math.min((width - 16f) / (halfWidth * 2), (height - 8f) / 39f));
        float centerX = Math.clamp((float) (width * config.horizontalPosition),
                halfWidth * scale + 4, width - halfWidth * scale - 4);
        float anchorY = Math.clamp((float) (height * config.verticalPosition),
                8 * scale + 4, height - 31 * scale - 4);
        return new Bounds(centerX - halfWidth * scale, anchorY - 8 * scale,
                centerX + halfWidth * scale, anchorY + 31 * scale, scale, centerX, anchorY);
    }

    public static Bounds draw(GuiGraphicsExtractor graphics, PopupConfig config, CommandShortcut.Notice notice, float fade) {
        Bounds box = bounds(graphics.guiWidth(), graphics.guiHeight(), config, notice);
        drawAt(graphics, config, notice, box.centerX, box.anchorY, box.scale, fade);
        return box;
    }

    static void drawSample(GuiGraphicsExtractor graphics, PopupConfig config, CommandShortcut.Notice notice,
                           float centerX, float centerY, int maxWidth) {
        float scale = Math.min(1f, (float) maxWidth / (halfWidth(notice) * 2));
        drawAt(graphics, config, notice, centerX, centerY - 11.5f * scale, scale, 1);
    }

    private static void drawAt(GuiGraphicsExtractor graphics, PopupConfig config, CommandShortcut.Notice notice,
                               float centerX, float anchorY, float scale, float fade) {
        float textAlpha = fade * (float) config.textOpacity;
        float backgroundAlpha = config.backgroundEnabled ? fade * (float) config.backgroundOpacity : 0;
        int text = color(config.textColor, textAlpha);
        int background = color(config.backgroundColor, backgroundAlpha);
        if ((text >>> 24) == 0 && (background >>> 24) == 0) return;
        var font = Minecraft.getInstance().font;
        Component title = Component.literal(notice.title()).withStyle(ChatFormatting.BOLD);
        int halfWidth = halfWidth(notice);
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(centerX, anchorY);
            pose.scale(scale, scale);
            if ((background >>> 24) != 0) {
                graphics.fill(-halfWidth, -8, halfWidth, 31, background);
                graphics.fill(-halfWidth, -8, halfWidth, -6, color(config.textColor, backgroundAlpha));
            }
            if ((text >>> 24) != 0) {
                graphics.centeredText(font, title, 0, 0, text);
                graphics.centeredText(font, notice.subtitle(), 0, 16, text);
            }
        } finally {
            pose.popMatrix();
        }
    }

    private static int halfWidth(CommandShortcut.Notice notice) {
        var font = Minecraft.getInstance().font;
        Component title = Component.literal(notice.title()).withStyle(ChatFormatting.BOLD);
        return (Math.max(font.width(title), font.width(notice.subtitle())) + 24) / 2;
    }

    private static int color(int rgb, float opacity) {
        return (Math.clamp(Math.round(opacity * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
