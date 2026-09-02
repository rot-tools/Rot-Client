package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side Catacombs helpers for the Serveri.
 * Parses scoreboard/chat, classifies dungeon ESP, Spirit Leap slots, F7
 * terminals and requeue cues using the same Hypixel strings.
 */
public final class DungeonPolicy {
    public enum EspKind {
        NONE,
        STARRED,
        BAT,
        FEL,
        SHADOW_ASSASSIN,
        WITHER_KEY,
        BLOOD_KEY,
        MIMIC,
        PRINCE,
        WATCHER,
        CRYSTAL,
        RELIC,
        WITHER,
        BLAZE,
        THORN,
        LIVID,
        SPIRIT_BEAR
    }

    public enum DungeonClass {
        UNKNOWN,
        ARCHER,
        BERSERK,
        MAGE,
        HEALER,
        TANK
    }

    public enum Terminal {
        NONE,
        PANES,
        STARTS_WITH,
        SELECT_ALL,
        COLORS,
        NUMBERS,
        RUBIX,
        MELODY
    }

    public enum Invincibility {
        NONE,
        BONZO,
        SPIRIT,
        PHOENIX
    }

    // SPIRIT_BEAR is classified from holograms for Spirit Bear (C).

    public record Sidebar(
            String floor,
            String secrets,
            int secretsFound,
            int secretsTotal,
            int score,
            int clearedPercent,
            DungeonClass dungeonClass,
            int crypts,
            int deaths,
            boolean boss) {
    }

    public record TerminalItem(
            int index,
            String name,
            String itemId,
            boolean enchanted,
            int count) {
        public TerminalItem(int index, String name, String itemId, boolean enchanted) {
            this(index, name, itemId, enchanted, 1);
        }
    }

    public record TerminalClick(int slot, int button) {
    }

    public record MelodyState(Integer buttonRow, Integer current, Integer correct) {
        public boolean readyToClick() {
            return buttonRow != null && current != null && correct != null && current.equals(correct);
        }

        public int clickSlot() {
            return buttonRow == null ? -1 : buttonRow * 9 + 16;
        }
    }

    public static final List<String> MELODY_SKIP_MODES = List.of("Edges", "All");
    public static final List<String> I4_LEAP_CLASSES = List.of("Tank", "Mage", "Healer", "Archer");

    public static final long BONZO_MILLIS = 3_000L;
    public static final long SPIRIT_MILLIS = 3_000L;
    public static final long PHOENIX_MILLIS = 4_000L;
    public static final long TERRACOTTA_MILLIS = 15_000L;
    public static final int DEFAULT_REQUEUE_DELAY_TICKS = 40;
    public static final String REQUEUE_COMMAND = "/instancerequeue";
    public static final String MIMIC_PARTY = "Mimic Killed!";
    public static final String PRINCE_PARTY = "Prince Killed!";
    public static final String BAT_PARTY = "Bat Killed!";

    private static final Pattern SECRETS = Pattern.compile(
            "(?i)(?:secrets?[:\\s]+)?(\\d+)\\s*/\\s*(\\d+)(?:\\s*secrets?)?");
    private static final Pattern SCORE = Pattern.compile("(?i)(?:score[:\\s]+)?(\\d{2,3})\\b");
    private static final Pattern CLEARED = Pattern.compile("(?i)(?:cleared[:\\s]+)?(\\d{1,3})%");
    private static final Pattern CRYPTS = Pattern.compile("(?i)crypts?[:\\s]+(\\d+)");
    private static final Pattern DEATHS = Pattern.compile("(?i)deaths?[:\\s]+(\\d+)");
    private static final Pattern BOSS = Pattern.compile(
            "(?i)\\b(maxor|storm|goldor|necron|sadan|livid|thorn|wither king|the boss)\\b");
    private static final Pattern FLOOR = Pattern.compile("(?i)\\b(m[1-7]|f[1-7])\\b");
    private static final Pattern CLASS = Pattern.compile(
            "(?i)\\b(archer|berserk|mage|healer|tank)\\b");
    private static final Pattern STARTS_LETTER = Pattern.compile("(?i)starts with:\\s*'([^']+)'");
    private static final Pattern SELECT_COLOR = Pattern.compile(
            "(?i)select all the\\s+([a-z ]+?)\\s+items?");
    private static final Pattern COLOR_TITLE = Pattern.compile(
            "(?i)(?:what color was|select the color)\\s+(?:the\\s+)?([a-z]+(?:\\s+gray|\\s+blue|\\s+green)?)");
    private static final Pattern NUMBER_NAME = Pattern.compile("^(\\d+)$");
    private static final Set<Integer> RUBIX_SLOTS = Set.of(12, 13, 14, 21, 22, 23, 30, 31, 32);

