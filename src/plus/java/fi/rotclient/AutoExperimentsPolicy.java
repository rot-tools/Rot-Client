package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Auto Experiments state machines.
 *
 * <p>Two handlers are driven from the container's slot-update packets, clicking
 * whatever {@link Handler#nextClick()} hands back. The behavior includes two
 * deliberate quirks: Ultrasequencer advances its click counter even when the
 * recorded order has no entry for it, and Chronomatron restarts its click counter
 * every time a new flash is recorded. The Minecraft-facing side lives in
 * {@code AutoExperimentsRuntime}.
 */
public final class AutoExperimentsPolicy {
    /** The phase indicator is read from the center of the bottom row. */
    public static final int CENTER_SLOT = 49;
    public static final int MIN_CLICK_DELAY = 100;
    public static final int MAX_CLICK_DELAY = 1000;
    public static final int MIN_DELAY_VARIETY = 0;
    public static final int MAX_DELAY_VARIETY = 1000;
    public static final int MIN_SERUM = 0;
    public static final int MAX_SERUM = 3;

    private static final String GLOWSTONE = "glowstone";
    private static final String CLOCK = "clock";
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    public enum Experiment {
        CHRONOMATRON,
        ULTRASEQUENCER
    }

    /**
     * One container slot.
     *
     * @param index    the slot's own index, used both for range checks and as the
     *                 click target
     * @param glint    true when the stack carries an enchantment glint override,
     *                 which is how Hypixel marks a lit Chronomatron button
     * @param itemPath registry path, only ever compared against glowstone and clock
     * @param name     hover name, still carrying section-sign color codes
     * @param count    stack size, the Ultrasequencer ordering key
     */
    public record SlotView(int index, boolean glint, String itemPath, String name, int count) {
        public SlotView {
            itemPath = itemPath == null ? "" : itemPath;
            name = name == null ? "" : name;
        }
    }

    /** The two settings the handlers read while deciding when a run is done. */
    public record Options(boolean getMaxXp, int serumCount) {
        public Options {
            serumCount = clampSerumCount(serumCount);
        }
    }

    private AutoExperimentsPolicy() {
    }

    /** The experiment is matched purely on the screen title prefix. */
    public static Experiment forTitle(String title) {
        if (title == null) {
            return null;
        }
        if (title.startsWith("Chronomatron (")) {
            return Experiment.CHRONOMATRON;
        }
        if (title.startsWith("Ultrasequencer (")) {
            return Experiment.ULTRASEQUENCER;
        }
        return null;
    }

    public static Handler handlerFor(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return switch (experiment) {
            case CHRONOMATRON -> new ChronomatronHandler();
            case ULTRASEQUENCER -> new UltrasequencerHandler();
        };
    }

    /** {@code clickDelay + (0..delayVariety).random()}. */
    public static long delay(int clickDelay, int delayVariety, Random random) {
        return clampClickDelay(clickDelay) + random.nextInt(clampDelayVariety(delayVariety) + 1);
    }

    public static int clampClickDelay(int value) {
        return Math.max(MIN_CLICK_DELAY, Math.min(MAX_CLICK_DELAY, value));
    }

    public static int clampDelayVariety(int value) {
        return Math.max(MIN_DELAY_VARIETY, Math.min(MAX_DELAY_VARIETY, value));
    }

    public static int clampSerumCount(int value) {
        return Math.max(MIN_SERUM, Math.min(MAX_SERUM, value));
    }

    /**
     * Drop every section sign together with the character that follows it, whatever
     * that character is.
     */
    public static String stripControlCodes(String text) {
        if (text == null) {
            return "";
        }
        if (text.indexOf('\u00a7') == -1) {
            return text;
        }
        char[] out = new char[text.length()];
        int outPos = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\u00a7') {
                i += 2;
            } else {
                out[outPos++] = c;
                i++;
            }
        }
        return new String(out, 0, outPos);
    }

    public abstract static class Handler {
        private int clicks;
        private boolean hasData;

        protected int clicks() {
            return clicks;
        }

        protected void clicks(int value) {
            clicks = value;
        }

        protected boolean hasData() {
            return hasData;
        }

        protected void hasData(boolean value) {
            hasData = value;
        }

        public abstract void onSlotUpdate(List<SlotView> slots, Options options);

        public abstract OptionalInt nextClick();

        public abstract boolean shouldClose(boolean autoClose, Options options);
    }

    static final class ChronomatronHandler extends Handler {
        private final List<Integer> order = new ArrayList<>();
        private int lastAddedSlot = -1;
        private boolean close;

        @Override
        public void onSlotUpdate(List<SlotView> slots, Options options) {
            SlotView center = at(slots, CENTER_SLOT);
            if (center == null) {
                return;
            }
            if (lastAddedSlot != -1 && GLOWSTONE.equals(center.itemPath())) {
                SlotView last = at(slots, lastAddedSlot);
                if (last != null && !last.glint()) {
                    close = order.size() > (options.getMaxXp() ? 15 : 11 - options.serumCount());
                    hasData(false);
                    return;
                }
            }
            if (hasData() || !CLOCK.equals(center.itemPath())) {
                return;
            }
            for (SlotView slot : slots) {
                if (slot.index() >= 10 && slot.index() < 44 && slot.glint()) {
                    order.add(slot.index());
                    lastAddedSlot = slot.index();
                    hasData(true);
                    clicks(0);
                    return;
                }
            }
        }

        @Override
        public OptionalInt nextClick() {
            if (hasData() && clicks() < order.size()) {
                int at = clicks();
                clicks(at + 1);
                return OptionalInt.of(order.get(at));
            }
            return OptionalInt.empty();
        }

        @Override
        public boolean shouldClose(boolean autoClose, Options options) {
            if (!autoClose || !close) {
                return false;
            }
            if (clicks() < order.size()) {
                return false;
            }
            close = false;
            return true;
        }

        List<Integer> order() {
            return List.copyOf(order);
        }
    }

    static final class UltrasequencerHandler extends Handler {
        private final Map<Integer, Integer> order = new ConcurrentHashMap<>();

        @Override
        public void onSlotUpdate(List<SlotView> slots, Options options) {
            SlotView center = at(slots, CENTER_SLOT);
            if (center == null) {
                return;
            }
            if (CLOCK.equals(center.itemPath())) {
                hasData(false);
                return;
            }
            if (hasData() || !GLOWSTONE.equals(center.itemPath())) {
                return;
            }
            order.clear();
            for (SlotView slot : slots) {
                if (slot.index() < 9 || slot.index() >= 45) {
                    continue;
                }
                if (!DIGITS.matcher(stripControlCodes(slot.name())).matches()) {
                    continue;
                }
                order.put(slot.count() - 1, slot.index());
            }
            hasData(true);
            clicks(0);
        }

        @Override
        public OptionalInt nextClick() {
            if (hasData()) {
                return OptionalInt.empty();
            }
            int at = clicks();
            clicks(at + 1);
            Integer slot = order.get(at);
            return slot == null ? OptionalInt.empty() : OptionalInt.of(slot);
        }

        @Override
        public boolean shouldClose(boolean autoClose, Options options) {
            return autoClose
                    && order.size() > (options.getMaxXp() ? 20 : 9 - options.serumCount());
        }

        Map<Integer, Integer> order() {
            return Map.copyOf(order);
        }
    }

    /**
     * The menu list is indexed by slot number ({@code slots.get(49)}). Chest
     * menus store slots in index order, but looking up by {@link SlotView#index()}
     * keeps the same meaning if the snapshot is ever sparse.
     */
    private static SlotView at(List<SlotView> slots, int index) {
        if (slots == null || index < 0) {
            return null;
        }
        if (index < slots.size()) {
            SlotView direct = slots.get(index);
            if (direct != null && direct.index() == index) {
                return direct;
            }
        }
        for (SlotView slot : slots) {
            if (slot.index() == index) {
                return slot;
            }
        }
        return null;
    }
}
