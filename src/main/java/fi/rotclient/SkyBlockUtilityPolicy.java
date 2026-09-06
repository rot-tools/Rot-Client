package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SkyBlock flavor extras folded onto existing Rot Client parents: detector
 * alerts, Glacite chat, Scatha pet rarity, previous-server / queue chat,
 * calendar minister, item stars/candy, carpets, Hub rats, and the later
 * screenshot slice (master stars, clouds, nether fog, totem, armor, icons,
 * quick join).
 */
public final class SkyBlockUtilityPolicy {
    public static final int DEFAULT_PREVIOUS_SERVER_SECONDS = 360;
    public static final int MIN_PREVIOUS_SERVER_SECONDS = 30;
    public static final int MAX_PREVIOUS_SERVER_SECONDS = 3_600;
    public static final String CLOUD_OFF = "Off";
    public static final String CLOUD_DWARVEN = "Dwarven";
    public static final String CLOUD_MINING = "Mining";
    public static final String CLOUD_ALWAYS = "Always";
    public static final double DEFAULT_NETHER_FOG_SCALE = 0.25D;
    public static final double MIN_NETHER_FOG_SCALE = 0.05D;
    public static final double MAX_NETHER_FOG_SCALE = 1.0D;
    public static final int DEFAULT_ARMOR_PERCENT = 100;
    public static final String DEFAULT_QUICK_JOIN_TEXT = "Join {ip}";
    public static final String DEFAULT_QUICK_JOIN_IP = "hypixel.net";

