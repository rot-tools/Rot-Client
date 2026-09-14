package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Escrow Fix: reopen AH or Bazaar after Hypixel closes the
 * GUI because of an escrow/claim chat line. Titles and chat strings are the
 * same Hypixel SkyBlock messages the Serveri uses.
 */
public final class EscrowFixPolicy {
    private static final Pattern CONTROL = Pattern.compile("§.");
    private static final Pattern ESCROW_BZ = Pattern.compile(
            "Escrow refunded (\\d+) coins for Bazaar Instant Buy Submit!");
    private static final Map<String, String> AH_MESSAGES = ahMessages();

    private EscrowFixPolicy() {
    }

    /**
     * @return {@code ah} or {@code bz} command without a slash, or {@code null}
     */
    public static String commandForMessage(String raw) {
        String text = strip(raw);
        if (text.isEmpty()) {
            return null;
        }
        String mapped = AH_MESSAGES.get(text);
        if (mapped != null) {
            return mapped;
        }
        if (ESCROW_BZ.matcher(text).find()) {
            return "bz";
        }
        return null;
    }

    public static String strip(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return CONTROL.matcher(raw).replaceAll("").trim();
    }

    static boolean isAuctionHouseReopen(String command) {
        return "ah".equals(command);
    }

    static boolean isBazaarReopen(String command) {
        return "bz".equals(command);
    }

    private static Map<String, String> ahMessages() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("There was an error with the auction house! (AUCTION_EXPIRED_OR_NOT_FOUND)", "ah");
        out.put("There was an error with the auction house! (INVALID_BID)", "ah");
        out.put("Claiming BIN auction...", "ah");
        out.put("Visit the Auction House to collect your item!", "ah");
        return Map.copyOf(out);
    }

    static String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        return command.trim().toLowerCase(Locale.ROOT);
    }
}
