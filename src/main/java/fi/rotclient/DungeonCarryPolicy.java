package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dungeon-floor carry quotas, party/webhook lines, and persistence model.
 */
public final class DungeonCarryPolicy {
    public static final List<String> FLOORS = List.of(
            "E", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
            "M1", "M2", "M3", "M4", "M5", "M6", "M7");

    private static final Pattern WEBHOOK_PATH = Pattern.compile(
            "^/api/webhooks/\\d+/[A-Za-z0-9._-]+/?$");

    public static final class TrackedCarry {
        public String player = "";
        public String floor = "F7";
        public int total;
        public int completed;
        public long startedAtMillis;

        public TrackedCarry() {}

        public TrackedCarry(String player, String floor, int total) {
            this.player = sanitizePlayer(player);
            this.floor = normalizeFloor(floor);
            this.total = Math.max(1, total);
            this.startedAtMillis = System.currentTimeMillis();
        }

        public boolean done() {
            return completed >= total && total > 0;
        }

        public String hudLine() {
            return player + " " + floor + " " + completed + "/" + total;
        }
    }

    public static final class HistoryEntry {
        public String player = "";
        public String floor = "F7";
        public int total;
        public long completedAtMillis;
        public long durationMillis;

        public HistoryEntry() {}
    }

    public record Snapshot(List<TrackedCarry> active, List<HistoryEntry> history) {
        public Snapshot {
            active = active == null ? List.of() : List.copyOf(active);
            history = history == null ? List.of() : List.copyOf(history);
        }
    }

    private DungeonCarryPolicy() {
    }

    public static String sanitizePlayer(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.replaceAll("[^A-Za-z0-9_]", "");
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        return name;
    }

    public static String normalizeFloor(String raw) {
        if (raw == null || raw.isBlank()) {
            return "F7";
        }
        String text = raw.trim().toUpperCase(Locale.ROOT);
        if ("E".equals(text) || "ENTRANCE".equals(text)) {
            return "E";
        }
        if (text.matches("[FM][1-7]")) {
            return text;
        }
        return "F7";
    }

    public static boolean matchesFloor(String tracked, String current) {
        return normalizeFloor(tracked).equals(normalizeFloor(current));
    }

    public static Optional<TrackedCarry> find(List<TrackedCarry> carries, String player) {
        String name = sanitizePlayer(player).toLowerCase(Locale.ROOT);
        if (name.isBlank() || carries == null) {
            return Optional.empty();
        }
        for (TrackedCarry carry : carries) {
            if (carry != null && name.equalsIgnoreCase(carry.player)) {
                return Optional.of(carry);
            }
        }
        return Optional.empty();
    }

    public static List<TrackedCarry> add(
            List<TrackedCarry> carries, String player, String floor, int total) {
        String name = sanitizePlayer(player);
        if (name.isBlank()) {
            return carries == null ? List.of() : List.copyOf(carries);
        }
        List<TrackedCarry> out = new ArrayList<>();
        if (carries != null) {
            for (TrackedCarry carry : carries) {
                if (carry != null && !name.equalsIgnoreCase(carry.player)) {
                    out.add(carry);
                }
            }
        }
        out.add(new TrackedCarry(name, floor, total));
        return List.copyOf(out);
    }

    public static List<TrackedCarry> remove(List<TrackedCarry> carries, String player) {
        String name = sanitizePlayer(player);
        List<TrackedCarry> out = new ArrayList<>();
        if (carries != null) {
            for (TrackedCarry carry : carries) {
                if (carry != null && !name.equalsIgnoreCase(carry.player)) {
                    out.add(carry);
                }
            }
        }
        return List.copyOf(out);
    }

    public static boolean increment(
            List<TrackedCarry> carries, String player, String floor) {
        Optional<TrackedCarry> found = find(carries, player);
        if (found.isEmpty()) {
            return false;
        }
        TrackedCarry carry = found.get();
        if (!matchesFloor(carry.floor, floor) || carry.done()) {
            return false;
        }
        carry.completed = Math.min(carry.total, carry.completed + 1);
        return true;
    }

    public static String startMessage(TrackedCarry carry) {
        if (carry == null) {
            return "";
        }
        return "Dungeon carry started: " + carry.player + " " + carry.floor + " x" + carry.total;
    }

    public static String progressMessage(TrackedCarry carry) {
        if (carry == null) {
            return "";
        }
        return carry.player + " " + carry.floor + " " + carry.completed + "/" + carry.total;
    }

    public static String completeMessage(TrackedCarry carry) {
        if (carry == null) {
            return "";
        }
        return "Dungeon carry complete: " + carry.player + " " + carry.floor;
    }

    public static String sanitizeWebhookUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        try {
            java.net.URI uri = java.net.URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"discord.com".equals(host) && !"discordapp.com".equals(host)) {
                return "";
            }
            if (!WEBHOOK_PATH.matcher(uri.getPath() == null ? "" : uri.getPath()).matches()) {
                return "";
            }
            return value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public static List<String> hudLines(List<TrackedCarry> carries, boolean onlyDungeons, boolean inDungeon) {
        if (onlyDungeons && !inDungeon) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("Dungeon Carry");
        if (carries == null || carries.isEmpty()) {
            lines.add("None");
            return lines;
        }
        for (TrackedCarry carry : carries) {
            if (carry != null && !carry.player.isBlank()) {
                lines.add(carry.hudLine());
            }
        }
        return lines;
    }

    public static String writeSnapshot(Snapshot snapshot) {
        Snapshot value = snapshot == null ? new Snapshot(List.of(), List.of()) : snapshot;
        return new com.google.gson.Gson().toJson(value);
    }

    public static Snapshot readSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return new Snapshot(List.of(), List.of());
        }
        try {
            Snapshot parsed = new com.google.gson.Gson().fromJson(json, Snapshot.class);
            if (parsed == null) {
                return new Snapshot(List.of(), List.of());
            }
            return new Snapshot(parsed.active(), parsed.history());
        } catch (RuntimeException ignored) {
            return new Snapshot(List.of(), List.of());
        }
    }

    public static Matcher joinChat(String line) {
        return Pattern.compile(
                "(?i)^Party Finder > (?:\\[[^\\]]+]\\s*)?(\\w{3,16}) joined the dungeon group!.*$")
                .matcher(DungeonPolicy.normalize(line));
    }
}
