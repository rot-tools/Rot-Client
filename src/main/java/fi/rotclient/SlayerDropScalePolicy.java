package fi.rotclient;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Pure Slayer drop family, lifetime, and range rules. */
public final class SlayerDropScalePolicy {
    public record Window(
            SlayerPolicy.SlayerType type,
            long bossDiedAtMillis,
            double x,
            double y,
            double z) {
    }

    private static final Map<SlayerPolicy.SlayerType, Set<String>> DROPS = Map.of(
            SlayerPolicy.SlayerType.REVENANT, Set.of(
                    "REVENANT_FLESH", "FOUL_FLESH", "PESTILENCE_RUNE", "UNDEAD_CATALYST",
                    "SMITE_6", "BEHEADED_HORROR", "REVENANT_CATALYST", "SNAKE_RUNE",
                    "FESTERING_MAGGOT", "REVENANT_VISCERA", "SCYTHE_BLADE", "SEVERED_HAND",
                    "SHREDDED_SINEW", "WARDEN_HEART", "MATCH_STICKS", "MATCHA_DYE"),
            SlayerPolicy.SlayerType.TARANTULA, Set.of(
                    "TARANTULA_WEB", "TOXIC_ARROW_POISON", "BITE_RUNE", "DARK_QUEENS_SOUL_DROP",
                    "SPIDER_CATALYST", "TARANTULA_SILK", "BANE_OF_ARTHROPODS_6",
                    "TARANTULA_CATALYST", "FLY_SWATTER", "VIAL_OF_VENOM", "TARANTULA_TALISMAN",
                    "DIGESTED_MOSQUITO", "SHRIVELED_WASP", "ENSNARED_SNAIL", "PRIMORDIAL_EYE",
                    "BRICK_RED_DYE"),
            SlayerPolicy.SlayerType.SVEN, Set.of(
                    "WOLF_TOOTH", "HAMSTER_WHEEL", "SPIRIT_RUNE", "CRITICAL_6", "FURBALL",
                    "RED_CLAW_EGG", "COUTURE_RUNE", "GRIZZLY_BAIT", "OVERFLUX_CAPACITOR",
                    "CELESTE_DYE"),
            SlayerPolicy.SlayerType.VOIDGLOOM, Set.of(
                    "NULL_SPHERE", "TWILIGHT_ARROW_POISON", "ENDERSNAKE_RUNE", "SUMMONING_EYE",
                    "MANA_STEAL_1", "TRANSMISSION_TUNER", "NULL_ATOM", "HAZMAT_ENDERMAN",
                    "POCKET_ESPRESSO_MACHINE", "SMARTY_PANTS_1", "END_RUNE", "HANDY_BLOOD_CHALICE",
                    "SINFUL_DICE", "ENDER_ARTIFACT_UPGRADER", "VOID_CONQUEROR_ENDERMAN_SKIN",
                    "ETHERWARP_MERGER", "JUDGEMENT_CORE", "ENCHANT_RUNE", "ENDSTONE_IDOL",
                    "BYZANTIUM_DYE"),
            SlayerPolicy.SlayerType.INFERNO, Set.of(
                    "DERELICT_ASHE", "ENCHANTED_BLAZE_POWDER", "LAVA_TEARS_RUNE", "WISP_ICE_WATER",
                    "BUNDLE_OF_MAGMA_ARROWS", "MANA_DISINTEGRATOR", "SCORCHED_BOOKS", "KELVIN_INVERTER",
                    "BLAZE_ROD_DISTILLATE", "GLOWSTONE_DUST_DISTILLATE", "MAGMA_CREAM_DISTILLATE",
                    "NETHER_STALK_DISTILLATE", "GABAGOOL_DISTILLATE", "SCORCHED_POWER_CRYSTAL",
                    "ARCHFIEND_DICE", "FIRE_ASPECT_3", "FIERY_BURST_RUNE", "FLAWED_OPAL_GEM",
                    "DUPLEX_1", "HIGH_CLASS_ARCHFIEND_DICE", "WILSON_ENGINEERING_PLANS",
                    "SUBZERO_INVERTER", "FLAME_DYE"),
            SlayerPolicy.SlayerType.VAMPIRE, Set.of(
                    "COVEN_SEAL", "BUNDLE_OF_QUANTUM", "SOULTWIST_RUNE", "BUBBA_BLISTER",
                    "CHOCOLATE_CHIP", "GUARDIAN_LUCKY_BLOCK", "MCGRUBBER_BURGER",
                    "UNFANGED_VAMPIRE_PART", "BUNDLE_OF_THE_ONE", "SANGRIA_DYE"));

    private SlayerDropScalePolicy() {
    }

    public static boolean isDropFor(SlayerPolicy.SlayerType type, String skyBlockId) {
        if (type == null) {
            return false;
        }
        return DROPS.getOrDefault(type, Set.of()).contains(normalize(skyBlockId));
    }

    public static Set<String> dropsFor(SlayerPolicy.SlayerType type) {
        return type == null ? Set.of() : Set.copyOf(DROPS.getOrDefault(type, Set.of()));
    }

    public static Set<String> allDrops() {
        Set<String> all = new TreeSet<>();
        DROPS.values().forEach(all::addAll);
        return Set.copyOf(all);
    }

    /** Empty configured list intentionally means the complete default catalog. */
    public static boolean selected(java.util.Collection<String> configured, String skyBlockId) {
        String id = normalize(skyBlockId);
        if (!allDrops().contains(id)) return false;
        if (configured == null || configured.isEmpty()) return true;
        return configured.stream().map(SlayerDropScalePolicy::normalize).anyMatch(id::equals);
    }

    public static boolean shouldScale(
            Window window,
            String skyBlockId,
            long nowMillis,
            double itemX,
            double itemY,
            double itemZ,
            double rangeMultiplier,
            int unscaleSeconds) {
        if (window == null || !isDropFor(window.type(), skyBlockId)) {
            return false;
        }
        long lifetime = clampUnscaleSeconds(unscaleSeconds) * 1_000L;
        if (nowMillis < window.bossDiedAtMillis()
                || nowMillis - window.bossDiedAtMillis() >= lifetime) {
            return false;
        }
        double radius = 10.0D * clampRangeMultiplier(rangeMultiplier);
        double dx = itemX - window.x();
        double dy = itemY - window.y();
        double dz = itemZ - window.z();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public static double clampScale(double value) {
        if (!Double.isFinite(value)) return 3.0D;
        return Math.max(1.0D, Math.min(8.0D, value));
    }

    public static double clampRangeMultiplier(double value) {
        if (!Double.isFinite(value)) return 1.0D;
        return Math.max(0.5D, Math.min(5.0D, value));
    }

    public static int clampUnscaleSeconds(int value) {
        return Math.max(1, Math.min(60, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
