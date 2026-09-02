package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns the SkyBlock calendar's relative "Starts in: 5d 3h 20m" countdown into
 * absolute timestamps.
 */
public final class CalendarDatePolicy {
    public static final String MENU_TITLE = "Calendar and Events";
    private static final String COUNTDOWN_PREFIX = "Starts in: ";
    private static final long SECOND_MS = 1000L;
    private static final long MINUTE_MS = 60L * SECOND_MS;
    private static final long HOUR_MS = 60L * MINUTE_MS;
    private static final long DAY_MS = 24L * HOUR_MS;

    /**
     * One extra tooltip line: a label plus the absolute instant it refers to.
     */
    public record Entry(String label, long epochMs) {
    }

    private CalendarDatePolicy() {
    }

    public static boolean isCalendarMenu(String title) {
        return MENU_TITLE.equals(title);
    }

    /**
     * Parses the countdown text that follows "Starts in: ". Units are matched
     * loosely, so "3h 20m" and "1d" both work.
     */
    public static Optional<Long> countdownMillis(String countdown) {
        if (countdown == null || countdown.isBlank()) {
            return Optional.empty();
        }
        String text = strip(countdown);
        long days = unit(text, 'd');
        long hours = unit(text, 'h');
        long minutes = unit(text, 'm');
        long seconds = unit(text, 's');
        if (days == 0L && hours == 0L && minutes == 0L && seconds == 0L) {
            return Optional.empty();
        }
        return Optional.of(days * DAY_MS + hours * HOUR_MS + minutes * MINUTE_MS + seconds * SECOND_MS);
    }

    /**
     * Rounds up to the next whole five-second mark, because Hypixel only ever
     * ticks the calendar countdown in five second steps.
     */
    public static long roundUpToFiveSeconds(long epochMs) {
        long second = Math.floorMod(Math.floorDiv(epochMs, SECOND_MS), 60L);
        long remainder = second % 5L;
        if (remainder == 0L) {
            return epochMs;
        }
        return epochMs + (5L - remainder) * SECOND_MS;
    }

    /**
     * Extra lines for the hovered calendar entry, or empty when the tooltip is
     * not an event countdown.
     */
    public static List<Entry> entries(boolean enabled, String title, String itemName, List<String> lore, long nowMs) {
        List<Entry> out = new ArrayList<>();
        if (!enabled || !isCalendarMenu(title) || lore == null) {
            return out;
        }
        Optional<Long> countdown = firstCountdown(lore);
        if (countdown.isEmpty()) {
            return out;
        }
        long start = roundUpToFiveSeconds(nowMs + countdown.get());
        out.add(new Entry("§eDate of Event", start));
        String name = strip(itemName == null ? "" : itemName);
        if (name.endsWith("Spooky Festival")) {
            out.add(new Entry("§6Fear Mongerer Arrives", start - HOUR_MS));
        } else if (name.endsWith("Season of Jerry")) {
            out.add(new Entry("§cWorkshop Opens", start - 7L * HOUR_MS - 40L * MINUTE_MS));
        }
        return List.copyOf(out);
    }

    private static Optional<Long> firstCountdown(List<String> lore) {
        for (String line : lore) {
            String plain = strip(line);
            if (plain.startsWith(COUNTDOWN_PREFIX)) {
                return countdownMillis(plain.substring(plain.indexOf(':') + 1));
            }
        }
        return Optional.empty();
    }

    private static long unit(String text, char unit) {
        int index = text.indexOf(unit);
        if (index <= 0) {
            return 0L;
        }
        int start = index;
        while (start > 0 && Character.isDigit(text.charAt(start - 1))) {
            start--;
        }
        if (start == index) {
            return 0L;
        }
        try {
            return Long.parseLong(text.substring(start, index));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String strip(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
    }
}
