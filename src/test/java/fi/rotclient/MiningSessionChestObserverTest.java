package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionChestObserverTest {
    @Test
    void detailedResultExposesCanonicalChestAndCurrencyCredits() {
        Fixture fixture = startedFixture();

        MiningSessionChestObserver.FinalizationResult result =
                fixture.observer.finalizeContextDetailed(
                        contextWithItemsAndCurrencies(
                                List.of(item(
                                        0, "Rough Ruby Gemstone", 2L)),
                                List.of(currency(
                                        1, "Gemstone Powder", 296L))),
                        1_100L);

        assertTrue(result.accepted());
        assertEquals(2, result.shadowAppliedCount());
        assertEquals(2, result.canonicalCredits().size());
        assertEquals(SessionSourceType.CHEST,
                result.canonicalCredits().get(0).sourceType());
        assertEquals(SessionSourceType.CURRENCY,
                result.canonicalCredits().get(1).sourceType());
    }

    @Test
    void finalizedUnknownOnlyChestStillCountsWithoutInventingRewardRows() {
        Fixture fixture = startedFixture();

        MiningSessionChestObserver.FinalizationResult result =
                fixture.observer.finalizeContextDetailed(
                        contextWithItem("Mystery Relic", 1L),
                        1_100L);

        assertTrue(result.accepted());
        assertEquals(0, result.shadowAppliedCount());
        assertTrue(result.canonicalCredits().isEmpty());
        assertTrue(fixture.sink.contains("CHEST_REWARD_UNKNOWN"));
    }

    @Test
    void twoKnownRewardsCommitTogether() {
        Fixture fixture = startedFixture();
        int credited = fixture.observer.finalizeContext(
                contextWithItems(
                        item(0, "Rough Ruby Gemstone", 2L),
                        item(1, "Gold Essence", 5L)),
                1_500L);

        assertEquals(2, credited);
        assertEquals(2, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_REWARD_CREDITED"));
    }

    @Test
    void knownAndUnknownCommitsKnownAndLogsUnknown() {
        Fixture fixture = startedFixture();
        int credited = fixture.observer.finalizeContext(
                contextWithItems(
                        item(0, "Rough Ruby Gemstone", 2L),
                        item(1, "Mystery Relic", 1L)),
                1_000L);

        assertEquals(1, credited);
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains("CHEST_REWARD_UNKNOWN"));
        assertTrue(fixture.sink.contains("CHEST_REWARD_CREDITED"));
    }

    @Test
    void knownAndCurrencyCommitsBothAtomically() {
        Fixture fixture = startedFixture();
        int credited = fixture.observer.finalizeContext(
                contextWithItemsAndCurrencies(
                        List.of(item(0, "Rough Ruby Gemstone", 2L)),
                        List.of(currency(1, "Gemstone Powder", 50L))),
                1_100L);

        assertEquals(2, credited);
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CANDIDATE"));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
        assertTrue(fixture.sink.contains("CHEST_REWARD_CREDITED"));
    }

    @Test
    void currencyOnlyChestCommitsAfterEndSeparator() {
        Fixture fixture = startedFixture();
        int credited = fixture.observer.finalizeContext(
                contextWithCurrencies(currency(0, "Gemstone Powder", 296L)),
                1_100L);

        assertEquals(1, credited);
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
        assertEquals(
                296L,
                fixture.ledger.quantity(
                        MiningSessionCategory.CURRENCY,
                        gemstonePowder()));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
    }

    @Test
    void knownItemUnknownItemAndKnownCurrencyCommitKnownEntries() {
        Fixture fixture = startedFixture();
        int credited = fixture.observer.finalizeContext(
                contextWithItemsAndCurrencies(
                        List.of(
                                item(0, "Rough Ruby Gemstone", 2L),
                                item(1, "Mystery Relic", 1L)),
                        List.of(currency(2, "Gemstone Powder", 50L))),
                1_100L);

        assertEquals(2, credited);
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
        assertTrue(fixture.sink.contains("CHEST_REWARD_UNKNOWN"));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
    }

    @Test
    void unknownCurrencyDoesNotAppend() {
        Fixture fixture = startedFixture();
        int credited = fixture.observer.finalizeContext(
                contextWithCurrencies(currency(0, "Yoggie Powder", 10L)),
                1_100L);

        assertEquals(0, credited);
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_UNKNOWN"));
        assertFalse(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
    }

    @Test
    void oneRejectedKnownCandidateCommitsNone() {
        Fixture fixture = fixture();
        fixture.observer.synchronizeEngineState(
                false,
                TrackerSelection.RUBY,
                1L);
        int credited = fixture.observer.finalizeContext(
                contextWithItems(
                        item(0, "Rough Ruby Gemstone", 2L),
                        item(1, "Gold Essence", 5L)),
                1_000L);

        assertEquals(0, credited);
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "TRACKER_DISABLED"));
        assertTrue(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "BATCH_REJECTED_INVALID"));
    }

    @Test
    void trackerDisabledRejectsMixedKnownBatch() {
        Fixture fixture = fixture();
        fixture.observer.synchronizeEngineState(
                false,
                TrackerSelection.RUBY,
                1L);
        int credited = fixture.observer.finalizeContext(
                contextWithItemsAndCurrencies(
                        List.of(item(0, "Rough Ruby Gemstone", 2L)),
                        List.of(currency(1, "Gemstone Powder", 50L))),
                1_000L);

        assertEquals(0, credited);
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
        assertTrue(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "BATCH_REJECTED_INVALID"));
        assertFalse(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
        assertFalse(fixture.sink.contains("CHEST_REWARD_CREDITED"));
    }

    @Test
    void conflictingEventIdentityCommitsNone() {
        Fixture fixture = startedFixture();
        fixture.observer.finalizeContext(
                contextWithItem("Rough Ruby Gemstone", 2L),
                1_000L);

        int credited = fixture.observer.finalizeContext(
                new PowderChestContextTracker.FinalizedChestContext(
                        "chest-1-1",
                        1L,
                        1L,
                        1L,
                        1_000L,
                        1_100L,
                        List.of(item(0, "Rough Ruby Gemstone", 9L)),
                        List.of()),
                1_100L);

        assertEquals(0, credited);
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "BATCH_REJECTED_CONFLICT"));
    }

    @Test
    void duplicateIdentitiesInsideBatchCommitNone() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        CaptureSink sink = new CaptureSink();
        MiningSessionChestObserver observer = new MiningSessionChestObserver(
                new MiningResourceCatalog(),
                ledger,
                sink);
        observer.synchronizeEngineState(true, TrackerSelection.RUBY, 1L);

        MiningSessionLedger.BatchAppendResult result = ledger.appendAllAtomically(List.of(
                ledgerBatchEntry("chest:1:1:line:0", 2L),
                ledgerBatchEntry("chest:1:1:line:0", 2L)));

        assertEquals(
                MiningSessionLedger.BatchAppendOutcome.REJECTED_DUPLICATE_IN_BATCH,
                result.outcome());
        assertEquals(0, ledger.entryCount());
    }

    @Test
    void trackerDisabledRejectsKnownCredit() {
        Fixture fixture = fixture();
        fixture.observer.synchronizeEngineState(
                false,
                TrackerSelection.RUBY,
                1L);
        int credited = fixture.observer.finalizeContext(
                contextWithItem("Rough Ruby Gemstone", 2L),
                1_000L);

        assertEquals(0, credited);
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertTrue(fixture.sink.contains(
                "CHEST_REWARD_REJECTED",
                "TRACKER_DISABLED"));
    }

    @Test
    void duplicateReplayWithinWindowCreditsOnce() {
        Fixture fixture = startedFixture();
        PowderChestContextTracker.FinalizedChestContext context =
                contextWithItemsAndCurrencies(
                        List.of(item(0, "Rough Ruby Gemstone", 2L)),
                        List.of(currency(1, "Gemstone Powder", 50L)));

        assertEquals(2, fixture.observer.finalizeContext(context, 1_000L));
        assertEquals(0, fixture.observer.finalizeContext(context, 1_200L));
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_DUPLICATE"));
    }

    @Test
    void duplicateReplayOutsideWindowCreditsAgain() {
        Fixture fixture = startedFixture();
        PowderChestContextTracker.FinalizedChestContext first =
                contextWithItemsAndCurrencies(
                        List.of(item(0, "Rough Ruby Gemstone", 2L)),
                        List.of(currency(1, "Gemstone Powder", 50L)));
        PowderChestContextTracker.FinalizedChestContext second =
                new PowderChestContextTracker.FinalizedChestContext(
                        "chest-1-2",
                        2L,
                        1L,
                        1L,
                        2_000L,
                        2_100L,
                        List.of(item(0, "Rough Ruby Gemstone", 2L)),
                        List.of(currency(1, "Gemstone Powder", 50L)));

        assertEquals(2, fixture.observer.finalizeContext(first, 1_000L));
        assertEquals(2, fixture.observer.finalizeContext(
                second,
                1_000L + MiningSessionChestObserver.REPLAY_WINDOW_MILLIS + 1L));
        assertEquals(2, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(2, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
    }

    @Test
    void resetClearsSackResearchCorrelation() {
        Fixture fixture = startedFixture();
        finalizeRuby(fixture, 2L, 1_000L);
        fixture.observer.resetState();

        fixture.observer.noteSackChangesForResearch(
                List.of(new SackChangeParser.Change(
                        2L,
                        "Rough Ruby Gemstone",
                        List.of("Gemstone Sack"))),
                1_500L);

        assertFalse(fixture.sink.contains("CHEST_SACK_RESEARCH_ONLY"));
    }

    @Test
    void sackChangeNearFinalizedChestEmitsResearchOnly() {
        Fixture fixture = startedFixture();
        finalizeRuby(fixture, 2L, 1_000L);

        fixture.observer.noteSackChangesForResearch(
                List.of(new SackChangeParser.Change(
                        2L,
                        "Rough Ruby Gemstone",
                        List.of("Gemstone Sack"))),
                1_500L);

        assertTrue(fixture.sink.contains("CHEST_SACK_RESEARCH_ONLY"));
        assertEquals(1, categoryEntries(fixture, MiningSessionCategory.CHEST_LOOT));
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.CURRENCY));
    }

    @Test
    void chestLootNeverAppearsUnderMinedCategories() {
        Fixture fixture = startedFixture();
        finalizeRuby(fixture, 2L, 1_000L);

        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.TARGET_MINED));
        assertEquals(0, categoryEntries(fixture, MiningSessionCategory.OTHER_MINED));
    }

    private static int finalizeRuby(
            Fixture fixture,
            long quantity,
            long timestamp) {
        return fixture.observer.finalizeContext(
                contextWithItem("Rough Ruby Gemstone", quantity),
                timestamp);
    }

    private static PowderChestContextTracker.FinalizedChestContext
            contextWithItem(String displayName, long quantity) {
        return contextWithItems(item(0, displayName, quantity));
    }

    private static PowderChestContextTracker.FinalizedChestContext
            contextWithItems(
            PowderChestContextTracker.BufferedItemReward... items) {
        return contextWithItemsAndCurrencies(List.of(items), List.of());
    }

    private static PowderChestContextTracker.FinalizedChestContext
            contextWithCurrencies(
            PowderChestContextTracker.BufferedCurrencyReward... currencies) {
        return contextWithItemsAndCurrencies(List.of(), List.of(currencies));
    }

    private static PowderChestContextTracker.FinalizedChestContext
            contextWithItemsAndCurrencies(
            List<PowderChestContextTracker.BufferedItemReward> items,
            List<PowderChestContextTracker.BufferedCurrencyReward> currencies) {
        return new PowderChestContextTracker.FinalizedChestContext(
                "chest-1-1",
                1L,
                1L,
                1L,
                1_000L,
                1_100L,
                items,
                currencies);
    }

    private static PowderChestContextTracker.BufferedItemReward item(
            int lineIndex,
            String displayName,
            long quantity) {
        return new PowderChestContextTracker.BufferedItemReward(
                lineIndex,
                "    " + displayName + " x" + quantity,
                displayName,
                quantity);
    }

    private static PowderChestContextTracker.BufferedCurrencyReward currency(
            int lineIndex,
            String displayName,
            long quantity) {
        return new PowderChestContextTracker.BufferedCurrencyReward(
                lineIndex,
                "    " + displayName + " x" + quantity,
                displayName,
                quantity);
    }

    private static MiningSessionResource gemstonePowder() {
        return MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");
    }

    private static int categoryEntries(
            Fixture fixture,
            MiningSessionCategory category) {
        return (int) fixture.ledger.entries().stream()
                .filter(entry -> entry.category() == category)
                .count();
    }

    private static MiningSessionLedger.BatchEntry ledgerBatchEntry(
            String eventId,
            long quantity) {
        MiningSessionResource ruby = MiningSessionResource.genericItem(
                "ROUGH_RUBY_GEM",
                "Rough Ruby Gemstone");
        return new MiningSessionLedger.BatchEntry(
                MiningSessionClassification.wouldCredit(
                        new MiningSessionObservation(
                                ruby,
                                quantity,
                                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                                TrackerSelection.RUBY,
                                1L,
                                1_000L,
                                eventId,
                                "chest-1-1",
                                "powder-chest-chat",
                                null),
                        MiningSessionCategory.CHEST_LOOT),
                MiningSessionPriceResolution.unresolved());
    }

    private static Fixture startedFixture() {
        Fixture fixture = fixture();
        fixture.observer.synchronizeEngineState(
                true,
                TrackerSelection.RUBY,
                1L);
        return fixture;
    }

    private static Fixture fixture() {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MiningSessionLedger ledger = new MiningSessionLedger();
        return new Fixture(
                catalog,
                sink,
                ledger,
                new MiningSessionChestObserver(catalog, ledger, sink));
    }

    private record Fixture(
            MiningResourceCatalog catalog,
            CaptureSink sink,
            MiningSessionLedger ledger,
            MiningSessionChestObserver observer) {
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
    }

    private record Marker(String name, String details) {
    }
}
