package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonF7PolicyTest {
    @Test
    void crystalAndMelodyMatchHypixelChat() {
        assertTrue(DungeonF7Policy.crystalPickup("Henri picked up an Energy Crystal!"));
        assertTrue(DungeonF7Policy.crystalSpawnChat("[BOSS] Maxor: YOU TRICKED ME!"));
        assertTrue(DungeonF7Policy.crystalSpawnChat("[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!"));
        assertEquals(34, DungeonF7Policy.CRYSTAL_RESPAWN_TICKS);
        assertEquals(1_700L, DungeonF7Policy.CRYSTAL_RESPAWN_MILLIS);
        assertTrue(DungeonF7Policy.holdingEnergyCrystal("Energy Crystal"));
        assertTrue(DungeonF7Policy.melodyTerminalTitle("Click the button on time!"));
        assertTrue(DungeonF7Policy.melodyAlertChat("Party > Bob: melody"));
        assertTrue(DungeonF7Policy.melodyAlertChat("Henri is at the melody terminal"));
        assertFalse(DungeonF7Policy.melodyAlertChat("The melody of the wither"));
        assertEquals(DungeonF7Policy.WitherBoss.MAXOR, DungeonF7Policy.witherBoss("Maxor 300M❤"));
        assertEquals(DungeonF7Policy.WitherBoss.NECRON, DungeonF7Policy.witherBoss("Necron"));
    }

    @Test
    void dioritePillarsAndSimonButtons() {
        assertEquals(EmberDungeonPolicy.GlassTint.LIME,
                DungeonF7Policy.dioriteTint(46, 169, 41).orElseThrow());
        assertEquals(EmberDungeonPolicy.GlassTint.RED,
                DungeonF7Policy.dioriteTint(100, 180, 41).orElseThrow());
        assertTrue(DungeonF7Policy.dioriteTint(0, 0, 0).isEmpty());
        assertTrue(DungeonF7Policy.isSimonButton(110, 121, 93));
        assertFalse(DungeonF7Policy.isSimonButton(110, 121, 91));
        assertEquals(16, DungeonF7Policy.simonButtons().size());
        assertEquals(16, DungeonF7Policy.simonLanterns().size());
        assertTrue(DungeonF7Policy.isSimonLantern(111, 120, 92));
        assertFalse(DungeonF7Policy.isSimonLantern(110, 120, 92));
        assertEquals(new EmberDungeonPolicy.IntVec(110, 121, 93),
                DungeonF7Policy.simonButtonForLantern(111, 121, 93));
        assertTrue(DungeonF7Policy.isSimonSequenceLit("minecraft:sea_lantern"));
        assertFalse(DungeonF7Policy.isSimonSequenceLit("minecraft:obsidian"));
        assertEquals(0xFF22C55E, DungeonF7Policy.simonColor(0, 0xFF22C55E, 0xFFFACC15, 0xFF38BDF8));
        assertEquals(0xFFFACC15, DungeonF7Policy.simonColor(1, 0xFF22C55E, 0xFFFACC15, 0xFF38BDF8));
    }

    @Test
    void simonRecordsEachRoundInOrderIncludingRepeats() {
        EmberDungeonPolicy.IntVec a = new EmberDungeonPolicy.IntVec(110, 121, 93);
        EmberDungeonPolicy.IntVec b = new EmberDungeonPolicy.IntVec(110, 122, 94);
        DungeonF7Policy.SimonState state = DungeonF7Policy.SimonState.idle();
        state = DungeonF7Policy.observeSimon(state, List.of(a));
        state = DungeonF7Policy.observeSimon(state, List.of(a));
        state = DungeonF7Policy.observeSimon(state, List.of());
        assertEquals(List.of(a), state.order());
        assertEquals(a, state.nextButton());
        state = DungeonF7Policy.consumeNext(state);
        assertTrue(state.remaining().isEmpty());
        state = DungeonF7Policy.observeSimon(state, List.of(a));
        state = DungeonF7Policy.observeSimon(state, List.of(b));
        state = DungeonF7Policy.observeSimon(state, List.of());
        assertEquals(List.of(a, b), state.order());
        assertEquals(a, state.nextButton());
        state = DungeonF7Policy.consumeNext(state);
        assertEquals(b, state.nextButton());
        assertEquals(List.of(a, b), DungeonF7Policy.consumeNext(
                DungeonF7Policy.observeSimon(DungeonF7Policy.SimonState.idle(), List.of(a, b)))
                .order());
    }

    @Test
    void overlayColorsWrongClicksMelodySkipAndDragons() {
        assertEquals(0x8000FF00, DungeonF7Policy.numbersOverlayColor(0, 0x8000FF00, 0x8000C800, 0x80009600, 1));
        assertEquals(0x800072FF, DungeonF7Policy.rubixOverlayColor(0, 0x800072FF, 0x80CD0000, 1));
        assertTrue(DungeonF7Policy.overlayTypeEnabled(
                DungeonPolicy.Terminal.NUMBERS, true, true, true, true, true, true));
        assertFalse(DungeonF7Policy.overlayTypeEnabled(
                DungeonPolicy.Terminal.NUMBERS, true, false, true, true, true, true));
        List<EmberDungeonPolicy.ArrowClicks> remaining = List.of(new EmberDungeonPolicy.ArrowClicks(0, 1));
        assertTrue(DungeonF7Policy.blockWrongArrow(-2, 120, 79, true, false, false, remaining));
        assertFalse(DungeonF7Policy.blockWrongArrow(-2, 120, 75, true, false, false, remaining));
        EmberDungeonPolicy.IntVec next = new EmberDungeonPolicy.IntVec(110, 121, 93);
        assertTrue(DungeonF7Policy.blockWrongSimon(110, 121, 94, true, false, next));
        assertFalse(DungeonF7Policy.blockWrongSimon(110, 121, 93, true, false, next));
        assertFalse(DungeonF7Policy.blockWrongSimon(110, 121, 94, true, true, next));
    }

    @Test
    void melodySkipQueuesLowerRowsOnEdges() {
        DungeonPolicy.MelodyState ready = new DungeonPolicy.MelodyState(1, 0, 0);
        List<DungeonPolicy.TerminalClick> extra = DungeonPolicy.melodySkipClicks(ready, true, true, "Edges");
        assertEquals(List.of(
                new DungeonPolicy.TerminalClick(34, 0),
                new DungeonPolicy.TerminalClick(43, 0)), extra);
        assertEquals(List.of(new DungeonPolicy.TerminalClick(34, 0)),
                DungeonPolicy.melodySkipClicks(ready, true, true, "Edges", 3));
        assertTrue(DungeonPolicy.melodySkipClicks(ready, false, true, "Edges").isEmpty());
        assertTrue(DungeonPolicy.melodySkipClicks(
                new DungeonPolicy.MelodyState(0, 0, 0), true, false, "Edges").isEmpty());
        assertEquals(List.of(
                new DungeonPolicy.TerminalClick(25, 0),
                new DungeonPolicy.TerminalClick(34, 0),
                new DungeonPolicy.TerminalClick(43, 0)),
                DungeonPolicy.melodySkipClicks(
                        new DungeonPolicy.MelodyState(0, 4, 4), true, false, "Edges"));
    }

    @Test
    void stormDeathRelicLookAndDragonPads() {
        assertTrue(DungeonF7Policy.stormDeath(
                "[BOSS] Storm: I should have known that I stood no chance."));
        assertEquals("Henri", DungeonF7Policy.melodyPlayer("Party > Henri: Melody 2/3").orElseThrow());
        assertEquals("Henri", DungeonF7Policy.melodyPlayer("Party > Henri: Melody 2/4").orElseThrow());
        assertTrue(DungeonF7Policy.holdingRelic("Corrupted Red Relic", "Red"));
        assertEquals(5, DungeonF7Policy.dragonPads().size());
        assertTrue(DungeonF7Policy.dragonHealthLine("Ice Dragon", 12_500_000F).contains("M"));
        assertTrue(EmberDungeonPolicy.isOnI4Device(66.5, 128, 50));
        assertFalse(EmberDungeonPolicy.isOnI4Device(0, 64, 0));
        assertEquals(DungeonPolicy.DungeonClass.TANK, DungeonF7Policy.leapClass("Tank"));
        assertTrue(DungeonF7Policy.i4Action(174, DungeonF7Policy.I4_ROD_TICK));
    }

    @Test
    void p3HudMelodySlotsProtectAndTimers() {
        var progress = DungeonF7Policy.p3Progress(
                "Henri completed a terminal! (3/7)").orElseThrow();
        assertEquals("terminal", progress.kind());
        assertEquals(3, progress.current());
        assertTrue(DungeonF7Policy.p3GateDestroyed("The gate has been destroyed!"));
        assertEquals("P3  T 3/7  D 1/7  L 0/7", DungeonF7Policy.p3HudLine(3, 1, 0));
        assertEquals(16, DungeonF7Policy.melodySlotForDigit(1));
        assertEquals(34, DungeonF7Policy.melodySlotForDigit(3));
        assertEquals(43, DungeonF7Policy.melodySlotForDigit(4));
        assertEquals(-1, DungeonF7Policy.melodySlotForDigit(4, 3));
        assertTrue(DungeonF7Policy.protectTerminal(1_000L, 1_200L, 400));
        assertFalse(DungeonF7Policy.protectTerminal(1_000L, 1_500L, 400));
        assertEquals("Any Key", DungeonF7Policy.normalizeCloseChestMode("any key"));
        assertTrue(DungeonF7Policy.dragonSpawnChat(
                "[BOSS] Wither King: I will now summon my dragons!"));
        assertEquals("Goldor frenzy 60t", DungeonF7Policy.goldorFrenzyLine(60));
        assertEquals("Purple pad 20t", DungeonF7Policy.purplePadLine(20));
        assertEquals("Dragon spawn 5.0s", DungeonF7Policy.dragonSpawnLine(5_000L, 0L));
        assertEquals("167t", DungeonF7Policy.formatCountdown("Maxor", 8_350L, true, true, false));
        assertEquals("Maxor 8.0s", DungeonF7Policy.formatCountdown("Maxor", 8_000L, false, true, true));
        assertEquals(840, DungeonF7Policy.clampRelicSpawnTicks(840));
        assertEquals(42_000L, DungeonF7Policy.relicSpawnMillis(840));
        assertEquals("Healer", DungeonF7Policy.normalizeSoloClass("healer"));
        assertEquals(List.of("Ice", "Soul", "Power", "Flame", "Apex"),
                DungeonF7Policy.dragonFocusOrder(true));
        assertEquals("Dragons Power > Flame > Apex > Ice > Soul  Solo Tank",
                DungeonF7Policy.dragonPriorityLine(false, Set.of(), "Tank"));
        assertEquals("Dragons Power > Flame > Apex  Solo Healer",
                DungeonF7Policy.dragonPriorityLine(false, Set.of("ice", "soul"), "Healer"));
        assertEquals("Ice", DungeonF7Policy.dragonPadName("Ice Dragon 12M").orElseThrow());
        assertTrue(DungeonF7Policy.dragonKillChat("The Ice Dragon was slain!"));
        assertFalse(DungeonF7Policy.dragonKillChat("[BOSS] Wither King: I will now summon my dragons!"));
        assertTrue(DungeonF7Policy.slotInSolution(
                List.of(new DungeonPolicy.TerminalClick(10, 0)), 10));
        assertTrue(DungeonF7Policy.shouldBlockWrongTerminalSlot(true, false, true, false));
        assertFalse(DungeonF7Policy.shouldBlockWrongTerminalSlot(true, true, true, false));
        assertTrue(DungeonF7Policy.shouldHideClickedSlot(true, true, false, false));
        assertFalse(DungeonF7Policy.shouldHideClickedSlot(true, true, true, false));
        assertFalse(DungeonF7Policy.shouldHideClickedSlot(true, true, false, true));
    }

    @Test
    void timerAndTitleFilters() {
        assertEquals(DungeonAssistPolicy.F7Timer.MAXOR_START,
                DungeonAssistPolicy.f7TimerFromChat("[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!"));
        assertTrue(DungeonF7Policy.timerAllowed(
                DungeonAssistPolicy.F7Timer.MAXOR_START, true, true, true, true, true, true, false));
        assertFalse(DungeonF7Policy.timerAllowed(
                DungeonAssistPolicy.F7Timer.MAXOR_START, true, true, true, true, true, false, true));
        assertTrue(DungeonF7Policy.titleAllowed(
                DungeonAssistPolicy.F7Title.CRYSTAL, true, true, true, true));
        assertFalse(DungeonF7Policy.titleAllowed(
                DungeonAssistPolicy.F7Title.GATE, true, true, true, false));
    }
}
