package fi.rotclient.mixin;

import fi.rotclient.AutoSprintPolicy;
import fi.rotclient.RotClientClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Plus-only vanilla sprint input override. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerAutoSprintMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean rotclient$autoSprint(Input input) {
        return AutoSprintPolicy.resolveSprintInput(
                input.sprint(), RotClientClient.isAutoSprintEnabled());
    }
}
