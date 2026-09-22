package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonTerminalSolverPolicyTest {
    @Test
    void terminalsSolvePanesStartsWithAndNumbers() {
        assertEquals(DungeonPolicy.Terminal.PANES, DungeonPolicy.detectTerminal("Correct all the panes!"));
        assertEquals(DungeonPolicy.Terminal.STARTS_WITH, DungeonPolicy.detectTerminal("What starts with: 'A'?"));
        assertEquals(DungeonPolicy.Terminal.NUMBERS, DungeonPolicy.detectTerminal("Click in order!"));
        List<DungeonPolicy.TerminalItem> panes = List.of(
                new DungeonPolicy.TerminalItem(10, "Lime Stained Glass", "lime_stained_glass", false),
                new DungeonPolicy.TerminalItem(11, "Red Stained Glass", "red_stained_glass", false));
        assertEquals(List.of(11), DungeonPolicy.solveTerminal(DungeonPolicy.Terminal.PANES, "", panes));
        List<DungeonPolicy.TerminalItem> starts = List.of(
                new DungeonPolicy.TerminalItem(10, "Apple", "apple", false),
                new DungeonPolicy.TerminalItem(11, "Bone", "bone", false));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.STARTS_WITH, "What starts with: 'A'?", starts));
        List<DungeonPolicy.TerminalItem> numbers = List.of(
                new DungeonPolicy.TerminalItem(10, "2", "blue_stained_glass", false),
                new DungeonPolicy.TerminalItem(12, "1", "blue_stained_glass", false),
                new DungeonPolicy.TerminalItem(13, "3", "blue_stained_glass", true));
        assertEquals(List.of(12, 10), DungeonPolicy.solveTerminal(DungeonPolicy.Terminal.NUMBERS, "", numbers));
        List<DungeonPolicy.TerminalItem> ten = List.of(
                new DungeonPolicy.TerminalItem(10, "10", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(11, "1", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(12, "5", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(13, "2", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(14, "8", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(19, "3", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(20, "9", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(21, "4", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(22, "6", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(23, "7", "red_stained_glass_pane", false));
        assertEquals(List.of(11, 13, 19, 21, 12, 22, 23, 14, 20, 10),
                DungeonPolicy.solveTerminal(DungeonPolicy.Terminal.NUMBERS, "", ten));
        List<DungeonPolicy.TerminalItem> melody = List.of(
                new DungeonPolicy.TerminalItem(5, "Magenta", "magenta_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(23, "Lime", "lime_stained_glass_pane", false));
        DungeonPolicy.MelodyState state = DungeonPolicy.parseMelody(melody);
        assertEquals(4, state.correct());
        assertEquals(1, state.buttonRow());
        assertEquals(4, state.current());
        assertTrue(state.readyToClick());
        assertEquals(25, state.clickSlot());
        List<DungeonPolicy.TerminalItem> threeRowMelody = List.of(
                new DungeonPolicy.TerminalItem(16, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(25, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(34, "Button", "lime_terracotta", false),
                new DungeonPolicy.TerminalItem(11, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(20, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(29, "Note", "white_stained_glass_pane", false));
        assertEquals(3, DungeonPolicy.melodyPlayRows(threeRowMelody));
        List<DungeonPolicy.TerminalItem> fourRowMelody = List.of(
                new DungeonPolicy.TerminalItem(16, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(25, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(34, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(43, "Button", "lime_terracotta", false),
                new DungeonPolicy.TerminalItem(11, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(20, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(29, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(38, "Note", "white_stained_glass_pane", false));
        assertEquals(4, DungeonPolicy.melodyPlayRows(fourRowMelody));
        List<DungeonPolicy.TerminalItem> colors = List.of(
                new DungeonPolicy.TerminalItem(10, "Light Gray Dye", "light_gray_dye", false),
                new DungeonPolicy.TerminalItem(11, "Ink Sac", "ink_sac", false));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, "Select all the silver items!", colors));
        assertEquals(DungeonPolicy.Terminal.COLORS, DungeonPolicy.detectTerminal("What color was red?"));
        assertEquals(DungeonPolicy.Terminal.COLORS, DungeonPolicy.detectTerminal("Select the color silver"));
        assertEquals("red", DungeonTerminalSolverPolicy.extractSelectColor("What color was red?"));
        assertEquals("silver", DungeonTerminalSolverPolicy.extractSelectColor("Select the color light gray"));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.COLORS, "What color was silver?", colors));
        assertTrue(DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.COLORS, "What color was the stained glass?", colors).isEmpty());
        List<DungeonPolicy.TerminalItem> greenItems = List.of(
                new DungeonPolicy.TerminalItem(10, "Lime Dye", "lime_dye", false),
                new DungeonPolicy.TerminalItem(11, "Cactus Green", "cactus_green", false),
                new DungeonPolicy.TerminalItem(12, "Poppy", "poppy", false));
        assertEquals(List.of(10, 11), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, "Select all the green items!", greenItems));
        assertEquals(List.of(12), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, "Select all the red items!", greenItems));
        List<DungeonPolicy.TerminalItem> borderPanes = List.of(
                new DungeonPolicy.TerminalItem(10, "Red Stained Glass", "red_stained_glass", false),
                new DungeonPolicy.TerminalItem(11, "Red Stained Glass", "red_stained_glass", false),
                new DungeonPolicy.TerminalItem(16, "Red Stained Glass", "red_stained_glass", false));
        assertEquals(List.of(11), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.PANES, "Correct all the panes!", borderPanes));
        List<DungeonPolicy.TerminalItem> extraNumber = List.of(
                new DungeonPolicy.TerminalItem(11, "1", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(12, "2", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(28, "3", "red_stained_glass_pane", false));
        assertEquals(List.of(11, 12), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.NUMBERS, "Click in order!", extraNumber));
        List<DungeonPolicy.TerminalItem> extraStarts = List.of(
                new DungeonPolicy.TerminalItem(10, "Apple", "apple", false),
                new DungeonPolicy.TerminalItem(37, "Axe", "iron_axe", false));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.STARTS_WITH, "What starts with: 'A'?", extraStarts));
    }
}
