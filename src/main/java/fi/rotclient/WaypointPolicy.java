package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure chat-coordinate waypoint rules: parse party/all pings, reject out of
 * bounds / duplicates, and expire temporary markers.
 */
public final class WaypointPolicy {
    public static final int COORD_BOUND = 5000;
    public static final long CHAT_DURATION_MS = 60_000L;
    public static final long PING_DURATION_MS = 15_000L;
    public static final double PING_DISTANCE = 64.0D;
    public static final String PING_OFF = "Off";
    public static final String PING_LOOK_TARGET = "Look Target";

    public record ChatPing(String sender, int x, int y, int z, boolean fromParty) {
    }

    public record Marker(
            String name,
            int x,
            int y,
            int z,
            int colorArgb,
            long addedAtMs,
            long durationMs) {
        public boolean expired(long nowMs) {
            return nowMs > addedAtMs + durationMs;
        }
    }

    public enum AddStatus {
        ADDED,
        OUT_OF_BOUNDS,
        DUPLICATE,
        DISABLED
    }

    public record AddResult(AddStatus status, Marker marker) {
    }

    private static final Pattern PARTY_PREFIX = Pattern.compile("^Party > ", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARTY_PING = Pattern.compile(
            "^Party > (?:\\[[^]]*])?\\s*(\\w{1,16})(?:\\s+\\S)?\\s*:\\s*x:\\s*(-?\\d+),\\s*y:\\s*(-?\\d+),\\s*z:\\s*(-?\\d+).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ALL_PING = Pattern.compile(
            "^(?!Party >)(?:.*\\s)?(?:\\[[^]]*])?\\s*(\\w{1,16})(?:\\s+\\S)?\\s*:\\s*x:\\s*(-?\\d+),?\\s*y:\\s*(-?\\d+),?\\s*z:\\s*(-?\\d+).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final int[] COLORS = {
            0xFFE33B3B,
            0xFF4CC9B0,
            0xFF4C9EFF,
            0xFFFFC857,
            0xFF35D167,
            0xFFB07CFF,
            0xFFFF8A5B,
            0xFF7AD7FF,
            0xFFF3F7FB,
            0xFFE91E8C,
            0xFF00AAAA,
            0xFF8BC34A,
            0xFFFFEB3B
    };

    private WaypointPolicy() {
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        return FORMATTING.matcher(raw).replaceAll("").trim();
    }

    public static String normalizePingMode(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("Default")
                || raw.equalsIgnoreCase(PING_OFF)) {
            return PING_OFF;
        }
        if (raw.equalsIgnoreCase(PING_LOOK_TARGET) || raw.equalsIgnoreCase("On")) {
            return PING_LOOK_TARGET;
        }
        return PING_OFF;
    }

    public static boolean lookTargetPingEnabled(String pingMode) {
        return PING_LOOK_TARGET.equals(normalizePingMode(pingMode));
    }

    public static Optional<ChatPing> parseChat(
            String raw,
            boolean fromPartyEnabled,
            boolean fromAllEnabled) {
        String value = stripFormatting(raw);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        Matcher party = PARTY_PING.matcher(value);
        if (party.matches()) {
            if (!fromPartyEnabled) {
                return Optional.empty();
            }
            return Optional.of(new ChatPing(
                    party.group(1),
                    Integer.parseInt(party.group(2)),
                    Integer.parseInt(party.group(3)),
                    Integer.parseInt(party.group(4)),
                    true));
        }
        if (!fromAllEnabled || PARTY_PREFIX.matcher(value).find()) {
            return Optional.empty();
        }
        Matcher all = ALL_PING.matcher(value);
        if (!all.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ChatPing(
                all.group(1),
                Integer.parseInt(all.group(2)),
                Integer.parseInt(all.group(3)),
                Integer.parseInt(all.group(4)),
                false));
    }

    public static boolean shouldAcceptOwnPing(
            String sender,
            String localName,
            boolean personalEnabled) {
        if (personalEnabled || sender == null || localName == null) {
            return true;
        }
        return !sender.equalsIgnoreCase(localName.trim());
    }

    public static boolean inBounds(int x, int y, int z) {
        return Math.abs(x) <= COORD_BOUND
                && Math.abs(y) <= COORD_BOUND
                && Math.abs(z) <= COORD_BOUND;
    }

    public static boolean isDuplicate(List<Marker> existing, int x, int y, int z) {
        if (existing == null) {
            return false;
        }
        for (Marker marker : existing) {
            if (marker.x() == x && marker.y() == y && marker.z() == z) {
                return true;
            }
        }
        return false;
    }

    public static AddResult add(
            boolean moduleEnabled,
            List<Marker> existing,
            String name,
            int x,
            int y,
            int z,
            long nowMs,
            long durationMs,
            int colorIndex) {
        if (!moduleEnabled) {
            return new AddResult(AddStatus.DISABLED, null);
        }
        if (!inBounds(x, y, z)) {
            return new AddResult(AddStatus.OUT_OF_BOUNDS, null);
        }
        if (isDuplicate(existing, x, y, z)) {
            return new AddResult(AddStatus.DUPLICATE, null);
        }
        int color = COLORS[Math.floorMod(colorIndex, COLORS.length)];
        Marker marker = new Marker(
                name == null || name.isBlank() ? "Waypoint" : name,
                x,
                y,
                z,
                color,
                nowMs,
                Math.max(0L, durationMs));
        return new AddResult(AddStatus.ADDED, marker);
    }

    public static int manhattan(int x, int y, int z, int px, int py, int pz) {
        return Math.abs(x - px) + Math.abs(y - py) + Math.abs(z - pz);
    }

    public static String chatName(String sender) {
        return sender == null || sender.isBlank() ? "Waypoint" : sender;
    }
}
