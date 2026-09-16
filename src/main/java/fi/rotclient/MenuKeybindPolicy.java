package fi.rotclient;

import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SkyBlock chest-GUI slot clicks. Titles and slot indexes
 * follow Hypixel wardrobe / pets / loadout menus, which the local
 * Serveri uses as well.
 */
public final class MenuKeybindPolicy {
    public static final int WARDROBE_NEXT_SLOT = 53;
    public static final int WARDROBE_PREVIOUS_SLOT = 45;
    public static final int WARDROBE_SLOT_BASE = 36;
    public static final int PETS_NEXT_SLOT = 53;
    public static final int PETS_PREVIOUS_SLOT = 45;
    public static final int PETS_CLOSE_SLOT = 49;
    public static final int LOADOUT_NEXT_SLOT = 44;
    public static final int LOADOUT_PREVIOUS_SLOT = 17;

    public static final int[] PET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    public static final int[] LOADOUT_SLOTS = {
            14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    private static final Pattern WARDROBE_TITLE =
            Pattern.compile("\\((\\d)/(\\d)\\) (Armor|Equipment) Sets");
    private static final Pattern PETS_TITLE =
            Pattern.compile("(?:\\((\\d)/(\\d)\\)\\s*)?Pets");
    private static final Pattern LOADOUT_TITLE =
            Pattern.compile("\\((\\d)/(\\d)\\) Loadout");
    private static final Pattern EQUIPPED_SLOT_NAME =
            Pattern.compile("Slot (\\d): Equipped");

    public record PageTitle(int current, int total) {
    }

    private MenuKeybindPolicy() {
    }

    /**
     * Hypixel chest titles often include {@code §} codes and nbsp; strip them
     * the same way dungeon scoreboard detection does before matching.
     */
    public static String stripGuiText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§.", "")
                .replace('\u00A0', ' ')
                .trim();
    }

