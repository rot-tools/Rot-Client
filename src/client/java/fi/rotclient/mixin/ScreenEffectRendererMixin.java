package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Local first-person fire overlay suppression. Does not change burning state.
 */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Redirect(
            method = "submit",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/state/level/PlayerRenderState;isOnFire:Z"))
    private boolean rotclient$hideFireOverlay(PlayerRenderState state) {
        if (QolVisualRuntime.shouldHideFireOverlay()) {
            return false;
        }
        return state.isOnFire;
    }
}
