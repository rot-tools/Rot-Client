package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SkyBlockItemRelationsTest {
    @Test
    void enchantedFormsCoverCoreMiningResources() {
        SkyBlockItemRelations relations = SkyBlockItemRelations.builtin();
        assertEquals(
                "ENCHANTED_HARD_STONE",
                relations.enchantedFormOf("HARD_STONE").orElseThrow());
        assertEquals(
                "ENCHANTED_COBBLESTONE",
                relations.enchantedFormOf("cobblestone").orElseThrow());
        assertEquals(
                "ENCHANTED_GLACITE",
                relations.enchantedFormOf("GLACITE").orElseThrow());
        assertEquals(
                "ENCHANTED_UMBER",
                relations.enchantedFormOf("UMBER").orElseThrow());
        assertEquals(
                "ENCHANTED_COAL",
                relations.enchantedFormOf("COAL").orElseThrow());
        assertEquals(
                "ENCHANTED_IRON",
                relations.enchantedFormOf("IRON_INGOT").orElseThrow());
        assertEquals(
                "ENCHANTED_REDSTONE",
                relations.enchantedFormOf("REDSTONE").orElseThrow());
        // Physical block evidence tokens are not enchanted-form aliases.
        assertTrue(relations.enchantedFormOf("COAL_ORE").isEmpty());
        assertTrue(relations.enchantedFormOf("IRON_ORE").isEmpty());
        assertTrue(relations.enchantedFormOf("UNKNOWN_ORE").isEmpty());
    }

    @Test
    void gemstoneTierFollowResearchUpgradeChain() {
        SkyBlockItemRelations relations = SkyBlockItemRelations.builtin();
        assertEquals(
                "FLAWED_RUBY_GEM",
                relations.nextGemstoneTier("ROUGH_RUBY_GEM").orElseThrow());
        assertEquals(
                "FINE_RUBY_GEM",
                relations.nextGemstoneTier("FLAWED_RUBY_GEM").orElseThrow());
        assertEquals(
                "PERFECT_JADE_GEM",
                relations.nextGemstoneTier("FLAWLESS_JADE_GEM").orElseThrow());
        assertTrue(relations.nextGemstoneTier("PERFECT_RUBY_GEM").isEmpty());
        assertFalse(relations.hasKnownEnchantedForm("ROUGH_RUBY_GEM"));
    }
}
