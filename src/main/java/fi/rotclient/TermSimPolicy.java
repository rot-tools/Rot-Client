package fi.rotclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Local F7 terminal simulator layouts and click rules. Minecraft-free so
 * Auto Terms can solve the same titles and item snapshots as a real chest.
 */
public final class TermSimPolicy {
    public enum Kind {
        HUB,
        PANES,
        NUMBERS,
        STARTS_WITH,
        SELECT_ALL,
        RUBIX,
        MELODY
    }

    public record Slot(int index, String itemId, String name, int count, boolean enchanted) {
        public Slot(int index, String itemId, String name) {
            this(index, itemId, name, 1, false);
        }
    }

    public record MelodyClock(
            int magentaColumn,
            int limeColumn,
            int currentRow,
            int limeDirection,
            int tickCounter) {
    }

    public record Layout(
            Kind kind,
            String title,
            int size,
            List<Slot> slots,
            String letter,
            String color,
            MelodyClock melody) {
        public Layout {
            slots = slots == null ? List.of() : List.copyOf(slots);
            letter = letter == null ? "" : letter;
            color = color == null ? "" : color;
        }

        public Slot slotAt(int index) {
            for (Slot slot : slots) {
                if (slot.index() == index) {
                    return slot;
                }
            }
            return null;
        }
    }

    public record ClickResult(Layout layout, boolean accepted, boolean complete) {
    }

    public static final Kind[] RANDOM_POOL = {
            Kind.PANES, Kind.RUBIX, Kind.NUMBERS, Kind.STARTS_WITH, Kind.SELECT_ALL
    };

    public static final int[] RUBIX_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    public static final String[] RUBIX_COLORS = {
            "orange_stained_glass_pane",
            "yellow_stained_glass_pane",
            "green_stained_glass_pane",
            "blue_stained_glass_pane",
            "red_stained_glass_pane"
    };

    public static final int HUB_RESET = 4;
    public static final int HUB_PANES = 10;
    public static final int HUB_RUBIX = 11;
    public static final int HUB_NUMBERS = 12;
    public static final int HUB_RANDOM = 13;
    public static final int HUB_STARTS = 14;
    public static final int HUB_SELECT = 15;
    public static final int HUB_MELODY = 16;

    private static final String BLACK = "black_stained_glass_pane";
    private static final String[] START_LETTERS = {"A", "B", "C", "D", "E", "G", "I", "S"};
    private static final String[][] START_ITEMS = {
            {"apple", "Apple"}, {"arrow", "Arrow"}, {"bread", "Bread"}, {"bone", "Bone"},
            {"coal", "Coal"}, {"dirt", "Dirt"}, {"emerald", "Emerald"}, {"egg", "Egg"},
            {"gold_ingot", "Gold Ingot"}, {"iron_ingot", "Iron Ingot"}, {"stick", "Stick"},
            {"string", "String"}, {"sand", "Sand"}, {"sugar", "Sugar"}
    };
    private static final String[] SELECT_COLORS = {
            "red", "blue", "green", "yellow", "orange", "pink", "purple", "white",
            "black", "brown", "cyan", "lime", "magenta", "gray", "silver"
    };

    private TermSimPolicy() {
    }

    public static String titleFor(Kind kind) {
        return switch (kind) {
            case HUB -> "Terminal Simulator";
            case PANES -> "Correct all the panes!";
            case NUMBERS -> "Click in order!";
            case STARTS_WITH -> "What starts with: 'A'?";
            case SELECT_ALL -> "Select all the red items!";
            case RUBIX -> "Change all to same color!";
            case MELODY -> "Click the button on time!";
        };
    }

    public static int sizeFor(Kind kind) {
        return switch (kind) {
            case HUB -> 27;
            case NUMBERS -> 36;
            case PANES, RUBIX, STARTS_WITH -> 45;
            case SELECT_ALL, MELODY -> 54;
        };
    }

