package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fi.rotclient.DungeonPolicy.*;

/**
 * Plus-only solutions for Hypixel dungeon terminals.
 * The Lite artifact contains only the inert extension entry point.
 */
public final class DungeonTerminalSolverPolicy {
    private static final Pattern STARTS_LETTER = Pattern.compile("(?i)starts with:\\s*'([^']+)'");
    private static final Pattern SELECT_COLOR = Pattern.compile(
            "(?i)select all the\\s+([a-z ]+?)\\s+items?");
    private static final Pattern COLOR_TITLE = Pattern.compile(
            "(?i)(?:what color was|select the color)\\s+(?:the\\s+)?([a-z]+(?:\\s+gray|\\s+blue|\\s+green)?)");
    private static final Pattern NUMBER_NAME = Pattern.compile("^(\\d+)$");
    private static final Set<Integer> RUBIX_SLOTS = Set.of(12, 13, 14, 21, 22, 23, 30, 31, 32);
    private static final Set<Integer> PANE_SLOTS = Set.of(
            11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33);
    private static final Set<Integer> NUMBER_SLOTS = Set.of(
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25);
    private static final Set<Integer> STARTS_WITH_SLOTS = Set.of(
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34);

    private DungeonTerminalSolverPolicy() {
    }

    public static List<TerminalClick> solveClicks(
            Terminal terminal, String title, List<TerminalItem> items) {
        if (terminal == null || terminal == Terminal.NONE || items == null) {
            return List.of();
        }
        return switch (terminal) {
            case PANES -> clicks(solveRedGreen(items), 0);
            case STARTS_WITH -> clicks(solveStartsWith(title, items), 0);
            case SELECT_ALL, COLORS -> clicks(solveSelectAll(title, items), 0);
            case NUMBERS -> clicks(solveNumbers(items), 0);
            case RUBIX -> solveRubixClicks(items);
            case MELODY -> solveMelodyClicks(items);
            case NONE -> List.of();
        };
    }

    private static List<TerminalClick> clicks(List<Integer> slots, int button) {
        List<TerminalClick> hits = new ArrayList<>();
        for (int slot : slots) {
            hits.add(new TerminalClick(slot, button));
        }
        return List.copyOf(hits);
    }

    private static List<Integer> solveRedGreen(List<TerminalItem> items) {
        List<Integer> hits = new ArrayList<>();
        for (TerminalItem item : items) {
            if (!PANE_SLOTS.contains(item.index())) {
                continue;
            }
            String id = item.itemId().toLowerCase(Locale.ROOT);
            String name = item.name().toLowerCase(Locale.ROOT);
            boolean pane = id.contains("stained_glass") || name.contains("stained glass");
            boolean red = id.contains("red") || name.contains("red");
            if (pane && red) {
                hits.add(item.index());
            }
        }
        return List.copyOf(hits);
    }

    private static List<Integer> solveStartsWith(String title, List<TerminalItem> items) {
        Matcher matcher = STARTS_LETTER.matcher(normalize(title));
        if (!matcher.find()) {
            return List.of();
        }
        String letter = matcher.group(1).toLowerCase(Locale.ROOT);
        List<Integer> hits = new ArrayList<>();
        for (TerminalItem item : items) {
            if (!STARTS_WITH_SLOTS.contains(item.index()) || item.name().isBlank()) {
                continue;
            }
            String name = normalize(item.name()).toLowerCase(Locale.ROOT);
            if (name.contains("stained glass")) {
                continue;
            }
            boolean special = item.itemId().toLowerCase(Locale.ROOT).contains("golden_apple")
                    || name.contains("golden apple");
            if (item.enchanted() && !special) {
                continue;
            }
            if (name.startsWith(letter)) {
                hits.add(item.index());
            }
        }
        return List.copyOf(hits);
    }

    private static List<Integer> solveSelectAll(String title, List<TerminalItem> items) {
        String color = extractSelectColor(title);
        if (color.isEmpty()) {
            return List.of();
        }
        List<Integer> hits = new ArrayList<>();
        for (TerminalItem item : items) {
            if (!inTerminalGrid(item.index()) || item.enchanted()) {
                continue;
            }
            if (itemMatchesSelectColor(color, item.name(), item.itemId())) {
                hits.add(item.index());
            }
        }
        return List.copyOf(hits);
    }

    static String extractSelectColor(String title) {
        String text = normalize(title).toLowerCase(Locale.ROOT);
        Matcher select = SELECT_COLOR.matcher(text);
        if (select.find()) {
            return fixColorName(select.group(1).trim());
        }
        Matcher color = COLOR_TITLE.matcher(text);
        if (color.find()) {
            return fixColorName(color.group(1).trim());
        }
        return "";
    }

