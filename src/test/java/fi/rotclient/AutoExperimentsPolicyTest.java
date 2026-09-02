package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoExperimentsPolicyTest {
    private static final AutoExperimentsPolicy.Options DEFAULT =
            new AutoExperimentsPolicy.Options(false, 0);

    private static AutoExperimentsPolicy.SlotView slot(
            int index, boolean glint, String path, String name, int count) {
        return new AutoExperimentsPolicy.SlotView(index, glint, path, name, count);
    }

    private static List<AutoExperimentsPolicy.SlotView> board(
            AutoExperimentsPolicy.SlotView center, AutoExperimentsPolicy.SlotView... extras) {
        List<AutoExperimentsPolicy.SlotView> slots = new ArrayList<>();
        for (int index = 0; index < 54; index++) {
            slots.add(slot(index, false, "black_stained_glass_pane", "", 1));
        }
        slots.set(AutoExperimentsPolicy.CENTER_SLOT, center);
        for (AutoExperimentsPolicy.SlotView extra : extras) {
            slots.set(extra.index(), extra);
        }
        return slots;
    }

    @Test
    void classifiesExperimentTitles() {
        assertEquals(AutoExperimentsPolicy.Experiment.CHRONOMATRON,
                AutoExperimentsPolicy.forTitle("Chronomatron (Beginner)"));
        assertEquals(AutoExperimentsPolicy.Experiment.ULTRASEQUENCER,
                AutoExperimentsPolicy.forTitle("Ultrasequencer (Expert)"));
        assertNull(AutoExperimentsPolicy.forTitle("Superpairs (Grand)"));
        assertNull(AutoExperimentsPolicy.forTitle("Your Bags"));
        assertNull(AutoExperimentsPolicy.forTitle(null));
    }

    @Test
    void chronomatronRecordsTheGlintingSlotWhenClockIsCenter() {
        AutoExperimentsPolicy.Handler handler =
                AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.Experiment.CHRONOMATRON);
        assertInstanceOf(AutoExperimentsPolicy.ChronomatronHandler.class, handler);

        handler.onSlotUpdate(board(
                slot(49, false, "clock", "Chronomatron", 1),
                slot(12, true, "red_terracotta", "", 1)), DEFAULT);

        assertEquals(OptionalInt.of(12), handler.nextClick());
        assertTrue(handler.nextClick().isEmpty());
    }

    @Test
    void chronomatronAppendsEachRoundAndReplaysFromTheStart() {
        AutoExperimentsPolicy.Handler handler =
                AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.Experiment.CHRONOMATRON);

        handler.onSlotUpdate(board(
                slot(49, false, "clock", "", 1),
                slot(12, true, "red_terracotta", "", 1)), DEFAULT);
        assertEquals(OptionalInt.of(12), handler.nextClick());

        handler.onSlotUpdate(board(
                slot(49, false, "glowstone", "", 1),
                slot(12, false, "red_terracotta", "", 1)), DEFAULT);

        handler.onSlotUpdate(board(
                slot(49, false, "clock", "", 1),
                slot(14, true, "blue_terracotta", "", 1)), DEFAULT);

        assertEquals(OptionalInt.of(12), handler.nextClick());
        assertEquals(OptionalInt.of(14), handler.nextClick());
        assertTrue(handler.nextClick().isEmpty());
    }

    @Test
    void chronomatronClosesAfterSerumAdjustedLength() {
        AutoExperimentsPolicy.Handler handler =
                AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.Experiment.CHRONOMATRON);
        AutoExperimentsPolicy.Options options = new AutoExperimentsPolicy.Options(false, 0);

        for (int round = 0; round < 12; round++) {
            handler.onSlotUpdate(board(
                    slot(49, false, "clock", "", 1),
                    slot(10 + round, true, "red_terracotta", "", 1)), options);
            while (handler.nextClick().isPresent()) {
                // drain the replay, ticking through the whole order
            }
            handler.onSlotUpdate(board(
                    slot(49, false, "glowstone", "", 1),
                    slot(10 + round, false, "red_terracotta", "", 1)), options);
        }

        assertTrue(handler.shouldClose(true, options));
        assertFalse(handler.shouldClose(false, options));
    }

    @Test
    void ultrasequencerClicksCountOrderAfterClockAppears() {
        AutoExperimentsPolicy.Handler handler =
                AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.Experiment.ULTRASEQUENCER);

        handler.onSlotUpdate(board(
                slot(49, false, "glowstone", "", 1),
                slot(20, false, "red_dye", "3", 3),
                slot(21, false, "red_dye", "1", 1),
                slot(22, false, "red_dye", "2", 2)), DEFAULT);
        assertTrue(handler.nextClick().isEmpty(), "waits until the clock, when hasData is cleared");

        handler.onSlotUpdate(board(
                slot(49, false, "clock", "", 1),
                slot(20, false, "red_dye", "3", 3),
                slot(21, false, "red_dye", "1", 1),
                slot(22, false, "red_dye", "2", 2)), DEFAULT);

        assertEquals(OptionalInt.of(21), handler.nextClick());
        assertEquals(OptionalInt.of(22), handler.nextClick());
        assertEquals(OptionalInt.of(20), handler.nextClick());
        assertTrue(handler.nextClick().isEmpty());
    }

    @Test
    void ultrasequencerClosesWhenSequenceBeatsSerumCap() {
        AutoExperimentsPolicy.Handler handler =
                AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.Experiment.ULTRASEQUENCER);
        List<AutoExperimentsPolicy.SlotView> extras = new ArrayList<>();
        for (int n = 1; n <= 10; n++) {
            extras.add(slot(9 + n, false, "red_dye", Integer.toString(n), n));
        }
        handler.onSlotUpdate(board(
                slot(49, false, "glowstone", "", 1),
                extras.toArray(AutoExperimentsPolicy.SlotView[]::new)), DEFAULT);
        assertTrue(handler.shouldClose(true, DEFAULT));
        assertFalse(handler.shouldClose(false, DEFAULT));
    }

    @Test
    void delayStaysInsideConfiguredBounds() {
        Random random = new Random(1L);
        for (int i = 0; i < 40; i++) {
            long delay = AutoExperimentsPolicy.delay(200, 50, random);
            assertTrue(delay >= 200 && delay <= 250, "delay=" + delay);
        }
        assertEquals(100, AutoExperimentsPolicy.clampClickDelay(0));
        assertEquals(1000, AutoExperimentsPolicy.clampClickDelay(5000));
        assertEquals(3, AutoExperimentsPolicy.clampSerumCount(99));
    }

    @Test
    void stripControlCodesRemovesFormatting() {
        assertEquals("12", AutoExperimentsPolicy.stripControlCodes("§a12"));
        assertEquals("3", AutoExperimentsPolicy.stripControlCodes("3"));
        assertEquals("", AutoExperimentsPolicy.stripControlCodes(null));
    }
}