    public static Kind hubChoice(int slot) {
        return switch (slot) {
            case HUB_PANES -> Kind.PANES;
            case HUB_RUBIX -> Kind.RUBIX;
            case HUB_NUMBERS -> Kind.NUMBERS;
            case HUB_RANDOM -> Kind.HUB;
            case HUB_STARTS -> Kind.STARTS_WITH;
            case HUB_SELECT -> Kind.SELECT_ALL;
            case HUB_MELODY -> Kind.MELODY;
            default -> null;
        };
    }

    public static Layout hub() {
        return hub(Map.of());
    }

    public static Layout hub(Map<Kind, Integer> pbs) {
        Slot[] slots = new Slot[27];
        for (int i = 0; i < 27; i++) {
            slots[i] = black(i);
        }
        slots[HUB_RESET] = new Slot(HUB_RESET, "black_dye", "Reset PBs!");
        slots[HUB_PANES] = new Slot(HUB_PANES, "lime_dye",
                DungeonLeftoverPolicy.hubSlotName(Kind.PANES, pbs));
        slots[HUB_RUBIX] = new Slot(HUB_RUBIX, "red_dye",
                DungeonLeftoverPolicy.hubSlotName(Kind.RUBIX, pbs));
        slots[HUB_NUMBERS] = new Slot(HUB_NUMBERS, "cyan_dye",
                DungeonLeftoverPolicy.hubSlotName(Kind.NUMBERS, pbs));
        slots[HUB_RANDOM] = new Slot(HUB_RANDOM, "white_dye", "Random");
        slots[HUB_STARTS] = new Slot(HUB_STARTS, "pink_dye",
                DungeonLeftoverPolicy.hubSlotName(Kind.STARTS_WITH, pbs));
        slots[HUB_SELECT] = new Slot(HUB_SELECT, "brown_dye",
                DungeonLeftoverPolicy.hubSlotName(Kind.SELECT_ALL, pbs));
        slots[HUB_MELODY] = new Slot(HUB_MELODY, "purple_dye",
                DungeonLeftoverPolicy.hubSlotName(Kind.MELODY, pbs));
        return new Layout(Kind.HUB, titleFor(Kind.HUB), 27, Arrays.asList(slots), "", "", null);
    }

    public static Layout randomPractice(Random rng) {
        Kind kind = RANDOM_POOL[rng.nextInt(RANDOM_POOL.length)];
        return generate(kind, rng);
    }

    public static Layout generate(Kind kind, Random rng) {
        if (kind == null || kind == Kind.HUB) {
            return hub();
        }
        return switch (kind) {
            case PANES -> generatePanes(rng);
            case NUMBERS -> generateNumbers(rng);
            case STARTS_WITH -> generateStartsWith(rng);
            case SELECT_ALL -> generateSelectAll(rng);
            case RUBIX -> generateRubix(rng);
            case MELODY -> generateMelody(rng);
            case HUB -> hub();
        };
    }

    public static ClickResult click(Layout layout, int slot, int button) {
        if (layout == null) {
            return new ClickResult(hub(), false, false);
        }
        return switch (layout.kind()) {
            case HUB -> clickHub(layout, slot);
            case PANES -> clickPanes(layout, slot);
            case NUMBERS -> clickNumbers(layout, slot);
            case STARTS_WITH -> clickStartsWith(layout, slot);
            case SELECT_ALL -> clickSelectAll(layout, slot);
            case RUBIX -> clickRubix(layout, slot, button);
            case MELODY -> clickMelody(layout, slot);
        };
    }

