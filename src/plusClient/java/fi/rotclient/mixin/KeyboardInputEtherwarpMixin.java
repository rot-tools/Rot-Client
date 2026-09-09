package fi.rotclient.mixin;

import fi.rotclient.EtherwarpPlusRuntime;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep sneak in the client input while Etherwarp auto-shift is holding, even
 * if GLFW / {@code KeyMapping.setAll()} already cleared the shift mapping.
 */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputEtherwarpMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void rotclient$forceEtherwarpSneak(CallbackInfo ci) {
        if (!EtherwarpPlusRuntime.forceSneak()) {
            return;
        }
        ClientInput input = (ClientInput) (Object) this;
        Input keys = input.keyPresses;
        if (keys == null || keys.shift()) {
            return;
        }
        input.keyPresses = new Input(
                keys.forward(),
                keys.backward(),
                keys.left(),
                keys.right(),
                keys.jump(),
                true,
                keys.sprint());
    }
}
