package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
abstract class CameraEyeHeightMixin {
    @Shadow
    private Entity entity;

    @ModifyExpressionValue(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 1))
    private float rotclient$instantSneak(float original) {
        if (!QolVisualRuntime.instantSneakEnabled() || this.entity == null) {
            return original;
        }
        return QolVisualRuntime.adjustedEyeHeight(this.entity.getEyeHeight());
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F"))
    private float rotclient$eyeHeightFix(float original) {
        return QolVisualRuntime.adjustedEyeHeight(original);
    }
}
