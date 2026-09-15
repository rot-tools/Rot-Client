package fi.rotclient.mixin;

import fi.rotclient.SmoothZoomRuntime;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies Rot's Smooth Zoom before Minecraft stores Camera.fov and constructs
 * the world projection matrix.
 *
 * Minecraft 26.2 Camera.update():
 *
 *     this.fov = this.calculateFov(partialTicks);
 *     ...
 *     this.setupPerspective(..., this.fov, ...);
 *
 * Therefore calculateFov is the correct place to modify the effective FOV.
 */
@Mixin(Camera.class)
abstract class CameraZoomMixin {
    @Inject(
            method = "calculateFov",
            at = @At("RETURN"),
            cancellable = true)
    private void rotclient$smoothZoomFov(
            float partialTicks,
            CallbackInfoReturnable<Float> cir) {

        cir.setReturnValue(
                SmoothZoomRuntime.modifyFov(
                        cir.getReturnValueF()));
    }
}