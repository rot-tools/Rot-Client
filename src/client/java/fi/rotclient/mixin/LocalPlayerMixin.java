package fi.rotclient.mixin;

import fi.rotclient.AutoSprintPolicy;
import fi.rotclient.ItemProtectRuntime;
import fi.rotclient.RotClientClient;
import fi.rotclient.StallMarketRuntime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Auto Sprint: while the module is enabled, {@code Input.sprint()} reads as
 * true inside
 * {@link LocalPlayer#aiStep()}, so vanilla sprint rules apply whenever the
 * player moves forward.
 */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean rotclient$autoSprint(Input input) {
        return AutoSprintPolicy.resolveSprintInput(
                input.sprint(),
                RotClientClient.isAutoSprintEnabled());
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void rotclient$protectDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (ItemProtectRuntime.shouldBlockDrop(self.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "openTextEdit", at = @At("HEAD"))
    private void rotclient$fillBazaarSearchSign(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        StallMarketRuntime.fillPendingBazaarSign(sign, front);
    }
}
