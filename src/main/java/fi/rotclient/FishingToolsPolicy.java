package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bait remaining, thunder-bottle charge, and Totem of Corruption nametags.
 */
public final class FishingToolsPolicy {
    private static final Pattern BAIT_REMAINING = Pattern.compile(
            "Bait Remaining:\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTEM_TIME = Pattern.compile(
            "Remaining:\\s*(?:(\\d+)m\\s*)?(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTEM_OWNER = Pattern.compile(
            "Owner:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private FishingToolsPolicy() {
    }

    public static boolean isBaitName(String itemName) {
        String name = FishingCreaturesPolicy.strip(itemName).toLowerCase(Locale.ROOT);
        return name.endsWith(" bait") || name.contains("chum");
    }

    public static Integer parseBaitRemaining(List<String> loreLines) {
        if (loreLines == null) {
            return null;
        }
        for (String line : loreLines) {
            Matcher matcher = BAIT_REMAINING.matcher(FishingCreaturesPolicy.strip(line));
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isThunderBottleId(String skyBlockId) {
        if (skyBlockId == null || skyBlockId.isBlank()) {
            return false;
        }
        String id = skyBlockId.toUpperCase(Locale.ROOT);
        return id.contains("THUNDER_IN_A_BOTTLE")
                || id.contains("STORM_IN_A_BOTTLE")
                || id.contains("HURRICANE_IN_A_BOTTLE");
    }

    public static boolean isEmptyThunderBottle(String skyBlockId) {
        return isThunderBottleId(skyBlockId)
                && skyBlockId.toUpperCase(Locale.ROOT).contains("EMPTY");
    }

    public static boolean isBottleChargedChat(String stripped) {
        String text = FishingCreaturesPolicy.strip(stripped).toLowerCase(Locale.ROOT);
        return text.contains("bottle of thunder has fully charged")
                || text.contains("bottle of storm has fully charged")
                || text.contains("bottle of hurricane has fully charged");
    }

    public static boolean isTotemNametag(String nametag) {
        return FishingCreaturesPolicy.strip(nametag).toLowerCase(Locale.ROOT)
                .contains("totem of corruption");
    }

    public static Integer parseTotemSeconds(String nametag) {
        Matcher matcher = TOTEM_TIME.matcher(FishingCreaturesPolicy.strip(nametag));
        if (!matcher.find()) {
            return null;
        }
        int minutes = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        return minutes * 60 + seconds;
    }

    public static String parseTotemOwner(String nametag) {
        Matcher matcher = TOTEM_OWNER.matcher(FishingCreaturesPolicy.strip(nametag));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    public static boolean baitChanged(String previousName, String currentName) {
        if (previousName == null || previousName.isBlank() || currentName == null || currentName.isBlank()) {
            return false;
        }
        return !stripName(previousName).equals(stripName(currentName));
    }

    public static boolean shouldMuteBanshee(boolean enabled, String soundId, float pitch) {
        if (!enabled || soundId == null) {
            return false;
        }
        String id = soundId.toLowerCase(Locale.ROOT);
        return id.contains("ghast.ambient") && pitch >= 0.18F && pitch <= 0.50F;
    }

    public static boolean shouldMuteDrake(boolean enabled, String soundId) {
        if (!enabled || soundId == null) {
            return false;
        }
        String id = soundId.toLowerCase(Locale.ROOT);
        return id.contains("item.totem.use");
    }

    public static boolean isSpongeId(String blockId) {
        if (blockId == null) {
            return false;
        }
        String id = blockId.toLowerCase(Locale.ROOT);
        return id.endsWith(":sponge") || id.endsWith(":wet_sponge") || id.contains("sponge");
    }

    private static String stripName(String name) {
        return FishingCreaturesPolicy.strip(name).toLowerCase(Locale.ROOT);
    }

    public static String baitHud(boolean hudOn, Integer remaining, boolean noBaitWarn) {
        if (!hudOn) {
            return "";
        }
        if (remaining == null) {
            return noBaitWarn ? "Bait none" : "";
        }
        return "Bait " + remaining;
    }
}
