package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regressions found while auditing the Utilities modules. */
class UtilitiesRegressionTest {
    @Test
    void failedPollsBackOffAndNeverRetryFasterThanTheServerAsked() {
        assertEquals(0L, MarketWatchBackoffPolicy.delayMillis(0, 0L));
        assertEquals(30_000L, MarketWatchBackoffPolicy.delayMillis(1, 0L));
        assertEquals(60_000L, MarketWatchBackoffPolicy.delayMillis(2, 0L));
        assertEquals(120_000L, MarketWatchBackoffPolicy.delayMillis(3, 0L));
        // Capped, however many failures pile up.
        assertEquals(MarketWatchBackoffPolicy.MAX_MILLIS, MarketWatchBackoffPolicy.delayMillis(50, 0L));
        // A Retry-After longer than our own backoff wins; a shorter one never shortens it.
        assertEquals(300_000L, MarketWatchBackoffPolicy.delayMillis(1, 300_000L));
        assertEquals(120_000L, MarketWatchBackoffPolicy.delayMillis(3, 5_000L));
    }

    @Test
    void retryAfterHeaderIsParsedDefensively() {
        assertEquals(120_000L, MarketWatchBackoffPolicy.retryAfterMillis("120"));
        assertEquals(120_000L, MarketWatchBackoffPolicy.retryAfterMillis(" 120 "));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis(""));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis(null));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis("soon"));
        assertEquals(0L, MarketWatchBackoffPolicy.retryAfterMillis("-5"));
        assertEquals(
                MarketWatchBackoffPolicy.MAX_MILLIS,
                MarketWatchBackoffPolicy.retryAfterMillis("99999999"));
    }

    @Test
    void sellerUuidsAreNormalizedAndBadOnesRejected() {
        assertEquals(
                "069a79f444e94726a5befca90e38aaf5",
                MarketWatchSellerNameService.normalizeUuid("069A79F4-44E9-4726-A5BE-FCA90E38AAF5"));
        assertEquals("", MarketWatchSellerNameService.normalizeUuid("not-a-uuid"));
        assertEquals("", MarketWatchSellerNameService.normalizeUuid(null));
        assertEquals("", MarketWatchSellerNameService.normalizeUuid("069a79f444e94726a5befca90e38aaf"));
    }

    @Test
    void thereIsNoEzEmoteBecauseItWouldBypassHypixelsChatFilter() {
        assertTrue(ChatCommandsPolicy.applyEmotes("ez", true).isEmpty());
        assertTrue(ChatCommandsPolicy.applyEmotes("gg ez", true).isEmpty());
        // The other emotes are untouched.
        assertEquals("hello ❤", ChatCommandsPolicy.applyEmotes("hello <3", true).orElseThrow());
    }

    @Test
    void waypointFloodIsCappedKeepingTheNewest() {
        List<WaypointPolicy.Marker> markers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            markers.add(new WaypointPolicy.Marker("m" + i, i, 64, 0, 0xFFFFFFFF, i, 60_000L));
        }

        WaypointPolicy.trimOldest(markers, WaypointPolicy.MAX_MARKERS);

        assertEquals(WaypointPolicy.MAX_MARKERS, markers.size());
        assertEquals("m68", markers.get(0).name());
        assertEquals("m99", markers.get(markers.size() - 1).name());
    }
}
