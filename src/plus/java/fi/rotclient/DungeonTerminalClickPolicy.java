package fi.rotclient;

import java.util.List;

/** Plus-only terminal click guards; unrelated chest/player slots stay vanilla. */
final class DungeonTerminalClickPolicy {
    record PacketClick(int button, boolean cloneInput) { }

    private DungeonTerminalClickPolicy() { }

    static boolean sameSession(Object previousMenu, String previousTitle,
                               Object currentMenu, String currentTitle) {
        return previousMenu != null && previousMenu == currentMenu
                && previousTitle != null && previousTitle.equals(currentTitle);
    }

    static PacketClick packetClick(int button, boolean cloneEnabled) {
        // A reversed Rubix cycle must remain PICKUP/right, even with Clone on.
        return button == 1 ? new PacketClick(1, false)
                : cloneEnabled ? new PacketClick(2, true) : new PacketClick(0, false);
    }

    static boolean canQueueClick(DungeonPolicy.Terminal terminal, int slot, int button,
                                 List<DungeonPolicy.TerminalClick> live, int melodyRows) {
        if (terminal == null || terminal == DungeonPolicy.Terminal.NONE
                || slot < 0 || button < 0 || button > 2 || live == null) return false;
        if (terminal == DungeonPolicy.Terminal.MELODY) {
            // Preserve explicitly enabled Melody Skip, bounded to observed rows.
            return slot % 9 == 7 && slot / 9 >= 1 && slot / 9 <= melodyRows;
        }
        if (terminal == DungeonPolicy.Terminal.NUMBERS) {
            return !live.isEmpty() && live.getFirst().slot() == slot;
        }
        for (DungeonPolicy.TerminalClick click : live) {
            if (click.slot() == slot && (terminal != DungeonPolicy.Terminal.RUBIX
                    || click.button() == (button == 2 ? 0 : button))) return true;
        }
        return false;
    }
}
