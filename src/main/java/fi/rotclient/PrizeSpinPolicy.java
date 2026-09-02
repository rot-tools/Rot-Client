package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Visual prize-roll helpers for dungeon reward chests and Vanguard chat dumps.
 * Overlay-only; does not click, buy, or send packets.
 */
public final class PrizeSpinPolicy {
    public static final int SPIN_MILLIS = 2_400;
    public static final int REEL_LENGTH = 28;
    public static final String CHEST_WOOD = "Wood";
    public static final String CHEST_GOLD = "Gold";
    public static final String CHEST_DIAMOND = "Diamond";
    public static final String CHEST_EMERALD = "Emerald";
    public static final String CHEST_OBSIDIAN = "Obsidian";
    public static final String CHEST_BEDROCK = "Bedrock";

    private static final Pattern CHEST_TITLE = Pattern.compile(
            "(?i)\\b(wood|golden?|diamond|emerald|obsidian|bedrock)\\s+chest\\b");
    private static final Pattern VANGUARD_START = Pattern.compile(
            "(?i)\\bvanguard\\b.*(reward|loot|found|received|rolling|spin)");
    private static final Pattern VANGUARD_ITEM = Pattern.compile(
            "(?i)^\\s*(?:\\d+\\.\\s+|[+•\\-*]\\s+)(.+?)\\s*$");
    private static final Pattern VANGUARD_END = Pattern.compile(
            "(?i)(click to|collected|closed|enjoy your|unclaimed)");

    private PrizeSpinPolicy() {
    }

    public static Optional<String> rewardChestType(String title) {
        if (title == null) {
            return Optional.empty();
        }
        Matcher matcher = CHEST_TITLE.matcher(strip(title));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String raw = matcher.group(1).toLowerCase(Locale.ROOT);
        return Optional.of(switch (raw) {
            case "gold", "golden" -> CHEST_GOLD;
            case "diamond" -> CHEST_DIAMOND;
            case "emerald" -> CHEST_EMERALD;
            case "obsidian" -> CHEST_OBSIDIAN;
            case "bedrock" -> CHEST_BEDROCK;
            default -> CHEST_WOOD;
        });
    }

    public static boolean isFillerName(String name) {
        String text = strip(name).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return true;
        }
        return text.contains("stained glass")
                || text.endsWith("glass pane")
                || text.equals("barrier")
                || text.equals("black stained glass pane")
                || text.contains("click to open")
                || text.contains("close")
                || text.equals("arrow")
                || text.equals("paper");
    }

    public static List<String> lootNames(List<String> slotNames) {
        List<String> loot = new ArrayList<>();
        if (slotNames == null) {
            return loot;
        }
        for (String name : slotNames) {
            if (!isFillerName(name)) {
                loot.add(strip(name));
            }
        }
        return loot;
    }

    public static List<String> spinReel(List<String> loot, int winnerIndex, int length) {
        List<String> reel = new ArrayList<>();
        if (loot == null || loot.isEmpty() || length <= 0) {
            return reel;
        }
        int winner = Math.floorMod(winnerIndex, loot.size());
        for (int i = 0; i < length; i++) {
            reel.add(loot.get(i % loot.size()));
        }
        reel.set(length - 1, loot.get(winner));
        return reel;
    }

    public static double easeOutCubic(double t) {
        double x = t < 0.0D ? 0.0D : Math.min(1.0D, t);
        double inv = 1.0D - x;
        return 1.0D - inv * inv * inv;
    }

    public static int focusedIndex(double progress, int length) {
        if (length <= 1) {
            return 0;
        }
        double eased = easeOutCubic(progress);
        int index = (int) Math.floor(eased * (length - 1));
        return Math.max(0, Math.min(length - 1, index));
    }

    public static ChatKind classifyVanguardLine(boolean collecting, String line) {
        String text = strip(line);
        if (text.isBlank()) {
            return collecting ? ChatKind.IGNORE : ChatKind.IGNORE;
        }
        if (VANGUARD_START.matcher(text).find()) {
            return ChatKind.START;
        }
        if (collecting && VANGUARD_END.matcher(text).find()) {
            return ChatKind.END;
        }
        if (collecting && VANGUARD_ITEM.matcher(text).matches()) {
            return ChatKind.ITEM;
        }
        return ChatKind.IGNORE;
    }

    public static String vanguardItemName(String line) {
        Matcher matcher = VANGUARD_ITEM.matcher(strip(line));
        return matcher.matches() ? strip(matcher.group(1)) : "";
    }

    private static String strip(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
    }

    public enum ChatKind {
        START,
        ITEM,
        END,
        IGNORE
    }
}
