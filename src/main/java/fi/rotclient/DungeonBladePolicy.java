package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Catacombs HUD timers and chat helpers: hub warp cooldown, Oruo quiz
 * remaining time, Maxor stun, Storm crush, Last Breath window, term-start
 * countdown, secret-spawn tick, death party text and Explosive Shot
 * per-enemy. Minecraft-free for unit tests.
 */
public final class DungeonBladePolicy {
    public static final long WARP_COOLDOWN_MS = 30_000L;
    public static final int QUIZ_INTRO_TICKS = 220;
    public static final int QUIZ_QUESTION_TICKS = 100;
    public static final int MAXOR_STUN_TICKS = 240;
    public static final int STORM_CRUSH_TICKS = 20;
    public static final int STORM_LB_TICKS = 680;
    public static final int STORM_LB_DISPLAY_TICKS = 100;
    public static final int TERM_START_TICKS = 100;
    public static final int SECRET_SPAWN_TICKS = 20;
    public static final String DEFAULT_DEATH_MESSAGE = "{player} died";
    public static final String DEFAULT_MELODY_PARTY = "Melody Terminal start!";
    public static final double PREDEV_X = 1.0D;
    public static final double PREDEV_Z = 77.0D;
    public static final double PREDEV_RANGE = 3.0D;
    public static final double SS_X = 108.0D;
    public static final double SS_Y = 119.0D;
    public static final double SS_Z = 94.0D;
    public static final double SS_RANGE = 3.0D;
    public static final double PRE4_MIN_X = 63.0D;
    public static final double PRE4_MAX_X = 64.0D;
    public static final double PRE4_Y = 127.0D;
    public static final double PRE4_MIN_Z = 35.0D;
    public static final double PRE4_MAX_Z = 36.0D;
    public static final int ITEM_HIGHLIGHT_NEAR_TICKS = 11;
    public static final double ITEM_HIGHLIGHT_NEAR = 3.5D;
    public static final double ITEM_HIGHLIGHT_FAR = 20.0D;
    public static final int ITEM_HIGHLIGHT_FAR_COLOR = 0xFFFF5555;
    public static final int ITEM_HIGHLIGHT_READY_COLOR = 0xFF55FF55;
    public static final int ITEM_HIGHLIGHT_DELAY_COLOR = 0xFFFFAA00;
    public static final int CHEST_WARNING_DEFAULT = 55;
    public static final int CHEST_WARNING_MAX = 60;
    public static final int LOCATION_TICKS = 40;
    public static final String PLAYER_COUNT_TITLE = "Not enough players";
    public static final String DEVICE_COMPLETE_TITLE = "Device Completed!";
    public static final String CHEST_WARNING_TITLE = "Chest count reached";

    private static final Pattern ENTERED_CATACOMBS = Pattern.compile(
            "(?i)entered (?:MM )?\\w+ Catacombs, Floor .+!");
    private static final Pattern EXPLOSIVE_SHOT = Pattern.compile(
            "(?i)^Your Explosive Shot hit (\\d+) enemies for ([\\d,.]+) damage\\.$");
    private static final Pattern DEATH = Pattern.compile(
            "^☠ (.+) and became a ghost\\.$");
    private static final Pattern TIME_ELAPSED = Pattern.compile(
            "(?i)^Time Elapsed:");
    private static final Pattern UNCLAIMED_CHESTS = Pattern.compile(
            "(?i)^\\s*Unclaimed chests: (\\d+)$");
    private static final Pattern OWN_LEAP = Pattern.compile(
            "(?i)^You have teleported to .+!$");
    private static final Pattern STARTING = Pattern.compile(
            "(?i)^Starting in \\d+ seconds?");
    private static final Pattern CLASS_LETTER = Pattern.compile("\\[([ABHMT])]");
    private static final Pattern PARTY_LINE = Pattern.compile(
            "(?i)^Party > (?:\\[[^\\]]+]\\s*)?([A-Za-z0-9_]{1,16}):\\s*(.+)$");
    private static final Pattern PARTY_PERCENT = Pattern.compile("(\\d+)%");
    private static final Pattern LOCATION_PREFIX = Pattern.compile("^(At |Inside )");
    private static final Set<String> DUNGEON_DROPS = Set.of(
            "Revive Stone",
            "Trap",
            "Decoy",
            "Inflatable Jerry",
            "Defuse Kit",
            "Dungeon Chest Key",
            "Treasure Talisman",
            "Architect's First Draft",
            "Spirit Leap",
            "Healing VIII Splash Potion",
            "Training Weights",
            "Candycomb");

