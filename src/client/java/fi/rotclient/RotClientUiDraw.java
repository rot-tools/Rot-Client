package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared Client UI drawing primitives. No gameplay logic.
 */
public final class RotClientUiDraw {
    static final int RADIUS_XS = 2;
    static final int RADIUS_SM = 4;
    static final int RADIUS_MD = 8;
    static final int BUTTON_HEIGHT = 26;
    static final int BACK_BUTTON_WIDTH = 28;
    static final int SCRIM_COLOR = 0x99000000;
    static final int METRIC_CARD_HEIGHT = 56;
    static final int SCROLLBAR_WIDTH = 8;
    static final int SCROLLBAR_HIT_WIDTH = 12;
    static final int SCROLLBAR_MIN_THUMB_HEIGHT = 40;

    private RotClientUiDraw() {
    }

    static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    public static void text(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color) {
        text(graphics, font, value, x, y, color, false);
    }

    public static void text(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color,
            boolean ignoredShadow) {
        if (graphics == null || font == null) {
            return;
        }
        glyph(graphics, font, value, x, y, color, ignoredShadow);
    }

    public static void vanillaText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color) {
        if (graphics == null || font == null) {
            return;
        }
        graphics.text(font, RotClientFonts.vanilla(value), x, y, color, false);
    }

    /**
     * Dashboard glyphs: a dark halo plus an optional extra 1px stroke so letters
     * stay readable on near-black panels.
     */
    static void glyph(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color,
            boolean emphasis) {
        if (graphics == null || font == null) {
            return;
        }
        var component = RotClientFonts.component(value);
        if (luma(color) < 140) {
            graphics.text(font, component, x, y, color, false);
            return;
        }
        int halo = withAlpha(0xFF050308, emphasis ? 0xBB : 0x88);
        graphics.text(font, component, x, y + 1, halo, false);
        if (emphasis) {
            graphics.text(font, component, x + 1, y, halo, false);
            graphics.text(font, component, x + 1, y, color, false);
        }
        graphics.text(font, component, x, y, color, false);
    }

    static int luma(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    static void drawChevron(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            double openAmount,
            int color) {
        if (graphics == null) {
            return;
        }
        float t = (float) RotClientEase.clamp01(openAmount);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 5.0F, y + 6.0F);
        graphics.pose().rotate(t * (float) (Math.PI / 2.0D));
        graphics.fill(-1, -4, 1, 1, color);
        graphics.fill(-1, -1, 4, 1, color);
        graphics.pose().popMatrix();
    }

    public static void centeredText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color) {
        if (graphics == null || font == null) {
            return;
        }
        graphics.centeredText(font, RotClientFonts.component(value), x, y, color);
    }

    static int textWidth(Font font, String value) {
        return RotClientFonts.width(font, value);
    }

    static int cornerInset(int dy, int radius) {
        if (radius <= 0 || dy < 0 || dy >= radius) {
            return 0;
        }
        double r = radius;
        double y = radius - 1 - dy;
        double inner = Math.sqrt(Math.max(0.0D, r * r - y * y));
        int inset = (int) Math.round(r - inner);
        return Math.max(0, Math.min(radius, inset));
    }

    static void roundedFill(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int color) {
        roundedFill(graphics, left, top, right, bottom, color, RADIUS_SM);
    }

    static void roundedFill(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int color,
            int radius) {
        if (graphics == null || right <= left || bottom <= top) {
            return;
        }
        int width = right - left;
        int height = bottom - top;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            graphics.fill(left, top, right, bottom, color);
            return;
        }
        for (int y = 0; y < r; y++) {
            int inset = cornerInset(y, r);
            graphics.fill(left + inset, top + y, right - inset, top + y + 1, color);
        }
        if (height > 2 * r) {
            graphics.fill(left, top + r, right, bottom - r, color);
        }
        for (int y = 0; y < r; y++) {
            int inset = cornerInset(r - 1 - y, r);
            int row = height - r + y;
            graphics.fill(left + inset, top + row, right - inset, top + row + 1, color);
        }
    }

    /**
     * How many {@code fill} calls {@link #roundedFill} issues for a given
     * height. Radius-only corners plus one body rect, not one scanline per row.
     */
    static int roundedFillSpanCount(int height, int radius) {
        if (height <= 0) {
            return 0;
        }
        int r = Math.max(0, Math.min(radius, height / 2));
        if (r == 0) {
            return 1;
        }
        return (2 * r) + (height > 2 * r ? 1 : 0);
    }

    static void roundedOutline(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int color) {
        roundedOutline(graphics, left, top, right, bottom, color, RADIUS_SM);
    }

    static void roundedOutline(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int color,
            int radius) {
        roundedFill(graphics, left, top, right, top + 1, color, radius);
        roundedFill(graphics, left, bottom - 1, right, bottom, color, radius);
        graphics.fill(left, top + radius, left + 1, bottom - radius, color);
        graphics.fill(right - 1, top + radius, right, bottom - radius, color);
    }

    static void drawShadowedPanel(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        roundedFill(
                graphics,
                x + 4,
                y + 6,
                x + width + 4,
                y + height + 6,
                RotClientTheme.SHADOW,
                RADIUS_MD);
        roundedFill(
                graphics, x, y, x + width, y + height, RotClientTheme.SURFACE, RADIUS_MD);
        roundedOutline(
                graphics, x, y, x + width, y + height, RotClientTheme.BORDER, RADIUS_MD);
    }

    static void drawHeaderBar(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String title,
            String subtitle) {
        drawHeaderBar(graphics, font, x, y, width, height, title, subtitle, false);
    }

    static void drawHeaderBar(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String title,
            String subtitle,
            boolean backArrow) {
        roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + height,
                RotClientTheme.DASHBOARD_HEADER,
                RADIUS_MD);
        graphics.fill(x, y + height - 1, x + width, y + height, RotClientTheme.DIVIDER);
        if (font == null) {
            return;
        }
        int textX = x + (backArrow ? 40 : 12);
        glyph(graphics, font, title == null ? "" : title, textX, y + 8, RotClientTheme.TEXT, true);
        if (subtitle != null && !subtitle.isBlank()) {
            glyph(graphics, font, subtitle, textX, y + 22, RotClientTheme.TEXT_MUTED, false);
        }
    }

    static void drawScrim(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, SCRIM_COLOR);
    }

    static void drawBackButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            int y) {
        drawButton(
                graphics, font, mouseX, mouseY, x, y, BACK_BUTTON_WIDTH, "←", false, true);
    }

    static boolean hitBackButton(int mouseX, int mouseY, int x, int y) {
        return inside(mouseX, mouseY, x, y, BACK_BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    static void drawButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            String label,
            boolean accent,
            boolean enabled) {
        drawButton(
                graphics, font, mouseX, mouseY, x, y, width, BUTTON_HEIGHT,
                label, accent, enabled);
    }

    static void drawButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            String label,
            boolean accent,
            boolean enabled) {
        int safeHeight = Math.max(16, height);
        boolean hover = enabled && inside(mouseX, mouseY, x, y, width, safeHeight);
        int fill = !enabled
                ? RotClientTheme.BUTTON_DISABLED
                : (accent
                        ? RotClientTheme.BORDER_BRIGHT
                        : (hover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON));
        roundedFill(graphics, x, y, x + width, y + safeHeight, fill, RADIUS_SM);
        if (font != null && label != null) {
            int text = !enabled
                    ? RotClientTheme.TEXT_MUTED
                    : (accent ? 0xFF111118 : RotClientTheme.BUTTON_TEXT);
            String shown = ellipsize(font, label, width - 10);
            int textY = y + Math.max(2, (safeHeight - 8) / 2);
            glyph(
                    graphics,
                    font,
                    ellipsizeAndHover(font, label, width - 10, x, y, safeHeight),
                    x + (width - RotClientFonts.width(font, shown)) / 2,
                    textY,
                    text,
                    true);
        }
    }

    static void drawCard(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean hover) {
        int fill = hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT;
        roundedFill(graphics, x, y, x + width, y + height, fill, RADIUS_SM);
        roundedOutline(graphics, x, y, x + width, y + height, RotClientTheme.BORDER, RADIUS_SM);
    }

    static void drawAccentCard(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean active) {
        roundedFill(graphics, x, y, x + width, y + height, RotClientTheme.SURFACE_ALT, RADIUS_SM);
        graphics.fill(
                x,
                y + 8,
                x + 3,
                y + height - 8,
                active ? RotClientTheme.SUCCESS : RotClientTheme.HUD_ACCENT);
    }

    static void drawElevatedCard(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        roundedFill(
                graphics,
                x + 2,
                y + 3,
                x + width + 2,
                y + height + 3,
                withAlpha(RotClientTheme.SHADOW, 0x55),
                RADIUS_SM);
        roundedFill(graphics, x, y, x + width, y + height, RotClientTheme.SURFACE_ALT, RADIUS_SM);
        roundedOutline(graphics, x, y, x + width, y + height, RotClientTheme.BORDER, RADIUS_SM);
    }

    static void drawNavItem(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            String label,
            String subtitle,
            boolean selected,
            boolean active) {
        boolean hover = inside(mouseX, mouseY, x, y, width, height);
        int fill = selected
                ? RotClientTheme.SELECTED_ROW
                : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.DASHBOARD_SIDEBAR);
        roundedFill(graphics, x, y, x + width, y + height, fill, RADIUS_SM);
        if (selected) {
            graphics.fill(x, y + 6, x + 3, y + height - 6, RotClientTheme.BORDER_BRIGHT);
        }
        if (font == null) {
            return;
        }
        glyph(
                graphics,
                font,
                ellipsizeAndHover(font, label == null ? "" : label, width - 16, x + 10, y + 6, 12),
                x + 10,
                y + 8,
                RotClientTheme.TEXT,
                true);
        glyph(
                graphics,
                font,
                ellipsizeAndHover(
                        font,
                        subtitle == null ? "" : subtitle,
                        width - 16,
                        x + 10,
                        y + 20,
                        12),
                x + 10,
                y + 22,
                active ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_MUTED,
                false);
    }

    static void drawMetricCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String title,
            String value,
            int accent) {
        drawMetricCard(graphics, font, x, y, width, title, value, "", accent);
    }

    static void drawMetricCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String title,
            String value,
            String hint,
            int accent) {
        drawElevatedCard(graphics, x, y, width, METRIC_CARD_HEIGHT);
        graphics.fill(x, y + 10, x + 3, y + METRIC_CARD_HEIGHT - 10, accent);
        sectionLabel(graphics, font, title, x + 10, y + 8);
        metricValue(
                graphics,
                font,
                ellipsizeAndHover(font, value, width - 24, x + 10, y + 20, 14),
                x + 10,
                y + 22,
                RotClientTheme.TEXT);
        if (hint != null && !hint.isBlank()) {
            helpText(
                    graphics,
                    font,
                    ellipsizeAndHover(font, hint, width - 24, x + 10, y + 36, 12),
                    x + 10,
                    y + 38);
        }
    }

    static void cardTitle(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        pageTitle(graphics, font, text, x, y);
    }

    static void drawSectionLabel(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        sectionLabel(graphics, font, text, x, y);
    }

    static void sectionLabel(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        if (font == null) {
            return;
        }
        glyph(graphics, font, text == null ? "" : text, x, y, RotClientTheme.TEXT_MUTED, true);
    }

    static void pageTitle(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        if (font == null) {
            return;
        }
        glyph(graphics, font, text == null ? "" : text, x, y, RotClientTheme.TEXT, true);
    }

    static void helpText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        if (font == null) {
            return;
        }
        glyph(graphics, font, text == null ? "" : text, x, y, RotClientTheme.TEXT_MUTED, false);
    }

    static void bodyText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        if (font == null) {
            return;
        }
        glyph(graphics, font, text == null ? "" : text, x, y, RotClientTheme.TEXT_DIM, false);
    }

    static void metricValue(
            GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        if (font == null) {
            return;
        }
        glyph(graphics, font, text == null ? "" : text, x, y, color, true);
    }

    static void drawStatusPill(
            GuiGraphicsExtractor graphics,
            Font font,
            int rightX,
            int y,
            String text,
            int color) {
        if (font == null) {
            return;
        }
        String label = text == null ? "" : text;
        int width = RotClientFonts.width(font, label) + 16;
        int left = rightX - width;
        roundedFill(graphics, left, y, rightX, y + 16, withAlpha(color, 0x33), RADIUS_SM);
        graphics.text(font, RotClientFonts.component(label), left + 8, y + 4, color, false);
    }

    static void drawToggle(
            GuiGraphicsExtractor graphics, int x, int y, boolean enabled, boolean hover) {
        int width = QolUtilityUiMath.TOGGLE_WIDTH;
        int height = QolUtilityUiMath.TOGGLE_HEIGHT;
        int fill = enabled
                ? (hover ? RotClientTheme.HUD_ACCENT : withAlpha(RotClientTheme.HUD_ACCENT, 0xCC))
                : (hover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.TOGGLE_OFF);
        roundedFill(graphics, x, y, x + width, y + height, fill, height / 2);
        int knob = height - 4;
        int knobX = enabled ? x + width - knob - 2 : x + 2;
        roundedFill(
                graphics,
                knobX,
                y + 2,
                knobX + knob,
                y + 2 + knob,
                RotClientTheme.TOGGLE_KNOB,
                knob / 2);
    }

    static void drawColorSwatch(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int color,
            boolean hover) {
        roundedFill(graphics, x, y, x + width, y + height, color, RADIUS_XS);
        roundedOutline(
                graphics,
                x,
                y,
                x + width,
                y + height,
                hover ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER,
                RADIUS_XS);
    }

    static void drawRainbowSwatch(GuiGraphicsExtractor graphics, int x, int y, int size) {
        for (int dx = 0; dx < size; dx++) {
            float hue = dx / (float) Math.max(1, size - 1);
            int color = RotClientColorMath.toArgb(hue * 360.0F, 1.0F, 1.0F, 0xFF);
            graphics.fill(x + dx, y, x + dx + 1, y + size, color);
        }
        roundedOutline(graphics, x, y, x + size, y + size, RotClientTheme.BORDER, RADIUS_XS);
    }

    static void drawHoverTooltip(
            GuiGraphicsExtractor graphics,
            Font font,
            String text,
            int mouseX,
            int mouseY,
            int maxX,
            int maxY) {
        if (font == null || text == null || text.isBlank()) {
            return;
        }
        int maxWidth = Math.min(280, Math.max(120, maxX - 24));
        java.util.List<String> lines = wrapTooltip(font, text, maxWidth - 12);
        int width = 12;
        for (String line : lines) {
            width = Math.max(width, RotClientFonts.width(font, line) + 12);
        }
        int height = 8 + lines.size() * 11;
        int x = Math.min(mouseX + 14, maxX - width - 4);
        int y = Math.min(mouseY + 14, maxY - height - 4);
        x = Math.max(4, x);
        y = Math.max(4, y);
        roundedFill(graphics, x, y, x + width, y + height, withAlpha(0xFF111118, 0xF2), RADIUS_SM);
        roundedOutline(graphics, x, y, x + width, y + height, RotClientTheme.BORDER, RADIUS_SM);
        int row = y + 5;
        for (String line : lines) {
            graphics.text(font, RotClientFonts.component(line), x + 6, row, RotClientTheme.TEXT, false);
            row += 11;
        }
    }

    static java.util.List<String> wrapTooltip(Font font, String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String remaining = text.trim();
        while (!remaining.isEmpty()) {
            if (font == null || RotClientFonts.width(font, remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int cut = remaining.length();
            while (cut > 1 && RotClientFonts.width(font, remaining.substring(0, cut)) > maxWidth) {
                cut--;
            }
            int space = remaining.lastIndexOf(' ', cut);
            if (space >= 8) {
                cut = space;
            }
            lines.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
            if (lines.size() >= 8) {
                if (!remaining.isEmpty()) {
                    lines.set(lines.size() - 1, ellipsize(font, lines.get(lines.size() - 1) + " " + remaining, maxWidth));
                }
                break;
            }
        }
        return lines;
    }

    static void drawScrollbar(
            GuiGraphicsExtractor graphics,
            int x,
            int top,
            int bottom,
            int contentHeight,
            int scrollPixels) {
        drawScrollbar(graphics, x, top, bottom, contentHeight, scrollPixels, false, false);
    }

    static void drawScrollbar(
            GuiGraphicsExtractor graphics,
            int x,
            int top,
            int bottom,
            int contentHeight,
            int scrollPixels,
            boolean hovered,
            boolean dragging) {
        int viewport = Math.max(0, bottom - top);
        if (viewport <= 0) {
            return;
        }
        roundedFill(
                graphics,
                x,
                top,
                x + SCROLLBAR_WIDTH,
                bottom,
                withAlpha(RotClientTheme.BORDER, 0x55),
                RADIUS_XS);
        int thumb = thumbHeight(contentHeight, viewport);
        int maxScroll = Math.max(0, contentHeight - viewport);
        int travel = Math.max(0, viewport - thumb);
        int thumbY = top;
        if (maxScroll > 0) {
            thumbY = top + (int) Math.round(travel * (scrollPixels / (double) maxScroll));
        }
        int fill = dragging || hovered ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.TEXT_MUTED;
        roundedFill(graphics, x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, fill, RADIUS_XS);
    }

    static int thumbHeight(int contentHeight, int viewportHeight) {
        if (contentHeight <= viewportHeight || contentHeight <= 0) {
            return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, viewportHeight);
        }
        int thumb = (int) Math.round(viewportHeight * (viewportHeight / (double) contentHeight));
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.min(viewportHeight, thumb));
    }

    static String ellipsize(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (font == null || maxWidth <= 0) {
            return text;
        }
        if (RotClientFonts.width(font, text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int budget = Math.max(0, maxWidth - RotClientFonts.width(font, ellipsis));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = builder.toString() + text.charAt(i);
            if (RotClientFonts.width(font, next) > budget) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + ellipsis;
    }

    static boolean truncated(Font font, String text, int maxWidth) {
        if (font == null || text == null) {
            return false;
        }
        return RotClientFonts.width(font, text) > maxWidth;
    }

    static String hoverIfTruncated(
            Font font,
            String full,
            int maxWidth,
            int x,
            int y,
            int height,
            int mouseX,
            int mouseY) {
        if (full == null || full.isBlank() || !truncated(font, full, maxWidth)) {
            return null;
        }
        if (!inside(mouseX, mouseY, x, y, Math.max(1, maxWidth), Math.max(8, height))) {
            return null;
        }
        return full;
    }

    private static String hoverFrameTip = "";
    private static int hoverFrameMouseX;
    private static int hoverFrameMouseY;

    static void beginHoverFrame(int mouseX, int mouseY) {
        hoverFrameTip = "";
        hoverFrameMouseX = mouseX;
        hoverFrameMouseY = mouseY;
    }

    static void noteHoverTip(String tip) {
        if (tip != null && !tip.isBlank()) {
            hoverFrameTip = tip;
        }
    }

    static String ellipsizeAndHover(
            Font font,
            String full,
            int maxWidth,
            int x,
            int y,
            int height) {
        String tip = hoverIfTruncated(
                font, full, maxWidth, x, y, height, hoverFrameMouseX, hoverFrameMouseY);
        if (tip != null) {
            hoverFrameTip = tip;
        }
        return ellipsize(font, full, maxWidth);
    }

    static String takeHoverFrameTip() {
        return hoverFrameTip == null ? "" : hoverFrameTip;
    }

    static void drawHoverFrame(
            GuiGraphicsExtractor graphics,
            Font font,
            int maxX,
            int maxY) {
        if (hoverFrameTip == null || hoverFrameTip.isBlank()) {
            return;
        }
        drawHoverTooltip(graphics, font, hoverFrameTip, hoverFrameMouseX, hoverFrameMouseY, maxX, maxY);
    }
}
