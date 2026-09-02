package fi.rotclient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure carry pricing, trade inference and persistence model. */
public final class SlayerCarryPolicy {
    private static final Pattern TRADE = Pattern.compile(
            "(?i)^Trade completed with (?:\\[.*?] )?(?<player>\\w{1,16})!$");
    private static final Pattern COINS = Pattern.compile(
            "(?i)^\\+ (?<amount>\\d+(?:\\.\\d+)?)M coins$");
    private static final Pattern WEBHOOK_PATH = Pattern.compile(
            "^/api/webhooks/\\d+/[A-Za-z0-9._-]+/?$");

    public static final class HistoryEntry {
        public String player = "";
        public String type = "";
        public int tier;
        public int amount;
        public long durationMillis;
        public long completedAtMillis;
        public double priceMillions;

        public HistoryEntry() {}

        public HistoryEntry(String player, SlayerPolicy.SlayerType type, int tier, int amount,
                            long durationMillis, long completedAtMillis, double priceMillions) {
            this.player = player == null ? "" : player.trim();
            this.type = type == null ? "" : type.name();
            this.tier = Math.max(0, Math.min(5, tier));
            this.amount = Math.max(0, amount);
            this.durationMillis = Math.max(0L, durationMillis);
            this.completedAtMillis = Math.max(0L, completedAtMillis);
            this.priceMillions = Math.max(0.0D, priceMillions);
        }

        public SlayerPolicy.SlayerType slayerType() {
            try { return SlayerPolicy.SlayerType.valueOf(type); }
            catch (RuntimeException ignored) { return null; }
        }
    }

    public record Trade(String player, double amountMillions) {}
    public record PriceMatch(SlayerPolicy.SlayerType type, int tier, int count, double unitMillions) {}

    private SlayerCarryPolicy() {}

    public static Optional<String> tradePlayer(String line) {
        Matcher m = TRADE.matcher(strip(line));
        return m.matches() ? Optional.of(m.group("player")) : Optional.empty();
    }

    public static Optional<Double> receivedMillions(String line) {
        Matcher m = COINS.matcher(strip(line));
        if (!m.matches()) return Optional.empty();
        try {
            double amount = Double.parseDouble(m.group("amount"));
            return Double.isFinite(amount) && amount > 0.0D ? Optional.of(amount) : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static List<Double> prices(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<Double> result = new ArrayList<>();
        for (String token : csv.split(",")) {
            try {
                double value = Double.parseDouble(token.trim());
                if (Double.isFinite(value) && value > 0.0D) result.add(value);
            } catch (NumberFormatException ignored) {}
        }
        return List.copyOf(result);
    }

    public static List<PriceMatch> infer(double receivedMillions,
                                         String voidT3, String voidT4,
                                         String infernoT2, String infernoT3, String infernoT4) {
        if (!Double.isFinite(receivedMillions) || receivedMillions <= 0.0D) return List.of();
        List<PriceMatch> result = new ArrayList<>();
        addMatches(result, receivedMillions, SlayerPolicy.SlayerType.VOIDGLOOM, 3, voidT3);
        addMatches(result, receivedMillions, SlayerPolicy.SlayerType.VOIDGLOOM, 4, voidT4);
        addMatches(result, receivedMillions, SlayerPolicy.SlayerType.INFERNO, 2, infernoT2);
        addMatches(result, receivedMillions, SlayerPolicy.SlayerType.INFERNO, 3, infernoT3);
        addMatches(result, receivedMillions, SlayerPolicy.SlayerType.INFERNO, 4, infernoT4);
        return List.copyOf(result);
    }

    public static String sanitizeWebhookUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) return "";
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !(host.equals("discord.com") || host.equals("discordapp.com"))
                    || !WEBHOOK_PATH.matcher(path).matches()
                    || uri.getUserInfo() != null || uri.getFragment() != null) return "";
            return value.length() <= 512 ? value : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public static double rngChancePercent(long storedXp, long requiredXp,
                                          double baseChancePercent, int magicFind) {
        if (requiredXp <= 0L || storedXp >= requiredXp) return 100.0D;
        double meterMultiplier = 1.0D + Math.min(2.0D * Math.max(0L, storedXp) / requiredXp, 2.0D);
        double meterChance = Math.max(0.0D, baseChancePercent) * meterMultiplier;
        double result = meterChance < 5.0D
                ? meterChance * (1.0D + Math.max(0, magicFind) / 100.0D)
                : meterChance;
        return Math.min(100.0D, result);
    }

    private static void addMatches(List<PriceMatch> result, double total,
                                   SlayerPolicy.SlayerType type, int tier, String csv) {
        for (double unit : prices(csv)) {
            int count = (int) Math.rint(total / unit);
            if (count > 0 && Math.abs(total - count * unit) < 0.01D) {
                result.add(new PriceMatch(type, tier, count, unit));
            }
        }
    }

    private static String strip(String value) {
        return value == null ? "" : value.replaceAll("§.", "").trim();
    }
}
