package fi.rotclient;

/**
 * Retry delay for the Market Watch API polls. A failed poll used to be retried
 * at the normal interval, and a failed auction crawl restarted from page zero,
 * so a rate-limited client kept hammering the API.
 */
public final class MarketWatchBackoffPolicy {
    public static final long BASE_MILLIS = 30_000L;
    public static final long MAX_MILLIS = 15L * 60L * 1000L;
    public static final int MAX_DOUBLINGS = 5;

    private MarketWatchBackoffPolicy() {
    }

    /**
     * How long to stay quiet after {@code consecutiveFailures} failed polls in a row.
     * Zero failures means no extra wait. A server-supplied Retry-After can only lengthen it.
     */
    public static long delayMillis(int consecutiveFailures, long retryAfterMillis) {
        long exponential = 0L;
        if (consecutiveFailures > 0) {
            int doublings = Math.min(consecutiveFailures - 1, MAX_DOUBLINGS);
            exponential = Math.min(MAX_MILLIS, BASE_MILLIS << doublings);
        }
        long serverAsked = Math.max(0L, Math.min(MAX_MILLIS, retryAfterMillis));
        return Math.max(exponential, serverAsked);
    }

    /** Parses a Retry-After header in seconds; anything else means "not supplied". */
    public static long retryAfterMillis(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return 0L;
        }
        try {
            long seconds = Long.parseLong(headerValue.trim());
            return seconds <= 0L ? 0L : Math.min(MAX_MILLIS, seconds * 1_000L);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
