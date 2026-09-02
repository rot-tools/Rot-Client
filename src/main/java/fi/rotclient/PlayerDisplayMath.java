package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Player Display projections and EHP math. Never invents unavailable stats.
 */
public final class PlayerDisplayMath {
    public enum StatKind {
        HEALTH,
        MANA,
        OVERFLOW_MANA,
        DEFENSE,
        VITALITY,
        EHP,
        SPEED
    }

    public record HudLine(StatKind kind, String label, String value, boolean available) {
    }

    private PlayerDisplayMath() {
    }

    /**
     * Conventional SkyBlock EHP: health * (1 + defense/100).
     * Requires both health and defense.
     */
    public static OptionalDouble ehp(
            OptionalDouble health,
            OptionalDouble defense) {
        if (health.isEmpty() || defense.isEmpty()) {
            return OptionalDouble.empty();
        }
        double hp = health.getAsDouble();
        double def = defense.getAsDouble();
        if (!Double.isFinite(hp) || !Double.isFinite(def) || hp < 0 || def < 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(hp * (1.0D + def / 100.0D));
    }

    public static List<HudLine> visibleLines(
            SkyBlockStatBarParser.Stats stats,
            boolean health,
            boolean mana,
            boolean overflow,
            boolean defense,
            boolean vitality,
            boolean ehp,
            boolean speed) {
        List<HudLine> lines = new ArrayList<>();
        if (health) {
            lines.add(line(
                    StatKind.HEALTH,
                    "Health",
                    formatPair(stats.health(), stats.maxHealth()),
                    stats.health().isPresent()));
        }
        if (mana) {
            lines.add(line(
                    StatKind.MANA,
                    "Mana",
                    formatPair(stats.mana(), stats.maxMana()),
                    stats.mana().isPresent()));
        }
        if (overflow) {
            lines.add(line(
                    StatKind.OVERFLOW_MANA,
                    "Overflow",
                    SkyBlockStatBarParser.formatStat(stats.overflowMana()),
                    stats.overflowMana().isPresent()));
        }
        if (defense) {
            lines.add(line(
                    StatKind.DEFENSE,
                    "Defense",
                    SkyBlockStatBarParser.formatStat(stats.defense()),
                    stats.defense().isPresent()));
        }
        if (vitality) {
            lines.add(line(
                    StatKind.VITALITY,
                    "Vitality",
                    SkyBlockStatBarParser.formatStat(stats.vitality()),
                    stats.vitality().isPresent()));
        }
        if (ehp) {
            OptionalDouble value = ehp(stats.health(), stats.defense());
            lines.add(line(
                    StatKind.EHP,
                    "EHP",
                    SkyBlockStatBarParser.formatStat(value),
                    value.isPresent()));
        }
        if (speed) {
            lines.add(line(
                    StatKind.SPEED,
                    "Speed",
                    SkyBlockStatBarParser.formatStat(stats.speed()),
                    stats.speed().isPresent()));
        }
        return List.copyOf(lines);
    }

    private static HudLine line(
            StatKind kind,
            String label,
            String value,
            boolean available) {
        return new HudLine(kind, label, value, available);
    }

    private static String formatPair(
            OptionalDouble current,
            OptionalDouble max) {
        if (current.isEmpty()) {
            return "--";
        }
        if (max.isEmpty()) {
            return SkyBlockStatBarParser.formatStat(current);
        }
        return SkyBlockStatBarParser.formatStat(current)
                + "/"
                + SkyBlockStatBarParser.formatStat(max);
    }

    public static String iconFor(StatKind kind) {
        return switch (kind) {
            case HEALTH -> "❤";
            case MANA -> "✎";
            case OVERFLOW_MANA -> "ʬ";
            case DEFENSE -> "❈";
            case VITALITY -> "♨";
            case EHP -> "⛨";
            case SPEED -> "✦";
        };
    }

    public static String displayLabel(StatKind kind, boolean showIcons) {
        return composePrefix(kind, showIcons, true);
    }

    public static String composePrefix(StatKind kind, boolean showIcons, boolean showLabel) {
        String name = switch (kind) {
            case HEALTH -> "Health";
            case MANA -> "Mana";
            case OVERFLOW_MANA -> "Overflow";
            case DEFENSE -> "Defense";
            case VITALITY -> "Vitality";
            case EHP -> "EHP";
            case SPEED -> "Speed";
        };
        if (showIcons && showLabel) {
            return iconFor(kind) + " " + name;
        }
        if (showIcons) {
            return iconFor(kind);
        }
        if (showLabel) {
            return name;
        }
        return "";
    }

    public static String composeLine(
            StatKind kind,
            String value,
            boolean showIcons,
            boolean showLabel) {
        String prefix = composePrefix(kind, showIcons, showLabel);
        String amount = value == null ? "" : value.trim();
        if (prefix.isBlank()) {
            return amount;
        }
        if (amount.isBlank()) {
            return prefix;
        }
        return prefix + " " + amount;
    }

    public static String withMax(String value, boolean showMax) {
        if (showMax || value == null) {
            return value == null ? "" : value;
        }
        int slash = value.indexOf('/');
        if (slash <= 0) {
            return value;
        }
        return value.substring(0, slash).trim();
    }
}
