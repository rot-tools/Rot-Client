package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Slayer polish that is not family-specific: spawn-particle and
 * damage-splash filters, spawn-kill nametags, Inferno gummy warning, Maddox
 * not-spawnable clicks, Vampire chalice/effigy helpers, and personal-best keys.
 */
public final class SlayerPolishPolicy {
    public static final long SPAWN_PARTICLE_WINDOW_MILLIS = 3_000L;
    public static final double SPAWN_PARTICLE_RANGE = 5.0D;
    public static final double DAMAGE_SPLASH_RANGE = 8.0D;
    public static final long GUMMY_DURATION_MILLIS = 60L * 60L * 1_000L;
    public static final long GUMMY_WARN_INTERVAL_MILLIS = 10_000L;

    public record BlockCoord(int x, int y, int z) {
    }

    public static final List<BlockCoord> EFFIGY_POSITIONS = List.of(
            new BlockCoord(150, 79, 95),
            new BlockCoord(193, 93, 119),
            new BlockCoord(235, 110, 147),
            new BlockCoord(293, 96, 134),
            new BlockCoord(262, 99, 94),
            new BlockCoord(240, 129, 118));

    private static final Pattern DAMAGE_SPLASH = Pattern.compile(
            "^[✧✯]?\\d+[⚔+✧❤♞☄✷ﬗ✯]*$");
    private static final Pattern SPAWN_MOB_NAME = Pattern.compile(
            "(?i)^\\[Lv\\d+]\\s+(?:[^\\s]+\\s+)*?(?<name>.+?)\\s+(?<min>[\\d.,kmb]+)/(?<max>[\\d.,kmb]+)❤$");
    private static final Pattern CHALICE = Pattern.compile("(?i)^\\d+(?:\\.\\d+)?s$");
    private static final Pattern NOT_SPAWNABLE = Pattern.compile(
            "(?i).*(only inside the rift!?|doesn't exist here!).*");
    private static final Pattern GUMMY_CONSUME = Pattern.compile(
            "(?i).*(re-heated gummy polar bear|gummy polar bear).*");
    private static final Pattern HABANERO = Pattern.compile("(?i)ultimate_habanero_tactics");

    private SlayerPolishPolicy() {
    }

    public static String personalBestKey(SlayerPolicy.SlayerType type, int tier) {
        String family = type == null ? "UNKNOWN" : type.name();
        return family + ':' + Math.max(0, Math.min(5, tier));
    }

    public static Optional<Long> improvedPersonalBest(
            Long previousMillis,
            long durationMillis) {
        if (durationMillis < 0L) {
            return Optional.empty();
        }
        if (previousMillis == null || durationMillis < previousMillis) {
            return Optional.of(durationMillis);
        }
        return Optional.empty();
    }

    public static boolean isSpawnParticle(String particleId) {
        String id = normalizeId(particleId);
        return id.contains("enchant")
                || id.contains("witch")
                || id.contains("entity_effect")
                || id.contains("instant_effect")
                || id.contains("mob_spell")
                || id.equals("minecraft:effect");
    }

    public static boolean nearRecentDeath(
            double x,
            double y,
            double z,
            double deathX,
            double deathY,
            double deathZ,
            long diedAtMillis,
            long nowMillis) {
        if (nowMillis - diedAtMillis > SPAWN_PARTICLE_WINDOW_MILLIS || nowMillis < diedAtMillis) {
            return false;
        }
        double dx = x - deathX;
        double dy = y - deathY;
        double dz = z - deathZ;
        return dx * dx + dy * dy + dz * dz <= SPAWN_PARTICLE_RANGE * SPAWN_PARTICLE_RANGE;
    }

