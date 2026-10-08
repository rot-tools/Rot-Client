package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonF7TimerObservationPolicyTest {
    private static final String START = "[BOSS] Storm: Pathetic Maxor, just like expected.";
    @Test void stormStartsIndependentPadAndLightningInNominalTicks() {
        var state = DungeonF7TimerObservationPolicy.observe(null, START, 1000);
        assertEquals(2000L, state.deadlines().get(DungeonAssistPolicy.F7Timer.STORM_PAD));
        assertEquals(27000L, state.deadlines().get(DungeonAssistPolicy.F7Timer.STORM_LIGHTNING));
        state = DungeonF7TimerObservationPolicy.observe(state, "[BOSS] Storm: ENERGY HEED MY CALL!", 2000);
        assertEquals(4750L, state.deadlines().get(DungeonAssistPolicy.F7Timer.STORM_PY));
        assertEquals(27000L, state.deadlines().get(DungeonAssistPolicy.F7Timer.STORM_LIGHTNING));
    }
    @Test void expiredAndRepeatedDialogueCannotExtendADeadline() {
        var first = DungeonF7TimerObservationPolicy.observe(null, START, 1000);
        assertEquals(first, DungeonF7TimerObservationPolicy.observe(first, START, 50000));
        assertEquals(first, DungeonF7TimerObservationPolicy.observe(first, "Party > Bob: " + START, 2000));
        assertEquals(first, DungeonF7TimerObservationPolicy.observe(first, "[BOSS] Storm: ENERGY HEED MY CALL! fake", 2000));
    }
    @Test void stormDeathAndCoreOpeningClearOldPhaseTimers() {
        var state = DungeonF7TimerObservationPolicy.observe(null, START, 1000);
        state = DungeonF7TimerObservationPolicy.observe(state,
                "[BOSS] Storm: I should have known that I stood no chance.", 2000);
        assertTrue(state.deadlines().isEmpty());
        state = DungeonF7TimerObservationPolicy.observe(state,
                "[BOSS] Goldor: Who dares trespass into my domain?", 3000);
        assertEquals(6000L, state.deadlines().get(DungeonAssistPolicy.F7Timer.GOLDOR));
        assertEquals(state, DungeonF7TimerObservationPolicy.observe(state,
                "[BOSS] Storm: I should have known that I stood no chance.", 4000));
        assertEquals(state, DungeonF7TimerObservationPolicy.observe(state, START, 4000));
        state = DungeonF7TimerObservationPolicy.observe(state, "The Core entrance is opening!", 4500);
        assertTrue(state.deadlines().isEmpty());
    }
    @Test void nominalCyclesKeepTheirOriginalAnchorUntilThePhaseEnds() {
        var state = DungeonF7TimerObservationPolicy.observe(null, START, 1000);
        assertEquals(3000L, DungeonF7TimerObservationPolicy.displayedDeadline(state,
                DungeonAssistPolicy.F7Timer.STORM_PAD, 2600));
        assertEquals(2000L, state.deadlines().get(DungeonAssistPolicy.F7Timer.STORM_PAD));
        state = DungeonF7TimerObservationPolicy.observe(state, "[BOSS] Goldor: Who dares trespass into my domain?", 3000);
        assertEquals(9000L, DungeonF7TimerObservationPolicy.displayedDeadline(state,
                DungeonAssistPolicy.F7Timer.GOLDOR, 6500));
        assertEquals(0L, DungeonF7TimerObservationPolicy.displayedDeadline(state,
                DungeonAssistPolicy.F7Timer.STORM_PAD, 6500));
        state = DungeonF7TimerObservationPolicy.observe(state, "The Core entrance is opening!", 7000);
        assertEquals(0L, DungeonF7TimerObservationPolicy.displayedDeadline(state,
                DungeonAssistPolicy.F7Timer.GOLDOR, 7500));
    }
    @Test void invalidClockAndUnrelatedDialogueLeaveStateUnchanged() {
        var state = DungeonF7TimerObservationPolicy.empty();
        assertEquals(state, DungeonF7TimerObservationPolicy.observe(state, START, -1));
        assertEquals(state, DungeonF7TimerObservationPolicy.observe(state, START, Long.MAX_VALUE));
        assertEquals(state, DungeonF7TimerObservationPolicy.observe(state, "hello", 1000));
    }
}
