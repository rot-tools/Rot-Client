package fi.rotclient.mixin;

import fi.rotclient.DoubleUseFixRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftDoubleUseMixin {
    @Shadow
    public LocalPlayer player;

    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"),
            cancellable = true)
    private void rotclient$doubleUseRod(CallbackInfo ci) {
        if (!DoubleUseFixRuntime.shouldReplaceBlockUseWithItemUse()) {
            return;
        }
        Minecraft client = (Minecraft) (Object) this;
        if (client.gameMode != null && player != null) {
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND);
        }
        ci.cancel();
    }

    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"),
            cancellable = true)
    private void rotclient$doubleUseDagger(CallbackInfo ci) {
        if (DoubleUseFixRuntime.shouldCancelItemUseOnBlock()) {
            ci.cancel();
        }
    }
}
