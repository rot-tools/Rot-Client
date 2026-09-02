package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Engine inventory-gain path returns ObservationResult so runtime can trace
 * accept/reject without relying on HUD side effects.
 */
final class MaterialInventoryGainResultWiringTest {
    @Test
    void acceptedOtherGainReturnsAppendedResult() {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                false,
                TrackerSelection.GOLD,
                MiningSessionParity.LiveBaseline.material(
                        TrackerSelection.GOLD,
                        Map.of(),
                        Map.of(),
                        10L),
                10L);
        engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);
        MiningSessionShadowObserver.ObservationResult result =
                engine.onMaterialInventoryGain(
                        TrackedMaterial.MITHRIL, 3L, 110L);
        assertTrue(result.appended());
        assertEquals(1, engine.snapshot(120L)
                .map(s -> s.entryCount(MiningSessionCategory.OTHER_MINED))
                .orElse(0));
    }

    @Test
    void missingCorrelationReturnsRejectedResult() {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                MiningSessionParity.LiveBaseline.material(
                        TrackerSelection.GOLD,
                        Map.of(),
                        Map.of(),
                        10L),
                10L);
        MiningSessionShadowObserver.ObservationResult result =
                engine.onMaterialInventoryGain(
                        TrackedMaterial.HARD_STONE, 10L, 50L);
        assertTrue(!result.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_MINING_CORRELATION,
                result.reason());
    }
}
