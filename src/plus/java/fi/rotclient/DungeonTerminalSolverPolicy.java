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
    private static final Pattern STARTS_LETTER = Pattern.compile(
            "(?i)^What starts with:\\s*'([a-z])'\\?$");
    private static final Pattern SELECT_COLOR = Pattern.compile(
            "(?i)^select all the\\s+([a-z ]+?)\\s+items?!$");
    private static final Pattern COLOR_TITLE = Pattern.compile(
            "(?i)^(?:what color was|select the color)\\s+(?:the\\s+)?([a-z]+(?:\\s+gray|\\s+blue|\\s+green)?)\\??!?$");
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
        if (terminal == null || terminal == Terminal.NONE || items == null
                || (!normalize(title).isEmpty() && !supportsTitle(terminal, title))) {
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

    static boolean supportsTitle(Terminal terminal, String title) {
        String text = normalize(title);
        if (terminal == null) return false;
        return switch (terminal) {
            case PANES -> text.equalsIgnoreCase("Correct all the panes!");
            case NUMBERS -> text.equalsIgnoreCase("Click in order!");
            case RUBIX -> text.equalsIgnoreCase("Change all to same color!")
                    || text.equalsIgnoreCase("Change all the items!");
            case MELODY -> text.equalsIgnoreCase("Click the button on time!")
                    || text.equalsIgnoreCase("Click the button in time!");
            case STARTS_WITH -> STARTS_LETTER.matcher(text).matches();
            case SELECT_ALL -> SELECT_COLOR.matcher(text).matches();
            case COLORS -> COLOR_TITLE.matcher(text).matches();
            case NONE -> false;
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
            String id = itemPath(item.itemId());
            String name = normalize(item.name()).toLowerCase(Locale.ROOT);
            boolean pane = id.contains("stained_glass") || name.contains("stained glass");
            boolean red = id.equals("red_stained_glass") || id.equals("red_stained_glass_pane")
                    || (id.isEmpty() && name.startsWith("red stained glass"));
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
            if (itemPath(item.itemId()).endsWith("stained_glass_pane") || name.contains("stained glass")) {
                continue;
            }
            boolean special = itemPath(item.itemId()).equals("golden_apple")
                    || itemPath(item.itemId()).equals("enchanted_golden_apple");
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
            return canonicalColor(select.group(1));
        }
        Matcher color = COLOR_TITLE.matcher(text);
        if (color.find()) {
            return canonicalColor(color.group(1));
        }
        return "";
    }

    private static String canonicalColor(String name) {
        String text = normalize(name).toLowerCase(Locale.ROOT).replace('_', ' ');
        if (text.equals("light gray")) return "silver";
        return switch (text) {
            case "white", "orange", "magenta", "light blue", "yellow", "lime", "pink", "gray",
                    "silver", "cyan", "purple", "blue", "brown", "green", "red", "black" -> text;
            default -> "";
        };
    }

    static boolean itemMatchesSelectColor(String color, String name, String itemId) {
        String id = itemPath(itemId);
        if (id.equals("black_stained_glass_pane") || id.equals("black_stained_glass")) {
            return false;
        }
        // Prefix mapping adapted from Odin SelectAllHandler (BSD-3-Clause,
        // Copyright (c) 2025, odtheking), 833e0533ef9c47529b790612627a65618ebd5a58.
        // Also cross-checked with NoammAddons ColorsTerminal (CC0-1.0).
        // Full notices: docs/third-party/Odin-LICENSE.txt and NoammAddons-LICENSE.txt.
        // Names carry the puzzle's color; arbitrary item-ID substrings do not.
        String text = normalize(name).toLowerCase(Locale.ROOT);
        for (String token : colorKeywords(color)) {
            if (text.equals(token) || text.startsWith(token + " ")) {
                return true;
            }
        }
        return false;
    }

    static List<String> colorKeywords(String color) {
        String canonical = canonicalColor(color);
        return switch (canonical) {
            case "green" -> List.of("green", "cactus");
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
            String id = itemPath(item.itemId());
            // Odin NumbersHandler / NoammAddons NumberTerminal use the red
            // pane and stack count as authority. Completed panes retain digits.
            if ((id.equals("red_stained_glass_pane") || id.equals("red_stained_glass"))
                    && item.count() > 0 && item.count() <= 14) {
                numbered.add(new Numbered(item.index(), item.count()));
            }
        }
        numbered.sort(Comparator.comparingInt(Numbered::value).thenComparingInt(Numbered::index));
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
        String id = itemPath(item.itemId());
        if (!id.endsWith("_stained_glass_pane") && !id.endsWith("_stained_glass")) return -1;
        String color = id.substring(0, id.indexOf("_stained_glass"));
        if (color.equals("orange")) {
            return 1;
        }
        if (color.equals("yellow")) {
            return 2;
        }
        if (color.equals("lime") || color.equals("green")) {
            return 3;
        }
        if (color.equals("blue")) {
            return 4;
        }
        if (color.equals("red")) {
            return 0;
        }
        return -1;
    }

    private static String itemPath(String itemId) {
        String id = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }

    private static boolean inTerminalGrid(int index) {
        int row = index / 9;
        int col = index % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 7 && index < 54;
    }
}
