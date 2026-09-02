package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionEngineTest {
    @Test
    void freshEngineIsInactiveAndHasNoSnapshot() {
        Fixture fixture = fixture();

        assertTrue(fixture.engine.snapshot(1L).isEmpty());
        assertFalse(fixture.engine.isObservationEnabled());
        assertTrue(fixture.engine.lastAcceptedEventTimestamp().isEmpty());
    }

    @Test
    void diagnosticStartCreatesFreshSession() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.GOLD, true, 10L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 11L);
        assertEquals(1L, snapshot.sessionId());
        assertEquals(1L, snapshot.sessionEpoch());
        assertEquals(1L, snapshot.selectionEpoch());
        assertEquals(10L, snapshot.sessionStartMillis());
        assertTrue(snapshot.diagnosticsActive());
        assertTrue(snapshot.trackerEnabled());
        assertEquals(TrackerSelection.GOLD, snapshot.selectedTracker());
        assertTrue(fixture.sink.contains("MINING_SESSION_STARTED"));
    }

    @Test
    void secondDiagnosticStartClearsPriorState() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.GOLD, true, 10L);
        appendGold(fixture, 4L, 4L, 11L, "gold-1");

        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                goldBaseline(4L, 0L, 20L),
                20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 21L);
        assertEquals(0, snapshot.entryCount());
        assertEquals(2L, snapshot.sessionId());
        assertEquals(2L, snapshot.sessionEpoch());
        assertTrue(snapshot.lastAcceptedEventTimestamp().isEmpty());
    }

    @Test
    void targetAndOtherEntriesCoexistInOneSnapshot() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        appendRuby(fixture, 8L, 8L, 8L, 11L, "ruby-1");
        appendOtherTopaz(fixture, 20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 30L);
        assertEquals(1, snapshot.targetMinedEntryCount());
        assertEquals(1, snapshot.otherMinedEntryCount());
        assertEquals(2, snapshot.entryCount());
    }

    @Test
    void targetQuantityRemainsSeparateFromOtherQuantity() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        appendRuby(fixture, 8L, 8L, 8L, 11L, "ruby-1");
        appendOtherTopaz(fixture, 20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 30L);
        MiningSessionResource ruby = resource(
                fixture, GemstoneType.RUBY, GemstoneTier.ROUGH);
        MiningSessionResource topaz = resource(
                fixture, GemstoneType.TOPAZ, GemstoneTier.ROUGH);
        assertEquals(8L, snapshot.quantity(
                MiningSessionCategory.TARGET_MINED, ruby));
        assertEquals(0L, snapshot.quantity(
                MiningSessionCategory.OTHER_MINED, ruby));
        assertEquals(80L, snapshot.quantity(
                MiningSessionCategory.OTHER_MINED, topaz));
        assertEquals(0L, snapshot.quantity(
                MiningSessionCategory.TARGET_MINED, topaz));
    }

    @Test
    void chestLootRemainsEmptyWithoutPowderChestBlock() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 3L, 3L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(0, snapshot.entryCount(MiningSessionCategory.CHEST_LOOT));
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void powderChestBlockRoutesEndToEnd() {
        Fixture fixture = startedGoldFixture();
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 30L);
        assertEquals(1, snapshot.entryCount(MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_REWARD_CREDITED"));
        assertEquals(
                MiningSessionParity.Status.NOT_CHECKED,
                snapshot.targetParityStatus());
    }

    @Test
    void oreSelectionChangeDoesNotDiscardStandalonePowderChestContext() {
        Fixture fixture = startedGoldFixture();
        String separator = String.valueOf(
                PowderChestChatParser.SEPARATOR_CHAR).repeat(48);
        fixture.engine.observePowderChestChatLine("CHEST LOCKPICKED", 20L);
        fixture.engine.observePowderChestChatLine(
                "    Rough Ruby Gemstone x2", 21L);

        fixture.engine.onSelectionChanged(
                TrackerSelection.DIAMOND,
                true,
                baseline(TrackerSelection.DIAMOND, 22L),
                22L);
        MiningSessionChestObserver.FinalizationResult result =
                fixture.engine.observePowderChestChatLine(separator, 23L);

        assertTrue(result.accepted());
        assertEquals(1, result.canonicalCredits().size());
        assertEquals(1, snapshot(fixture, 24L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void disablingPowderChestTrackerAbandonsPartialContext() {
        Fixture fixture = startedGoldFixture();
        String separator = String.valueOf(
                PowderChestChatParser.SEPARATOR_CHAR).repeat(48);
        fixture.engine.observePowderChestChatLine("CHEST LOCKPICKED", 20L);
        fixture.engine.observePowderChestChatLine(
                "    Rough Ruby Gemstone x2", 21L);

        fixture.engine.onPowderChestTrackerDisabled(22L);
        MiningSessionChestObserver.FinalizationResult result =
                fixture.engine.observePowderChestChatLine(separator, 23L);

        assertFalse(result.accepted());
        assertEquals(0, snapshot(fixture, 24L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void reenablingPowderChestTrackerCreditsTheNextDistinctChest() {
        Fixture fixture = startedGoldFixture();
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                20L);
        assertEquals(1, snapshot(fixture, 25L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));

        fixture.engine.onPowderChestTrackerDisabled(30L);

        MiningSessionChestObserver.FinalizationResult result =
                observePowderChestBlock(
                        fixture,
                        List.of("Gold Essence x3"),
                        40L);

        assertTrue(result.accepted());
        assertEquals(1, result.canonicalCredits().size());
        assertEquals(
                "GOLD_ESSENCE",
                result.canonicalCredits().getFirst().itemId());
        assertEquals(2, snapshot(fixture, 50L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
        assertFalse(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "BATCH_REJECTED_CONFLICT"));
    }

    @Test
    void partialPowderChestBlockInvalidatedByLifecycleCreatesZeroEntries() {
        Fixture fixture = startedGoldFixture();
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                20L);
        fixture.engine.observePowderChestChatLine(
                "    Rough Ruby Gemstone x2",
                21L);
        fixture.engine.onDiagnosticStop(22L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 23L);
        assertEquals(0, snapshot.entryCount(MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_INTERRUPTED"));
    }

    @Test
    void pristineAndSackPathsStillBehaveAsBefore() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 11L);
        fixture.engine.observeSackChanges(List.of(topazSack()), 12L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 13L);
        assertEquals(1, snapshot.otherMinedEntryCount());
        assertEquals(0, snapshot.chestLootEntryCount());
    }

    @Test
    void diagnosticTickExpiresOpenChestWithoutFurtherChat() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                1_000L);
        fixture.engine.observePowderChestChatLine(
                "    Rough Ruby Gemstone x2",
                1_100L);

        long expireAt = 1_100L
                + PowderChestContextTracker.INACTIVITY_TIMEOUT_MILLIS
                + 1L;
        fixture.engine.onDiagnosticTick(expireAt);

        assertEquals(0, snapshot(fixture, expireAt)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_INACTIVE"));
    }

    @Test
    void diagnosticsActiveTrackerDisabledStillCreditsChestForCurrentSession() {
        // Current Session keeps diagnosticsActive without requiring the live
        // tracker; OTHER/chest collection must still credit in that mode.
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, false, 10L);
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                20L);

        assertEquals(1, snapshot(fixture, 30L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_FINALIZED"));
        assertFalse(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "TRACKER_DISABLED"));
    }

    @Test
    void diagnosticsInactiveIgnoresPowderChestBlock() {
        Fixture fixture = fixture();
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                20L);

        assertFalse(fixture.engine.isDiagnosticsActive());
        assertEquals(0, fixture.engine.snapshot(30L)
                .map(snapshot -> snapshot.entryCount(
                        MiningSessionCategory.CHEST_LOOT))
                .orElse(0));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_STARTED"));
    }

    @Test
    void duplicatePowderChestBlockReplayWithinWindowCreditsOnce() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                20L);
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                21L);

        assertEquals(1, snapshot(fixture, 22L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_DUPLICATE"));
    }

    @Test
    void worldChangeClearsChestSackResearchCorrelation() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        observePowderChestBlock(
                fixture,
                List.of("Rough Ruby Gemstone x2"),
                20L);
        fixture.engine.onWorldChanged(
                TrackerSelection.RUBY,
                true,
                baseline(TrackerSelection.RUBY, 25L),
                25L);
        fixture.engine.observeSackChanges(
                List.of(new SackChangeParser.Change(
                        2L,
                        "Rough Ruby Gemstone",
                        List.of("Gemstone Sack"))),
                26L);

        assertFalse(fixture.sink.contains("CHEST_SACK_RESEARCH_ONLY"));
    }

    @Test
    void bufferedRewardsProduceZeroLedgerEntriesBeforeEndMarker() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                20L);
        assertEquals(0, snapshot(fixture, 21L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));

        fixture.engine.observePowderChestChatLine(
                "    Rough Ruby Gemstone x2",
                21L);
        assertEquals(0, snapshot(fixture, 22L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void resetDiscardsBufferedChestRewardsWithoutLedgerOutput() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                20L);
        fixture.engine.observePowderChestChatLine(
                "    Rough Ruby Gemstone x2",
                21L);
        fixture.engine.onReset(
                true,
                TrackerSelection.RUBY,
                baseline(TrackerSelection.RUBY, 22L),
                22L);

        assertEquals(0, snapshot(fixture, 23L)
                .entryCount(MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_INTERRUPTED"));
    }

    @Test
    void currencyRemainsEmptyWithoutChestCurrency() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 3L, 3L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(0, snapshot.currencyEntryCount());
        assertEquals(0, snapshot.entryCount(MiningSessionCategory.CURRENCY));
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.CURRENCY));
    }

    @Test
    void powderChestBlockCreditsCurrencyAtomically() {
        Fixture fixture = startedRubyFixture();
        observePowderChestBlock(
                fixture,
                List.of(
                        "Rough Ruby Gemstone x48",
                        "Gemstone Powder x296"),
                20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 30L);
        MiningSessionResource ruby = resource(
                fixture, GemstoneType.RUBY, GemstoneTier.ROUGH);
        MiningSessionResource powder = MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");

        assertEquals(2, snapshot.entryCount());
        assertEquals(1, snapshot.chestLootEntryCount());
        assertEquals(1, snapshot.currencyEntryCount());
        assertEquals(48L, snapshot.quantity(
                MiningSessionCategory.CHEST_LOOT,
                ruby));
        assertEquals(296L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
        assertTrue(fixture.sink.contains("CHEST_REWARD_CREDITED"));
        assertEquals(
                MiningSessionParity.Status.NOT_CHECKED,
                snapshot.targetParityStatus());
    }

    @Test
    void sackAfterPowderChestDoesNotChangeCurrency() {
        Fixture fixture = startedRubyFixture();
        observePowderChestBlock(
                fixture,
                List.of(
                        "Rough Ruby Gemstone x48",
                        "Gemstone Powder x296"),
                20L);
        MiningSessionResource powder = MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");

        fixture.engine.observeSackChanges(
                List.of(new SackChangeParser.Change(
                        296L,
                        "Gemstone Powder",
                        List.of("Gemstone Sack"))),
                21L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 22L);
        assertEquals(296L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
        assertTrue(fixture.sink.contains("CHEST_SACK_RESEARCH_ONLY"));
    }

    @Test
    void everyEntryPriceRemainsUnresolved() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        appendRuby(fixture, 8L, 8L, 8L, 11L, "ruby-1");
        appendOtherTopaz(fixture, 20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 30L);
        assertEquals(snapshot.entryCount(), snapshot.unresolvedPriceEntryCount());
        assertTrue(snapshot.entries().stream().allMatch(entry ->
                entry.priceResolution().status()
                        == MiningSessionPriceResolution.PriceStatus.UNRESOLVED));
    }

    @Test
    void trackerOffRejectsNewTargetEvents() {
        Fixture fixture = startedGoldFixture();
        fixture.engine.onTrackerDisabled(
                TrackerSelection.GOLD,
                goldBaseline(0L, 0L, 11L),
                11L);

        MiningSessionEngine.EventDisposition result = appendGold(
                fixture, 3L, 3L, 12L, "gold-1");

        assertEquals(MiningSessionEngine.EventDisposition.INACTIVE, result);
        assertEquals(0, snapshot(fixture, 13L).entryCount());
    }

    @Test
    void trackerOnCannotReuseOldOtherCorrelation() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 11L);
        assertEquals(1, fixture.engine.pendingOtherContextCount());
        fixture.engine.onTrackerDisabled(
                TrackerSelection.RUBY,
                baseline(TrackerSelection.RUBY, 12L),
                12L);
        fixture.engine.onTrackerEnabled(
                TrackerSelection.RUBY,
                baseline(TrackerSelection.RUBY, 13L),
                13L);

        fixture.engine.observeSackChanges(
                List.of(topazSack()),
                14L);

        assertEquals(0, snapshot(fixture, 15L).entryCount());
        assertEquals(0, fixture.engine.pendingOtherContextCount());
    }

    @Test
    void targetTransitionPreservesAcceptedEntries() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "gold-1");

        fixture.engine.onSelectionChanged(
                TrackerSelection.DIAMOND,
                true,
                baseline(TrackerSelection.DIAMOND, 12L),
                12L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 13L);
        assertEquals(1, snapshot.entryCount());
        assertEquals(TrackerSelection.DIAMOND, snapshot.selectedTracker());
        assertEquals(2L, snapshot.selectionEpoch());
        assertEquals(12L, snapshot.selectionStartMillis());
    }

    @Test
    void targetTransitionClearsPendingCorrelation() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 11L);

        fixture.engine.onSelectionChanged(
                TrackerSelection.TOPAZ,
                true,
                baseline(TrackerSelection.TOPAZ, 12L),
                12L);

        assertEquals(0, fixture.engine.pendingOtherContextCount());
    }

    @Test
    void worldTransitionPreservesAcceptedEntries() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "gold-1");

        fixture.engine.onWorldChanged(
                TrackerSelection.GOLD,
                true,
                goldBaseline(5L, 0L, 12L),
                12L);

        assertEquals(1, snapshot(fixture, 13L).entryCount());
        assertEquals(2L, snapshot(fixture, 13L).selectionEpoch());
    }

    @Test
    void worldTransitionClearsPendingCorrelation() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        fixture.engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 11L);

        fixture.engine.onWorldChanged(
                TrackerSelection.RUBY,
                true,
                baseline(TrackerSelection.RUBY, 12L),
                12L);

        assertEquals(0, fixture.engine.pendingOtherContextCount());
    }

    @Test
    void resetClearsUnifiedLedger() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "gold-1");

        fixture.engine.onReset(
                true,
                TrackerSelection.GOLD,
                baseline(TrackerSelection.GOLD, 12L),
                12L);

        assertEquals(0, snapshot(fixture, 13L).entryCount());
    }

    @Test
    void resetChangesSessionId() {
        Fixture fixture = startedGoldFixture();
        long before = snapshot(fixture, 11L).sessionId();

        fixture.engine.onReset(
                true,
                TrackerSelection.GOLD,
                baseline(TrackerSelection.GOLD, 12L),
                12L);

        assertNotEquals(before, snapshot(fixture, 13L).sessionId());
    }

    @Test
    void resetChangesSessionEpoch() {
        Fixture fixture = startedGoldFixture();
        long before = snapshot(fixture, 11L).sessionEpoch();

        fixture.engine.onReset(
                true,
                TrackerSelection.GOLD,
                baseline(TrackerSelection.GOLD, 12L),
                12L);

        assertEquals(before + 1L, snapshot(fixture, 13L).sessionEpoch());
    }

    @Test
    void resetRetainsDiagnosticsActiveState() {
        Fixture fixture = startedGoldFixture();

        fixture.engine.onReset(
                false,
                TrackerSelection.GOLD,
                baseline(TrackerSelection.GOLD, 12L),
                12L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 13L);
        assertTrue(snapshot.diagnosticsActive());
        assertFalse(snapshot.trackerEnabled());
        assertTrue(fixture.sink.contains("MINING_SESSION_RESET"));
    }

    @Test
    void diagnosticStopProducesFinalSnapshot() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "gold-1");

        MiningSessionSnapshot stopped = fixture.engine.onDiagnosticStop(12L)
                .orElseThrow();

        assertFalse(stopped.diagnosticsActive());
        assertEquals(1, stopped.entryCount());
        assertEquals(12L, stopped.snapshotAtMillis());
        assertTrue(fixture.sink.contains("MINING_SESSION_SNAPSHOT"));
        assertTrue(fixture.sink.contains("MINING_SESSION_STOPPED"));
    }

    @Test
    void retainedFinalSnapshotRemainsQueryable() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "gold-1");
        MiningSessionSnapshot stopped = fixture.engine.onDiagnosticStop(12L)
                .orElseThrow();

        MiningSessionSnapshot retained = snapshot(fixture, 99L);

        assertEquals(stopped.sessionId(), retained.sessionId());
        assertEquals(12L, retained.snapshotAtMillis());
        assertEquals(stopped.entries(), retained.entries());
    }

    @Test
    void diagnosticStopRejectsLaterEvents() {
        Fixture fixture = startedGoldFixture();
        fixture.engine.onDiagnosticStop(11L);

        MiningSessionEngine.EventDisposition result = appendGold(
                fixture, 5L, 5L, 12L, "gold-1");

        assertEquals(MiningSessionEngine.EventDisposition.INACTIVE, result);
        assertEquals(0, snapshot(fixture, 13L).entryCount());
    }

    @Test
    void exactEntryIdentityIsIdempotent() {
        Fixture fixture = startedGoldFixture();
        assertEquals(
                MiningSessionEngine.EventDisposition.APPENDED,
                appendGold(fixture, 5L, 5L, 11L, "same-event"));

        MiningSessionEngine.EventDisposition duplicate = appendGold(
                fixture, 5L, 5L, 11L, "same-event");

        assertEquals(MiningSessionEngine.EventDisposition.DUPLICATE, duplicate);
        assertEquals(1, snapshot(fixture, 12L).entryCount());
        assertEquals(5L, snapshot(fixture, 12L).totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void identityConflictIsVisibleAndAtomic() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "same-event");

        MiningSessionEngine.EventDisposition conflict = appendGold(
                fixture, 6L, 11L, 12L, "same-event");

        assertEquals(
                MiningSessionEngine.EventDisposition.IDENTITY_CONFLICT,
                conflict);
        MiningSessionSnapshot snapshot = snapshot(fixture, 13L);
        assertEquals(1, snapshot.entryCount());
        assertEquals(5L, snapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void sessionIdOverflowIsAtomic() {
        CaptureSink sink = new CaptureSink();
        MiningSessionEngine engine = new MiningSessionEngine(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                sink,
                Long.MAX_VALUE,
                0L,
                0L);

        assertThrows(ArithmeticException.class, () ->
                engine.onDiagnosticStart(
                        true,
                        TrackerSelection.GOLD,
                        baseline(TrackerSelection.GOLD, 10L),
                        10L));

        assertTrue(engine.snapshot(11L).isEmpty());
        assertFalse(sink.contains("MINING_SESSION_STARTED"));
    }

    @Test
    void epochOverflowIsAtomic() {
        CaptureSink sink = new CaptureSink();
        MiningSessionEngine engine = new MiningSessionEngine(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                sink,
                0L,
                Long.MAX_VALUE,
                0L);

        assertThrows(ArithmeticException.class, () ->
                engine.onDiagnosticStart(
                        true,
                        TrackerSelection.GOLD,
                        baseline(TrackerSelection.GOLD, 10L),
                        10L));

        assertTrue(engine.snapshot(11L).isEmpty());
        assertFalse(sink.contains("MINING_SESSION_STARTED"));
    }

    @Test
    void selectionEpochOverflowIsAtomic() {
        CaptureSink sink = new CaptureSink();
        MiningSessionEngine engine = new MiningSessionEngine(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                Long.MAX_VALUE);

        assertThrows(ArithmeticException.class, () ->
                engine.onDiagnosticStart(
                        true,
                        TrackerSelection.GOLD,
                        baseline(TrackerSelection.GOLD, 10L),
                        10L));

        assertTrue(engine.snapshot(11L).isEmpty());
    }

    @Test
    void explicitTimestampsProduceDeterministicBehavior() {
        Fixture first = startedGoldFixture();
        Fixture second = startedGoldFixture();
        appendGold(first, 7L, 7L, 11L, "gold-1");
        appendGold(second, 7L, 7L, 11L, "gold-1");

        MiningSessionSnapshot firstSnapshot = snapshot(first, 12L);
        MiningSessionSnapshot secondSnapshot = snapshot(second, 12L);

        assertEquals(firstSnapshot.entries(), secondSnapshot.entries());
        assertEquals(
                firstSnapshot.lastAcceptedEventTimestamp(),
                secondSnapshot.lastAcceptedEventTimestamp());
        assertEquals(
                firstSnapshot.targetParityStatus(),
                secondSnapshot.targetParityStatus());
    }

    @Test
    void snapshotCreationDoesNotMutateEngine() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 7L, 7L, 11L, "gold-1");

        MiningSessionSnapshot first = snapshot(fixture, 12L);
        MiningSessionSnapshot second = snapshot(fixture, 13L);

        assertEquals(first.entries(), second.entries());
        assertEquals(first.entryCounts(), second.entryCounts());
        assertEquals(first.quantities(), second.quantities());
        assertEquals(first.parityMismatchCount(), second.parityMismatchCount());
    }

    @Test
    void engineStateIsNotPersistentAcrossInstances() {
        Fixture first = startedGoldFixture();
        appendGold(first, 7L, 7L, 11L, "gold-1");

        Fixture second = fixture();

        assertEquals(1, snapshot(first, 12L).entryCount());
        assertTrue(second.engine.snapshot(12L).isEmpty());
    }

    @Test
    void engineHasNoHudDependency() {
        assertTrue(java.util.Arrays.stream(
                        MiningSessionEngine.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == RotClientHud.class));
    }

    @Test
    void targetMirroringDoesNotMutateLiveMaterialFixture() {
        Fixture fixture = fixture();
        MaterialTrackerState live = new MaterialTrackerState();
        live.sessionActualRawEquivalent = 9L;
        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                goldBaseline(9L, 0L, 10L),
                10L);

        appendGold(fixture, 3L, 12L, 11L, "gold-1");

        assertEquals(9L, live.sessionActualRawEquivalent);
        assertEquals(0L, live.sessionBlocks);
    }

    @Test
    void otherMinedSliceTwoBehaviorRemainsAvailable() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);

        appendOtherTopaz(fixture, 20L);

        MiningSessionSnapshot snapshot = snapshot(fixture, 30L);
        assertEquals(1, snapshot.otherMinedEntryCount());
        assertEquals(MiningSessionCategory.OTHER_MINED,
                snapshot.entries().getFirst().category());
        assertTrue(fixture.sink.contains("OTHER_MINING_OBSERVED"));
        assertTrue(fixture.sink.contains("OTHER_MINING_WOULD_CREDIT"));
    }

    @Test
    void targetBlockAffectsParityButCreatesNoQuantityEntry() {
        Fixture fixture = startedGoldFixture();

        assertTrue(fixture.engine.onAcceptedTargetMaterialBlock(
                TrackedMaterial.GOLD,
                2,
                2L,
                11L,
                "gold-block-1",
                "confirmed-break"));

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(0, snapshot.entryCount());
        assertEquals(MiningSessionParity.Status.MATCH,
                snapshot.targetParityStatus());
        assertEquals(11L,
                snapshot.lastAcceptedEventTimestamp().orElseThrow());
        assertEquals(0, fixture.engine.pendingOtherContextCount());
    }

    @Test
    void gemstoneTierAndRoughEquivalentParityMatch() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);

        appendRuby(fixture, 2L, 2L, 2L, 11L, "ruby-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(MiningSessionParity.Status.MATCH,
                snapshot.targetParityStatus());
        assertEquals(0L, snapshot.parityMismatchCount());
        assertTrue(fixture.sink.contains("MINING_SESSION_PARITY_OK"));
    }

    @Test
    void independentParityMatchDoesNotMaskTierMismatch() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);

        fixture.engine.onAcceptedTargetGemstoneQuantity(
                GemstoneType.RUBY,
                GemstoneTier.FLAWED,
                1L,
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                0L,
                80L,
                11L,
                "ruby-flawed-1",
                "PRISTINE");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(MiningSessionParity.Status.MISMATCH,
                snapshot.targetParityStatus());
        assertEquals(1L, snapshot.parityMismatchCount());
        assertTrue(fixture.sink.details(
                "MINING_SESSION_TARGET_MIRRORED")
                .contains("parityStatus=MISMATCH"));
    }

    @Test
    void parityMismatchDoesNotCorrectShadowQuantity() {
        Fixture fixture = startedGoldFixture();

        appendGold(fixture, 5L, 4L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(MiningSessionParity.Status.MISMATCH,
                snapshot.targetParityStatus());
        assertEquals(1L, snapshot.parityMismatchCount());
        assertEquals(5L, snapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void negativeTimestampIsRejected() {
        Fixture fixture = fixture();

        assertThrows(IllegalArgumentException.class, () ->
                fixture.engine.onDiagnosticStart(
                        true,
                        TrackerSelection.GOLD,
                        baseline(TrackerSelection.GOLD, 0L),
                        -1L));
    }

    @Test
    void diagnosticMarkersContainCanonicalBoundedFields() {
        Fixture fixture = startedGoldFixture();
        appendGold(fixture, 5L, 5L, 11L, "gold-1");
        fixture.engine.onDiagnosticStop(12L);

        String mirrored = fixture.sink.details(
                "MINING_SESSION_TARGET_MIRRORED");
        assertTrue(mirrored.contains("resourceId=GOLD_INGOT"));
        assertTrue(mirrored.contains("category=TARGET_MINED"));
        assertTrue(mirrored.contains("selectionEpoch="));
        assertTrue(mirrored.contains("parityStatus=MATCH"));
        assertFalse(fixture.sink.allDetails().contains("D:\\"));
    }

    private static Fixture startedGoldFixture() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.GOLD, true, 10L);
        return fixture;
    }

    private static Fixture startedRubyFixture() {
        Fixture fixture = fixture();
        start(fixture, TrackerSelection.RUBY, true, 10L);
        return fixture;
    }

    private static Fixture fixture() {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        return new Fixture(
                catalog,
                sink,
                new MiningSessionEngine(
                        catalog,
                        new MiningSessionLedger(),
                        sink,
                        0L,
                        0L,
                        0L));
    }

    private static void start(
            Fixture fixture,
            TrackerSelection selection,
            boolean enabled,
            long timestamp) {
        fixture.engine.onDiagnosticStart(
                enabled,
                selection,
                baseline(selection, timestamp),
                timestamp);
    }

    private static MiningSessionEngine.EventDisposition appendGold(
            Fixture fixture,
            long quantity,
            long liveCumulative,
            long timestamp,
            String eventId) {
        return fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                liveCumulative,
                timestamp,
                eventId,
                "inventory");
    }

    private static MiningSessionEngine.EventDisposition appendRuby(
            Fixture fixture,
            long quantity,
            long liveTierCumulative,
            long liveRoughEquivalent,
            long timestamp,
            String eventId) {
        return fixture.engine.onAcceptedTargetGemstoneQuantity(
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                quantity,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                liveTierCumulative,
                liveRoughEquivalent,
                timestamp,
                eventId,
                "Gemstone Sack");
    }

    private static void appendOtherTopaz(
            Fixture fixture,
            long breakTimestamp) {
        fixture.engine.onConfirmedGemstoneBreak(
                GemstoneType.TOPAZ,
                breakTimestamp);
        fixture.engine.observeSackChanges(
                List.of(topazSack()),
                breakTimestamp + 1L);
    }

    private static SackChangeParser.Change topazSack() {
        return new SackChangeParser.Change(
                80L,
                "Rough Topaz Gemstone",
                List.of("Gemstone Sack"));
    }

    private static MiningSessionSnapshot snapshot(
            Fixture fixture,
            long timestamp) {
        return fixture.engine.snapshot(timestamp).orElseThrow();
    }

    private static MiningSessionResource resource(
            Fixture fixture,
            GemstoneType gemstone,
            GemstoneTier tier) {
        return fixture.catalog.fromGemstone(gemstone, tier)
                .orElseThrow()
                .resource();
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long timestamp) {
        if (selection.isGemstone()) {
            Map<GemstoneTier, Long> quantities =
                    new EnumMap<>(GemstoneTier.class);
            for (GemstoneTier tier : GemstoneTier.values()) {
                quantities.put(tier, 0L);
            }
            return MiningSessionParity.LiveBaseline.gemstone(
                    selection,
                    quantities,
                    0L,
                    0L,
                    timestamp);
        }
        Map<TrackedMaterial, Long> quantities =
                new EnumMap<>(TrackedMaterial.class);
        Map<TrackedMaterial, Long> blocks =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            quantities.put(material, 0L);
            blocks.put(material, 0L);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection,
                quantities,
                blocks,
                timestamp);
    }

    private static MiningSessionChestObserver.FinalizationResult
            observePowderChestBlock(
            Fixture fixture,
            List<String> rewardLines,
            long startTimestamp) {
        String separator = String.valueOf(
                PowderChestChatParser.SEPARATOR_CHAR).repeat(48);
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                startTimestamp);
        long timestamp = startTimestamp + 1L;
        for (String rewardLine : rewardLines) {
            fixture.engine.observePowderChestChatLine(
                    "    " + rewardLine,
                    timestamp++);
        }
        return fixture.engine.observePowderChestChatLine(separator, timestamp);
    }

    private static MiningSessionParity.LiveBaseline goldBaseline(
            long normalized,
            long blocks,
            long timestamp) {
        return MiningSessionParity.LiveBaseline.material(
                TrackerSelection.GOLD,
                Map.of(TrackedMaterial.GOLD, normalized),
                Map.of(TrackedMaterial.GOLD, blocks),
                timestamp);
    }

    private record Fixture(
            MiningResourceCatalog catalog,
            CaptureSink sink,
            MiningSessionEngine engine) {
        Fixture {
            assertNotNull(catalog);
            assertNotNull(sink);
            assertNotNull(engine);
        }
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        private final List<Marker> markers = new ArrayList<>();

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
            markers.add(new Marker(marker, details));
        }

        private boolean contains(String marker) {
            return markers.stream().anyMatch(value ->
                    value.name().equals(marker));
        }

        private boolean contains(String marker, String text) {
            return markers.stream().anyMatch(value ->
                    value.name().equals(marker)
                            && value.details().contains(text));
        }

        private String details(String marker) {
            return markers.stream()
                    .filter(value -> value.name().equals(marker))
                    .map(Marker::details)
                    .findFirst()
                    .orElseThrow();
        }

        private String allDetails() {
            return markers.toString();
        }
    }

    private record Marker(String name, String details) {
    }
}
