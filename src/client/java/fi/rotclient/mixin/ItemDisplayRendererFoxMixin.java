package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.MapArtOverrideRuntime;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Local Fox replacement for item-display wall art after DisplayRenderer applies its transform. */
@Mixin(DisplayRenderer.ItemDisplayRenderer.class)
abstract class ItemDisplayRendererFoxMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void rotclient$configureFoxItemDisplay(
            Display.ItemDisplay display,
            ItemDisplayEntityRenderState state,
            float tickProgress,
            CallbackInfo ci) {
        ClientBoundaryGuard.run(
                "FOX_ITEM_DISPLAY_LAYOUT",
                () -> MapArtOverrideRuntime.configureItemDisplay(display, state, tickProgress));
    }

    @Inject(method = "submitInner", at = @At("HEAD"), cancellable = true)
    private void rotclient$renderFoxItemDisplay(
            ItemDisplayEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            float interpolationProgress,
            CallbackInfo ci) {
        boolean rendered = ClientBoundaryGuard.call(
                "FOX_ITEM_DISPLAY_RENDER",
                () -> MapArtOverrideRuntime.renderItemDisplay(state, poseStack, collector, lightCoords),
                false);
        if (rendered) {
            ci.cancel();
        }
    }
}
