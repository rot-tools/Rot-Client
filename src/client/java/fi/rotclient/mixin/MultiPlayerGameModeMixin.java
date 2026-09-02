package fi.rotclient.mixin;

import fi.rotclient.DungeonRuntime;
import fi.rotclient.MiningAssistRuntime;
import fi.rotclient.RotClientClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void rotclient$recordPlayerAttack(
            Player player,
            Entity target,
            CallbackInfo ci) {
        if (target instanceof LivingEntity
                && !(target instanceof Player)
                && !(target instanceof ArmorStand)) {
            RotClientClient.onPlayerAttack(target);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void rotclient$breakerSkipSecrets(
            BlockPos pos,
            Direction face,
            CallbackInfoReturnable<Boolean> cir) {
        if (DungeonRuntime.shouldSkipBreakerSecretMine(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void rotclient$trackMineStart(
            BlockPos pos,
            Direction face,
            CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.FALSE.equals(cir.getReturnValue())) {
            return;
        }
        MiningAssistRuntime.startMining(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void rotclient$trackMineContinue(
            BlockPos pos,
            Direction face,
            CallbackInfoReturnable<Boolean> cir) {
        MiningAssistRuntime.startMining(pos);
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void rotclient$trackMineStop(CallbackInfo ci) {
        MiningAssistRuntime.stopMining();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void rotclient$blockWrongF7Clicks(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (player == null) {
            Minecraft client = Minecraft.getInstance();
            sneaking = client != null && client.player != null && client.player.isShiftKeyDown();
        }
        if (DungeonRuntime.shouldCancelBlockUse(hit.getBlockPos(), sneaking)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
