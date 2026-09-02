package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kuudra Fresh Tools, build-progress, and phase-title decisions. Minecraft
 * drawing and party chat live in the client runtime.
 */
public final class KuudraAlertPolicy {
    public static final long FRESH_DURATION_MS = 10_000L;
    public static final String PARTY_FRESH = "FRESH";

    private static final Pattern CONTROL = Pattern.compile("§.");
    private static final Pattern PARTY_CHAT =
            Pattern.compile("^Party > (?:\\[[^]]+] )?([A-Za-z0-9_]{1,16}):\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILD_CHAT = Pattern.compile(
            "Building Progress\\s+(\\d+)%\\s+\\((\\d+)\\s+Players? Helping\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILD_PROGRESS = Pattern.compile("PROGRESS:\\s*(\\d+)%", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_BUILD = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)", Pattern.CASE_INSENSITIVE);

    public record BuildInfo(int percent, int helpers) {
    }

    public record FreshEntry(String name, long expiresAtMs) {
    }

    public record Snapshot(
            long ownFreshUntilMs,
            Map<String, Long> partyFreshUntilMs,
            int buildPercent,
            int buildHelpers) {
        public Snapshot {
            partyFreshUntilMs = partyFreshUntilMs == null
                    ? Map.of()
                    : Map.copyOf(partyFreshUntilMs);
        }

        public static Snapshot idle() {
            return new Snapshot(0L, Map.of(), -1, -1);
        }
    }

    private KuudraAlertPolicy() {
    }

    public static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return CONTROL.matcher(raw).replaceAll("").trim();
    }

    public static boolean isOwnFresh(String chat) {
        String text = strip(chat).toLowerCase(Locale.ROOT);
        return text.contains("fresh tools perk") && text.contains("10 seconds");
    }

    public static Optional<String> partyFreshName(String chat) {
        Matcher matcher = PARTY_CHAT.matcher(strip(chat));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String body = matcher.group(2) == null ? "" : matcher.group(2).trim();
        if (!PARTY_FRESH.equalsIgnoreCase(body)) {
            return Optional.empty();
        }
        String name = matcher.group(1);
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    public static Optional<BuildInfo> parseBuild(String text) {
        String plain = strip(text);
        if (plain.isBlank()) {
            return Optional.empty();
        }
        Matcher helpers = BUILD_CHAT.matcher(plain);
        if (helpers.find()) {
            return Optional.of(new BuildInfo(
                    clampPercent(parseInt(helpers.group(1))),
                    Math.max(0, parseInt(helpers.group(2)))));
        }
        Matcher progress = BUILD_PROGRESS.matcher(plain);
        if (progress.find()) {
            return Optional.of(new BuildInfo(clampPercent(parseInt(progress.group(1))), -1));
        }
        Matcher scoreboard = SCOREBOARD_BUILD.matcher(plain);
        if (scoreboard.find()) {
            return Optional.of(new BuildInfo(clampPercent(parseInt(scoreboard.group(1))), -1));
        }
        return Optional.empty();
    }

    public static Snapshot applyOwnFresh(Snapshot current, long now) {
        Snapshot state = idle(current);
        return new Snapshot(
                now + FRESH_DURATION_MS,
                state.partyFreshUntilMs(),
                state.buildPercent(),
                state.buildHelpers());
    }

    public static Snapshot applyPartyFresh(Snapshot current, String name, long now) {
        Snapshot state = idle(current);
        if (name == null || name.isBlank()) {
            return state;
        }
        Map<String, Long> party = new LinkedHashMap<>(state.partyFreshUntilMs());
        party.put(name, now + FRESH_DURATION_MS);
        return new Snapshot(state.ownFreshUntilMs(), party, state.buildPercent(), state.buildHelpers());
    }

    public static Snapshot applyBuild(Snapshot current, BuildInfo info) {
        Snapshot state = idle(current);
        if (info == null) {
            return state;
        }
        int helpers = info.helpers() >= 0 ? info.helpers() : state.buildHelpers();
        return new Snapshot(state.ownFreshUntilMs(), state.partyFreshUntilMs(), info.percent(), helpers);
    }

    public static Snapshot prune(Snapshot current, long now) {
        Snapshot state = idle(current);
        long own = state.ownFreshUntilMs() > now ? state.ownFreshUntilMs() : 0L;
        Map<String, Long> party = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : state.partyFreshUntilMs().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > now) {
                party.put(entry.getKey(), entry.getValue());
            }
        }
        return new Snapshot(own, party, state.buildPercent(), state.buildHelpers());
    }

    public static Snapshot clearRun() {
        return Snapshot.idle();
    }

    public static String titleForPhase(IotaKuudraPolicy.Phase phase) {
        if (phase == null) {
            return "";
        }
        return switch (phase) {
            case SUPPLIES -> "Supplies";
            case BUILD -> "Build";
            case EATEN -> "Ballista";
            case STUN -> "Stun";
            case DPS -> "DPS";
            case SKIP -> "Tired";
            case COMPLETED -> "Kuudra Down";
            default -> "";
        };
    }

    public static String noPreTitle(String pileName) {
        if (pileName == null || pileName.isBlank()) {
            return "";
        }
        return "No Pre · " + pileName;
    }

    public static List<String> hudLines(
            Snapshot snapshot,
            long now,
            boolean showOwnFresh,
            boolean showPartyFresh,
            boolean showBuild) {
        Snapshot state = prune(idle(snapshot), now);
        List<String> lines = new ArrayList<>();
        if (showOwnFresh && state.ownFreshUntilMs() > now) {
            lines.add("Fresh " + formatSeconds(state.ownFreshUntilMs() - now));
        }
        if (showPartyFresh) {
            for (FreshEntry entry : partyEntries(state, now)) {
                lines.add(entry.name() + " " + formatSeconds(entry.expiresAtMs() - now));
            }
        }
        if (showBuild && state.buildPercent() >= 0) {
            if (state.buildHelpers() >= 0) {
                lines.add("Build " + state.buildPercent() + "% · " + state.buildHelpers());
            } else {
                lines.add("Build " + state.buildPercent() + "%");
            }
        }
        return List.copyOf(lines);
    }

    public static boolean hudEnabled(boolean parent, boolean fresh, boolean party, boolean build) {
        return parent && (fresh || party || build);
    }

    public static List<FreshEntry> partyEntries(Snapshot snapshot, long now) {
        Snapshot state = prune(idle(snapshot), now);
        List<FreshEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Long> entry : state.partyFreshUntilMs().entrySet()) {
            entries.add(new FreshEntry(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(entries);
    }

    public static String formatSeconds(long remainingMs) {
        long clamped = Math.max(0L, remainingMs);
        return String.format(Locale.ROOT, "%.1fs", clamped / 1000.0D);
    }

    private static Snapshot idle(Snapshot current) {
        return current == null ? Snapshot.idle() : current;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
