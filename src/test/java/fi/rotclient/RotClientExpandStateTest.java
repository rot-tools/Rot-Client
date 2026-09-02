package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientExpandStateTest {
    @Test
    void firstSyncSnapsSoOpenDashboardDoesNotReplay() {
        RotClientExpandState state = new RotClientExpandState();
        state.syncTargets(List.of("qol"), List.of("mining", "qol"));
        assertEquals(0.0D, state.amount("mining"), 1.0E-9);
        assertEquals(1.0D, state.amount("qol"), 1.0E-9);
    }

    @Test
    void advanceMovesTowardNewlyOpenedSection() {
        RotClientExpandState state = new RotClientExpandState();
        state.syncTargets(List.of(), List.of("qol"));
        state.syncTargets(List.of("qol"), List.of("qol"));
        state.advance(1_000_000L);
        state.advance(1_000_000L + 16_000_000L);
        assertTrue(state.amount("qol") > 0.0D);
        assertTrue(state.amount("qol") < 1.0D);
        assertTrue(state.visuallyOpen("qol"));
    }

    @Test
    void snapJumpsToTargets() {
        RotClientExpandState state = new RotClientExpandState();
        state.syncTargets(List.of(), List.of("qol"));
        state.syncTargets(List.of("qol"), List.of("qol"));
        state.snap();
        assertEquals(1.0D, state.amount("qol"), 1.0E-9);
        assertFalse(state.visuallyOpen("missing"));
    }

    @Test
    void highHzAdvanceMovesLessThan60Hz() {
        RotClientExpandState sixty = new RotClientExpandState();
        sixty.syncTargets(List.of(), List.of("qol"));
        sixty.syncTargets(List.of("qol"), List.of("qol"));
        sixty.advanceSeconds(1.0D / 60.0D);

        RotClientExpandState high = new RotClientExpandState();
        high.syncTargets(List.of(), List.of("qol"));
        high.syncTargets(List.of("qol"), List.of("qol"));
        high.advanceSeconds(1.0D / 144.0D);

        assertTrue(high.amount("qol") > 0.0D);
        assertTrue(high.amount("qol") < sixty.amount("qol"));
    }
}
