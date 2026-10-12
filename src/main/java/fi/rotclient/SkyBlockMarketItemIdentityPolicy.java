package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Locale;

/** Price-provider identity only; raw item IDs and instance UUIDs keep their gameplay meaning. */
public final class SkyBlockMarketItemIdentityPolicy {
    private SkyBlockMarketItemIdentityPolicy() {
    }

    public static String quoteId(String rawId, String petInfo, String displayName, String menuTitle) {
        String id = rawId == null ? "" : rawId.trim().toUpperCase(Locale.ROOT);
        if ("PET".equals(id)) return petQuoteId(petInfo);
        if (!"COMMON_HOOK".equals(id) || !isBaitSackTitle(menuTitle)) return id;
        String name = clean(displayName);
        // Bait Sack entries can carry the hook's NBT ID. Only this verified presentation is resolved.
        if ("fish bait".equals(name)) return "FISH_BAIT";
        return "common hook".equals(name) ? id : "";
    }

    public static boolean isBaitSackTitle(String title) {
        return switch (clean(title)) {
            case "bait sack", "small bait sack", "medium bait sack", "large bait sack" -> true;
            default -> false;
        };
    }

    public static String petQuoteId(String petInfo) {
        if (petInfo == null || petInfo.isBlank() || petInfo.length() > 65_536) return "";
        try {
            JsonElement parsed = JsonParser.parseString(petInfo);
            if (!parsed.isJsonObject()) return "";
            JsonObject pet = parsed.getAsJsonObject();
            String type = text(pet, "type").trim().toUpperCase(Locale.ROOT);
            if (!type.matches("[A-Z][A-Z0-9_]{0,63}")) return "";
            // NEU price keys use rarity ordinals, which differ from the legacy SPECIAL item-rarity ID.
            int rarity = switch (text(pet, "tier").trim().toUpperCase(Locale.ROOT)) {
                case "COMMON" -> 0;
                case "UNCOMMON" -> 1;
                case "RARE" -> 2;
                case "EPIC" -> 3;
                case "LEGENDARY" -> 4;
                case "MYTHIC" -> 5;
                case "DIVINE" -> 6;
                case "SPECIAL" -> 7;
                case "VERY_SPECIAL" -> 8;
                default -> -1;
            };
            return rarity < 0 ? "" : type + ";" + rarity;
        } catch (RuntimeException malformed) {
            return "";
        }
    }

    private static String text(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : "";
    }

    private static String clean(String text) {
        return ChatTextPolicy.stripFormatting(text == null ? "" : text).trim().toLowerCase(Locale.ROOT);
    }
}
