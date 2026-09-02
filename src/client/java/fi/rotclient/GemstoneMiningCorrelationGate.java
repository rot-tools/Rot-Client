package fi.rotclient;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Keeps short-lived mining signals separated by gemstone type.
 *
 * This class does not update tracker state. It only exposes whether recent
 * block-break and Pristine signals exist for later event correlation.
 */
final class GemstoneMiningCorrelationGate {
    static final long DEFAULT_WINDOW_MILLIS =
            8_000L;

    private final long windowMillis;

    private final Map<GemstoneType, State> states =
            new EnumMap<>(GemstoneType.class);

    GemstoneMiningCorrelationGate() {
        this(DEFAULT_WINDOW_MILLIS);
    }

    GemstoneMiningCorrelationGate(
            long windowMillis) {
        if (windowMillis <= 0L) {
            throw new IllegalArgumentException(
                    "Correlation window must be positive");
        }

        this.windowMillis = windowMillis;

        for (GemstoneType gemstone :
                GemstoneType.values()) {
            states.put(
                    gemstone,
                    new State());
        }
    }

    void recordDirectBreak(
            GemstoneType gemstone,
            long epochMillis) {
        requireGemstone(gemstone);
        requireTimestamp(epochMillis);

        State state = states.get(gemstone);

        prune(
                state,
                epochMillis);

        state.directBreaks.addLast(
                epochMillis);
    }

    void recordPristine(
            GemstoneType gemstone,
            long amount,
            long epochMillis) {
        requireGemstone(gemstone);
        requireTimestamp(epochMillis);

        if (amount <= 0L) {
            throw new IllegalArgumentException(
                    "Pristine amount must be positive");
        }

        State state = states.get(gemstone);

        prune(
                state,
                epochMillis);

        state.pristineEpochMillis =
                epochMillis;

        state.pristineAmount =
                amount;
    }

    Correlation snapshot(
            GemstoneType gemstone,
            long epochMillis) {
        requireGemstone(gemstone);
        requireTimestamp(epochMillis);

        State state = states.get(gemstone);

        prune(
                state,
                epochMillis);

        long latestBreakAgeMillis =
                -1L;

        if (!state.directBreaks.isEmpty()) {
            latestBreakAgeMillis =
                    age(
                            epochMillis,
                            state.directBreaks.getLast());
        }

        boolean pristineObserved =
                state.pristineEpochMillis >= 0L
                        && age(
                        epochMillis,
                        state.pristineEpochMillis)
                        <= windowMillis;

        return new Correlation(
                state.directBreaks.size(),
                latestBreakAgeMillis,
                pristineObserved,
                pristineObserved
                        ? state.pristineAmount
                        : 0L);
    }

    void consumePristine(
            GemstoneType gemstone,
            long epochMillis) {
        requireGemstone(gemstone);
        requireTimestamp(epochMillis);

        State state = states.get(gemstone);

        prune(
                state,
                epochMillis);

        state.pristineEpochMillis =
                -1L;

        state.pristineAmount =
                0L;
    }

    void reset() {
        for (State state : states.values()) {
            state.directBreaks.clear();
            state.pristineEpochMillis = -1L;
            state.pristineAmount = 0L;
        }
    }

    private void prune(
            State state,
            long epochMillis) {
        while (!state.directBreaks.isEmpty()) {
            long breakEpochMillis =
                    state.directBreaks.getFirst();

            if (age(
                    epochMillis,
                    breakEpochMillis)
                    <= windowMillis) {
                break;
            }

            state.directBreaks.removeFirst();
        }

        if (state.pristineEpochMillis >= 0L
                && age(
                epochMillis,
                state.pristineEpochMillis)
                > windowMillis) {
            state.pristineEpochMillis = -1L;
            state.pristineAmount = 0L;
        }
    }

    private static long age(
            long now,
            long eventEpochMillis) {
        if (eventEpochMillis > now) {
            return 0L;
        }

        return now - eventEpochMillis;
    }

    private static void requireGemstone(
            GemstoneType gemstone) {
        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone type cannot be null");
        }
    }

    private static void requireTimestamp(
            long epochMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be negative");
        }
    }

    private static final class State {
        private final Deque<Long> directBreaks =
                new ArrayDeque<>();

        private long pristineEpochMillis =
                -1L;

        private long pristineAmount;
    }

    record Correlation(
            int directBreaks,
            long latestDirectBreakAgeMillis,
            boolean pristineObserved,
            long pristineAmount) {
    }
}
