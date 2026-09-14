package fi.rotclient;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Wardrobe keybinds + auto-equip rules. Titles, slot indexes, lime-dye
 * equipped marker, {@code /wd} hidden-menu flow, and ping delay follow the
 * Hypixel Armor Sets menu.
 */
public final class WardrobeKeybindPolicy {
    public static final int MIN_PING_MS = 10;
    public static final int MAX_PING_MS = 1000;
    public static final int DEFAULT_PING_MS = 250;
    public static final int MIN_DELAY_TICKS = 0;
    public static final int MAX_DELAY_TICKS = 8;
    public static final int DEFAULT_CLICK_DELAY = 1;
    public static final int DEFAULT_CLOSE_DELAY = 1;
    public static final int MAX_VARIANCE = 5;
    public static final long AUTO_EQUIP_TIMEOUT_MS = 2000L;
    public static final String OPEN_COMMAND = "wd";

    private WardrobeKeybindPolicy() {
    }

    public static int clampPingMs(int ms) {
        return Math.max(MIN_PING_MS, Math.min(MAX_PING_MS, ms));
    }

    public static int clampDelayTicks(int ticks) {
        return Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, ticks));
    }

    public static int clampVariance(int ticks) {
        return Math.max(0, Math.min(MAX_VARIANCE, ticks));
    }

    public static int clampSlotIndex(int slot1to9) {
        return Math.max(1, Math.min(9, slot1to9));
    }

    public static int wardrobeSlotIndex(int slot1to9) {
        return MenuKeybindPolicy.WARDROBE_SLOT_BASE + clampSlotIndex(slot1to9) - 1;
    }

    public static boolean isWardrobeTitle(String stripped) {
        return MenuKeybindPolicy.parseWardrobeTitle(stripped) != null
                || containsArmorSets(stripped);
    }

    public static boolean containsArmorSets(String stripped) {
        if (stripped == null) {
            return false;
        }
        String text = MenuKeybindPolicy.stripGuiText(stripped);
        return text.contains("Armor Sets") || text.contains("Equipment Sets");
    }

    public static boolean isEquipped(
            boolean limeDye,
            String hoverName) {
        return limeDye || MenuKeybindPolicy.isEquippedWardrobeName(hoverName);
    }

    public static boolean isEmptyMarker(boolean grayDye, String hoverName) {
        if (!grayDye) {
            return false;
        }
        String text = MenuKeybindPolicy.stripGuiText(hoverName).toLowerCase(Locale.ROOT);
        return text.contains("empty");
    }

    public static boolean isSlotReady(
            boolean empty,
            boolean loadingPane,
            boolean emptyMarker) {
        return !empty && !loadingPane && !emptyMarker;
    }

    public static boolean matchesInput(int code, String configured) {
        Integer mouse = QolKeybindNames.resolveMouseButton(configured);
        if (mouse != null) {
            return mouse == code;
        }
        return MenuKeybindPolicy.matchesKey(code, configured);
    }

    public static boolean isPageOrUnequipAction(
            int code,
            String nextKey,
            String previousKey,
            String unequipKey) {
        return matchesInput(code, nextKey)
                || matchesInput(code, previousKey)
                || matchesInput(code, unequipKey);
    }

    public static boolean shouldClickAutoEquip(boolean ready, boolean equipped) {
        return ready && !equipped;
    }

    public static boolean pingReady(long nowMs, long lastClickMs, int pingMs) {
        return nowMs - lastClickMs >= clampPingMs(pingMs);
    }

    public static boolean shouldCancelOtherInput(
            boolean inWardrobe,
            boolean cancelAll,
            boolean overrideHeld,
            boolean inventoryOrEscape,
            boolean wardrobeAction) {
        if (!inWardrobe || !cancelAll || overrideHeld || inventoryOrEscape) {
            return false;
        }
        return !wardrobeAction;
    }

    public static int swapClickSlot(boolean firstEquipped, int firstSlot, int secondSlot) {
        return firstEquipped ? secondSlot : firstSlot;
    }

    public static int delayWithVariance(int delay, int variance, double random01) {
        int base = clampDelayTicks(delay);
        int spread = clampVariance(variance);
        if (spread <= 0) {
            return base;
        }
        double roll = Math.max(0.0D, Math.min(1.0D, random01));
        return base + (int) Math.round(spread * roll);
    }

    public static boolean autoEquipTimedOut(long startedAtMs, long nowMs) {
        return nowMs - startedAtMs > AUTO_EQUIP_TIMEOUT_MS;
    }

    public static String effectiveStyle(String configuredStyle, boolean useHotbar) {
        String style = QolSkyblockExtras.normalizeStyle(configuredStyle);
        if (QolSkyblockExtras.STYLE_CUSTOM.equals(style)) {
            return QolSkyblockExtras.STYLE_CUSTOM;
        }
        if (QolSkyblockExtras.STYLE_HOTBAR.equals(style) || useHotbar) {
            return QolSkyblockExtras.STYLE_HOTBAR;
        }
        return QolSkyblockExtras.STYLE_SIMPLE;
    }

    public static OptionalInt customSlotForKey(int code, String[] customBinds) {
        if (customBinds == null) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < Math.min(9, customBinds.length); i++) {
            if (matchesInput(code, customBinds[i])) {
                return OptionalInt.of(wardrobeSlotIndex(i + 1));
            }
        }
        return OptionalInt.empty();
    }

    public static OptionalInt hotbarSlotForKey(int glfwKey, int[] hotbarGlfwKeys) {
        if (hotbarGlfwKeys == null) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < Math.min(9, hotbarGlfwKeys.length); i++) {
            if (hotbarGlfwKeys[i] == glfwKey && glfwKey != -1) {
                return OptionalInt.of(wardrobeSlotIndex(i + 1));
            }
        }
        return OptionalInt.empty();
    }

    public static OptionalInt resolveConfiguredSlotKey(
            int code,
            String configuredStyle,
            boolean useHotbar,
            String[] customBinds,
            int[] hotbarGlfwKeys) {
        String style = effectiveStyle(configuredStyle, useHotbar);
        if (QolSkyblockExtras.STYLE_CUSTOM.equals(style)) {
            return customSlotForKey(code, customBinds);
        }
        if (QolSkyblockExtras.STYLE_HOTBAR.equals(style)) {
            return hotbarSlotForKey(code, hotbarGlfwKeys);
        }
        int number = MenuKeybindPolicy.numberRowIndex(code);
        if (number < 0 || number >= 9) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(wardrobeSlotIndex(number + 1));
    }

    public static boolean canStartHiddenEquip(
            boolean stationaryOnly,
            boolean movementInputHeld,
            double horizontalSpeedSquared) {
        if (!stationaryOnly) {
            return true;
        }
        return !movementInputHeld
                && Double.isFinite(horizontalSpeedSquared)
                && horizontalSpeedSquared <= 1.0E-4D;
    }

    public static String equipHudText(int slot1to9) {
        return "Equipping §7[§c" + clampSlotIndex(slot1to9) + "§7]";
    }

    public static int slotNumberFromIndex(int containerSlot) {
        return containerSlot - MenuKeybindPolicy.WARDROBE_SLOT_BASE + 1;
    }

    public static boolean isMovementKeyName(String token) {
        if (token == null) {
            return false;
        }
        String key = token.trim().toUpperCase(Locale.ROOT);
        return key.equals("W")
                || key.equals("A")
                || key.equals("S")
                || key.equals("D")
                || key.equals("SPACE")
                || key.equals("LEFT_SHIFT")
                || key.equals("LEFT_CONTROL");
    }
}
