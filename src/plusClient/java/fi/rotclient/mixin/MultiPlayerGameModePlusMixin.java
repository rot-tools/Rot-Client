// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient.mixin;

import fi.rotclient.DungeonPlusInputRuntime;
import fi.rotclient.AutismInteractionRuntime;
import fi.rotclient.QolClientFlavorSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModePlusMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void rotclientplus$breakerInstamine(
            BlockPos pos,
            Direction face,
            CallbackInfoReturnable<Boolean> cir) {
        if (AutismInteractionRuntime.block(pos, InteractionHand.MAIN_HAND, true)) {
            cir.setReturnValue(false);
            return;
        }
        if (DungeonPlusInputRuntime.shouldSkipBreakerSecretMine(pos)) {
            cir.setReturnValue(false);
            return;
        }
        if (QolClientFlavorSupport.hooks().tryBreakerInstamine(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void rotclientplus$blockWrongF7Clicks(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (AutismInteractionRuntime.block(hit.getBlockPos(), hand, false)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (player == null) {
            Minecraft client = Minecraft.getInstance();
            sneaking = client != null && client.player != null && client.player.isShiftKeyDown();
        }
        if (QolClientFlavorSupport.hooks().shouldCancelBlockUse(hit.getBlockPos(), sneaking)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void rotclientplus$blockWrongF7Entity(
            Player player,
            Entity entity,
            net.minecraft.world.phys.EntityHitResult hit,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (AutismInteractionRuntime.entity(entity, hand, false)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (player == null) {
            Minecraft client = Minecraft.getInstance();
            sneaking = client != null && client.player != null && client.player.isShiftKeyDown();
        }
        if (QolClientFlavorSupport.hooks().shouldCancelEntityUse(entity, sneaking)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void rotclient$autismBlockMine(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (AutismInteractionRuntime.block(pos, InteractionHand.MAIN_HAND, true)) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void rotclient$autismBlockAttack(Player player, Entity target, CallbackInfo ci) {
        if (AutismInteractionRuntime.entity(target, InteractionHand.MAIN_HAND, true)) ci.cancel();
    }
}
