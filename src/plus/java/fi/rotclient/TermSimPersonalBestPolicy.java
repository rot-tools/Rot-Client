package fi.rotclient;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Plus-owned local Terminal Simulator personal-best storage. */
public final class TermSimPersonalBestPolicy {
    private TermSimPersonalBestPolicy() {}

    public static Map<TermSimPolicy.Kind, Integer> parsePersonalBests(String stored) {
        EnumMap<TermSimPolicy.Kind, Integer> map = new EnumMap<>(TermSimPolicy.Kind.class);
        if (stored == null || stored.isBlank()) {
            return map;
        }
        for (String part : stored.split(",")) {
            String[] bits = part.split("=");
            if (bits.length != 2) {
                continue;
            }
            try {
                TermSimPolicy.Kind kind = TermSimPolicy.Kind.valueOf(bits[0].trim().toUpperCase(Locale.ROOT));
                int millis = Integer.parseInt(bits[1].trim());
                if (kind != TermSimPolicy.Kind.HUB && millis > 0) {
                    map.put(kind, millis);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return map;
    }

    public static String writePersonalBests(Map<TermSimPolicy.Kind, Integer> pbs) {
        if (pbs == null || pbs.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<TermSimPolicy.Kind, Integer> entry : pbs.entrySet()) {
            if (entry.getKey() == TermSimPolicy.Kind.HUB || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(entry.getKey().name()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    public static boolean recordPersonalBest(
            Map<TermSimPolicy.Kind, Integer> pbs, TermSimPolicy.Kind kind, int millis) {
        if (pbs == null || kind == null || kind == TermSimPolicy.Kind.HUB || millis <= 0) {
            return false;
        }
        Integer previous = pbs.get(kind);
        if (previous != null && previous > 0 && millis >= previous) {
            return false;
        }
        pbs.put(kind, millis);
        return true;
    }

    public static String hubSlotName(TermSimPolicy.Kind kind, Map<TermSimPolicy.Kind, Integer> pbs) {
        String base = TermSimPolicy.titleFor(kind == null ? TermSimPolicy.Kind.HUB : kind);
        if (kind == null || kind == TermSimPolicy.Kind.HUB || pbs == null) {
            return base;
        }
        Integer pb = pbs.get(kind);
        if (pb == null || pb <= 0) {
            return base;
        }
        return base + " §e" + format(pb);
    }

    private static String format(long millis) {
        return String.format(Locale.ROOT, "%.2fs", Math.max(0L, millis) / 1000.0D);
    }
}
