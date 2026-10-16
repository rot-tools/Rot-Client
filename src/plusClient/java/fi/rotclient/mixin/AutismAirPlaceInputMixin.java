// SPDX-License-Identifier: GPL-3.0-only
// AUTISM Client AirPlace input ownership adapted to Rot Client+ 26.3.
package fi.rotclient.mixin;

import fi.rotclient.AutismAirPlaceRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class AutismAirPlaceInputMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void rotclient$deferAirPlace(CallbackInfo ci) {
        if (AutismAirPlaceRuntime.handlesClick()) ci.cancel();
    }
}
