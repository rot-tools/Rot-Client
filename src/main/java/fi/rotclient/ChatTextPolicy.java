package fi.rotclient;

/**
 * Shared Minecraft/SkyBlock chat color stripping.
 */
public final class ChatTextPolicy {
    private ChatTextPolicy() {
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§.", "").replaceAll("&[0-9a-fk-or]", "");
    }
}
