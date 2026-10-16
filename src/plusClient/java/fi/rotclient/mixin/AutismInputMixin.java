// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient.mixin;

import fi.rotclient.AutismMovementRuntime;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class AutismInputMixin {
    @Inject(method="tick",at=@At("RETURN"))
    private void rotclient$autismInput(CallbackInfo ci){
        ClientInput input=(ClientInput)(Object)this;
        input.keyPresses=AutismMovementRuntime.input(input,input.keyPresses);
    }
}
