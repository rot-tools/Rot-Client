package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses equipped SkyBlock pet data into Pet HUD fields.
 */
public final class PetHudPolicy {
    public static final int LEVEL_COLOR = 0xFFAAAAAA;
    public static final int HELD_LABEL_COLOR = 0xFFAAAAAA;
    public static final int PROGRESS_TEXT_COLOR = 0xFFAAAAAA;
    public static final int PET_FALLBACK_COLOR = 0xFFDDDDDD;
    public static final int HELD_ITEM_FALLBACK_COLOR = 0xFFDDDDDD;

    private static final Pattern LVL_NAME =
            Pattern.compile(
                    "\\[Lvl\\s+(\\d+)\\]\\s*(.+)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PET_LABEL =
            Pattern.compile(
                    "^(?:Active\\s+)?Pet:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PET_EXPERIENCE =
            Pattern.compile(
                    "\"exp\"\\s*:\\s*"
                            + "([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"
                            + "(?:[eE][-+]?\\d+)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PET_TIER =
            Pattern.compile(
                    "\"tier\"\\s*:\\s*\"([A-Z_]+)\"",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PROGRESS_TO_LEVEL =
            Pattern.compile(
                    "Progress\\s+to\\s+Level\\s+(\\d+)"
                            + "\\s*:\\s*"
                            + "(\\d+(?:\\.\\d+)?)%",
                    Pattern.CASE_INSENSITIVE);

    public record Snapshot(
            int level,
            String name,
            String heldItem,
            double experience,
            int petColor,
            int heldItemColor,
            double progressPercent,
            int nextLevel,
            boolean maxLevel) {

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

            petColor =
                    normalizeRarityColor(
                            petColor,
                            PET_FALLBACK_COLOR);

            heldItemColor =
                    normalizeRarityColor(
                            heldItemColor,
                            HELD_ITEM_FALLBACK_COLOR);

            if (maxLevel) {
                progressPercent = 100.0D;
                nextLevel = -1;
            } else if (!Double.isFinite(progressPercent)
                    || progressPercent < 0.0D) {

                progressPercent = -1.0D;
                nextLevel = -1;
            } else {
                progressPercent =
                        Math.max(
                                0.0D,
                                Math.min(
                                        100.0D,
                                        progressPercent));
            }
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
                    PET_FALLBACK_COLOR,
                    HELD_ITEM_FALLBACK_COLOR,
                    -1.0D,
                    -1,
                    false);
        }

        /*
         * Compatibility constructor for the V1 Pet HUD cache/runtime shape.
         */
        public Snapshot(
                int level,
                String name,
                String heldItem,
                double experience,
                int heldItemColor) {

            this(
                    level,
                    name,
                    heldItem,
                    experience,
                    PET_FALLBACK_COLOR,
                    heldItemColor,
                    -1.0D,
                    -1,
                    false);
        }

        public String levelLabel() {
            if (level < 0) {
                return "";
            }

            return "[Lvl " + level + "]";
        }

        public boolean hasProgress() {
            return !maxLevel
                    && progressPercent >= 0.0D
                    && nextLevel > 0;
        }

        public String progressLabel() {
            if (maxLevel) {
                return "MAX LEVEL";
            }

            if (!hasProgress()) {
                return "";
            }

            return String.format(
                    Locale.ROOT,
                    "%.1f%% to Lv %d",
                    progressPercent,
                    nextLevel);
        }
    }

    private record Progress(
            double percent,
            int nextLevel,
            boolean maxLevel) {
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

                if (text
                        .toLowerCase(
                                Locale.ROOT)
                        .startsWith(
                                "held item:")) {

                    held =
                            text.substring(
                                            "held item:"
                                                    .length())
                                    .trim();

                    if (held.equalsIgnoreCase(
                            "none")
                            || held.equals(
                            "\u2716")
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

        Progress progress =
                parseLevelProgress(
                        loreLines);

        return Optional.of(
                new Snapshot(
                        level,
                        name,
                        held,
                        -1.0D,
                        PET_FALLBACK_COLOR,
                        HELD_ITEM_FALLBACK_COLOR,
                        progress.percent(),
                        progress.nextLevel(),
                        progress.maxLevel()));
    }

    public static Snapshot withRuntimeDetails(
            Snapshot snapshot,
            String petInfo,
            int petTextColor,
            int heldItemTextColor,
            List<String> loreLines) {

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

        int petColor =
                petRarityColor(
                        petInfo,
                        petTextColor);

        int heldColor =
                heldItemTextColor == 0
                        ? snapshot.heldItemColor()
                        : normalizeRarityColor(
                                heldItemTextColor,
                                snapshot.heldItemColor());

        Progress parsedProgress =
                parseLevelProgress(
                        loreLines);

        boolean maxLevel =
                parsedProgress.maxLevel()
                        || snapshot.maxLevel();

        double progressPercent =
                parsedProgress.percent() >= 0.0D
                        ? parsedProgress.percent()
                        : snapshot.progressPercent();

        int nextLevel =
                parsedProgress.nextLevel() > 0
                        ? parsedProgress.nextLevel()
                        : snapshot.nextLevel();

        return new Snapshot(
                snapshot.level(),
                snapshot.name(),
                snapshot.heldItem(),
                experience,
                petColor,
                heldColor,
                progressPercent,
                nextLevel,
                maxLevel);
    }

    public static Snapshot mergeTabSnapshot(
            Snapshot existing,
            Snapshot tab) {

        if (existing == null) {
            return tab;
        }

        if (tab == null) {
            return existing;
        }

        if (!tab.name().isEmpty() && !tab.name().equalsIgnoreCase(existing.name())) {
            return tab; // Never attach another pet's XP, held item or rarity to a new pet.
        }
        boolean liveProgress = tab.hasProgress() || tab.maxLevel();
        boolean sameLevel = tab.level() < 0 || tab.level() == existing.level();
        return new Snapshot(
                tab.level() >= 0 ? tab.level() : existing.level(),
                tab.name().isEmpty() ? existing.name() : tab.name(),
                tab.heldItem().isEmpty() ? existing.heldItem() : tab.heldItem(),
                tab.experience() >= 0 ? tab.experience() : existing.experience(),
                existing.petColor(), existing.heldItemColor(),
                liveProgress ? tab.progressPercent() : sameLevel ? existing.progressPercent() : -1,
                liveProgress ? tab.nextLevel() : sameLevel ? existing.nextLevel() : -1,
                liveProgress ? tab.maxLevel() : sameLevel && existing.maxLevel());
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

    static int petRarityColor(
            String petInfo,
            int rawNameColor) {

        if (petInfo != null
                && !petInfo.isBlank()) {

            Matcher matcher =
                    PET_TIER.matcher(
                            petInfo);

            if (matcher.find()) {
                String tier =
                        matcher.group(1)
                                .toUpperCase(
                                        Locale.ROOT);

                return switch (tier) {
                    case "COMMON" ->
                            ItemRarityPolicy.DEFAULT_COMMON;

                    case "UNCOMMON" ->
                            ItemRarityPolicy.DEFAULT_UNCOMMON;

                    case "RARE" ->
                            ItemRarityPolicy.DEFAULT_RARE;

                    case "EPIC" ->
                            ItemRarityPolicy.DEFAULT_EPIC;

                    case "LEGENDARY" ->
                            ItemRarityPolicy.DEFAULT_LEGENDARY;

                    case "MYTHIC" ->
                            ItemRarityPolicy.DEFAULT_MYTHIC;

                    case "DIVINE" ->
                            ItemRarityPolicy.DEFAULT_DIVINE;

                    case "SPECIAL",
                         "VERY_SPECIAL" ->
                            ItemRarityPolicy.DEFAULT_SPECIAL;

                    default ->
                            normalizeRarityColor(
                                    rawNameColor,
                                    PET_FALLBACK_COLOR);
                };
            }
        }

        return normalizeRarityColor(
                rawNameColor,
                PET_FALLBACK_COLOR);
    }

    static int normalizeRarityColor(
            int rawColor) {

        return normalizeRarityColor(
                rawColor,
                HELD_ITEM_FALLBACK_COLOR);
    }

    private static int normalizeRarityColor(
            int rawColor,
            int fallback) {

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

        return fallback;
    }

    private static Progress parseLevelProgress(
            List<String> loreLines) {

        if (loreLines == null
                || loreLines.isEmpty()) {

            return new Progress(
                    -1.0D,
                    -1,
                    false);
        }

        for (String raw : loreLines) {
            String text =
                    MenuKeybindPolicy
                            .stripGuiText(
                                    raw)
                            .trim();

            if (text.isEmpty()) {
                continue;
            }

            String upper =
                    text.toUpperCase(
                            Locale.ROOT);

            if (upper.equals("MAX LEVEL")
                    || upper.equals("MAX LEVEL!")
                    || upper.contains(
                            "MAX LEVEL REACHED")
                    || (upper.startsWith(
                            "PROGRESS TO LEVEL")
                            && upper.contains(
                            "MAXED"))) {

                return new Progress(
                        100.0D,
                        -1,
                        true);
            }

            Matcher progress =
                    PROGRESS_TO_LEVEL
                            .matcher(
                                    text);

            if (progress.find()) {
                try {
                    int nextLevel =
                            Integer.parseInt(
                                    progress.group(1));

                    double percent =
                            Double.parseDouble(
                                    progress.group(2));

                    return new Progress(
                            Math.max(
                                    0.0D,
                                    Math.min(
                                            100.0D,
                                            percent)),
                            nextLevel,
                            false);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed server text.
                }
            }
        }

        return new Progress(
                -1.0D,
                -1,
                false);
    }

    public static Optional<Snapshot> parseTabText(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        List<String> lines = text.lines().map(CommissionDisplayPolicy::normalizeLine).toList();
        String title = null;
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equalsIgnoreCase("Pet:") || line.equalsIgnoreCase("Active Pet:")) {
                for (int j = i + 1; j < Math.min(lines.size(), i + 4); j++) {
                    if (LVL_NAME.matcher(lines.get(j)).matches()) { title = lines.get(j); start = j; break; }
                }
                break;
            }
            Matcher labeled = PET_LABEL.matcher(line);
            if (labeled.matches()) { title = labeled.group(1).trim(); start = i; break; }
        }
        // Compatibility for a standalone pet row, never arbitrary player list entries.
        if (title == null && lines.size() == 1 && LVL_NAME.matcher(lines.getFirst()).matches()) {
            title = lines.getFirst(); start = 0;
        }
        if (title == null || title.equalsIgnoreCase("none") || title.equals("-") || title.equals("✖"))
            return Optional.empty();
        List<String> details = new java.util.ArrayList<>();
        for (int i = start + 1; i < Math.min(lines.size(), start + 7); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) continue;
            if (line.matches("(?i)^(?:Skills|Commissions|Area|Stats|Players|Forge|Profile):.*")) break;
            details.add(line);
        }
        Optional<Snapshot> result = parse(title, details);
        if (result.isEmpty() || result.get().hasProgress() || result.get().maxLevel()) return result;
        Snapshot pet = result.get();
        Pattern fraction = Pattern.compile("(?i)^(?:EXP|XP|Pet EXP|Pet XP)?\\s*:?\\s*([0-9,.]+[kmb]?)\\s*/\\s*([0-9,.]+[kmb]?)(?:\\s.*)?$");
        Pattern percent = Pattern.compile("(?i)^(?:Progress|EXP|XP):\\s*([0-9.]+)%$");
        for (String line : details) {
            Matcher f = fraction.matcher(line), pc = percent.matcher(line);
            double progress = -1;
            if (f.matches()) {
                double current = tabNumber(f.group(1)), total = tabNumber(f.group(2));
                if (total > 0 && current >= 0 && current <= total) progress = 100 * current / total;
            } else if (pc.matches()) progress = tabNumber(pc.group(1));
            if (progress >= 0 && progress <= 100 && pet.level() >= 0) {
                return Optional.of(new Snapshot(pet.level(), pet.name(), pet.heldItem(), -1,
                        pet.petColor(), pet.heldItemColor(), progress, pet.level() + 1, false));
            }
        }
        return result;
    }

    private static double tabNumber(String raw) {
        String number = raw.toLowerCase(Locale.ROOT).replace(",", "");
        double scale = number.endsWith("k") ? 1_000 : number.endsWith("m") ? 1_000_000 : number.endsWith("b") ? 1_000_000_000 : 1;
        if (scale != 1) number = number.substring(0, number.length() - 1);
        try { return Double.parseDouble(number) * scale; }
        catch (NumberFormatException ignored) { return -1; }
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