package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an equipped SkyBlock pet hover name and lore into HUD fields.
 */
public final class PetHudPolicy {
    public static final int LEVEL_COLOR = 0xFFAAAAAA;
    public static final int NAME_COLOR = 0xFFFFAA00;
    public static final int HELD_LABEL_COLOR = 0xFFAAAAAA;
    public static final int XP_COLOR = 0xFFAAAAAA;
    public static final int HELD_ITEM_FALLBACK_COLOR = 0xFFDDDDDD;

    private static final Pattern LVL_NAME =
            Pattern.compile(
                    "\\[Lvl\\s+(\\d+)\\]\\s*(.+)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PET_LABEL =
            Pattern.compile(
                    "^(?:Active\\s+)?Pet:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE);

    /*
     * Hypixel stores the pet's total experience inside ExtraAttributes
     * petInfo JSON. Keep the parser deliberately narrow: only read exp.
     */
    private static final Pattern PET_EXPERIENCE =
            Pattern.compile(
                    "\"exp\"\\s*:\\s*"
                            + "([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"
                            + "(?:[eE][-+]?\\d+)?)",
                    Pattern.CASE_INSENSITIVE);

    public record Snapshot(
            int level,
            String name,
            String heldItem,
            double experience,
            int heldItemColor) {

        public Snapshot {
            name =
                    name == null
                            ? ""
                            : name;

            heldItem =
                    heldItem == null
                            ? ""
                            : heldItem;

            if (!Double.isFinite(experience)
                    || experience < 0.0D) {

                experience = -1.0D;
            }

            heldItemColor =
                    normalizeRarityColor(
                            heldItemColor);
        }

        public Snapshot(
                int level,
                String name,
                String heldItem) {

            this(
                    level,
                    name,
                    heldItem,
                    -1.0D,
                    HELD_ITEM_FALLBACK_COLOR);
        }

        public String levelLabel() {
            if (level < 0) {
                return "";
            }

            return "[Lvl " + level + "]";
        }

        public String experienceLabel() {
            if (experience < 0.0D
                    || !Double.isFinite(experience)) {

                return "";
            }

            long rounded =
                    Math.max(
                            0L,
                            Math.round(experience));

            return "XP: "
                    + String.format(
                    Locale.ROOT,
                    "%,d",
                    rounded);
        }
    }

    private PetHudPolicy() {
    }

    public static Optional<Snapshot> parse(
            String hoverName,
            List<String> loreLines) {

        String title =
                MenuKeybindPolicy
                        .stripGuiText(
                                hoverName);

        if (title.isEmpty()) {
            return Optional.empty();
        }

        int level = -1;
        String name = title;

        Matcher matcher =
                LVL_NAME.matcher(
                        title);

        if (matcher.find()) {
            level =
                    Integer.parseInt(
                            matcher.group(1));

            name =
                    matcher.group(2)
                            .trim();
        }

        String held = "";

        if (loreLines != null) {
            for (String raw : loreLines) {
                String text =
                        MenuKeybindPolicy
                                .stripGuiText(
                                        raw);

                if (text.toLowerCase(Locale.ROOT)
                        .startsWith(
                                "held item:")) {

                    held =
                            text.substring(
                                            "held item:"
                                                    .length())
                                    .trim();

                    if (held.equalsIgnoreCase(
                            "none")
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

        return Optional.of(
                new Snapshot(
                        level,
                        name,
                        held));
    }

    /**
     * Add runtime-only details that are not available from flattened lore:
     * total pet XP from petInfo and the held item's styled rarity color.
     */
    public static Snapshot withRuntimeDetails(
            Snapshot snapshot,
            String petInfo,
            int heldItemTextColor) {

        if (snapshot == null) {
            return null;
        }

        double parsedExperience =
                parseExperience(
                        petInfo);

        double experience =
                parsedExperience >= 0.0D
                        ? parsedExperience
                        : snapshot.experience();

        int heldColor =
                heldItemTextColor == 0
                        ? snapshot.heldItemColor()
                        : heldItemTextColor;

        return new Snapshot(
                snapshot.level(),
                snapshot.name(),
                snapshot.heldItem(),
                experience,
                heldColor);
    }

    static double parseExperience(
            String petInfo) {

        if (petInfo == null
                || petInfo.isBlank()) {

            return -1.0D;
        }

        Matcher matcher =
                PET_EXPERIENCE
                        .matcher(
                                petInfo);

        if (!matcher.find()) {
            return -1.0D;
        }

        try {
            double value =
                    Double.parseDouble(
                            matcher.group(1));

            return Double.isFinite(value)
                    && value >= 0.0D
                    ? value
                    : -1.0D;
        } catch (NumberFormatException ignored) {
            return -1.0D;
        }
    }

    /**
     * Convert the actual styled lore color to the existing Rot/SkyBlock
     * rarity palette. Unknown/non-rarity colors fall back to neutral text.
     */
    static int normalizeRarityColor(
            int rawColor) {

        int rgb =
                rawColor
                        & 0x00FFFFFF;

        if (rgb == (ItemRarityPolicy.DEFAULT_COMMON
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_COMMON;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_UNCOMMON
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_UNCOMMON;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_RARE
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_RARE;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_EPIC
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_EPIC;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_LEGENDARY
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_LEGENDARY;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_MYTHIC
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_MYTHIC;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_DIVINE
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_DIVINE;
        }

        if (rgb == (ItemRarityPolicy.DEFAULT_SPECIAL
                & 0x00FFFFFF)) {

            return ItemRarityPolicy.DEFAULT_SPECIAL;
        }

        return HELD_ITEM_FALLBACK_COLOR;
    }

    /**
     * Reads an equipped pet from tab-list header/footer/widget text.
     */
    public static Optional<Snapshot> parseTabText(
            String text) {

        if (text == null
                || text.isBlank()) {

            return Optional.empty();
        }

        Optional<Snapshot> fromLvl =
                Optional.empty();

        for (String rawLine
                : MenuKeybindPolicy
                .stripGuiText(text)
                .split("\\R")) {

            String line =
                    rawLine.trim();

            if (line.isEmpty()) {
                continue;
            }

            Matcher labeled =
                    PET_LABEL.matcher(
                            line);

            if (labeled.find()) {
                String rest =
                        labeled.group(1)
                                .trim();

                if (rest.equalsIgnoreCase(
                        "none")
                        || rest.equals("-")
                        || rest.equals("✖")
                        || rest.isEmpty()) {

                    continue;
                }

                Optional<Snapshot> parsed =
                        parse(
                                rest,
                                List.of());

                if (parsed.isPresent()) {
                    return parsed;
                }

                return Optional.of(
                        new Snapshot(
                                -1,
                                rest,
                                ""));
            }

            Matcher lvl =
                    LVL_NAME.matcher(
                            line);

            if (lvl.find()) {
                fromLvl =
                        Optional.of(
                                new Snapshot(
                                        Integer.parseInt(
                                                lvl.group(1)),
                                        lvl.group(2)
                                                .trim(),
                                        ""));
            }
        }

        return fromLvl;
    }

    public static boolean loreMeansEquipped(
            List<String> loreLines) {

        if (loreLines == null) {
            return false;
        }

        for (String raw : loreLines) {
            String text =
                    MenuKeybindPolicy
                            .stripGuiText(
                                    raw)
                            .toLowerCase(
                                    Locale.ROOT);

            if (text.contains(
                    "despawn")
                    || text.contains(
                    "this pet is spawned")
                    || text.equals(
                    "spawned")
                    || text.contains(
                    "currently equipped")
                    || text.equals(
                    "equipped")) {

                return true;
            }
        }

        return false;
    }
}