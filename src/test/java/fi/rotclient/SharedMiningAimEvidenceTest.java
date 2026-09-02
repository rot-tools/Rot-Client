package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class SharedMiningAimEvidenceTest {
    @AfterEach
    void tearDown() {
        SharedMiningAimEvidence.reset();
    }

    @Test
    void nearbyBlockCountsAsMiningProximity() {
        SharedMiningAimEvidence.recordAimForTest(
                new BlockPos(10, 64, 10), 1_000L);
        assertTrue(SharedMiningAimEvidence.isNearRecentAim(
                new BlockPos(11, 64, 10)));
        assertTrue(SharedMiningAimEvidence.isNearRecentAim(
                new BlockPos(10, 65, 12)));
        assertFalse(SharedMiningAimEvidence.isNearRecentAim(
                new BlockPos(20, 64, 10)));
    }
}
