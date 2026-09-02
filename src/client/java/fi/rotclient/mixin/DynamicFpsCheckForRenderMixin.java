package fi.rotclient.mixin;

import fi.rotclient.RotClientClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamic FPS skips the draw when its target is below 15 FPS. Keep rendering
 * while a Rot Client dashboard is open. Applied only if {@code dynamic_fps}
 * is loaded.
 */
@Mixin(targets = "dynamic_fps.impl.DynamicFPSMod", remap = false)
abstract class DynamicFpsCheckForRenderMixin {
    @Inject(method = "checkForRender()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void rotclient$alwaysRenderDashboard(CallbackInfoReturnable<Boolean> cir) {
        if (RotClientClient.wantsMonitorRefreshUi()) {
            cir.setReturnValue(true);
        }
    }
}
