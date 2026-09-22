package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fi.rotclient.BlockOutlineRuntime;
import fi.rotclient.ClientBoundaryGuard;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Block Outline: swaps the aimed-at block's selection box for the custom
 * color / rainbow. {@code renderBlockOutline} calls {@code renderHitOutline}
 * twice when the high-contrast setting is on (a black backing pass, then the
 * real one); ordinal 1 targets only the second call so the backing pass
 * always stays vanilla. Vanilla still draws the real pass too, whenever the
 * module is off or the custom draw fails.
 */
@Mixin(LevelRenderer.class)
abstract class BlockOutlineMixin {
    @WrapOperation(
            method = "renderBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                            + "DDD"
                            + "Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;"
                            + "IF)V",
                    ordinal = 1))
    private void rotclient$blockOutline(
            LevelRenderer instance,
            PoseStack poseStack,
            VertexConsumer builder,
            double camX,
            double camY,
            double camZ,
            BlockOutlineRenderState state,
            int color,
            float width,
            Operation<Void> original) {
        boolean drawn = ClientBoundaryGuard.call(
                "BLOCK_OUTLINE",
                () -> BlockOutlineRuntime.draw(
                        poseStack, builder, state.shape(), state.pos(), camX, camY, camZ, width),
                Boolean.FALSE);
        if (!drawn) {
            original.call(instance, poseStack, builder, camX, camY, camZ, state, color, width);
        }
    }
}
