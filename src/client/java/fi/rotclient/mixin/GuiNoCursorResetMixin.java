package fi.rotclient.mixin;

import fi.rotclient.NoCursorResetController;
import fi.rotclient.NoCursorResetPolicy;
import fi.rotclient.RotClientClient;
import fi.rotclient.StorageOverlayRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Preserves cursor position across supported GUI→GUI transitions when
 * No Cursor Reset is enabled.
 */
@Mixin(Gui.class)
abstract class GuiNoCursorResetMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void rotclient$beforeSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.mouseHandler == null) {
            return;
        }
        Screen previous = ((Gui) (Object) this).screen();
        if (screen == null && StorageOverlayRuntime.shouldPinClosedContainer(previous)) {
            ci.cancel();
            return;
        }
        MouseHandler mouse = client.mouseHandler;
        boolean storageTransition =
                StorageOverlayRuntime.shouldKeepCursorAcrossScreens(previous, screen);
        boolean preserve = RotClientClient.isNoCursorResetEnabled() || storageTransition;
        int timeout = RotClientClient.noCursorUnhookTimeoutMs();
        if (storageTransition) {
            timeout = Math.max(timeout, NoCursorResetPolicy.DEFAULT_TIMEOUT_MS);
        }
        RotClientClient.noCursorReset().onScreenChanging(
                preserve,
                previous != null,
                screen != null,
                mouse.xpos(),
                mouse.ypos(),
                System.currentTimeMillis(),
                timeout);
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void rotclient$afterSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        NoCursorResetController controller = RotClientClient.noCursorReset();
        if (!controller.consumeRestore(System.currentTimeMillis())) {
            return;
        }
        GLFW.glfwSetCursorPos(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
        if (client.mouseHandler instanceof MouseHandlerCursorAccessor access) {
            access.rotclient$setXpos(controller.savedX());
            access.rotclient$setYpos(controller.savedY());
        }
    }
}
