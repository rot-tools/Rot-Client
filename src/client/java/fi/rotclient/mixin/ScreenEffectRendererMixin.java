package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
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
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"))
    private boolean rotclient$hideFireOverlay(LocalPlayer player) {
        if (QolVisualRuntime.shouldHideFireOverlay()) {
            return false;
        }
        return player.isOnFire();
    }
}
