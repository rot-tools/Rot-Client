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

    /*
     * Shared hover animation used by buttons and interactive surfaces.
     *
     * Hit boxes remain fixed. Only rendering eases toward the hover state.
     * This gives the rest of Rot Client the responsive interaction language
     * originally introduced by the Overview action cards.
     */
    private static final java.util.Map<String, InteractionMotion>
            INTERACTION_MOTION =
            new java.util.HashMap<>();

    private static final class InteractionMotion {
        double amount;
        long lastNanos;

        InteractionMotion(
                double amount,
                long lastNanos) {

            this.amount = amount;
            this.lastNanos = lastNanos;
        }
    }

    private static String interactionKey(
            String type,
            int x,
            int y,
            int width,
            int height,
            String identity) {

        return (type == null ? "" : type)
                + ":"
                + x
                + ":"
                + y
                + ":"
                + width
                + ":"
                + height
                + ":"
                + (identity == null ? "" : identity);
    }

    private static float interactionAmount(
            String key,
            boolean hovered) {

        long now =
                System.nanoTime();

        InteractionMotion state =
                INTERACTION_MOTION.get(key);

        if (state == null) {
            if (!hovered) {
                return 0.0F;
            }

            state =
                    new InteractionMotion(
                            0.0D,
                            now);

            INTERACTION_MOTION.put(
                    key,
                    state);
        }

        long idle =
                Math.max(
                        0L,
                        now - state.lastNanos);

        if (idle > 500_000_000L) {
            state.amount =
                    0.0D;
        }

        double dt =
                Math.max(
                        1.0D / 1000.0D,
                        Math.min(
                                0.05D,
                                idle <= 0L
                                        ? 1.0D / 120.0D
                                        : idle / 1_000_000_000.0D));

        state.lastNanos =
                now;

        state.amount =
                RotClientEase.expToward(
                        state.amount,
                        hovered
                                ? 1.0D
                                : 0.0D,
                        dt,
                        18.0D);

        if (!hovered
                && state.amount <= 0.0005D) {

            INTERACTION_MOTION.remove(key);
            return 0.0F;
        }

        if (INTERACTION_MOTION.size() > 4096) {
            INTERACTION_MOTION.clear();
        }

        return (float) RotClientEase.smoothstep(
                RotClientEase.clamp01(
                        state.amount));
    }

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
        vanillaText(graphics, font, value, x, y, color, false);
    }

    public static void vanillaText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color,
            boolean shadow) {
        if (graphics == null || font == null) {
            return;
        }
        graphics.text(font, RotClientFonts.vanilla(value), x, y, color, shadow);
    }

    public static void legacyText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color,
            boolean shadow) {
        if (graphics == null || font == null) {
            return;
        }
        graphics.text(font, RotClientFonts.legacy(value), x, y, color, shadow);
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
        drawShadowedPanel(graphics, x, y, width, height, RotClientTheme.SURFACE);
    }

    static void drawShadowedPanel(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, int background) {
        roundedFill(
                graphics,
                x + 4,
                y + 6,
                x + width + 4,
                y + height + 6,
                RotClientTheme.SHADOW,
                RADIUS_MD);
        roundedFill(
                graphics, x, y, x + width, y + height, background, RADIUS_MD);
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

        int safeWidth =
                Math.max(
                        1,
                        width);

        int safeHeight =
                Math.max(
                        16,
                        height);

        boolean hovered =
                enabled
                        && inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        safeWidth,
                        safeHeight);

        float hoverAmount =
                interactionAmount(
                        interactionKey(
                                "button",
                                x,
                                y,
                                safeWidth,
                                safeHeight,
                                label),
                        hovered);

        int lift =
                enabled
                        ? Math.round(
                        2.0F * hoverAmount)
                        : 0;

        int visualY =
                y - lift;

        if (enabled) {
            int shadowAlpha =
                    0x20
                            + Math.round(
                            0x2C * hoverAmount);

            roundedFill(
                    graphics,
                    x + 1,
                    visualY + 2,
                    x + safeWidth + 1,
                    visualY + safeHeight + 3,
                    withAlpha(
                            RotClientTheme.SHADOW,
                            shadowAlpha),
                    RADIUS_SM);
        }

        int fill =
                !enabled
                        ? RotClientTheme.BUTTON_DISABLED
                        : accent
                        ? withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0xD0)
                        : RotClientTheme.BUTTON;

        roundedFill(
                graphics,
                x,
                visualY,
                x + safeWidth,
                visualY + safeHeight,
                fill,
                RADIUS_SM);

        if (enabled
                && hoverAmount > 0.001F) {

            roundedFill(
                    graphics,
                    x,
                    visualY,
                    x + safeWidth,
                    visualY + safeHeight,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            accent
                                    ? 0x20
                                    + Math.round(
                                    0x28 * hoverAmount)
                                    : Math.round(
                                    0x32 * hoverAmount)),
                    RADIUS_SM);
        }

        int border =
                !enabled
                        ? RotClientTheme.BORDER
                        : accent
                        ? withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0xD8)
                        : hoverAmount > 0.001F
                        ? withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0x48
                                        + Math.round(
                                        0x68 * hoverAmount))
                        : RotClientTheme.BORDER;

        roundedOutline(
                graphics,
                x,
                visualY,
                x + safeWidth,
                visualY + safeHeight,
                border,
                RADIUS_SM);

        if (font == null
                || label == null) {
            return;
        }

        String shown =
                ellipsize(
                        font,
                        label,
                        Math.max(
                                1,
                                safeWidth - 12));

        int textX =
                x
                        + (
                        safeWidth
                                - RotClientFonts.width(
                                font,
                                shown))
                        / 2;

        int textY =
                visualY
                        + Math.max(
                        2,
                        (safeHeight - 8)
                                / 2);

        int textColor =
                !enabled
                        ? RotClientTheme.TEXT_MUTED
                        : accent
                        || hoverAmount > 0.20F
                        ? RotClientTheme.TEXT
                        : RotClientTheme.BUTTON_TEXT;

        glyph(
                graphics,
                font,
                shown,
                textX,
                textY,
                textColor,
                enabled
                        && (
                        accent
                                || hoverAmount > 0.20F));
    }
    static void drawPremiumButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            String label,
            boolean selected,
            boolean enabled) {

        drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x,
                y,
                width,
                BUTTON_HEIGHT,
                label,
                selected,
                enabled);
    }

    static void drawPremiumButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            String label,
            boolean selected,
            boolean enabled) {

        int safeWidth =
                Math.max(
                        1,
                        width);

        int safeHeight =
                Math.max(
                        16,
                        height);

        boolean hovered =
                enabled
                        && inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        safeWidth,
                        safeHeight);

        float hoverAmount =
                interactionAmount(
                        interactionKey(
                                "premium",
                                x,
                                y,
                                safeWidth,
                                safeHeight,
                                label),
                        hovered);

        int lift =
                enabled
                        ? Math.round(
                        2.0F * hoverAmount)
                        : 0;

        int visualY =
                y - lift;

        if (enabled) {
            int shadowAlpha =
                    selected
                            ? 0x3C
                            + Math.round(
                            0x20 * hoverAmount)
                            : 0x24
                            + Math.round(
                            0x2C * hoverAmount);

            roundedFill(
                    graphics,
                    x + 1,
                    visualY + 2,
                    x + safeWidth + 1,
                    visualY + safeHeight + 3,
                    withAlpha(
                            RotClientTheme.SHADOW,
                            shadowAlpha),
                    RADIUS_SM);
        }

        roundedFill(
                graphics,
                x,
                visualY,
                x + safeWidth,
                visualY + safeHeight,
                !enabled
                        ? RotClientTheme.BUTTON_DISABLED
                        : selected
                        ? RotClientTheme.SELECTED_ROW
                        : RotClientTheme.BUTTON,
                RADIUS_SM);

        if (enabled
                && (
                selected
                        || hoverAmount > 0.001F)) {

            int washAlpha =
                    selected
                            ? 0x24
                            + Math.round(
                            0x18 * hoverAmount)
                            : Math.round(
                            0x34 * hoverAmount);

            roundedFill(
                    graphics,
                    x,
                    visualY,
                    x + safeWidth,
                    visualY + safeHeight,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            washAlpha),
                    RADIUS_SM);
        }

        int border =
                !enabled
                        ? RotClientTheme.BORDER
                        : selected
                        ? withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0xD8)
                        : hoverAmount > 0.001F
                        ? withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0x50
                                        + Math.round(
                                        0x68 * hoverAmount))
                        : RotClientTheme.BORDER;

        roundedOutline(
                graphics,
                x,
                visualY,
                x + safeWidth,
                visualY + safeHeight,
                border,
                RADIUS_SM);

        if (enabled
                && selected
                && safeHeight > 10) {

            graphics.fill(
                    x + 1,
                    visualY + 5,
                    x + 4,
                    visualY + safeHeight - 5,
                    RotClientTheme.HUD_ACCENT);
        }

        if (font == null
                || label == null) {
            return;
        }

        String shown =
                ellipsizeAndHover(
                        font,
                        label,
                        Math.max(
                                1,
                                safeWidth - 18),
                        x,
                        y,
                        safeHeight);

        int textX =
                x
                        + (
                        safeWidth
                                - RotClientFonts.width(
                                font,
                                shown))
                        / 2;

        int textY =
                visualY
                        + Math.max(
                        3,
                        (safeHeight - 8)
                                / 2);

        glyph(
                graphics,
                font,
                shown,
                textX,
                textY,
                !enabled
                        ? RotClientTheme.TEXT_MUTED
                        : selected
                        || hoverAmount > 0.20F
                        ? RotClientTheme.TEXT
                        : RotClientTheme.BUTTON_TEXT,
                selected
                        || hoverAmount > 0.20F);
    }
    static int drawAnimatedActionCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float hoverAmount,
            boolean active) {

        float t =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                hoverAmount));

        int lift =
                Math.round(
                        2.0F * t);

        int visualY =
                y - lift;

        int shadowAlpha =
                0x22
                        + Math.round(
                        0x24 * t);

        roundedFill(
                graphics,
                x + 1,
                visualY + 3,
                x + width + 1,
                visualY + height + 3,
                withAlpha(
                        0xFF000000,
                        shadowAlpha),
                RADIUS_SM);

        roundedFill(
                graphics,
                x,
                visualY,
                x + width,
                visualY + height,
                RotClientTheme.SURFACE_ALT,
                RADIUS_SM);

        if (t > 0.001F) {
            roundedFill(
                    graphics,
                    x,
                    visualY,
                    x + width,
                    visualY + height,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            Math.round(
                                    0x22 * t)),
                    RADIUS_SM);
        }

        if (active) {
            roundedFill(
                    graphics,
                    x,
                    visualY,
                    x + width,
                    visualY + height,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            0x1E),
                    RADIUS_SM);
        }

        int borderColor;

        if (active) {
            borderColor =
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            0xD0);

        } else if (t > 0.001F) {
            borderColor =
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            0x58
                                    + Math.round(
                                    0x68 * t));

        } else {
            borderColor =
                    RotClientTheme.BORDER;
        }

        roundedOutline(
                graphics,
                x,
                visualY,
                x + width,
                visualY + height,
                borderColor,
                RADIUS_SM);

        int railAlpha =
                active
                        ? 0xFF
                        : 0x50
                        + Math.round(
                        0x70 * t);

        graphics.fill(
                x + 1,
                visualY + 10,
                x + 3,
                visualY + height - 10,
                withAlpha(
                        RotClientTheme.HUD_ACCENT,
                        railAlpha));

        return visualY;
    }
    static void drawCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean hover) {

        float hoverAmount =
                interactionAmount(
                        interactionKey(
                                "card",
                                x,
                                y,
                                width,
                                height,
                                ""),
                        hover);

        if (hoverAmount > 0.001F) {
            roundedFill(
                    graphics,
                    x + 1,
                    y + 2,
                    x + width + 1,
                    y + height + 3,
                    withAlpha(
                            RotClientTheme.SHADOW,
                            0x1C
                                    + Math.round(
                                    0x2C * hoverAmount)),
                    RADIUS_SM);
        }

        roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + height,
                RotClientTheme.SURFACE_ALT,
                RADIUS_SM);

        if (hoverAmount > 0.001F) {
            roundedFill(
                    graphics,
                    x,
                    y,
                    x + width,
                    y + height,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            Math.round(
                                    0x2E * hoverAmount)),
                    RADIUS_SM);
        }

        roundedOutline(
                graphics,
                x,
                y,
                x + width,
                y + height,
                hoverAmount > 0.001F
                        ? withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0x48
                                        + Math.round(
                                        0x68 * hoverAmount))
                        : RotClientTheme.BORDER,
                RADIUS_SM);
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

    static void drawInteractiveSurface(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float hoverAmount,
            boolean selected,
            int accentColor,
            int radius) {

        int safeRadius =
                Math.max(
                        0,
                        radius);

        int safeAccent =
                accentColor == 0
                        ? RotClientTheme.HUD_ACCENT
                        : accentColor;

        float supplied =
                (float) RotClientEase.smoothstep(
                        RotClientEase.clamp01(
                                hoverAmount));

        float t;

        if (hoverAmount <= 0.0F
                || hoverAmount >= 1.0F) {

            t =
                    interactionAmount(
                            interactionKey(
                                    "surface",
                                    x,
                                    y,
                                    width,
                                    height,
                                    Integer.toString(
                                            safeAccent)),
                            hoverAmount >= 1.0F);

        } else {
            t =
                    supplied;
        }

        if (selected
                || t > 0.01F) {

            int shadowAlpha =
                    selected
                            ? 0x40
                            : 0x18
                            + Math.round(
                            0x20 * t);

            roundedFill(
                    graphics,
                    x + 1,
                    y + 2,
                    x + width + 1,
                    y + height + 3,
                    withAlpha(
                            RotClientTheme.SHADOW,
                            shadowAlpha),
                    safeRadius);
        }

        roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + height,
                selected
                        ? RotClientTheme.SELECTED_ROW
                        : RotClientTheme.SURFACE_ALT,
                safeRadius);

        if (selected
                || t > 0.001F) {

            int wash =
                    selected
                            ? 0x1E
                            + Math.round(
                            0x14 * t)
                            : Math.round(
                            0x2A * t);

            roundedFill(
                    graphics,
                    x,
                    y,
                    x + width,
                    y + height,
                    withAlpha(
                            safeAccent,
                            wash),
                    safeRadius);
        }

        int outline;

        if (selected) {
            outline =
                    withAlpha(
                            safeAccent,
                            0xD0);

        } else if (t > 0.001F) {
            outline =
                    withAlpha(
                            safeAccent,
                            Math.min(
                                    0xB8,
                                    0x48
                                            + Math.round(
                                            0x70 * t)));

        } else {
            outline =
                    RotClientTheme.BORDER;
        }

        roundedOutline(
                graphics,
                x,
                y,
                x + width,
                y + height,
                outline,
                safeRadius);
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

        boolean hover =
                inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        height);

        float hoverAmount =
                interactionAmount(
                        interactionKey(
                                "nav",
                                x,
                                y,
                                width,
                                height,
                                label),
                        hover);

        roundedFill(
                graphics,
                x,
                y,
                x + width,
                y + height,
                selected
                        ? RotClientTheme.SELECTED_ROW
                        : RotClientTheme.DASHBOARD_SIDEBAR,
                RADIUS_SM);

        if (!selected
                && hoverAmount > 0.001F) {

            roundedFill(
                    graphics,
                    x,
                    y,
                    x + width,
                    y + height,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            Math.round(
                                    0x20 * hoverAmount)),
                    RADIUS_SM);
        }

        if (selected) {
            graphics.fill(
                    x,
                    y + 6,
                    x + 3,
                    y + height - 6,
                    RotClientTheme.BORDER_BRIGHT);
        }

        if (font == null) {
            return;
        }

        glyph(
                graphics,
                font,
                ellipsizeAndHover(
                        font,
                        label == null
                                ? ""
                                : label,
                        width - 16,
                        x + 10,
                        y + 6,
                        12),
                x + 10,
                y + 8,
                RotClientTheme.TEXT,
                true);

        glyph(
                graphics,
                font,
                ellipsizeAndHover(
                        font,
                        subtitle == null
                                ? ""
                                : subtitle,
                        width - 16,
                        x + 10,
                        y + 22,
                        12),
                x + 10,
                y + 24,
                active
                        ? RotClientTheme.SUCCESS
                        : hoverAmount > 0.25F
                        ? RotClientTheme.TEXT_DIM
                        : RotClientTheme.TEXT_MUTED,
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

    static void drawSquareLatch(
            GuiGraphicsExtractor graphics, int x, int y, boolean enabled, boolean hover) {
        int size = QolUtilityUiMath.SQUARE_LATCH_SIZE;
        int fill;
        if (enabled) {
            fill = hover ? RotClientTheme.HUD_ACCENT : withAlpha(RotClientTheme.HUD_ACCENT, 0xE6);
        } else {
            fill = hover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON;
        }
        roundedFill(graphics, x, y, x + size, y + size, fill, RADIUS_XS);
        roundedOutline(
                graphics,
                x,
                y,
                x + size,
                y + size,
                enabled ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER,
                RADIUS_XS);
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

    static void drawBitmapIcon(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String[] rows,
            int color) {
        if (graphics == null || rows == null) {
            return;
        }
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            if (line == null) {
                continue;
            }
            for (int col = 0; col < line.length(); col++) {
                char cell = line.charAt(col);
                if (cell == '.' || cell == ' ') {
                    continue;
                }
                graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
            }
        }
    }

    static void drawHeaderCommunityLink(
            GuiGraphicsExtractor graphics,
            RotClientHeaderLinksPolicy.Rect hit,
            String[] glyph,
            boolean hover) {
        if (graphics == null || hit == null) {
            return;
        }
        if (hover) {
            roundedFill(
                    graphics,
                    hit.x(),
                    hit.y(),
                    hit.x() + hit.width(),
                    hit.y() + hit.height(),
                    withAlpha(RotClientTheme.HUD_ACCENT, 0x40),
                    RADIUS_XS);
            roundedOutline(
                    graphics,
                    hit.x(),
                    hit.y(),
                    hit.x() + hit.width(),
                    hit.y() + hit.height(),
                    RotClientTheme.HUD_ACCENT,
                    RADIUS_XS);
        }
        drawBitmapIcon(
                graphics,
                hit.iconX(),
                hit.iconY(),
                glyph,
                hover ? RotClientTheme.ERROR : RotClientTheme.HUD_ACCENT);
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
