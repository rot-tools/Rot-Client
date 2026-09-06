package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watcher blood-camp timings and speed-bucket alerts.
 */
public final class DungeonWatcherPolicy {
    public enum Speed {
        NONE,
        FAST,
        NORMAL,
        SLOW,
        VERY_SLOW
    }

    public record Timers(long speakMs, long moveMs, long spawnedMs, boolean complete) {
        public List<String> hudLines(boolean showTicks) {
            List<String> lines = new ArrayList<>();
            lines.add("Blood timers");
            if (speakMs > 0L) {
                lines.add("Speak: " + format(speakMs, showTicks));
            }
            if (moveMs > 0L) {
                lines.add("Move: " + format(moveMs, showTicks));
            }
            if (spawnedMs > 0L) {
                lines.add("Total: " + format(spawnedMs, showTicks));
            }
            return lines;
        }
    }

    public static final String SPEAK_CHAT = "[BOSS] The Watcher: Let's see how you can handle this.";
    public static final String BLOOD_DOOR = "The BLOOD DOOR has been opened!";
    public static final String ALL_SPAWNED = "[BOSS] The Watcher: You have proven yourself. You may pass.";
    public static final String ALL_SPAWNED_ALT = "[BOSS] The Watcher: That will be enough for now.";

    private static final Pattern WATCHER_NAME = Pattern.compile("(?i)the watcher");

    private DungeonWatcherPolicy() {
    }

    public static boolean bloodDoor(String chat) {
        String text = DungeonPolicy.normalize(chat);
        return text.toLowerCase(Locale.ROOT).contains("blood door") && text.toLowerCase(Locale.ROOT).contains("opened");
    }

    public static boolean speakChat(String chat) {
        return DungeonPolicy.normalize(chat).equalsIgnoreCase(SPEAK_CHAT)
                || DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT)
                .contains("let's see how you can handle this");
    }

    public static boolean allSpawnedChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return text.contains("[boss] the watcher")
                && (text.contains("you have proven yourself")
                || text.contains("that will be enough")
                || text.contains("you may pass"));
    }

    public static boolean watcherHologram(String name) {
        return name != null && WATCHER_NAME.matcher(name).find();
    }

    public static Speed speedForSpeak(long speakMs) {
        if (speakMs <= 0L) {
            return Speed.NONE;
        }
        if (speakMs < 4_000L) {
            return Speed.FAST;
        }
        if (speakMs < 7_000L) {
            return Speed.NORMAL;
        }
        if (speakMs < 11_000L) {
            return Speed.SLOW;
        }
        return Speed.VERY_SLOW;
    }

    public static String alertText(
            Speed speed, String fast, String normal, String slow, String verySlow) {
        return switch (speed) {
            case FAST -> DungeonAthenPortPolicy.applyMcCodes(fast);
            case NORMAL -> DungeonAthenPortPolicy.applyMcCodes(normal);
            case SLOW -> DungeonAthenPortPolicy.applyMcCodes(slow);
            case VERY_SLOW -> DungeonAthenPortPolicy.applyMcCodes(verySlow);
            case NONE -> "";
        };
    }

    public static String format(long millis, boolean ticks) {
        if (millis < 0L) {
            millis = 0L;
        }
        if (ticks) {
            return (millis / 50L) + "t";
        }
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0D);
    }

    public static String breakdownLine(String label, long millis, boolean ticks) {
        return "Watcher " + label + ": " + format(millis, ticks);
    }

    public static Optional<String> partyFinderJoinName(String chat) {
        Matcher matcher = Pattern.compile(
                "(?i)^Party Finder > (?:\\[[^\\]]+]\\s*)?(\\w{3,16}) joined the dungeon group!")
                .matcher(DungeonPolicy.normalize(chat));
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }
}
