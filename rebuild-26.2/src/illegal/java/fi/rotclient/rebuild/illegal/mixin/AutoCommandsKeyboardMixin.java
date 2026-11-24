package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.autocommands.AutoCommandsInput;
import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class AutoCommandsKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void rotRebuild$commandShortcut(long window, int action, KeyEvent event, CallbackInfo callback) {
        if (IllegalEdition.handleAutoLoadoutKey(window, action, event) || AutoCommandsInput.onKeyPress(window, action, event)) {
            callback.cancel();
        }
    }
}
