package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class MiningSessionRuntimeRegressionTest {
    @Test
    void materialCorrelationAcceptsObservedSevenSecondDelay() {
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                MiningSessionDirectBreakTracker.FamilyKey.material(
                        TrackedMaterial.GOLD);

        tracker.recordConfirmedBreak(family, 1L, 1_000L);

        Optional<MiningSessionDirectBreakTracker.Correlation> correlation =
                tracker.findCorrelation(
                        family,
                        MiningSessionDirectBreakTracker.EvidenceChannel
                                .MATERIAL_QUANTITY,
                        1L,
                        true,
                        false,
                        1L,
                        8_320L);

        assertTrue(correlation.isPresent());
        assertTrue(tracker.consume(correlation.get(), 8_320L));
    }

    @Test
    void materialCorrelationRejectsAfterTenSecondWindow() {
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                MiningSessionDirectBreakTracker.FamilyKey.material(
                        TrackedMaterial.GOLD);

        tracker.recordConfirmedBreak(family, 1L, 1_000L);

        assertTrue(tracker.findCorrelation(
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel
                        .MATERIAL_QUANTITY,
                1L,
                true,
                false,
                1L,
                11_001L).isEmpty());
    }

    @Test
    void newestMatchingMaterialContextIsSelected() {
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                MiningSessionDirectBreakTracker.FamilyKey.material(
                        TrackedMaterial.GOLD);

        tracker.recordConfirmedBreak(family, 1L, 1_000L);
        tracker.recordConfirmedBreak(family, 1L, 1_100L);

        MiningSessionDirectBreakTracker.Correlation correlation =
                tracker.findCorrelation(
                        family,
                        MiningSessionDirectBreakTracker.EvidenceChannel
                                .MATERIAL_QUANTITY,
                        1L,
                        true,
                        false,
                        1L,
                        1_200L)
                        .orElseThrow();

        assertEquals("context-2", correlation.contextId());
    }
}
