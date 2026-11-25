package fi.rotclient.rebuild.illegal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
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
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9]{8}");
    private static final Pattern CLAIM_ID = Pattern.compile(
            "(?<![A-Za-z0-9./@_-])(?:https://)?rewards\\.hypixel\\.net/claim-reward/([A-Za-z0-9]{8})(?![A-Za-z0-9_/?#=&%+-])",
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

    static Optional<String> findClaimId(URI url) {
        if (url == null || !"https".equalsIgnoreCase(url.getScheme()) || !CLAIM_HOST.equalsIgnoreCase(url.getHost())
                || url.getPort() != -1 || url.getUserInfo() != null || url.getQuery() != null || url.getFragment() != null) {
            return Optional.empty();
        }
        String path = url.getRawPath();
        if (path == null || !path.startsWith(CLAIM_PATH)) return Optional.empty();
        String id = path.substring(CLAIM_PATH.length());
        return ID.matcher(id).matches() ? Optional.of(id) : Optional.empty();
    }

    static URI fetchUrl(String claimId) {
        if (!ID.matcher(claimId).matches()) throw new IllegalArgumentException("Invalid reward link.");
        return URI.create("https://" + CLAIM_HOST + CLAIM_PATH + claimId);
    }

    static URI claimUrl(ParsedPage page, int optionIndex) {
        if (!page.skippable() || !ID.matcher(page.id()).matches()
                || page.rewards().stream().noneMatch(reward -> reward.index() == optionIndex)) {
            throw new IllegalArgumentException("This choice cannot be claimed in-game.");
        }
        return URI.create("https://" + CLAIM_HOST + CLAIM_PATH + "claim"
                + "?option=" + optionIndex + "&id=" + page.id()
                + "&activeAd=" + Math.max(0, page.activeAd())
                + "&_csrf=" + URLEncoder.encode(page.csrfToken(), StandardCharsets.UTF_8)
                + "&watchedFallback=false");
    }

    static boolean successfulClaim(String body) {
        String text = body.strip();
        if (text.isEmpty() || text.equalsIgnoreCase("ok") || text.equalsIgnoreCase("success")) return true;
        try {
            JsonElement response = JsonParser.parseString(text);
            if (response.isJsonPrimitive() && response.getAsJsonPrimitive().isBoolean()) return response.getAsBoolean();
            if (!response.isJsonObject()) return false;
            JsonObject result = response.getAsJsonObject();
            if (result.has("success")) return bool(result, "success", false);
            if (result.has("error") && !result.get("error").isJsonNull()
                    && !text(result, "error").isBlank() && !"false".equals(text(result, "error"))) return false;
            return "success".equalsIgnoreCase(text(result, "status")) || "ok".equalsIgnoreCase(text(result, "status"));
        } catch (RuntimeException malformed) {
            return false;
        }
    }

    public static ParsedPage parsePage(String html, String expectedId) {
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
        String id = text(root, "id");
        if (!ID.matcher(id).matches() || !id.equals(expectedId)) throw new IllegalStateException("Reward link mismatch.");
        return new ParsedPage(
                id,
                token,
                bool(root, "skippable", false),
                integer(root, "activeAd", 0),
                streakCounter(streak, "score"),
                streakCounter(streak, "highScore"),
                List.copyOf(rewards));
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

    static String streakSummary(ParsedPage page) {
        return "Streak " + displayCounter(page.streakScore())
                + "  ·  Best " + displayCounter(page.streakBest());
    }

    private static String displayCounter(int value) {
        return value < 0 ? "—" : Integer.toString(value);
    }

    private static int streakCounter(JsonObject streak, String key) {
        try {
            return Math.max(-1, Integer.parseInt(text(streak, key)));
        } catch (NumberFormatException ignored) {
            return -1;
        }
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
        if ("housing_package".equalsIgnoreCase(prettyType) && pack != null && !pack.isBlank()) {
            String skull = pack.replace("specialoccasion_reward_card_skull_", "");
            String named = i18n.getOrDefault("housing.skull." + skull, humanize(skull));
            return amount > 1 ? amount + "× " + named : named;
        }
        if ("add_vanity".equalsIgnoreCase(prettyType) && key != null && !key.isBlank()) {
            String vanity = i18n.getOrDefault("vanity." + key, humanize(key));
            return vanity;
        }
        String keyType = prettyType.toLowerCase(Locale.ROOT);
        String fallback = switch (keyType) {
            case "dust" -> "Mystery Dust";
            case "experience" -> "Hypixel XP";
            case "adsense_token" -> amount == 1 ? "Reward Token" : "Reward Tokens";
            case "housing_package" -> "Housing Block";
            case "add_vanity" -> "Collectible";
            case "coins", "tokens", "souls" -> (game == null || game.isBlank() ? "" : gameName(game) + " ") + humanize(prettyType);
            default -> humanize(prettyType);
        };
        String template = i18n.getOrDefault("type." + keyType, fallback);
        String quantity = amount > 0 ? NumberFormat.getIntegerInstance(Locale.US).format(amount) : "";
        String named = template.replace("{$game}", gameName(game)).replace("{game}", gameName(game))
                .replace("{$amount}", quantity).replace("{amount}", quantity).strip();
        if (amount > 0 && !template.contains("{amount}") && !template.contains("{$amount}")) {
            return quantity + " " + named;
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
        return description.replace("{$game}", gameName(game)).replace("{game}", gameName(game)).strip();
    }

    private static String gameName(String game) {
        return switch (game == null ? "" : game.toUpperCase(Locale.ROOT)) {
            case "" -> "";
            case "SKYBLOCK" -> "SkyBlock";
            case "SKYWARS" -> "SkyWars";
            case "BEDWARS" -> "Bed Wars";
            case "LEGACY" -> "Classic Games";
            case "MCGO" -> "Cops and Crims";
            case "TNTGAMES" -> "TNT Games";
            default -> humanize(game);
        };
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
            int streakScore,
            int streakBest,
            List<RewardOption> rewards) {
    }
}
