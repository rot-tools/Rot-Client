package fi.rotclient;

import java.util.Locale;

/**
 * Pure Hypixel address detection helper for optional server-context checks.
 * Auto Sprint does not use this as a disable gate.
 */
public final class HypixelServerPolicy {
    private HypixelServerPolicy() {
    }

    /**
     * @param serverAddress IP / hostname from the multiplayer server entry,
     *                      or blank/null when not on a remote server
     */
    public static boolean isHypixelAddress(String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) {
            return false;
        }
        String host = serverAddress.trim().toLowerCase(Locale.ROOT);
        int colon = host.indexOf(':');
        if (colon > 0) {
            host = host.substring(0, colon);
        }
        return host.equals("hypixel")
                || host.equals("hypixel.net")
                || host.endsWith(".hypixel.net")
                || host.contains("hypixel.net");
    }
}
