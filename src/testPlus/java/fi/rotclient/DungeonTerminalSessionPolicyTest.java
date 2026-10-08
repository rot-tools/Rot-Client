package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static fi.rotclient.DungeonPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonTerminalSessionPolicyTest {
    private static final String RUBIX = "Change all to same color!";
    private static final String STARTS = "What starts with: 'N'?";

    @Test
    void rubixWaitsForCompleteBoardThenRetainsGoalThroughCorrections() {
        Object menu = new Object();
        DungeonTerminalSessionPolicy session = new DungeonTerminalSessionPolicy();
        session.bind(menu, RUBIX);
        List<TerminalItem> initial = board(3, 3, 3, 3, 3, 3, 3, 3, 4);
        assertTrue(solve(session, Terminal.RUBIX, RUBIX, initial.subList(0, 8), false, 1_000).isEmpty());
        assertEquals(-1, session.lockedRubixGoal());
        assertEquals(List.of(new TerminalClick(32, 1)), solve(session, Terminal.RUBIX, RUBIX, initial, false, 1_000));
        assertEquals(3, session.lockedRubixGoal());

        List<TerminalItem> correction = board(4, 4, 4, 4, 4, 4, 4, 4, 3);
        assertEquals(4, DungeonTerminalSolverPolicy.rubixGoal(correction, false, true));
        List<TerminalClick> locked = solve(session, Terminal.RUBIX, RUBIX, correction, false, 1_100);
        assertEquals(8, locked.size());
        assertTrue(locked.stream().allMatch(click -> click.button() == 1));
        assertEquals(3, session.lockedRubixGoal());

        session.bind(new Object(), RUBIX);
        assertEquals(List.of(new TerminalClick(32, 0)), solve(session, Terminal.RUBIX, RUBIX, correction, false, 1_200));
        assertEquals(4, session.lockedRubixGoal());
    }

    @Test
    void leftOnlyGoalMinimizesForwardClicksRatherThanForcingReverseSolutionsLeft() {
        List<TerminalItem> initial = board(0, 0, 0, 0, 0, 0, 0, 1, 1);
        assertEquals(0, DungeonTerminalSolverPolicy.rubixGoal(initial, false, true));
        assertEquals(1, DungeonTerminalSolverPolicy.rubixGoal(initial, true, true));
        DungeonTerminalSessionPolicy session = new DungeonTerminalSessionPolicy();
        session.bind(new Object(), RUBIX);
        List<TerminalClick> clicks = solve(session, Terminal.RUBIX, RUBIX, initial, true, 1_000);
        assertEquals(7, clicks.size());
        assertTrue(clicks.stream().allMatch(click -> click.button() == 0));
        assertEquals(1, session.lockedRubixGoal());
        solve(session, Terminal.RUBIX, RUBIX, initial, false, 1_100);
        assertEquals(1, session.lockedRubixGoal());
    }

    @Test
    void freshServerGlintUpdateAcknowledgesNativeGlintItemPermanentlyForMenu() {
        Object menu = new Object();
        DungeonTerminalSessionPolicy session = starts(menu);
        List<TerminalItem> initial = nativeItem();
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 1_000));
        session.clickSent(menu, STARTS, 10, 40, initial, 1_000);
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 1_001).isEmpty());
        assertTrue(session.serverSlot(menu, STARTS, 41, nativeGlint(10)));
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 3_000).isEmpty());
    }

    @Test
    void unchangedAndOlderStateResendsCannotConfirmNativeItemAndTimeoutAllowsRetry() {
        Object menu = new Object();
        DungeonTerminalSessionPolicy session = starts(menu);
        List<TerminalItem> initial = nativeItem();
        session.clickSent(menu, STARTS, 10, 40, initial, 1_000);
        assertFalse(session.serverSlot(menu, STARTS, 40, nativeGlint(10)));
        assertFalse(session.serverSlot(menu, STARTS, 39, nativeGlint(10)));
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 1_100).isEmpty());
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 1_800));
    }

    @Test
    void differentContainerSlotAndTitleCannotAcknowledgeOrCorrectActiveMenu() {
        Object menu = new Object();
        Object otherMenu = new Object();
        DungeonTerminalSessionPolicy session = starts(menu);
        session.clickSent(menu, STARTS, 10, 40, nativeItem(), 1_000);
        assertFalse(session.serverSlot(otherMenu, STARTS, 41, nativeGlint(10)));
        assertFalse(session.serverSlot(menu, "What starts with: 'A'?", 41, nativeGlint(10)));
        assertFalse(session.serverSlot(menu, STARTS, 41, nativeGlint(11)));
        assertTrue(session.serverSlot(menu, STARTS, 41, nativeGlint(10)));
        assertFalse(session.serverSlot(otherMenu, STARTS, 42,
                new TerminalItem(10, "Nether Star", "nether_star", false, 1)));
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 3_000).isEmpty());
        session.bind(otherMenu, STARTS);
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 3_001));
    }

    @Test
    void freshNonglintRejectionAndItemReplacementPermitRetryWithoutFalseSuccess() {
        Object menu = new Object();
        DungeonTerminalSessionPolicy session = starts(menu);
        List<TerminalItem> initial = List.of(new TerminalItem(10, "Netherrack", "netherrack", false, 1));
        session.clickSent(menu, STARTS, 10, 40, initial, 1_000);
        assertTrue(session.serverSlot(menu, STARTS, 41, initial.getFirst()));
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 1_001));

        session.clickSent(menu, STARTS, 10, 41, initial, 1_001);
        assertTrue(session.serverSlot(menu, STARTS, 42,
                new TerminalItem(10, "Netherrack", "netherrack", true, 1)));
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, initial, false, 1_002).isEmpty());
        assertTrue(session.serverSlot(menu, STARTS, 43,
                new TerminalItem(10, "Nether Star", "nether_star", true, 1)));
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 1_003));
    }

    @Test
    void stateFreshnessHandlesWrapAndRejectsInvalidOrAmbiguousStateIds() {
        assertTrue(DungeonTerminalSessionPolicy.stateAdvanced(32_767, 0));
        assertTrue(DungeonTerminalSessionPolicy.stateAdvanced(32_766, 1));
        assertFalse(DungeonTerminalSessionPolicy.stateAdvanced(0, 32_767));
        assertFalse(DungeonTerminalSessionPolicy.stateAdvanced(7, 7));
        assertFalse(DungeonTerminalSessionPolicy.stateAdvanced(0, 16_384));
        assertFalse(DungeonTerminalSessionPolicy.stateAdvanced(-1, 1));
        assertFalse(DungeonTerminalSessionPolicy.stateAdvanced(1, 32_768));
    }

    @Test
    void stalePacketWithoutSentClickDoesNothingButLocalAcceptedPracticeIsExplicitEvidence() {
        Object menu = new Object();
        DungeonTerminalSessionPolicy session = starts(menu);
        assertFalse(session.serverSlot(menu, STARTS, 41, nativeGlint(10)));
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 1_000));
        session.simulatorAccepted(menu, STARTS, 10, nativeItem());
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 3_000).isEmpty());
        session.reset();
        session.bind(menu, STARTS);
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 3_001));
    }

    @Test
    void disableCorrectionAndReenableCannotRestoreOldAcknowledgementOrGoal() {
        Object menu = new Object();
        DungeonTerminalSessionPolicy session = starts(menu);
        session.clickSent(menu, STARTS, 10, 40, nativeItem(), 1_000);
        assertTrue(session.serverSlot(menu, STARTS, 41, nativeGlint(10)));
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 1_001).isEmpty());
        session.enabled(false);
        assertFalse(session.serverSlot(menu, STARTS, 42,
                new TerminalItem(10, "Nether Star", "nether_star", false, 1)));
        session.bind(menu, STARTS);
        assertTrue(solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 1_002).isEmpty());
        session.enabled(true);
        session.bind(menu, STARTS);
        assertEquals(List.of(new TerminalClick(10, 0)), solve(session, Terminal.STARTS_WITH, STARTS, nativeItem(), false, 1_003));

        session.bind(menu, RUBIX);
        solve(session, Terminal.RUBIX, RUBIX, board(3, 3, 3, 3, 3, 3, 3, 3, 4), false, 1_004);
        assertEquals(3, session.lockedRubixGoal());
        session.enabled(false);
        session.enabled(true);
        session.bind(menu, RUBIX);
        solve(session, Terminal.RUBIX, RUBIX, board(4, 4, 4, 4, 4, 4, 4, 4, 3), false, 1_005);
        assertEquals(4, session.lockedRubixGoal());
    }

    private static DungeonTerminalSessionPolicy starts(Object menu) {
        DungeonTerminalSessionPolicy session = new DungeonTerminalSessionPolicy();
        session.bind(menu, STARTS);
        return session;
    }

    private static List<TerminalClick> solve(DungeonTerminalSessionPolicy session, Terminal type,
                                            String title, List<TerminalItem> items, boolean leftOnly, long now) {
        return session.solve(type, title, items, leftOnly, now, 800);
    }

    private static List<TerminalItem> nativeItem() {
        // The bridge masks intrinsic glint for solving. Server packets carry raw glint.
        return List.of(new TerminalItem(10, "Nether Star", "nether_star", false, 1));
    }

    private static TerminalItem nativeGlint(int slot) {
        return new TerminalItem(slot, "Nether Star", "nether_star", true, 1);
    }

    private static List<TerminalItem> board(int... colors) {
        int[] slots = {12, 13, 14, 21, 22, 23, 30, 31, 32};
        String[] names = {"red", "orange", "yellow", "green", "blue"};
        List<TerminalItem> items = new ArrayList<>();
        for (int i = 0; i < colors.length; i++) {
            String color = names[colors[i]];
            items.add(new TerminalItem(slots[i], color, color + "_stained_glass_pane", false, 1));
        }
        return List.copyOf(items);
    }
}
