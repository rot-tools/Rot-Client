package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientCrashRecoveryTest {
    @Test
    void cleanShutdownKeepsExactPausedBoundary() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        // startedAt must be >0 — normalize() rewrites 0 to "now".
        config.startedAtMillis = 1_000L;
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.pausedAtMillis = 1_000L + 30 * 60_000L;
        config.lastHeartbeatMillis = config.pausedAtMillis;
        config.cleanShutdown = true;
        config.offlineAutoPaused = true;

        RotClientCurrentSessionMath.recoverUncleanShutdown(config);
        assertEquals(1_000L + 30 * 60_000L, config.pausedAtMillis);
        assertEquals(
                30 * 60_000L,
                RotClientCurrentSessionMath.activeDurationMillis(
                        config, config.pausedAtMillis + 12 * 3_600_000L));
    }

    @Test
    void uncleanShutdownClosesAtLastHeartbeat() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.startedAtMillis = 1_000L;
        config.state = RotClientCurrentSessionConfig.STATE_ACTIVE;
        config.lastHeartbeatMillis = 1_000L + 30 * 60_000L;
        config.cleanShutdown = false;

        RotClientCurrentSessionMath.recoverUncleanShutdown(config);
        assertTrue(config.isPaused());
        assertTrue(config.offlineAutoPaused);
        assertTrue(config.cleanShutdown);
        assertEquals(1_000L + 30 * 60_000L, config.pausedAtMillis);

        long afterOffline = config.pausedAtMillis + 12 * 3_600_000L;
        assertEquals(
                30 * 60_000L,
                RotClientCurrentSessionMath.activeDurationMillis(
                        config, afterOffline));
    }

    @Test
    void twelveHourOfflineAfterCrashNotCountedActive() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.startedAtMillis = 1_000L;
        config.state = RotClientCurrentSessionConfig.STATE_ACTIVE;
        config.lastHeartbeatMillis = 1_000L + 30 * 60_000L;
        config.cleanShutdown = false;
        config.currentTargetId = "GOLD";
        config.targetSegments = List.of(
                new RotClientCurrentSessionConfig.TargetSegment(
                        "GOLD", 1_000L, 0L));
        config.items = List.of(
                new RotClientCurrentSessionConfig.SessionItemRecord(
                        "HARD_STONE",
                        "Hard Stone",
                        50L,
                        SessionSourceType.MINING.name(),
                        MiningClassification.OTHER.name(),
                        SkyBlockArea.DWARVEN_MINES.id(),
                        true,
                        RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE.name(),
                        0.0));
        config.displayNumber = 3;
        String sessionId = config.sessionId;

        RotClientCurrentSessionMath.recoverUncleanShutdown(config);

        assertEquals(sessionId, config.sessionId);
        assertEquals(3, config.displayNumber);
        assertEquals("GOLD", config.currentTargetId);
        assertEquals(1, config.items.size());
        assertEquals(50L, config.items.get(0).quantity());
        assertEquals(1, config.targetSegments.size());

        long now = config.pausedAtMillis + 12 * 3_600_000L;
        assertEquals(
                30 * 60_000L,
                RotClientCurrentSessionMath.activeDurationMillis(config, now));
    }

    @Test
    void controllerHeartbeatMarksUncleanUntilGracefulPause() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        session.heartbeat(started + 100L);
        assertFalse(session.snapshotConfig().cleanShutdown);

        session.heartbeat(
                started + RotClientCurrentSessionMath.HEARTBEAT_INTERVAL_MILLIS
                        + 1L);
        assertEquals(
                started + RotClientCurrentSessionMath.HEARTBEAT_INTERVAL_MILLIS
                        + 1L,
                session.snapshotConfig().lastHeartbeatMillis);

        session.pauseForOffline(started + 60_000L);
        assertTrue(session.snapshotConfig().cleanShutdown);
        assertTrue(session.snapshotConfig().offlineAutoPaused);
    }

    @Test
    void resumeAfterCrashRecoveryStartsNewActiveInterval() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        long started = session.snapshotConfig().startedAtMillis;
        // 10 min active, then offline pause (crash recovery equivalent).
        session.pauseForOffline(started + 10 * 60_000L);
        long resumeAt = started + 10 * 60_000L + 12 * 3_600_000L;
        assertTrue(session.resumeAfterOffline(resumeAt));
        assertTrue(session.isActive());
        assertEquals(10 * 60_000L, session.activeDurationMillis(resumeAt));
        assertEquals(
                15 * 60_000L,
                session.activeDurationMillis(resumeAt + 5 * 60_000L));
    }

    @Test
    void heartbeatIntervalDocumentsMaxCrashUncertainty() {
        assertEquals(5_000L, RotClientCurrentSessionMath.HEARTBEAT_INTERVAL_MILLIS);
    }
}
