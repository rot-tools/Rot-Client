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

/**
 * Nether Fog Darkening only ever makes fog thicker: the factor is applied only
 * when it is below 1, so it can never extend how far you can see. Removing fog
 * is a separate, Plus-only feature (FogRendererMixin in the Plus mixin config).
 */
@Mixin(FogRenderer.class)
abstract class NetherFogDarkeningMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void rotclient$netherFogDarkening(
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
        float factor = QolVisualRuntime.netherFogFactor();
        if (factor < 1.0F) {
            data.environmentalStart *= factor;
            data.environmentalEnd *= factor;
        }
    }
}