    public static PageTitle parseWardrobeTitle(String title) {
        String text = stripGuiText(title);
        if (text.isEmpty()) {
            return null;
        }
        Matcher matcher = WARDROBE_TITLE.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new PageTitle(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)));
    }

    public static PageTitle parsePetsTitle(String title) {
        String text = stripGuiText(title);
        if (text.isEmpty()) {
            return null;
        }
        Matcher matcher = PETS_TITLE.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int current = matcher.group(1) == null ? 1 : Integer.parseInt(matcher.group(1));
        int total = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));
        return new PageTitle(current, total);
    }

    public static PageTitle parseLoadoutTitle(String title) {
        String text = stripGuiText(title);
        if (text.isEmpty()) {
            return null;
        }
        Matcher matcher = LOADOUT_TITLE.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new PageTitle(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)));
    }

    public static boolean isEquippedWardrobeName(String hoverName) {
        String text = stripGuiText(hoverName);
        return !text.isEmpty() && EQUIPPED_SLOT_NAME.matcher(text).find();
    }

    public static boolean loreMeansPetEquipped(List<String> loreLines) {
        if (loreLines == null) {
            return false;
        }
        for (String line : loreLines) {
            if (stripGuiText(line).contains("Click to despawn!")) {
                return true;
            }
        }
        return false;
    }

    public static OptionalInt resolveWardrobeSlot(
            String title,
            String keyName,
            String nextKey,
            String previousKey,
            String unequipKey,
            int equippedSlotIndex,
            boolean disableUnequip,
            boolean targetSlotEmpty) {
        PageTitle page = parseWardrobeTitle(title);
        if (page == null) {
            return OptionalInt.empty();
        }
        if (matchesKey(keyName, nextKey)) {
            return page.current() >= page.total()
                    ? OptionalInt.empty()
                    : OptionalInt.of(WARDROBE_NEXT_SLOT);
        }
        if (matchesKey(keyName, previousKey)) {
            return page.current() <= 1
                    ? OptionalInt.empty()
                    : OptionalInt.of(WARDROBE_PREVIOUS_SLOT);
        }
        if (matchesKey(keyName, unequipKey)) {
            return equippedSlotIndex >= 0
                    ? OptionalInt.of(equippedSlotIndex)
                    : OptionalInt.empty();
        }
        int index = numberRowIndex(keyName);
        if (index < 0 || index > 8) {
            return OptionalInt.empty();
        }
        int slot = WARDROBE_SLOT_BASE + index;
        if (disableUnequip && equippedSlotIndex == slot) {
            return OptionalInt.empty();
        }
        if (disableUnequip && targetSlotEmpty) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(slot);
    }

    public static OptionalInt resolvePetsSlot(
            String title,
            String keyName,
            String nextKey,
            String previousKey,
            String unequipKey,
            int equippedSlotIndex,
            boolean targetAlreadyEquipped,
            boolean disableUnequip,
            boolean closeIfAlreadyEquipped) {
        PageTitle page = parsePetsTitle(title);
        if (page == null) {
            return OptionalInt.empty();
        }
        if (matchesKey(keyName, nextKey)) {
            return page.current() >= page.total()
                    ? OptionalInt.empty()
                    : OptionalInt.of(PETS_NEXT_SLOT);
        }
        if (matchesKey(keyName, previousKey)) {
            return page.current() <= 1
                    ? OptionalInt.empty()
                    : OptionalInt.of(PETS_PREVIOUS_SLOT);
        }
        if (matchesKey(keyName, unequipKey)) {
            return equippedSlotIndex >= 0
                    ? OptionalInt.of(equippedSlotIndex)
                    : OptionalInt.empty();
        }
        int index = numberRowIndex(keyName);
        if (index < 0 || index >= PET_SLOTS.length) {
            return OptionalInt.empty();
        }
        int slot = PET_SLOTS[index];
        if (targetAlreadyEquipped) {
            if (closeIfAlreadyEquipped) {
                return OptionalInt.of(PETS_CLOSE_SLOT);
            }
            if (disableUnequip) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(slot);
    }

    public static OptionalInt resolveLoadoutSlot(
            String title,
            String keyName,
            String nextKey,
            String previousKey) {
        PageTitle page = parseLoadoutTitle(title);
        if (page == null) {
            return OptionalInt.empty();
        }
        if (matchesKey(keyName, nextKey)) {
            return page.current() >= page.total()
                    ? OptionalInt.empty()
                    : OptionalInt.of(LOADOUT_NEXT_SLOT);
        }
        if (matchesKey(keyName, previousKey)) {
            return page.current() <= 1
                    ? OptionalInt.empty()
                    : OptionalInt.of(LOADOUT_PREVIOUS_SLOT);
        }
        int index = loadoutIndex(keyName);
        if (index < 0 || index >= LOADOUT_SLOTS.length) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(LOADOUT_SLOTS[index]);
    }

    public static boolean matchesKey(String keyName, String configured) {
        if (keyName == null || keyName.isBlank() || configured == null || configured.isBlank()) {
            return false;
        }
        String actual = QolKeybindNames.canonicalKeyName(keyName, "");
        String expected = QolKeybindNames.canonicalKeyName(configured, "");
        return !expected.isEmpty() && expected.equals(actual);
    }

    static int numberRowIndex(String keyName) {
        String key = QolKeybindNames.canonicalKeyName(keyName, "");
        return switch (key) {
            case "1" -> 0;
            case "2" -> 1;
            case "3" -> 2;
            case "4" -> 3;
            case "5" -> 4;
            case "6" -> 5;
            case "7" -> 6;
            case "8" -> 7;
            case "9" -> 8;
            default -> -1;
        };
    }

    static int loadoutIndex(String keyName) {
        int number = numberRowIndex(keyName);
        if (number >= 0) {
            return number;
        }
        return switch (QolKeybindNames.canonicalKeyName(keyName, "")) {
            case "0" -> 9;
            case "MINUS" -> 10;
            case "EQUAL" -> 11;
            default -> -1;
        };
    }
}
