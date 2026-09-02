package fi.rotclient.mixin;

import fi.rotclient.RewardClaimRuntime;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiRewardClaimMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void rotclient$keepRewardClaimInClient(Screen screen, CallbackInfo ci) {
        if (RewardClaimRuntime.shouldBlockScreen(screen)) {
            ci.cancel();
        }
    }
}
