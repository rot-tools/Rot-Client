package fi.rotclient.mixin;

import fi.rotclient.FreecamRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse look turns the detached camera. Vanilla {@code turnPlayer} still runs
 * so sensitivity/smooth-camera math is reused, then the player rotation is
 * restored to the frozen standing pose.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerFreecamMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void rotclient$freecamLookStart(double movementTime, CallbackInfo ci) {
        FreecamRuntime.noteLookStart();
    }

    @Inject(method = "turnPlayer", at = @At("RETURN"))
    private void rotclient$freecamLookAbsorb(double movementTime, CallbackInfo ci) {
        FreecamRuntime.absorbLook();
    }
}
