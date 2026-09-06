package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Party Finder lore status, join-stat thresholds, and kick decisions.
 */
public final class DungeonPartyFinderPolicy {
    public enum SlotStatus {
        NONE,
        JOINABLE,
        DUPE,
        BLOCKED,
        VC,
        PERM,
        CARRY
    }

    public record Stats(long secrets, double secretAverage, int magicalPower, int pbSeconds) {
        public Stats {
            secrets = Math.max(0L, secrets);
            secretAverage = Double.isFinite(secretAverage) ? Math.max(0.0D, secretAverage) : 0.0D;
            magicalPower = Math.max(0, magicalPower);
            pbSeconds = Math.max(0, pbSeconds);
        }
    }

    public record KickThresholds(
            String requiredPb, String requiredSecrets, String requiredAverage, String requiredMp) {
        public KickThresholds {
            requiredPb = requiredPb == null ? "" : requiredPb.trim();
            requiredSecrets = requiredSecrets == null ? "" : requiredSecrets.trim();
            requiredAverage = requiredAverage == null ? "" : requiredAverage.trim();
            requiredMp = requiredMp == null ? "" : requiredMp.trim();
        }
    }

    private static final Pattern JOIN = Pattern.compile(
            "(?i)^Party Finder > (?:\\[[^\\]]+]\\s*)?(\\w{3,16}) joined the dungeon group!");
    private static final Pattern TIME = Pattern.compile("(?:(\\d+):)?(\\d{1,2}):(\\d{2})");

    private DungeonPartyFinderPolicy() {
    }

    public static Optional<String> joinPlayer(String chat) {
        Matcher matcher = JOIN.matcher(DungeonPolicy.normalize(chat));
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public static SlotStatus loreStatus(List<String> lore) {
        if (lore == null) {
            return SlotStatus.NONE;
        }
        String joined = String.join(" ", lore).toLowerCase(Locale.ROOT);
        if (joined.contains("perm") || joined.contains("permanent")) {
            return SlotStatus.PERM;
        }
        if (joined.contains("carry")) {
            return SlotStatus.CARRY;
        }
        if (joined.contains("vc req") || joined.contains("require vc") || joined.contains("must vc")
                || joined.contains("voice chat")) {
            return SlotStatus.VC;
        }
        if (joined.contains("blocked") || joined.contains("blacklist") || joined.contains("no join")) {
            return SlotStatus.BLOCKED;
        }
        if (joined.contains("dupe") || joined.contains("duplicate")) {
            return SlotStatus.DUPE;
        }
        if (joined.contains("s+ ") || joined.contains("s+") || joined.contains("cata")
                || joined.contains("note:")) {
            return SlotStatus.JOINABLE;
        }
        return SlotStatus.NONE;
    }

    public static Optional<String> partyLeader(String itemName, List<String> lore) {
        String fromName = firstName(itemName);
        if (!fromName.isBlank()) {
            return Optional.of(fromName);
        }
        if (lore == null) {
            return Optional.empty();
        }
        for (String line : lore) {
            String found = firstName(line);
            if (!found.isBlank()) {
                return Optional.of(found);
            }
        }
        return Optional.empty();
    }

    public static boolean loreHasStats(List<String> lore) {
        if (lore == null) {
            return false;
        }
        String joined = String.join(" ", lore).toLowerCase(Locale.ROOT);
        return joined.contains("secrets")
                || joined.contains("secret average")
                || joined.contains("magical power")
                || joined.contains("best time")
                || joined.contains("personal best");
    }

    private static String firstName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("([A-Za-z0-9_]{3,16})").matcher(DungeonPolicy.normalize(raw));
        return matcher.find() ? matcher.group(1) : "";
    }

    public static int highlightColor(
            SlotStatus status,
            int joinable,
            int dupe,
            int blocked,
            int vc,
            int perm,
            int carry) {
        return switch (status) {
            case JOINABLE -> joinable;
            case DUPE -> dupe;
            case BLOCKED -> blocked;
            case VC -> vc;
            case PERM -> perm;
            case CARRY -> carry;
            case NONE -> 0;
        };
    }

    public static long parseCount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT).replace(",", "");
        double mul = 1.0D;
        if (text.endsWith("k")) {
            mul = 1_000.0D;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("m")) {
            mul = 1_000_000.0D;
            text = text.substring(0, text.length() - 1);
        }
        try {
            return (long) Math.round(Double.parseDouble(text) * mul);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public static int parsePbSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        Matcher matcher = TIME.matcher(raw.trim());
        if (!matcher.find()) {
            try {
                return Math.max(0, Integer.parseInt(raw.trim()));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        int minutes = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int mid = Integer.parseInt(matcher.group(2));
        int sec = Integer.parseInt(matcher.group(3));
        if (matcher.group(1) == null) {
            return mid * 60 + sec;
        }
        return minutes * 3600 + mid * 60 + sec;
    }

    public static double parseAverage(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    public static boolean shouldKick(Stats stats, KickThresholds thresholds) {
        if (stats == null || thresholds == null) {
            return false;
        }
        int pbNeed = parsePbSeconds(thresholds.requiredPb());
        if (pbNeed > 0 && (stats.pbSeconds() <= 0 || stats.pbSeconds() > pbNeed)) {
            return true;
        }
        long secretsNeed = parseCount(thresholds.requiredSecrets());
        if (secretsNeed > 0 && stats.secrets() < secretsNeed) {
            return true;
        }
        double avgNeed = parseAverage(thresholds.requiredAverage());
        if (avgNeed > 0.0D && stats.secretAverage() + 1.0e-6D < avgNeed) {
            return true;
        }
        int mpNeed = (int) parseCount(thresholds.requiredMp());
        return mpNeed > 0 && stats.magicalPower() < mpNeed;
    }

    public static String kickReason(Stats stats, KickThresholds thresholds) {
        if (!shouldKick(stats, thresholds)) {
            return "";
        }
        return "Low dungeon stats";
    }

    public static String statsLine(String player, Stats stats) {
        if (stats == null) {
            return player + " stats unavailable";
        }
        return player
                + " PB " + formatPb(stats.pbSeconds())
                + " | Secrets " + stats.secrets()
                + " | SAvg " + String.format(Locale.ROOT, "%.1f", stats.secretAverage())
                + " | MP " + stats.magicalPower();
    }

    public static String formatPb(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", mins, secs);
    }

    public static String partyKickCommand(String player) {
        String name = DungeonCarryPolicy.sanitizePlayer(player);
        return name.isBlank() ? "" : "/p kick " + name;
    }

    public static String partyKickChat(String player, String reason) {
        String name = DungeonCarryPolicy.sanitizePlayer(player);
        if (name.isBlank()) {
            return "";
        }
        if (reason == null || reason.isBlank()) {
            return "/pc Kicked " + name;
        }
        return "/pc Kicked " + name + " (" + reason + ")";
    }
}
