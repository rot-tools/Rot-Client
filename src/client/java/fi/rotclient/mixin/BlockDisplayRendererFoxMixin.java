package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.MapArtOverrideRuntime;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Local Fox replacement for large fixed block-display wall art. */
@Mixin(DisplayRenderer.BlockDisplayRenderer.class)
abstract class BlockDisplayRendererFoxMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void rotclient$configureFoxBlockDisplay(
            Display.BlockDisplay display,
            BlockDisplayEntityRenderState state,
            float tickProgress,
            CallbackInfo ci) {
        ClientBoundaryGuard.run(
                "FOX_BLOCK_DISPLAY_LAYOUT",
                () -> MapArtOverrideRuntime.configureBlockDisplay(display, state, tickProgress));
    }

    @Inject(method = "submitInner", at = @At("HEAD"), cancellable = true)
    private void rotclient$renderFoxBlockDisplay(
            BlockDisplayEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            float interpolationProgress,
            CallbackInfo ci) {
        boolean rendered = ClientBoundaryGuard.call(
                "FOX_BLOCK_DISPLAY_RENDER",
                () -> MapArtOverrideRuntime.renderDisplay(state, poseStack, collector, lightCoords),
                false);
        if (rendered) {
            ci.cancel();
        }
    }
}
