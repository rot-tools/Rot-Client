package fi.rotclient.mixin;

import fi.rotclient.MarketTradeRuntime;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMarketTradeMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void rotclient$stopMarketTrade(long window, int action, KeyEvent event, CallbackInfo ci) {
        MarketTradeRuntime.cancelOnEscape(event, action);
    }
}
