package fi.rotclient.mixin;

import fi.rotclient.StallMarketRuntime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared drop protection and Bazaar search sign behavior. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method = "openTextEdit", at = @At("HEAD"))
    private void rotclient$fillBazaarSearchSign(SignBlockEntity sign, SignTextSlot slot, CallbackInfo ci) {
        StallMarketRuntime.fillPendingBazaarSign(sign, slot);
    }
}
