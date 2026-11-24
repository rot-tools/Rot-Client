package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Illegal edition sprint input override. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerAutoSprintMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean rotclient$autoSprint(Input input) {
        return input.sprint() || IllegalEdition.autoSprintEnabled();
    }
}
