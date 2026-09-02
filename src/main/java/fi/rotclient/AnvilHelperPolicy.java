package fi.rotclient;

/**
 * Highlights matching enchanted books in the SkyBlock Anvil chest.
 */
public final class AnvilHelperPolicy {
    private AnvilHelperPolicy() {
    }

    public static boolean isAnvilTitle(String stripped) {
        if (stripped == null) {
            return false;
        }
        String text = AutoConversationPolicy.stripFormatting(stripped).trim();
        return text.equalsIgnoreCase("Anvil");
    }

    public static boolean isAnvilMarker(String hoverName, boolean barrier) {
        return barrier && AutoConversationPolicy.stripFormatting(hoverName).equalsIgnoreCase("Anvil");
    }

    public static boolean highlightBook(String targetEnchantId, String slotId, boolean enchantedBook) {
        if (!enchantedBook || targetEnchantId == null || targetEnchantId.isBlank()) {
            return false;
        }
        if ("ENCHANTMENT_UNKNOWN".equalsIgnoreCase(targetEnchantId)) {
            return false;
        }
        return targetEnchantId.equalsIgnoreCase(slotId == null ? "" : slotId.trim());
    }
}
