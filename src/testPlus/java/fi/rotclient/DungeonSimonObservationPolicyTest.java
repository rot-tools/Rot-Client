package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class DungeonSimonObservationPolicyTest {
    private static final EmberDungeonPolicy.IntVec A = new EmberDungeonPolicy.IntVec(110, 121, 93);
    private static final EmberDungeonPolicy.IntVec B = new EmberDungeonPolicy.IntVec(110, 122, 94);
    private static final List<EmberDungeonPolicy.IntVec> GRID = DungeonF7Policy.simonButtons();

    @Test
    void separatedFlashesPreserveOrderAndRepeatsWithoutGuessingTheInputPhase() {
        var state = playback(null, A);
        state = playback(state, A); // The same lit sample is not another flash.
        for (int i = 0; i < 60; i++) state = playback(state);
        assertNull(state.simon().nextButton());
        assertEquals(DungeonSimonObservationPolicy.Phase.PLAYBACK, state.phase());
        state = playback(state, B);
        state = playback(state);
        state = playback(state, A);
        state = playback(state);
        assertEquals(List.of(A, B, A), state.simon().order());
        assertTrue(state.simon().remaining().isEmpty());
        state = input(state);
        assertEquals(List.of(A, B, A), state.simon().remaining());
        assertEquals(A, state.simon().nextButton());
    }

    @Test
    void actualButtonGridAppearanceGatesInputAndWaitsForTheLastLightToGoDark() {
        var state = playback(null, A);
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID.subList(0, 8), List.of(), false, true);
        assertNull(state.simon().nextButton());
        state = DungeonSimonObservationPolicy.observe(state, List.of(B), GRID, List.of(), false, true);
        assertEquals(DungeonSimonObservationPolicy.Phase.PLAYBACK, state.phase());
        assertNull(state.simon().nextButton());
        state = input(state);
        assertEquals(List.of(A, B), state.simon().remaining());
        assertEquals(DungeonSimonObservationPolicy.Phase.INPUT, state.phase());
    }

    @Test
    void onlyCorrectPoweredEdgesAdvanceAndInputFeedbackNeverAddsFlashes() {
        var state = playback(null, A);
        state = playback(state);
        state = playback(state, B);
        state = input(state);
        state = DungeonSimonObservationPolicy.observe(state, List.of(B), GRID, List.of(B), false, true);
        assertEquals(List.of(A, B), state.simon().remaining()); // Wrong input does not advance.
        state = input(state);
        state = DungeonSimonObservationPolicy.observe(state, List.of(A), GRID, List.of(A), false, true);
        assertEquals(List.of(B), state.simon().remaining());
        assertEquals(List.of(A, B), state.simon().order());
        state = DungeonSimonObservationPolicy.observe(state, List.of(A), GRID, List.of(A), false, true);
        assertEquals(List.of(B), state.simon().remaining());
        state = input(state);
        state = DungeonSimonObservationPolicy.observe(state, List.of(B), GRID, List.of(B), false, true);
        assertEquals(DungeonSimonObservationPolicy.Phase.COMPLETE, state.phase());
        assertNull(state.simon().nextButton());
        assertEquals(List.of(A, B), state.simon().order());
    }

    @Test
    void repeatedButtonNeedsSeparatePoweredPulses() {
        var state = playback(null, A);
        state = playback(state);
        state = playback(state, A);
        state = input(state);
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(A), false, true);
        assertEquals(List.of(A), state.simon().remaining());
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(A), false, true);
        assertEquals(List.of(A), state.simon().remaining());
        state = input(state);
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(A), false, true);
        assertEquals(DungeonSimonObservationPolicy.Phase.COMPLETE, state.phase());
    }

    @Test
    void disappearedButtonsClearTheOldRoundBeforeTheNextFullPlayback() {
        var state = input(playback(null, A));
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(A), false, true);
        state = playback(state);
        assertTrue(state.simon().order().isEmpty());
        assertEquals(DungeonSimonObservationPolicy.Phase.PLAYBACK, state.phase());
        state = playback(state, B);
        state = playback(state);
        state = playback(state, A);
        state = input(state);
        assertEquals(List.of(B, A), state.simon().order());
        assertEquals(B, state.simon().nextButton());
    }

    @Test
    void contextLossAndPoweredStartResetAllPlaybackAndProgress() {
        var state = input(playback(null, A));
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(), true, true);
        assertTrue(state.simon().order().isEmpty());
        assertNull(state.simon().nextButton());
        state = DungeonSimonObservationPolicy.observe(state, List.of(B), List.of(), List.of(), true, true);
        state = DungeonSimonObservationPolicy.observe(state, List.of(), List.of(), List.of(), true, true);
        state = DungeonSimonObservationPolicy.observe(state, List.of(A), List.of(), List.of(), true, true);
        assertEquals(List.of(B, A), state.simon().order()); // Held start power does not keep resetting.
        state = DungeonSimonObservationPolicy.observe(state, List.of(A), GRID, List.of(A), true, false);
        assertEquals(DungeonSimonObservationPolicy.empty(), state);
        assertTrue(DungeonSimonObservationPolicy.restart().simon().order().isEmpty());
    }

    @Test
    void simultaneousUnorderedFlashesSuppressSuggestionsUntilRestart() {
        var state = playback(null, A);
        state = playback(state);
        state = playback(state, A, B);
        state = input(state);
        assertFalse(state.ordered());
        assertNull(state.simon().nextButton());
        assertTrue(state.simon().remaining().isEmpty());
        state = playback(DungeonSimonObservationPolicy.restart(), B);
        state = input(state);
        assertTrue(state.ordered());
        assertEquals(B, state.simon().nextButton());
    }

    @Test
    void missedInitialOrderRecoversWhenButtonsDisappearForTheNextRound() {
        var state = input(playback(null, A, B));
        assertFalse(state.ordered());
        assertNull(state.simon().nextButton());
        state = playback(state, B);
        state = input(state);
        assertTrue(state.ordered());
        assertEquals(List.of(B), state.simon().remaining());
    }

    @Test
    void joiningDuringInputAndUnrelatedBlockCoordinatesNeverInventASequence() {
        var state = DungeonSimonObservationPolicy.observe(null, List.of(A), GRID, List.of(A), false, true);
        assertEquals(DungeonSimonObservationPolicy.Phase.IDLE, state.phase());
        assertNull(state.simon().nextButton());
        var unrelated = new EmberDungeonPolicy.IntVec(111, 121, 93);
        state = DungeonSimonObservationPolicy.observe(null, List.of(unrelated), GRID, List.of(), false, true);
        assertTrue(state.simon().order().isEmpty());
        state = playback(null, A);
        var incompleteGrid = new ArrayList<>(GRID.subList(0, 8));
        incompleteGrid.add(unrelated);
        incompleteGrid.add(DungeonF7Policy.simonStart());
        state = DungeonSimonObservationPolicy.observe(state, List.of(), incompleteGrid, List.of(A), false, true);
        assertEquals(DungeonSimonObservationPolicy.Phase.PLAYBACK, state.phase());
        assertNull(state.simon().nextButton());
    }

    @Test
    void multiplePowerEdgesCannotChooseAnArbitraryOrderAndMissingButtonsCannotAdvance() {
        var state = playback(null, A);
        state = playback(state);
        state = playback(state, B);
        state = input(state);
        state = DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(A, B), false, true);
        assertFalse(state.ordered());
        assertNull(state.simon().nextButton());
        state = input(playback(null, A));
        var gridWithoutA = new ArrayList<>(GRID);
        gridWithoutA.remove(A);
        state = DungeonSimonObservationPolicy.observe(state, List.of(), gridWithoutA, List.of(A), false, true);
        assertEquals(List.of(A), state.simon().remaining());
    }

    @Test
    void snapshotsAreDetachedAndDuplicateCoordinatesAreNotExtraFlashes() {
        var lit = new ArrayList<>(List.of(A, A));
        var state = DungeonSimonObservationPolicy.observe(null, lit, null, null, false, true);
        lit.clear();
        assertEquals(List.of(A), state.simon().order());
        assertEquals(Set.of(A), state.simon().prevLit());
        assertThrows(UnsupportedOperationException.class, () -> state.simon().order().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.poweredButtons().add(A));
    }

    private static DungeonSimonObservationPolicy.State playback(
            DungeonSimonObservationPolicy.State state, EmberDungeonPolicy.IntVec... lit) {
        return DungeonSimonObservationPolicy.observe(state, List.of(lit), List.of(), List.of(), false, true);
    }

    private static DungeonSimonObservationPolicy.State input(DungeonSimonObservationPolicy.State state) {
        return DungeonSimonObservationPolicy.observe(state, List.of(), GRID, List.of(), false, true);
    }
}
