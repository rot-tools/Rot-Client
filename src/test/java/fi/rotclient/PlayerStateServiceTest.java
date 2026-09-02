package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PlayerStateServiceTest {
    @Test
    void emptySnapshotIsUnknownNotInventedZero() {
        PlayerStateService.Snapshot empty = PlayerStateService.empty(1000L);
        assertEquals(-1, empty.magicFind());
        assertEquals(PlayerStateService.Confidence.UNKNOWN, empty.confidence());
        assertFalse(empty.hasAuthoritativeMagicFind());
    }

    @Test
    void publishBumpsRevisionWithoutMutatingPriorFieldsInPlace() {
        PlayerStateService service = new PlayerStateService();
        PlayerStateService.Snapshot first = service.publish(new PlayerStateService.Snapshot(
                42, 10, 100, 200, 5, 9,
                SkyBlockArea.DWARVEN_MINES,
                "MITHRIL_DRILL",
                0L,
                5_000L,
                PlayerStateService.Confidence.AUTHORITATIVE));
        assertEquals(1L, first.revision());
        assertTrue(first.hasAuthoritativeMagicFind());

        PlayerStateService.Snapshot second = service.publish(new PlayerStateService.Snapshot(
                50, -1, -1, -1, -1, -1,
                SkyBlockArea.GLACITE_TUNNELS,
                "",
                0L,
                6_000L,
                PlayerStateService.Confidence.OBSERVED));
        assertEquals(2L, second.revision());
        assertEquals(50, service.latest().magicFind());
        assertEquals(-1, service.latest().petLuck());
        assertTrue(service.latest().isStale(20_000L, 1_000L));
    }
}
