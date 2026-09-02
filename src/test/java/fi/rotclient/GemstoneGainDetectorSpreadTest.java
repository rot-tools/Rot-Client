package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class GemstoneGainDetectorSpreadTest {
    @Test
    void directAndSpreadBlocksReachOneListenerAndOneBatch() {
        List<GemstoneType> observedBlocks = new ArrayList<>();
        GemstoneGainDetector detector = detector(observedBlocks);

        detector.recordAttackTarget(
                position(0, 64, 0),
                GemstoneType.RUBY,
                10_000L);

        detector.acceptBlockRemoval(
                position(0, 64, 0),
                GemstoneType.RUBY,
                true,
                10_100L,
                true);

        detector.acceptBlockRemoval(
                position(1, 64, 0),
                GemstoneType.RUBY,
                true,
                10_120L,
                true);

        assertEquals(
                List.of(GemstoneType.RUBY, GemstoneType.RUBY),
                observedBlocks);
        assertEquals(
                2,
                detector.batchSnapshot(
                                GemstoneType.RUBY,
                                10_200L)
                        .directBreaks());
    }

    @Test
    void mixedGemstoneSpreadKeepsGemstoneBatchesIsolated() {
        List<GemstoneType> observedBlocks = new ArrayList<>();
        GemstoneGainDetector detector = detector(observedBlocks);

        detector.recordAttackTarget(
                position(10, 70, 10),
                GemstoneType.RUBY,
                20_000L);

        detector.acceptBlockRemoval(
                position(10, 70, 10),
                GemstoneType.RUBY,
                true,
                20_100L,
                true);

        detector.acceptBlockRemoval(
                position(11, 70, 10),
                GemstoneType.TOPAZ,
                true,
                20_120L,
                true);

        assertEquals(
                List.of(GemstoneType.RUBY, GemstoneType.TOPAZ),
                observedBlocks);
        assertEquals(
                1,
                detector.batchSnapshot(
                                GemstoneType.RUBY,
                                20_200L)
                        .directBreaks());
        assertEquals(
                1,
                detector.batchSnapshot(
                                GemstoneType.TOPAZ,
                                20_200L)
                        .directBreaks());
    }

    @Test
    void unrelatedGemstoneRemovalDoesNotReachListenerOrBatch() {
        List<GemstoneType> observedBlocks = new ArrayList<>();
        GemstoneGainDetector detector = detector(observedBlocks);

        detector.recordAttackTarget(
                position(0, 64, 0),
                GemstoneType.JADE,
                30_000L);

        assertNull(
                detector.acceptBlockRemoval(
                        position(2, 64, 0),
                        GemstoneType.JADE,
                        true,
                        30_100L,
                        true));
        assertEquals(List.of(), observedBlocks);
        assertEquals(
                0,
                detector.batchSnapshot(
                                GemstoneType.JADE,
                                30_200L)
                        .directBreaks());
    }

    private static GemstoneGainDetector detector(
            List<GemstoneType> observedBlocks) {
        return new GemstoneGainDetector(
                new GemstoneGainDetector.Listener() {
                    @Override
                    public void onBlock(
                            GemstoneType gemstone,
                            long epochMillis) {
                        observedBlocks.add(gemstone);
                    }

                    @Override
                    public void onGain(
                            GemstoneType gemstone,
                            GemstoneTier tier,
                            long amount,
                            String source) {
                    }
                });
    }

    private static GemstoneDirectBreakTracker.Position position(
            int x,
            int y,
            int z) {
        return new GemstoneDirectBreakTracker.Position(x, y, z);
    }
}
