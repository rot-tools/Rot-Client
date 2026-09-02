package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trophy-fish chat + Golden Fish lava timer, formatting-stripped.
 */
public final class FishingTrophyPolicy {
    public static final String DEFAULT_MIN_RARITY = "BRONZE";
    public static final List<String> RARITIES = List.of("BRONZE", "SILVER", "GOLD", "DIAMOND");
    public static final int GOLDEN_DESPAWN_SECONDS = 60;
    public static final int GOLDEN_WEAK_HITS = 3;

    private static final Pattern TROPHY = Pattern.compile(
            "TROPHY FISH! You caught an? (.+?) (Bronze|Silver|Gold|Diamond)!",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GOLDEN_SPAWN = Pattern.compile(
            "You spot a Golden Fish surface from beneath the lava!", Pattern.CASE_INSENSITIVE);
    private static final Pattern GOLDEN_INTERACT = Pattern.compile(
            "The Golden Fish escapes your hook but looks weakened\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern GOLDEN_WEAK = Pattern.compile(
            "The Golden Fish is weak!", Pattern.CASE_INSENSITIVE);
    private static final Pattern GOLDEN_DESPAWN = Pattern.compile(
            "The Golden Fish swims back beneath the lava\\.", Pattern.CASE_INSENSITIVE);

    public record Catch(String name, String rarity) {
        public int rank() {
            return rankOf(rarity);
        }
    }

    public enum GoldenEvent {
        NONE,
        SPAWN,
        INTERACT,
        WEAK,
        DESPAWN
    }

    private FishingTrophyPolicy() {
    }

    public static String normalizeRarity(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MIN_RARITY;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return RARITIES.contains(upper) ? upper : DEFAULT_MIN_RARITY;
    }

    public static int rankOf(String rarity) {
        int idx = RARITIES.indexOf(normalizeRarity(rarity));
        return Math.max(0, idx);
    }

    public static Catch parseCatch(String stripped) {
        Matcher matcher = TROPHY.matcher(FishingCreaturesPolicy.strip(stripped));
        if (!matcher.find()) {
            return null;
        }
        return new Catch(matcher.group(1).trim(), matcher.group(2).toUpperCase(Locale.ROOT));
    }

    public static boolean shouldHideCatch(boolean filterOn, Catch catchInfo, String minimum) {
        return filterOn && catchInfo != null && catchInfo.rank() < rankOf(minimum);
    }

    public static boolean shouldTitle(boolean moduleEnabled, boolean titlesOn, Catch catchInfo) {
        return moduleEnabled && titlesOn && catchInfo != null;
    }

    public static GoldenEvent goldenEvent(String stripped) {
        String text = FishingCreaturesPolicy.strip(stripped);
        if (GOLDEN_SPAWN.matcher(text).find()) {
            return GoldenEvent.SPAWN;
        }
        if (GOLDEN_INTERACT.matcher(text).find()) {
            return GoldenEvent.INTERACT;
        }
        if (GOLDEN_WEAK.matcher(text).find()) {
            return GoldenEvent.WEAK;
        }
        if (GOLDEN_DESPAWN.matcher(text).find()) {
            return GoldenEvent.DESPAWN;
        }
        return GoldenEvent.NONE;
    }

    public static int nextGoldenHits(GoldenEvent event, int currentHits) {
        int hits = Math.max(0, currentHits);
        return switch (event) {
            case SPAWN -> 0;
            case INTERACT -> Math.min(GOLDEN_WEAK_HITS, hits + 1);
            case WEAK -> GOLDEN_WEAK_HITS;
            case DESPAWN, NONE -> hits;
        };
    }

    public static boolean isTrophyFishId(String skyBlockId) {
        String rarity = trophyRarityFromId(skyBlockId);
        return rarity != null && skyBlockId.toUpperCase(Locale.ROOT).contains("_");
    }

    public static String trophyRarityFromId(String skyBlockId) {
        if (skyBlockId == null || skyBlockId.isBlank()) {
            return null;
        }
        String id = skyBlockId.trim().toUpperCase(Locale.ROOT);
        for (String rarity : RARITIES) {
            if (id.endsWith("_" + rarity)) {
                return rarity;
            }
        }
        return null;
    }

    public static Integer filletMagmafish(String skyBlockId, int count) {
        String rarity = trophyRarityFromId(skyBlockId);
        if (rarity == null) {
            return null;
        }
        int per = switch (rarity) {
            case "BRONZE" -> 1;
            case "SILVER" -> 2;
            case "GOLD" -> 5;
            default -> 10;
        };
        return per * Math.max(1, count);
    }

    public static boolean isGeyserCloud(String particleId, double y) {
        if (particleId == null) {
            return false;
        }
        String id = particleId.toLowerCase(Locale.ROOT);
        return id.contains("cloud") && y >= 117.0D && y <= 119.5D;
    }

    public static String goldenHud(boolean timerOn, boolean active, int hits, long ageMs) {
        if (!timerOn || !active) {
            return "";
        }
        long remain = Math.max(0L, GOLDEN_DESPAWN_SECONDS * 1000L - Math.max(0L, ageMs));
        return "Golden Fish " + hits + "/" + GOLDEN_WEAK_HITS
                + "  " + FishingCreaturesPolicy.formatSeconds(remain / 1000.0D);
    }
}
