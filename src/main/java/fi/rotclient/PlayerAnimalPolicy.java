package fi.rotclient;

import java.util.List;
import java.util.Locale;

/** Pure selection rules for client-only player animal rendering. */
public final class PlayerAnimalPolicy {
    public static final List<String> SCOPES = List.of("Self", "Players", "Everyone");
    public static final List<String> SPECIES = List.of("Cow", "Pig", "Cat", "Wolf");

    private PlayerAnimalPolicy() {
    }

    public static boolean applies(
            boolean enabled, String scope, int entityId, int localPlayerId) {
        if (!enabled || entityId < 0 || localPlayerId < 0) {
            return false;
        }
        boolean self = entityId == localPlayerId;
        return switch (normalize(scope, SCOPES, "Self")) {
            case "Players" -> !self;
            case "Everyone" -> true;
            default -> self;
        };
    }

    public static String normalizeSpecies(String value) {
        return normalize(value, SPECIES, "Cow");
    }

    public static String texture(String species, boolean baby) {
        String suffix = baby ? "_baby" : "";
        return switch (normalizeSpecies(species)) {
            case "Pig" -> "textures/entity/pig/pig_temperate" + suffix + ".png";
            case "Cat" -> "textures/entity/cat/cat_tabby" + suffix + ".png";
            case "Wolf" -> "textures/entity/wolf/wolf" + suffix + ".png";
            default -> "textures/entity/cow/cow_temperate" + suffix + ".png";
        };
    }

    private static String normalize(String value, List<String> allowed, String fallback) {
        String raw = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        for (String option : allowed) {
            if (option.toLowerCase(Locale.ROOT).equals(raw)) {
                return option;
            }
        }
        return fallback;
    }
}
