package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compact Extra Stats reprint from Catacombs end-of-run chat. Collects
 * score, time, bits, secrets and deaths, then formats a local summary.
 * Minecraft-free for unit tests.
 */
public final class DungeonExtraStatsPolicy {
    public static final int DUMP_QUIET_TICKS = 40;
    private static final Pattern DEFEATED = Pattern.compile(
            "^☠ Defeated (.+) in ([\\dhms0-9 ]+?)\\s*(\\(NEW RECORD!\\))?$");
    private static final Pattern TEAM_SCORE = Pattern.compile(
            "^Team Score: (\\d+) \\((.{1,2})\\)\\s?(\\(NEW RECORD!\\))?$");
    private static final Pattern XP = Pattern.compile(
            "^(\\+[\\d,.]+\\s?\\w+ Experience)\\s?(?:\\(.+\\))?$");
    private static final Pattern BITS = Pattern.compile("^(\\+\\d+ Bits)$");
    private static final Pattern DAMAGE = Pattern.compile(
            "^(Total Damage as .+: [\\d,.]+)\\s?(\\(NEW RECORD!\\))?$");
    private static final Pattern HEAL = Pattern.compile(
            "^(Ally Healing: [\\d,.]+)\\s?(\\(NEW RECORD!\\))?$");
    private static final Pattern KILLS = Pattern.compile(
            "^(Enemies Killed: \\d+)\\s?(\\(NEW RECORD!\\))?$");
    private static final Pattern DEATHS = Pattern.compile("^Deaths: (\\d+)$");
    private static final Pattern SECRETS = Pattern.compile("^Secrets Found: (\\d+)$");
    private static final Pattern BAR = Pattern.compile("^▬+$");

    public record Snapshot(
            String defeated,
            String time,
            boolean timePb,
            int score,
            String letter,
            boolean scorePb,
            String bits,
            String secrets,
            String deaths,
            String damage,
            String kills,
            String heal,
            List<String> xp,
            boolean collecting,
            boolean printed) {
        public Snapshot {
            defeated = defeated == null ? "" : defeated;
            time = time == null ? "" : time;
            letter = letter == null ? "" : letter;
            bits = bits == null ? "" : bits;
            secrets = secrets == null ? "" : secrets;
            deaths = deaths == null ? "" : deaths;
            damage = damage == null ? "" : damage;
            kills = kills == null ? "" : kills;
            heal = heal == null ? "" : heal;
            xp = xp == null ? List.of() : List.copyOf(xp);
        }

        public static Snapshot idle() {
            return new Snapshot("", "", false, 0, "", false, "", "", "", "", "", "", List.of(), false, false);
        }

        public boolean ready() {
            return score > 0 || !defeated.isBlank();
        }

        public boolean dumpFinished() {
            return !collecting && ready();
        }

        public List<String> compactLines() {
            List<String> lines = new ArrayList<>();
            if (!defeated.isBlank()) {
                lines.add("Defeated " + defeated
                        + (time.isBlank() ? "" : " in " + time)
                        + (timePb ? " (PB)" : ""));
            }
            if (score > 0) {
                lines.add("Score " + score
                        + (letter.isBlank() ? "" : " (" + letter + ")")
                        + (scorePb ? " (PB)" : "")
                        + (bits.isBlank() ? "" : "  " + bits));
            }
            if (!secrets.isBlank() || !deaths.isBlank()) {
                lines.add("Secrets " + (secrets.isBlank() ? "-" : secrets)
                        + "  Deaths " + (deaths.isBlank() ? "-" : deaths));
            }
            if (!damage.isBlank() || !kills.isBlank() || !heal.isBlank()) {
                lines.add(joinDash(damage, kills, heal));
            }
            if (!xp.isEmpty()) {
                lines.add(xp.getFirst());
            }
            return List.copyOf(lines);
        }
    }

    private DungeonExtraStatsPolicy() {
    }

    public static boolean extraStatsHeader(String chat) {
        return DungeonPolicy.isExtraStatsChat(chat);
    }

    public static boolean extraStatsLine(String chat) {
        String text = DungeonPolicy.normalize(chat);
        return extraStatsHeader(chat)
                || DEFEATED.matcher(text).matches()
                || TEAM_SCORE.matcher(text).matches()
                || XP.matcher(text).matches()
                || BITS.matcher(text).matches()
                || DAMAGE.matcher(text).matches()
                || HEAL.matcher(text).matches()
                || KILLS.matcher(text).matches()
                || DEATHS.matcher(text).matches()
                || SECRETS.matcher(text).matches()
                || BAR.matcher(text).matches();
    }

