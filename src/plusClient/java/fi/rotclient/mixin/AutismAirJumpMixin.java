// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client AutismLivingEntityMovementMixin; local player only.
package fi.rotclient.mixin;

import fi.rotclient.AutismAirJumpRuntime;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class AutismAirJumpMixin {
    @Shadow protected boolean jumping;
    @Shadow private int noJumpDelay;

    @Inject(method="aiStep", at=@At("HEAD"))
    private void rotclient$prepareAirJump(CallbackInfo ci) {
        AutismAirJumpRuntime.prepare((LivingEntity)(Object)this);
    }

    @Inject(method="aiStep", at=@At(value="FIELD", target="Lnet/minecraft/world/entity/LivingEntity;jumping:Z", opcode=Opcodes.GETFIELD))
    private void rotclient$airJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (jumping && noJumpDelay == 0 && AutismAirJumpRuntime.mayJump(entity)) {
            entity.jumpFromGround();
            noJumpDelay = 10;
        }
    }

    @Inject(method="jumpFromGround", at=@At("HEAD"))
    private void rotclient$consumeAirJump(CallbackInfo ci) {
        AutismAirJumpRuntime.jumped((LivingEntity)(Object)this);
    }
}
