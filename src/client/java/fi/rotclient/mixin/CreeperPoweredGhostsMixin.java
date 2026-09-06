package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fi.rotclient.GhostsRuntime;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Strip the vanilla charged overlay on Mist ghosts so the styled box is
 * what you see, matching NoFrills Ghost Vision's {@code isPowered} gate.
 */
@Mixin(Creeper.class)
abstract class CreeperPoweredGhostsMixin {
    @ModifyReturnValue(method = "isPowered", at = @At("RETURN"))
    private boolean rotclient$hideGhostChargedOverlay(boolean original) {
        if (!original) {
            return false;
        }
        return GhostsRuntime.shouldHideChargedOverlay((Creeper) (Object) this) ? false : original;
    }
}
