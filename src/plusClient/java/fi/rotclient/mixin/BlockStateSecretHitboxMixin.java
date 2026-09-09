package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import fi.rotclient.SecretHitboxesRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Selection-box outline mixin: the selection box uses
 * {@code getShape}, which ClipContext does not cover.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateSecretHitboxMixin {
    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true)
    private void rotclient$secretHitboxOutline(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        VoxelShape override = SecretHitboxesRuntime.shapeFor((BlockState) (Object) this, pos);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideDiorite(CallbackInfoReturnable<RenderShape> cir) {
        if (QolVisualRuntime.shouldHideDiorite((BlockState) (Object) this)) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
