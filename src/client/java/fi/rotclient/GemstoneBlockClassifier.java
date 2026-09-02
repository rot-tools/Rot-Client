package fi.rotclient;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Maps vanilla stained glass identifiers to Hypixel gemstone types.
 *
 * The mapping intentionally avoids direct Blocks constants because their
 * mapped Java field names can change between Minecraft versions.
 */
final class GemstoneBlockClassifier {
    private static final String DESCRIPTION_PREFIX =
            "block.minecraft.";

    private static final String REGISTRY_PREFIX =
            "minecraft:";

    private GemstoneBlockClassifier() {
    }

    static GemstoneType fromBlockState(
            BlockState state) {
        if (state == null) {
            return null;
        }

        return fromIdentifier(
                state.getBlock()
                        .getDescriptionId());
    }

    static GemstoneType fromIdentifier(
            String identifier) {
        String blockName =
                normalizeBlockName(
                        identifier);

        if (blockName == null) {
            return null;
        }

        return switch (blockName) {
            case "red_stained_glass",
                    "red_stained_glass_pane" ->
                    GemstoneType.RUBY;

            case "orange_stained_glass",
                    "orange_stained_glass_pane" ->
                    GemstoneType.AMBER;

            case "light_blue_stained_glass",
                    "light_blue_stained_glass_pane" ->
                    GemstoneType.SAPPHIRE;

            case "lime_stained_glass",
                    "lime_stained_glass_pane" ->
                    GemstoneType.JADE;

            case "purple_stained_glass",
                    "purple_stained_glass_pane" ->
                    GemstoneType.AMETHYST;

            case "yellow_stained_glass",
                    "yellow_stained_glass_pane" ->
                    GemstoneType.TOPAZ;

            case "magenta_stained_glass",
                    "magenta_stained_glass_pane" ->
                    GemstoneType.JASPER;

            case "white_stained_glass",
                    "white_stained_glass_pane" ->
                    GemstoneType.OPAL;

            case "black_stained_glass",
                    "black_stained_glass_pane" ->
                    GemstoneType.ONYX;

            case "blue_stained_glass",
                    "blue_stained_glass_pane" ->
                    GemstoneType.AQUAMARINE;

            case "brown_stained_glass",
                    "brown_stained_glass_pane" ->
                    GemstoneType.CITRINE;

            case "green_stained_glass",
                    "green_stained_glass_pane" ->
                    GemstoneType.PERIDOT;

            default -> null;
        };
    }

    private static String normalizeBlockName(
            String identifier) {
        if (identifier == null
                || identifier.isBlank()) {
            return null;
        }

        String normalized =
                identifier.trim()
                        .toLowerCase(
                                Locale.ROOT);

        if (normalized.startsWith(
                DESCRIPTION_PREFIX)) {
            normalized =
                    normalized.substring(
                            DESCRIPTION_PREFIX.length());
        }
        else if (normalized.startsWith(
                REGISTRY_PREFIX)) {
            normalized =
                    normalized.substring(
                            REGISTRY_PREFIX.length());
        }
        else if (normalized.indexOf(':') >= 0
                || normalized.indexOf('.') >= 0) {
            return null;
        }

        return normalized;
    }
}
