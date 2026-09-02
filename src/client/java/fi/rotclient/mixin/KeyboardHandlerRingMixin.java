package fi.rotclient.mixin;

import fi.rotclient.RingKeybindsRuntime;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerRingMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void rotclient$commandBind(
            long window,
            int action,
            KeyEvent event,
            CallbackInfo ci) {
        int key = event == null ? -1 : event.key();
        if (RingKeybindsRuntime.onKeyPress(key, action)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void rotclient$commandBindChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (RingKeybindsRuntime.shouldCancelCharTyped()) {
            ci.cancel();
        }
    }
}
