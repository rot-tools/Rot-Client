package fi.rotclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static fi.rotclient.DungeonPolicy.*;

/**
 * Plus-only, bounded state for one terminal menu. Goal locking and clicked-slot
 * acknowledgement are adapted from Odin RubixHandler / StartsWithHandler,
 * Copyright (c) 2025, odtheking (BSD-3-Clause), revision 833e0533ef9c47529b790612627a65618ebd5a58.
 * Full copyright, conditions and disclaimer: docs/third-party/Odin-LICENSE.txt.
 * Rot additionally checks menu identity and fresh 15-bit container state IDs.
 */
final class DungeonTerminalSessionPolicy {
    private record PendingStart(TerminalItem original, int stateId, long sentAt) { }
    private record AcknowledgedStart(TerminalItem original, int stateId) { }

    private Object menu;
    private boolean enabled = true;
    private String title = "";
    private int rubixGoal = -1;
    private final Map<Integer, PendingStart> startsPending = new HashMap<>();
    private final Map<Integer, AcknowledgedStart> startsAcknowledged = new HashMap<>();

    void bind(Object currentMenu, String currentTitle) {
        if (!enabled) {
            reset();
            return;
        }
        String normalized = normalize(currentTitle);
        if (menu != currentMenu || !title.equals(normalized)) {
            reset();
            menu = currentMenu;
            title = normalized;
        }
    }

    void enabled(boolean active) {
        if (enabled != active) reset();
        enabled = active;
    }

    void reset() {
        menu = null;
        title = "";
        rubixGoal = -1;
        startsPending.clear();
        startsAcknowledged.clear();
    }

    List<TerminalClick> solve(Terminal terminal, String currentTitle, List<TerminalItem> items,
                              boolean leftOnly, long now, int timeoutMs) {
        if (menu == null || !title.equals(normalize(currentTitle))
                || !DungeonTerminalSolverPolicy.supportsTitle(terminal, currentTitle)) return List.of();
        if (terminal == Terminal.RUBIX) {
            // Partial packets must not choose a goal from an unfinished board.
            int candidate = DungeonTerminalSolverPolicy.rubixGoal(items, leftOnly, true);
            if (candidate < 0) return List.of();
            if (rubixGoal < 0) rubixGoal = candidate;
            return DungeonTerminalSolverPolicy.solveRubixWithGoal(items, rubixGoal, leftOnly);
        }
        List<TerminalClick> live = DungeonTerminalSolverPolicy.solveClicks(terminal, currentTitle, items);
        if (terminal != Terminal.STARTS_WITH) return live;
        int timeout = DungeonAthenPortPolicy.clampResyncMs(timeoutMs);
        startsPending.entrySet().removeIf(entry -> now < entry.getValue().sentAt()
                || now - entry.getValue().sentAt() >= timeout);
        return live.stream().filter(click -> !startsAcknowledged.containsKey(click.slot())
                && !startsPending.containsKey(click.slot())).toList();
    }

    void clickSent(Object currentMenu, String currentTitle, int slot, int stateId,
                   List<TerminalItem> items, long now) {
        if (!matches(currentMenu, currentTitle) || startsAcknowledged.containsKey(slot)
                || startsPending.containsKey(slot)) return;
        boolean valid = DungeonTerminalSolverPolicy.solveClicks(Terminal.STARTS_WITH, currentTitle, items)
                .stream().anyMatch(click -> click.slot() == slot);
        if (!valid) return;
        for (TerminalItem item : items) {
            if (item.index() == slot) {
                startsPending.put(slot, new PendingStart(item, stateId, now));
                return;
            }
        }
    }

    /** Only actual, applied server packet callbacks may call this method. */
    boolean serverSlot(Object currentMenu, String currentTitle, int stateId, TerminalItem observed) {
        if (!matches(currentMenu, currentTitle) || observed == null) return false;
        int slot = observed.index();
        PendingStart pending = startsPending.get(slot);
        if (pending != null) {
            // Resends / older corrections are not confirmation of a click.
            if (!stateAdvanced(pending.stateId(), stateId)) return false;
            startsPending.remove(slot);
            if (sameItem(pending.original(), observed) && observed.enchanted()) {
                startsAcknowledged.put(slot, new AcknowledgedStart(pending.original(), stateId));
            }
            return true;
        }
        AcknowledgedStart acknowledged = startsAcknowledged.get(slot);
        if (acknowledged == null || !stateAdvanced(acknowledged.stateId(), stateId)) return false;
        if (!sameItem(acknowledged.original(), observed) || !observed.enchanted()) {
            startsAcknowledged.remove(slot);
        } else {
            startsAcknowledged.put(slot, new AcknowledgedStart(acknowledged.original(), stateId));
        }
        return true;
    }

    /** The local simulator has explicit accepted-click evidence, without packets. */
    void simulatorAccepted(Object currentMenu, String currentTitle, int slot,
                           List<TerminalItem> before) {
        if (!matches(currentMenu, currentTitle)) return;
        for (TerminalItem item : before) {
            if (item.index() == slot) {
                startsPending.remove(slot);
                startsAcknowledged.put(slot, new AcknowledgedStart(item, -1));
                return;
            }
        }
    }

    private boolean matches(Object currentMenu, String currentTitle) {
        return menu != null && menu == currentMenu && title.equals(normalize(currentTitle))
                && DungeonTerminalSolverPolicy.supportsTitle(Terminal.STARTS_WITH, currentTitle);
    }

    /** Vanilla container IDs wrap at 32768; the ambiguous half-cycle is rejected. */
    static boolean stateAdvanced(int previous, int current) {
        if (previous < 0 || previous > 0x7FFF || current < 0 || current > 0x7FFF) return false;
        int distance = (current - previous) & 0x7FFF;
        return distance > 0 && distance < 0x4000;
    }

    private static boolean sameItem(TerminalItem first, TerminalItem second) {
        return Objects.equals(first.itemId(), second.itemId())
                && normalize(first.name()).equals(normalize(second.name())) && second.count() > 0;
    }

    int lockedRubixGoal() { return rubixGoal; }
    boolean hasPendingStarts() { return !startsPending.isEmpty(); }
}
