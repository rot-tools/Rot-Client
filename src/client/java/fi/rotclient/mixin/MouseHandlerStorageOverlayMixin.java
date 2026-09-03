package fi.rotclient.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.StorageOverlayRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@code releaseMouse} recenters the cursor whenever a GUI opens after
 * a grabbed gameplay frame. Storage page switches do that for one tick; keep
 * the existing cursor instead of snapping to the window centre.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerStorageOverlayMixin {
    private static final int CURSOR_NORMAL = 212993;

    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    private void rotclient$keepStorageOverlayCursor(CallbackInfo ci) {
        if (!StorageOverlayRuntime.shouldKeepUngrabbedCursor()) {
            return;
        }
        MouseHandler mouse = (MouseHandler) (Object) this;
        if (!mouse.isMouseGrabbed()) {
            ci.cancel();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        MouseHandlerCursorAccessor access = (MouseHandlerCursorAccessor) mouse;
        access.rotclient$setMouseGrabbed(false);
        InputConstants.grabOrReleaseMouse(
                client.getWindow(), CURSOR_NORMAL, mouse.xpos(), mouse.ypos());
        ci.cancel();
    }
}
