package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Crash-containment regressions: external-boundary failures must not wipe
 * Current Session quantities or escape guarded client boundaries.
 */
final class ClientCrashContainmentRegressionTest {
    @BeforeEach
    void resetGuard() {
        ClientBoundaryGuard.resetFailureCountForTests();
    }

    @Test
    void boundaryFailureDoesNotClearCurrentSessionQuantities() {
        AtomicLong clock = new AtomicLong(2_000L);
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(TrackerSelection.GOLD, clock.get());
        session.creditItem(
                "MITHRIL_ORE",
                "Mithril",
                25L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                clock.get());
        assertEquals(25L, qty(session, "MITHRIL_ORE"));

        long before = ClientBoundaryGuard.failureCount();
        assertDoesNotThrow(() ->
                ClientBoundaryGuard.run("HUD_RENDER", () -> {
                    throw new IllegalStateException("corrupt hud state");
                }));
        assertEquals(before + 1L, ClientBoundaryGuard.failureCount());

        assertDoesNotThrow(() ->
                session.refreshFromEngineMetadata(
                        null,
                        TrackerSelection.GOLD,
                        MiningSessionPriceBook.unavailable(),
                        clock.addAndGet(50L)));
        assertEquals(25L, qty(session, "MITHRIL_ORE"));
        assertTrue(session.isActive());
    }

    @Test
    void flushAfterBoundaryNoiseKeepsLedger() {
        AtomicLong clock = new AtomicLong(3_000L);
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(TrackerSelection.DIAMOND, clock.get());
        session.creditItem(
                "DIAMOND",
                "Diamond",
                7L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                clock.get());

        ClientBoundaryGuard.run("TRACKER_AUTOSAVE", () -> {
            throw new RuntimeException("simulated save boundary");
        });
        assertDoesNotThrow(() ->
                session.flushIfDirty(clock.addAndGet(10_000L), true));
        assertEquals(7L, qty(session, "DIAMOND"));
    }

    @Test
    void historyStoreLoadMalformedFileFailsClosedWithoutWipingCallerState()
            throws Exception {
        java.nio.file.Path temp = java.nio.file.Files.createTempFile(
                "rot-history-", ".json");
        try {
            java.nio.file.Files.writeString(
                    temp, "{not-json", java.nio.charset.StandardCharsets.UTF_8);
            MiningSessionHistoryStore.LoadResult result =
                    MiningSessionHistoryStore.load(temp);
            assertTrue(!result.available());
            assertTrue(result.document().isEmpty());
        } finally {
            java.nio.file.Files.deleteIfExists(temp);
        }
    }

    private static long qty(RotClientCurrentSession session, String itemId) {
        return session.snapshotConfig().items.stream()
                .filter(item -> itemId.equals(item.itemId()))
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .findFirst()
                .orElse(0L);
    }
}