    public static Layout tickMelody(Layout layout) {
        if (layout == null || layout.kind() != Kind.MELODY || layout.melody() == null) {
            return layout;
        }
        MelodyClock clock = layout.melody();
        int ticks = clock.tickCounter() + 1;
        if (ticks % 10 != 0) {
            return withMelody(layout, new MelodyClock(
                    clock.magentaColumn(), clock.limeColumn(), clock.currentRow(),
                    clock.limeDirection(), ticks));
        }
        int lime = clock.limeColumn() + clock.limeDirection();
        int dir = clock.limeDirection();
        if (lime <= 1 || lime >= 5) {
            dir = -dir;
            lime = Math.max(1, Math.min(5, lime));
        }
        return withMelody(layout, new MelodyClock(
                clock.magentaColumn(), lime, clock.currentRow(), dir, ticks));
    }

    public static List<DungeonPolicy.TerminalItem> terminalItems(Layout layout) {
        if (layout == null) {
            return List.of();
        }
        List<DungeonPolicy.TerminalItem> items = new ArrayList<>();
        for (Slot slot : layout.slots()) {
            items.add(new DungeonPolicy.TerminalItem(
                    slot.index(), slot.name(), slot.itemId(), slot.enchanted(), slot.count()));
        }
        return List.copyOf(items);
    }

    public static DungeonPolicy.Terminal dungeonTerminal(Layout layout) {
        if (layout == null) {
            return DungeonPolicy.Terminal.NONE;
        }
        return switch (layout.kind()) {
            case HUB -> DungeonPolicy.Terminal.NONE;
            case PANES -> DungeonPolicy.Terminal.PANES;
            case NUMBERS -> DungeonPolicy.Terminal.NUMBERS;
            case STARTS_WITH -> DungeonPolicy.Terminal.STARTS_WITH;
            case SELECT_ALL -> DungeonPolicy.Terminal.SELECT_ALL;
            case RUBIX -> DungeonPolicy.Terminal.RUBIX;
            case MELODY -> DungeonPolicy.Terminal.MELODY;
        };
    }

    public static int pingTicks(int pingMillis) {
        return Math.max(0, pingMillis / 50);
    }

    private static ClickResult clickHub(Layout layout, int slot) {
        Kind choice = hubChoice(slot);
        if (choice == null) {
            return new ClickResult(layout, false, false);
        }
        return new ClickResult(layout, true, true);
    }

    private static Layout generatePanes(Random rng) {
        int size = sizeFor(Kind.PANES);
        Slot[] slots = filledBlack(size);
        boolean anyRed = false;
        for (int index : innerSlots(1, 3, 1, 7)) {
            boolean red = rng.nextBoolean();
            anyRed |= red;
            slots[index] = pane(index, red);
        }
        if (!anyRed) {
            slots[10] = pane(10, true);
        }
        return new Layout(Kind.PANES, titleFor(Kind.PANES), size, Arrays.asList(slots), "", "", null);
    }

    private static ClickResult clickPanes(Layout layout, int slot) {
        Slot current = layout.slotAt(slot);
        if (current == null || !current.itemId().contains("stained_glass")) {
            return new ClickResult(layout, false, false);
        }
        if (current.itemId().contains("black")) {
            return new ClickResult(layout, false, false);
        }
        boolean wasRed = current.itemId().contains("red");
        List<Slot> next = replace(layout.slots(), pane(slot, !wasRed));
        Layout updated = new Layout(Kind.PANES, layout.title(), layout.size(), next, "", "", null);
        return new ClickResult(updated, true, !hasRedPane(updated));
    }

