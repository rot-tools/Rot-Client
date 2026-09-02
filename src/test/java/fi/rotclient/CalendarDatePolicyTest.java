package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarDatePolicyTest {
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;

    @Test
    void parsesCountdownUnits() {
        assertEquals(5L * DAY + 3L * HOUR + 20L * MINUTE,
                CalendarDatePolicy.countdownMillis("5d 3h 20m").orElseThrow());
        assertEquals(3L * HOUR + 20L * MINUTE,
                CalendarDatePolicy.countdownMillis("§b3h 20m").orElseThrow());
        assertEquals(45_000L, CalendarDatePolicy.countdownMillis("45s").orElseThrow());
        assertTrue(CalendarDatePolicy.countdownMillis("soon").isEmpty());
        assertTrue(CalendarDatePolicy.countdownMillis("").isEmpty());
    }

    @Test
    void roundsUpToFiveSecondTicks() {
        assertEquals(5_000L, CalendarDatePolicy.roundUpToFiveSeconds(5_000L));
        assertEquals(10_000L, CalendarDatePolicy.roundUpToFiveSeconds(6_000L));
        assertEquals(10_000L, CalendarDatePolicy.roundUpToFiveSeconds(9_000L));
    }

    @Test
    void onlyProducesEntriesInsideCalendarMenu() {
        List<String> lore = List.of("§7Starts in: §b1h 30m");
        assertTrue(CalendarDatePolicy
                .entries(true, "Your Bags", "Spooky Festival", lore, 0L)
                .isEmpty());
        assertTrue(CalendarDatePolicy
                .entries(false, CalendarDatePolicy.MENU_TITLE, "Spooky Festival", lore, 0L)
                .isEmpty());
        assertFalse(CalendarDatePolicy
                .entries(true, CalendarDatePolicy.MENU_TITLE, "Spooky Festival", lore, 0L)
                .isEmpty());
    }

    @Test
    void addsSpookyAndJerryExtras() {
        List<String> lore = List.of("§7Starts in: §b2h");
        List<CalendarDatePolicy.Entry> spooky = CalendarDatePolicy.entries(
                true, CalendarDatePolicy.MENU_TITLE, "§6Spooky Festival", lore, 0L);
        assertEquals(2, spooky.size());
        assertEquals(2L * HOUR, spooky.get(0).epochMs());
        assertEquals(HOUR, spooky.get(1).epochMs());

        List<CalendarDatePolicy.Entry> jerry = CalendarDatePolicy.entries(
                true, CalendarDatePolicy.MENU_TITLE, "§cSeason of Jerry", lore, 0L);
        assertEquals(2, jerry.size());
        assertEquals(2L * HOUR - 7L * HOUR - 40L * MINUTE, jerry.get(1).epochMs());

        List<CalendarDatePolicy.Entry> plain = CalendarDatePolicy.entries(
                true, CalendarDatePolicy.MENU_TITLE, "New Year Celebration", lore, 0L);
        assertEquals(1, plain.size());
    }
}
