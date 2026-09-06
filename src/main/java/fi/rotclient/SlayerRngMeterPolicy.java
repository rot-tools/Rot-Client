package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft-free RNG Meter chat and inventory rules. The live HUD needs both
 * the selected drop and Stored XP; opening {@code /rng} is the reliable source
 * when Hypixel never reprints the selection chat line.
 */
public final class SlayerRngMeterPolicy {
    private static final Pattern CHAT_SELECTION = Pattern.compile(
            "(?i)you set your (?<family>.+) rng meter to drop (?<item>.+?)!");
    private static final Pattern CHAT_STORED_XP = Pattern.compile(
            "(?i)rng meter\\s*-\\s*(?<xp>[\\d,]+)\\s*stored xp");
    private static final Pattern BOOK_NAME = Pattern.compile(
            "(?i)enchanted book\\s*\\((?<name>.+)\\)");
    private static final Pattern METER_TITLE = Pattern.compile(
            "(?i)^(?<name>.+?) rng meter(?:\\s*[\\(\\[]\\s*\\d+\\s*/\\s*\\d+\\s*[\\)\\]])?$");
    private static final Pattern PAGE_SUFFIX = Pattern.compile(
            "(?i)\\s*[\\(\\[]\\s*\\d+\\s*/\\s*\\d+\\s*[\\)\\]]\\s*$");
    private static final Pattern PROGRESS = Pattern.compile(
            "(?i)(?:progress|stored xp)\\s*:?\\s*([\\d,.]+[kKmM]?)(?:\\s*/\\s*([\\d,.]+[kKmM]?))?");
    private static final Pattern SELECTED_DROP = Pattern.compile(
            "(?i)selected drop\\s*:?\\s*(?<item>.+)");
    private static final Pattern ENCHANT_LINE = Pattern.compile(
            "(?i)^[a-z][a-z0-9' ]+ [ivxlcdm]+$");
    private static final Set<String> NON_SLAYER_METERS = Set.of(
            "catacombs",
            "dungeon",
            "master mode",
            "crystal nucleus",
            "experimentation table",
            "frozen corpse");

