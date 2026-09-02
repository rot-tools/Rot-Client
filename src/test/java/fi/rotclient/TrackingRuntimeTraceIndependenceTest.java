package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Domain OTHER collection must work with tracking runtime trace OFF.
 * Trace only records; it must never gate credits.
 */
final class TrackingRuntimeTraceIndependenceTest {
    @Test
    void traceDisabledDoesNotAffectCollectionActiveOtherCredits() {
        assertFalse(TrackingRuntimeTrace.isEnabled());

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
        assertTrue(engine.isCollectionActive());
        assertFalse(TrackingRuntimeTrace.isEnabled());

        engine.onConfirmedMaterialBreak(TrackedMaterial.HARD_STONE, 1, 100L);
        engine.onMaterialInventoryGain(TrackedMaterial.HARD_STONE, 5L, 110L);

        assertEquals(1, engine.snapshot(120L)
                .map(s -> s.entryCount(MiningSessionCategory.OTHER_MINED))
                .orElse(0));
    }
}
