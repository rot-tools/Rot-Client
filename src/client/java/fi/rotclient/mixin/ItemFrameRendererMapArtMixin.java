package fi.rotclient.mixin;

import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.MapArtOverrideRuntime;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMapArtMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void rotclient$mapArtPanel(
            ItemFrame frame, ItemFrameRenderState state, float tickProgress, CallbackInfo ci) {
        ClientBoundaryGuard.run(
                "MAP_ART_FRAME_LAYOUT",
                () -> MapArtOverrideRuntime.configureItemFrame(state.mapRenderState, frame));
    }
}
