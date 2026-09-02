package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hypixel daily-reward claim parsing. Chat IDs, Hypixel claim-page HTML, and
 * in-client labels stay Minecraft-free so tests can lock the contract.
 */
public final class RewardClaimPolicy {
    public static final String CLAIM_HOST = "rewards.hypixel.net";
    public static final String CLAIM_PATH = "/claim-reward/";
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private static final Pattern CLAIM_ID = Pattern.compile(
            "(?:https?://)?rewards\\.hypixel\\.net/claim-reward/([A-Za-z0-9]{8})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN = Pattern.compile(
            "window\\.(?:securityToken|csrfToken)\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern APP_DATA_QUOTED = Pattern.compile(
            "window\\.appData\\s*=\\s*'(\\{.*?})'\\s*;", Pattern.DOTALL);
    private static final Pattern APP_DATA_PLAIN = Pattern.compile(
            "window\\.appData\\s*=\\s*(\\{.*?})\\s*;", Pattern.DOTALL);
    private static final Pattern I18N = Pattern.compile(
            "window\\.i18n\\s*=\\s*(\\{.*?})\\s*;", Pattern.DOTALL);

    private RewardClaimPolicy() {
    }

    public static Optional<String> findClaimId(String chat) {
        if (chat == null || chat.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CLAIM_ID.matcher(stripFormatting(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    public static String fetchUrl(String claimId) {
        return "https://" + CLAIM_HOST + CLAIM_PATH + claimId;
    }

    public static String claimUrl(ParsedPage page, int optionIndex) {
        if (page == null || page.id == null || page.id.isBlank()) {
            throw new IllegalArgumentException("missing claim id");
        }
        int option = Math.max(0, optionIndex);
        return "https://" + CLAIM_HOST + CLAIM_PATH + "claim"
                + "?option=" + option
                + "&id=" + page.id
                + "&activeAd=" + Math.max(0, page.activeAd)
                + "&_csrf=" + (page.csrfToken == null ? "" : page.csrfToken)
                + "&watchedFallback=false";
    }

    public static ParsedPage parsePage(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("empty reward page");
        }
        String token = firstGroup(TOKEN, html);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("security token missing");
        }
        String dataJson = firstGroup(APP_DATA_QUOTED, html);
        if (dataJson == null) {
            dataJson = firstGroup(APP_DATA_PLAIN, html);
        }
        if (dataJson == null) {
            throw new IllegalStateException("app data missing");
        }
        dataJson = dataJson.replace("\\'", "'");
        JsonObject root = JsonParser.parseString(dataJson).getAsJsonObject();
        Map<String, String> i18n = parseI18n(firstGroup(I18N, html));
        List<RewardOption> rewards = parseRewards(root.get("rewards"), i18n);
        if (rewards.isEmpty()) {
            throw new IllegalStateException("no rewards listed");
        }
        JsonObject streak = object(root, "dailyStreak");
        JsonObject ad = object(root, "ad");
        return new ParsedPage(
                text(root, "id"),
                token,
                bool(root, "skippable", false),
                integer(root, "activeAd", 0),
                integer(streak, "value", 0),
                integer(streak, "score", integer(streak, "value", 0)),
                integer(streak, "highScore", 0),
                integer(ad, "duration", 0),
                text(ad, "link"),
                List.copyOf(rewards),
                Map.copyOf(i18n));
    }

    public static int adWaitSeconds(ParsedPage page, boolean waitForAd) {
        if (page == null || !waitForAd || page.skippable) {
            return 0;
        }
        return Math.max(0, Math.min(60, page.adDurationSeconds));
    }

    public static int rarityColor(String rarity) {
        String key = rarity == null ? "" : rarity.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "RARE" -> 0xFF3B82F6;
            case "EPIC" -> 0xFFA855F7;
            case "LEGENDARY" -> 0xFFF59E0B;
            default -> 0xFF94A3B8;
        };
    }

    static String stripFormatting(String raw) {
        return raw.replaceAll("§.", "").replace('\n', ' ').trim();
    }

    private static List<RewardOption> parseRewards(JsonElement element, Map<String, String> i18n) {
        List<RewardOption> rewards = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return rewards;
        }
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) {
                continue;
            }
            JsonObject row = array.get(i).getAsJsonObject();
            String type = text(row, "reward");
            String rarity = text(row, "rarity");
            int amount = integer(row, "amount", 0);
            String game = text(row, "gameType");
            String pack = text(row, "package");
            String key = text(row, "key");
            rewards.add(new RewardOption(
                    i,
                    type,
                    rarity,
                    amount,
                    game,
                    pack,
                    key,
                    titleFor(type, rarity, amount, game, pack, key, i18n),
                    subtitleFor(type, game, i18n)));
        }
        return rewards;
    }

    public static String titleFor(
            String type,
            String rarity,
            int amount,
            String game,
            String pack,
            String key,
            Map<String, String> i18n) {
        String prettyType = type == null ? "Reward" : type;
        if (("housing_package".equalsIgnoreCase(prettyType)
                        || "housing_package".equalsIgnoreCase(prettyType))
                && pack != null) {
            String skull = pack.replace("specialoccasion_reward_card_skull_", "");
            String named = i18n.getOrDefault("housing.skull." + skull, humanize(skull));
            return amount > 1 ? amount + "× " + named : named;
        }
        if (("add_vanity".equalsIgnoreCase(prettyType)
                        || "add_vanity".equalsIgnoreCase(prettyType))
                && key != null) {
            String vanity = i18n.getOrDefault("vanity." + key, humanize(key));
            return vanity;
        }
        String template = i18n.getOrDefault(
                "type." + prettyType.toLowerCase(Locale.ROOT),
                humanize(prettyType));
        String gameName = game == null || game.isBlank() ? "SkyBlock" : humanize(game);
        String named = template.replace("{$game}", gameName).replace("{game}", gameName);
        if (amount > 1 && !named.contains(Integer.toString(amount))) {
            return amount + "× " + named;
        }
        return named;
    }

    static String subtitleFor(String type, String game, Map<String, String> i18n) {
        if (type == null) {
            return "";
        }
        String description = i18n.getOrDefault(
                "type." + type.toLowerCase(Locale.ROOT) + ".description",
                "");
        String gameName = game == null || game.isBlank() ? "SkyBlock" : humanize(game);
        return description.replace("{$game}", gameName).replace("{game}", gameName);
    }

    private static Map<String, String> parseI18n(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return map;
        }
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (RuntimeException ignored) {
            return map;
        }
        return map;
    }

    private static String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Reward";
        }
        String[] parts = raw.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    private static String firstGroup(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static JsonObject object(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return root.getAsJsonObject(key);
    }

    private static String text(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public record RewardOption(
            int index,
            String type,
            String rarity,
            int amount,
            String gameType,
            String pack,
            String key,
            String title,
            String subtitle) {
    }

    public record ParsedPage(
            String id,
            String csrfToken,
            boolean skippable,
            int activeAd,
            int streakValue,
            int streakScore,
            int streakBest,
            int adDurationSeconds,
            String adLink,
            List<RewardOption> rewards,
            Map<String, String> i18n) {
    }
}
