package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

/**
 * HSV color picker with cached gradient rendering.
 *
 * <p>Root cause of prior lag: {@code drawSvSquare} issued {@code SV_SIZE^2}
 * (~19,600) individual {@code fill()} calls plus HSV math every frame.
 * This implementation draws the SV field as {@code SV_SIZE} horizontal
 * {@code fillGradient} rows from a hue-keyed cache, and the hue strip from a
 * one-time cache. Config is never written from this screen.
 */
final class RotClientColorPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 280;
    private static final int SV_SIZE = 140;
    private static final int HUE_HEIGHT = 14;
    private static final int HEX_FIELD_WIDTH = 118;
    private static final int HEX_FIELD_HEIGHT = 16;

    private static final int[] HUE_STRIP = buildHueStrip(SV_SIZE);
    private static int cachedSvHueKey = Integer.MIN_VALUE;
    private static int[] cachedSvLeft;
    private static int[] cachedSvRight;

    private final Screen parent;
    private final String title;
    private final int originalColor;
    private final IntConsumer onLive;
    private final IntConsumer onApply;

    private float hue;
    private float saturation;
    private float value;
    private int alpha;
    private String hexInput;
    private boolean editingHex;
    private int lastPublishedColor;
    private String rgbLabel = "";
    private String alphaLabel = "";

    private enum DragTarget {
        NONE,
        SV,
        HUE,
        SLIDER_R,
        SLIDER_G,
        SLIDER_B,
        SLIDER_A
    }

    private DragTarget dragTarget = DragTarget.NONE;

    RotClientColorPickerScreen(
            Screen parent,
            String title,
            int initialColor,
            IntConsumer onApply) {
        this(parent, title, initialColor, null, onApply);
    }

    RotClientColorPickerScreen(
            Screen parent,
            String title,
            int initialColor,
            IntConsumer onLive,
            IntConsumer onApply) {
        super(Component.literal("Color Picker"));
        this.parent = parent;
        this.title = title == null ? "Color" : title;
        this.originalColor = initialColor;
        this.onLive = onLive;
        this.onApply = onApply;
        RotClientColorMath.Hsv hsv = RotClientColorMath.fromArgb(initialColor);
        this.hue = hsv.h();
        this.saturation = hsv.s();
        this.value = hsv.v();
        this.alpha = RotClientAppearanceConfig.alphaOf(initialColor);
        if (this.alpha == 0) {
            this.alpha = 0xFF;
        }
        this.hexInput = RotClientAppearanceConfig.toHexRgb(initialColor);
        this.lastPublishedColor = currentColor();
        refreshLabels(lastPublishedColor);
        ensureSvCache(hue);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {
        float uiScale = uiScale();
        int logicalMouseX = Math.round(mouseX / uiScale);
        int logicalMouseY = Math.round(mouseY / uiScale);
        graphics.pose().pushMatrix();
        graphics.pose().scale(uiScale, uiScale);

        int logicalWidth = Math.round(width / uiScale);
        int logicalHeight = Math.round(height / uiScale);
        RotClientUiDraw.drawScrim(graphics, 0, 0, logicalWidth, logicalHeight);

        int panelX = panelX();
        int panelY = panelY();
        RotClientUiDraw.drawShadowedPanel(
                graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics,
                font,
                panelX,
                panelY,
                PANEL_WIDTH,
                36,
                "Color Picker",
                title,
                true);
        RotClientUiDraw.drawBackButton(
                graphics, font, logicalMouseX, logicalMouseY, panelX + 8, panelY + 5);

        int svX = panelX + 20;
        int svY = panelY + 52;
        ensureSvCache(hue);
        drawSvSquareCached(graphics, svX, svY);
        int hueY = svY + SV_SIZE + 10;
        drawHueBarCached(graphics, svX, hueY, SV_SIZE, HUE_HEIGHT);

        int current = currentColor();
        int previewX = svX + SV_SIZE + 24;
        RotClientUiDraw.text(graphics, font, "Preview", previewX, svY, RotClientTheme.TEXT_MUTED, true);
        RotClientUiDraw.roundedFill(
                graphics,
                previewX,
                svY + 14,
                previewX + 90,
                svY + 54,
                current,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.roundedOutline(
                graphics,
                previewX,
                svY + 14,
                previewX + 90,
                svY + 54,
                RotClientTheme.DIVIDER,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.drawRainbowSwatch(graphics, previewX + 96, svY + 24, 22);

        int hexX = previewX;
        int hexY = svY + 64;
        if (editingHex) {
            RotClientUiDraw.roundedFill(
                    graphics,
                    hexX,
                    hexY,
                    hexX + HEX_FIELD_WIDTH,
                    hexY + HEX_FIELD_HEIGHT,
                    RotClientTheme.FIELD_ACTIVE,
                    RotClientUiDraw.RADIUS_SM);
            RotClientUiDraw.roundedOutline(
                    graphics,
                    hexX,
                    hexY,
                    hexX + HEX_FIELD_WIDTH,
                    hexY + HEX_FIELD_HEIGHT,
                    RotClientTheme.BORDER_BRIGHT,
                    RotClientUiDraw.RADIUS_SM);
            RotClientUiDraw.text(graphics, font, hexInput + "_",
                    hexX + 4, hexY + 4, RotClientTheme.TEXT, false);
        } else {
            RotClientUiDraw.text(graphics, font, "Hex " + hexInput,
                    hexX, hexY + 4, RotClientTheme.TEXT_DIM, false);
        }
        RotClientUiDraw.text(graphics, font, rgbLabel, previewX, svY + 86, RotClientTheme.TEXT_MUTED, false);
        RotClientUiDraw.text(graphics, font, alphaLabel, previewX, svY + 100, RotClientTheme.TEXT_MUTED, false);

        drawSlider(graphics, previewX, svY + 118, 120, "R", ((current >> 16) & 0xFF));
        drawSlider(graphics, previewX, svY + 140, 120, "G", ((current >> 8) & 0xFF));
        drawSlider(graphics, previewX, svY + 162, 120, "B", (current & 0xFF));
        drawSlider(graphics, previewX, svY + 184, 120, "A", alpha);

        int buttonY = panelY + PANEL_HEIGHT - 36;
        RotClientUiDraw.drawButton(
                graphics, font, logicalMouseX, logicalMouseY,
                panelX + 20, buttonY, 90, "Apply", true, true);
        RotClientUiDraw.drawButton(
                graphics, font, logicalMouseX, logicalMouseY,
                panelX + 120, buttonY, 90, "Cancel", false, true);
        RotClientUiDraw.drawButton(
                graphics, font, logicalMouseX, logicalMouseY,
                panelX + 220, buttonY, 120, "Reset", false, true);

        int cx = svX + Math.round(saturation * (SV_SIZE - 1));
        int cy = svY + Math.round((1.0F - value) * (SV_SIZE - 1));
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF000000);

        int hx = svX + Math.round((hue / 360.0F) * (SV_SIZE - 1));
        graphics.fill(hx - 1, hueY - 2, hx + 2, hueY + HUE_HEIGHT + 2, 0xFFFFFFFF);

        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        float uiScale = uiScale();
        int mouseX = Math.round((float) event.x() / uiScale);
        int mouseY = Math.round((float) event.y() / uiScale);
        int panelX = panelX();
        int panelY = panelY();
        if (!RotClientUiDraw.inside(
                mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)
                || RotClientUiDraw.hitBackButton(mouseX, mouseY, panelX + 8, panelY + 5)) {
            onClose();
            return true;
        }
        int svX = panelX + 20;
        int svY = panelY + 52;
        int hueY = svY + SV_SIZE + 10;
        int previewX = svX + SV_SIZE + 24;
        int hexX = previewX;
        int hexY = svY + 64;
        int buttonY = panelY + PANEL_HEIGHT - 36;

        if (RotClientUiDraw.inside(
                mouseX, mouseY, hexX, hexY, HEX_FIELD_WIDTH, HEX_FIELD_HEIGHT)) {
            editingHex = true;
            dragTarget = DragTarget.NONE;
            return true;
        }
        editingHex = false;

        if (RotClientUiDraw.inside(mouseX, mouseY, svX, svY, SV_SIZE, SV_SIZE)) {
            dragTarget = DragTarget.SV;
            applySv(mouseX, mouseY, svX, svY);
            return true;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, svX, hueY, SV_SIZE, HUE_HEIGHT)) {
            dragTarget = DragTarget.HUE;
            applyHue(mouseX, svX);
            return true;
        }
        if (beginSliderDrag(mouseX, mouseY, previewX, svY + 118, DragTarget.SLIDER_R)
                || beginSliderDrag(mouseX, mouseY, previewX, svY + 140, DragTarget.SLIDER_G)
                || beginSliderDrag(mouseX, mouseY, previewX, svY + 162, DragTarget.SLIDER_B)
                || beginSliderDrag(mouseX, mouseY, previewX, svY + 184, DragTarget.SLIDER_A)) {
            return true;
        }
        if (RotClientUiDraw.inside(
                mouseX, mouseY, panelX + 20, buttonY, 90, RotClientUiDraw.BUTTON_HEIGHT)) {
            publishIfChanged(true);
            if (onApply != null) {
                onApply.accept(currentColor());
            }
            Minecraft.getInstance().gui.setScreen(parent);
            return true;
        }
        if (RotClientUiDraw.inside(
                mouseX, mouseY, panelX + 120, buttonY, 90, RotClientUiDraw.BUTTON_HEIGHT)) {
            // Cancel restores the color that existed when the picker opened.
            restoreOriginal();
            if (onLive != null) {
                onLive.accept(originalColor);
            }
            Minecraft.getInstance().gui.setScreen(parent);
            return true;
        }
        if (RotClientUiDraw.inside(
                mouseX, mouseY, panelX + 220, buttonY, 120, RotClientUiDraw.BUTTON_HEIGHT)) {
            restoreOriginal();
            publishIfChanged(true);
            return true;
        }
        dragTarget = DragTarget.NONE;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY) {
        if (event.button() != 0 || dragTarget == DragTarget.NONE) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        float uiScale = uiScale();
        int mouseX = Math.round((float) event.x() / uiScale);
        int mouseY = Math.round((float) event.y() / uiScale);
        int panelX = panelX();
        int panelY = panelY();
        int svX = panelX + 20;
        int svY = panelY + 52;
        int previewX = svX + SV_SIZE + 24;
        switch (dragTarget) {
            case SV -> applySv(mouseX, mouseY, svX, svY);
            case HUE -> applyHue(mouseX, svX);
            case SLIDER_R -> applySlider(mouseX, previewX, 0);
            case SLIDER_G -> applySlider(mouseX, previewX, 1);
            case SLIDER_B -> applySlider(mouseX, previewX, 2);
            case SLIDER_A -> applySlider(mouseX, previewX, 3);
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragTarget = DragTarget.NONE;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editingHex) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!hexInput.isEmpty()) {
                    hexInput = hexInput.substring(0, hexInput.length() - 1);
                }
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER
                    || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                applyHexInput();
                editingHex = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                editingHex = false;
                syncHexFromColor();
                return true;
            }
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            restoreOriginal();
            if (onLive != null) {
                onLive.accept(originalColor);
            }
            Minecraft.getInstance().gui.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!editingHex) {
            return super.charTyped(event);
        }
        String typed = event.codepointAsString();
        if (typed == null || typed.isEmpty()) {
            return true;
        }
        char c = typed.charAt(0);
        if (c == '#') {
            if (hexInput.isEmpty()) {
                hexInput = "#";
            }
            return true;
        }
        if (isHexDigit(c) && hexInput.length() < 7) {
            if (hexInput.isEmpty()) {
                hexInput = "#";
            }
            hexInput += Character.toUpperCase(c);
            return true;
        }
        return true;
    }

    @Override
    public void onClose() {
        restoreOriginal();
        if (onLive != null) {
            onLive.accept(originalColor);
        }
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void restoreOriginal() {
        RotClientColorMath.Hsv hsv = RotClientColorMath.fromArgb(originalColor);
        hue = hsv.h();
        saturation = hsv.s();
        value = hsv.v();
        alpha = RotClientAppearanceConfig.alphaOf(originalColor);
        if (alpha == 0) {
            alpha = 0xFF;
        }
        syncHexFromColor();
        ensureSvCache(hue);
        lastPublishedColor = currentColor();
        refreshLabels(lastPublishedColor);
    }

    private void applyHexInput() {
        int previous = currentColor();
        int parsed = RotClientAppearanceConfig.fromHexRgb(hexInput, alpha, previous);
        RotClientColorMath.Hsv hsv = RotClientColorMath.fromArgb(parsed);
        hue = hsv.h();
        saturation = hsv.s();
        value = hsv.v();
        alpha = RotClientAppearanceConfig.clamp(
                RotClientAppearanceConfig.alphaOf(parsed), 0, 255);
        if (alpha == 0) {
            alpha = 0xFF;
        }
        syncHexFromColor();
        ensureSvCache(hue);
        publishIfChanged(true);
    }

    private void applySv(int mouseX, int mouseY, int svX, int svY) {
        float newS = clamp01((mouseX - svX) / (float) (SV_SIZE - 1));
        float newV = clamp01(1.0F - (mouseY - svY) / (float) (SV_SIZE - 1));
        if (newS == saturation && newV == value) {
            return;
        }
        saturation = newS;
        value = newV;
        syncHexFromColor();
        publishIfChanged(false);
    }

    private void applyHue(int mouseX, int svX) {
        float newHue = clamp01((mouseX - svX) / (float) (SV_SIZE - 1)) * 360.0F;
        if (Math.round(newHue) == Math.round(hue)) {
            hue = newHue;
            return;
        }
        hue = newHue;
        ensureSvCache(hue);
        syncHexFromColor();
        publishIfChanged(false);
    }

    private boolean beginSliderDrag(
            int mouseX,
            int mouseY,
            int x,
            int y,
            DragTarget target) {
        int barX = x + 14;
        int barW = 120 - 14;
        if (!RotClientUiDraw.inside(mouseX, mouseY, barX, y, barW, 12)) {
            return false;
        }
        dragTarget = target;
        int channel = switch (target) {
            case SLIDER_R -> 0;
            case SLIDER_G -> 1;
            case SLIDER_B -> 2;
            case SLIDER_A -> 3;
            default -> -1;
        };
        if (channel >= 0) {
            applySlider(mouseX, x, channel);
        }
        return true;
    }

    private void applySlider(int mouseX, int x, int channel) {
        int barX = x + 14;
        int barW = 120 - 14;
        int value255 = RotClientAppearanceConfig.clamp(
                Math.round(
                        clamp01((mouseX - barX) / (float) Math.max(1, barW - 1)) * 255.0F),
                0,
                255);
        int current = currentColor();
        int r = (current >> 16) & 0xFF;
        int g = (current >> 8) & 0xFF;
        int b = current & 0xFF;
        switch (channel) {
            case 0 -> {
                if (r == value255) {
                    return;
                }
                r = value255;
            }
            case 1 -> {
                if (g == value255) {
                    return;
                }
                g = value255;
            }
            case 2 -> {
                if (b == value255) {
                    return;
                }
                b = value255;
            }
            case 3 -> {
                if (alpha == value255) {
                    return;
                }
                alpha = value255;
                syncHexFromColor();
                publishIfChanged(false);
                return;
            }
            default -> {
                return;
            }
        }
        int rgb = (alpha << 24) | (r << 16) | (g << 8) | b;
        RotClientColorMath.Hsv hsv = RotClientColorMath.fromArgb(rgb);
        hue = hsv.h();
        saturation = hsv.s();
        value = hsv.v();
        ensureSvCache(hue);
        syncHexFromColor();
        publishIfChanged(false);
    }

    private void publishIfChanged(boolean force) {
        int color = currentColor();
        if (!force && color == lastPublishedColor) {
            return;
        }
        lastPublishedColor = color;
        refreshLabels(color);
        if (onLive != null) {
            onLive.accept(color);
        }
    }

    private int currentColor() {
        return RotClientColorMath.toArgb(hue, saturation, value, alpha);
    }

    private void syncHexFromColor() {
        hexInput = RotClientAppearanceConfig.toHexRgb(currentColor());
    }

    private void refreshLabels(int color) {
        rgbLabel = "R " + ((color >> 16) & 0xFF)
                + "  G " + ((color >> 8) & 0xFF)
                + "  B " + (color & 0xFF);
        alphaLabel = "A " + alpha;
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static void ensureSvCache(float hueDegrees) {
        int key = Math.round(hueDegrees);
        if (key == cachedSvHueKey && cachedSvLeft != null && cachedSvRight != null) {
            return;
        }
        if (cachedSvLeft == null || cachedSvLeft.length != SV_SIZE) {
            cachedSvLeft = new int[SV_SIZE];
            cachedSvRight = new int[SV_SIZE];
        }
        for (int row = 0; row < SV_SIZE; row++) {
            float v = 1.0F - row / (float) (SV_SIZE - 1);
            cachedSvLeft[row] = RotClientColorMath.toArgb(hueDegrees, 0.0F, v, 0xFF);
            cachedSvRight[row] = RotClientColorMath.toArgb(hueDegrees, 1.0F, v, 0xFF);
        }
        cachedSvHueKey = key;
    }

    private static void drawSvSquareCached(
            GuiGraphicsExtractor graphics,
            int x,
            int y) {
        for (int row = 0; row < SV_SIZE; row++) {
            graphics.fillGradient(
                    x,
                    y + row,
                    x + SV_SIZE,
                    y + row + 1,
                    cachedSvLeft[row],
                    cachedSvRight[row]);
        }
        RotClientUiDraw.roundedOutline(
                graphics, x, y, x + SV_SIZE, y + SV_SIZE, RotClientTheme.BORDER);
    }

    private static void drawHueBarCached(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height) {
        for (int col = 0; col < width; col++) {
            graphics.fill(
                    x + col,
                    y,
                    x + col + 1,
                    y + height,
                    HUE_STRIP[col]);
        }
        RotClientUiDraw.roundedOutline(
                graphics, x, y, x + width, y + height, RotClientTheme.BORDER);
    }

    private static int[] buildHueStrip(int width) {
        int[] strip = new int[width];
        for (int col = 0; col < width; col++) {
            float h = (col / (float) Math.max(1, width - 1)) * 360.0F;
            strip[col] = RotClientColorMath.toArgb(h, 1.0F, 1.0F, 0xFF);
        }
        return strip;
    }

    private void drawSlider(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            String label,
            int value255) {
        RotClientUiDraw.text(graphics, font, label, x, y, RotClientTheme.TEXT_MUTED, false);
        int barX = x + 14;
        int barW = width - 14;
        RotClientUiDraw.roundedFill(
                graphics, barX, y + 3, barX + barW, y + 9, RotClientTheme.FIELD);
        int fill = Math.round((value255 / 255.0F) * barW);
        RotClientUiDraw.roundedFill(
                graphics,
                barX,
                y + 3,
                barX + Math.max(1, fill),
                y + 9,
                RotClientTheme.BORDER_BRIGHT);
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }

    private float uiScale() {
        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float fitWidth = scaledWidth / (float) (PANEL_WIDTH + 40);
        float fitHeight = scaledHeight / (float) (PANEL_HEIGHT + 40);
        return Math.max(0.55F, Math.min(1.0F, Math.min(fitWidth, fitHeight)));
    }

    private int panelX() {
        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return Math.round((scaledWidth / uiScale() - PANEL_WIDTH) / 2.0F);
    }

    private int panelY() {
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.round((scaledHeight / uiScale() - PANEL_HEIGHT) / 2.0F);
    }

    /** Test hook: SV cache rebuilds only when quantized hue changes. */
    static int svCacheHueKeyForTests() {
        return cachedSvHueKey;
    }

    static void ensureSvCacheForTests(float hueDegrees) {
        ensureSvCache(hueDegrees);
    }

    static boolean hueStripReadyForTests() {
        return HUE_STRIP != null && HUE_STRIP.length == SV_SIZE;
    }
}
