package fi.rotclient;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrackedMaterialTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void profilesExposeHypixelNamesRecipesAndBazaarIds() {
        assertEquals("Gold Ingot", TrackedMaterial.GOLD.rawItemName());
        assertEquals(
                "ENCHANTED_GOLD",
                TrackedMaterial.GOLD.enchantedBazaarId());

        assertEquals("Diamond", TrackedMaterial.DIAMOND.rawItemName());
        assertEquals("DIAMOND", TrackedMaterial.DIAMOND.rawBazaarId());
        assertEquals(
                "ENCHANTED_DIAMOND",
                TrackedMaterial.DIAMOND.enchantedBazaarId());

        assertEquals("Mithril", TrackedMaterial.MITHRIL.rawItemName());
        assertEquals(
                "MITHRIL_ORE",
                TrackedMaterial.MITHRIL.rawBazaarId());
        assertEquals(
                "ENCHANTED_MITHRIL",
                TrackedMaterial.MITHRIL.enchantedBazaarId());

        assertEquals("Titanium", TrackedMaterial.TITANIUM.rawItemName());
        assertEquals(
                "TITANIUM_ORE",
                TrackedMaterial.TITANIUM.rawBazaarId());
        assertEquals(
                "ENCHANTED_TITANIUM",
                TrackedMaterial.TITANIUM.enchantedBazaarId());

        assertEquals("Tungsten", TrackedMaterial.TUNGSTEN.rawItemName());
        assertEquals(
                "TUNGSTEN",
                TrackedMaterial.TUNGSTEN.rawBazaarId());
        assertEquals(
                "ENCHANTED_TUNGSTEN",
                TrackedMaterial.TUNGSTEN.enchantedBazaarId());
        assertEquals("Umber", TrackedMaterial.UMBER.rawItemName());
        assertEquals("UMBER", TrackedMaterial.UMBER.rawBazaarId());
        assertEquals("ENCHANTED_UMBER",
                TrackedMaterial.UMBER.enchantedBazaarId());
        assertEquals("ENCHANTED_LAPIS_LAZULI_BLOCK",
                TrackedMaterial.LAPIS.enchantedBlockBazaarId());
        assertEquals("ENCHANTED_COAL_BLOCK",
                TrackedMaterial.COAL.enchantedBlockBazaarId());
        assertNull(TrackedMaterial.TUNGSTEN.enchantedBlockBazaarId());

        for (TrackedMaterial material : TrackedMaterial.values()) {
            int expectedRawPerEnchanted =
                    material == TrackedMaterial.HARD_STONE ? 576 : 160;
            assertEquals(
                    expectedRawPerEnchanted,
                    material.rawPerEnchanted(),
                    material.name());
        }

        assertEquals(160, TrackedMaterial.GOLD.enchantedPerBlock());
        assertEquals(160, TrackedMaterial.DIAMOND.enchantedPerBlock());
        assertEquals(0, TrackedMaterial.MITHRIL.enchantedPerBlock());
        assertEquals(0, TrackedMaterial.TITANIUM.enchantedPerBlock());
        assertEquals(0, TrackedMaterial.TUNGSTEN.enchantedPerBlock());

        assertEquals(5, TrackedMaterial.GOLD.baseDrop());
        assertEquals(5, TrackedMaterial.DIAMOND.baseDrop());
        assertEquals(1, TrackedMaterial.MITHRIL.baseDrop());
        assertEquals(2, TrackedMaterial.TITANIUM.baseDrop());
        assertEquals(1, TrackedMaterial.TUNGSTEN.baseDrop());

        assertSame(
                MiningFortuneCategory.ORE,
                TrackedMaterial.GOLD.fortuneType());
        assertSame(
                MiningFortuneCategory.DWARVEN_METAL,
                TrackedMaterial.MITHRIL.fortuneType());
        assertSame(
                MiningFortuneCategory.DWARVEN_METAL,
                TrackedMaterial.TITANIUM.fortuneType());
        assertSame(
                MiningFortuneCategory.DWARVEN_METAL,
                TrackedMaterial.TUNGSTEN.fortuneType());
        assertSame(
                MiningFortuneCategory.BLOCK,
                TrackedMaterial.HARD_STONE.fortuneType());
        assertSame(
                MiningFortuneCategory.BLOCK,
                TrackedMaterial.COBBLESTONE.fortuneType());

        assertSame(
                Items.PRISMARINE_CRYSTALS,
                TrackedMaterial.MITHRIL.iconItem());
        assertSame(
                Items.POLISHED_DIORITE,
                TrackedMaterial.TITANIUM.iconItem());
        assertSame(
                Items.PAPER,
                TrackedMaterial.TUNGSTEN.iconItem());
    }

    @Test
    void blockProfilesExposeCorrectMaterialBaseDrops() {
        assertEquals(
                1,
                TrackedMaterial.GOLD.baseDrop(
                        Blocks.GOLD_ORE.defaultBlockState()));
        assertEquals(
                5,
                TrackedMaterial.GOLD.baseDrop(
                        Blocks.GOLD_BLOCK.defaultBlockState()));
        assertEquals(
                1,
                TrackedMaterial.DIAMOND.baseDrop(
                        Blocks.DIAMOND_ORE.defaultBlockState()));
        assertEquals(
                5,
                TrackedMaterial.DIAMOND.baseDrop(
                        Blocks.DIAMOND_BLOCK.defaultBlockState()));

        assertEquals(
                1,
                TrackedMaterial.MITHRIL.baseDrop(
                        Blocks.WOOL.gray().defaultBlockState()));
        assertEquals(
                1,
                TrackedMaterial.MITHRIL.baseDrop(
                        Blocks.DYED_TERRACOTTA.cyan().defaultBlockState()));
        assertEquals(
                2,
                TrackedMaterial.MITHRIL.baseDrop(
                        Blocks.PRISMARINE.defaultBlockState()));
        assertEquals(
                2,
                TrackedMaterial.MITHRIL.baseDrop(
                        Blocks.PRISMARINE_BRICKS.defaultBlockState()));
        assertEquals(
                2,
                TrackedMaterial.MITHRIL.baseDrop(
                        Blocks.DARK_PRISMARINE.defaultBlockState()));
        assertEquals(
                5,
                TrackedMaterial.MITHRIL.baseDrop(
                        Blocks.WOOL.lightBlue().defaultBlockState()));

        assertEquals(
                2,
                TrackedMaterial.TITANIUM.baseDrop(
                        Blocks.POLISHED_DIORITE.defaultBlockState()));
        assertEquals(
                0,
                TrackedMaterial.TITANIUM.baseDrop(
                        Blocks.STONE.defaultBlockState()));
        assertEquals(
                0,
                TrackedMaterial.TITANIUM.baseDrop(
                        Blocks.COBBLESTONE.defaultBlockState()));

        assertEquals(
                1,
                TrackedMaterial.TUNGSTEN.baseDrop(
                        Blocks.COBBLESTONE.defaultBlockState()));
        assertEquals(
                1,
                TrackedMaterial.TUNGSTEN.baseDrop(
                        Blocks.INFESTED_COBBLESTONE.defaultBlockState()));
        assertEquals(
                1,
                TrackedMaterial.TUNGSTEN.baseDrop(
                        Blocks.COBBLESTONE_SLAB.defaultBlockState()));
        assertEquals(
                1,
                TrackedMaterial.TUNGSTEN.baseDrop(
                        Blocks.COBBLESTONE_STAIRS.defaultBlockState()));
        assertEquals(
                3,
                TrackedMaterial.TUNGSTEN.baseDrop(
                        Blocks.CLAY.defaultBlockState()));
        assertEquals(
                0,
                TrackedMaterial.TUNGSTEN.baseDrop(
                        Blocks.STONE.defaultBlockState()));

        assertEquals(1, TrackedMaterial.UMBER.baseDrop(
                Blocks.TERRACOTTA.defaultBlockState()));
        assertEquals(2, TrackedMaterial.UMBER.baseDrop(
                Blocks.DYED_TERRACOTTA.brown().defaultBlockState()));
        assertEquals(3, TrackedMaterial.UMBER.baseDrop(
                Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState()));
        assertEquals(0, TrackedMaterial.UMBER.baseDrop(
                Blocks.RED_SANDSTONE.defaultBlockState()));

        assertEquals(1, TrackedMaterial.COAL.baseDrop(
                Blocks.COAL_ORE.defaultBlockState()));
        assertEquals(5, TrackedMaterial.COAL.baseDrop(
                Blocks.COAL_BLOCK.defaultBlockState()));
        assertEquals(1, TrackedMaterial.IRON.baseDrop(
                Blocks.IRON_ORE.defaultBlockState()));
        assertEquals(5, TrackedMaterial.IRON.baseDrop(
                Blocks.IRON_BLOCK.defaultBlockState()));
        assertEquals(4, TrackedMaterial.LAPIS.baseDrop(
                Blocks.LAPIS_ORE.defaultBlockState()));
        assertEquals(9, TrackedMaterial.LAPIS.baseDrop(
                Blocks.LAPIS_BLOCK.defaultBlockState()));
        assertEquals(4, TrackedMaterial.REDSTONE.baseDrop(
                Blocks.REDSTONE_ORE.defaultBlockState()));
        assertEquals(9, TrackedMaterial.REDSTONE.baseDrop(
                Blocks.REDSTONE_BLOCK.defaultBlockState()));
        assertEquals(1, TrackedMaterial.EMERALD.baseDrop(
                Blocks.EMERALD_ORE.defaultBlockState()));
        assertEquals(5, TrackedMaterial.EMERALD.baseDrop(
                Blocks.EMERALD_BLOCK.defaultBlockState()));
        assertEquals(1, TrackedMaterial.QUARTZ.baseDrop(
                Blocks.NETHER_QUARTZ_ORE.defaultBlockState()));
        assertEquals(5, TrackedMaterial.QUARTZ.baseDrop(
                Blocks.QUARTZ_BLOCK.defaultBlockState()));
    }

    @Test
    void unknownOrMissingIdsSafelyFallBackToGold() {
        assertSame(
                TrackedMaterial.DIAMOND,
                TrackedMaterial.fromId(" diamond "));
        assertSame(
                TrackedMaterial.MITHRIL,
                TrackedMaterial.fromId("mithril"));
        assertSame(
                TrackedMaterial.TITANIUM,
                TrackedMaterial.fromId(" titanium "));
        assertSame(
                TrackedMaterial.TUNGSTEN,
                TrackedMaterial.fromId(" TUNGSTEN "));

        assertSame(
                TrackedMaterial.GOLD,
                TrackedMaterial.fromId("unknown"));
        assertSame(
                TrackedMaterial.GOLD,
                TrackedMaterial.fromId(null));
    }

    @Test
    void displayNamesResolveToRawEquivalentMultipliers() {
        assertEquals(
                1,
                TrackedMaterial.GOLD.displayMultiplier("Gold Ingot"));
        assertEquals(
                1,
                TrackedMaterial.GOLD.displayMultiplier("Gold Ore"));
        assertEquals(
                160,
                TrackedMaterial.GOLD.displayMultiplier(
                        "Enchanted Gold Ingot"));
        assertEquals(
                25_600,
                TrackedMaterial.GOLD.displayMultiplier(
                        "Enchanted Gold Block"));

        assertEquals(
                1,
                TrackedMaterial.DIAMOND.displayMultiplier("Diamond"));
        assertEquals(
                1,
                TrackedMaterial.DIAMOND.displayMultiplier("Diamond Ore"));
        assertEquals(
                160,
                TrackedMaterial.DIAMOND.displayMultiplier(
                        "Enchanted Diamond"));
        assertEquals(
                25_600,
                TrackedMaterial.DIAMOND.displayMultiplier(
                        "Enchanted Diamond Block"));

        assertEquals(
                1,
                TrackedMaterial.MITHRIL.displayMultiplier("Mithril"));
        assertEquals(
                160,
                TrackedMaterial.MITHRIL.displayMultiplier(
                        "Enchanted Mithril"));

        assertEquals(
                1,
                TrackedMaterial.TITANIUM.displayMultiplier("Titanium"));
        assertEquals(
                160,
                TrackedMaterial.TITANIUM.displayMultiplier(
                        "Enchanted Titanium"));

        assertEquals(
                1,
                TrackedMaterial.TUNGSTEN.displayMultiplier("Tungsten"));
        assertEquals(
                160,
                TrackedMaterial.TUNGSTEN.displayMultiplier(
                        "Enchanted Tungsten"));

        assertEquals(
                0,
                TrackedMaterial.DIAMOND.displayMultiplier(
                        "Enchanted Gold"));
        assertEquals(
                0,
                TrackedMaterial.TITANIUM.displayMultiplier(
                        "Enchanted Tungsten"));
        assertEquals(
                0,
                TrackedMaterial.TUNGSTEN.displayMultiplier(
                        "Enchanted Titanium"));
    }

    @Test
    void compactAndBazaarMatchingStayInsideSelectedMaterial() {
        assertTrue(
                TrackedMaterial.GOLD.matchesCompactReward(
                        "COMPACT! You found an Enchanted Gold Ingot!"));

        assertFalse(
                TrackedMaterial.DIAMOND.matchesCompactReward(
                        "COMPACT! You found an Enchanted Gold Ingot!"));

        assertTrue(
                TrackedMaterial.DIAMOND.matchesCompactReward(
                        "COMPACT! You found an Enchanted Diamond!"));

        assertTrue(
                TrackedMaterial.MITHRIL.matchesCompactReward(
                        "COMPACT! You found an Enchanted Mithril!"));

        assertTrue(
                TrackedMaterial.TITANIUM.matchesCompactReward(
                        "COMPACT! You found an Enchanted Titanium!"));

        assertFalse(
                TrackedMaterial.TUNGSTEN.matchesCompactReward(
                        "COMPACT! You found an Enchanted Titanium!"));

        assertFalse(
                TrackedMaterial.TITANIUM.matchesCompactReward(
                        "COMPACT! You found an Enchanted Tungsten!"));

        assertTrue(
                TrackedMaterial.DIAMOND.matchesSaleItem("Diamond"));
        assertTrue(
                TrackedMaterial.DIAMOND.matchesSaleItem(
                        "Enchanted Diamond"));
        assertFalse(
                TrackedMaterial.DIAMOND.matchesSaleItem(
                        "Enchanted Diamond Block"));

        assertTrue(
                TrackedMaterial.TITANIUM.matchesSaleItem("Titanium"));
        assertTrue(
                TrackedMaterial.TITANIUM.matchesSaleItem(
                        "Enchanted Titanium"));
        assertFalse(
                TrackedMaterial.TITANIUM.matchesSaleItem(
                        "Enchanted Tungsten"));

        assertTrue(
                TrackedMaterial.TUNGSTEN.matchesSaleItem(
                        "Enchanted Tungsten"));
        assertFalse(
                TrackedMaterial.TUNGSTEN.matchesSaleItem(
                        "Enchanted Titanium"));
    }
}
