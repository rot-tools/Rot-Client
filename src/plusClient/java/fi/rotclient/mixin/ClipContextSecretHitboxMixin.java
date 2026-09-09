package fi.rotclient.mixin;

import fi.rotclient.SecretHitboxesRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClipContext.class)
abstract class ClipContextSecretHitboxMixin {
    @Inject(method = "getBlockShape", at = @At("RETURN"), cancellable = true)
    private void rotclient$secretHitbox(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir) {
        VoxelShape override = SecretHitboxesRuntime.shapeFor(state, pos);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
