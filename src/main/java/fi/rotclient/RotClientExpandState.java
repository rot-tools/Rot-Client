package fi.rotclient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-id 0..1 open amounts for accordion menus. First sync snaps so opening
 * the dashboard does not replay every already-open section.
 */
final class RotClientExpandState {
    private static final double STIFFNESS = RotClientEase.EXPAND_STIFFNESS;

    private final Map<String, Double> displayed = new LinkedHashMap<>();
    private final Map<String, Double> target = new LinkedHashMap<>();
    private long lastAdvanceNs;

    void syncTargets(Collection<String> openIds, Collection<String> knownIds) {
        if (knownIds == null) {
            return;
        }
        for (String id : knownIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            double nextTarget = contains(openIds, id) ? 1.0D : 0.0D;
            target.put(id, nextTarget);
            displayed.putIfAbsent(id, nextTarget);
        }
    }

    void advance(long nowNs) {
        double dt = RotClientEase.frameDeltaSeconds(lastAdvanceNs, nowNs);
        lastAdvanceNs = nowNs <= 0L ? lastAdvanceNs : nowNs;
        advanceSeconds(dt);
    }

    void advanceSeconds(double dtSeconds) {
        for (Map.Entry<String, Double> entry : target.entrySet()) {
            double current = displayed.getOrDefault(entry.getKey(), entry.getValue());
            displayed.put(
                    entry.getKey(),
                    RotClientEase.expToward(current, entry.getValue(), dtSeconds, STIFFNESS));
        }
    }

    double amount(String id) {
        if (id == null || id.isBlank()) {
            return 0.0D;
        }
        return RotClientEase.clamp01(displayed.getOrDefault(id, 0.0D));
    }

    boolean visuallyOpen(String id) {
        return amount(id) > 0.02D;
    }

    void snap() {
        displayed.putAll(target);
        lastAdvanceNs = 0L;
    }

    private static boolean contains(Collection<String> openIds, String id) {
        if (openIds == null) {
            return false;
        }
        for (String candidate : openIds) {
            if (id.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
