package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft-free RNG Meter chat and inventory rules. The live HUD needs both
 * the selected drop and Stored XP; opening {@code /rng} is the reliable source
 * when Hypixel never reprints the selection chat line.
 */
public final class SlayerRngMeterPolicy {
    private static final Pattern CHAT_SELECTION = Pattern.compile(
            "(?i)you set your .*rng meter to drop (?<item>.+?)!");
    private static final Pattern CHAT_STORED_XP = Pattern.compile(
            "(?i)rng meter\\s*-\\s*(?<xp>[\\d,]+)\\s*stored xp");
    private static final Pattern BOOK_NAME = Pattern.compile(
            "(?i)enchanted book\\s*\\((?<name>.+)\\)");
    private static final Pattern METER_TITLE = Pattern.compile(
            "(?i)^(?<name>.+) rng meter$");
    private static final Pattern PROGRESS = Pattern.compile(
            "(?i)(?:progress|stored xp)\\s*:?\\s*([\\d,.]+[kKmM]?)(?:\\s*/\\s*([\\d,.]+[kKmM]?))?");
    private static final Pattern SELECTED_DROP = Pattern.compile(
            "(?i)selected drop\\s*:?\\s*(?<item>.+)");

    public record SlotView(int index, String name, List<String> lore) {
        public SlotView {
            name = name == null ? "" : name;
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    public record Selection(String itemName, long storedXp) {
        public Selection {
            itemName = itemName == null ? "" : itemName.trim();
            storedXp = Math.max(-1L, storedXp);
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
        if (text.isBlank() || text.equalsIgnoreCase("Catacombs RNG Meter")) {
            return false;
        }
        return METER_TITLE.matcher(text).matches();
    }

    public static boolean isSlayerMenu(String title) {
        String text = strip(title);
        return text.equalsIgnoreCase("Slayer") || text.equalsIgnoreCase("Slayer Menu");
    }

    public static boolean loreMeansSelected(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String text = strip(line);
            if (text.equalsIgnoreCase("SELECTED")
                    || text.toUpperCase(Locale.ROOT).endsWith("SELECTED")
                    || text.toLowerCase(Locale.ROOT).contains("click to deselect")
                    || text.toLowerCase(Locale.ROOT).contains("currently selected")) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Selection> fromRngMeterInventory(String title, List<SlotView> slots) {
        if (!isRngMeterInventory(title) || slots == null) {
            return Optional.empty();
        }
        for (SlotView slot : slots) {
            if (slot.name().isBlank() || !loreMeansSelected(slot.lore())) {
                continue;
            }
            long stored = storedXpFromLore(slot.lore()).orElse(-1L);
            return Optional.of(new Selection(displayName(slot.name()), stored));
        }
        return Optional.empty();
    }

    public static Optional<Selection> fromSlayerMenu(String title, List<SlotView> slots) {
        if (!isSlayerMenu(title) || slots == null) {
            return Optional.empty();
        }
        for (SlotView slot : slots) {
            if (slot.index() != 35) {
                continue;
            }
            String selected = selectedDropFromLore(slot.lore()).orElse("");
            if (selected.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new Selection(displayName(selected), storedXpFromLore(slot.lore()).orElse(-1L)));
        }
        return Optional.empty();
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

    private static Optional<String> selectedDropFromLore(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (int i = 0; i < lore.size(); i++) {
            String text = strip(lore.get(i));
            if (text.equalsIgnoreCase("Selected Drop") && i + 1 < lore.size()) {
                String next = strip(lore.get(i + 1));
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
