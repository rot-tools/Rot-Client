package fi.rotclient.mixin;

import fi.rotclient.HateDoorsRuntime;
import fi.rotclient.SkyBlockUtilityRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class LevelHateDoorsMixin {
    @Inject(
            method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"),
            cancellable = true)
    private void rotclient$hateDoorsGlass(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!((Object) this instanceof ClientLevel)) {
            return;
        }
        BlockState rewritten = HateDoorsRuntime.rewrite(pos, cir.getReturnValue());
        if (rewritten == null) {
            rewritten = SkyBlockUtilityRuntime.rewriteCarpet(cir.getReturnValue());
        }
        if (rewritten != null) {
            cir.setReturnValue(rewritten);
        }
    }
}
