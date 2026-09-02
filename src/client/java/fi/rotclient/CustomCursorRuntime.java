package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

/**
 * Draws the Rot Client pointer and applies GLFW hide + resize-edge kind.
 */
public final class CustomCursorRuntime {
    private static CustomCursorPolicy.ResizeKind resizeKind = CustomCursorPolicy.ResizeKind.NONE;
    private static long pressStartedAtMs;

    private static int lastCursorMode = Integer.MIN_VALUE;

    private CustomCursorRuntime() {
    }

    static void setResizeKind(CustomCursorPolicy.ResizeKind kind) {
        resizeKind = kind == null ? CustomCursorPolicy.ResizeKind.NONE : kind;
    }

    static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        boolean enabled = qol.extras().customCursorEnabled;
        boolean menuOpen = client.gui != null && client.gui.screen() != null;
        if (!(client.gui != null && client.gui.screen() instanceof MiningUiScreen)) {
            resizeKind = CustomCursorPolicy.ResizeKind.NONE;
        }
        boolean hide = CustomCursorPolicy.shouldHideVanillaCursor(
                enabled, qol.extras().customCursorHideVanilla, menuOpen);
        int desired = hide
                ? GLFW.GLFW_CURSOR_HIDDEN
                : (menuOpen ? GLFW.GLFW_CURSOR_NORMAL : Integer.MIN_VALUE);
        if (hide) {
            GLFW.glfwSetInputMode(
                    client.getWindow().handle(),
                    GLFW.GLFW_CURSOR,
                    GLFW.GLFW_CURSOR_HIDDEN);
            lastCursorMode = desired;
        } else if (menuOpen) {
            if (desired != lastCursorMode) {
                GLFW.glfwSetInputMode(
                        client.getWindow().handle(),
                        GLFW.GLFW_CURSOR,
                        GLFW.GLFW_CURSOR_NORMAL);
                lastCursorMode = desired;
            }
        }
        if (!enabled || !menuOpen) {
            pressStartedAtMs = 0L;
        }
        keepCursorFromSnapping(client);
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.screen() == null || graphics == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!CustomCursorPolicy.shouldDrawOverlay(
                extras.customCursorEnabled,
                extras.customCursorHideVanilla,
                true)) {
            return;
        }
        double[] pointer = pointerGuiPos(client, mouseX, mouseY);
        int x = (int) Math.round(pointer[0]);
        int y = (int) Math.round(pointer[1]);
        long now = System.currentTimeMillis();
        boolean pressed = GLFW.glfwGetMouseButton(
                client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
        if (pressed) {
            if (pressStartedAtMs <= 0L) {
                pressStartedAtMs = now;
            }
        } else {
            pressStartedAtMs = 0L;
        }
        long held = pressStartedAtMs <= 0L ? 0L : now - pressStartedAtMs;
        float pulse = CustomCursorPolicy.clickPulse(
                pressed, extras.customCursorClickAnim, held);
        int size = CustomCursorPolicy.pointerSizePx(extras.customCursorSize, pulse);
        int fill = CustomCursorPolicy.effectiveFill(extras.customCursorFill);
        int outline = CustomCursorPolicy.effectiveOutline(extras.customCursorOutline);
        int accent = CustomCursorPolicy.effectiveAccent(extras.customCursorAccent);
        graphics.nextStratum();
        graphics.nextStratum();
        CustomCursorPolicy.ResizeKind kind = resizeKind;
        if (kind != CustomCursorPolicy.ResizeKind.NONE) {
            drawResizeArrows(
                    graphics,
                    x,
                    y,
                    size,
                    accent,
                    outline,
                    kind);
        } else {
            drawPointer(
                    graphics,
                    x,
                    y,
                    size,
                    fill,
                    outline,
                    accent,
                    pressed);
        }
        int ring = CustomCursorPolicy.holdRingAlpha(
                pressed, extras.customCursorHoldAnim, held);
        if (ring > 0) {
            int radius = size + 4 + (int) Math.min(10L, held / 40L);
            RotClientUiDraw.roundedOutline(
                    graphics,
                    x - radius / 2,
                    y - radius / 2,
                    x + radius / 2,
                    y + radius / 2,
                    RotClientUiDraw.withAlpha(accent, ring),
                    radius / 2);
        }
    }

    private static void keepCursorFromSnapping(Minecraft client) {
        NoCursorResetController controller = RotClientClient.noCursorReset();
        if (client.getWindow() == null || !controller.consumeRestore(System.currentTimeMillis())) {
            return;
        }
        GLFW.glfwSetCursorPos(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
    }

    private static double[] pointerGuiPos(Minecraft client, int fallbackX, int fallbackY) {
        if (client.mouseHandler == null || client.getWindow() == null) {
            return new double[] { fallbackX, fallbackY };
        }
        var window = client.getWindow();
        int guiW = Math.max(1, window.getGuiScaledWidth());
        int guiH = Math.max(1, window.getGuiScaledHeight());
        int winW = Math.max(1, window.getWidth());
        int winH = Math.max(1, window.getHeight());
        return new double[] {
                client.mouseHandler.xpos() * guiW / (double) winW,
                client.mouseHandler.ypos() * guiH / (double) winH
        };
    }

    private static void drawPointer(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int size,
            int fill,
            int outline,
            int accent,
            boolean pressed) {
        int h = Math.max(14, size);
        int diagonalRows = Math.max(9, h * 2 / 3);
        int body = pressed ? accent : fill;
        for (int row = 0; row < diagonalRows; row++) {
            int width = 1 + (row * 2 / 3);
            graphics.fill(x - 1, y + row, x + width + 1, y + row + 1, outline);
            if (width > 2) {
                graphics.fill(x, y + row, x + width, y + row + 1, body);
            }
        }
        int stemX = x + Math.max(3, diagonalRows / 3);
        int stemTop = y + diagonalRows - 3;
        graphics.fill(stemX - 1, stemTop, stemX + 4, y + h + 2, outline);
        graphics.fill(stemX, stemTop + 1, stemX + 3, y + h + 1, body);
    }

    private static void drawResizeArrows(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int size,
            int accent,
            int outline,
            CustomCursorPolicy.ResizeKind kind) {
        int arm = Math.max(8, size / 2);
        boolean horiz = kind == CustomCursorPolicy.ResizeKind.E
                || kind == CustomCursorPolicy.ResizeKind.W
                || kind == CustomCursorPolicy.ResizeKind.NE
                || kind == CustomCursorPolicy.ResizeKind.NW
                || kind == CustomCursorPolicy.ResizeKind.SE
                || kind == CustomCursorPolicy.ResizeKind.SW;
        boolean vert = kind == CustomCursorPolicy.ResizeKind.N
                || kind == CustomCursorPolicy.ResizeKind.S
                || kind == CustomCursorPolicy.ResizeKind.NE
                || kind == CustomCursorPolicy.ResizeKind.NW
                || kind == CustomCursorPolicy.ResizeKind.SE
                || kind == CustomCursorPolicy.ResizeKind.SW;
        if (horiz) {
            graphics.fill(x - arm, y - 1, x + arm, y + 2, outline);
            graphics.fill(x - arm + 1, y, x + arm - 1, y + 1, accent);
            graphics.fill(x - arm, y - 3, x - arm + 3, y + 4, accent);
            graphics.fill(x + arm - 3, y - 3, x + arm, y + 4, accent);
        }
        if (vert) {
            graphics.fill(x - 1, y - arm, x + 2, y + arm, outline);
            graphics.fill(x, y - arm + 1, x + 1, y + arm - 1, accent);
            graphics.fill(x - 3, y - arm, x + 4, y - arm + 3, accent);
            graphics.fill(x - 3, y + arm - 3, x + 4, y + arm, accent);
        }
    }
}
