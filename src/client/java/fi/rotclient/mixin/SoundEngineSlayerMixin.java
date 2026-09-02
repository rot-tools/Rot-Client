package fi.rotclient.mixin;

import fi.rotclient.DianaRuntime;
import fi.rotclient.FishingSuiteRuntime;
import fi.rotclient.ForagingRuntime;
import fi.rotclient.IotaRuntime;
import fi.rotclient.SlayerRuntime;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels optional local Slayer fight sounds (Voidgloom, Vampire, Sven wolves).
 * Pitch/volume getters NPE until {@link SoundInstance#resolve} fills
 * {@code AbstractSoundInstance.sound}, so this runs after that call.
 */
@Mixin(SoundEngine.class)
abstract class SoundEngineSlayerMixin {
    @Inject(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SoundInstance;resolve(Lnet/minecraft/client/sounds/SoundManager;)Lnet/minecraft/client/sounds/WeighedSoundEvents;",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void rotclient$filterSlayerSound(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (sound == null || sound.getIdentifier() == null || sound.getSound() == null) {
            return;
        }
        String id = sound.getIdentifier().toString();
        float pitch = sound.getPitch();
        float volume = sound.getVolume();
        ForagingRuntime.observeSound(id, pitch, volume);
        if (SlayerRuntime.shouldSuppressSound(id)
                || FishingSuiteRuntime.shouldSuppressSound(id, pitch)
                || ForagingRuntime.shouldMuteSound(id, volume)
                || IotaRuntime.shouldMuteSound(id)
                || DianaRuntime.shouldMuteSound(
                        id,
                        pitch,
                        volume,
                        sound.getX() == 0.0 && sound.getY() == 0.0 && sound.getZ() == 0.0)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
