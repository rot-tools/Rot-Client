package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class TermSimPolicyTest {
    @Test
    void randomPoolExcludesMelody() {
        for (TermSimPolicy.Kind kind : TermSimPolicy.RANDOM_POOL) {
            assertFalse(kind == TermSimPolicy.Kind.MELODY);
        }
        assertEquals(5, TermSimPolicy.RANDOM_POOL.length);
    }

    @Test
    void titlesMatchHypixelWindows() {
        assertEquals("Correct all the panes!", TermSimPolicy.titleFor(TermSimPolicy.Kind.PANES));
        assertEquals("Click in order!", TermSimPolicy.titleFor(TermSimPolicy.Kind.NUMBERS));
        assertEquals("Change all to same color!", TermSimPolicy.titleFor(TermSimPolicy.Kind.RUBIX));
        assertEquals("Click the button on time!", TermSimPolicy.titleFor(TermSimPolicy.Kind.MELODY));
        assertTrue(TermSimPolicy.generate(TermSimPolicy.Kind.STARTS_WITH, new Random(1))
                .title()
                .startsWith("What starts with:"));
        assertTrue(TermSimPolicy.generate(TermSimPolicy.Kind.SELECT_ALL, new Random(2))
                .title()
                .startsWith("Select all the"));
    }

    @Test
    void panesClicksRedUntilNoneRemain() {
        Random rng = new Random(3);
        TermSimPolicy.Layout layout = TermSimPolicy.generate(TermSimPolicy.Kind.PANES, rng);
        assertEquals(DungeonPolicy.Terminal.PANES, DungeonPolicy.detectTerminal(layout.title()));
        List<Integer> hits = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.PANES, layout.title(), TermSimPolicy.terminalItems(layout));
        assertFalse(hits.isEmpty());
        TermSimPolicy.Layout current = layout;
        for (int i = 0; i < 40 && !hits.isEmpty(); i++) {
            TermSimPolicy.ClickResult result = TermSimPolicy.click(current, hits.getFirst(), 0);
            assertTrue(result.accepted());
            current = result.layout();
            hits = DungeonPolicy.solveTerminal(
                    DungeonPolicy.Terminal.PANES, current.title(), TermSimPolicy.terminalItems(current));
            if (result.complete()) {
                assertTrue(hits.isEmpty());
                return;
            }
        }
        assertTrue(hits.isEmpty());
    }

    @Test
    void numbersAutoClicksFirstRemainingOnly() {
        TermSimPolicy.Layout layout = TermSimPolicy.generate(TermSimPolicy.Kind.NUMBERS, new Random(4));
        List<Integer> remaining = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.NUMBERS, layout.title(), TermSimPolicy.terminalItems(layout));
        assertTrue(remaining.size() > 1);
        TermSimPolicy.ClickResult wrong = TermSimPolicy.click(layout, remaining.get(1), 0);
        assertFalse(wrong.accepted());
        TermSimPolicy.ClickResult first = TermSimPolicy.click(layout, remaining.getFirst(), 0);
        assertTrue(first.accepted());
        List<Integer> after = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.NUMBERS, first.layout().title(),
                TermSimPolicy.terminalItems(first.layout()));
        assertEquals(remaining.size() - 1, after.size());
    }

    @Test
    void rubixCyclesAndCompletesWhenUniform() {
        TermSimPolicy.Layout layout = TermSimPolicy.generate(TermSimPolicy.Kind.RUBIX, new Random(5));
        TermSimPolicy.Layout current = layout;
        for (int i = 0; i < 80; i++) {
            List<DungeonPolicy.TerminalClick> clicks = DungeonPolicy.solveTerminalClicks(
                    DungeonPolicy.Terminal.RUBIX, current.title(), TermSimPolicy.terminalItems(current));
            if (clicks.isEmpty()) {
                assertTrue(TermSimPolicy.click(current, 12, 0).complete() || uniformRubix(current));
                return;
            }
            DungeonPolicy.TerminalClick click = clicks.getFirst();
            TermSimPolicy.ClickResult result = TermSimPolicy.click(current, click.slot(), click.button());
            assertTrue(result.accepted());
            current = result.layout();
            if (result.complete()) {
                assertTrue(uniformRubix(current));
                return;
            }
        }
        assertTrue(uniformRubix(current));
    }

    @Test
    void startsWithAndSelectAllUseEnchantedAsDone() {
        TermSimPolicy.Layout starts = TermSimPolicy.generate(TermSimPolicy.Kind.STARTS_WITH, new Random(6));
        List<Integer> hits = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.STARTS_WITH, starts.title(), TermSimPolicy.terminalItems(starts));
        assertFalse(hits.isEmpty());
        TermSimPolicy.ClickResult clicked = TermSimPolicy.click(starts, hits.getFirst(), 0);
        assertTrue(clicked.accepted());
        assertTrue(clicked.layout().slotAt(hits.getFirst()).enchanted());

        TermSimPolicy.Layout select = TermSimPolicy.generate(TermSimPolicy.Kind.SELECT_ALL, new Random(7));
        List<Integer> colorHits = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, select.title(), TermSimPolicy.terminalItems(select));
        assertFalse(colorHits.isEmpty());
        TermSimPolicy.ClickResult colorClick = TermSimPolicy.click(select, colorHits.getFirst(), 0);
        assertTrue(colorClick.accepted());
        assertTrue(colorClick.layout().slotAt(colorHits.getFirst()).enchanted());
    }

    @Test
    void melodyAdvancesWhenColumnMatches() {
        TermSimPolicy.Layout layout = TermSimPolicy.generate(TermSimPolicy.Kind.MELODY, new Random(8));
        TermSimPolicy.MelodyClock clock = layout.melody();
        TermSimPolicy.Layout aligned = layout;
        for (int i = 0; i < 80 && aligned.melody().limeColumn() != aligned.melody().magentaColumn(); i++) {
            aligned = TermSimPolicy.tickMelody(aligned);
        }
        assertEquals(aligned.melody().limeColumn(), aligned.melody().magentaColumn());
        int button = aligned.melody().currentRow() * 9 + 7;
        TermSimPolicy.ClickResult result = TermSimPolicy.click(aligned, button, 0);
        assertTrue(result.accepted());
        assertEquals(clock.currentRow() + 1, result.layout().melody().currentRow());
    }

    @Test
    void hubMapsSlotsAndPingTicksMatchLayout() {
        assertEquals(TermSimPolicy.Kind.PANES, TermSimPolicy.hubChoice(10));
        assertEquals(TermSimPolicy.Kind.MELODY, TermSimPolicy.hubChoice(16));
        assertEquals(0, TermSimPolicy.pingTicks(0));
        assertEquals(3, TermSimPolicy.pingTicks(150));
        assertTrue(TermSimPolicy.click(TermSimPolicy.hub(), 13, 0).accepted());
    }

    private static boolean uniformRubix(TermSimPolicy.Layout layout) {
        String first = layout.slotAt(12).itemId();
        for (int index : TermSimPolicy.RUBIX_SLOTS) {
            if (!first.equals(layout.slotAt(index).itemId())) {
                return false;
            }
        }
        return true;
    }
}
