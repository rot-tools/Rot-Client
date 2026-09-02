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
    private static final Pattern ROW = Pattern.compile(
            "^(.+?)\\s*:\\s*(DONE|COMPLETE|([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*%)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FRACTION = Pattern.compile(
            "^(.+?)\\s*:\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*/\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*$");
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
            "active effects");

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
            String line = AutoConversationPolicy.stripFormatting(raw).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (isCommissionsHeader(line)) {
                inSection = true;
                continue;
            }
            if (inSection && isSectionHeader(line)) {
                inSection = false;
                continue;
            }
            if (!inSection) {
                continue;
            }
            Commission row = parseRow(line);
            if (row != null) {
                out.put(row.name().toLowerCase(Locale.ROOT), row);
            }
        }
        return List.copyOf(out.values());
    }

    public static Commission parseRow(String stripped) {
        if (stripped == null || stripped.isBlank()) {
            return null;
        }
        Matcher matcher = ROW.matcher(stripped.trim());
        if (!matcher.matches()) {
            return parseFractionRow(stripped.trim());
        }
        String name = sanitizeName(matcher.group(1));
        if (!looksLikeCommissionName(name)) {
            return null;
        }
        String status = matcher.group(2);
        if (status == null) {
            return null;
        }
        if (status.equalsIgnoreCase("DONE") || status.equalsIgnoreCase("COMPLETE")) {
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
            return null;
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

    public static boolean isCommissionsMenu(String title) {
        String compact = title == null ? "" : title.replace(":", "").trim().toLowerCase(Locale.ROOT);
        return compact.equals("commissions") || compact.startsWith("commissions ");
    }

    public static boolean loreCompleted(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String text = line == null ? "" : line.replaceAll("§.", "").toUpperCase(Locale.ROOT);
            if (text.contains("COMPLETED")) {
                return true;
            }
        }
        return false;
    }

    static boolean isCommissionsHeader(String line) {
        String compact = line.replace(":", "").trim().toLowerCase(Locale.ROOT);
        return compact.equals("commissions") || compact.startsWith("commissions ");
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
                && !compact.equals("commissions");
    }

    private static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceFirst("^[-•●○]+\\s*", "").trim();
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
        return SECTION_HEADERS.contains(compact);
    }

    private static String formatProgress(Commission commission, boolean colored) {
        String body = commission.done()
                ? "100%"
                : String.format(Locale.ROOT, "%.0f%%", commission.progressPercent());
        if (!colored) {
            return body;
        }
        return percentColorTag(commission.progressPercent()) + body + "§r";
    }
}
