package fi.rotclient.mixin;

import fi.rotclient.FullbrightNightRuntime;
import fi.rotclient.RotClientClient;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
abstract class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void rotclient$applyFullbright(
            LightmapRenderState state,
            float tickProgress,
            CallbackInfo callback) {
        if (RotClientClient.isFullbrightEnabled()) {
            state.needsUpdate = true;
            state.ambientColor = LightmapRenderStateExtractor.WHITE;
            state.darknessEffectScale = 0.0F;
            state.bossOverlayWorldDarkening = 0.0F;
            state.brightness = 1.0F;
            return;
        }
        FullbrightNightRuntime.applyNightLightmap(state);
    }
}
