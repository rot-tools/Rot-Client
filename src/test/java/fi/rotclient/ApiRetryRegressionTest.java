package fi.rotclient;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

final class ApiRetryRegressionTest {
    @Test void aWhollyUndecodableProductObjectIsNotASuccessfulPriceUpdate() {
        var products = com.google.gson.JsonParser.parseString("{\"BROKEN\":1,\"OTHER\":null}")
                .getAsJsonObject();
        assertFalse(BazaarPriceService.parseMarketPrices(products).available());
    }

    @Test void hugeValidDelayCannotWrapToAnImmediateRetry() {
        assertEquals(MarketWatchBackoffPolicy.MAX_MILLIS,
                MarketWatchBackoffPolicy.retryAfterMillis(Long.toString(Long.MAX_VALUE), 0L));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis("-1", 0L));
    }

    @Test void retryAfterAcceptsHttpDatesAndNeverRetriesBeforeRequestedTimeWithinBound() {
        long now = Instant.parse("2026-10-08T12:00:00Z").toEpochMilli();
        assertEquals(120_000L, MarketWatchBackoffPolicy.retryAfterMillis(
                "Thu, 08 Oct 2026 12:02:00 GMT", now));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis(
                "Thu, 08 Oct 2026 11:59:00 GMT", now));
        assertEquals(MarketWatchBackoffPolicy.MAX_MILLIS, MarketWatchBackoffPolicy.retryAfterMillis(
                "Fri, 09 Oct 2026 12:00:00 GMT", now));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis("not a date", now));
    }

    @Test void aServerDelayCannotShortenTheFailureBackoff() {
        long retry = MarketWatchBackoffPolicy.retryAfterMillis("120", 0L);
        assertEquals(120_000L, MarketWatchBackoffPolicy.delayMillis(1, retry));
        assertEquals(240_000L, MarketWatchBackoffPolicy.delayMillis(4, retry));
    }
}
