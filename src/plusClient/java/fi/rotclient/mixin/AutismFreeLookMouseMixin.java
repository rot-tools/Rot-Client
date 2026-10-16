// SPDX-License-Identifier: GPL-3.0-only
// FreeLook mouse section from AUTISM Client AutismMouseHandlerMixin by Melonik/contributors.
package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.rotclient.AutismFreeLookRuntime;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
abstract class AutismFreeLookMouseMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void rotclient$freeLookTurn(LocalPlayer player, double x, double y, Operation<Void> original) {
        if (!AutismFreeLookRuntime.turn(x, y)) original.call(player, x, y);
    }
}