    private DungeonPolicy() {
    }

    public static Sidebar parseSidebar(List<String> lines) {
        String floor = "";
        String secrets = "";
        int found = -1;
        int total = -1;
        int score = -1;
        int cleared = -1;
        int crypts = -1;
        int deaths = -1;
        boolean boss = false;
        DungeonClass dungeonClass = DungeonClass.UNKNOWN;
        if (lines == null) {
            return new Sidebar(floor, secrets, found, total, score, cleared, dungeonClass,
                    crypts, deaths, boss);
        }
        for (String raw : lines) {
            String line = normalize(raw);
            Matcher secretsMatch = SECRETS.matcher(line);
            if (secretsMatch.find() && (line.toLowerCase(Locale.ROOT).contains("secret")
                    || found < 0)) {
                found = Integer.parseInt(secretsMatch.group(1));
                total = Integer.parseInt(secretsMatch.group(2));
                secrets = found + "/" + total;
            }
            Matcher floorMatch = FLOOR.matcher(line);
            if (floorMatch.find() && (line.toLowerCase(Locale.ROOT).contains("catacomb")
                    || line.toLowerCase(Locale.ROOT).contains("master")
                    || floor.isEmpty())) {
                floor = floorMatch.group(1).toUpperCase(Locale.ROOT);
            }
            Matcher classMatch = CLASS.matcher(line);
            if (classMatch.find() && dungeonClass == DungeonClass.UNKNOWN) {
                dungeonClass = dungeonClass(classMatch.group(1));
            }
            Matcher clearedMatch = CLEARED.matcher(line);
            if (clearedMatch.find() && line.toLowerCase(Locale.ROOT).contains("clear")) {
                cleared = Integer.parseInt(clearedMatch.group(1));
            }
            Matcher scoreMatch = SCORE.matcher(line);
            if (scoreMatch.find() && line.toLowerCase(Locale.ROOT).contains("score")) {
                score = Integer.parseInt(scoreMatch.group(1));
            }
            Matcher cryptMatch = CRYPTS.matcher(line);
            if (cryptMatch.find()) {
                crypts = Integer.parseInt(cryptMatch.group(1));
            }
            Matcher deathMatch = DEATHS.matcher(line);
            if (deathMatch.find()) {
                deaths = Integer.parseInt(deathMatch.group(1));
            }
            if (BOSS.matcher(line).find()) {
                boss = true;
            }
        }
        return new Sidebar(floor, secrets, found, total, score, cleared, dungeonClass,
                crypts, deaths, boss);
    }

    public static boolean crossedScoreMilestone(int previous, int current, int threshold) {
        int goal = Math.max(1, threshold);
        return current >= goal && previous < goal;
    }

