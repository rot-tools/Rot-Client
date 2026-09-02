package fi.rotclient.mixin;

import fi.rotclient.WardrobeAutoEquipRuntime;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerWardrobeMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void rotclient$wardrobeAutoEquip(
            long window,
            int action,
            KeyEvent event,
            CallbackInfo ci) {
        if (WardrobeAutoEquipRuntime.onKeyPress(event, action)) {
            ci.cancel();
        }
    }
}
