package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
abstract class ItemEntityRendererMixin {
    private static final java.util.Map<ItemEntityRenderState, Float> ROTCLIENT_SLAYER_SCALES =
            new java.util.WeakHashMap<>();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void rotclient$rememberSlayerScale(
            ItemEntity entity,
            ItemEntityRenderState state,
            float partialTick,
            CallbackInfo ci) {
        ROTCLIENT_SLAYER_SCALES.put(state, fi.rotclient.SlayerRuntime.itemDropScale(entity));
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                    shift = At.Shift.AFTER))
    private void rotclient$itemScale(
            ItemEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        float scale = QolVisualRuntime.itemEntityScale()
                * ROTCLIENT_SLAYER_SCALES.getOrDefault(state, 1.0F);
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
