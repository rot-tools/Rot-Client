package fi.rotclient.mixin;

import fi.rotclient.EtherwarpPlusRuntime;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerEtherwarpMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void rotclient$etherwarpLeftClick(
            long window,
            MouseButtonInfo buttonInfo,
            int action,
            CallbackInfo ci) {
        int button = buttonInfo == null ? -1 : buttonInfo.button();
        if (EtherwarpPlusRuntime.onMousePress(button, action)) {
            ci.cancel();
        }
    }
}