    private static Layout generateNumbers(Random rng) {
        int size = sizeFor(Kind.NUMBERS);
        Slot[] slots = filledBlack(size);
        List<Integer> values = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            values.add(i);
        }
        Collections.shuffle(values, rng);
        int n = 0;
        for (int index : innerSlots(1, 2, 1, 7)) {
            int value = values.get(n++);
            slots[index] = new Slot(index, "red_stained_glass_pane", String.valueOf(value), value, false);
        }
        return new Layout(Kind.NUMBERS, titleFor(Kind.NUMBERS), size, Arrays.asList(slots), "", "", null);
    }

    private static ClickResult clickNumbers(Layout layout, int slot) {
        int nextValue = Integer.MAX_VALUE;
        int nextSlot = -1;
        for (Slot item : layout.slots()) {
            if (!item.itemId().contains("red_stained_glass")) {
                continue;
            }
            if (item.count() < nextValue) {
                nextValue = item.count();
                nextSlot = item.index();
            }
        }
        if (slot != nextSlot) {
            return new ClickResult(layout, false, false);
        }
        Slot current = layout.slotAt(slot);
        Slot lime = new Slot(slot, "lime_stained_glass_pane", "", current.count(), false);
        List<Slot> next = replace(layout.slots(), lime);
        Layout updated = new Layout(Kind.NUMBERS, layout.title(), layout.size(), next, "", "", null);
        boolean done = true;
        for (Slot item : updated.slots()) {
            if (item.itemId().contains("red_stained_glass")) {
                done = false;
                break;
            }
        }
        return new ClickResult(updated, true, done);
    }

    private static Layout generateStartsWith(Random rng) {
        String letter = START_LETTERS[rng.nextInt(START_LETTERS.length)];
        int size = sizeFor(Kind.STARTS_WITH);
        Slot[] slots = filledBlack(size);
        boolean guaranteed = false;
        for (int index : innerSlots(1, 3, 1, 7)) {
            boolean match = !guaranteed || rng.nextDouble() > 0.55D;
            Slot item = randomNamed(index, letter, match, rng);
            if (item.name().toLowerCase(Locale.ROOT).startsWith(letter.toLowerCase(Locale.ROOT))) {
                guaranteed = true;
            }
            slots[index] = item;
        }
        if (!guaranteed) {
            slots[10] = new Slot(10, "apple", "Apple");
            letter = "A";
        }
        String title = "What starts with: '" + letter + "'?";
        return new Layout(Kind.STARTS_WITH, title, size, Arrays.asList(slots), letter, "", null);
    }

    private static ClickResult clickStartsWith(Layout layout, int slot) {
        Slot current = layout.slotAt(slot);
        if (current == null || current.enchanted()) {
            return new ClickResult(layout, false, false);
        }
        String letter = layout.letter().toLowerCase(Locale.ROOT);
        if (!current.name().toLowerCase(Locale.ROOT).startsWith(letter)) {
            return new ClickResult(layout, false, false);
        }
        Slot enchanted = new Slot(slot, current.itemId(), current.name(), current.count(), true);
        List<Slot> next = replace(layout.slots(), enchanted);
        Layout updated = new Layout(
                Kind.STARTS_WITH, layout.title(), layout.size(), next, layout.letter(), "", null);
        List<Integer> remaining = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.STARTS_WITH, updated.title(), terminalItems(updated));
        return new ClickResult(updated, true, remaining.isEmpty());
    }

    private static Layout generateSelectAll(Random rng) {
        String color = SELECT_COLORS[rng.nextInt(SELECT_COLORS.length)];
        String titleColor = "silver".equals(color) ? "silver" : color;
        int size = sizeFor(Kind.SELECT_ALL);
        Slot[] slots = filledBlack(size);
        boolean guaranteed = false;
        for (int index : innerSlots(1, 4, 1, 7)) {
            boolean match = !guaranteed || rng.nextDouble() > 0.65D;
            Slot item = colorItem(index, match ? color : otherColor(color, rng), false);
            if (match) {
                guaranteed = true;
            }
            slots[index] = item;
        }
        if (!guaranteed) {
            slots[10] = colorItem(10, color, false);
        }
        String title = "Select all the " + titleColor + " items!";
        return new Layout(Kind.SELECT_ALL, title, size, Arrays.asList(slots), "", color, null);
    }

    private static ClickResult clickSelectAll(Layout layout, int slot) {
        Slot current = layout.slotAt(slot);
        if (current == null || current.enchanted()) {
            return new ClickResult(layout, false, false);
        }
        String blob = (current.name() + " " + current.itemId()).toLowerCase(Locale.ROOT);
        String want = "silver".equals(layout.color()) ? "light_gray" : layout.color();
        if (!blob.contains(want) && !blob.contains(layout.color())) {
            return new ClickResult(layout, false, false);
        }
        Slot enchanted = new Slot(slot, current.itemId(), current.name(), current.count(), true);
        List<Slot> next = replace(layout.slots(), enchanted);
        Layout updated = new Layout(
                Kind.SELECT_ALL, layout.title(), layout.size(), next, "", layout.color(), null);
        List<Integer> remaining = DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, updated.title(), terminalItems(updated));
        return new ClickResult(updated, true, remaining.isEmpty());
    }

    private static Layout generateRubix(Random rng) {
        int size = sizeFor(Kind.RUBIX);
        Slot[] slots = filledBlack(size);
        for (int index : RUBIX_SLOTS) {
            int meta = rng.nextInt(RUBIX_COLORS.length);
            slots[index] = rubixPane(index, meta);
        }
        return new Layout(Kind.RUBIX, titleFor(Kind.RUBIX), size, Arrays.asList(slots), "", "", null);
    }

    private static ClickResult clickRubix(Layout layout, int slot, int button) {
        boolean allowed = false;
        for (int index : RUBIX_SLOTS) {
            if (index == slot) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return new ClickResult(layout, false, false);
        }
        Slot current = layout.slotAt(slot);
        int meta = rubixIndex(current == null ? "" : current.itemId());
        if (meta < 0) {
            return new ClickResult(layout, false, false);
        }
        int nextMeta = button == 1
                ? (meta - 1 + RUBIX_COLORS.length) % RUBIX_COLORS.length
                : (meta + 1) % RUBIX_COLORS.length;
        List<Slot> next = replace(layout.slots(), rubixPane(slot, nextMeta));
        Layout updated = new Layout(Kind.RUBIX, layout.title(), layout.size(), next, "", "", null);
        return new ClickResult(updated, true, rubixComplete(updated));
    }

    private static Layout generateMelody(Random rng) {
        int magenta = 1 + rng.nextInt(5);
        MelodyClock clock = new MelodyClock(magenta, 1, 1, 1, 0);
        return withMelody(new Layout(
                Kind.MELODY, titleFor(Kind.MELODY), sizeFor(Kind.MELODY),
                List.of(), "", "", clock), clock);
    }

    private static ClickResult clickMelody(Layout layout, int slot) {
        MelodyClock clock = layout.melody();
        if (clock == null) {
            return new ClickResult(layout, false, false);
        }
        if (slot % 9 != 7 || slot / 9 != clock.currentRow() || clock.limeColumn() != clock.magentaColumn()) {
            return new ClickResult(layout, false, false);
        }
        int row = clock.currentRow() + 1;
        MelodyClock next = new MelodyClock(
                1 + Math.floorMod(clock.magentaColumn() + 1, 5),
                clock.limeColumn(),
                row,
                clock.limeDirection(),
                clock.tickCounter());
        Layout updated = withMelody(layout, next);
        return new ClickResult(updated, true, row >= 5);
    }

    private static Layout withMelody(Layout layout, MelodyClock clock) {
        int size = sizeFor(Kind.MELODY);
        Slot[] slots = filledBlack(size);
        for (int index = 0; index < size; index++) {
            int row = index / 9;
            int col = index % 9;
            if (col == clock.magentaColumn() && (row < 1 || row >= 5)) {
                slots[index] = namedPane(index, "magenta_stained_glass_pane");
            } else if (col == clock.limeColumn() && row == clock.currentRow()) {
                slots[index] = namedPane(index, "lime_stained_glass_pane");
            } else if (col >= 1 && col < 6 && row == clock.currentRow()) {
                slots[index] = namedPane(index, "red_stained_glass_pane");
            } else if (col == 7 && row == clock.currentRow()) {
                slots[index] = new Slot(index, "lime_terracotta", "");
            } else if (col == 7 && row >= 1 && row < 5) {
                slots[index] = new Slot(index, "red_terracotta", "");
            } else if (col >= 1 && col < 6 && row >= 1 && row < 5) {
                slots[index] = namedPane(index, "white_stained_glass_pane");
            }
        }
        return new Layout(Kind.MELODY, titleFor(Kind.MELODY), size, Arrays.asList(slots), "", "", clock);
    }

    private static Slot[] filledBlack(int size) {
        Slot[] slots = new Slot[size];
        for (int i = 0; i < size; i++) {
            slots[i] = black(i);
        }
        return slots;
    }

    private static Slot black(int index) {
        return new Slot(index, BLACK, "");
    }

    private static Slot pane(int index, boolean red) {
        return namedPane(index, red ? "red_stained_glass_pane" : "lime_stained_glass_pane");
    }

    private static Slot namedPane(int index, String id) {
        return new Slot(index, id, "");
    }

    private static Slot rubixPane(int index, int meta) {
        int safe = Math.max(0, Math.min(RUBIX_COLORS.length - 1, meta));
        return namedPane(index, RUBIX_COLORS[safe]);
    }

    private static int rubixIndex(String itemId) {
        for (int i = 0; i < RUBIX_COLORS.length; i++) {
            if (RUBIX_COLORS[i].equals(itemId)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean rubixComplete(Layout layout) {
        Slot first = layout.slotAt(12);
        if (first == null) {
            return false;
        }
        for (int index : RUBIX_SLOTS) {
            Slot slot = layout.slotAt(index);
            if (slot == null || !first.itemId().equals(slot.itemId())) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRedPane(Layout layout) {
        for (Slot slot : layout.slots()) {
            if (slot.itemId().contains("red_stained_glass")) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> innerSlots(int rowMin, int rowMax, int colMin, int colMax) {
        List<Integer> slots = new ArrayList<>();
        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    private static List<Slot> replace(List<Slot> slots, Slot replacement) {
        List<Slot> next = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            next.add(slot.index() == replacement.index() ? replacement : slot);
        }
        return next;
    }

    private static Slot randomNamed(int index, String letter, boolean match, Random rng) {
        List<String[]> pool = new ArrayList<>();
        for (String[] item : START_ITEMS) {
            boolean starts = item[1].toUpperCase(Locale.ROOT).startsWith(letter.toUpperCase(Locale.ROOT));
            if (starts == match) {
                pool.add(item);
            }
        }
        if (pool.isEmpty()) {
            pool.add(match ? new String[] {"apple", "Apple"} : new String[] {"stick", "Stick"});
        }
        String[] pick = pool.get(rng.nextInt(pool.size()));
        return new Slot(index, pick[0], pick[1]);
    }

    private static String otherColor(String color, Random rng) {
        String pick = color;
        while (pick.equals(color)) {
            pick = SELECT_COLORS[rng.nextInt(SELECT_COLORS.length)];
        }
        return pick;
    }

    private static Slot colorItem(int index, String color, boolean enchanted) {
        String id = switch (color) {
            case "silver" -> "light_gray_wool";
            case "black" -> "ink_sac";
            case "blue" -> "blue_wool";
            case "brown" -> "cocoa_beans";
            case "white" -> "white_wool";
            default -> color + "_wool";
        };
        String name = switch (color) {
            case "silver" -> "Light Gray Wool";
            case "black" -> "Ink Sac";
            case "brown" -> "Cocoa Beans";
            default -> color.substring(0, 1).toUpperCase(Locale.ROOT) + color.substring(1) + " Wool";
        };
        return new Slot(index, id, name, 1, enchanted);
    }
}
