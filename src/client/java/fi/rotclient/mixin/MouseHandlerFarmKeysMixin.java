package fi.rotclient.mixin;

import fi.rotclient.FarmKeysRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerFarmKeysMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void rotclient$lockFarmKeysLook(double movementTime, CallbackInfo ci) {
        if (FarmKeysRuntime.cameraLocked()) {
            ci.cancel();
        }
    }
}
