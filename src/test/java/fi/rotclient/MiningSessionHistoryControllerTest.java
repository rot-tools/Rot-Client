package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MiningSessionHistoryControllerTest {
    @TempDir
    Path tempDir;

    @Test
    void saveRejectsActiveAndMissingAndSupportsDuplicate() {
        Fixture fixture = fixture();
        assertSame(
                MiningSessionHistoryController.SaveResult.REJECTED_NO_SESSION,
                fixture.history.saveCurrentStoppedSession());

        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 3L, 11L, "gold-1");
        assertSame(
                MiningSessionHistoryController.SaveResult.REJECTED_ACTIVE,
                fixture.history.saveCurrentStoppedSession());

        fixture.analytics.stop();
        assertSame(
                MiningSessionHistoryController.SaveResult.SAVED,
                fixture.history.saveCurrentStoppedSession());
        assertSame(
                MiningSessionHistoryController.SaveResult.ALREADY_SAVED,
                fixture.history.saveCurrentStoppedSession());
        assertEquals(1, fixture.history.document().size());
        assertEquals(0, fixture.networkCalls.get());
    }

    @Test
    void currentSessionBackedControllerSavesOnlyCanonicalPausedFreeze() {
        Fixture fixture = fixture();
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "CANONICAL_MOB_DROP",
                9L,
                SessionSourceType.MOB,
                started + 1L);
        fixture.clock.set(started + 10L);
        MiningSessionHistoryController history =
                new MiningSessionHistoryController(
                        fixture.analytics,
                        fixture.engine,
                        fixture.clipboard::add,
                        fixture.clock,
                        tempDir.resolve("current-session-history.json"),
                        current);

        assertSame(
                MiningSessionHistoryController.SaveResult.REJECTED_ACTIVE,
                history.saveCurrentStoppedSession());
        current.pause(started + 20L);
        fixture.clock.set(started + 30L);
        assertTrue(history.hasUnsavedStoppedSnapshot());
        assertSame(
                MiningSessionHistoryController.SaveResult.SAVED,
                history.saveCurrentStoppedSession());
        assertFalse(history.hasUnsavedStoppedSnapshot());

        RotClientSessionFreeze frozen = history.document().sessions().get(0)
                .currentSessionFreeze().orElseThrow();
        assertEquals(9L, frozen.itemRows().get(0).quantity());
        assertEquals(SessionSourceType.MOB,
                frozen.itemRows().get(0).source());
        String id = history.document().sessions().get(0).recordId();
        assertSame(MiningSessionHistoryController.CopyResult.COPIED,
                history.copy(id));
        String copied = fixture.clipboard.get(fixture.clipboard.size() - 1);
        assertTrue(copied.contains("Active duration:"));
        assertTrue(copied.contains("Paused duration:"));
        assertTrue(copied.contains("Canonical item rows: 1"));
        assertTrue(copied.contains("CANONICAL_MOB_DROP x9"));
        assertEquals(0, fixture.engine.snapshot(started + 30L)
                .map(MiningSessionSnapshot::entryCount).orElse(0));
    }

    @Test
    void pausedCanonicalSnapshotDoesNotBecomeUnsavedFromClockDriftAlone() {
        Fixture fixture = fixture();
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "CANONICAL_DROP", 2L, SessionSourceType.MOB, started + 1L);
        current.pause(started + 20L);
        fixture.clock.set(started + 30L);
        MiningSessionHistoryController history =
                new MiningSessionHistoryController(
                        fixture.analytics,
                        fixture.engine,
                        fixture.clipboard::add,
                        fixture.clock,
                        tempDir.resolve("clock-stable-history.json"),
                        current);

        assertSame(MiningSessionHistoryController.SaveResult.SAVED,
                history.saveCurrentStoppedSession());
        fixture.clock.set(started + 30_000L);

        assertFalse(history.hasUnsavedStoppedSnapshot());
        assertSame(MiningSessionHistoryController.SaveResult.ALREADY_SAVED,
                history.saveCurrentStoppedSession());
        assertEquals(1, history.document().size());
    }

    @Test
    void largeCanonicalFreezeRemainsArchivableAndPresentationSafe() {
        Fixture fixture = fixture();
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        for (int index = 0; index < 2_049; index++) {
            current.creditUnknown(
                    "UNIQUE_" + index,
                    1L,
                    SessionSourceType.MOB,
                    started + index + 1L);
        }
        current.pause(started + 3_000L);
        fixture.clock.set(started + 4_000L);
        MiningSessionHistoryController history =
                new MiningSessionHistoryController(
                        fixture.analytics,
                        fixture.engine,
                        fixture.clipboard::add,
                        fixture.clock,
                        tempDir.resolve("oversized-history.json"),
                        current);

        assertDoesNotThrow(history::presentation);
        assertSame(MiningSessionHistoryController.SaveResult.SAVED,
                history.saveCurrentStoppedSession());
        assertEquals(2_049, current.snapshotConfig().items.size());
        assertEquals(1, history.document().size());
        assertEquals(2_049, history.document().sessions().getFirst()
                .currentSessionFreeze().orElseThrow().itemRows().size());
    }

    @Test
    void listOpenCopyDeleteClearAndIsolation() {
        Fixture fixture = fixture();
        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 4L, 11L, "gold-1");
        playChest(fixture, 20L);
        fixture.analytics.stop();
        assertSame(
                MiningSessionHistoryController.SaveResult.SAVED,
                fixture.history.saveCurrentStoppedSession());

        String list = fixture.history.listText();
        assertTrue(list.contains("Session History"));
        assertTrue(list.contains("Gold"));
        assertTrue(list.contains("id "));

        String id = fixture.history.document().sessions().get(0).recordId();
        assertSame(
                MiningSessionHistoryController.OpenResult.OPENED,
                fixture.history.open("1"));
        assertTrue(fixture.history.selectedRecord().isPresent());

        MiningSessionAnalyticsViewModel liveBefore =
                fixture.analytics.viewModel();
        assertSame(
                MiningSessionHistoryController.CopyResult.COPIED,
                fixture.history.copy(id));
        assertFalse(fixture.clipboard.isEmpty());
        assertTrue(fixture.clipboard.get(0)
                .contains("Saved Session History"));
        assertTrue(fixture.clipboard.get(0)
                .contains("stored historical session"));
        assertFalse(fixture.clipboard.get(0).toLowerCase().contains("chat"));
        assertEquals(
                liveBefore.resolvedItemValue(),
                fixture.analytics.viewModel().resolvedItemValue());

        assertSame(
                MiningSessionAnalyticsController.ResetResult.CLEARED,
                fixture.analytics.reset());
        assertEquals(1, fixture.history.document().size());

        MaterialTrackerState live = new MaterialTrackerState();
        live.sessionBlocks = 77;
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String token = fixture.history.pendingClearToken();
        assertSame(
                MiningSessionHistoryController.ClearResult.CLEARED,
                fixture.history.confirmClear(token));
        assertTrue(fixture.history.document().isEmpty());
        assertEquals(77, live.sessionBlocks);
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED,
                fixture.analytics.viewModel().sessionState());
        assertEquals(0, fixture.networkCalls.get());
    }

    @Test
    void clearConfirmationExpiresAndDeleteLeavesAnalyticsIntact() {
        Fixture fixture = fixture();
        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 2L, 11L, "gold-1");
        fixture.analytics.stop();
        fixture.history.saveCurrentStoppedSession();

        fixture.history.requestClear();
        fixture.clock.set(fixture.clock.nowMillis()
                + MiningSessionHistoryController.CLEAR_CONFIRM_TTL_MILLIS);
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear("hdeadbeefdeadbeef"));

        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 5L, 40L, "gold-2");
        String id = fixture.history.document().sessions().get(0).recordId();
        assertSame(
                MiningSessionHistoryController.DeleteResult.DELETED,
                fixture.history.delete(id));
        assertTrue(fixture.engine.isDiagnosticsActive());
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                fixture.analytics.viewModel().sessionState());
    }

    @Test
    void mismatchedClearTokenIsConsumedAndDoesNotClear() {
        Fixture fixture = fixture();
        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 2L, 11L, "gold-token");
        fixture.analytics.stop();
        fixture.history.saveCurrentStoppedSession();

        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String validToken = fixture.history.pendingClearToken();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear("hwrongtoken000001"));
        assertEquals(1, fixture.history.document().size());
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear(validToken));
        assertEquals(1, fixture.history.document().size());
    }

    @Test
    void clearConfirmationRemainsValidJustBeforeSharedTimeout() {
        Fixture fixture = savedHistoryFixture();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String token = fixture.history.pendingClearToken();
        long requestedAt = fixture.clock.nowMillis();
        fixture.clock.set(requestedAt
                + MiningSessionHistoryController.CLEAR_CONFIRM_TTL_MILLIS
                - 1L);
        assertSame(
                MiningSessionHistoryController.ClearResult.CLEARED,
                fixture.history.confirmClear(token));
        assertTrue(fixture.history.document().isEmpty());
    }

    @Test
    void clearConfirmationExpiresAtSharedTimeoutBoundary() {
        Fixture fixture = savedHistoryFixture();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String token = fixture.history.pendingClearToken();
        long requestedAt = fixture.clock.nowMillis();
        fixture.clock.set(requestedAt
                + MiningSessionHistoryController.CLEAR_CONFIRM_TTL_MILLIS);
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear(token));
        assertEquals(1, fixture.history.document().size());
    }

    @Test
    void clearConfirmationExpiresAfterSharedTimeout() {
        Fixture fixture = savedHistoryFixture();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String token = fixture.history.pendingClearToken();
        long requestedAt = fixture.clock.nowMillis();
        fixture.clock.set(requestedAt
                + MiningSessionHistoryController.CLEAR_CONFIRM_TTL_MILLIS
                + 1L);
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear(token));
        assertEquals(1, fixture.history.document().size());
    }

    @Test
    void newClearRequestInvalidatesPreviousTokenAndAcceptsCurrent() {
        Fixture fixture = savedHistoryFixture();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String oldToken = fixture.history.pendingClearToken();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String newToken = fixture.history.pendingClearToken();
        assertNotEquals(oldToken, newToken);
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear(oldToken));
        assertEquals(1, fixture.history.document().size());

        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_REQUIRED,
                fixture.history.requestClear());
        String currentToken = fixture.history.pendingClearToken();
        long requestedAt = fixture.clock.nowMillis();
        fixture.clock.set(requestedAt
                + MiningSessionHistoryController.CLEAR_CONFIRM_TTL_MILLIS
                - 1L);
        assertSame(
                MiningSessionHistoryController.ClearResult.CLEARED,
                fixture.history.confirmClear(currentToken));
        assertTrue(fixture.history.document().isEmpty());
    }

    @Test
    void confirmClearWithoutPendingTokenFailsClosed() {
        Fixture fixture = savedHistoryFixture();
        assertSame(
                MiningSessionHistoryController.ClearResult.CONFIRMATION_EXPIRED,
                fixture.history.confirmClear("hno pending token01"));
        assertEquals(1, fixture.history.document().size());
    }

    @Test
    void clearConfirmationPromptUsesSharedTimeoutPhraseWithoutThirtySeconds() {
        assertEquals(
                120_000L,
                MiningSessionHistoryController.CLEAR_CONFIRM_TTL_MILLIS);
        String phrase =
                MiningSessionHistoryController.clearConfirmValidityPhrase();
        assertEquals("within 2 minutes", phrase);
        assertFalse(phrase.contains("30"));
        String prompt =
                MiningSessionHistoryController.clearConfirmationPrompt(
                        "htokenexample00001");
        assertTrue(prompt.contains(phrase));
        assertFalse(prompt.contains("30s"));
        assertFalse(prompt.toLowerCase().contains("30 second"));
        assertEquals(
                "Confirm Session History clear within 2 minutes: "
                        + "/rot history clear confirm htokenexample00001",
                prompt);
    }

    private Fixture savedHistoryFixture() {
        Fixture fixture = fixture();
        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 2L, 11L, "gold-clear");
        fixture.analytics.stop();
        fixture.history.saveCurrentStoppedSession();
        return fixture;
    }

    @Test
    void presentationStatesForEmptySaveAndSelected() {
        Fixture fixture = fixture();
        MiningSessionHistoryPresentation empty =
                fixture.history.presentation();
        assertTrue(empty.available());
        assertTrue(empty.empty());
        assertFalse(empty.saveEnabled());
        assertEquals("No saved sessions yet.", empty.emptyStateMessage());

        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 3L, 11L, "gold-1");
        fixture.analytics.stop();
        assertTrue(fixture.history.presentation().saveEnabled());
        fixture.history.saveCurrentStoppedSession();
        assertFalse(fixture.history.presentation().saveEnabled());

        fixture.history.open("1");
        MiningSessionHistoryPresentation detail =
                fixture.history.presentation();
        assertTrue(detail.detailOpen());
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                detail.selectedViewModel().sessionState());
        BigDecimal historical = detail.selectedViewModel().resolvedItemValue();

        fixture.analytics.start(true, TrackerSelection.GOLD);
        assertNotEquals(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                fixture.analytics.viewModel().sessionState());
        assertEquals(
                0,
                historical.compareTo(
                        fixture.history.presentation()
                                .selectedViewModel()
                                .resolvedItemValue()));
    }

    private Fixture fixture() {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MiningSessionEngine engine = new MiningSessionEngine(
                catalog,
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L,
                fixedBook(
                        10L,
                        Map.of(
                                TrackedMaterial.GOLD.rawBazaarId(),
                                new BigDecimal("2.00"),
                                "ROUGH_TOPAZ_GEM",
                                new BigDecimal("2.00"),
                                "FLAWED_RUBY_GEM",
                                new BigDecimal("5.00"))));
        List<String> clipboard = new ArrayList<>();
        AtomicInteger network = new AtomicInteger();
        MutableClock clock = new MutableClock(10L);
        MiningSessionAnalyticsController analytics =
                new MiningSessionAnalyticsController(
                        engine,
                        clipboard::add,
                        (selection, at) -> baseline(selection, at),
                        clock);
        MiningSessionHistoryController history =
                new MiningSessionHistoryController(
                        analytics,
                        engine,
                        clipboard::add,
                        clock,
                        tempDir.resolve("rotclient-session-history.json"));
        return new Fixture(engine, analytics, history, clipboard, network, clock);
    }

    private static MiningSessionPriceProvider fixedBook(
            long observedAt,
            Map<String, BigDecimal> prices) {
        MiningSessionPriceBook book =
                MiningSessionPriceBook.available(observedAt, prices);
        return now -> book;
    }

    private static void appendGold(
            Fixture fixture,
            long quantity,
            long at,
            String eventId) {
        fixture.clock.set(at);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                quantity,
                at,
                eventId,
                "test");
    }

    private static void playChest(Fixture fixture, long timestamp) {
        fixture.clock.set(timestamp);
        fixture.engine.observePowderChestChatLine("CHEST LOCKPICKED", timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Rough Topaz Gemstone x24",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Flawed Ruby Gemstone x2",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gold Essence x2",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gemstone Powder x291",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                        .repeat(27),
                timestamp);
        fixture.clock.set(timestamp);
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long at) {
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
                    at);
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
                at);
    }

    private record Fixture(
            MiningSessionEngine engine,
            MiningSessionAnalyticsController analytics,
            MiningSessionHistoryController history,
            List<String> clipboard,
            AtomicInteger networkCalls,
            MutableClock clock) {
    }

    private static final class MutableClock
            implements MiningSessionAnalyticsController.Clock,
            MiningSessionHistoryController.Clock {
        private long now;

        private MutableClock(long now) {
            this.now = now;
        }

        void set(long now) {
            this.now = now;
        }

        @Override
        public long nowMillis() {
            return now;
        }
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
        }
    }
}
