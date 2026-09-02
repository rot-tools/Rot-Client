package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Local render suppression for Render Optimizer + Hide Players.
 */
@Mixin(EntityRenderer.class)
abstract class EntityRendererSuppressMixin<T extends Entity> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void rotclient$suppressEntityRender(
            T entity,
            Frustum frustum,
            double camX,
            double camY,
            double camZ,
            CallbackInfoReturnable<Boolean> cir) {
        if (QolVisualRuntime.shouldSuppressEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z"))
    private boolean rotclient$hideEntityFire(Entity entity) {
        if (QolVisualRuntime.shouldHideEntityFire()) {
            return false;
        }
        return entity.displayFireAnimation();
    }
}