    public static boolean isDamageSplash(String hologram) {
        String text = SlayerFightPolicy.normalize(hologram).replace(",", "").trim();
        if (text.isEmpty() || text.contains("/") || text.contains(" ")) {
            return false;
        }
        if (text.chars().anyMatch(ch -> ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z')) {
            return false;
        }
        return DAMAGE_SPLASH.matcher(text).matches();
    }

    public static boolean shouldHideSpawnMobName(String hologram) {
        String text = SlayerFightPolicy.normalize(hologram).trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("corrupted")
                || lower.contains("runic")
                || lower.contains("rare")
                || lower.contains("super")
                || lower.contains("revenant")
                || lower.contains("tarantula")
                || lower.contains("sven")
                || lower.contains("voidgloom")
                || lower.contains("inferno")
                || lower.contains("bloodfiend")
                || lower.contains("broodfather")
                || lower.contains("packmaster")
                || lower.contains("demonlord")
                || lower.contains("seraph")) {
            return false;
        }
        Matcher matcher = SPAWN_MOB_NAME.matcher(text);
        if (!matcher.matches()) {
            return false;
        }
        String min = matcher.group("min");
        String max = matcher.group("max");
        return min.equals(max) || min.equals("0") || min.equals("0.0");
    }

    public static boolean isChaliceHologram(String hologram) {
        return CHALICE.matcher(SlayerFightPolicy.normalize(hologram).trim()).matches();
    }

    public static boolean isNotSpawnableLore(String loreLine) {
        return NOT_SPAWNABLE.matcher(SlayerFightPolicy.normalize(loreLine)).matches();
    }

    public static boolean isMaddoxSlayerMenu(String title) {
        return SlayerFightPolicy.normalize(title).equalsIgnoreCase("Slayer");
    }

    public static boolean shouldBlockMaddoxClick(String title, List<String> lore) {
        if (!isMaddoxSlayerMenu(title) || lore == null) {
            return false;
        }
        for (String line : lore) {
            if (isNotSpawnableLore(line)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGummyConsumeChat(String line) {
        return GUMMY_CONSUME.matcher(SlayerFightPolicy.normalize(line)).find();
    }

    public static boolean isGummyActive(long expiresAtMillis, long nowMillis, String tabOrSidebar) {
        if (expiresAtMillis > nowMillis) {
            return true;
        }
        String text = tabOrSidebar == null ? "" : tabOrSidebar.toLowerCase(Locale.ROOT);
        return text.contains("gummy polar") || text.contains("smoldering polar");
    }

    public static boolean isSmolderingArea(String sidebar) {
        String text = SlayerFightPolicy.normalize(sidebar).toLowerCase(Locale.ROOT);
        return text.contains("smoldering tomb") || text.contains("the wasteland");
    }

    public static boolean hasHabaneroEnchant(String enchantId) {
        return enchantId != null && HABANERO.matcher(enchantId).find();
    }

    public static boolean shouldWarnGummy(
            boolean habanero,
            boolean smolderingArea,
            boolean gummyActive) {
        return (habanero || smolderingArea) && !gummyActive;
    }

    public static List<BlockCoord> unbrokenEffigies(List<String> colorNames) {
        List<BlockCoord> result = new ArrayList<>();
        if (colorNames == null) {
            return List.of();
        }
        int count = Math.min(EFFIGY_POSITIONS.size(), colorNames.size());
        for (int i = 0; i < count; i++) {
            if (isUnbrokenEffigyColor(colorNames.get(i))) {
                result.add(EFFIGY_POSITIONS.get(i));
            }
        }
        return List.copyOf(result);
    }

    public static boolean isUnbrokenEffigyColor(String colorName) {
        String color = colorName == null ? "" : colorName.trim().toLowerCase(Locale.ROOT);
        return color.contains("gray")
                || color.contains("grey")
                || color.contains("aaaaaa")
                || color.contains("555555");
    }

    public static boolean isEffigyScoreboardLine(String line) {
        return SlayerFightPolicy.normalize(line).toLowerCase(Locale.ROOT).contains("effig");
    }

    private static String normalizeId(String particleId) {
        return particleId == null ? "" : particleId.trim().toLowerCase(Locale.ROOT);
    }
}