    public static EspKind classifyHologram(String hologram) {
        String text = normalize(hologram).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return EspKind.NONE;
        }
        if (text.contains("wither key")) {
            return EspKind.WITHER_KEY;
        }
        if (text.contains("blood key")) {
            return EspKind.BLOOD_KEY;
        }
        if (text.contains("shadow assassin")) {
            return EspKind.SHADOW_ASSASSIN;
        }
        if (text.contains("fels") || text.contains("super angry archaeologist")) {
            return EspKind.FEL;
        }
        if (text.contains("the watcher") || text.contains("chosen")) {
            return EspKind.WATCHER;
        }
        if (text.contains("spirit bear")) {
            return EspKind.SPIRIT_BEAR;
        }
        if (text.contains("thorn") && !text.contains("status")) {
            return EspKind.THORN;
        }
        if (text.contains("livid") && !text.contains("status")) {
            return EspKind.LIVID;
        }
        if (text.contains("mimic")) {
            return EspKind.MIMIC;
        }
        if (text.contains("prince")) {
            return EspKind.PRINCE;
        }
        if (text.contains("energy crystal")) {
            return EspKind.CRYSTAL;
        }
        if (text.contains("relic")) {
            return EspKind.RELIC;
        }
        if (text.contains("blaze") && (text.contains("❤") || text.contains("/"))) {
            return EspKind.BLAZE;
        }
        if (text.contains("maxor") || text.contains("storm") || text.contains("goldor")
                || text.contains("necron") || text.contains("wither")) {
            return EspKind.WITHER;
        }
        if (isStarredName(hologram)) {
            return EspKind.STARRED;
        }
        return EspKind.NONE;
    }

    public static boolean isStarredName(String hologram) {
        String text = hologram == null ? "" : hologram;
        return text.contains("✯") || text.contains("★") || text.contains("✮")
                || text.toLowerCase(Locale.ROOT).contains("starred");
    }

    public static boolean isSecretBat(String entityType) {
        return "bat".equalsIgnoreCase(entityType == null ? "" : entityType.trim());
    }

    public static boolean isLeapMenu(String title) {
        String text = normalize(title).toLowerCase(Locale.ROOT);
        return text.equals("spirit leap") || text.equals("ghost leap") || text.contains("spirit leap");
    }

    public static DungeonClass classFromLore(List<String> lore) {
        if (lore == null) {
            return DungeonClass.UNKNOWN;
        }
        for (String line : lore) {
            Matcher matcher = CLASS.matcher(normalize(line));
            if (matcher.find()) {
                return dungeonClass(matcher.group(1));
            }
        }
        return DungeonClass.UNKNOWN;
    }

    public static int leapSortKey(DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case ARCHER -> 0;
            case MAGE -> 1;
            case BERSERK -> 2;
            case TANK -> 3;
            case HEALER -> 4;
            case UNKNOWN -> 5;
        };
    }

    public static Terminal detectTerminal(String title) {
        String text = normalize(title).toLowerCase(Locale.ROOT);
        if (text.contains("correct all the panes")) {
            return Terminal.PANES;
        }
        if (text.contains("starts with:")) {
            return Terminal.STARTS_WITH;
        }
        if (text.contains("select all the")) {
            return Terminal.SELECT_ALL;
        }
        if (text.contains("click in order")) {
            return Terminal.NUMBERS;
        }
        if (text.contains("change all to same color") || text.contains("change all the items")) {
            return Terminal.RUBIX;
        }
        if (text.contains("click the button on time") || text.contains("click the button in time")) {
            return Terminal.MELODY;
        }
        if (text.contains("what color was") || text.contains("select the color")) {
            return Terminal.COLORS;
        }
        return Terminal.NONE;
    }

    public static List<Integer> solveTerminal(Terminal terminal, String title, List<TerminalItem> items) {
        return solveTerminalClicks(terminal, title, items).stream()
                .map(TerminalClick::slot)
                .distinct()
                .toList();
    }

    public static List<TerminalClick> solveTerminalClicks(
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

    public static List<TerminalClick> preferHumanClick(
            List<TerminalClick> clicks, int lastSlot) {
        if (clicks == null || clicks.size() < 2) {
            return clicks == null ? List.of() : clicks;
        }
        int last = lastSlot < 0 ? 22 : lastSlot;
        TerminalClick best = clicks.getFirst();
        int bestDist = Integer.MAX_VALUE;
        int bestNeighbors = Integer.MAX_VALUE;
        for (TerminalClick click : clicks) {
            int dx = (click.slot() % 9) - (last % 9);
            int dy = (click.slot() / 9) - (last / 9);
            int dist = dx * dx + dy * dy;
            int neighbors = 0;
            for (TerminalClick other : clicks) {
                if (other.slot() == click.slot()) {
                    continue;
                }
                int ox = (other.slot() % 9) - (click.slot() % 9);
                int oy = (other.slot() / 9) - (click.slot() / 9);
                if (ox * ox + oy * oy <= 2) {
                    neighbors++;
                }
            }
            if (dist < bestDist || (dist == bestDist && neighbors < bestNeighbors)) {
                best = click;
                bestDist = dist;
                bestNeighbors = neighbors;
            }
        }
        List<TerminalClick> ordered = new ArrayList<>();
        ordered.add(best);
        for (TerminalClick click : clicks) {
            if (click.slot() != best.slot() || click.button() != best.button()) {
                ordered.add(click);
            }
        }
        return List.copyOf(ordered);
    }

    public static MelodyState parseMelody(List<TerminalItem> items) {
        if (items == null) {
            return new MelodyState(null, null, null);
        }
        Integer button = null;
        Integer current = null;
        Integer correct = null;
        for (TerminalItem item : items) {
            String id = item.itemId().toLowerCase(Locale.ROOT);
            if (id.contains("magenta_stained_glass") && correct == null) {
                correct = item.index() - 1;
            }
            if (id.contains("lime_stained_glass")) {
                button = item.index() / 9 - 1;
                current = item.index() % 9 - 1;
            }
        }
        return new MelodyState(button, current, correct);
    }

    public static boolean shouldAutoSolve(Terminal terminal, boolean melody, boolean numbers,
            boolean colors, boolean rubix, boolean panes, boolean starts) {
        return switch (terminal) {
            case MELODY -> melody;
            case NUMBERS -> numbers;
            case SELECT_ALL, COLORS -> colors;
            case RUBIX -> rubix;
            case PANES -> panes;
            case STARTS_WITH -> starts;
            case NONE -> false;
        };
    }

    public static boolean isMimicChat(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("mimic dead")
                || text.contains("$skytils-dungeon-score-mimic$")
                || text.contains("mimic slain")
                || text.equals("mimic killed!");
    }

    public static boolean isPrinceChat(String line) {
        return normalize(line).toLowerCase(Locale.ROOT).contains("a prince falls");
    }

    public static boolean isBatScoreChat(String line) {
        return normalize(line).toLowerCase(Locale.ROOT).contains("a bat has been slain");
    }

    public static Invincibility invincibilityFromChat(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        if (text.contains("bonzo") && (text.contains("saved") || text.contains("clown"))) {
            return Invincibility.BONZO;
        }
        if (text.contains("second wind") || text.contains("spirit mask")) {
            return Invincibility.SPIRIT;
        }
        if (text.contains("phoenix") && text.contains("saved")) {
            return Invincibility.PHOENIX;
        }
        return Invincibility.NONE;
    }

    public static long invincibilityMillis(Invincibility kind) {
        return switch (kind) {
            case BONZO -> BONZO_MILLIS;
            case SPIRIT -> SPIRIT_MILLIS;
            case PHOENIX -> PHOENIX_MILLIS;
            case NONE -> 0L;
        };
    }

    public static boolean isTerracottaChat(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("[boss] sadan") && text.contains("terracotta");
    }

    public static boolean isDungeonEnd(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("extra stats")
                || text.contains("dungeon reward")
                || text.contains("team score:")
                || text.contains("the catacombs -");
    }

    public static boolean isBloodCampReady(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("[boss] the watcher")
                && (text.contains("that will be enough") || text.contains("you have proven"));
    }

    public static Optional<String> partyAnnounce(String line) {
        if (isMimicChat(line)) {
            return Optional.of(MIMIC_PARTY);
        }
        if (isPrinceChat(line)) {
            return Optional.of(PRINCE_PARTY);
        }
        if (isBatScoreChat(line)) {
            return Optional.of(BAT_PARTY);
        }
        return Optional.empty();
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§.", "")
                .replace('\u00A0', ' ')
                .replace("✯", "")
                .trim();
    }

    public static DungeonClass dungeonClass(String raw) {
        if (raw == null || raw.isBlank()) {
            return DungeonClass.UNKNOWN;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "archer" -> DungeonClass.ARCHER;
            case "berserk" -> DungeonClass.BERSERK;
            case "mage" -> DungeonClass.MAGE;
            case "healer" -> DungeonClass.HEALER;
            case "tank" -> DungeonClass.TANK;
            default -> DungeonClass.UNKNOWN;
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
            if (!inTerminalGrid(item.index())) {
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
            if (!inTerminalGrid(item.index()) || item.name().isBlank()) {
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
            String blob = fixColorName((item.name() + " " + item.itemId()).toLowerCase(Locale.ROOT));
            if (blob.contains("black_stained") || blob.contains("black stained")) {
                continue;
            }
            if (blob.contains(color)) {
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
        return text;
    }

    private static List<Integer> solveNumbers(List<TerminalItem> items) {
        record Numbered(int index, int value) {
        }
        List<Numbered> numbered = new ArrayList<>();
        for (TerminalItem item : items) {
            if (!inTerminalGrid(item.index()) || item.enchanted()) {
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

    public static List<TerminalClick> melodySkipClicks(
            MelodyState melody,
            boolean skip,
            boolean skipFirstRow,
            String skipMode) {
        if (melody == null || !melody.readyToClick() || !skip || melody.buttonRow() >= 3) {
            return List.of();
        }
        if (!skipFirstRow && melody.buttonRow() == 0) {
            return List.of();
        }
        boolean all = "All".equalsIgnoreCase(skipMode == null ? "" : skipMode.trim());
        boolean edge = melody.current() != null && (melody.current() == 0 || melody.current() == 4);
        if (!all && !edge) {
            return List.of();
        }
        List<TerminalClick> extra = new ArrayList<>();
        int slot = melody.clickSlot();
        if (melody.buttonRow() < 3) {
            extra.add(new TerminalClick(slot + 9, 0));
        }
        if (melody.buttonRow() < 2) {
            extra.add(new TerminalClick(slot + 18, 0));
        }
        if (melody.buttonRow() < 1) {
            extra.add(new TerminalClick(slot + 27, 0));
        }
        return List.copyOf(extra);
    }

    public static String normalizeMelodySkipMode(String value) {
        if (value != null && value.trim().equalsIgnoreCase("All")) {
            return "All";
        }
        return "Edges";
    }

    public static String normalizeI4LeapClass(String value) {
        if (value == null || value.isBlank()) {
            return "Tank";
        }
        String key = value.trim();
        for (String option : I4_LEAP_CLASSES) {
            if (option.equalsIgnoreCase(key)) {
                return option;
            }
        }
        return "Tank";
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

    public static boolean isRagnarockCancelled(String line) {
        return normalize(line).toLowerCase(Locale.ROOT).contains("ragnarock was cancelled");
    }

    public static Optional<Integer> ragnarockStrength(String loreLine) {
        Matcher matcher = Pattern.compile("(?i)strength:\\s*\\+(\\d+)").matcher(normalize(loreLine));
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    public static Optional<String> f7Title(String line) {
        String text = normalize(line);
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher terminals = Pattern.compile("(?i)activated a terminal!\\s*\\((\\d+/\\d+)\\)").matcher(text);
        if (terminals.find()) {
            return Optional.of("Terminal " + terminals.group(1));
        }
        if (lower.contains("completed a device")) {
            return Optional.of("Device complete");
        }
        if (lower.contains("activated a lever")) {
            return Optional.of("Lever");
        }
        if (lower.contains("[boss] maxor")) {
            return Optional.of("Maxor");
        }
        if (lower.contains("[boss] storm")) {
            return Optional.of("Storm");
        }
        if (lower.contains("[boss] goldor")) {
            return Optional.of("Goldor");
        }
        if (lower.contains("[boss] necron")) {
            return Optional.of("Necron");
        }
        if (lower.contains("energy crystal")) {
            return Optional.of("Crystal");
        }
        return Optional.empty();
    }

    public static boolean isRoomClearedChat(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("cleared a room") || text.contains("room cleared");
    }

    public static boolean isRoomSecretsChat(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("completed all secrets") || text.contains("all secrets found");
    }

    public static boolean secretCountIncreased(int previous, int current) {
        return previous >= 0 && current > previous;
    }

    public static boolean isSalvageMenu(String title) {
        return normalize(title).toLowerCase(Locale.ROOT).contains("salvage");
    }

    public static Optional<Integer> salvageQuality(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (String line : lore) {
            Matcher matcher = Pattern.compile("(?i)(?:item )?quality[:\\s]+(\\d+)").matcher(normalize(line));
            if (matcher.find()) {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

    public static boolean isPartyFinderMenu(String title) {
        String text = normalize(title).toLowerCase(Locale.ROOT);
        return text.contains("party finder") || text.contains("dungeon finder");
    }

    public static Optional<Integer> partyFinderCata(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (String line : lore) {
            Matcher matcher = Pattern.compile("(?i)(?:catacombs|cata)[:\\s]+(\\d+)").matcher(normalize(line));
            if (matcher.find()) {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

    public static boolean isDungeonChestMenu(String title) {
        String text = normalize(title).toLowerCase(Locale.ROOT);
        return text.contains("wood chest")
                || text.contains("gold chest")
                || text.contains("diamond chest")
                || text.contains("emerald chest")
                || text.contains("obsidian chest")
                || text.contains("bedrock chest");
    }

    public static boolean isChestLootSlot(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String text = normalize(line).toLowerCase(Locale.ROOT);
            if (text.contains("coins") || text.contains("essence") || text.contains("enchanted")) {
                return true;
            }
        }
        return false;
    }

    public static List<String> blessingLines(List<String> sidebarLines) {
        List<String> blessings = new ArrayList<>();
        if (sidebarLines == null) {
            return List.of();
        }
        for (String raw : sidebarLines) {
            String line = normalize(raw);
            if (line.toLowerCase(Locale.ROOT).contains("blessing of")) {
                blessings.add(line);
            }
        }
        return List.copyOf(blessings);
    }

    public static Optional<Integer> breakerCharges(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (String line : lore) {
            Optional<TempleDungeonPolicy.BreakerCharges> breaker =
                    TempleDungeonPolicy.parseBreakerCharges(line);
            if (breaker.isPresent()) {
                return Optional.of(breaker.get().current());
            }
            Matcher matcher = Pattern.compile("(?i)charges?[:\\s]+(\\d+)").matcher(normalize(line));
            if (matcher.find()) {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

    public static Optional<Integer> blazeHealth(String hologram) {
        String text = normalize(hologram).toLowerCase(Locale.ROOT);
        if (!text.contains("blaze")) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*/\\s*\\d+").matcher(text);
        if (matcher.find()) {
            return Optional.of((int) Double.parseDouble(matcher.group(1)));
        }
        matcher = Pattern.compile("(\\d+)❤").matcher(text);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    public static boolean isThreeWeirdosTruth(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("[npc]")
                && (text.contains("the reward is not in my chest")
                || text.contains("my chest doesn't have the reward"));
    }

    public static boolean isQuizCorrect(String line) {
        String text = normalize(line).toLowerCase(Locale.ROOT);
        return text.contains("[npc]") && text.contains("correct");
    }

    public static boolean isSimonStart(int x, int y, int z) {
        return x == 110 && y == 121 && z == 91;
    }

    public static boolean isF7Diorite(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("diorite");
    }

    public static boolean inF7PillarBox(int x, int y, int z) {
        return x >= 45 && x <= 115 && y >= 160 && y <= 230 && z >= 45 && z <= 145;
    }
}
