package fi.rotclient;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kuudra chest valuation using local market snapshots,
 * {@code ChestProfitUtil}, and the essence/book/salvage calculators. Prices are
 * supplied by the caller (Rot Bazaar/BIN quotes). Minecraft-free.
 */
public final class IotaKuudraProfitPolicy {
    public static final String PRICING_SELL_ORDER = "SELL_ORDER";
    public static final String PRICING_INSTA_SELL = "INSTA_SELL";
    public static final String FACTION_MAGE = "ENCHANTED_MYCELIUM";
    public static final String FACTION_BARBARIAN = "ENCHANTED_RED_SAND";
    public static final String NETHER_STAR = "NETHER_STAR";
    public static final String ESSENCE_CRIMSON = "ESSENCE_CRIMSON";
    public static final String KISMET_FEATHER = "KISMET_FEATHER";
    public static final String WHEEL_OF_FATE = "WHEEL_OF_FATE";
    public static final double DEFAULT_PET_BONUS = 20.0D;
    public static final int SALVAGE_BASE_ESSENCE = 108;
    public static final double SALVAGE_STAR_MULTIPLIER = 0.63D;
    public static final int STAR_GLYPH = 10026;
    public static final Map<String, String> KUUDRA_NAME_IDS = Map.of(
            "CRIMSON ESSENCE", ESSENCE_CRIMSON,
            "KUUDRA TEETH", "KUUDRA_TEETH",
            "KISMET FEATHER", KISMET_FEATHER,
            "WHEEL OF FATE", WHEEL_OF_FATE);
    private static final Pattern QTY = Pattern.compile("(?i)\\sx([\\d,]+)$");
    private static final Pattern ARMOR = Pattern.compile(
            "^(AURORA|CRIMSON|TERROR|FERVOR|HOLLOW)_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)$");
    private static final Pattern CONTROL = Pattern.compile("\\p{C}");

    @FunctionalInterface
    public interface Prices {
        long price(String itemId);
    }

    public enum KeyTier {
        FREE(0L, 0),
        BASIC(160_000L, 2),
        HOT(320_000L, 4),
        BURNING(600_000L, 16),
        FIERY(1_200_000L, 40),
        INFERNAL(2_400_000L, 80),
        UNKNOWN(0L, 0);

        public final long baseCoins;
        public final int materialAmount;

        KeyTier(long baseCoins, int materialAmount) {
            this.baseCoins = baseCoins;
            this.materialAmount = materialAmount;
        }
    }

    private IotaKuudraProfitPolicy() {
    }

