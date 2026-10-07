package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static fi.rotclient.DungeonPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonTerminalClickPolicyTest {
    @Test
    void cloneSettingCannotReverseARubixRightClick() {
        assertEquals(new DungeonTerminalClickPolicy.PacketClick(1, false),
                DungeonTerminalClickPolicy.packetClick(1, true));
        assertEquals(new DungeonTerminalClickPolicy.PacketClick(2, true),
                DungeonTerminalClickPolicy.packetClick(0, true));
        assertEquals(new DungeonTerminalClickPolicy.PacketClick(0, false),
                DungeonTerminalClickPolicy.packetClick(0, false));
    }

    @Test
    void identicalTitlesInDifferentMenusCannotShareQueuedClicks() {
        Object first = new Object();
        Object next = new Object();
        assertTrue(DungeonTerminalClickPolicy.sameSession(first, "Click in order!", first, "Click in order!"));
        assertFalse(DungeonTerminalClickPolicy.sameSession(first, "Click in order!", next, "Click in order!"));
        assertFalse(DungeonTerminalClickPolicy.sameSession(first, "Click in order!", first, "SkyBlock Menu"));
        assertFalse(DungeonTerminalClickPolicy.sameSession(null, "Click in order!", first, "Click in order!"));
    }

    @Test
    void numericQueueOnlyAcceptsNextUnfinishedNumber() {
        List<TerminalClick> live = List.of(new TerminalClick(12, 0), new TerminalClick(13, 0));
        assertTrue(can(Terminal.NUMBERS, 12, 0, live, 3));
        assertFalse(can(Terminal.NUMBERS, 13, 0, live, 3));
        assertFalse(can(Terminal.NUMBERS, 54, 0, live, 3));
        assertFalse(can(Terminal.NUMBERS, 12, 0, List.of(), 3));
    }

    @Test
    void wrongRubixDirectionAndStaleSolvedSlotsCannotBeQueued() {
        List<TerminalClick> live = List.of(new TerminalClick(12, 1), new TerminalClick(13, 0));
        assertTrue(can(Terminal.RUBIX, 12, 1, live, 3));
        assertFalse(can(Terminal.RUBIX, 12, 0, live, 3));
        assertTrue(can(Terminal.RUBIX, 13, 2, live, 3));
        assertFalse(can(Terminal.RUBIX, 14, 0, live, 3));
        assertFalse(can(Terminal.NONE, 12, 0, live, 3));
    }

    @Test
    void melodySkipCannotQueueRemovedRowsOrInventorySlots() {
        assertTrue(can(Terminal.MELODY, 34, 0, List.of(), 3));
        assertFalse(can(Terminal.MELODY, 43, 0, List.of(), 3));
        assertTrue(can(Terminal.MELODY, 43, 0, List.of(), 4));
        assertFalse(can(Terminal.MELODY, 52, 0, List.of(), 4));
        assertFalse(can(Terminal.MELODY, 32, 0, List.of(), 3));
        assertFalse(can(Terminal.MELODY, 34, 8, List.of(), 3));
    }

    private static boolean can(Terminal type, int slot, int button, List<TerminalClick> live, int rows) {
        return DungeonTerminalClickPolicy.canQueueClick(type, slot, button, live, rows);
    }
}
