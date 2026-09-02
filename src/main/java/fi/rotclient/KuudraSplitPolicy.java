package fi.rotclient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kuudra split chat timers for the Serveri.
 * Supply → Build → Fuel/Eaten → Stun → DPS → Skip → Kill, with local PBs.
 */
public final class KuudraSplitPolicy {
    public enum Phase {
        NONE,
        SUPPLY,
        BUILD,
        FUEL,
        STUN,
        DPS,
        SKIP,
        KILL
    }

    public record Snapshot(
            long startMs,
            Map<Phase, Long> times,
            Phase active,
            boolean finished) {
        public Snapshot {
            times = times == null ? Map.of() : Map.copyOf(times);
            active = active == null ? Phase.NONE : active;
        }

        public static Snapshot idle() {
            return new Snapshot(0L, Map.of(), Phase.NONE, false);
        }
    }

    private KuudraSplitPolicy() {
    }

    public static Phase eventFromChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return Phase.NONE;
        }
        if (text.contains("kuudra down") || text.contains("kuudra has been defeated")) {
            return Phase.KILL;
        }
        if (text.contains("defeat") && (text.contains("kuudra") || text.trim().equals("defeat"))) {
            return Phase.NONE;
        }
        if (text.contains("destroyed one of kuudra's pods")
                || text.contains("destroyed one of kuudras pods")) {
            return Phase.STUN;
        }
        if (text.contains("has been eaten by kuudra")) {
            return Phase.FUEL;
        }
        if (text.contains("building progress")
                || text.contains("the ballista")
                || text.contains("charged a fuel cell")) {
            return Phase.BUILD;
        }
        if (text.contains("supplies recovered")
                || text.contains("recovered a supply")
                || text.contains("brought a supply")
                || text.matches(".*\\b[1-6]/6\\b.*supply.*")) {
            return Phase.SUPPLY;
        }
        if (text.contains("kuudra's hollow") && text.contains("starting")
                || text.contains("[npc] elle") && text.contains("good luck")
                || text.contains("[npc] elle") && text.contains("fish up kuudra")) {
            return Phase.SUPPLY;
        }
        if (text.contains("kuudra grows tired") || text.contains("fresh tools")) {
            return Phase.SKIP;
        }
        if (text.startsWith("progress:") && text.contains("%")) {
            return Phase.DPS;
        }
        return Phase.NONE;
    }

    public static Snapshot apply(Snapshot current, Phase event, long now) {
        Snapshot state = current == null ? Snapshot.idle() : current;
        if (event == Phase.NONE) {
            return state;
        }
        if (event == Phase.SUPPLY && state.startMs() <= 0L) {
            return new Snapshot(now, Map.of(), Phase.SUPPLY, false);
        }
        if (state.startMs() <= 0L) {
            return new Snapshot(now, Map.of(), event, event == Phase.KILL);
        }
        Map<Phase, Long> times = new EnumMap<>(Phase.class);
        times.putAll(state.times());
        Phase previous = state.active();
        if (previous != Phase.NONE && previous != event && !times.containsKey(previous)) {
            times.put(previous, Math.max(0L, now - state.startMs()));
        }
        if (event == Phase.KILL) {
            times.put(Phase.KILL, Math.max(0L, now - state.startMs()));
            return new Snapshot(state.startMs(), times, Phase.NONE, true);
        }
        return new Snapshot(state.startMs(), times, event, false);
    }

    public static List<String> hudLines(Snapshot state, long now, Map<String, Long> pbs) {
        if (state == null || state.startMs() <= 0L) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("Kuudra");
        for (Phase phase : List.of(
                Phase.SUPPLY, Phase.BUILD, Phase.FUEL, Phase.STUN, Phase.DPS, Phase.SKIP, Phase.KILL)) {
            Long done = state.times().get(phase);
            boolean live = state.active() == phase;
            if (done == null && !live && !state.finished()) {
                continue;
            }
            long ms = done != null ? done : (live ? now - state.startMs() : 0L);
            if (ms <= 0L && !live) {
                continue;
            }
            lines.add(label(phase) + ": " + format(ms) + pbSuffix(phase.name(), pbs, done != null));
        }
        return lines;
    }

    public static boolean recordPersonalBest(Map<String, Long> pbs, Phase phase, long millis) {
        if (pbs == null || phase == null || phase == Phase.NONE || millis <= 0L) {
            return false;
        }
        String key = phase.name();
        Long previous = pbs.get(key);
        if (previous != null && previous > 0L && millis >= previous) {
            return false;
        }
        pbs.put(key, millis);
        return true;
    }

    public static Map<String, Long> parseTimes(String stored) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (stored == null || stored.isBlank()) {
            return map;
        }
        for (String part : stored.split(",")) {
            String[] bits = part.split("=");
            if (bits.length != 2) {
                continue;
            }
            try {
                long millis = Long.parseLong(bits[1].trim());
                if (millis > 0L) {
                    map.put(bits[0].trim().toUpperCase(Locale.ROOT), millis);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    public static String writeTimes(Map<String, Long> times) {
        if (times == null || times.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Long> entry : times.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0L) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(entry.getKey().toUpperCase(Locale.ROOT)).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    public static String label(Phase phase) {
        return switch (phase) {
            case SUPPLY -> "Supply";
            case BUILD -> "Build";
            case FUEL -> "Eaten";
            case STUN -> "Stun";
            case DPS -> "DPS";
            case SKIP -> "Skip";
            case KILL -> "Kill";
            case NONE -> "";
        };
    }

    private static String pbSuffix(String key, Map<String, Long> pbs, boolean finished) {
        if (!finished || pbs == null) {
            return "";
        }
        Long pb = pbs.get(key);
        return pb == null || pb <= 0L ? "" : " (PB " + format(pb) + ")";
    }

    private static String format(long millis) {
        return String.format(Locale.ROOT, "%.2fs", Math.max(0L, millis) / 1000.0D);
    }
}
