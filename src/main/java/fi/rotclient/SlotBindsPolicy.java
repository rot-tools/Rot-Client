package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Pure inventory slot-bind rules: one hotbar slot linked to one inventory
 * slot, shift-swap, and when connector lines should draw.
 */
public final class SlotBindsPolicy {
    public static final int MIN_SLOT = 5;
    public static final int MAX_SLOT = 44;
    public static final int HOTBAR_MIN = 36;
    public static final int HOTBAR_MAX = 44;
    public static final float MIN_LINE_WIDTH = 0.1F;
    public static final float MAX_LINE_WIDTH = 4.0F;

    public enum LineDisplay {
        HOVER,
        HOVER_SHIFT,
        NONE
    }

    public enum BindAction {
        START,
        COMPLETE,
        REMOVE,
        REJECT_SAME,
        REJECT_NO_HOTBAR,
        IGNORE
    }

    public record BindChange(BindAction action, Integer pendingSlot, Integer from, Integer to) {
        public static BindChange start(int slot) {
            return new BindChange(BindAction.START, slot, null, null);
        }

        public static BindChange complete(int from, int to) {
            return new BindChange(BindAction.COMPLETE, null, from, to);
        }

        public static BindChange remove(int slot) {
            return new BindChange(BindAction.REMOVE, null, slot, null);
        }

        public static BindChange reject(BindAction action) {
            return new BindChange(action, null, null, null);
        }

        public static BindChange ignore() {
            return new BindChange(BindAction.IGNORE, null, null, null);
        }
    }

    private SlotBindsPolicy() {
    }

    public static boolean isBindableSlot(int index) {
        return index >= MIN_SLOT && index <= MAX_SLOT;
    }

    public static boolean isHotbarSlot(int index) {
        return index >= HOTBAR_MIN && index <= HOTBAR_MAX;
    }

    public static boolean canBind(int first, int second) {
        if (!isBindableSlot(first) || !isBindableSlot(second) || first == second) {
            return false;
        }
        return isHotbarSlot(first) ^ isHotbarSlot(second);
    }

    public static float clampLineWidth(float width) {
        if (!Float.isFinite(width)) {
            return 0.5F;
        }
        return Math.max(MIN_LINE_WIDTH, Math.min(MAX_LINE_WIDTH, width));
    }

    public static LineDisplay parseLineDisplay(String raw) {
        if (raw == null) {
            return LineDisplay.HOVER;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("none")) {
            return LineDisplay.NONE;
        }
        if (normalized.contains("shift")) {
            return LineDisplay.HOVER_SHIFT;
        }
        return LineDisplay.HOVER;
    }

    public static String normalizeProfile(String raw) {
        if (raw == null) {
            return "Profile 1";
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("Profile 2")) {
            return "Profile 2";
        }
        if (trimmed.equalsIgnoreCase("Profile 3")) {
            return "Profile 3";
        }
        return "Profile 1";
    }

    public static BindChange onBindKey(
            int hoveredSlot,
            Integer pendingSlot,
            Map<Integer, Integer> binds) {
        if (!isBindableSlot(hoveredSlot)) {
            return BindChange.ignore();
        }
        if (pendingSlot == null) {
            if (binds != null && (binds.containsKey(hoveredSlot) || binds.containsValue(hoveredSlot))) {
                return BindChange.remove(hoveredSlot);
            }
            return BindChange.start(hoveredSlot);
        }
        if (pendingSlot == hoveredSlot) {
            return BindChange.reject(BindAction.REJECT_SAME);
        }
        if (!canBind(pendingSlot, hoveredSlot)) {
            return BindChange.reject(BindAction.REJECT_NO_HOTBAR);
        }
        return BindChange.complete(pendingSlot, hoveredSlot);
    }

    public static Map<Integer, Integer> apply(Map<Integer, Integer> current, BindChange change) {
        Map<Integer, Integer> next = current == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(current);
        if (change == null) {
            return next;
        }
        if (change.action() == BindAction.COMPLETE) {
            next.put(change.from(), change.to());
        } else if (change.action() == BindAction.REMOVE) {
            int slot = change.from();
            next.remove(slot);
            next.entrySet().removeIf(entry -> entry.getValue() == slot);
        }
        return next;
    }

    public static OptionalInt boundPartner(Map<Integer, Integer> binds, int slot) {
        if (binds == null) {
            return OptionalInt.empty();
        }
        Integer direct = binds.get(slot);
        if (direct != null) {
            return OptionalInt.of(direct);
        }
        for (Map.Entry<Integer, Integer> entry : binds.entrySet()) {
            if (entry.getValue() == slot) {
                return OptionalInt.of(entry.getKey());
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Hotbar index 0-8 used as the SWAP button, targeting the non-hotbar slot.
     */
    public static OptionalInt swapInventorySlot(int clicked, int bound) {
        if (!canBind(clicked, bound)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(isHotbarSlot(clicked) ? bound : clicked);
    }

    public static OptionalInt swapHotbarButton(int clicked, int bound) {
        if (!canBind(clicked, bound)) {
            return OptionalInt.empty();
        }
        int hotbar = isHotbarSlot(clicked) ? clicked : bound;
        return OptionalInt.of(hotbar % 36);
    }

    public static boolean shouldDrawLines(
            LineDisplay display,
            boolean bindingInProgress,
            boolean hoveredHasBind,
            boolean shiftDown) {
        if (bindingInProgress) {
            return true;
        }
        return switch (display == null ? LineDisplay.HOVER : display) {
            case HOVER -> hoveredHasBind;
            case HOVER_SHIFT -> hoveredHasBind && shiftDown;
            case NONE -> false;
        };
    }
}
