package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.MapArtOverrideRuntime;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only the local front rendering of paintings while Fox is enabled. */
@Mixin(PaintingRenderer.class)
abstract class PaintingRendererFoxMixin {
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotclient$renderFoxPainting(
            PaintingRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        boolean rendered = ClientBoundaryGuard.call(
                "FOX_PAINTING_RENDER",
                () -> MapArtOverrideRuntime.renderPainting(state, poseStack, collector),
                false);
        if (rendered) {
            ci.cancel();
        }
    }
}
