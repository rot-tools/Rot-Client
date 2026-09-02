package fi.rotclient.mixin;

import fi.rotclient.NameHiderRuntime;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
abstract class PlayerTabOverlayNameHiderMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void rotclient$hideTabName(
            PlayerInfo info,
            CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(NameHiderRuntime.apply(cir.getReturnValue()));
    }
}
