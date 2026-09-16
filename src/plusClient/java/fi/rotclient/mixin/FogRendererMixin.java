package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void rotclient$hideFog(
            Camera camera,
            int renderDistanceInChunks,
            DeltaTracker deltaTracker,
            float darkenWorldAmount,
            ClientLevel level,
            CallbackInfoReturnable<FogData> cir) {
        FogData data = cir.getReturnValue();
        if (data == null) {
            return;
        }
        if (QolVisualRuntime.shouldHideFog()) {
            data.renderDistanceStart = Float.MAX_VALUE;
            data.renderDistanceEnd = Float.MAX_VALUE;
            data.environmentalStart = Float.MAX_VALUE;
            data.environmentalEnd = Float.MAX_VALUE;
            return;
        }
        float factor = QolVisualRuntime.netherFogFactor();
        if (factor < 1.0F) {
            data.environmentalStart *= factor;
            data.environmentalEnd *= factor;
        }
    }
}
