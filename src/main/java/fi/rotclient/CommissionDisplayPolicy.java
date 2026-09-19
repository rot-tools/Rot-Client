package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Commission display: parse tab-list commission rows and format HUD
 * lines with {@code #name} / {@code #progress} placeholders.
 */
public final class CommissionDisplayPolicy {
    public static final String DEFAULT_TITLE = "<red>Commissions:";
    public static final String DEFAULT_NONE = "<red>No commissions available!";
    public static final String DEFAULT_ROW = "<gray>- <r>#name: #progress";
    /** Hypixel shows at most four commissions; more than this means the parser ran past the widget. */
    public static final int MAX_COMMISSIONS = 8;
    /** How long the last good result stays up when a tab-list refresh briefly loses the widget. */
    public static final long HOLD_MILLIS = 5_000L;
    private static final Pattern SECTION_CODE = Pattern.compile("§.");
    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200F\\u2060\\uFEFF\\u00AD]");
    /** Non-breaking, thin and other Unicode spaces count as spaces; {@code \s} alone misses them. */
    private static final Pattern SPACES = Pattern.compile("[\\p{Z}\\s]+");
    private static final Pattern LEADING_BULLET = Pattern.compile("^[\\s\\-•●○▪▫◆◇►▶➤]+");
    private static final Pattern ROW = Pattern.compile(
            "^(.+?)\\s*[:\\-–—]\\s*(DONE|COMPLETE|COMPLETED|([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*%)[!.]*\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FRACTION = Pattern.compile(
            "^(.+?)\\s*[:\\-–—]\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*/\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*$");
    private static final Pattern BARE_PERCENT = Pattern.compile(
            "^(.+?)\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*%\\s*$");
    private static final Set<String> SECTION_HEADERS = Set.of(
            "area",
            "profile",
            "skills",
            "collection",
            "event",
            "dungeon",
            "slayer",
            "account",
            "server",
            "info",
            "guest",
            "guests",
            "stats",
            "pet",
            "powders",
            "crystals",
            "forge",
            "forges",
            "timers",
            "frozen corpses",
            "scrap",
            "fairy souls",
            "soulflow",
            "bank",
            "sb level",
            "gems",
            "players",
            "coop",
            "island",
            "minions",
            "bestiary",
            "essence",
            "essences",
            "party",
            "daily quests",
            "active effects",
            "commission progress",
            "hotm",
            "heart of the mountain");

    public record Commission(String name, float progressPercent, boolean done) {
        public Commission {
            name = name == null ? "" : name.trim();
            progressPercent = Math.max(0.0F, Math.min(100.0F, progressPercent));
        }
    }

    private CommissionDisplayPolicy() {
    }

    public static List<Commission> parseTabLines(List<String> strippedLines) {
        Map<String, Commission> out = new LinkedHashMap<>();
        if (strippedLines == null) {
            return List.of();
        }
        boolean inSection = false;
        for (String raw : strippedLines) {
            String line = normalizeLine(raw);
            if (line.isEmpty()) {
                continue;
            }
            if (isCommissionsHeader(line)) {
                inSection = true;
                continue;
            }
            if (!inSection) {
                continue;
            }
            Commission row = parseRow(line);
            if (row != null) {
                out.put(row.name().toLowerCase(Locale.ROOT), row);
                if (out.size() >= MAX_COMMISSIONS) {
                    break;
                }
                continue;
            }
            // The next widget ends the section, whether or not it is a header we have heard of.
            if (isSectionHeader(line)) {
                inSection = false;
            }
        }
        return List.copyOf(out.values());
    }

    /**
     * Tab rows carry colour codes, and Hypixel pads them with non-breaking and zero-width
     * characters that {@code trim()} and {@code \s} do not treat as spaces. Without this a row like
     * {@code "Mithril Miner: 40%\u00A0"} silently failed to parse.
     */
    public static String normalizeLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String text = raw;
        if (text.indexOf('§') >= 0) {
            text = SECTION_CODE.matcher(text).replaceAll("");
        }
        text = INVISIBLE.matcher(text).replaceAll("");
        return SPACES.matcher(text).replaceAll(" ").trim();
    }

    /**
     * Keeps the last good list on screen for {@link #HOLD_MILLIS} when a refresh comes back empty,
     * so a tab-list rebuild does not flash "No commissions available!". A real change (a non-empty
     * parse) always wins immediately.
     */
    public static List<Commission> retain(
            List<Commission> parsed,
            List<Commission> held,
            long heldAtMillis,
            long nowMillis) {
        if (parsed != null && !parsed.isEmpty()) {
            return parsed;
        }
        if (held == null || held.isEmpty()) {
            return List.of();
        }
        long age = nowMillis - heldAtMillis;
        return age >= 0L && age < HOLD_MILLIS ? held : List.of();
    }

    public static Commission parseRow(String stripped) {
        if (stripped == null || stripped.isBlank()) {
            return null;
        }
        String normalized = normalizeLine(stripped);
        if (normalized.isEmpty()) {
            return null;
        }
        Matcher matcher = ROW.matcher(normalized);
        if (!matcher.matches()) {
            return parseFractionRow(normalized);
        }
        String name = sanitizeName(matcher.group(1));
        if (!looksLikeCommissionName(name)) {
            return null;
        }
        String status = matcher.group(2);
        if (status == null) {
            return null;
        }
        if (status.equalsIgnoreCase("DONE")
                || status.equalsIgnoreCase("COMPLETE")
                || status.equalsIgnoreCase("COMPLETED")) {
            return new Commission(name, 100.0F, true);
        }
        try {
            float percent = Float.parseFloat(matcher.group(3).replace(",", ""));
            return new Commission(name, percent, percent >= 100.0F);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Commission parseFractionRow(String stripped) {
        Matcher matcher = FRACTION.matcher(stripped);
        if (!matcher.matches()) {
            return parseBarePercentRow(stripped);
        }
        String name = sanitizeName(matcher.group(1));
        if (!looksLikeCommissionName(name)) {
            return null;
        }
        try {
            float current = Float.parseFloat(matcher.group(2).replace(",", ""));
            float max = Float.parseFloat(matcher.group(3).replace(",", ""));
            if (max <= 0.0F) {
                return null;
            }
            float percent = 100.0F * current / max;
            return new Commission(name, percent, percent >= 100.0F);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Commission parseBarePercentRow(String stripped) {
        Matcher matcher = BARE_PERCENT.matcher(stripped);
        if (!matcher.matches()) {
            return null;
        }
        String name = sanitizeName(matcher.group(1));
        if (!looksLikeCommissionName(name)) {
            return null;
        }
        try {
            float percent = Float.parseFloat(matcher.group(2).replace(",", ""));
            return new Commission(name, percent, percent >= 100.0F);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean isCommissionsMenu(String title) {
        String compact = title == null ? "" : title.replace(":", "").trim().toLowerCase(Locale.ROOT);
        return compact.equals("commissions") || compact.startsWith("commissions ");
    }

    public static boolean loreCompleted(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String text = line == null
                    ? ""
                    : SECTION_CODE.matcher(line).replaceAll("").toUpperCase(Locale.ROOT);
            if (text.contains("COMPLETED")) {
                return true;
            }
        }
        return false;
    }

    static boolean isCommissionsHeader(String line) {
        String compact = line.replace(":", "").trim().toLowerCase(Locale.ROOT);
        return compact.equals("commissions")
                || compact.equals("commission progress")
                || compact.startsWith("commissions ");
    }

    static boolean looksLikeCommissionName(String name) {
        if (name == null || name.length() < 3) {
            return false;
        }
        String compact = name.toLowerCase(Locale.ROOT);
        return !SECTION_HEADERS.contains(compact)
                && !compact.equals("ping")
                && !compact.equals("fps")
                && !compact.equals("cookies")
                && !compact.equals("commissions")
                && !compact.equals("commission progress");
    }

    private static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return LEADING_BULLET.matcher(raw).replaceFirst("").trim();
    }

    public static String formatLine(
            String template,
            Commission commission,
            boolean coloredPercent) {
        String pattern = template == null || template.isBlank() ? DEFAULT_ROW : template;
        String progress = formatProgress(commission, coloredPercent);
        return applyLegacyTags(pattern
                .replace("#name", commission.name())
                .replace("#progress", progress));
    }

    public static String formatTitle(String template) {
        String text = template == null || template.isBlank() ? DEFAULT_TITLE : template;
        return applyLegacyTags(text);
    }

    public static String formatNone(String template) {
        String text = template == null || template.isBlank() ? DEFAULT_NONE : template;
        return applyLegacyTags(text);
    }

    public static String percentColorTag(float percent) {
        float p = Math.max(0.0F, Math.min(100.0F, percent));
        if (p >= 100.0F) {
            return "§a";
        }
        if (p >= 75.0F) {
            return "§3";
        }
        if (p >= 50.0F) {
            return "§e";
        }
        if (p >= 25.0F) {
            return "§6";
        }
        return "§c";
    }

    public static String applyLegacyTags(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("<red>", "§c")
                .replace("<gray>", "§7")
                .replace("<yellow>", "§e")
                .replace("<gold>", "§6")
                .replace("<green>", "§a")
                .replace("<aqua>", "§b")
                .replace("<r>", "§r")
                .replace("<reset>", "§r");
    }

    static boolean isSectionHeader(String line) {
        String compact = line.replace(":", "").trim().toLowerCase(Locale.ROOT);
        if (compact.startsWith("players") || compact.startsWith("guests")) {
            return true;
        }
        return SECTION_HEADERS.contains(compact) || isWidgetHeader(line);
    }

    /**
     * A tab widget title is a short label ending in a colon with nothing after it, such as
     * {@code "Fossil Dust:"}. Recognising the shape means a widget added by Hypixel later still
     * ends the commission list, instead of its rows being read as commissions.
     */
    static boolean isWidgetHeader(String line) {
        if (line == null) {
            return false;
        }
        String text = line.trim();
        if (!text.endsWith(":")) {
            return false;
        }
        String label = text.substring(0, text.length() - 1).trim();
        if (label.length() < 2 || label.length() > 40) {
            return false;
        }
        for (int i = 0; i < label.length(); i++) {
            if (Character.isLetter(label.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String formatProgress(Commission commission, boolean colored) {
        // Rounded down, so 99.6% reads 99% instead of a "100%" that is not finished.
        String body = commission.done()
                ? "100%"
                : ((int) Math.floor(commission.progressPercent())) + "%";
        if (!colored) {
            return body;
        }
        return percentColorTag(commission.progressPercent()) + body + "§r";
    }
}