    private DungeonBladePolicy() {
    }

    public static boolean dungeonEnterChat(String chat) {
        return ENTERED_CATACOMBS.matcher(DungeonPolicy.normalize(chat)).find();
    }

    public static OptionalInt quizTimerTicks(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (text.contains("I am Oruo the Omniscient")
                && text.contains("I have learned all there is to know")) {
            return OptionalInt.of(QUIZ_INTRO_TICKS);
        }
        if (text.contains("2 questions left")) {
            return OptionalInt.of(QUIZ_QUESTION_TICKS);
        }
        if (text.contains("One more question")) {
            return OptionalInt.of(QUIZ_QUESTION_TICKS);
        }
        return OptionalInt.empty();
    }

    public static int quizStage(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (text.contains("I am Oruo the Omniscient")
                && text.contains("I have learned all there is to know")) {
            return 1;
        }
        if (text.contains("2 questions left")) {
            return 2;
        }
        if (text.contains("One more question")) {
            return 3;
        }
        return 0;
    }

    public static boolean maxorStunStart(String chat) {
        return DungeonF7Policy.crystalSpawnChat(chat);
    }

    public static boolean maxorEnraged(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return text.contains("maxor is enraged");
    }

    public static boolean stormCrushChat(String chat) {
        String text = DungeonPolicy.normalize(chat);
        return text.equals("[BOSS] Storm: Oof")
                || text.equals("[BOSS] Storm: Ouch, that hurt!");
    }

