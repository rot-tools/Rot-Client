package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto GFS: {@code /gfs &lt;sack item&gt; &lt;missing&gt;} when the
 * inventory already holds that SkyBlock id. Dungeon-start Mort lines, a timer,
 * and Architect's First Draft on the local player's puzzle fail use the real
 * Hypixel chat / NBT ids.
 */
public final class AutoGfsPolicy {
    public static final int MIN_TIMER_SECONDS = 1;
    public static final int MAX_TIMER_SECONDS = 60;
    public static final int DEFAULT_TIMER_SECONDS = 5;
    public static final String DRAFT_COMMAND = "gfs architect's first draft 1";

    public record SackItem(String skyblockId, String sackName, int stackSize) {
    }

    public static final SackItem LEAP = new SackItem("SPIRIT_LEAP", "spirit_leap", 16);
    public static final SackItem PEARL = new SackItem("ENDER_PEARL", "ender_pearl", 16);
    public static final SackItem JERRY = new SackItem("INFLATABLE_JERRY", "inflatable_jerry", 64);
    public static final SackItem TNT = new SackItem("SUPERBOOM_TNT", "superboom_tnt", 64);
    public static final SackItem TWILIGHT = new SackItem(
            "TWILIGHT_ARROW_POISON", "twilight_arrow_poison", 64);

    private static final Pattern PUZZLE_FAIL = Pattern.compile(
            "^PUZZLE FAIL! (\\w{1,16}) .+$|^\\[STATUE] Oruo the Omniscient: (\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$");
    private static final Pattern DUNGEON_START = Pattern.compile(
            "\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!");
    private static final Pattern CONTROL = Pattern.compile("§.");

    private AutoGfsPolicy() {
    }

    public static int clampTimerSeconds(int value) {
        return Math.max(MIN_TIMER_SECONDS, Math.min(MAX_TIMER_SECONDS, value));
    }

    public static boolean locationAllowsRefill(
            boolean inSkyblockSetting,
            boolean inKuudraSetting,
            boolean inDungeonSetting,
            boolean isSkyblock,
            boolean isKuudra,
            boolean isDungeon) {
        if (inSkyblockSetting) {
            return isSkyblock;
        }
        if (inKuudraSetting && isKuudra) {
            return true;
        }
        return inDungeonSetting && isDungeon;
    }

    public static boolean isKuudraSidebar(String sidebar) {
        String text = strip(sidebar).toLowerCase(Locale.ROOT);
        return text.contains("kuudra");
    }

    /**
     * Only a stack that is already present is refilled. Missing item → no
     * {@code /gfs}. Full stack → no {@code /gfs}.
     */
    public static Optional<String> gfsCommand(boolean present, int currentCount, SackItem item) {
        if (item == null || !present) {
            return Optional.empty();
        }
        int count = Math.max(0, currentCount);
        if (count == item.stackSize()) {
            return Optional.empty();
        }
        int need = item.stackSize() - count;
        if (need <= 0) {
            return Optional.empty();
        }
        return Optional.of("gfs " + item.sackName() + " " + need);
    }

    public static boolean isDungeonStart(String raw) {
        return DUNGEON_START.matcher(strip(raw)).find();
    }

    public static boolean isLocalPuzzleFail(String raw, String localName) {
        if (localName == null || localName.isBlank()) {
            return false;
        }
        Matcher matcher = PUZZLE_FAIL.matcher(strip(raw));
        if (!matcher.matches()) {
            return false;
        }
        String failed = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return localName.equalsIgnoreCase(failed);
    }

    public static boolean timerReady(int tickCount, int lastTick, int timerSeconds) {
        int interval = clampTimerSeconds(timerSeconds) * 20;
        return tickCount - lastTick >= interval;
    }

    public static boolean idMatches(String skyblockId, SackItem item) {
        if (item == null || skyblockId == null) {
            return false;
        }
        return item.skyblockId().equalsIgnoreCase(skyblockId.trim());
    }

    public static List<SackItem> enabledItems(
            boolean pearl, boolean jerry, boolean tnt, boolean leap, boolean twilight) {
        return List.of(LEAP, PEARL, JERRY, TNT, TWILIGHT).stream()
                .filter(item -> (item == LEAP && leap)
                        || (item == PEARL && pearl)
                        || (item == JERRY && jerry)
                        || (item == TNT && tnt)
                        || (item == TWILIGHT && twilight))
                .toList();
    }

    public static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return CONTROL.matcher(raw).replaceAll("").trim();
    }
}
