package fi.rotclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Auto Sell: shift/middle/left click matching dungeon drops in the player
 * inventory of Trades and Booster Cookie chests. Names are Hypixel hover-name
 * substrings; the Serveri uses the same item names.
 */
public final class AutoSellPolicy {
    public static final int MIN_DELAY = 2;
    public static final int MAX_DELAY = 10;
    public static final int DEFAULT_DELAY = 6;
    public static final int MIN_RANDOMIZATION = 0;
    public static final int MAX_RANDOMIZATION = 5;
    public static final int DEFAULT_RANDOMIZATION = 1;
    public static final String CLICK_SHIFT = "Shift";
    public static final String CLICK_MIDDLE = "Middle";
    public static final String CLICK_LEFT = "Left";
    public static final List<String> CLICK_TYPES = List.of(CLICK_SHIFT, CLICK_MIDDLE, CLICK_LEFT);
    public static final String BLACKLIST_NAME = "skeleton master chestplate";

    public static final List<String> DEFAULT_ITEMS = List.of(
            "enchanted ice",
            "superboom tnt",
            "rotten",
            "skeleton master",
            "skeleton grunt",
            "cutlass",
            "skeleton lord",
            "skeleton soldier",
            "zombie soldier",
            "zombie knight",
            "zombie commander",
            "zombie lord",
            "skeletor",
            "super heavy",
            "heavy",
            "sniper helmet",
            "dreadlord",
            "earth shard",
            "zombie commander whip",
            "machine gun",
            "sniper bow",
            "soulstealer bow",
            "silent death",
            "training weight",
            "beating heart",
            "premium flesh",
            "mimic fragment",
            "enchanted rotten flesh",
            "sign",
            "enchanted bone",
            "defuse kit",
            "optical lens",
            "tripwire hook",
            "button",
            "carpet",
            "lever",
            "diamond atom",
            "healing viii splash potion",
            "healing 8 splash potion",
            "candycomb");

    public enum ClickKind {
        QUICK_MOVE,
        CLONE,
        PICKUP
    }

    public record SlotView(int index, String hoverName, boolean empty) {
        public SlotView {
            hoverName = hoverName == null ? "" : hoverName;
        }
    }

    private static final Pattern CONTROL = Pattern.compile("§.");

    private AutoSellPolicy() {
    }

    public static int clampDelay(int value) {
        return Math.max(MIN_DELAY, Math.min(MAX_DELAY, value));
    }

    public static int clampRandomization(int value) {
        return Math.max(MIN_RANDOMIZATION, Math.min(MAX_RANDOMIZATION, value));
    }

    public static String normalizeClickType(String value) {
        if (value == null) {
            return CLICK_SHIFT;
        }
        String text = value.trim();
        if (text.equalsIgnoreCase(CLICK_MIDDLE)) {
            return CLICK_MIDDLE;
        }
        if (text.equalsIgnoreCase(CLICK_LEFT)) {
            return CLICK_LEFT;
        }
        return CLICK_SHIFT;
    }

    public static ClickKind clickKind(String clickType) {
        return switch (normalizeClickType(clickType)) {
            case CLICK_MIDDLE -> ClickKind.CLONE;
            case CLICK_LEFT -> ClickKind.PICKUP;
            default -> ClickKind.QUICK_MOVE;
        };
    }

    public static boolean isSellMenu(String title) {
        String text = strip(title).toLowerCase(Locale.ROOT);
        return text.equals("trades") || text.equals("booster cookie");
    }

    /**
     * Wait in milliseconds, computed as {@code (delay + random(0..rand)) * 50}.
     */
    public static long waitMs(int delay, int randomization, double unitRandom) {
        int extra = 0;
        int span = clampRandomization(randomization);
        if (span > 0 && Double.isFinite(unitRandom)) {
            extra = (int) Math.round(Math.max(0.0D, Math.min(1.0D, unitRandom)) * span);
        }
        return (long) (clampDelay(delay) + extra) * 50L;
    }

    public static boolean delayElapsed(long nowMs, long lastMs, long waitMs) {
        return nowMs - lastMs >= waitMs;
    }

    public static OptionalInt nextSellSlot(List<SlotView> slots, Collection<String> sellList) {
        if (slots == null || slots.isEmpty() || sellList == null || sellList.isEmpty()) {
            return OptionalInt.empty();
        }
        Set<String> needles = normalizeList(sellList);
        int invStart = Math.max(0, slots.size() - 36);
        for (int i = invStart; i < slots.size(); i++) {
            SlotView slot = slots.get(i);
            if (slot == null || slot.empty()) {
                continue;
            }
            if (shouldSell(slot.hoverName(), needles)) {
                return OptionalInt.of(slot.index());
            }
        }
        return OptionalInt.empty();
    }

    public static boolean shouldSell(String hoverName, Collection<String> sellList) {
        String name = strip(hoverName).toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.contains(BLACKLIST_NAME)) {
            return false;
        }
        for (String needle : normalizeList(sellList)) {
            if (!needle.isEmpty() && name.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> parseList(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("[,\\n]")) {
            String needle = part.trim().toLowerCase(Locale.ROOT);
            if (!needle.isEmpty()) {
                out.add(needle);
            }
        }
        return out;
    }

    public static String formatList(Collection<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return String.join(", ", normalizeList(items));
    }

    public static Set<String> withDefaults(Collection<String> current) {
        Set<String> out = new LinkedHashSet<>(normalizeList(current));
        out.addAll(DEFAULT_ITEMS);
        return out;
    }

    public static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return CONTROL.matcher(raw).replaceAll("").trim();
    }

    private static Set<String> normalizeList(Collection<String> items) {
        Set<String> out = new LinkedHashSet<>();
        if (items == null) {
            return out;
        }
        for (String item : items) {
            if (item == null) {
                continue;
            }
            String needle = item.trim().toLowerCase(Locale.ROOT);
            if (!needle.isEmpty()) {
                out.add(needle);
            }
        }
        return out;
    }

    static List<String> defaultCopy() {
        return new ArrayList<>(DEFAULT_ITEMS);
    }
}
