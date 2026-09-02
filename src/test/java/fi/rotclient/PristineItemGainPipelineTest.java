package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class PristineItemGainPipelineTest {
    @Test
    void acceptedNonTargetPristineCreditsCanonicalCurrentSessionExactlyOnce() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.RUBY);
        RotClientCurrentSession current = new RotClientCurrentSession();
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        PristineMessageParser.Reward reward =
                new PristineMessageParser.Reward(GemstoneType.TOPAZ, 4L);

        PristineItemGainPipeline.Outcome first =
                PristineItemGainPipeline.submit(
                        engine,
                        current,
                        reward,
                        SkyBlockArea.CRYSTAL_HOLLOWS,
                        110L);
        PristineItemGainPipeline.Outcome replay =
                PristineItemGainPipeline.submit(
                        engine,
                        current,
                        reward,
                        SkyBlockArea.CRYSTAL_HOLLOWS,
                        111L);

        assertTrue(first.creditedOthers());
        assertFalse(replay.creditedOthers());
        assertEquals(1, engine.snapshot(120L).orElseThrow().entryCount());
        RotClientCurrentSessionConfig.SessionItemRecord row =
                current.snapshotConfig().items.getFirst();
        assertEquals("FLAWED_TOPAZ_GEM", row.itemId());
        assertEquals(4L, row.quantity());
        assertEquals(SessionSourceType.MINING, row.source());
        assertEquals(MiningClassification.OTHER,
                row.miningClassification());
    }

    @Test
    void targetPristineAndPausedCurrentSessionAreNeverCreditedAsOthers() {
        MiningSessionEngine targetEngine = activeEngine(TrackerSelection.TOPAZ);
        RotClientCurrentSession targetCurrent = new RotClientCurrentSession();
        targetEngine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        PristineMessageParser.Reward reward =
                new PristineMessageParser.Reward(GemstoneType.TOPAZ, 4L);
        assertFalse(PristineItemGainPipeline.submit(
                targetEngine,
                targetCurrent,
                reward,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                110L).creditedOthers());
        assertTrue(targetCurrent.snapshotConfig().items.isEmpty());

        MiningSessionEngine pausedEngine = activeEngine(TrackerSelection.RUBY);
        RotClientCurrentSession pausedCurrent = new RotClientCurrentSession();
        pausedCurrent.pause(90L);
        pausedEngine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        assertFalse(PristineItemGainPipeline.submit(
                pausedEngine,
                pausedCurrent,
                reward,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                110L).creditedOthers());
        assertTrue(pausedCurrent.snapshotConfig().items.isEmpty());
    }

    @Test
    void equalPristineRewardsFromTwoBreaksBothReachCanonicalSession() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.RUBY);
        RotClientCurrentSession current = new RotClientCurrentSession();
        PristineMessageParser.Reward reward =
                new PristineMessageParser.Reward(GemstoneType.TOPAZ, 4L);
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 101L);

        PristineItemGainPipeline.Outcome first =
                PristineItemGainPipeline.submit(
                        engine, current, reward,
                        SkyBlockArea.CRYSTAL_HOLLOWS, 110L);
        PristineItemGainPipeline.Outcome second =
                PristineItemGainPipeline.submit(
                        engine, current, reward,
                        SkyBlockArea.CRYSTAL_HOLLOWS, 111L);

        assertTrue(first.creditedOthers());
        assertTrue(second.creditedOthers());
        assertFalse(first.observation().contextId().equals(
                second.observation().contextId()));
        assertEquals(8L,
                current.snapshotConfig().items.getFirst().quantity());
    }

    private static MiningSessionEngine activeEngine(TrackerSelection selection) {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                true,
                selection,
                MiningSessionParity.LiveBaseline.gemstone(
                        selection, Map.of(), 0L, 0L, 10L),
                10L);
        return engine;
    }
}
