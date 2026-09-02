package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionParityTest {
    @Test
    void noComparisonIsNotChecked() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.Snapshot snapshot = fixture.parity.snapshot();

        assertEquals(MiningSessionParity.Status.NOT_CHECKED, snapshot.status());
        assertEquals(0L, snapshot.mismatchCount());
        assertNull(snapshot.lastMismatch());
    }

    @Test
    void matchingGemstoneTierQuantityUsesLiveBaseline() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        80L,
                        180L,
                        1_100L));

        assertMatch(result, 80L, 80L);
        assertTrue(fixture.sink.contains("MINING_SESSION_PARITY_OK"));
    }

    @Test
    void mismatchingGemstoneTierRecordsImmutableDetail() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        79L,
                        180L,
                        1_100L));

        assertEquals(MiningSessionParity.Status.MISMATCH, result.status());
        assertTrue(result.compared());
        assertEquals(80L, result.liveQuantity());
        assertEquals(79L, result.shadowQuantity());
        assertEquals(1L, result.mismatchCount());
        assertEquals("ROUGH_RUBY_GEM", result.mismatchDetail().resourceId());
        assertEquals(result.mismatchDetail(),
                fixture.parity.snapshot().lastMismatch());
        assertTrue(fixture.sink.contains(
                "MINING_SESSION_PARITY_MISMATCH"));
    }

    @Test
    void matchingRoughEquivalentUsesCompatibleUnits() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneRoughEquivalent(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        400L,
                        660L,
                        1_100L));

        assertMatch(result, 400L, 400L);
        assertEquals(
                MiningSessionParity.ComparisonUnit.ROUGH_EQUIVALENT,
                result.comparisonUnit());
    }

    @Test
    void mismatchingRoughEquivalentDoesNotUsePhysicalItemCount() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneRoughEquivalent(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        5L,
                        660L,
                        1_100L));

        assertEquals(MiningSessionParity.Status.MISMATCH, result.status());
        assertEquals(400L, result.liveQuantity());
        assertEquals(5L, result.shadowQuantity());
    }

    @Test
    void matchingGemstoneBlocksUsesSeparateBaseline() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneBlocks(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        3L,
                        13L,
                        1_100L));

        assertMatch(result, 3L, 3L);
        assertEquals(
                MiningSessionParity.ComparisonUnit.BLOCK_COUNT,
                result.comparisonUnit());
    }

    @Test
    void blockAndItemUnitsCannotBeMixed() {
        Fixture fixture = gemstoneFixture();
        MiningSessionParity.Sample mixed = new MiningSessionParity.Sample(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                MiningSessionParity.Domain.GEMSTONE_BLOCKS,
                MiningSessionParity.ComparisonUnit.ITEM_QUANTITY,
                null,
                GemstoneType.RUBY,
                null,
                3L,
                13L,
                1_100L);

        MiningSessionParity.ComparisonResult result =
                fixture.parity.compare(mixed);

        assertEquals(MiningSessionParity.Status.INCOMPARABLE, result.status());
        assertFalse(result.compared());
        assertEquals(MiningSessionParity.Status.INCOMPARABLE,
                fixture.parity.snapshot().status());
        assertTrue(fixture.sink.markers.isEmpty());
    }

    @Test
    void matchingMaterialNormalizedQuantityUsesVerifiedBaseline() {
        Fixture fixture = materialFixture(
                TrackerSelection.GOLD,
                Map.of(TrackedMaterial.GOLD, 1_000L),
                Map.of(TrackedMaterial.GOLD, 20L));

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.materialNormalized(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        TrackedMaterial.GOLD,
                        160L,
                        1_160L,
                        1_100L));

        assertMatch(result, 160L, 160L);
        assertEquals(
                MiningSessionParity.ComparisonUnit.RAW_EQUIVALENT,
                result.comparisonUnit());
    }

    @Test
    void unsupportedMaterialComparisonIsIncomparable() {
        Fixture fixture = materialFixture(
                TrackerSelection.MITHRIL_TITANIUM,
                Map.of(TrackedMaterial.MITHRIL, 100L),
                Map.of());

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.materialNormalized(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        TrackedMaterial.TUNGSTEN,
                        1L,
                        1L,
                        1_100L));

        assertEquals(MiningSessionParity.Status.INCOMPARABLE, result.status());
        assertFalse(result.compared());
        assertEquals(0L, result.mismatchCount());
    }

    @Test
    void otherMinedIsExcludedWithoutChangingTargetParity() {
        Fixture fixture = gemstoneFixture();
        fixture.parity.compare(MiningSessionParity.Sample.gemstoneTier(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                80L,
                180L,
                1_100L));

        MiningSessionParity.ComparisonResult ignored = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        fixture.context,
                        MiningSessionCategory.OTHER_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        1L,
                        300L,
                        1_101L));

        assertEquals(MiningSessionParity.Status.INCOMPARABLE, ignored.status());
        assertFalse(ignored.compared());
        assertEquals(MiningSessionParity.Status.MATCH,
                fixture.parity.snapshot().status());
        assertEquals(0L, fixture.parity.snapshot().mismatchCount());
        assertEquals(1, fixture.sink.markers.size());
    }

    @Test
    void wrongTargetEpochIsNotComparedOrRecorded() {
        Fixture fixture = gemstoneFixture();
        MiningSessionParity.Context stale = new MiningSessionParity.Context(
                fixture.context.sessionId(),
                fixture.context.sessionEpoch(),
                fixture.context.selectionEpoch() - 1L,
                fixture.context.selection(),
                fixture.context.startedAtMillis());

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        stale,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        1L,
                        180L,
                        1_100L));

        assertEquals(MiningSessionParity.Status.INCOMPARABLE, result.status());
        assertFalse(result.compared());
        assertEquals(MiningSessionParity.Status.NOT_CHECKED,
                fixture.parity.snapshot().status());
        assertTrue(fixture.sink.markers.isEmpty());
    }

    @Test
    void targetTransitionStartsNewContextAndPreservesMismatchCount() {
        Fixture fixture = gemstoneFixture();
        fixture.parity.compare(MiningSessionParity.Sample.gemstoneTier(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                79L,
                180L,
                1_100L));
        MiningSessionParity.Context amberContext =
                new MiningSessionParity.Context(
                        1L,
                        1L,
                        4L,
                        TrackerSelection.AMBER,
                        2_000L);

        fixture.parity.beginContext(
                amberContext,
                MiningSessionParity.LiveBaseline.gemstone(
                        TrackerSelection.AMBER,
                        Map.of(GemstoneTier.ROUGH, 50L),
                        50L,
                        4L,
                        2_000L));

        assertEquals(MiningSessionParity.Status.NOT_CHECKED,
                fixture.parity.snapshot().status());
        assertEquals(1L, fixture.parity.snapshot().mismatchCount());
        assertNull(fixture.parity.snapshot().lastMismatch());
        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        amberContext,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.AMBER,
                        GemstoneTier.ROUGH,
                        10L,
                        60L,
                        2_100L));
        assertEquals(MiningSessionParity.Status.MATCH, result.status());
        assertTrue(result.compared());
        assertEquals(10L, result.liveQuantity());
        assertEquals(10L, result.shadowQuantity());
        assertEquals(1L, result.mismatchCount());
    }

    @Test
    void resetClearsParityState() {
        Fixture fixture = gemstoneFixture();
        fixture.parity.compare(MiningSessionParity.Sample.gemstoneTier(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                79L,
                180L,
                1_100L));

        fixture.parity.reset();

        assertEquals(MiningSessionParity.Status.NOT_CHECKED,
                fixture.parity.snapshot().status());
        assertEquals(0L, fixture.parity.snapshot().mismatchCount());
        assertNull(fixture.parity.snapshot().lastMismatch());
    }

    @Test
    void mismatchDoesNotMutateShadowFixture() {
        Fixture fixture = gemstoneFixture();
        long[] shadowFixture = {79L};

        fixture.parity.compare(MiningSessionParity.Sample.gemstoneTier(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                shadowFixture[0],
                180L,
                1_100L));

        assertEquals(79L, shadowFixture[0]);
    }

    @Test
    void mismatchDoesNotMutateLiveFixture() {
        Fixture fixture = gemstoneFixture();
        GemstoneLedger liveFixture = new GemstoneLedger();
        liveFixture.add(GemstoneTier.ROUGH, 180L);

        fixture.parity.compare(MiningSessionParity.Sample.gemstoneTier(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                79L,
                liveFixture.quantity(GemstoneTier.ROUGH),
                1_100L));

        assertEquals(180L,
                liveFixture.quantity(GemstoneTier.ROUGH));
        assertEquals(180L, liveFixture.totalRoughEquivalent());
    }

    @Test
    void repeatedIdenticalParityResultIsDeterministicAndIdempotent() {
        Fixture fixture = gemstoneFixture();
        MiningSessionParity.Sample sample =
                MiningSessionParity.Sample.gemstoneTier(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        79L,
                        180L,
                        1_100L);

        MiningSessionParity.ComparisonResult first =
                fixture.parity.compare(sample);
        MiningSessionParity.ComparisonResult repeated =
                fixture.parity.compare(sample);

        assertSame(first, repeated);
        assertEquals(1L, fixture.parity.snapshot().mismatchCount());
        assertEquals(1, fixture.sink.markers.size());
    }

    @Test
    void mismatchCounterIncrementsForDistinctComparisons() {
        Fixture fixture = gemstoneFixture();
        fixture.parity.compare(MiningSessionParity.Sample.gemstoneTier(
                fixture.context,
                MiningSessionCategory.TARGET_MINED,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                79L,
                180L,
                1_100L));

        MiningSessionParity.ComparisonResult second = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        158L,
                        260L,
                        1_101L));

        assertEquals(MiningSessionParity.Status.MISMATCH, second.status());
        assertEquals(2L, second.mismatchCount());
        assertEquals(2, fixture.sink.markers.size());
    }

    @Test
    void mismatchCounterOverflowIsAtomic() {
        CaptureSink sink = new CaptureSink(true);
        MiningSessionParity parity = new MiningSessionParity(
                sink,
                Long.MAX_VALUE);
        MiningSessionParity.Context context = rubyContext();
        parity.beginContext(context, rubyBaseline());
        MiningSessionParity.Sample mismatch =
                MiningSessionParity.Sample.gemstoneTier(
                        context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        79L,
                        180L,
                        1_100L);

        assertThrows(ArithmeticException.class,
                () -> parity.compare(mismatch));

        assertEquals(MiningSessionParity.Status.NOT_CHECKED,
                parity.snapshot().status());
        assertEquals(Long.MAX_VALUE,
                parity.snapshot().mismatchCount());
        assertNull(parity.snapshot().lastMismatch());
        assertTrue(sink.markers.isEmpty());
    }

    @Test
    void materialBlocksCanBeComparedWithoutMixingItemQuantity() {
        Fixture fixture = materialFixture(
                TrackerSelection.GOLD,
                Map.of(TrackedMaterial.GOLD, 1_000L),
                Map.of(TrackedMaterial.GOLD, 20L));

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.materialBlocks(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        TrackedMaterial.GOLD,
                        2L,
                        22L,
                        1_100L));

        assertMatch(result, 2L, 2L);
        assertEquals(MiningSessionParity.Domain.MATERIAL_BLOCKS,
                result.domain());
    }

    @Test
    void liveCounterBelowBaselineIsIncomparable() {
        Fixture fixture = gemstoneFixture();

        MiningSessionParity.ComparisonResult result = fixture.parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        fixture.context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        0L,
                        99L,
                        1_100L));

        assertEquals(MiningSessionParity.Status.INCOMPARABLE, result.status());
        assertFalse(result.compared());
        assertEquals(0L, fixture.parity.snapshot().mismatchCount());
    }

    @Test
    void baselineCopiesMutableMapsAndExposesImmutableViews() {
        EnumMap<TrackedMaterial, Long> normalized =
                new EnumMap<>(TrackedMaterial.class);
        normalized.put(TrackedMaterial.GOLD, 10L);
        MiningSessionParity.LiveBaseline baseline =
                MiningSessionParity.LiveBaseline.material(
                        TrackerSelection.GOLD,
                        normalized,
                        Map.of(),
                        1_000L);

        normalized.put(TrackedMaterial.GOLD, 99L);

        assertEquals(10L,
                baseline.materialNormalizedQuantities()
                        .get(TrackedMaterial.GOLD));
        assertThrows(UnsupportedOperationException.class,
                () -> baseline.materialNormalizedQuantities()
                        .put(TrackedMaterial.DIAMOND, 1L));
    }

    @Test
    void inactiveDiagnosticsDoNotEmitParityMarkers() {
        CaptureSink sink = new CaptureSink(false);
        MiningSessionParity parity = new MiningSessionParity(sink);
        MiningSessionParity.Context context = rubyContext();
        parity.beginContext(context, rubyBaseline());

        MiningSessionParity.ComparisonResult result = parity.compare(
                MiningSessionParity.Sample.gemstoneTier(
                        context,
                        MiningSessionCategory.TARGET_MINED,
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH,
                        80L,
                        180L,
                        1_100L));

        assertEquals(MiningSessionParity.Status.MATCH, result.status());
        assertTrue(sink.markers.isEmpty());
    }

    private static Fixture gemstoneFixture() {
        CaptureSink sink = new CaptureSink(true);
        MiningSessionParity parity = new MiningSessionParity(sink);
        MiningSessionParity.Context context = rubyContext();
        parity.beginContext(context, rubyBaseline());
        return new Fixture(parity, context, sink);
    }

    private static MiningSessionParity.Context rubyContext() {
        return new MiningSessionParity.Context(
                1L,
                1L,
                3L,
                TrackerSelection.RUBY,
                1_000L);
    }

    private static MiningSessionParity.LiveBaseline rubyBaseline() {
        return MiningSessionParity.LiveBaseline.gemstone(
                TrackerSelection.RUBY,
                Map.of(
                        GemstoneTier.ROUGH, 100L,
                        GemstoneTier.FLAWED, 2L),
                260L,
                10L,
                1_000L);
    }

    private static Fixture materialFixture(
            TrackerSelection selection,
            Map<TrackedMaterial, Long> normalized,
            Map<TrackedMaterial, Long> blocks) {
        CaptureSink sink = new CaptureSink(true);
        MiningSessionParity parity = new MiningSessionParity(sink);
        MiningSessionParity.Context context = new MiningSessionParity.Context(
                1L,
                1L,
                3L,
                selection,
                1_000L);
        parity.beginContext(
                context,
                MiningSessionParity.LiveBaseline.material(
                        selection,
                        normalized,
                        blocks,
                        1_000L));
        return new Fixture(parity, context, sink);
    }

    private static void assertMatch(
            MiningSessionParity.ComparisonResult result,
            long expectedLive,
            long expectedShadow) {
        assertEquals(MiningSessionParity.Status.MATCH, result.status());
        assertTrue(result.compared());
        assertEquals(expectedLive, result.liveQuantity());
        assertEquals(expectedShadow, result.shadowQuantity());
        assertEquals(0L, result.mismatchCount());
        assertNull(result.mismatchDetail());
    }

    private record Fixture(
            MiningSessionParity parity,
            MiningSessionParity.Context context,
            CaptureSink sink) {
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        private final boolean active;
        private final List<Marker> markers = new ArrayList<>();

        private CaptureSink(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void record(String marker, String details) {
            markers.add(new Marker(marker, details));
        }

        private boolean contains(String marker) {
            return markers.stream()
                    .anyMatch(value -> value.name().equals(marker));
        }
    }

    private record Marker(String name, String details) {
    }
}
