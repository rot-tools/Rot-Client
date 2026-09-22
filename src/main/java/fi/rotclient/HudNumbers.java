package fi.rotclient;

import java.util.Locale;

/** Number formatting for space-constrained HUD cells. Minecraft-free. */
final class HudNumbers {
    private HudNumbers() {
    }

    /**
     * Short whole-number label of at most four characters for positive
     * values below one billion: 999, 1.2k, 12k, 123k, 1.2m, 12m, 123m.
     */
    static String compactCount(long value) {
        long abs = Math.abs(value);
        if (abs < 1_000L) return Long.toString(value);
        if (abs < 10_000L) return trimmed(value / 1_000.0) + "k";
        if (abs < 1_000_000L) return (value / 1_000L) + "k";
        if (abs < 10_000_000L) return trimmed(value / 1_000_000.0) + "m";
        if (abs < 1_000_000_000L) return (value / 1_000_000L) + "m";
        return trimmed(value / 1_000_000_000.0) + "b";
    }

    private static String trimmed(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0")
                ? text.substring(0, text.length() - 2)
                : text;
    }
}
