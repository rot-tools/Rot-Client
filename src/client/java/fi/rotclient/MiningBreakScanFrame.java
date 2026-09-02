package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** One immutable nearby-world sample shared by every material detector. */
final class MiningBreakScanFrame {
    record Sample(BlockPos pos, BlockState state) {
    }

    private final List<Sample> samples;
    private final long captureNanos;

    MiningBreakScanFrame(List<Sample> samples, long captureNanos) {
        this.samples = List.copyOf(samples);
        this.captureNanos = Math.max(0L, captureNanos);
    }

    static MiningBreakScanFrame capture(Minecraft client) {
        if (client.level == null || client.player == null) return null;
        long started = System.nanoTime();
        BlockPos center = client.player.blockPosition();
        List<Sample> samples = new ArrayList<>(
                MiningBreakScanPlan.POSITIONS_PER_FRAME);
        for (int dx = -MiningBreakScanPlan.HORIZONTAL_RADIUS;
                dx <= MiningBreakScanPlan.HORIZONTAL_RADIUS; dx++) {
            for (int dy = -MiningBreakScanPlan.VERTICAL_RADIUS;
                    dy <= MiningBreakScanPlan.VERTICAL_RADIUS; dy++) {
                for (int dz = -MiningBreakScanPlan.HORIZONTAL_RADIUS;
                        dz <= MiningBreakScanPlan.HORIZONTAL_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz).immutable();
                    samples.add(new Sample(pos, client.level.getBlockState(pos)));
                }
            }
        }
        return new MiningBreakScanFrame(samples, System.nanoTime() - started);
    }

    List<Sample> samples() {
        return samples;
    }

    long captureNanos() {
        return captureNanos;
    }
}