    public static String resolveItemId(String nbtId, String hoverName) {
        if (nbtId != null && !nbtId.isBlank()) {
            return nbtId.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        }
        String name = stripQty(plain(hoverName)).toUpperCase(Locale.ROOT).trim();
        if (name.endsWith(" SHARD")) {
            String prefix = name.substring(0, name.length() - " SHARD".length()).trim().replace(' ', '_');
            if (!prefix.isBlank()) {
                return "SHARD_" + prefix;
            }
        }
        for (Map.Entry<String, String> entry : KUUDRA_NAME_IDS.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return name.replace(' ', '_');
    }

    public static int resolveQuantity(String hoverName, int stackCount) {
        Matcher matcher = QTY.matcher(stripQtyKeep(plain(hoverName)));
        if (matcher.find()) {
            try {
                return Math.max(1, Integer.parseInt(matcher.group(1).replace(",", "")));
            } catch (NumberFormatException ignored) {
                return Math.max(1, stackCount);
            }
        }
        return Math.max(1, stackCount);
    }

    public static int countStars(String hoverName) {
        String text = hoverName == null ? "" : hoverName;
        int stars = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == STAR_GLYPH) {
                stars++;
            }
        }
        return stars;
    }

    public static int salvageEssence(int stars) {
        int starSum = 0;
        for (int k = 1; k <= Math.max(0, stars); k++) {
            starSum += 20 + 5 * k;
        }
        return SALVAGE_BASE_ESSENCE + (int) Math.floor(starSum * SALVAGE_STAR_MULTIPLIER);
    }

    public static boolean isKuudraArmor(String itemId) {
        return itemId != null && ARMOR.matcher(itemId.trim().toUpperCase(Locale.ROOT)).matches();
    }

    public static String enchantedBookPriceId(String enchantName, int level) {
        if (enchantName == null || enchantName.isBlank() || level <= 0) {
            return "";
        }
        return "ENCHANTMENT_" + enchantName.trim().toUpperCase(Locale.ROOT) + "_" + level;
    }

    public static long itemValue(
            String itemId,
            String bookEnchantId,
            int quantity,
            int stars,
            boolean salvageArmor,
            double petBonusPercent,
            Prices prices) {
        if (prices == null || itemId == null || itemId.isBlank()) {
            return 0L;
        }
        int qty = Math.max(1, quantity);
        if (ESSENCE_CRIMSON.equals(itemId)) {
            int boosted = (int) Math.round(qty * (1.0D + Math.max(0.0D, petBonusPercent) / 100.0D));
            return prices.price(ESSENCE_CRIMSON) * Math.max(1, boosted);
        }
        if ("ENCHANTED_BOOK".equals(itemId)) {
            if (bookEnchantId == null || bookEnchantId.isBlank()) {
                return 0L;
            }
            return prices.price(bookEnchantId) * qty;
        }
        if (salvageArmor && isKuudraArmor(itemId)) {
            return prices.price(ESSENCE_CRIMSON) * salvageEssence(stars) * qty;
        }
        return prices.price(itemId) * qty;
    }

    public static KeyTier parseKeyTier(String lore) {
        String text = plain(lore).toLowerCase(Locale.ROOT);
        if (!text.contains("kuudra")) {
            return KeyTier.UNKNOWN;
        }
        if (text.contains("infernal")) {
            return KeyTier.INFERNAL;
        }
        if (text.contains("fiery")) {
            return KeyTier.FIERY;
        }
        if (text.contains("burning")) {
            return KeyTier.BURNING;
        }
        if (text.contains("hot")) {
            return KeyTier.HOT;
        }
        if (text.contains("basic")) {
            return KeyTier.BASIC;
        }
        return KeyTier.UNKNOWN;
    }

    public static boolean buySlotLooksFree(String lore) {
        String text = plain(lore).toLowerCase(Locale.ROOT);
        return text.contains("free reward chest") || text.contains("free");
    }

    public static long keyCost(KeyTier tier, boolean paid, boolean mageFaction, Prices prices) {
        if (!paid || prices == null || tier == null || tier == KeyTier.FREE || tier == KeyTier.UNKNOWN) {
            return 0L;
        }
        String material = mageFaction ? FACTION_MAGE : FACTION_BARBARIAN;
        return tier.baseCoins
                + 2L * prices.price(NETHER_STAR)
                + (long) tier.materialAmount * prices.price(material);
    }

    public static long quotePrice(boolean sellOrder, PriceTooltipsPolicy.Quote quote) {
        if (quote == null) {
            return 0L;
        }
        double bazaar = sellOrder ? quote.bazaarSell() : quote.bazaarBuy();
        if (bazaar > 0.0D) {
            return Math.max(0L, Math.round(bazaar));
        }
        if (quote.lowestBin() > 0.0D) {
            return Math.max(0L, Math.round(quote.lowestBin()));
        }
        return 0L;
    }

    public static long preferLive(long liveCoins, long loreCoins) {
        return liveCoins > 0L ? liveCoins : Math.max(0L, loreCoins);
    }

    public static boolean sellOrderMode(String mode) {
        return mode == null
                || mode.isBlank()
                || PRICING_SELL_ORDER.equalsIgnoreCase(mode.trim());
    }

    private static String plain(String text) {
        if (text == null) {
            return "";
        }
        return CONTROL.matcher(text).replaceAll("");
    }

    private static String stripQty(String name) {
        return QTY.matcher(name == null ? "" : name.trim()).replaceFirst("").trim();
    }

    private static String stripQtyKeep(String name) {
        return name == null ? "" : name.trim();
    }
}
