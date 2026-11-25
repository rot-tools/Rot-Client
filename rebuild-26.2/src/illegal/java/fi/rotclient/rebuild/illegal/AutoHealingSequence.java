package fi.rotclient.rebuild.illegal;

/** Tick-based selection, one use, and restoration. */
final class AutoHealingSequence {
    interface Hotbar {
        int selectedSlot();
        void selectSlot(int slot);
        boolean ready(int slot);
        void use();
    }

    private enum Stage { SELECT, USE, RETURN }
    private Pending pending;
    private boolean manuallyCancelled;

    boolean active() { return pending != null; }

    void start(long tick, int selectionDelay, int useDelay, int returnDelay, int slot, Hotbar hotbar) {
        if (!hotbar.ready(slot)) return;
        pending = new Pending(hotbar.selectedSlot(), slot, Math.clamp(useDelay, 1, 10),
                Math.clamp(returnDelay, 1, 10), tick + Math.clamp(selectionDelay, 1, 10), Stage.SELECT);
    }

    void advance(long tick, boolean lowHealth, Hotbar hotbar) {
        var current = pending;
        if (current == null) return;
        int expectedSlot = current.stage() == Stage.SELECT ? current.original() : current.slot();
        if (hotbar.selectedSlot() != expectedSlot) {
            manualChange();
            return;
        }
        if (current.stage() != Stage.RETURN && (!lowHealth || !hotbar.ready(current.slot()))) {
            stop(hotbar);
            return;
        }
        if (tick < current.nextTick()) return;
        if (current.stage() == Stage.RETURN) {
            stop(hotbar);
            return;
        }
        try {
            if (current.stage() == Stage.SELECT) {
                pending = new Pending(current.original(), current.slot(), current.useDelay(), current.returnDelay(),
                        tick + current.useDelay(), Stage.USE);
                if (current.slot() != current.original()) hotbar.selectSlot(current.slot());
            } else {
                pending = new Pending(current.original(), current.slot(), current.useDelay(), current.returnDelay(),
                        tick + current.returnDelay(), Stage.RETURN);
                hotbar.use();
            }
        } catch (RuntimeException failure) {
            stop(hotbar);
            throw failure;
        }
    }

    void manualChange() {
        if (pending == null) return;
        pending = null;
        manuallyCancelled = true;
    }

    boolean canStart(boolean lowHealth) {
        if (!lowHealth) manuallyCancelled = false;
        return lowHealth && !manuallyCancelled;
    }

    void stop(Hotbar hotbar) {
        var current = pending;
        pending = null;
        if (current != null && current.stage() != Stage.SELECT && current.original() != current.slot()
                && hotbar.selectedSlot() == current.slot()) hotbar.selectSlot(current.original());
    }

    void reset() {
        pending = null;
        manuallyCancelled = false;
    }

    private record Pending(int original, int slot, int useDelay, int returnDelay, long nextTick, Stage stage) {}
}
