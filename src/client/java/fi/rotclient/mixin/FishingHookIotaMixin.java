package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.rotclient.IotaRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fishing-bobber owner fix: ignore the armor stand whose id is hook id + 1 so
 * the bobber is not stuck to that extra owner.
 */
@Mixin(FishingHook.class)
abstract class FishingHookIotaMixin {
    @WrapOperation(
            method = "onSyncedDataUpdated",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private Entity rotclient$ignoreHookArmorStand(
            Level world,
            int id,
            Operation<Entity> original) {
        Entity entity = original.call(world, id);
        if (!IotaRuntime.shouldFixFishingHook() || entity == null) {
            return entity;
        }
        FishingHook hook = (FishingHook) (Object) this;
        if (entity instanceof ArmorStand stand && stand.getId() == hook.getId() + 1) {
            return null;
        }
        return entity;
    }
}
