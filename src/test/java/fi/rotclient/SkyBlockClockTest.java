package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SkyBlockClockTest {
    @Test
    void epochIsEarlySpringFirstAtMidnight() {
        SkyBlockClock.Instant instant = SkyBlockClock.at(SkyBlockClock.EPOCH_MILLIS);
        assertEquals(1, instant.year());
        assertEquals("Early Spring", instant.month());
        assertEquals(1, instant.day());
        assertEquals(0, instant.hour());
        assertEquals(0, instant.minute());
        assertTrue(SkyBlockClock.formatDate(instant).contains("Early Spring"));
    }

    @Test
    void exactMinutesKeepNonTens() {
        long now = SkyBlockClock.EPOCH_MILLIS + 6_667L;
        SkyBlockClock.Instant instant = SkyBlockClock.at(now);
        assertEquals(8, instant.minute());
        assertTrue(SkyBlockClock.formatTime(instant, false, true).contains(":08"));
        assertTrue(SkyBlockClock.formatTime(instant, false, false).contains(":00"));
    }
}
