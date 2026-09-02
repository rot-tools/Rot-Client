package fi.rotclient;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Highlights the currently spawned pet in the Pets menu.
 */
public final class ActivePetHighlightPolicy {
    private static final Pattern TITLE = Pattern.compile("(?i).*/.*\\s*Pets");

    private ActivePetHighlightPolicy() {
    }

    public static boolean isPetsMenu(String strippedTitle) {
        if (strippedTitle == null) {
            return false;
        }
        String text = AutoConversationPolicy.stripFormatting(strippedTitle).trim();
        return TITLE.matcher(text).matches() || text.equalsIgnoreCase("Pets");
    }

    public static boolean isActivePet(String skyblockId, List<String> lore) {
        if (skyblockId == null || !skyblockId.equalsIgnoreCase("PET")) {
            return false;
        }
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String plain = AutoConversationPolicy.stripFormatting(line).trim();
            if (plain.equalsIgnoreCase("Click to despawn!")) {
                return true;
            }
        }
        return false;
    }
}
