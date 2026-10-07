package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static fi.rotclient.DungeonPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonTerminalUpstreamRegressionTest {
    @Test
    void blueDoesNotSelectLightBlueOrAnItemWithAnEmbeddedColorWord() {
        List<TerminalItem> items = List.of(
                item(10, "§bLight Blue Wool", "light_blue_wool", false, 1),
                item(11, "§9Blue Wool", "blue_wool", false, 1),
                item(12, "§9Lapis Lazuli", "lapis_lazuli", false, 1),
                item(13, "Enchanted Blue Wool", "blue_wool", false, 1));
        assertEquals(List.of(11, 12), solve(Terminal.SELECT_ALL, "Select all the BLUE items!", items));
        assertEquals(List.of(10), solve(Terminal.SELECT_ALL, "Select all the LIGHT BLUE items!", items));
    }

    @Test
    void distinctGrayAndGreenColorsRetainTheirOwnSolutions() {
        List<TerminalItem> items = List.of(
                item(10, "Light Gray Dye", "light_gray_dye", false, 1),
                item(11, "Gray Wool", "gray_wool", false, 1),
                item(12, "Lime Dye", "lime_dye", false, 1),
                item(13, "Cactus Green", "green_dye", false, 1));
        assertEquals(List.of(11), solve(Terminal.SELECT_ALL, "Select all the GRAY items!", items));
        assertEquals(List.of(10), solve(Terminal.SELECT_ALL, "Select all the SILVER items!", items));
        assertEquals(List.of(13), solve(Terminal.SELECT_ALL, "Select all the GREEN items!", items));
        assertEquals(List.of(12), solve(Terminal.SELECT_ALL, "Select all the LIME items!", items));
    }

    @Test
    void selectedItemsAndRenamedBorderPanesAreExcluded() {
        List<TerminalItem> items = List.of(
                item(10, "Black Wool", "minecraft:black_stained_glass_pane", false, 1),
                item(11, "Ink Sac", "ink_sac", false, 1),
                item(12, "Black Wool", "black_wool", true, 1));
        assertEquals(List.of(11), solve(Terminal.SELECT_ALL, "Select all the BLACK items!", items));
        assertTrue(solve(Terminal.SELECT_ALL, "Select all the INFRARED items!", items).isEmpty());
        assertTrue(solve(Terminal.PANES, "Correct all the panes!", List.of(
                item(11, "Red Stained Glass", "black_stained_glass_pane", false, 1))).isEmpty());
    }

    @Test
    void numericPaneStateAndStackCountOverrideRetainedOrMisleadingDigitNames() {
        List<TerminalItem> items = List.of(
                item(11, "1", "lime_stained_glass_pane", false, 1),
                item(12, "999999999999999999999999", "red_stained_glass_pane", false, 2),
                item(13, "3", "red_stained_glass_pane", false, 1),
                item(14, "4", "red_stained_glass_pane", false, 99),
                item(15, "5", "apple", false, 5));
        assertEquals(List.of(13, 12), solve(Terminal.NUMBERS, "Click in order!", items));
    }

    @Test
    void currentTenPaneAndLegacyFourteenPaneNumbersBothSolveInNumericOrder() {
        for (int width : new int[]{5, 7}) {
            List<TerminalItem> items = new ArrayList<>();
            List<Integer> expected = new ArrayList<>();
            int number = 1;
            for (int row = 1; row <= 2; row++) {
                int firstCol = width == 5 ? 2 : 1;
                for (int col = firstCol; col < firstCol + width; col++) {
                    int slot = row * 9 + col;
                    expected.add(slot);
                    items.add(item(slot, "", "red_stained_glass_pane", false, number++));
                }
            }
            assertEquals(expected, solve(Terminal.NUMBERS, "Click in order!", items));
        }
    }

    @Test
    void startsWithNeedsItsCompleteTitleAndNeverUsesPlayerInventoryOrGlassNames() {
        List<TerminalItem> items = List.of(
                item(10, "Apple", "apple", false, 1),
                item(11, "Apple", "black_stained_glass_pane", false, 1),
                item(40, "Apple", "apple", false, 1));
        assertEquals(List.of(10), solve(Terminal.STARTS_WITH, "What starts with: 'A'?", items));
        assertTrue(solve(Terminal.STARTS_WITH, "Profile starts with: 'A'", items).isEmpty());
        assertTrue(solve(Terminal.NUMBERS, "Select all the BLUE items!", items).isEmpty());
        assertTrue(solve(Terminal.NUMBERS, "Auction: Click in order!", items).isEmpty());
        assertFalse(DungeonTerminalSolverPolicy.supportsTitle(Terminal.MELODY,
                "Do not click the button on time!"));
    }

    @Test
    void rubixCannotTreatLightBlueOrOtherColoredNonPanesAsCycleColors() {
        List<TerminalItem> items = List.of(
                item(12, "Red Stained Glass Pane", "red_stained_glass_pane", false, 1),
                item(13, "Light Blue Stained Glass Pane", "light_blue_stained_glass_pane", false, 1),
                item(14, "Orange Dye", "orange_dye", false, 1));
        assertTrue(solve(Terminal.RUBIX, "Change all to same color!", items).isEmpty());
    }

    @Test
    void melodyBottomTargetAndInventoryPanesCannotProduceAnInvalidClick() {
        List<TerminalItem> items = List.of(
                item(41, "", "magenta_stained_glass_pane", false, 1),
                item(23, "", "lime_stained_glass_pane", false, 1),
                item(63, "", "lime_stained_glass_pane", false, 1));
        assertFalse(parseMelody(items).readyToClick());
        assertTrue(solve(Terminal.MELODY, "Click the button on time!", items).isEmpty());
    }

    @Test
    void threeRowMelodyBlackFooterDoesNotRecreateRemovedFourthButton() {
        List<TerminalItem> items = List.of(
                item(5, "", "magenta_stained_glass_pane", false, 1),
                item(32, "", "lime_stained_glass_pane", false, 1),
                item(34, "", "lime_terracotta", false, 1),
                item(43, "", "black_stained_glass_pane", false, 1),
                item(41, "", "magenta_stained_glass_pane", false, 1));
        assertEquals(3, melodyPlayRows(items));
        assertEquals(List.of(34), solve(Terminal.MELODY, "Click the button on time!", items));
    }

    @Test
    void legacyFourthMelodyButtonStillWorksButAmbiguousSnapshotsWait() {
        List<TerminalItem> items = List.of(
                item(5, "", "magenta_stained_glass_pane", false, 1),
                item(41, "", "lime_stained_glass_pane", false, 1),
                item(43, "", "lime_terracotta", false, 1));
        assertEquals(4, melodyPlayRows(items));
        assertEquals(List.of(43), solve(Terminal.MELODY, "Click the button on time!", items));
        List<TerminalItem> ambiguous = new ArrayList<>(items);
        ambiguous.add(item(23, "", "lime_stained_glass_pane", false, 1));
        assertFalse(parseMelody(ambiguous).readyToClick());
    }

    private static List<Integer> solve(Terminal type, String title, List<TerminalItem> items) {
        return DungeonTerminalSolverPolicy.solveClicks(type, title, items).stream().map(TerminalClick::slot).toList();
    }

    private static TerminalItem item(int slot, String name, String id, boolean glint, int count) {
        return new TerminalItem(slot, name, id, glint, count);
    }
}
