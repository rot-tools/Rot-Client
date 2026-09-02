package fi.rotclient;

/**
 * Documents Hard Stone quantity signal availability for targeted live tests.
 * Block evidence alone is never treated as authoritative quantity.
 */
final class HardStoneQuantitySignals {
    enum Signal {
        NONE,
        SACK_MESSAGE,
        ACTION_BAR,
        INVENTORY_DELTA
    }

    record Assessment(
            boolean blockEvidencePresent,
            Signal quantitySignal,
            boolean fortuneSafe,
            String liveTestHint) {
    }

    private HardStoneQuantitySignals() {
    }

    static Assessment assess(
            boolean blockEvidencePresent,
            boolean sackQuantityPresent,
            boolean actionBarQuantityPresent,
            boolean inventoryDeltaPresent) {
        Signal signal = Signal.NONE;
        if (sackQuantityPresent) {
            signal = Signal.SACK_MESSAGE;
        } else if (actionBarQuantityPresent) {
            signal = Signal.ACTION_BAR;
        } else if (inventoryDeltaPresent) {
            signal = Signal.INVENTORY_DELTA;
        }
        boolean fortuneSafe = signal != Signal.NONE;
        String hint;
        if (!blockEvidencePresent && signal == Signal.NONE) {
            hint = "No Hard Stone block evidence and no quantity signal.";
        } else if (blockEvidencePresent && signal == Signal.NONE) {
            hint = "Hard Stone block seen but quantity unresolved — do not "
                    + "fabricate 1 block = 1 Hard Stone.";
        } else if (signal == Signal.SACK_MESSAGE) {
            hint = "Prefer Sack quantity when present.";
        } else if (signal == Signal.ACTION_BAR) {
            hint = "Action-bar +N Hard Stone available as quantity signal.";
        } else {
            hint = "Inventory delta available; correlate with mining evidence.";
        }
        return new Assessment(
                blockEvidencePresent,
                signal,
                fortuneSafe,
                hint);
    }
}
