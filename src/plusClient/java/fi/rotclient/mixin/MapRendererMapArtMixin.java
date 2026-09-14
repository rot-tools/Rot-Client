package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.MapArtOverrideRuntime;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
abstract class MapRendererMapArtMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void rotclient$renderLocalMapArt(
            MapRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            boolean showOnlyFrame,
            int lightCoords,
            CallbackInfo ci) {
        boolean rendered = ClientBoundaryGuard.call(
                "MAP_ART_RENDER",
                () -> MapArtOverrideRuntime.render(state, poseStack, collector, lightCoords),
                false);
        if (rendered) {
            ci.cancel();
        }
    }
}
