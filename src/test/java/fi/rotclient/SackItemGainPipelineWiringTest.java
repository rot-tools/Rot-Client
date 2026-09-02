package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Runtime-wiring regression for the live Sack → OTHERS pipeline using the
 * real {@link SackItemGainPipeline} service chain (same path RotClientClient
 * delegates to after packet parse).
 */
final class SackItemGainPipelineWiringTest {
    private final MiningResourceCatalog catalog = new MiningResourceCatalog();

    @AfterEach
    void tearDown() {
        TrackingRuntimeTrace.clearStatusMemory();
    }

    @Test
    void goldTarget_liveObservedOthers_goThroughRealPipeline() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.COBBLESTONE, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 200L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 300L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 400L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 500L);

        assertOther(fixture, "Cobblestone", 115L, "COBBLESTONE", 110L);
        assertOther(fixture, "Diamond", 765L, "DIAMOND", 210L);
        assertOther(fixture, "Mithril", 1099L, "MITHRIL", 310L);
        assertOther(fixture, "Titanium", 42L, "TITANIUM", 410L);

        SackItemGainPipeline.Outcome gold = submit(
                fixture, sack("Gold Ingot", 1346L, "Mining Sack"), 510L);
        assertTrue(gold.decision().targetFamilyProtected());
        assertFalse(gold.decision().ingestOthers());
        assertFalse(gold.creditedOthers());
        assertEquals(0L, sessionOtherQty(fixture, "GOLD_INGOT"));
    }

    @Test
    void multiItemBatchAndRapidRedelivery_dedupes() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.COBBLESTONE, 1, 100L);

        List<SackChangeParser.Change> batch = List.of(
                sack("Diamond", 10L, "Mining Sack"),
                sack("Cobblestone", 20L, "Mining Sack"));
        for (SackChangeParser.Change change : batch) {
            SackItemGainPipeline.Outcome first = submit(fixture, change, 110L);
            assertTrue(first.creditedOthers(), change.itemName());
            SackItemGainPipeline.Outcome dup = submit(fixture, change, 120L);
            assertFalse(dup.creditedOthers(), "duplicate " + change.itemName());
        }
        assertEquals(10L, sessionOtherQty(fixture, "DIAMOND"));
        assertEquals(20L, sessionOtherQty(fixture, "COBBLESTONE"));
    }

    @Test
    void separateEqualComponentOccurrencesUseSeparateBreakContexts() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 101L);
        SackChangeParser.Change change = sack("Diamond", 10L, "Mining Sack");

        fixture.clock.set(110L);
        SackItemGainPipeline.Outcome first = SackItemGainPipeline.submit(
                fixture.context(), change, 10_000L, "component:1");
        fixture.clock.set(120L);
        SackItemGainPipeline.Outcome second = SackItemGainPipeline.submit(
                fixture.context(), change, 10_000L, "component:2");
        fixture.clock.set(121L);
        SackItemGainPipeline.Outcome repeatedSecond = SackItemGainPipeline.submit(
                fixture.context(), change, 10_000L, "component:2");

        assertTrue(first.creditedOthers());
        assertTrue(second.creditedOthers());
        assertFalse(repeatedSecond.creditedOthers());
        assertEquals(20L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void collectionInactiveBlocksIngest_resumeAllowsAgain() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.collectionActive.set(false);

        SackItemGainPipeline.Outcome blocked = submit(
                fixture, sack("Diamond", 50L, "Mining Sack"), 110L);
        assertFalse(blocked.creditedOthers());
        assertEquals(0L, sessionOtherQty(fixture, "DIAMOND"));

        fixture.collectionActive.set(true);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 200L);
        SackItemGainPipeline.Outcome allowed = submit(
                fixture, sack("Diamond", 50L, "Mining Sack"), 210L);
        assertTrue(allowed.creditedOthers());
        assertEquals(50L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void targetSwitchAfterOthers_doesNotRewritePriorCredits() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.COBBLESTONE, 1, 100L);
        submit(fixture, sack("Cobblestone", 40L, "Mining Sack"), 110L);
        assertEquals(40L, sessionOtherQty(fixture, "COBBLESTONE"));

        fixture.selection.set(TrackerSelection.DIAMOND);
        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.DIAMOND,
                MiningSessionParity.LiveBaseline.material(
                        TrackerSelection.DIAMOND,
                        Map.of(),
                        Map.of(),
                        200L),
                200L);
        fixture.session.ensureActiveForTracker(TrackerSelection.DIAMOND, 200L);

        assertEquals(40L, sessionOtherQty(fixture, "COBBLESTONE"));
        MiningHudOtherSummary summary = fixture.session.otherHudSummary();
        assertEquals(40L, summary.totalQuantity());
    }

    @Test
    void diagnosticsOnVsOff_identicalDomainResults() {
        Fixture off = goldSession();
        Fixture on = goldSession();
        off.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);
        on.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);

        TrackingRuntimeTrace.clearStatusMemory();
        SackItemGainPipeline.Outcome a = submit(
                off, sack("Mithril", 99L, "Mining Sack"), 110L);

        // Even with status memory seeded, pipeline domain outcome matches.
        TrackingRuntimeTrace.seedUniqueItemForTest(
                new TrackingRuntimeTrace.ObservedGain(
                        "SACK_ITEM_GAIN",
                        TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                        "Mithril",
                        "Mithril",
                        "",
                        "",
                        99L,
                        "MINING",
                        "OTHER",
                        "ACCEPTED",
                        TrackingRuntimeTrace.Reason.ACCEPTED_OTHER_MINING.name(),
                        "",
                        "sack",
                        SkyBlockItemIdentityResolver.fromTextToken("Mithril")));
        SackItemGainPipeline.Outcome b = submit(
                on, sack("Mithril", 99L, "Mining Sack"), 110L);

        assertEquals(a.creditedOthers(), b.creditedOthers());
        assertEquals(a.decision().ingestOthers(), b.decision().ingestOthers());
        assertEquals(
                sessionOtherQty(off, "MITHRIL"),
                sessionOtherQty(on, "MITHRIL"));
    }

    @Test
    void unresolvedPriceStillPreservesQuantity_andUnknownStableIdRetained() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.HARD_STONE, 1, 100L);
        SackItemGainPipeline.Outcome hardStone = submit(
                fixture, sack("Hard Stone", 128L, "Mining Sack"), 110L);
        assertTrue(hardStone.creditedOthers());
        assertEquals(128L, sessionOtherQty(fixture, "HARD_STONE"));
        RotClientCurrentSessionConfig.SessionItemRecord row =
                fixture.session.snapshotConfig().items.stream()
                        .filter(i -> "HARD_STONE".equals(i.itemId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                row.price());
        assertEquals(128L, row.quantity());
        assertEquals(0.0, row.resolvedGrossValue());

        SkyBlockItemIdentityResolver.Resolved unknown =
                SkyBlockItemIdentityResolver.fromTextToken("Weird Stable Ore");
        assertFalse(unknown.catalogMatch());
        assertEquals("WEIRD_STABLE_ORE", unknown.stableId());
    }

    @Test
    void trackerAutoPauseIndependent_collectionStillIngestsOthers() {
        Fixture fixture = goldSession();
        // collectionActive stays true while live tracker would be paused.
        fixture.collectionActive.set(true);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 100L);
        SackItemGainPipeline.Outcome outcome = submit(
                fixture, sack("Titanium", 7L, "Dwarven Sack"), 110L);
        assertTrue(outcome.creditedOthers());
        assertEquals(7L, sessionOtherQty(fixture, "TITANIUM"));
    }

    private void assertOther(
            Fixture fixture,
            String itemName,
            long qty,
            String expectedId,
            long now) {
        SackItemGainPipeline.Outcome outcome = submit(
                fixture, sack(itemName, qty, "Mining Sack"), now);
        assertEquals("OTHER_MINED", outcome.decision().classification(), itemName);
        assertTrue(outcome.creditedOthers(), itemName);
        assertEquals(qty, sessionOtherQty(fixture, expectedId), itemName);
    }

    private SackItemGainPipeline.Outcome submit(
            Fixture fixture,
            SackChangeParser.Change change,
            long now) {
        fixture.clock.set(now);
        return SackItemGainPipeline.submit(fixture.context(), change);
    }

    private static long sessionOtherQty(Fixture fixture, String itemId) {
        if (fixture.session.snapshotConfig().items == null) {
            return 0L;
        }
        return fixture.session.snapshotConfig().items.stream()
                .filter(i -> itemId.equals(i.itemId()))
                .filter(i -> i.source() == SessionSourceType.MINING)
                .filter(i -> i.miningClassification() == MiningClassification.OTHER)
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private Fixture goldSession() {
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
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(TrackerSelection.GOLD, 10L);
        return new Fixture(engine, session, catalog);
    }

    private static SackChangeParser.Change sack(
            String itemName,
            long delta,
            String sackName) {
        return new SackChangeParser.Change(
                delta, itemName, List.of(sackName));
    }

    private static final class Fixture {
        private final MiningSessionEngine engine;
        private final RotClientCurrentSession session;
        private final MiningResourceCatalog catalog;
        private final AtomicBoolean collectionActive = new AtomicBoolean(true);
        private final AtomicLong clock = new AtomicLong(10L);
        private final AtomicReference<TrackerSelection> selection =
                new AtomicReference<>(TrackerSelection.GOLD);

        private Fixture(
                MiningSessionEngine engine,
                RotClientCurrentSession session,
                MiningResourceCatalog catalog) {
            this.engine = engine;
            this.session = session;
            this.catalog = catalog;
        }

        private SackItemGainPipeline.Context context() {
            return new SackItemGainPipeline.Context(
                    engine,
                    session,
                    catalog,
                    selection::get,
                    () -> SkyBlockArea.DWARVEN_MINES,
                    collectionActive::get,
                    clock::get);
        }
    }
}
