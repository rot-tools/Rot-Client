package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Map;

final class GemstoneTrackerRegistry {
    private final Map<String, GemstoneTrackerState> states;

    GemstoneTrackerRegistry() {
        this(new LinkedHashMap<>());
    }

    GemstoneTrackerRegistry(Map<String, GemstoneTrackerState> states) {
        if (states == null) {
            throw new IllegalArgumentException(
                    "Gemstone state map cannot be null");
        }
        this.states = states;
    }

    GemstoneTrackerState state(GemstoneType gemstone) {
        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone type cannot be null");
        }

        String canonicalKey = gemstone.id();
        GemstoneTrackerState state = states.get(canonicalKey);
        if (state == null) {
            for (Map.Entry<String, GemstoneTrackerState> entry :
                    new LinkedHashMap<>(states).entrySet()) {
                String key = entry.getKey();
                if (key != null && key.equalsIgnoreCase(canonicalKey)) {
                    state = entry.getValue();
                    if (state == null) {
                        state = new GemstoneTrackerState();
                    }
                    states.remove(key);
                    states.put(canonicalKey, state);
                    return state;
                }
            }
            state = new GemstoneTrackerState();
            states.put(canonicalKey, state);
        }
        return state;
    }

    void normalize() {
        Map<String, GemstoneTrackerState> normalized =
                new LinkedHashMap<>();

        for (GemstoneType gemstone : GemstoneType.values()) {
            String canonicalKey = gemstone.id();
            GemstoneTrackerState canonicalState = null;

            for (Map.Entry<String, GemstoneTrackerState> entry :
                    states.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }
                if (!key.equalsIgnoreCase(canonicalKey)) {
                    continue;
                }
                if (key.equals(canonicalKey)) {
                    canonicalState = entry.getValue();
                    break;
                }
                if (canonicalState == null) {
                    canonicalState = entry.getValue();
                }
            }

            if (canonicalState == null) {
                canonicalState = states.get(canonicalKey);
            }
            if (canonicalState == null) {
                canonicalState = new GemstoneTrackerState();
            }
            canonicalState.normalize();
            normalized.put(canonicalKey, canonicalState);
        }

        states.clear();
        states.putAll(normalized);
    }

    void resetSession(GemstoneType gemstone) {
        GemstoneTrackerState state = state(gemstone);
        state.resetSession();
    }

    void resetAllSessions() {
        for (GemstoneTrackerState state : states.values()) {
            state.resetSession();
        }
    }

    private static boolean isCanonicalKey(String key) {
        for (GemstoneType gemstone : GemstoneType.values()) {
            if (gemstone.id().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String canonicalKey(String key) {
        for (GemstoneType gemstone : GemstoneType.values()) {
            if (gemstone.id().equalsIgnoreCase(key)) {
                return gemstone.id();
            }
        }
        return key;
    }
}
