package fi.rotclient.mixin;

import fi.rotclient.SlayerRuntime;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Changes only the local render state of background entities during a verified
 * Slayer fight. The original entity remains untouched for all game logic.
 */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererSlayerTransparencyMixin {
    /** Render states are short-lived; weak keys avoid retaining a renderer's state cache. */
    private static final Map<LivingEntityRenderState, Integer> ROTCLIENT_FADED_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN"))
    private void rotclient$markBackgroundEntityTranslucent(
            LivingEntity entity,
            LivingEntityRenderState state,
            float tickDelta,
            CallbackInfo ci) {
        if (state == null) {
            return;
        }
        Integer tint = SlayerRuntime.slayerFadeTintFor(entity);
        if (tint != null) {
            // Vanilla's translucent render path is selected from this state flag.
            state.isInvisible = true;
            ROTCLIENT_FADED_STATES.put(state, tint);
        } else {
            ROTCLIENT_FADED_STATES.remove(state);
        }
    }

    /**
     * The model tint hook retains the render state as an explicit method
     * argument. This is stable across the 26.2 renderer and avoids trying to
     * capture it from the {@code ARGB.multiply} call site.
     */
    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void rotclient$applyConfiguredFadeStrength(
            LivingEntityRenderState state,
            CallbackInfoReturnable<Integer> callback) {
        Integer configuredTint = ROTCLIENT_FADED_STATES.get(state);
        if (configuredTint != null) {
            callback.setReturnValue(configuredTint);
        }
    }
}
