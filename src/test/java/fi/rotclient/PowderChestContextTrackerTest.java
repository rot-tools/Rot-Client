package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowderChestContextTrackerTest {
    private static final String SEPARATOR =
            String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                    .repeat(48);

    @Test
    void fullStartRewardsEndFlowFinalizesBufferedItems() {
        Fixture fixture = fixture();
        long start = 1_000L;

        observe(fixture, "CHEST LOCKPICKED", start);
        observe(fixture, "    Rough Ruby Gemstone x2", start + 100L);
        observe(fixture, "    Gemstone Powder x50", start + 200L);
        var finalized = observe(fixture, SEPARATOR, start + 300L);

        assertTrue(finalized.isPresent());
        assertEquals(1, finalized.get().items().size());
        assertEquals(1, finalized.get().currencies().size());
        assertFalse(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_STARTED"));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_FINALIZED"));
        assertTrue(fixture.sink.contains("CHEST_REWARD_BUFFERED"));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CANDIDATE"));
    }

    @Test
    void noLedgerOutputBeforeEnd() {
        Fixture fixture = fixtureWithLedger();
        observe(fixture, "CHEST LOCKPICKED", 1_000L);
        assertChestLootCount(fixture, 0);

        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L);
        assertChestLootCount(fixture, 0);

        assertTrue(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_REWARD_BUFFERED"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_FINALIZED"));
    }

    @Test
    void inactivityTimeoutDiscardsBufferedCandidates() {
        Fixture fixture = fixtureWithLedger();
        observe(fixture, "CHEST LOCKPICKED", 1_000L);
        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L);

        long boundary = 1_100L
                + PowderChestContextTracker.INACTIVITY_TIMEOUT_MILLIS;
        fixture.tracker.expireIfNeeded(boundary);
        assertTrue(fixture.tracker.hasOpenContext());
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_INACTIVE"));

        fixture.tracker.expireIfNeeded(boundary + 1L);

        assertFalse(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_INACTIVE"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_FINALIZED"));
        assertChestLootCount(fixture, 0);
    }

    @Test
    void absoluteTimeoutExpiresDespiteRecognizedLines() {
        Fixture fixture = fixtureWithLedger();
        long openedAt = 1_000L;
        observe(fixture, "CHEST LOCKPICKED", openedAt);

        observe(fixture, "    Rough Ruby Gemstone x2", openedAt + 14_000L);
        long lastRecognized = openedAt
                + PowderChestContextTracker.ABSOLUTE_LIFETIME_MILLIS
                - 1_000L;
        observe(fixture, "    Gold Essence x1", lastRecognized);

        long boundary = openedAt
                + PowderChestContextTracker.ABSOLUTE_LIFETIME_MILLIS;
        fixture.tracker.expireIfNeeded(boundary);
        assertTrue(fixture.tracker.hasOpenContext());

        fixture.tracker.expireIfNeeded(boundary + 1L);

        assertFalse(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_ABSOLUTE"));
        assertChestLootCount(fixture, 0);
    }

    @Test
    void unrelatedChatDoesNotRefreshInactivityTimeout() {
        Fixture fixture = fixtureWithLedger();
        observe(fixture, "CHEST LOCKPICKED", 1_000L);
        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L);

        observe(fixture, "You found a secret!", 1_100L + 10_000L);
        observe(fixture, "Another unrelated line", 1_100L + 14_000L);

        long expireAt = 1_100L
                + PowderChestContextTracker.INACTIVITY_TIMEOUT_MILLIS
                + 1L;
        fixture.tracker.expireIfNeeded(expireAt);

        assertFalse(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_INACTIVE"));
    }

    @Test
    void nestedStartDiscardsPriorContextWithoutLedgerOutput() {
        Fixture fixture = fixtureWithLedger();
        observe(fixture, "CHEST LOCKPICKED", 1_000L);
        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L);
        observe(fixture, "CHEST LOCKPICKED", 1_200L);

        assertTrue(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_REPLACED"));
        assertChestLootCount(fixture, 0);
    }

    @Test
    void selectionEpochChangeDiscardsPriorContextWithoutLedgerOutput() {
        Fixture fixture = fixtureWithLedger();
        observe(fixture, "CHEST LOCKPICKED", 1_000L, 1L, 1L);
        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L, 1L, 1L);

        fixture.tracker.synchronizeEpochs(1L, 2L, 1_150L);

        assertFalse(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_INTERRUPTED"));
        assertChestLootCount(fixture, 0);
    }

    @Test
    void diagnosticAbandonDiscardsPriorContextWithoutLedgerOutput() {
        Fixture fixture = fixtureWithLedger();
        observe(fixture, "CHEST LOCKPICKED", 1_000L);
        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L);

        fixture.tracker.abandon("CHEST_CONTEXT_INTERRUPTED", 1_200L);

        assertFalse(fixture.tracker.hasOpenContext());
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_INTERRUPTED"));
        assertChestLootCount(fixture, 0);
    }

    @Test
    void uniqueContextIdsWithinSession() {
        Fixture fixture = fixture();
        observe(fixture, "CHEST LOCKPICKED", 1_000L);
        observe(fixture, SEPARATOR, 1_100L);
        observe(fixture, "CHEST LOCKPICKED", 1_200L);
        observe(fixture, SEPARATOR, 1_300L);

        List<String> contextIds = fixture.sink.detailsContaining(
                "CHEST_CONTEXT_STARTED",
                "contextId=");
        assertEquals(2, contextIds.size());
        assertFalse(contextIds.get(0).equals(contextIds.get(1)));
    }

    @Test
    void eventIdsIncludeSessionEpoch() {
        Fixture fixture = fixture();
        observe(fixture, "CHEST LOCKPICKED", 1_000L, 7L, 1L);
        observe(fixture, "    Rough Ruby Gemstone x2", 1_100L, 7L, 1L);
        var finalized = observe(fixture, SEPARATOR, 1_200L, 7L, 1L);

        assertTrue(finalized.isPresent());
        assertEquals(7L, finalized.get().sessionEpoch());
        assertEquals(
                "chest:7:1:line:0",
                MiningSessionChestObserver.eventId(
                        finalized.get().sessionEpoch(),
                        finalized.get().contextSequence(),
                        0));
    }

    private static void assertChestLootCount(Fixture fixture, int expected) {
        assertEquals(
                expected,
                fixture.ledger.totalItemQuantity(
                        MiningSessionCategory.CHEST_LOOT));
    }

    private static java.util.Optional<
            PowderChestContextTracker.FinalizedChestContext> observe(
            Fixture fixture,
            String line,
            long timestamp) {
        return observe(fixture, line, timestamp, 1L, 1L);
    }

    private static java.util.Optional<
            PowderChestContextTracker.FinalizedChestContext> observe(
            Fixture fixture,
            String line,
            long timestamp,
            long sessionEpoch,
            long selectionEpoch) {
        var finalized = fixture.tracker.observeLine(
                PowderChestChatParser.parse(line),
                sessionEpoch,
                selectionEpoch,
                timestamp);
        finalized.ifPresent(context -> fixture.observer.finalizeContext(
                context,
                timestamp));
        return finalized;
    }

    private static Fixture fixture() {
        return fixtureWithLedger();
    }

    private static Fixture fixtureWithLedger() {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionChestObserver observer = new MiningSessionChestObserver(
                catalog,
                ledger,
                sink);
        observer.synchronizeEngineState(
                true,
                TrackerSelection.RUBY,
                1L);
        return new Fixture(
                sink,
                new PowderChestContextTracker(sink),
                ledger,
                observer);
    }

    private record Fixture(
            CaptureSink sink,
            PowderChestContextTracker tracker,
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

        private List<String> detailsContaining(String marker, String text) {
            return markers.stream()
                    .filter(value -> value.name().equals(marker))
                    .map(Marker::details)
                    .filter(details -> details.contains(text))
                    .map(details -> {
                        int start = details.indexOf("contextId=");
                        int end = details.indexOf(' ', start);
                        return details.substring(
                                start,
                                end < 0 ? details.length() : end);
                    })
                    .toList();
        }
    }

    private record Marker(String name, String details) {
    }
}
