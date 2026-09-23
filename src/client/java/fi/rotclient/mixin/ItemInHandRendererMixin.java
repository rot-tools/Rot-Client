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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply the viewmodel to each submitted hand after vanilla creates its pose. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class ItemInHandRendererMixin {
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