    public static OptionalDouble explosiveShotPerEnemy(String chat) {
        Matcher matcher = EXPLOSIVE_SHOT.matcher(DungeonPolicy.normalize(chat));
        if (!matcher.matches()) {
            return OptionalDouble.empty();
        }
        try {
            int count = Integer.parseInt(matcher.group(1));
            if (count <= 0) {
                return OptionalDouble.empty();
            }
            double total = Double.parseDouble(matcher.group(2).replace(",", ""));
            return OptionalDouble.of(total / count);
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    public static String quizHudLine(int stage, int ticks) {
        if (ticks <= 0) {
            return "";
        }
        String time = String.format(Locale.ROOT, "%.1fs", ticks * 0.05D);
        if (stage >= 1 && stage <= 3) {
            return "Quiz " + stage + "/3 " + time;
        }
        return "Quiz " + time;
    }

    public static String warpHudLine(long remainingMs) {
        if (remainingMs <= 0L) {
            return "";
        }
        return String.format(Locale.ROOT, "Warp %.1fs", remainingMs / 1000.0D);
    }

    public static Optional<String> deathPlayer(String chat) {
        Matcher matcher = DEATH.matcher(DungeonPolicy.normalize(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String body = matcher.group(1).trim();
        if (body.regionMatches(true, 0, "You ", 0, 4)
                || body.regionMatches(true, 0, "Your ", 0, 5)
                || body.equalsIgnoreCase("You")
                || body.equalsIgnoreCase("Your")) {
            return Optional.of("You");
        }
        int space = body.indexOf(' ');
        String name = space < 0 ? body : body.substring(0, space);
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }

    public static String deathPartyMessage(String template, String player) {
        String name = player == null || player.isBlank() ? "Someone" : player;
        String text = template == null || template.isBlank() ? DEFAULT_DEATH_MESSAGE : template;
        return text.replace("{player}", name).replace("<player>", name).trim();
    }

    public static String explosiveShotHudLine(double perEnemy) {
        if (perEnemy <= 0.0D) {
            return "";
        }
        return String.format(Locale.ROOT, "Explosive %.0f / enemy", perEnemy);
    }

    public static boolean timeElapsedTab(String line) {
        return TIME_ELAPSED.matcher(DungeonPolicy.normalize(line)).find();
    }

    public static boolean timeElapsedPositive(String line) {
        if (!timeElapsedTab(line)) {
            return false;
        }
        String value = DungeonPolicy.normalize(line)
                .replaceFirst("(?i)^Time Elapsed:\\s*", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return false;
        }
        return !value.equals("0s")
                && !value.equals("0")
                && !value.equals("0m")
                && !value.equals("0m 0s")
                && !value.equals("0:00")
                && !value.equals("00:00")
                && !value.equals("0m 00s");
    }

    public static String secretSpawnHudLine(int ticks) {
        if (ticks <= 0) {
            return "";
        }
        return "Secrets " + ticks;
    }

    public static OptionalInt unclaimedChests(String line) {
        Matcher matcher = UNCLAIMED_CHESTS.matcher(DungeonPolicy.normalize(line));
        if (!matcher.matches()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    public static int stormLbRemaining(int elapsedTicks) {
        return STORM_LB_TICKS - Math.max(0, elapsedTicks);
    }

    public static String stormLbHudLine(int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks > STORM_LB_DISPLAY_TICKS) {
            return "";
        }
        return String.format(Locale.ROOT, "Last Breath %.1fs", remainingTicks * 0.05D);
    }

    public static String termStartHudLine(int ticks) {
        if (ticks <= 0) {
            return "";
        }
        return String.format(Locale.ROOT, "Terms %.1fs", ticks * 0.05D);
    }

    public static String melodyPartyMessage(String template) {
        String text = template == null || template.isBlank() ? DEFAULT_MELODY_PARTY : template.trim();
        return text;
    }

    public static OptionalInt melodyClayRow(List<DungeonPolicy.TerminalItem> items) {
        if (items == null) {
            return OptionalInt.empty();
        }
        for (DungeonPolicy.TerminalItem item : items) {
            if (item.itemId().toLowerCase(Locale.ROOT).contains("lime_terracotta")) {
                return OptionalInt.of(item.index() / 9);
            }
        }
        return OptionalInt.empty();
    }

    public static Optional<String> melodyProgressParty(int clayRow) {
        return switch (clayRow) {
            case 2 -> Optional.of("Melody 25%");
            case 3 -> Optional.of("Melody 50%");
            case 4 -> Optional.of("Melody 75%");
            default -> Optional.empty();
        };
    }

    public static String sectionObjectiveHud(String lastObjective, int completed, int total) {
        int safeTotal = Math.max(1, total);
        int safeDone = Math.max(0, completed);
        if (lastObjective == null || lastObjective.isBlank()) {
            return "(" + safeDone + "/" + safeTotal + ")";
        }
        return lastObjective + " (" + safeDone + "/" + safeTotal + ")";
    }

    public record PartyLocation(String username, String hud) {
    }

    public record MelodyParty(String username, int percent) {
    }

    public static Optional<MelodyParty> melodyPartyPercent(String chat) {
        Matcher matcher = PARTY_LINE.matcher(DungeonPolicy.normalize(chat));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Matcher percent = PARTY_PERCENT.matcher(matcher.group(2));
        if (!percent.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MelodyParty(matcher.group(1), Integer.parseInt(percent.group(1))));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static int melodyQuarters(int percent) {
        return Math.min(Math.max(percent, 0) / 25, 3);
    }

    public static String melodyTeammateHud(String displayName, int percent) {
        String name = displayName == null || displayName.isBlank() ? "Someone" : displayName;
        return name + " has melody! " + melodyQuarters(percent) + "/4";
    }

    public static boolean atThirdDevice(double x, double z) {
        double dx = x - PREDEV_X;
        double dz = z - PREDEV_Z;
        return Math.hypot(dx, dz) <= PREDEV_RANGE;
    }

    public static boolean atSimonSays(double x, double y, double z) {
        double dx = x - SS_X;
        double dy = y - SS_Y;
        double dz = z - SS_Z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= SS_RANGE;
    }

    public static boolean atFourthDevice(double x, double y, double z) {
        return x >= PRE4_MIN_X
                && x <= PRE4_MAX_X
                && Math.abs(y - PRE4_Y) < 0.6D
                && z >= PRE4_MIN_Z
                && z <= PRE4_MAX_Z;
    }

    public static boolean dungeonDropName(String name) {
        return DUNGEON_DROPS.contains(DungeonPolicy.normalize(name));
    }

    public static boolean highlightDungeonDrop(boolean enabled, boolean inDungeon, boolean inBoss, String name) {
        return enabled && inDungeon && !inBoss && dungeonDropName(name);
    }

    public static int dungeonDropColor(double distance, int ageTicks) {
        if (distance > ITEM_HIGHLIGHT_FAR) {
            return 0;
        }
        if (distance > ITEM_HIGHLIGHT_NEAR) {
            return ITEM_HIGHLIGHT_FAR_COLOR;
        }
        return ageTicks > ITEM_HIGHLIGHT_NEAR_TICKS
                ? ITEM_HIGHLIGHT_READY_COLOR
                : ITEM_HIGHLIGHT_DELAY_COLOR;
    }

    public static boolean ownLeapChat(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (OWN_LEAP.matcher(text).matches()) {
            return true;
        }
        return text.toLowerCase(Locale.ROOT).startsWith("you leaped to");
    }

    public static boolean startingCountdown(String chat) {
        return STARTING.matcher(DungeonPolicy.normalize(chat)).find();
    }

    public static int scoreboardClassCount(List<String> lines) {
        if (lines == null) {
            return 0;
        }
        int count = 0;
        for (String line : lines) {
            if (CLASS_LETTER.matcher(DungeonPolicy.normalize(line)).find()) {
                count++;
            }
        }
        return count;
    }

    public static boolean notEnoughPlayers(int classCount) {
        return classCount > 0 && classCount < 5;
    }

    public static boolean shouldTrackPredev(boolean predevEnabled, boolean predevAll, boolean healer) {
        return predevEnabled && (predevAll || healer);
    }

    public static String predevHudLine(long elapsedMs, long pbMs, boolean showPb) {
        if (elapsedMs < 0L) {
            return "";
        }
        String line = String.format(Locale.ROOT, "Predev %.2fs", elapsedMs / 1000.0D);
        if (showPb && pbMs > 0L) {
            line += String.format(Locale.ROOT, " (PB %.2fs)", pbMs / 1000.0D);
        }
        return line;
    }

    public static int clampChestWarning(int value) {
        return Math.max(1, Math.min(CHEST_WARNING_MAX, value));
    }

    public static boolean chestWarningReached(int hubUnclaimed, int runChests, int threshold) {
        if (hubUnclaimed < 0) {
            return false;
        }
        return hubUnclaimed + Math.max(0, runChests) >= clampChestWarning(threshold);
    }

    public static Optional<PartyLocation> partyLocation(String chat) {
        Matcher matcher = PARTY_LINE.matcher(DungeonPolicy.normalize(chat));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String body = matcher.group(2).strip();
        String formatted = formatLocation(body);
        Matcher prefix = LOCATION_PREFIX.matcher(formatted);
        if (!prefix.find()) {
            return Optional.empty();
        }
        String action = prefix.group(1);
        int index = body.indexOf(action);
        if (index < 0) {
            index = formatted.indexOf(action);
            if (index < 0) {
                return Optional.empty();
            }
            String location = formatted.substring(index + action.length());
            return Optional.of(new PartyLocation(
                    matcher.group(1),
                    matcher.group(1) + " is " + action + location + "!"));
        }
        String location = body.substring(index + action.length()).replaceAll("§.", "");
        return Optional.of(new PartyLocation(
                matcher.group(1),
                matcher.group(1) + " is " + action + location + "!"));
    }

    private static String formatLocation(String message) {
        String stripped = message == null ? "" : message.strip();
        if (stripped.length() < 2) {
            return message == null ? "" : message;
        }
        return Character.toUpperCase(stripped.charAt(0))
                + stripped.substring(1).toLowerCase(Locale.ROOT);
    }
}
