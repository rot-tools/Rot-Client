package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Price tooltip lines. Live auction/bazaar APIs are optional
 * inputs; NPC/motes/price-paid work from lore and ExtraAttributes alone.
 */
public final class PriceTooltipsPolicy {
    private static final Pattern COINS = Pattern.compile(
            "(?i)(?:sell(?:s)?(?: for)?|npc(?: sell)?(?: price)?)[:\\s]+([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*coins?");
    private static final Pattern MOTES = Pattern.compile(
            "(?i)motes?[:\\s]+([0-9][0-9,]*(?:\\.[0-9]+)?)");
    private static final Pattern COST = Pattern.compile(
            "(?i)^cost:\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*coins?");
    private static final Pattern STORED = Pattern.compile(
            "(?i)(?:stored|owned|compost available):\\s*([0-9][0-9,]*)");

    private PriceTooltipsPolicy() {
    }

    public static boolean shouldFetchRemoteQuotes(boolean priceTooltipsEnabled) {
        return priceTooltipsEnabled;
    }

    public record Quote(
            double lowestBin,
            double bazaarBuy,
            double bazaarSell,
            double npcCoins,
            double motes) {
    }

    public static List<String> lines(
            boolean enabled,
            boolean lowestBin,
            boolean bazaar,
            boolean npc,
            boolean motes,
            boolean pricePaid,
            int burgerCount,
            boolean inRift,
            int quantity,
            Quote quote,
            long paid) {
        List<String> out = new ArrayList<>();
        if (!enabled) {
            return out;
        }
        int qty = Math.max(1, quantity);
        Quote prices = quote == null ? new Quote(0, 0, 0, 0, 0) : quote;
        if (motes && inRift && prices.motes() > 0.0D) {
            double bonus = 1.0D + 0.05D * Math.max(0, burgerCount);
            out.add(formatLine("§dMotes Price", prices.motes() * bonus, qty));
        }
        if (npc && prices.npcCoins() > 0.0D) {
            out.add(formatLine("§eNPC Price", prices.npcCoins(), qty));
        }
        if (lowestBin && prices.lowestBin() > 0.0D) {
            out.add(formatLine("§eLowest BIN", prices.lowestBin(), qty));
        }
        if (bazaar) {
            if (prices.bazaarBuy() > 0.0D) {
                out.add(formatLine("§eBazaar Buy", prices.bazaarBuy(), qty));
            }
            if (prices.bazaarSell() > 0.0D) {
                out.add(formatLine("§eBazaar Sell", prices.bazaarSell(), qty));
            }
        }
        if (pricePaid && paid > 0L) {
            out.add(formatLine("§ePrice Paid", paid, 1));
        }
        return List.copyOf(out);
    }

    public static int stackQuantity(List<String> lore, int stackCount) {
        if (lore != null) {
            for (String raw : lore) {
                String line = ChatTextPolicy.stripFormatting(raw).trim();
                Matcher stored = STORED.matcher(line);
                if (stored.find()) {
                    return Math.max(1, parseInt(stored.group(1)));
                }
            }
        }
        return Math.max(1, stackCount);
    }

    public static double npcFromLore(List<String> lore) {
        return firstCoins(lore, COINS);
    }

    public static double motesFromLore(List<String> lore) {
        return firstCoins(lore, MOTES);
    }

    public static OptionalLong purchaseCostFromLore(List<String> lore) {
        if (lore == null) {
            return OptionalLong.empty();
        }
        for (String raw : lore) {
            String line = ChatTextPolicy.stripFormatting(raw).trim();
            Matcher matcher = COST.matcher(line);
            if (matcher.find()) {
                long value = Math.round(parseDouble(matcher.group(1)));
                if (value > 0L) {
                    return OptionalLong.of(value);
                }
            }
        }
        return OptionalLong.empty();
    }

    public static int clampBurgers(int burgers) {
        return Math.max(0, Math.min(5, burgers));
    }

    public static String formatLine(String name, double unitPrice, int quantity) {
        int qty = Math.max(1, quantity);
        String total = formatSeparator(unitPrice * qty);
        if (qty == 1) {
            return name + ": §6" + total;
        }
        return name + ": §6" + total + " §8(" + qty + "x " + formatSeparator(unitPrice) + ")";
    }

    public static String formatSeparator(double value) {
        if (value >= 1_000_000_000D) {
            return String.format(Locale.US, "%,.0f", value);
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.format(Locale.US, "%,.0f", value);
        }
        return String.format(Locale.US, "%,.1f", value);
    }

    private static double firstCoins(List<String> lore, Pattern pattern) {
        if (lore == null) {
            return 0.0D;
        }
        for (String raw : lore) {
            String line = ChatTextPolicy.stripFormatting(raw).trim();
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return parseDouble(matcher.group(1));
            }
        }
        return 0.0D;
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