    public record SlotView(int index, String name, List<String> lore) {
        public SlotView {
            name = name == null ? "" : name;
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    public record Selection(String itemName, long storedXp, SlayerPolicy.SlayerType family) {
        public Selection {
            itemName = itemName == null ? "" : itemName.trim();
            storedXp = Math.max(-1L, storedXp);
        }

        public Selection(String itemName, long storedXp) {
            this(itemName, storedXp, null);
        }

        public boolean hasItem() {
            return !itemName.isBlank();
        }
    }

    private SlayerRngMeterPolicy() {
    }

    public static boolean shouldWarnEmpty(boolean enabled, boolean rngMeterUpdate, boolean selectedDropKnown) {
        return enabled && rngMeterUpdate && !selectedDropKnown;
    }

    /**
     * The server's standard Stored XP status line is safe to hide only after a
     * known selection has been observed locally.  An unknown selection must
     * remain visible so the player can diagnose the missing mapping.
     */
    public static boolean shouldHideChat(boolean enabled, boolean rngMeterUpdate, boolean selectedDropKnown) {
        return enabled && rngMeterUpdate && selectedDropKnown;
    }

    public static Optional<String> chatSelection(String raw) {
        String line = strip(raw);
        if (line.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CHAT_SELECTION.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String item = matcher.group("item");
        return item == null || item.isBlank() ? Optional.empty() : Optional.of(item.trim());
    }

    public static Optional<SlayerPolicy.SlayerType> chatFamily(String raw) {
        String line = strip(raw);
        if (line.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CHAT_SELECTION.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return familyFromTitle(matcher.group("family"));
    }

    public static Optional<Long> chatStoredXp(String raw) {
        String line = strip(raw);
        if (line.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CHAT_STORED_XP.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(matcher.group("xp").replace(",", "")));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isRngMeterInventory(String title) {
        String text = strip(title);
        if (text.isBlank()) {
            return false;
        }
        Matcher matcher = METER_TITLE.matcher(text);
        if (!matcher.matches()) {
            return false;
        }
        return !isNonSlayerMeter(matcher.group("name"));
    }

    public static boolean isSlayerMenu(String title) {
        String text = strip(title);
        return text.equalsIgnoreCase("Slayer") || text.equalsIgnoreCase("Slayer Menu");
    }

    public static boolean isSlayerTypeMenu(String title) {
        if (isRngMeterInventory(title) || isSlayerMenu(title)) {
            return false;
        }
        return familyFromTitle(title).isPresent();
    }

    public static Optional<SlayerPolicy.SlayerType> familyFromTitle(String title) {
        String text = PAGE_SUFFIX.matcher(strip(title)).replaceFirst("");
        Matcher meter = METER_TITLE.matcher(text);
        if (meter.matches()) {
            text = strip(meter.group("name"));
        }
        if (text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (isNonSlayerMeter(text)) {
            return Optional.empty();
        }
        for (SlayerPolicy.SlayerType type : SlayerPolicy.SlayerType.values()) {
            String display = type.displayName().toLowerCase(Locale.ROOT);
            if (lower.equals(display) || lower.startsWith(display + " ") || lower.contains(display)) {
                return Optional.of(type);
            }
            for (String alias : type.aliases()) {
                String key = alias.toLowerCase(Locale.ROOT);
                if (lower.equals(key) || lower.startsWith(key + " ") || lower.contains(key)) {
                    return Optional.of(type);
                }
            }
        }
        return switch (lower) {
            case "enderman", "enderman slayer", "eman", "void" -> Optional.of(SlayerPolicy.SlayerType.VOIDGLOOM);
            case "zombie", "zombie slayer", "rev" -> Optional.of(SlayerPolicy.SlayerType.REVENANT);
            case "spider", "spider slayer", "tara" -> Optional.of(SlayerPolicy.SlayerType.TARANTULA);
            case "wolf", "wolf slayer" -> Optional.of(SlayerPolicy.SlayerType.SVEN);
            case "blaze", "blaze slayer" -> Optional.of(SlayerPolicy.SlayerType.INFERNO);
            case "vamp", "vampire", "vampire slayer" -> Optional.of(SlayerPolicy.SlayerType.VAMPIRE);
            default -> Optional.empty();
        };
    }

    public static boolean loreMeansSelected(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String text = strip(line);
            String lower = text.toLowerCase(Locale.ROOT);
            if (text.equalsIgnoreCase("SELECTED")
                    || text.toUpperCase(Locale.ROOT).endsWith("SELECTED")
                    || lower.contains("click to deselect")
                    || lower.contains("click to unselect")
                    || lower.contains("currently selected")
                    || lower.contains("currently active")) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Selection> fromRngMeterInventory(String title, List<SlotView> slots) {
        if (!isRngMeterInventory(title) || slots == null) {
            return Optional.empty();
        }
        SlayerPolicy.SlayerType family = familyFromTitle(title).orElse(null);
        for (SlotView slot : slots) {
            if (slot.name().isBlank() || !loreMeansSelected(slot.lore())) {
                continue;
            }
            long stored = storedXpFromLore(slot.lore()).orElse(-1L);
            return Optional.of(new Selection(itemName(slot), stored, family));
        }
        return Optional.empty();
    }

    public static Optional<Selection> fromSlayerMenu(String title, List<SlotView> slots) {
        if (slots == null || (!isSlayerMenu(title) && !isSlayerTypeMenu(title))) {
            return Optional.empty();
        }
        SlayerPolicy.SlayerType family = familyFromTitle(title).orElse(null);
        Optional<Selection> namedMeter = Optional.empty();
        for (SlotView slot : slots) {
            if (!looksLikeRngMeterItem(slot)) {
                continue;
            }
            Optional<Selection> parsed = selectionFromMeterItem(slot, family);
            if (parsed.isEmpty()) {
                continue;
            }
            if (family == null) {
                family = familyFromMeterItem(slot).orElse(null);
                parsed = Optional.of(new Selection(
                        parsed.get().itemName(), parsed.get().storedXp(), family));
            }
            if (slot.index() == 35) {
                return parsed;
            }
            if (namedMeter.isEmpty()) {
                namedMeter = parsed;
            }
        }
        return namedMeter;
    }

    public static String displayName(String raw) {
        String text = strip(raw);
        Matcher book = BOOK_NAME.matcher(text);
        if (book.matches()) {
            String inner = book.group("name");
            return inner == null ? text : strip(inner);
        }
        return text;
    }

    static Optional<Long> storedXpFromLore(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (String line : lore) {
            Matcher matcher = PROGRESS.matcher(strip(line));
            if (!matcher.find()) {
                continue;
            }
            try {
                return Optional.of(parseAmount(matcher.group(1)));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    static String itemName(SlotView slot) {
        if (slot == null) {
            return "";
        }
        String named = displayName(slot.name());
        if (!isBareEnchantedBook(named)) {
            return named;
        }
        for (String line : slot.lore()) {
            String text = strip(line);
            if (text.isBlank()
                    || loreMeansSelected(List.of(text))
                    || PROGRESS.matcher(text).find()) {
                continue;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.startsWith("click")
                    || lower.contains("odds")
                    || lower.contains("progress")
                    || lower.contains("stored xp")) {
                continue;
            }
            Matcher book = BOOK_NAME.matcher(text);
            if (book.matches()) {
                String inner = book.group("name");
                return inner == null ? named : strip(inner);
            }
            if (ENCHANT_LINE.matcher(text).matches()) {
                return text;
            }
        }
        return named;
    }

    static boolean isExplicitlyEmptySelection(String raw) {
        String text = strip(raw).toLowerCase(Locale.ROOT);
        return text.isBlank()
                || text.equals("none")
                || text.equals("n/a")
                || text.equals("not selected")
                || text.equals("no drop selected")
                || text.equals("no item selected")
                || text.equals("no rng meter selected")
                || text.equals("no rng meter drop selected")
                || text.equals("click to select")
                || text.equals("click to select!");
    }

    private static Optional<Selection> selectionFromMeterItem(
            SlotView slot,
            SlayerPolicy.SlayerType family) {
        Optional<String> selected = selectedDropFromLore(slot.lore());
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        String item = selected.get();
        SlayerPolicy.SlayerType resolvedFamily = family != null
                ? family
                : familyFromMeterItem(slot).orElse(null);
        if (isExplicitlyEmptySelection(item)) {
            return Optional.of(new Selection("", storedXpFromLore(slot.lore()).orElse(-1L), resolvedFamily));
        }
        return Optional.of(new Selection(
                displayName(item),
                storedXpFromLore(slot.lore()).orElse(-1L),
                resolvedFamily));
    }

    private static Optional<SlayerPolicy.SlayerType> familyFromMeterItem(SlotView slot) {
        if (slot == null || slot.lore().isEmpty()) {
            return Optional.empty();
        }
        return familyFromTitle(slot.lore().get(0));
    }

    private static boolean looksLikeRngMeterItem(SlotView slot) {
        if (slot == null) {
            return false;
        }
        String name = strip(slot.name()).toLowerCase(Locale.ROOT);
        if (name.equals("rng meter") || name.endsWith(" rng meter")) {
            return true;
        }
        for (String line : slot.lore()) {
            String text = strip(line).toLowerCase(Locale.ROOT);
            if (text.equals("selected drop") || text.startsWith("selected drop")) {
                return true;
            }
        }
        return slot.index() == 35;
    }

    private static Optional<String> selectedDropFromLore(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (int i = 0; i < lore.size(); i++) {
            String text = strip(lore.get(i));
            if (text.equalsIgnoreCase("Selected Drop")) {
                String next = nextNonBlank(lore, i + 1);
                return next.isBlank() ? Optional.empty() : Optional.of(next);
            }
            Matcher matcher = SELECTED_DROP.matcher(text);
            if (matcher.find()) {
                String item = matcher.group("item");
                return item == null || item.isBlank() ? Optional.empty() : Optional.of(item.trim());
            }
        }
        return Optional.empty();
    }

    private static String nextNonBlank(List<String> lore, int start) {
        for (int i = start; i < lore.size(); i++) {
            String text = strip(lore.get(i));
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private static boolean isBareEnchantedBook(String name) {
        String text = strip(name);
        return text.equalsIgnoreCase("Enchanted Book") || text.equalsIgnoreCase("Enchanted Book:");
    }

    private static boolean isNonSlayerMeter(String raw) {
        String lower = strip(raw).toLowerCase(Locale.ROOT);
        for (String prefix : NON_SLAYER_METERS) {
            if (lower.equals(prefix) || lower.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    private static long parseAmount(String raw) {
        String value = raw == null ? "" : raw.trim().replace(",", "");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty amount");
        }
        char suffix = Character.toUpperCase(value.charAt(value.length() - 1));
        long multiplier = 1L;
        if (suffix == 'K' || suffix == 'M') {
            multiplier = suffix == 'K' ? 1_000L : 1_000_000L;
            value = value.substring(0, value.length() - 1);
        }
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed) || parsed < 0.0D
                || parsed > Long.MAX_VALUE / (double) multiplier) {
            throw new IllegalArgumentException("invalid amount");
        }
        return Math.round(parsed * multiplier);
    }

    private static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
