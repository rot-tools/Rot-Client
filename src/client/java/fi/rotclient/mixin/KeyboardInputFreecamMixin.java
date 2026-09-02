package fi.rotclient.mixin;

import fi.rotclient.FreecamRuntime;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WASD/jump/sneak fly the detached camera. Zero the standing player's input
 * so the body stays still and no movement keys go in player-input packets.
 * {@code moveVector} lives on {@link ClientInput}; shadowing it on
 * {@link KeyboardInput} fails APPLY on playable 26.2 and aborts server join.
 */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputFreecamMixin extends ClientInput {
    @Inject(method = "tick", at = @At("RETURN"))
    private void rotclient$zeroPlayerInputForFreecam(CallbackInfo ci) {
        if (!FreecamRuntime.active()) {
            return;
        }
        this.keyPresses = new Input(false, false, false, false, false, false, false);
        this.moveVector = Vec2.ZERO;
    }
}
