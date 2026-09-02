package fi.rotclient.mixin;

import fi.rotclient.NameHiderRuntime;
import fi.rotclient.SkyblockFlavorRuntime;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
abstract class EntityRendererNameHiderMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void rotclient$hideNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(SkyblockFlavorRuntime.rewriteNameTag(NameHiderRuntime.apply(cir.getReturnValue())));
    }

    @Inject(
            method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("RETURN"))
    private void rotclient$hideExtractedNameTags(
            T entity,
            S state,
            float tickDelta,
            double x,
            double z,
            CallbackInfo ci) {
        if (state == null) {
            return;
        }
        if (state.nameTag != null) {
            state.nameTag = SkyblockFlavorRuntime.rewriteNameTag(NameHiderRuntime.apply(state.nameTag));
        }
        if (state.scoreText != null) {
            state.scoreText = SkyblockFlavorRuntime.rewriteNameTag(NameHiderRuntime.apply(state.scoreText));
        }
    }
}
