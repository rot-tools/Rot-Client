package fi.rotclient.mixin;

import fi.rotclient.GhostsPolicy;
import fi.rotclient.GhostsRuntime;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperRenderer.class)
abstract class CreeperRendererGhostsMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void rotclient$ghosts(
            Creeper creeper,
            CreeperRenderState state,
            float tickDelta,
            CallbackInfo ci) {
        if (state == null) {
            return;
        }
        GhostsPolicy.Decision decision = GhostsRuntime.decision(creeper);
        if (decision.forceVisible()) {
            state.isInvisible = false;
        }
        if (decision.hidePoweredLayer()) {
            state.isPowered = false;
        }
    }
}
