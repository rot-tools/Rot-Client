package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Collection must accept OTHER_MINED when Current Session collection is active
 * even if DiagnosticRecorder / instrumentation is off. Tracker auto-pause and
 * live tracker disabled must not block generic observation.
 */
final class CurrentSessionCollectionIndependenceTest {
    @Test
    void collectionActiveAcceptsHardStoneWithoutLiveTracker() {
        Fixture fixture = start(TrackerSelection.GOLD, false, 10L);
        assertTrue(fixture.engine.isCollectionActive());
        assertFalse(fixture.engine.isObservationEnabled());

        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.HARD_STONE, 1L, 110L);

        assertEquals(1, snapshot(fixture, 120L)
                .entryCount(MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void cobbleDiamondMithrilTitaniumAcceptedAsOtherWhenGoldTarget() {
        TrackedMaterial[] materials = {
                TrackedMaterial.COBBLESTONE,
                TrackedMaterial.DIAMOND,
                TrackedMaterial.MITHRIL,
                TrackedMaterial.TITANIUM
        };
        for (TrackedMaterial material : materials) {
            Fixture fixture = start(TrackerSelection.GOLD, false, 10L);
            fixture.engine.onConfirmedMaterialBreak(material, 1, 100L);
            fixture.engine.onMaterialInventoryGain(material, 1L, 110L);
            assertEquals(
                    1,
                    snapshot(fixture, 120L)
                            .entryCount(MiningSessionCategory.OTHER_MINED),
                    "expected OTHER for " + material.id());
        }
    }

    @Test
    void collectionInactiveRejectsOtherGain() {
        Fixture fixture = fresh();
        assertFalse(fixture.engine.isCollectionActive());
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.HARD_STONE, 1L, 110L);
        assertEquals(0, fixture.engine.snapshot(120L)
                .map(s -> s.entryCount(MiningSessionCategory.OTHER_MINED))
                .orElse(0));
    }

    @Test
    void stoppingCollectionStopsFurtherOtherCredits() {
        Fixture fixture = start(TrackerSelection.GOLD, false, 10L);
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.HARD_STONE, 1L, 110L);
        fixture.engine.onDiagnosticStop(200L);
        assertFalse(fixture.engine.isCollectionActive());

        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.COBBLESTONE, 1, 300L);
        fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.COBBLESTONE, 1L, 310L);
        assertEquals(1, fixture.engine.snapshot(320L)
                .map(s -> s.entryCount(MiningSessionCategory.OTHER_MINED))
                .orElse(0));
    }

    @Test
    void instrumentationSinkOffDoesNotBlockDomainCredits() {
        // CaptureSink returns isActive=false → no diagnostic markers required
        // for domain appends.
        Fixture fixture = fresh(false);
        start(fixture, TrackerSelection.GOLD, false, 10L);
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.HARD_STONE, 1L, 110L);
        assertEquals(1, snapshot(fixture, 120L)
                .entryCount(MiningSessionCategory.OTHER_MINED));
        assertTrue(fixture.sink.markers.isEmpty());
    }

    private static Fixture fresh() {
        return fresh(true);
    }

    private static Fixture fresh(boolean sinkActive) {
        CaptureSink sink = new CaptureSink(sinkActive);
        MiningSessionEngine engine = new MiningSessionEngine(sink);
        return new Fixture(sink, engine);
    }

    private static Fixture start(
            TrackerSelection selection,
            boolean trackerEnabled,
            long startedAt) {
        Fixture fixture = fresh();
        start(fixture, selection, trackerEnabled, startedAt);
        return fixture;
    }

    private static void start(
            Fixture fixture,
            TrackerSelection selection,
            boolean trackerEnabled,
            long startedAt) {
        fixture.engine.onDiagnosticStart(
                trackerEnabled,
                selection,
                MiningSessionParity.LiveBaseline.material(
                        selection.isMaterial()
                                ? selection
                                : TrackerSelection.GOLD,
                        Map.of(),
                        Map.of(),
                        startedAt),
                startedAt);
    }

    private static MiningSessionSnapshot snapshot(Fixture fixture, long at) {
        return fixture.engine.snapshot(at).orElseThrow();
    }

    private record Fixture(
            CaptureSink sink,
            MiningSessionEngine engine) {
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        private final boolean active;
        private final List<String> markers = new ArrayList<>();

        private CaptureSink(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void record(String marker, String details) {
            markers.add(marker);
        }
    }
}
