// SPDX-License-Identifier: GPL-3.0-only
// FreeLook camera section from AUTISM Client AutismCameraMixin by Melonik/contributors.
package fi.rotclient.mixin;

import fi.rotclient.AutismFreeLookRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class AutismFreeLookCameraMixin {
    @Shadow private boolean detached;
    @Shadow private Entity entity;
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void move(float distance, float vertical, float horizontal);
    @Shadow private float getMaxZoom(float zoom) { throw new AssertionError(); }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void rotclient$freeLookCamera(DeltaTracker delta, CallbackInfo ci) {
        if (!AutismFreeLookRuntime.active() || entity != net.minecraft.client.Minecraft.getInstance().player) return;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        detached = true;
        setPosition(Mth.lerp(partial, entity.xo, entity.getX()),
                Mth.lerp(partial, entity.yo, entity.getY()) + entity.getEyeHeight(),
                Mth.lerp(partial, entity.zo, entity.getZ()));
        boolean front = AutismFreeLookRuntime.front();
        setRotation(AutismFreeLookRuntime.yaw() + (front ? 180 : 0),
                (front ? -1 : 1) * AutismFreeLookRuntime.pitch());
        move(-getMaxZoom(4 * (entity instanceof LivingEntity living ? living.getScale() : 1)), 0, 0);
    }
}
