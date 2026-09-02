package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ViewmodelRuntime;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Shadow
    private float mainHandHeight;
    @Shadow
    private float offHandHeight;
    @Shadow
    private float oMainHandHeight;
    @Shadow
    private float oOffHandHeight;

    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                    shift = At.Shift.AFTER))
    private void rotclient$viewmodelOffset(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci) {
        ViewmodelRuntime.applyArmOffset(matrices, hand, item);
    }

    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void rotclient$viewmodelPose(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci) {
        ViewmodelRuntime.applyItemPose(matrices);
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"))
    private void rotclient$viewmodelHandOffset(
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm,
            CallbackInfo ci) {
        ViewmodelRuntime.applyHandOffset(matrices, arm);
    }

    @Inject(
            method = "renderPlayerArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getPlayerRenderer(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;"))
    private void rotclient$viewmodelHandPose(
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm,
            CallbackInfo ci) {
        ViewmodelRuntime.applyHandPose(matrices);
    }

    @Redirect(
            method = "swingArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    ordinal = 0))
    private void rotclient$viewmodelSwing(PoseStack matrices, float x, float y, float z) {
        ViewmodelRuntime.applySwingTranslate(matrices, x, y, z);
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void rotclient$skipEquip(
            ItemStack from,
            ItemStack to,
            CallbackInfoReturnable<Boolean> cir) {
        if (ViewmodelRuntime.skipEquipAnimation()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void rotclient$lockEquipHeight(CallbackInfo ci) {
        if (ViewmodelRuntime.skipEquipAnimation()) {
            this.mainHandHeight = 1.0F;
            this.offHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
            this.oOffHandHeight = 1.0F;
        }
    }
}
