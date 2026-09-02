package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GemstoneTierTest {
    @Test
    void roughEquivalentUsesGemstoneCraftingRatios() {
        assertEquals(1L, GemstoneTier.ROUGH.roughEquivalent());
        assertEquals(80L, GemstoneTier.FLAWED.roughEquivalent());
        assertEquals(6_400L, GemstoneTier.FINE.roughEquivalent());
        assertEquals(
                512_000L,
                GemstoneTier.FLAWLESS.roughEquivalent());
        assertEquals(
                2_560_000L,
                GemstoneTier.PERFECT.roughEquivalent());
    }

    @Test
    void buildsBazaarProductIds() {
        assertEquals(
                "ROUGH_RUBY_GEM",
                GemstoneTier.ROUGH.bazaarId("RUBY"));
        assertEquals(
                "PERFECT_AQUAMARINE_GEM",
                GemstoneTier.PERFECT.bazaarId("aquamarine"));
    }

    @Test
    void buildsDisplayNames() {
        assertEquals(
                "Rough Ruby Gemstone",
                GemstoneTier.ROUGH.itemName("Ruby"));
        assertEquals(
                "Flawless Onyx Gemstone",
                GemstoneTier.FLAWLESS.itemName("Onyx"));
    }

    @Test
    void detectsTierFromItemName() {
        assertEquals(
                GemstoneTier.ROUGH,
                GemstoneTier.fromItemName(
                        "Rough Ruby Gemstone"));
        assertEquals(
                GemstoneTier.PERFECT,
                GemstoneTier.fromItemName(
                        "Perfect Peridot Gemstone"));
        assertNull(
                GemstoneTier.fromItemName(
                        "Enchanted Mithril"));
    }

    @Test
    void rejectsBlankNamesAndIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GemstoneTier.ROUGH.bazaarId(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> GemstoneTier.ROUGH.itemName(" "));
    }
}
