package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelNoRenderMixin {
    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (QolVisualRuntime.shouldHideBreakParticles()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (QolVisualRuntime.shouldHideBreakParticles()) {
            ci.cancel();
        }
    }
}
