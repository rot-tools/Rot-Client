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
 * Live Hypixel fixtures (Cases A–G) exercising the same logical runtime
 * entrypoint as {@code RotClientClient.submitParsedSackItemGain}:
 * parsed Sack change → {@link SackItemGainPipeline} → classifier →
 * Current Session → HUD OTHERS projection.
 */
final class SackRuntimeOthersIngestionTest {
    private final MiningResourceCatalog catalog = new MiningResourceCatalog();

    @AfterEach
    void tearDown() {
        TrackingRuntimeTrace.clearStatusMemory();
    }

    @Test
    void caseA_cobblestoneWithMiningEvidence_isOtherMined() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.COBBLESTONE, 1, 100L);
        SackItemGainPipeline.Outcome outcome = submit(
                fixture,
                sack("Cobblestone", 489L, "Mining Sack"),
                110L,
                24_000L);
        assertEquals("OTHER_MINED", outcome.decision().classification());
        assertEquals("ACCEPTED_OTHER_MINING", outcome.decision().result());
        assertTrue(outcome.creditedOthers());
        assertEquals(489L, sessionOtherQty(fixture, "COBBLESTONE"));
        assertEquals(489L, fixture.session.otherHudSummary().totalQuantity());
    }

    @Test
    void caseB_mithrilOreWithEvidence_acceptedOtherMining() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);
        SackItemGainPipeline.Outcome outcome = submit(
                fixture, sack("Mithril", 479L, "Mining Sack"), 110L, 24_000L);
        assertEquals("OTHER_MINED", outcome.decision().classification());
        assertEquals("ACCEPTED_OTHER_MINING", outcome.decision().result());
        assertTrue(outcome.creditedOthers());
        assertEquals(479L, sessionOtherQty(fixture, "MITHRIL"));
    }

    @Test
    void caseC_diamondWithEvidence_acceptedOtherMining() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        SackItemGainPipeline.Outcome outcome = submit(
                fixture, sack("Diamond", 2935L, "Mining Sack"), 110L, 24_000L);
        assertEquals("OTHER_MINED", outcome.decision().classification());
        assertTrue(outcome.creditedOthers());
        assertEquals(2935L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void caseD_enchantedGold_notOthers_noTargetDoubleCount() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 100L);
        SackItemGainPipeline.Outcome outcome = submit(
                fixture,
                sack("Enchanted Gold", 48L, "Mining Sack"),
                110L,
                24_000L);
        assertTrue(outcome.decision().targetFamilyProtected());
        assertFalse(outcome.decision().ingestOthers());
        assertFalse(outcome.creditedOthers());
        assertEquals(0L, sessionOtherQty(fixture, "ENCHANTED_GOLD"));
        assertEquals(0L, fixture.session.otherHudSummary().totalQuantity());
    }

    @Test
    void caseE_last24s_evidenceAt17sStillEligible() {
        Fixture fixture = goldSession();
        // Break at t=0; Sack arrives 17s later with Last 24s covered window.
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.COBBLESTONE, 1, 1_000L);
        SackItemGainPipeline.Outcome withoutBatch = submit(
                fixture,
                sack("Cobblestone", 10L, "Mining Sack"),
                1_000L + 17_000L,
                0L);
        // Default 10s material window alone cannot cover 17s.
        assertEquals("UNATTRIBUTED", withoutBatch.decision().classification());
        assertFalse(withoutBatch.creditedOthers());

        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.DIAMOND, 1, 2_000L);
        SackItemGainPipeline.Outcome withBatch = submit(
                fixture,
                sack("Diamond", 20L, "Mining Sack"),
                2_000L + 17_000L,
                24_000L);
        assertEquals("OTHER_MINED", withBatch.decision().classification());
        assertTrue(withBatch.creditedOthers());
        assertEquals(20L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void caseF_insufficientEvidence_unattributedNotSilentlyDropped() {
        Fixture fixture = goldSession();
        // No break evidence at all.
        SackItemGainPipeline.Outcome outcome = submit(
                fixture,
                sack("Cobblestone", 50L, "Mining Sack"),
                110L,
                24_000L);
        assertEquals("UNATTRIBUTED", outcome.decision().classification());
        assertEquals("UNATTRIBUTED", outcome.decision().result());
        assertEquals(
                TrackingRuntimeTrace.Reason.UNATTRIBUTED_NO_SOURCE_EVIDENCE
                        .name(),
                outcome.decision().reason());
        assertFalse(outcome.creditedOthers());
        assertEquals(0L, sessionOtherQty(fixture, "COBBLESTONE"));
    }

    @Test
    void caseG_debugOnOff_identicalDomainAccounting() {
        Fixture off = goldSession();
        Fixture on = goldSession();
        off.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);
        on.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);

        TrackingRuntimeTrace.clearStatusMemory();
        SackItemGainPipeline.Outcome a = submit(
                off, sack("Mithril", 99L, "Mining Sack"), 110L, 24_000L);

        TrackingRuntimeTrace.seedUniqueItemForTest(
                new TrackingRuntimeTrace.ObservedGain(
                        "SACK_ITEM_GAIN",
                        TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                        "Mithril",
                        "MITHRIL_ORE",
                        "",
                        "",
                        99L,
                        "MINING",
                        "UNKNOWN",
                        "OBSERVED",
                        TrackingRuntimeTrace.Reason.INFO.name(),
                        "",
                        "sack",
                        SkyBlockItemIdentityResolver.fromTextToken("Mithril")));
        SackItemGainPipeline.Outcome b = submit(
                on, sack("Mithril", 99L, "Mining Sack"), 110L, 24_000L);

        assertEquals(a.creditedOthers(), b.creditedOthers());
        assertEquals(a.decision().classification(), b.decision().classification());
        assertEquals(
                sessionOtherQty(off, "MITHRIL"),
                sessionOtherQty(on, "MITHRIL"));
        assertEquals(99L, sessionOtherQty(off, "MITHRIL"));
    }

    @Test
    void duplicateSignal_doesNotDoubleIngest() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        SackChangeParser.Change change = sack("Diamond", 10L, "Mining Sack");
        assertTrue(submit(fixture, change, 110L, 24_000L).creditedOthers());
        assertFalse(submit(fixture, change, 120L, 24_000L).creditedOthers());
        assertEquals(10L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void trackerAutoPauseIndependent_collectionStillIngests() {
        Fixture fixture = goldSession();
        fixture.collectionActive.set(true);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);
        assertTrue(submit(
                fixture, sack("Mithril", 7L, "Mining Sack"), 110L, 24_000L)
                .creditedOthers());
    }

    @Test
    void explicitCollectionPause_blocksIngest() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.collectionActive.set(false);
        SackItemGainPipeline.Outcome blocked = submit(
                fixture, sack("Diamond", 50L, "Mining Sack"), 110L, 24_000L);
        assertFalse(blocked.creditedOthers());
        assertEquals(
                TrackingRuntimeTrace.Reason.REJECTED_COLLECTION_INACTIVE.name(),
                blocked.decision().reason());
    }

    @Test
    void unknownStableId_withoutCatalog_isRetainedUnattributed() {
        Fixture fixture = goldSession();
        SackItemGainPipeline.Outcome outcome = submit(
                fixture, sack("Weird Stable Ore", 3L, "Mining Sack"), 110L, 24_000L);
        assertFalse(outcome.creditedOthers());
        assertEquals("UNATTRIBUTED", outcome.decision().classification());
        assertEquals(3L, sessionOtherQtyAllowingUnattributed(fixture, "WEIRD_STABLE_ORE"));
    }

    private static long sessionOtherQtyAllowingUnattributed(
            Fixture fixture, String itemId) {
        return fixture.session.snapshotConfig().items.stream()
                .filter(i -> itemId.equals(i.itemId()))
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    @Test
    void hardStoneBlockEvidenceAlone_doesNotInventQuantity() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        assertEquals(0L, sessionOtherQty(fixture, "HARD_STONE"));
        HardStoneQuantitySignals.Assessment assessment =
                HardStoneQuantitySignals.assess(true, false, false, false);
        assertEquals(HardStoneQuantitySignals.Signal.NONE, assessment.quantitySignal());
        assertFalse(assessment.fortuneSafe());
    }

    @Test
    void analyticsOtherMined_usesSameCanonicalSessionLedger() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        submit(fixture, sack("Diamond", 40L, "Mining Sack"), 110L, 24_000L);
        assertEquals(40L, sessionOtherQty(fixture, "DIAMOND"));
        long engineOther = fixture.engine.snapshot(1_000L)
                .map(s -> s.totalItemQuantity(MiningSessionCategory.OTHER_MINED))
                .orElse(0L);
        assertEquals(40L, engineOther);
        assertEquals(40L, fixture.session.otherHudSummary().totalQuantity());
    }

    @Test
    void sackTokenDiagnostics_distinguishParsedVsUnparsed() {
        SackChangeParser.ParseResult parsed =
                SackChangeParser.parseWithDiagnostics(
                        "Added items\n+10 Cobblestone (Mining Sack)\n??? junk");
        assertEquals(1, parsed.changes().size());
        assertTrue(parsed.tokenTraces().stream().anyMatch(SackChangeParser.TokenTrace::parsed));
        assertTrue(parsed.tokenTraces().stream()
                .anyMatch(t -> !t.parsed() && t.safeLabel().contains("junk")));
        // Hard Stone absent from message → no HARD_STONE token.
        assertFalse(parsed.tokenTraces().stream()
                .anyMatch(t -> t.safeLabel().toLowerCase().contains("hard stone")));
    }

    private SackItemGainPipeline.Outcome submit(
            Fixture fixture,
            SackChangeParser.Change change,
            long now,
            long coveredBatchMillis) {
        fixture.clock.set(now);
        return SackItemGainPipeline.submit(
                fixture.context(), change, coveredBatchMillis);
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
