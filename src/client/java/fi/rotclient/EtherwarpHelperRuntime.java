package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Etherwarp destination prediction used by the shared guess overlay.
 * Left-click warp / auto-shift live in {@link EtherwarpPlusRuntime}.
 */
public final class EtherwarpHelperRuntime {
    private EtherwarpHelperRuntime() {
    }

    static void clear() {
        // Plus warp/sneak state is owned by EtherwarpPlusRuntime.
    }

    static boolean hasValidLookTarget(Minecraft client, QolUtilityConfig qol) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        LocalPlayer player = client.player;
        var eye = player.getEyePosition();
        var look = player.getViewVector(1.0F);
        java.util.Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(eye.x, eye.y, eye.z),
                java.util.Optional.empty(),
                qol.etherwarpUseServerPosition,
                new EtherwarpPredictor.Vec3d(look.x, look.y, look.z),
                EtherwarpPredictor.DEFAULT_RANGE,
                occupancy(client.level));
        return EtherwarpPredictor.canWarpTo(target);
    }

    private static EtherwarpPredictor.BlockOccupancy occupancy(BlockGetter level) {
        return new EtherwarpPredictor.BlockOccupancy() {
            @Override
            public boolean isSolidSurface(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return !state.getCollisionShape(level, pos).isEmpty();
            }

            @Override
            public boolean isStandSpaceClear(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return state.getCollisionShape(level, pos).isEmpty()
                        && level.getFluidState(pos).isEmpty();
            }
        };
    }
}
