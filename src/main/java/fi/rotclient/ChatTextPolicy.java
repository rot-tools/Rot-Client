package fi.rotclient;

/**
 * Shared Minecraft/SkyBlock chat color stripping.
 */
public final class ChatTextPolicy {
    private static final java.util.regex.Pattern SECTION_CODE = java.util.regex.Pattern.compile("§.");
    private static final java.util.regex.Pattern AMPERSAND_CODE =
            java.util.regex.Pattern.compile("&[0-9a-fk-or]");

    private ChatTextPolicy() {
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.indexOf('§') < 0 && raw.indexOf('&') < 0) {
            return raw;
        }
        return AMPERSAND_CODE.matcher(SECTION_CODE.matcher(raw).replaceAll("")).replaceAll("");
    }

    /**
     * True when the phrase is in a server line, not inside a player's
     * "Name: message" chat text. Anyone can type a phrase into chat.
     */
    public static boolean serverLineContains(String plain, String phrase) {
        int at = plain.indexOf(phrase);
        return at >= 0 && plain.lastIndexOf(':', at) < 0;
    }
}
