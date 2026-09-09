package fi.rotclient;

import java.util.Locale;

/**
 * SkyBlock calendar and clock from Unix time. One SkyBlock hour is 50 real
 * seconds (one day is 20 real minutes). Epoch is the public Hypixel year-1
 * start used by community clocks.
 */
public final class SkyBlockClock {
    public static final long EPOCH_MILLIS = 1_560_275_700_000L;
    public static final long HOUR_MILLIS = 50_000L;
    public static final long DAY_MILLIS = 24L * HOUR_MILLIS;
    public static final int MONTH_DAYS = 31;
    public static final int YEAR_MONTHS = 12;

    private static final String[] MONTHS = {
            "Early Spring",
            "Spring",
            "Late Spring",
            "Early Summer",
            "Summer",
            "Late Summer",
            "Early Autumn",
            "Autumn",
            "Late Autumn",
            "Early Winter",
            "Winter",
            "Late Winter"
    };

    public record Instant(
            int year,
            String month,
            int day,
            int hour,
            int minute,
            boolean dayTime) {
    }

    private SkyBlockClock() {
    }

    public static Instant at(long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - EPOCH_MILLIS);
        long days = elapsed / DAY_MILLIS;
        int year = (int) (days / (MONTH_DAYS * YEAR_MONTHS)) + 1;
        int dayOfYear = (int) (days % (MONTH_DAYS * YEAR_MONTHS));
        int monthIndex = dayOfYear / MONTH_DAYS;
        int day = dayOfYear % MONTH_DAYS + 1;
        long intoDay = elapsed % DAY_MILLIS;
        int hour = (int) (intoDay / HOUR_MILLIS);
        int minute = (int) ((intoDay % HOUR_MILLIS) * 60L / HOUR_MILLIS);
        boolean dayTime = hour >= 6 && hour < 18;
        return new Instant(year, MONTHS[monthIndex], day, hour, minute, dayTime);
    }

    public static String formatDate(Instant instant) {
        Instant value = instant == null ? at(System.currentTimeMillis()) : instant;
        return value.month() + " " + ordinal(value.day());
    }

    public static String formatTime(Instant instant, boolean twentyFour, boolean exact) {
        Instant value = instant == null ? at(System.currentTimeMillis()) : instant;
        int minute = exact ? value.minute() : (value.minute() / 10) * 10;
        int hour = value.hour();
        String clock;
        if (twentyFour) {
            clock = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
        } else {
            String suffix = hour >= 12 ? "pm" : "am";
            int twelve = hour % 12;
            if (twelve == 0) {
                twelve = 12;
            }
            clock = String.format(Locale.ROOT, "%d:%02d%s", twelve, minute, suffix);
        }
        String symbol = value.dayTime() ? "§e☀" : "§b☽";
        return "§7" + clock + " " + symbol;
    }

    private static String ordinal(int day) {
        int mod100 = day % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return day + "th";
        }
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }
}
