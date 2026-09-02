package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GemstoneBlockClassifierTest {
    @Test
    void mapsAllGlassDescriptionIdentifiers() {
        assertMapping(
                GemstoneType.RUBY,
                "red");

        assertMapping(
                GemstoneType.AMBER,
                "orange");

        assertMapping(
                GemstoneType.SAPPHIRE,
                "light_blue");

        assertMapping(
                GemstoneType.JADE,
                "lime");

        assertMapping(
                GemstoneType.AMETHYST,
                "purple");

        assertMapping(
                GemstoneType.TOPAZ,
                "yellow");

        assertMapping(
                GemstoneType.JASPER,
                "magenta");

        assertMapping(
                GemstoneType.OPAL,
                "white");

        assertMapping(
                GemstoneType.ONYX,
                "black");

        assertMapping(
                GemstoneType.AQUAMARINE,
                "blue");

        assertMapping(
                GemstoneType.CITRINE,
                "brown");

        assertMapping(
                GemstoneType.PERIDOT,
                "green");
    }

    @Test
    void acceptsRegistryIdentifiers() {
        assertEquals(
                GemstoneType.RUBY,
                GemstoneBlockClassifier.fromIdentifier(
                        "minecraft:red_stained_glass"));

        assertEquals(
                GemstoneType.JADE,
                GemstoneBlockClassifier.fromIdentifier(
                        "minecraft:lime_stained_glass_pane"));

        assertEquals(
                GemstoneType.AQUAMARINE,
                GemstoneBlockClassifier.fromIdentifier(
                        "minecraft:blue_stained_glass"));
    }

    @Test
    void acceptsNormalizedBareBlockNames() {
        assertEquals(
                GemstoneType.TOPAZ,
                GemstoneBlockClassifier.fromIdentifier(
                        "yellow_stained_glass"));

        assertEquals(
                GemstoneType.PERIDOT,
                GemstoneBlockClassifier.fromIdentifier(
                        "green_stained_glass_pane"));
    }

    @Test
    void ignoresCaseAndOuterWhitespace() {
        assertEquals(
                GemstoneType.RUBY,
                GemstoneBlockClassifier.fromIdentifier(
                        "  BLOCK.MINECRAFT.RED_STAINED_GLASS  "));

        assertEquals(
                GemstoneType.JASPER,
                GemstoneBlockClassifier.fromIdentifier(
                        "  Minecraft:Magenta_Stained_Glass_Pane  "));
    }

    @Test
    void rejectsUnrelatedAndUnknownBlocks() {
        assertNull(
                GemstoneBlockClassifier.fromIdentifier(
                        null));

        assertNull(
                GemstoneBlockClassifier.fromIdentifier(
                        ""));

        assertNull(
                GemstoneBlockClassifier.fromIdentifier(
                        "block.minecraft.stone"));

        assertNull(
                GemstoneBlockClassifier.fromIdentifier(
                        "block.minecraft.clear_glass"));

        assertNull(
                GemstoneBlockClassifier.fromIdentifier(
                        "othermod:red_stained_glass"));
    }

    private static void assertMapping(
            GemstoneType expected,
            String color) {
        assertEquals(
                expected,
                GemstoneBlockClassifier.fromIdentifier(
                        "block.minecraft."
                                + color
                                + "_stained_glass"));

        assertEquals(
                expected,
                GemstoneBlockClassifier.fromIdentifier(
                        "block.minecraft."
                                + color
                                + "_stained_glass_pane"));
    }
}
