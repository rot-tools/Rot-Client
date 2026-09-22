package fi.rotclient.mixin;

import fi.rotclient.SmoothZoomRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the Smooth Zoom key is held the mouse wheel changes the magnification, so it must not
 * also scroll the hotbar. Outside that window the wheel is left completely alone.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerZoomMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void rotclient$zoomWithWheel(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (SmoothZoomRuntime.onScroll(vertical)) {
            ci.cancel();
        }
    }
}
