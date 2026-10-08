package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuzzlerPolicyTest {
    private static final String CHALLENGE = "[NPC] Puzzler: ◀▲◀▲▲▶▶◀▲▼";
    private static PuzzlerPolicy.Scope scope() {
        return new PuzzlerPolicy.Scope(new Object(), new Object(), new Object(), "profile-a");
    }
    @Test void realTenArrowChallengeUsesTheNpcOrientation() {
        assertEquals(new PuzzlerPolicy.Target(182, 195, 138), PuzzlerPolicy.solve(CHALLENGE).orElseThrow());
        assertEquals(new PuzzlerPolicy.Target(181, 195, 135),
                PuzzlerPolicy.solve("§e[NPC] Puzzler: ▲ ▶ ▼ ◀ ▲ ▶ ▼ ◀ ▲ ▼").orElseThrow());
        assertEquals(new PuzzlerPolicy.Target(181, 195, 145),
                PuzzlerPolicy.solve("[NPC] Puzzler: ▲▲▲▲▲▲▲▲▲▲").orElseThrow());
        assertEquals(new PuzzlerPolicy.Target(171, 195, 135),
                PuzzlerPolicy.solve("[NPC] Puzzler: ▶▶▶▶▶▶▶▶▶▶").orElseThrow());
    }

    @Test void rejectsOtherChatAndMixedSequences() {
        assertTrue(PuzzlerPolicy.solve("Someone: ▲▶").isEmpty());
        assertTrue(PuzzlerPolicy.solve("[NPC] Puzzler: Wrong!").isEmpty());
        assertTrue(PuzzlerPolicy.solve("[NPC] Puzzler: ▲oops▶").isEmpty());
        assertTrue(PuzzlerPolicy.solve("[NPC] Puzzler: ▲▶").isEmpty());
        assertTrue(PuzzlerPolicy.solve("[NPC] Puzzler: ▲▲▲▲▲▲▲▲▲▲▲").isEmpty());
        assertTrue(PuzzlerPolicy.solve("Party > " + CHALLENGE).isEmpty());
    }

    @Test void authenticNpcRepliesRetirePreviousAnswerButUnrelatedChatDoesNot() {
        var state = new PuzzlerPolicy.State();
        var scope = scope();
        state.observe(CHALLENGE, scope, 1_000);
        state.observe("Party > " + CHALLENGE, scope, 1_010);
        assertTrue(state.current(scope, 1_020).isPresent());
        state.observe("[NPC] Puzzler: Thanks for solving my puzzle!", scope, 1_030);
        assertTrue(state.current(scope, 1_030).isEmpty());
        state.observe(CHALLENGE, scope, 1_040);
        state.observe("[NPC] Puzzler: Wrong!", scope, 1_050);
        assertTrue(state.current(scope, 1_050).isEmpty());
    }

    @Test void expiredOrClockReversedAnswerCannotReappear() {
        var state = new PuzzlerPolicy.State();
        var scope = scope();
        state.observe(CHALLENGE, scope, 1_000);
        assertTrue(state.current(scope, 120_999).isPresent());
        assertTrue(state.current(scope, 121_000).isEmpty());
        assertTrue(state.current(scope, 1_001).isEmpty());
        state.observe(CHALLENGE, scope, 2_000);
        assertTrue(state.current(scope, 1_999).isEmpty());
        assertTrue(state.current(scope, 2_001).isEmpty());
        state.observe(CHALLENGE, scope, 3_000);
        assertTrue(state.current(scope, 5_000).isPresent());
        assertTrue(state.current(scope, 4_000).isEmpty());
    }

    @Test void worldPlayerConfigAndHypixelProfileBoundariesClearTheAnswer() {
        var original = scope();
        var changed = new PuzzlerPolicy.Scope[] {
                new PuzzlerPolicy.Scope(new Object(), original.player(), original.config(), original.profileId()),
                new PuzzlerPolicy.Scope(original.world(), new Object(), original.config(), original.profileId()),
                new PuzzlerPolicy.Scope(original.world(), original.player(), new Object(), original.profileId()),
                new PuzzlerPolicy.Scope(original.world(), original.player(), original.config(), "profile-b")
        };
        for (var scope : changed) {
            var state = new PuzzlerPolicy.State();
            state.observe(CHALLENGE, original, 1_000);
            assertTrue(state.current(scope, 1_001).isEmpty());
            assertTrue(state.current(original, 1_002).isEmpty());
        }
    }

    @Test void disableOrLeavingDwarvenMinesClearsInsteadOfHidingAnOldAnswer() {
        var state = new PuzzlerPolicy.State();
        var scope = scope();
        state.observe(CHALLENGE, scope, 1_000);
        assertTrue(state.current(null, 1_001).isEmpty());
        assertTrue(state.current(scope, 1_002).isEmpty());
        state.observe(CHALLENGE, null, 1_003);
        assertTrue(state.current(scope, 1_004).isEmpty());
    }
}
