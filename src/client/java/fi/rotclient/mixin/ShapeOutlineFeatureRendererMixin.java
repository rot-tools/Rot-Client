package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fi.rotclient.BlockOutlineRuntime;
import fi.rotclient.ClientBoundaryGuard;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Block Outline: swaps the edge loop of the aimed-at block's selection box for
 * the custom color / rainbow. Vanilla still draws it whenever the module is off
 * or the custom draw fails.
 */
@Mixin(ShapeOutlineFeatureRenderer.class)
abstract class ShapeOutlineFeatureRendererMixin {
    @WrapOperation(
            method = "buildGroup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllEdges(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
    private void rotclient$blockOutline(
            VoxelShape shape,
            Shapes.DoubleLineConsumer vanillaEdges,
            Operation<Void> original,
            @Local ShapeOutlineFeatureRenderer.Submit submit,
            @Local VertexConsumer builder) {
        boolean drawn = ClientBoundaryGuard.call(
                "BLOCK_OUTLINE",
                () -> BlockOutlineRuntime.draw(
                        submit.renderType(), shape, submit.pose(), builder, submit.width()),
                Boolean.FALSE);
        if (!drawn) {
            original.call(shape, vanillaEdges);
        }
    }
}