    private static final Pattern SENDING_TO = Pattern.compile(
            "sending (?:you )?to (?:server )?(?<id>[A-Za-z0-9]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUEUE_POSITION = Pattern.compile(
            "(?:you are currently in )?position\\s+#?(?<n>[\\d,]+)\\s+of the(?: \\w+)? queue",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MINISTER = Pattern.compile(
            "minister\\s*:?\\s+(?<name>[A-Za-z][A-Za-z '\\-]{1,24})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_DROP = Pattern.compile(
            "PET DROP!.*Scatha",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_RARITY = Pattern.compile(
            "\\b(common|uncommon|rare|epic|legendary|mythic)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MASTER_STARS = Pattern.compile(
            "(?<first>.*)✪✪✪✪✪[➊➋➌➍➎](?<second>.*)");
    private static final Pattern[] ABSORB_CHAT = {
            Pattern.compile("Your (?:⚚ )?Bonzo's Mask saved your life!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Your Phoenix Pet saved you from certain death!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Second Wind Activated! Your Spirit Mask saved your life!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Your Remnant of the Eye saved you from certain death!", Pattern.CASE_INSENSITIVE)
    };
    private static final Map<Character, String> MOB_ICON_LABELS = mobIconLabels();

    private SkyBlockUtilityPolicy() {
    }

    public static boolean shouldPingDetector(int previousHits, int nextHits) {
        return nextHits == 1 && previousHits != 1 && previousHits >= 0;
    }

    public static boolean isWoolCarpet(String blockId) {
        String id = strip(blockId).toLowerCase(Locale.ROOT);
        int slash = id.indexOf(':');
        String path = slash >= 0 ? id.substring(slash + 1) : id;
        return path.endsWith("_carpet")
                && !"red_carpet".equals(path)
                && !"moss_carpet".equals(path);
    }

    public static boolean recolorDwarvenCarpet(boolean enabled, boolean inDwarvenMines, String blockId) {
        return enabled && inDwarvenMines && isWoolCarpet(blockId);
    }

    public static String pityChatLine(MiningLeftoverPolicy.Pity pity) {
        if (pity == null) {
            return "";
        }
        return "Mineshaft pity: " + pity.current() + "/" + pity.max();
    }

    public static String shaftEnterLine(String areaName) {
        String name = strip(areaName);
        if (name.isBlank()) {
            name = "Glacite Mineshaft";
        }
        return "Entered " + name;
    }

    public static boolean isMineshaftArea(String areaName) {
        String text = strip(areaName).toLowerCase(Locale.ROOT);
        return text.contains("mineshaft") && text.contains("glacite");
    }

    public static boolean isDwarvenMines(String areaName) {
        return strip(areaName).toLowerCase(Locale.ROOT).contains("dwarven");
    }

    public static boolean isHubIsland(String scoreboardOrArea) {
        String text = strip(scoreboardOrArea).toLowerCase(Locale.ROOT);
        return text.contains("village")
                || text.contains("community center")
                || text.contains("hub island")
                || "hub".equals(text);
    }

    public static boolean isCorpseStand(String nametag) {
        String name = strip(nametag).toLowerCase(Locale.ROOT);
        return name.contains("corpse")
                || name.contains("vanguard") && name.contains("frozen");
    }

    public static String corpseKeyId(MiningLeftoverPolicy.CorpseType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case UMBER -> "UMBER_KEY";
            case TUNGSTEN -> "TUNGSTEN_KEY";
            case VANGUARD -> "SKELETON_KEY";
            default -> "";
        };
    }

    public static String keyAnnounceLine(
            Map<MiningLeftoverPolicy.CorpseType, Integer> corpses,
            Map<String, Integer> inventoryCounts) {
        if (corpses == null || corpses.isEmpty()) {
            return "";
        }
        Map<String, Integer> counts = inventoryCounts == null ? Map.of() : inventoryCounts;
        List<String> parts = new ArrayList<>();
        for (Map.Entry<MiningLeftoverPolicy.CorpseType, Integer> entry : corpses.entrySet()) {
            String keyId = corpseKeyId(entry.getKey());
            if (keyId.isBlank() || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            int have = counts.getOrDefault(keyId, 0);
            parts.add(titleCase(entry.getKey().name()) + " " + have + "/" + entry.getValue());
        }
        if (parts.isEmpty()) {
            return "";
        }
        return "Corpse keys: " + String.join(", ", parts);
    }

    public static Optional<String> rewriteScathaPetDrop(boolean enabled, String chat) {
        if (!enabled || chat == null) {
            return Optional.empty();
        }
        String plain = strip(chat);
        if (!PET_DROP.matcher(plain).find()) {
            return Optional.empty();
        }
        if (NAMED_RARITY.matcher(plain).find()) {
            return Optional.empty();
        }
        String rarity = rarityFromCodes(chat);
        if (rarity.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(plain + " (" + rarity + ")");
    }

    public static Optional<String> parseServerId(String chat) {
        Matcher matcher = SENDING_TO.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group("id"));
    }

    public static boolean shouldAnnouncePreviousServer(
            String serverId,
            String lastId,
            long lastSeenMs,
            long nowMs,
            int windowSeconds) {
        if (serverId == null || serverId.isBlank() || lastId == null || lastId.isBlank()) {
            return false;
        }
        if (!serverId.equalsIgnoreCase(lastId)) {
            return false;
        }
        int window = clampPreviousServerSeconds(windowSeconds);
        return nowMs >= lastSeenMs && nowMs - lastSeenMs <= window * 1000L;
    }

    public static String previousServerLine(String serverId, long lastSeenMs, long nowMs) {
        long ago = Math.max(0L, nowMs - lastSeenMs) / 1000L;
        return "You've already been on " + strip(serverId) + " " + ago + "s ago";
    }

    public static int clampPreviousServerSeconds(int seconds) {
        return Math.max(MIN_PREVIOUS_SERVER_SECONDS, Math.min(MAX_PREVIOUS_SERVER_SECONDS, seconds));
    }

    public static Optional<Integer> parseQueuePosition(String chat) {
        Matcher matcher = QUEUE_POSITION.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group("n").replace(",", "")));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> estimateQueueSeconds(
            int previousPosition,
            long previousMs,
            int position,
            long nowMs) {
        if (previousPosition <= position || previousMs <= 0L || nowMs <= previousMs) {
            return Optional.empty();
        }
        double cleared = previousPosition - position;
        double elapsedSec = (nowMs - previousMs) / 1000.0D;
        if (cleared <= 0.0D || elapsedSec < 1.0D) {
            return Optional.empty();
        }
        double perSpot = elapsedSec / cleared;
        return Optional.of((int) Math.max(1.0D, Math.round(position * perSpot)));
    }

    public static String queueEstimateLine(int position, int seconds) {
        int minutes = Math.max(1, (int) Math.round(seconds / 60.0D));
        return "Queue #" + position + " ~" + minutes + " min";
    }

    public static Optional<String> parseMinister(List<String> lines) {
        if (lines == null) {
            return Optional.empty();
        }
        for (String line : lines) {
            Matcher matcher = MINISTER.matcher(strip(line));
            if (matcher.find()) {
                return Optional.of(matcher.group("name").trim());
            }
        }
        return Optional.empty();
    }

    public static String ministerTooltip(boolean enabled, String ministerName) {
        if (!enabled || ministerName == null || ministerName.isBlank()) {
            return "";
        }
        return "§eMinister: §b" + ministerName.trim();
    }

    public static int dungeonStars(int dungeonItemLevel, int upgradeLevel) {
        return Math.max(0, Math.max(dungeonItemLevel, upgradeLevel));
    }

    public static String starTooltip(boolean enabled, int stars) {
        if (!enabled || stars <= 0) {
            return "";
        }
        return "§eDungeon Stars: §6" + stars;
    }

    public static String petCandyTooltip(boolean enabled, int candyUsed) {
        if (!enabled || candyUsed < 0) {
            return "";
        }
        return "§ePet Candy: §6" + candyUsed + "/10";
    }

    public static int parseCandyUsed(String petInfo) {
        if (petInfo == null || petInfo.isBlank()) {
            return -1;
        }
        Matcher matcher = Pattern.compile("\"candyUsed\"\\s*:\\s*(-?\\d+)").matcher(petInfo);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static boolean isWitherBlade(String skyBlockId) {
        String id = strip(skyBlockId).toUpperCase(Locale.ROOT);
        return id.equals("HYPERION")
                || id.equals("ASTRAEA")
                || id.equals("VALKYRIE")
                || id.equals("SCYLLA");
    }

    public static boolean isImplosionParticle(String particleId) {
        String id = strip(particleId).toLowerCase(Locale.ROOT);
        return id.endsWith("explosion")
                || id.endsWith("explosion_emitter")
                || id.contains("huge_explosion");
    }

    public static boolean hideImplosion(
            boolean enabled,
            String particleId,
            boolean nearWitherBlade,
            double distanceSq) {
        return enabled
                && nearWitherBlade
                && isImplosionParticle(particleId)
                && distanceSq <= 4.0D;
    }

    public static boolean isHubRat(boolean inHub, boolean babyZombie) {
        return inHub && babyZombie;
    }

    public static boolean isMiningIsland(String areaName) {
        String text = strip(areaName).toLowerCase(Locale.ROOT);
        return isDwarvenMines(areaName)
                || text.contains("crystal hollow")
                || text.contains("glacite")
                || text.contains("mineshaft")
                || text.contains("deep cavern")
                || text.contains("dwarven base");
    }

    public static boolean isCrimsonIsle(String areaName) {
        return strip(areaName).toLowerCase(Locale.ROOT).contains("crimson");
    }

    public static String normalizeCloudMode(String value) {
        if (value == null) {
            return CLOUD_OFF;
        }
        String text = value.trim();
        if (text.equalsIgnoreCase(CLOUD_DWARVEN) || text.equalsIgnoreCase("Dwarven Mines")) {
            return CLOUD_DWARVEN;
        }
        if (text.equalsIgnoreCase(CLOUD_MINING) || text.equalsIgnoreCase("Mining Islands")) {
            return CLOUD_MINING;
        }
        if (text.equalsIgnoreCase(CLOUD_ALWAYS)) {
            return CLOUD_ALWAYS;
        }
        return CLOUD_OFF;
    }

    public static boolean shouldHideClouds(String mode, String areaName) {
        String normalized = normalizeCloudMode(mode);
        if (CLOUD_OFF.equals(normalized)) {
            return false;
        }
        if (CLOUD_ALWAYS.equals(normalized)) {
            return true;
        }
        if (CLOUD_DWARVEN.equals(normalized)) {
            return isDwarvenMines(areaName);
        }
        return isMiningIsland(areaName);
    }

    public static double clampFogScale(double scale) {
        if (!Double.isFinite(scale)) {
            return DEFAULT_NETHER_FOG_SCALE;
        }
        return Math.max(MIN_NETHER_FOG_SCALE, Math.min(MAX_NETHER_FOG_SCALE, scale));
    }

    public static float netherFogFactor(
            boolean enabled,
            boolean inCrimsonIsle,
            boolean hasNightVision,
            double scale) {
        if (!enabled || !inCrimsonIsle || !hasNightVision) {
            return 1.0F;
        }
        return (float) clampFogScale(scale);
    }

    public static int clampArmorPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    public static boolean hideArmor(int percent) {
        return clampArmorPercent(percent) <= 0;
    }

    public static boolean isAbsorbChat(String chat) {
        String plain = strip(chat);
        if (plain.isBlank()) {
            return false;
        }
        for (Pattern pattern : ABSORB_CHAT) {
            if (pattern.matcher(plain).find()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<String> revertMasterStars(boolean enabled, String hoverName, int stars) {
        if (!enabled || hoverName == null || stars <= 5) {
            return Optional.empty();
        }
        Matcher matcher = MASTER_STARS.matcher(hoverName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int master = Math.min(5, stars - 5);
        int gold = 5 - master;
        StringBuilder out = new StringBuilder(matcher.group("first"));
        if (master > 0) {
            out.append("§c");
            out.append("✪".repeat(master));
        }
        if (gold > 0) {
            out.append("§6");
            out.append("✪".repeat(gold));
        }
        out.append(matcher.group("second"));
        return Optional.of(out.toString());
    }

    public static String rewriteMobIcons(boolean enabled, String nametag) {
        if (!enabled || nametag == null || nametag.isEmpty()) {
            return nametag;
        }
        StringBuilder out = new StringBuilder(nametag.length());
        boolean changed = false;
        for (int i = 0; i < nametag.length(); i++) {
            char ch = nametag.charAt(i);
            String label = MOB_ICON_LABELS.get(ch);
            if (label == null) {
                out.append(ch);
            } else {
                changed = true;
                if (!out.isEmpty() && !Character.isWhitespace(out.charAt(out.length() - 1))) {
                    out.append(' ');
                }
                out.append('[').append(label).append(']');
            }
        }
        return changed ? out.toString() : nametag;
    }

    public static String quickJoinLabel(String template, String ip) {
        String safeIp = strip(ip);
        if (safeIp.isBlank()) {
            safeIp = DEFAULT_QUICK_JOIN_IP;
        }
        String text = template == null || template.isBlank() ? DEFAULT_QUICK_JOIN_TEXT : template.trim();
        return text.replace("{ip}", safeIp);
    }

    public static String sanitizeQuickJoinIp(String ip) {
        String text = strip(ip).replaceAll("[\\s\\r\\n]", "");
        if (text.isBlank()) {
            return DEFAULT_QUICK_JOIN_IP;
        }
        return text.length() > 128 ? text.substring(0, 128) : text;
    }

    public static String rarityFromCodes(String chat) {
        if (chat == null) {
            return "";
        }
        String lower = chat.toLowerCase(Locale.ROOT);
        if (lower.contains("§d") || lower.contains("§5")) {
            return "Legendary";
        }
        if (lower.contains("§6")) {
            return "Epic";
        }
        if (lower.contains("§9") || lower.contains("§1")) {
            return "Rare";
        }
        if (lower.contains("§a") || lower.contains("§2")) {
            return "Uncommon";
        }
        if (lower.contains("§f") || lower.contains("§7")) {
            return "Common";
        }
        return "";
    }

    public static Map<MiningLeftoverPolicy.CorpseType, Integer> countUnlooted(
            Map<MiningLeftoverPolicy.CorpseType, Boolean> tab) {
        Map<MiningLeftoverPolicy.CorpseType, Integer> out = new LinkedHashMap<>();
        if (tab == null) {
            return out;
        }
        for (Map.Entry<MiningLeftoverPolicy.CorpseType, Boolean> entry : tab.entrySet()) {
            if (Boolean.FALSE.equals(entry.getValue())) {
                out.merge(entry.getKey(), 1, Integer::sum);
            }
        }
        return out;
    }

    private static Map<Character, String> mobIconLabels() {
        Map<Character, String> labels = new LinkedHashMap<>();
        labels.put('\ue084', "Undead");
        labels.put('\ue081', "Skeletal");
        labels.put('\ue078', "Ender");
        labels.put('\ue074', "Arthropod");
        labels.put('\ue07b', "Humanoid");
        labels.put('\ue07c', "Infernal");
        labels.put('\ue076', "Cubic");
        labels.put('\ue079', "Frozen");
        labels.put('\ue082', "Spooky");
        labels.put('\ue07e', "Mythological");
        labels.put('\ue085', "Wither");
        labels.put('\ue083', "Subterranean");
        labels.put('\ue072', "Aquatic");
        labels.put('\ue018', "Pest");
        labels.put('\ue071', "Animal");
        labels.put('\ue07d', "Magmatic");
        labels.put('\ue077', "Elusive");
        labels.put('\ue075', "Construct");
        labels.put('\ue073', "Arcane");
        labels.put('\ue080', "Shielded");
        labels.put('\ue070', "Airborne");
        labels.put('\ue07a', "Glacial");
        labels.put('\ue086', "Woodland");
        return Map.copyOf(labels);
    }

    private static String titleCase(String value) {
        String text = strip(value).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String strip(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
    }
}
