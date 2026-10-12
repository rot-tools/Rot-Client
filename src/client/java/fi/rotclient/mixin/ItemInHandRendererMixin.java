package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ViewmodelRuntime;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply the viewmodel to each submitted hand after vanilla creates its pose. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class ItemInHandRendererMixin {
    // 26.3 supplies equip progress to the submission method, rather than the
    // old ItemInHandRenderer tick/render path. Zero keeps the hand fully raised.
    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"),
            argsOnly = true, ordinal = 3)
    private float rotclient$equipProgress(float progress) {
        return ViewmodelRuntime.skipEquipAnimation() ? 0.0F : progress;
    }

    @Redirect(method = "swingArm", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void rotclient$swingTranslation(PoseStack pose, float x, float y, float z) {
        ViewmodelRuntime.applySwingTranslate(pose, x, y, z);
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            shift = At.Shift.AFTER, ordinal = 0))
    private void rotclient$applyViewmodelPose(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci) {
        ViewmodelRuntime.applyArmOffset(poseStack, hand, item);
        if (ViewmodelRuntime.shouldTransform(item)) {
            ViewmodelRuntime.applyItemPose(poseStack);
        }
    }
}
