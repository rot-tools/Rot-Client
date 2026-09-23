package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ViewmodelRuntime;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 26.3 first-person renderer bridge for the shared viewmodel pose. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void rotclient$beginViewmodelPose(
            float tickProgress,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            CallbackInfo ci) {
        poseStack.pushPose();
        ViewmodelRuntime.applyItemPose(poseStack);
    }

    @Inject(method = "submitHandsWithItems", at = @At("RETURN"))
    private void rotclient$endViewmodelPose(
            float tickProgress,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            CallbackInfo ci) {
        poseStack.popPose();
    }
}
