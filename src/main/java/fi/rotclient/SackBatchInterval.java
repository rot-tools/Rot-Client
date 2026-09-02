package fi.rotclient;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel Sack batch covered-duration hints such as
 * {@code [Sacks] +1,141 items. (Last 24s.)}.
 *
 * <p>Correlation against mining evidence must use this covered interval when
 * present — a fixed short "last block" window is insufficient for real
 * Hypixel batch summaries.
 */
final class SackBatchInterval {
    static final long DEFAULT_COVERED_MILLIS = 10_000L;
    static final long MAX_COVERED_MILLIS = 90_000L;
    static final long MIN_COVERED_MILLIS = 1_000L;

    private static final Pattern LAST_SECONDS = Pattern.compile(
            "\\(\\s*Last\\s+(\\d{1,3})\\s*s\\.?\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private SackBatchInterval() {
    }

    /**
     * @return covered duration in millis when the message contains a
     * {@code Last Ns} hint; empty otherwise
     */
    static OptionalLong parseCoveredMillis(String sackMessage) {
        if (sackMessage == null || sackMessage.isBlank()) {
            return OptionalLong.empty();
        }
        Matcher matcher = LAST_SECONDS.matcher(sackMessage);
        if (!matcher.find()) {
            return OptionalLong.empty();
        }
        try {
            long seconds = Long.parseLong(matcher.group(1));
            if (seconds <= 0L) {
                return OptionalLong.empty();
            }
            long millis = Math.multiplyExact(seconds, 1_000L);
            return OptionalLong.of(clamp(millis));
        } catch (ArithmeticException | NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    /** Effective correlation window for a batch, always within safe bounds. */
    static long effectiveWindowMillis(long coveredMillisOrZero) {
        if (coveredMillisOrZero <= 0L) {
            return DEFAULT_COVERED_MILLIS;
        }
        return clamp(Math.max(DEFAULT_COVERED_MILLIS, coveredMillisOrZero));
    }

    static long clamp(long millis) {
        if (millis < MIN_COVERED_MILLIS) {
            return MIN_COVERED_MILLIS;
        }
        return Math.min(millis, MAX_COVERED_MILLIS);
    }
}