    public static Snapshot apply(Snapshot current, String chat) {
        Snapshot state = current == null ? Snapshot.idle() : current;
        String text = DungeonPolicy.normalize(chat);
        if (extraStatsHeader(chat)) {
            return new Snapshot("", "", false, 0, "", false, "", "", "", "", "", "", List.of(), true, false);
        }
        if (state.printed() || !state.collecting()) {
            return state;
        }
        if (BAR.matcher(text).matches()) {
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), state.secrets(), state.deaths(),
                    state.damage(), state.kills(), state.heal(),
                    state.xp(), false, state.printed());
        }
        Matcher defeated = DEFEATED.matcher(text);
        if (defeated.matches()) {
            return new Snapshot(
                    defeated.group(1).trim(),
                    defeated.group(2).trim(),
                    defeated.group(3) != null,
                    state.score(),
                    state.letter(),
                    state.scorePb(),
                    state.bits(),
                    state.secrets(),
                    state.deaths(),
                    state.damage(),
                    state.kills(),
                    state.heal(),
                    state.xp(),
                    true,
                    false);
        }
        Matcher score = TEAM_SCORE.matcher(text);
        if (score.matches()) {
            return new Snapshot(
                    state.defeated(),
                    state.time(),
                    state.timePb(),
                    Integer.parseInt(score.group(1)),
                    score.group(2),
                    score.group(3) != null,
                    state.bits(),
                    state.secrets(),
                    state.deaths(),
                    state.damage(),
                    state.kills(),
                    state.heal(),
                    state.xp(),
                    true,
                    false);
        }
        Matcher xp = XP.matcher(text);
        if (xp.matches()) {
            List<String> next = new ArrayList<>(state.xp());
            next.add(xp.group(1));
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), state.secrets(), state.deaths(),
                    state.damage(), state.kills(), state.heal(),
                    next, true, false);
        }
        Matcher bits = BITS.matcher(text);
        if (bits.matches()) {
            return copyBits(state, bits.group(1));
        }
        Matcher damage = DAMAGE.matcher(text);
        if (damage.matches()) {
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), state.secrets(), state.deaths(),
                    damage.group(1), state.kills(), state.heal(),
                    state.xp(), true, false);
        }
        Matcher heal = HEAL.matcher(text);
        if (heal.matches()) {
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), state.secrets(), state.deaths(),
                    state.damage(), state.kills(), heal.group(1),
                    state.xp(), true, false);
        }
        Matcher kills = KILLS.matcher(text);
        if (kills.matches()) {
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), state.secrets(), state.deaths(),
                    state.damage(), kills.group(1), state.heal(),
                    state.xp(), true, false);
        }
        Matcher deaths = DEATHS.matcher(text);
        if (deaths.matches()) {
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), state.secrets(), deaths.group(1),
                    state.damage(), state.kills(), state.heal(),
                    state.xp(), true, false);
        }
        Matcher secrets = SECRETS.matcher(text);
        if (secrets.matches()) {
            return new Snapshot(
                    state.defeated(), state.time(), state.timePb(),
                    state.score(), state.letter(), state.scorePb(),
                    state.bits(), secrets.group(1), state.deaths(),
                    state.damage(), state.kills(), state.heal(),
                    state.xp(), true, false);
        }
        return state;
    }

    public static boolean shouldPrint(Snapshot state) {
        return state != null && state.dumpFinished() && !state.printed();
    }

    public static Snapshot closeDump(Snapshot current) {
        if (current == null || current.printed() || !current.ready()) {
            return current == null ? Snapshot.idle() : current;
        }
        return new Snapshot(
                current.defeated(), current.time(), current.timePb(),
                current.score(), current.letter(), current.scorePb(),
                current.bits(), current.secrets(), current.deaths(),
                current.damage(), current.kills(), current.heal(),
                current.xp(), false, current.printed());
    }

    public static Snapshot markPrinted(Snapshot current) {
        return withPrinted(current, true);
    }

    private static Snapshot copyBits(Snapshot state, String bits) {
        return new Snapshot(
                state.defeated(), state.time(), state.timePb(),
                state.score(), state.letter(), state.scorePb(),
                bits, state.secrets(), state.deaths(),
                state.damage(), state.kills(), state.heal(),
                state.xp(), true, false);
    }

    private static Snapshot withPrinted(Snapshot state, boolean printed) {
        if (state == null) {
            return Snapshot.idle();
        }
        return new Snapshot(
                state.defeated(), state.time(), state.timePb(),
                state.score(), state.letter(), state.scorePb(),
                state.bits(), state.secrets(), state.deaths(),
                state.damage(), state.kills(), state.heal(),
                state.xp(), state.collecting(), printed);
    }

    private static String joinDash(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" - ");
            }
            builder.append(part);
        }
        return builder.toString();
    }
}
