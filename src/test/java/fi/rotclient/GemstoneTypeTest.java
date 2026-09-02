package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GemstoneTypeTest {
    @Test
    void containsAllSupportedGemstoneTypes() {
        assertEquals(12, GemstoneType.values().length);

        assertEquals(
                GemstoneType.RUBY,
                GemstoneType.fromId("RUBY"));
        assertEquals(
                GemstoneType.AMBER,
                GemstoneType.fromId("amber"));
        assertEquals(
                GemstoneType.SAPPHIRE,
                GemstoneType.fromId("SAPPHIRE"));
        assertEquals(
                GemstoneType.JADE,
                GemstoneType.fromId("jade"));
        assertEquals(
                GemstoneType.AMETHYST,
                GemstoneType.fromId("AMETHYST"));
        assertEquals(
                GemstoneType.TOPAZ,
                GemstoneType.fromId("topaz"));
        assertEquals(
                GemstoneType.JASPER,
                GemstoneType.fromId("JASPER"));
        assertEquals(
                GemstoneType.OPAL,
                GemstoneType.fromId("opal"));
        assertEquals(
                GemstoneType.ONYX,
                GemstoneType.fromId("ONYX"));
        assertEquals(
                GemstoneType.AQUAMARINE,
                GemstoneType.fromId("aquamarine"));
        assertEquals(
                GemstoneType.CITRINE,
                GemstoneType.fromId("CITRINE"));
        assertEquals(
                GemstoneType.PERIDOT,
                GemstoneType.fromId("peridot"));
    }

    @Test
    void buildsGemstoneItemNames() {
        assertEquals(
                "Rough Ruby Gemstone",
                GemstoneType.RUBY.itemName(
                        GemstoneTier.ROUGH));

        assertEquals(
                "Flawless Onyx Gemstone",
                GemstoneType.ONYX.itemName(
                        GemstoneTier.FLAWLESS));

        assertEquals(
                "Perfect Aquamarine Gemstone",
                GemstoneType.AQUAMARINE.itemName(
                        GemstoneTier.PERFECT));
    }

    @Test
    void buildsGemstoneBazaarIds() {
        assertEquals(
                "ROUGH_RUBY_GEM",
                GemstoneType.RUBY.bazaarId(
                        GemstoneTier.ROUGH));

        assertEquals(
                "FINE_JADE_GEM",
                GemstoneType.JADE.bazaarId(
                        GemstoneTier.FINE));

        assertEquals(
                "PERFECT_PERIDOT_GEM",
                GemstoneType.PERIDOT.bazaarId(
                        GemstoneTier.PERFECT));
    }

    @Test
    void detectsGemstoneFromItemName() {
        assertEquals(
                GemstoneType.SAPPHIRE,
                GemstoneType.fromItemName(
                        "Flawed Sapphire Gemstone"));

        assertEquals(
                GemstoneType.CITRINE,
                GemstoneType.fromItemName(
                        "Perfect Citrine Gemstone"));

        assertEquals(
                GemstoneType.JASPER,
                GemstoneType.fromItemName(
                        "rough jasper gemstone"));
    }

    @Test
    void matchesOnlyItsOwnGemstoneItems() {
        assertTrue(
                GemstoneType.RUBY.matchesItemName(
                        "Fine Ruby Gemstone"));

        assertFalse(
                GemstoneType.RUBY.matchesItemName(
                        "Fine Jade Gemstone"));

        assertFalse(
                GemstoneType.RUBY.matchesItemName(
                        "Enchanted Gold"));

        assertFalse(
                GemstoneType.RUBY.matchesItemName(null));
    }

    @Test
    void rejectsUnknownAndMissingValues() {
        assertNull(GemstoneType.fromId(null));
        assertNull(GemstoneType.fromId(""));
        assertNull(GemstoneType.fromId("EMERALD"));

        assertNull(GemstoneType.fromItemName(null));
        assertNull(
                GemstoneType.fromItemName(
                        "Enchanted Diamond"));

        assertThrows(
                IllegalArgumentException.class,
                () -> GemstoneType.RUBY.itemName(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> GemstoneType.RUBY.bazaarId(null));
    }
}
