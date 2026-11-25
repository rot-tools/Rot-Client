package fi.rotclient.rebuild.illegal;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AutoHealingSequenceTest {
    @Test void allIndependentDelaysAndHotbarSlotsHaveCorrectBoundaries() {
        for (int select = 1; select <= 10; select++) {
            for (int use = 1; use <= 10; use++) {
                for (int restore = 1; restore <= 10; restore++) {
                    for (int wand = 0; wand < 9; wand++) {
                        var sequence = new AutoHealingSequence();
                        var bar = new Bar((wand + 1) % 9, wand);
                        int original = bar.selected;
                        sequence.start(0, select, use, restore, wand, bar);
                        assertTrue(bar.actions.isEmpty());
                        sequence.advance(select - 1, true, bar);
                        assertTrue(bar.actions.isEmpty());
                        sequence.advance(select, true, bar);
                        assertEquals(List.of("select " + wand), bar.actions);
                        sequence.advance(select + use - 1, true, bar);
                        assertEquals(1, bar.actions.size());
                        sequence.advance(select + use, true, bar);
                        assertEquals(List.of("select " + wand, "use " + wand), bar.actions);
                        sequence.advance(select + use + restore - 1, false, bar);
                        assertTrue(sequence.active());
                        sequence.advance(select + use + restore, false, bar);
                        assertEquals(List.of("select " + wand, "use " + wand, "select " + original), bar.actions);
                        assertFalse(sequence.active());
                    }
                }
            }
        }
    }

    @Test void manualChangesCancelInEveryWaitingPhaseIncludingBetweenTicks() {
        for (int phase = 0; phase < 3; phase++) {
            var sequence = new AutoHealingSequence();
            var bar = new Bar(0, 7);
            sequence.start(0, 2, 2, 2, 7, bar);
            if (phase >= 1) sequence.advance(2, true, bar);
            if (phase == 2) sequence.advance(4, true, bar);
            int actions = bar.actions.size();
            sequence.manualChange();
            bar.selected = 5;
            bar.selected = 7;
            sequence.advance(6, true, bar);
            assertFalse(sequence.active());
            assertEquals(actions, bar.actions.size());
            assertFalse(sequence.canStart(true));
            assertFalse(sequence.canStart(false));
            assertTrue(sequence.canStart(true));
        }
    }

    @Test void missingItemAndRecoveredHpCancelBeforeUse() {
        for (int phase = 0; phase < 2; phase++) {
            var sequence = new AutoHealingSequence();
            var bar = new Bar(0, 7);
            sequence.start(0, 2, 2, 2, 7, bar);
            if (phase == 1) sequence.advance(2, true, bar);
            bar.ready = false;
            sequence.advance(phase == 0 ? 2 : 4, true, bar);
            assertEquals(0, bar.selected);
            assertFalse(sequence.active());
            assertEquals(phase == 0 ? List.of() : List.of("select 7", "select 0"), bar.actions);
        }
        var sequence = new AutoHealingSequence();
        var bar = new Bar(0, 7);
        sequence.start(0, 2, 2, 2, 7, bar);
        sequence.advance(2, false, bar);
        assertTrue(bar.actions.isEmpty());
    }

    @Test void alreadySelectedWandAndUseFailureAreHandled() {
        var sequence = new AutoHealingSequence();
        var bar = new Bar(7, 7);
        sequence.start(0, 1, 1, 1, 7, bar);
        sequence.advance(1, true, bar);
        sequence.advance(2, true, bar);
        sequence.advance(3, true, bar);
        assertEquals(List.of("use 7"), bar.actions);
        bar.selected = 0;
        bar.fail = true;
        sequence.start(4, 1, 1, 1, 7, bar);
        sequence.advance(5, true, bar);
        assertThrows(IllegalStateException.class, () -> sequence.advance(6, true, bar));
        assertEquals(0, bar.selected);
        assertFalse(sequence.active());
    }

    @Test void disablingRestoresAndWorldResetDiscardsPendingWork() {
        var sequence = new AutoHealingSequence();
        var bar = new Bar(0, 7);
        sequence.start(0, 1, 2, 3, 7, bar);
        sequence.advance(1, true, bar);
        sequence.stop(bar);
        assertEquals(0, bar.selected);
        sequence.start(4, 1, 2, 3, 7, bar);
        sequence.reset();
        sequence.advance(5, true, bar);
        assertEquals(List.of("select 7", "select 0"), bar.actions);
    }

    @Test void missingItemStaysIdleAndReinsertionCanStartWithoutToggling() {
        var sequence = new AutoHealingSequence();
        var bar = new Bar(0, 7);
        bar.ready = false;
        sequence.start(0, 2, 2, 2, 7, bar);
        assertFalse(sequence.active());
        for (int tick = 1; tick <= 20; tick++) sequence.advance(tick, true, bar);
        assertTrue(bar.actions.isEmpty());
        bar.ready = true;
        sequence.start(21, 2, 2, 2, 7, bar);
        sequence.advance(23, true, bar);
        sequence.advance(25, true, bar);
        sequence.advance(27, true, bar);
        assertEquals(List.of("select 7", "use 7", "select 0"), bar.actions);
    }

    @Test void removingAnItemCancelsBeforeTheConfiguredDelayExpires() {
        var sequence = new AutoHealingSequence();
        var bar = new Bar(0, 7);
        sequence.start(0, 10, 10, 10, 7, bar);
        bar.ready = false;
        sequence.advance(1, true, bar);
        assertFalse(sequence.active());
        assertTrue(bar.actions.isEmpty());
    }

    @Test void oldSavedSettingsKeepTheirValuesAndGetTimingDefaults() {
        var settings = new Gson().fromJson(
                "{\"enabled\":true,\"healthPercent\":42,\"minimumSeconds\":5,\"item\":\"WAND_OF_ATONEMENT\"}",
                AutoHealingSettings.class);
        settings.normalize();
        assertTrue(settings.enabled);
        assertEquals(42, settings.healthPercent);
        assertEquals(5, settings.minimumSeconds);
        assertEquals(2, settings.selectionDelayTicks);
        assertEquals(2, settings.stepDelayTicks);
        assertEquals(2, settings.returnDelayTicks);
        settings.selectionDelayTicks = -1;
        settings.stepDelayTicks = 99;
        settings.returnDelayTicks = 0;
        settings.normalize();
        assertEquals(1, settings.selectionDelayTicks);
        assertEquals(10, settings.stepDelayTicks);
        assertEquals(1, settings.returnDelayTicks);
    }

    private static final class Bar implements AutoHealingSequence.Hotbar {
        int selected;
        final int wand;
        boolean ready = true;
        boolean fail;
        final List<String> actions = new ArrayList<>();
        Bar(int selected, int wand) { this.selected = selected; this.wand = wand; }
        public int selectedSlot() { return selected; }
        public void selectSlot(int slot) { selected = slot; actions.add("select " + slot); }
        public boolean ready(int slot) { return ready && wand == slot; }
        public void use() {
            if (fail) throw new IllegalStateException();
            actions.add("use " + selected);
        }
    }
}
