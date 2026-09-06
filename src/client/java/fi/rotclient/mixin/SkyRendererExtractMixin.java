package fi.rotclient.mixin;

import fi.rotclient.FullbrightNightRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
abstract class SkyRendererExtractMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void rotclient$alwaysNight(
            ClientLevel level,
            float partialTicks,
            Camera camera,
            SkyRenderState state,
            CallbackInfo callback) {
        FullbrightNightRuntime.applyNightSky(state);
    }
}
