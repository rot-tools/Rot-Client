package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel "Your Skills" item lore so the GUI can overlay the current
 * level. Maxed skills use a distinct color chosen in config (aqua by default).
 */
public final class SkillLevelOverlayPolicy {
    public static final int DEFAULT_LEVEL_COLOR = 0xFFFFFFFF;
    public static final int DEFAULT_MAX_COLOR = 0xFF55FFFF;
    public static final int LABEL_BACKGROUND = 0xE0101018;

    public record LabelBox(int left, int top, int right, int bottom) {
    }

    private static final Pattern LEVEL_LINE =
            Pattern.compile(
                    "(?:Current\\s+)?(?:Skill\\s+)?(?:Level|Lvl\\.?|Lv\\.?)[:\\s]+(\\d+)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern SKIP_LINE =
            Pattern.compile("unlocks|requires|rewards|^progress\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRESS_TO =
            Pattern.compile("Progress to Level\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOVER_TRAILING_LEVEL =
            Pattern.compile("^(?:[A-Za-z][A-Za-z '\\-/]*?)\\s+(\\d{1,2})$");
    private static final Pattern HOVER_ROMAN =
            Pattern.compile("^(?:[A-Za-z][A-Za-z '\\-/]*?)\\s+([IVXLCDM]{1,6})$");

    public record Overlay(int level, boolean max) {
    }

    private SkillLevelOverlayPolicy() {
    }

    public static Optional<Overlay> parse(List<String> loreLines) {
        return parse("", loreLines, 0);
    }

    public static Optional<Overlay> parse(String hoverName, List<String> loreLines, int stackCount) {
        Integer level = null;
        boolean max = false;
        boolean inProgress = false;
        Integer progressImplied = null;
        String skillHint = hoverName;
        if (loreLines != null) {
            for (String raw : loreLines) {
                String text = MenuKeybindPolicy.stripGuiText(raw);
                if (text.isEmpty()) {
                    continue;
                }
                if (isSkillName(text)) {
                    skillHint = text;
                }
                if (isMaxPhrase(text)) {
                    max = true;
                }
                Matcher progress = PROGRESS_TO.matcher(text);
                if (progress.find()) {
                    int next = Integer.parseInt(progress.group(1));
                    if (next > 1) {
                        progressImplied = next - 1;
                    }
                    if (!isMaxPhrase(text)) {
                        inProgress = true;
                    }
                }
                if (SKIP_LINE.matcher(text).find()) {
                    continue;
                }
                Matcher matcher = LEVEL_LINE.matcher(text);
                if (matcher.find()) {
                    level = Integer.parseInt(matcher.group(1));
                }
            }
        }
        if (level == null) {
            String hover = MenuKeybindPolicy.stripGuiText(hoverName);
            Matcher hoverMatch = HOVER_TRAILING_LEVEL.matcher(hover);
            if (hoverMatch.matches()) {
                level = Integer.parseInt(hoverMatch.group(1));
            } else {
                Matcher roman = HOVER_ROMAN.matcher(hover);
                if (roman.matches()) {
                    int parsed = romanToInt(roman.group(1));
                    if (parsed > 0) {
                        level = parsed;
                    }
                }
            }
        }
        if (level == null) {
            level = progressImplied;
        }
        if (level == null && stackCount >= 2 && stackCount <= 60) {
            level = stackCount;
        }
        if (level == null && stackCount >= 1 && stackCount <= 60 && isSkillName(hoverName)) {
            level = stackCount;
        }
        if (level == null) {
            return Optional.empty();
        }
        if (!max && !inProgress) {
            int cap = maxLevelFor(skillHint);
            if (cap <= 0) {
                cap = maxLevelFor(hoverName);
            }
            if (cap > 0 && level >= cap) {
                max = true;
            }
        }
        return Optional.of(new Overlay(level, max));
    }

    public static int colorFor(boolean max, int inProgressArgb, int maxArgb) {
        int chosen = max ? maxArgb : inProgressArgb;
        if ((chosen >>> 24) == 0) {
            chosen |= 0xFF000000;
        }
        return chosen;
    }

    /** Bottom-right of a 16px item, matching vanilla stack counts. */
    public static int labelX(int slotX, int textWidth) {
        return slotX + 17 - Math.max(0, textWidth);
    }

    public static int labelY(int slotY) {
        return slotY + 8;
    }

    public static LabelBox labelBackground(int slotX, int slotY, int textWidth) {
        int x = labelX(slotX, textWidth);
        int y = labelY(slotY);
        int width = Math.max(1, textWidth);
        return new LabelBox(x - 1, y - 1, x + width + 1, y + 9);
    }

    static boolean isMaxPhrase(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("maxed")
                || lower.contains("max level")
                || lower.contains("maximum level")
                || lower.contains("reached the max");
    }

    static int maxLevelFor(String name) {
        String hover = MenuKeybindPolicy.stripGuiText(name).toLowerCase(Locale.ROOT);
        if (hover.contains("runecrafting") || hover.contains("social")) {
            return 25;
        }
        if (hover.contains("foraging")) {
            return 57;
        }
        if (hover.contains("farming")
                || hover.contains("mining")
                || hover.contains("combat")
                || hover.contains("enchanting")
                || hover.contains("taming")) {
            return 60;
        }
        if (hover.contains("fishing")
                || hover.contains("alchemy")
                || hover.contains("carpentry")
                || hover.contains("hunting")
                || hover.contains("catacombs")) {
            return 50;
        }
        return 0;
    }

    static boolean isSkillName(String hoverName) {
        String hover = MenuKeybindPolicy.stripGuiText(hoverName);
        return hover.matches("(?i).*(farming|mining|combat|foraging|fishing|enchanting|alchemy|taming|carpentry|runecrafting|social|hunting|catacombs).*");
    }

    static int romanToInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String value = raw.trim().toUpperCase();
        int total = 0;
        int prev = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            int current = switch (value.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> 0;
            };
            if (current == 0) {
                return 0;
            }
            total += current < prev ? -current : current;
            prev = current;
        }
        return total > 0 && total <= 60 ? total : 0;
    }
}
