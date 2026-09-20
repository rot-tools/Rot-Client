package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Persisted rules for automatic profile switching.
 *
 * This lives on {@link RotClientProfileConfig}, next to the profile list, and
 * deliberately not inside {@link RotClientProfileSettings}: the rules decide
 * which profile is active, so they must survive every profile switch instead
 * of being replaced by it.
 *
 * Off by default. A context without its own rule falls back to
 * {@link #fallbackProfileId}; with no fallback either, the current profile is
 * left alone.
 */
final class RotClientAutoSwitchConfig {
    boolean enabled;
    boolean notify = true;
    String fallbackProfileId = "";

    /** {@link AutoProfileContext#id()} to profile id. */
    Map<String, String> rules = new LinkedHashMap<>();

    RotClientAutoSwitchConfig copy() {
        RotClientAutoSwitchConfig copy = new RotClientAutoSwitchConfig();
        copy.enabled = enabled;
        copy.notify = notify;
        copy.fallbackProfileId = fallbackProfileId;
        copy.rules = rules == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(rules);
        return copy;
    }

    /**
     * Repairs damaged/manually edited data and drops references to profiles
     * that no longer exist. Unknown context ids are kept so a newer build's
     * rules are not lost by opening the file in an older one.
     */
    void normalize(Predicate<String> profileExists) {
        if (fallbackProfileId == null) {
            fallbackProfileId = "";
        } else {
            fallbackProfileId = fallbackProfileId.trim();
        }
        if (!fallbackProfileId.isEmpty()
                && !profileExists.test(fallbackProfileId)) {
            fallbackProfileId = "";
        }

        if (rules == null) {
            rules = new LinkedHashMap<>();
            return;
        }
        rules.entrySet().removeIf(entry ->
                entry.getKey() == null
                        || entry.getKey().isBlank()
                        || entry.getValue() == null
                        || !profileExists.test(entry.getValue().trim()));
    }

    /** Explicit rule for a context, or empty string. */
    String ruleFor(AutoProfileContext context) {
        if (context == null || rules == null) {
            return "";
        }
        String value = rules.get(context.id());
        return value == null ? "" : value.trim();
    }

    /**
     * The profile to activate for a context: its own rule, else the fallback,
     * else empty (meaning "do nothing").
     */
    String targetFor(AutoProfileContext context) {
        String rule = ruleFor(context);
        if (!rule.isEmpty()) {
            return rule;
        }
        return fallbackProfileId == null ? "" : fallbackProfileId;
    }

    /**
     * The profile to switch to when a context has just become stable, or an
     * empty string when nothing should happen: switching is off, no rule or
     * fallback applies, or that profile is already active.
     */
    String switchTargetFor(AutoProfileContext context, String activeProfileId) {
        if (!enabled || context == null) {
            return "";
        }
        String target = targetFor(context);
        if (target.isEmpty() || target.equals(activeProfileId)) {
            return "";
        }
        return target;
    }

    /** Sets or, with a blank id, clears the rule for one context. */
    void setRule(AutoProfileContext context, String profileId) {
        if (context == null) {
            return;
        }
        if (rules == null) {
            rules = new LinkedHashMap<>();
        }
        if (profileId == null || profileId.isBlank()) {
            rules.remove(context.id());
        } else {
            rules.put(context.id(), profileId.trim());
        }
    }

    /**
     * Next choice when a rule button is clicked: not set, then each profile in
     * list order, then back to not set.
     */
    static String nextProfileId(List<String> orderedProfileIds, String currentId) {
        if (orderedProfileIds == null || orderedProfileIds.isEmpty()) {
            return "";
        }
        int index = currentId == null || currentId.isBlank()
                ? -1
                : orderedProfileIds.indexOf(currentId);
        if (index < 0) {
            return orderedProfileIds.get(0);
        }
        return index + 1 < orderedProfileIds.size()
                ? orderedProfileIds.get(index + 1)
                : "";
    }
}
