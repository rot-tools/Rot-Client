package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable auto-clicker whitelist helpers for chat commands and config.
 * Matches are case-sensitive, using an exact Set.contains on held identity.
 */
public final class AutoClickerWhitelist {
    private AutoClickerWhitelist() {
    }

    public static boolean add(List<String> whitelist, String identity) {
        String normalized = AutoClickerItemIdentity.normalizeForWhitelist(identity);
        if (normalized.isBlank()) {
            return false;
        }
        if (whitelist == null) {
            return false;
        }
        for (String entry : whitelist) {
            if (entry != null && entry.equals(normalized)) {
                return false;
            }
        }
        whitelist.add(normalized);
        return true;
    }

    public static boolean remove(List<String> whitelist, String identity) {
        String normalized = AutoClickerItemIdentity.normalizeForWhitelist(identity);
        if (normalized.isBlank() || whitelist == null) {
            return false;
        }
        return whitelist.removeIf(entry ->
                entry != null && entry.equals(normalized));
    }

    public static List<String> snapshot(List<String> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) {
            return List.of();
        }
        return List.copyOf(whitelist);
    }

    public static List<String> ensureMutable(List<String> whitelist) {
        if (whitelist == null) {
            return new ArrayList<>();
        }
        return whitelist;
    }
}
