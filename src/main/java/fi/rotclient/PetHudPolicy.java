package fi.rotclient;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an equipped SkyBlock pet hover name and lore into HUD fields.
 */
public final class PetHudPolicy {
    public static final int LEVEL_COLOR = 0xFFAAAAAA;
    public static final int NAME_COLOR = 0xFFFFAA00;
    public static final int HELD_LABEL_COLOR = 0xFFFFAA00;

    private static final Pattern LVL_NAME =
            Pattern.compile("\\[Lvl\\s+(\\d+)\\]\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_LABEL =
            Pattern.compile("^(?:Active\\s+)?Pet:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    public record Snapshot(int level, String name, String heldItem) {
        public String levelLabel() {
            if (level < 0) {
                return "";
            }
            return "[Lvl " + level + "]";
        }
    }

    private PetHudPolicy() {
    }

    public static Optional<Snapshot> parse(String hoverName, List<String> loreLines) {
        String title = MenuKeybindPolicy.stripGuiText(hoverName);
        if (title.isEmpty()) {
            return Optional.empty();
        }
        int level = -1;
        String name = title;
        Matcher matcher = LVL_NAME.matcher(title);
        if (matcher.find()) {
            level = Integer.parseInt(matcher.group(1));
            name = matcher.group(2).trim();
        }
        String held = "";
        if (loreLines != null) {
            for (String raw : loreLines) {
                String text = MenuKeybindPolicy.stripGuiText(raw);
                if (text.toLowerCase().startsWith("held item:")) {
                    held = text.substring("held item:".length()).trim();
                    if (held.equalsIgnoreCase("none")
                            || held.equals("✖")
                            || held.equals("-")) {
                        held = "";
                    }
                    break;
                }
            }
        }
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Snapshot(level, name, held));
    }

    /**
     * Reads an equipped pet from tab-list header/footer/widget text.
     */
    public static Optional<Snapshot> parseTabText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Optional<Snapshot> fromLvl = Optional.empty();
        for (String rawLine : MenuKeybindPolicy.stripGuiText(text).split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher labeled = PET_LABEL.matcher(line);
            if (labeled.find()) {
                String rest = labeled.group(1).trim();
                if (rest.equalsIgnoreCase("none")
                        || rest.equals("-")
                        || rest.equals("✖")
                        || rest.isEmpty()) {
                    continue;
                }
                Optional<Snapshot> parsed = parse(rest, List.of());
                if (parsed.isPresent()) {
                    return parsed;
                }
                return Optional.of(new Snapshot(-1, rest, ""));
            }
            Matcher lvl = LVL_NAME.matcher(line);
            if (lvl.find()) {
                fromLvl = Optional.of(new Snapshot(
                        Integer.parseInt(lvl.group(1)),
                        lvl.group(2).trim(),
                        ""));
            }
        }
        return fromLvl;
    }

    public static boolean loreMeansEquipped(List<String> loreLines) {
        if (loreLines == null) {
            return false;
        }
        for (String raw : loreLines) {
            String text = MenuKeybindPolicy.stripGuiText(raw).toLowerCase();
            if (text.contains("despawn")
                    || text.contains("this pet is spawned")
                    || text.equals("spawned")
                    || text.contains("currently equipped")
                    || text.equals("equipped")) {
                return true;
            }
        }
        return false;
    }
}
