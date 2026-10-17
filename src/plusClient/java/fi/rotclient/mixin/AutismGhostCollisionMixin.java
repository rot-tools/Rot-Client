// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fi.rotclient.AutismAirJumpRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
abstract class AutismGhostCollisionMixin {
    @ModifyReturnValue(method="collide", at=@At("RETURN"))
    private Vec3 rotclient$ghostSupport(Vec3 resolved) {
        return AutismAirJumpRuntime.collision((Entity)(Object)this, resolved);
    }
}