    private static String fixColorName(String name) {
        String text = name == null ? "" : name.toLowerCase(Locale.ROOT);
        text = text.replace("light gray", "silver").replace("light_gray", "silver");
        text = text.replace("wool", "white").replace("bone", "white");
        text = text.replace("ink", "black").replace("lapis", "blue");
        text = text.replace("cocoa", "brown").replace("dandelion", "yellow");
        text = text.replace("rose", "red").replace("cactus", "green");
        text = text.replace("poppy", "red").replace("sunflower", "yellow");
        return text;
    }

    static boolean itemMatchesSelectColor(String color, String name, String itemId) {
        String blob = ((name == null ? "" : name) + " " + (itemId == null ? "" : itemId))
                .toLowerCase(Locale.ROOT);
        if (blob.contains("black_stained") || blob.contains("black stained")) {
            return false;
        }
        for (String token : colorKeywords(color)) {
            if (!token.isEmpty() && blob.contains(token)) {
                return true;
            }
        }
        return false;
    }

    static List<String> colorKeywords(String color) {
        String canonical = fixColorName(color == null ? "" : color.trim());
        return switch (canonical) {
            case "green" -> List.of("green", "cactus", "lime");
            case "red" -> List.of("red", "rose", "poppy");
            case "yellow" -> List.of("yellow", "dandelion", "sunflower");
            case "white" -> List.of("white", "bone", "wool");
            case "black" -> List.of("black", "ink");
            case "blue" -> List.of("blue", "lapis");
            case "brown" -> List.of("brown", "cocoa");
            case "silver" -> List.of("silver", "light gray", "light_gray");
            default -> canonical.isEmpty() ? List.of() : List.of(canonical);
        };
    }

    private static List<Integer> solveNumbers(List<TerminalItem> items) {
        record Numbered(int index, int value) {
        }
        List<Numbered> numbered = new ArrayList<>();
        for (TerminalItem item : items) {
            if (!NUMBER_SLOTS.contains(item.index()) || item.enchanted()) {
                continue;
            }
            String id = item.itemId().toLowerCase(Locale.ROOT);
            Matcher matcher = NUMBER_NAME.matcher(normalize(item.name()));
            if (matcher.matches()) {
                numbered.add(new Numbered(item.index(), Integer.parseInt(matcher.group(1))));
                continue;
            }
            if (id.contains("red_stained_glass") && item.count() > 0) {
                numbered.add(new Numbered(item.index(), item.count()));
            }
        }
        numbered.sort(Comparator.comparingInt(Numbered::value));
        List<Integer> hits = new ArrayList<>();
        for (Numbered numberedItem : numbered) {
            hits.add(numberedItem.index());
        }
        return List.copyOf(hits);
    }

    private static List<TerminalClick> solveRubixClicks(List<TerminalItem> items) {
        int[] costs = new int[5];
        List<TerminalItem> panes = new ArrayList<>();
        for (TerminalItem item : items) {
            if (!RUBIX_SLOTS.contains(item.index())) {
                continue;
            }
            int idx = rubixIndex(item);
            if (idx >= 0) {
                panes.add(item);
            }
        }
        for (int target = 0; target < 5; target++) {
            for (TerminalItem pane : panes) {
                int idx = rubixIndex(pane);
                int dist = Math.abs(target - idx);
                costs[target] += dist > 2 ? 5 - dist : dist;
            }
        }
        int origin = 0;
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            if (costs[i] < best) {
                best = costs[i];
                origin = i;
            }
        }
        List<TerminalClick> hits = new ArrayList<>();
        for (TerminalItem pane : panes) {
            int current = rubixIndex(pane);
            if (current < 0 || current == origin) {
                continue;
            }
            int diff = origin - current;
            if (diff > 2) {
                diff -= 5;
            }
            if (diff < -2) {
                diff += 5;
            }
            hits.add(new TerminalClick(pane.index(), diff > 0 ? 0 : 1));
        }
        return List.copyOf(hits);
    }

    private static List<TerminalClick> solveMelodyClicks(List<TerminalItem> items) {
        MelodyState melody = parseMelody(items);
        if (!melody.readyToClick()) {
            return List.of();
        }
        return List.of(new TerminalClick(melody.clickSlot(), 0));
    }

    private static int rubixIndex(TerminalItem item) {
        String blob = (item.itemId() + " " + item.name()).toLowerCase(Locale.ROOT);
        if (blob.contains("orange")) {
            return 1;
        }
        if (blob.contains("yellow")) {
            return 2;
        }
        if (blob.contains("lime") || blob.contains("green")) {
            return 3;
        }
        if (blob.contains("blue")) {
            return 4;
        }
        if (blob.contains("red")) {
            return 0;
        }
        return -1;
    }

    private static boolean inTerminalGrid(int index) {
        int row = index / 9;
        int col = index % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 7 && index < 54;
    }
}
