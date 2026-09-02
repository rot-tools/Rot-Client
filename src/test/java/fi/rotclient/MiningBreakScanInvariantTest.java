package fi.rotclient;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MiningBreakScanInvariantTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void sharedFrameDefensivelyCopiesSamples() {
        List<MiningBreakScanFrame.Sample> source = new ArrayList<>();
        source.add(new MiningBreakScanFrame.Sample(
                BlockPos.ZERO, Blocks.STONE.defaultBlockState()));

        MiningBreakScanFrame frame = new MiningBreakScanFrame(source, 10L);
        source.clear();

        assertEquals(1, frame.samples().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> frame.samples().clear());
    }

    @Test
    void detectorPlanRejectsUnboundedCounts() {
        assertEquals(2_475, MiningBreakScanPlan.sharedWorldReads(1));
        assertEquals(2_475, MiningBreakScanPlan.sharedWorldReads(64));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningBreakScanPlan.sharedWorldReads(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningBreakScanPlan.legacyWorldReads(65));
    }
}
