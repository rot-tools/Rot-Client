package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fi.rotclient.ViewmodelRuntime;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class LivingEntityViewmodelMixin {
    @ModifyExpressionValue(
            method = "getCurrentSwingDuration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectUtil;hasDigSpeed(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean rotclient$ignoreHaste(boolean original) {
        return ViewmodelRuntime.ignoreSwingEffects((LivingEntity) (Object) this) ? false : original;
    }

    @ModifyExpressionValue(
            method = "getCurrentSwingDuration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean rotclient$ignoreMiningFatigue(boolean original) {
        return ViewmodelRuntime.ignoreSwingEffects((LivingEntity) (Object) this) ? false : original;
    }

    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int rotclient$swingDuration(int original) {
        return ViewmodelRuntime.swingDuration((LivingEntity) (Object) this, original);
    }
}
