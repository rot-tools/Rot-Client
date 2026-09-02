package fi.rotclient;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Everything that varies between otherwise identical mining targets.
 *
 * <p>The tracker intentionally uses Hypixel display names instead of only the
 * vanilla item id. Enchanted items share vanilla base items with their raw
 * counterparts, while the display name remains stable in inventory, Sack and
 * Bazaar observations.</p>
 */
enum TrackedMaterial {
    COAL(
            "COAL",
            "Coal",
            "Pure Coal",
            "Coal",
            "Coal",
            "Enchanted Coal",
            "Enchanted Coal Block",
            "COAL",
            "ENCHANTED_COAL",
            () -> Items.COAL,
            160,
            160,
            5,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.COAL_ORE, 1),
                    block(() -> Blocks.COAL_BLOCK, 5)),
            new String[]{"Coal", "Coal Ore"},
            new String[]{"Enchanted Coal"},
            new String[]{"Enchanted Coal Block"}),
    IRON(
            "IRON",
            "Iron",
            "Pure Iron",
            "Iron Ingot",
            "Iron Ingots",
            "Enchanted Iron",
            "Enchanted Iron Block",
            "IRON_INGOT",
            "ENCHANTED_IRON",
            () -> Items.IRON_INGOT,
            160,
            160,
            5,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.IRON_ORE, 1),
                    block(() -> Blocks.IRON_BLOCK, 5)),
            new String[]{"Iron", "Iron Ingot", "Iron Ore"},
            new String[]{"Enchanted Iron"},
            new String[]{"Enchanted Iron Block"}),
    GOLD(
            "GOLD",
            "Gold",
            "Pure Gold",
            "Gold Ingot",
            "Gold Ingots",
            "Enchanted Gold",
            "Enchanted Gold Block",
            "GOLD_INGOT",
            "ENCHANTED_GOLD",
            () -> Items.GOLD_INGOT,
            160,
            160,
            5,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.GOLD_ORE, 1),
                    block(() -> Blocks.GOLD_BLOCK, 5)),
            new String[]{"Gold Ingot", "Gold Ore"},
            new String[]{"Enchanted Gold", "Enchanted Gold Ingot"},
            new String[]{"Enchanted Gold Block"}),
    LAPIS(
            "LAPIS",
            "Lapis",
            "Pure Lapis",
            "Lapis Lazuli",
            "Lapis Lazuli",
            "Enchanted Lapis Lazuli",
            "Enchanted Lapis Lazuli Block",
            "INK_SACK:4",
            "ENCHANTED_LAPIS_LAZULI",
            () -> Items.LAPIS_LAZULI,
            160,
            160,
            9,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.LAPIS_ORE, 4),
                    block(() -> Blocks.LAPIS_BLOCK, 9)),
            new String[]{"Lapis", "Lapis Lazuli", "Lapis Lazuli Ore"},
            new String[]{"Enchanted Lapis Lazuli"},
            new String[]{"Enchanted Lapis Lazuli Block"}),
    REDSTONE(
            "REDSTONE",
            "Redstone",
            "Pure Redstone",
            "Redstone",
            "Redstone",
            "Enchanted Redstone",
            "Enchanted Redstone Block",
            "REDSTONE",
            "ENCHANTED_REDSTONE",
            () -> Items.REDSTONE,
            160,
            160,
            9,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.REDSTONE_ORE, 4),
                    block(() -> Blocks.REDSTONE_BLOCK, 9)),
            new String[]{"Redstone", "Redstone Dust", "Redstone Ore"},
            new String[]{"Enchanted Redstone"},
            new String[]{"Enchanted Redstone Block"}),
    EMERALD(
            "EMERALD",
            "Emerald",
            "Pure Emerald",
            "Emerald",
            "Emeralds",
            "Enchanted Emerald",
            "Enchanted Emerald Block",
            "EMERALD",
            "ENCHANTED_EMERALD",
            () -> Items.EMERALD,
            160,
            160,
            5,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.EMERALD_ORE, 1),
                    block(() -> Blocks.EMERALD_BLOCK, 5)),
            new String[]{"Emerald", "Emerald Ore"},
            new String[]{"Enchanted Emerald"},
            new String[]{"Enchanted Emerald Block"}),
    DIAMOND(
            "DIAMOND",
            "Diamond",
            "Pure Diamond",
            "Diamond",
            "Diamonds",
            "Enchanted Diamond",
            "Enchanted Diamond Block",
            "DIAMOND",
            "ENCHANTED_DIAMOND",
            () -> Items.DIAMOND,
            160,
            160,
            5,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.DIAMOND_ORE, 1),
                    block(() -> Blocks.DIAMOND_BLOCK, 5)),
            new String[]{"Diamond", "Diamond Ore"},
            new String[]{"Enchanted Diamond"},
            new String[]{"Enchanted Diamond Block"}),
    QUARTZ(
            "QUARTZ",
            "Quartz",
            "Pure Quartz",
            "Nether Quartz",
            "Nether Quartz",
            "Enchanted Quartz",
            "Enchanted Quartz Block",
            "QUARTZ",
            "ENCHANTED_QUARTZ",
            () -> Items.QUARTZ,
            160,
            160,
            5,
            MiningFortuneCategory.ORE,
            List.of(
                    block(() -> Blocks.NETHER_QUARTZ_ORE, 1),
                    block(() -> Blocks.QUARTZ_BLOCK, 5)),
            new String[]{"Quartz", "Nether Quartz", "Nether Quartz Ore"},
            new String[]{"Enchanted Quartz"},
            new String[]{"Enchanted Quartz Block"}),
    MITHRIL(
            "MITHRIL",
            "Mithril",
            "Mithril",
            "Mithril",
            "Mithril",
            "Enchanted Mithril",
            null,
            "MITHRIL_ORE",
            "ENCHANTED_MITHRIL",
            () -> Items.PRISMARINE_CRYSTALS,
            160,
            0,
            1,
            MiningFortuneCategory.DWARVEN_METAL,
            List.of(
                    block(() -> Blocks.WOOL.gray(), 1),
                    block(() -> Blocks.DYED_TERRACOTTA.cyan(), 1),
                    block(() -> Blocks.PRISMARINE, 2),
                    block(() -> Blocks.PRISMARINE_BRICKS, 2),
                    block(() -> Blocks.DARK_PRISMARINE, 2),
                    block(() -> Blocks.WOOL.lightBlue(), 5)),
            new String[]{"Mithril"},
            new String[]{"Enchanted Mithril"},
            new String[0]),
    TITANIUM(
            "TITANIUM",
            "Titanium",
            "Titanium",
            "Titanium",
            "Titanium",
            "Enchanted Titanium",
            null,
            "TITANIUM_ORE",
            "ENCHANTED_TITANIUM",
            () -> Items.POLISHED_DIORITE,
            160,
            0,
            2,
            MiningFortuneCategory.DWARVEN_METAL,
            List.of(
                    block(() -> Blocks.POLISHED_DIORITE, 2)),
            new String[]{"Titanium"},
            new String[]{"Enchanted Titanium"},
            new String[0]),
    HARD_STONE(
            "HARD_STONE",
            "Hard Stone",
            "Hard Stone",
            "Hard Stone",
            "Hard Stone",
            "Enchanted Hard Stone",
            null,
            "HARD_STONE",
            "ENCHANTED_HARD_STONE",
            () -> Items.STONE,
            576,
            0,
            1,
            MiningFortuneCategory.BLOCK,
            List.of(
                    block(() -> Blocks.STONE, 1)),
            new String[]{"Hard Stone"},
            new String[]{"Enchanted Hard Stone"},
            new String[0]),
    TUNGSTEN(
            "TUNGSTEN",
            "Tungsten",
            "Tungsten",
            "Tungsten",
            "Tungsten",
            "Enchanted Tungsten",
            null,
            "TUNGSTEN",
            "ENCHANTED_TUNGSTEN",
            () -> Items.PAPER,
            160,
            0,
            1,
            MiningFortuneCategory.DWARVEN_METAL,
            List.of(
                    block(() -> Blocks.INFESTED_COBBLESTONE, 1),
                    block(() -> Blocks.COBBLESTONE, 1),
                    block(() -> Blocks.COBBLESTONE_SLAB, 1),
                    block(() -> Blocks.COBBLESTONE_STAIRS, 1),
                    block(() -> Blocks.CLAY, 3)),
            new String[]{"Tungsten"},
            new String[]{"Enchanted Tungsten"},
            new String[0]),
    UMBER(
            "UMBER",
            "Umber",
            "Umber",
            "Umber",
            "Umber",
            "Enchanted Umber",
            null,
            "UMBER",
            "ENCHANTED_UMBER",
            () -> Items.PAPER,
            160,
            0,
            1,
            MiningFortuneCategory.DWARVEN_METAL,
            List.of(
                    block(() -> Blocks.TERRACOTTA, 1),
                    block(() -> Blocks.DYED_TERRACOTTA.brown(), 2),
                    block(() -> Blocks.SMOOTH_RED_SANDSTONE, 3)),
            new String[]{"Umber"},
            new String[]{"Enchanted Umber"},
            new String[0]),
    /*
     * Common non-target filler block encountered while mining in Dwarven
     * Mines / Crystal Hollows corridors. Not a selectable dashboard target,
     * but tracked so its Mining Sack and direct-inventory pickups can be
     * correlated with confirmed breaks and credited to Current Session as
     * OTHER_MINED, matching the existing Hard Stone integration.
     *
     * Shares the vanilla Blocks.COBBLESTONE identity with TUNGSTEN's
     * retextured ore block. Selected-target routing resolves that overlap:
     * Tungsten credits only while Tungsten is selected, while ordinary
     * Cobblestone remains an off-target observation elsewhere.
     */
    COBBLESTONE(
            "COBBLESTONE",
            "Cobblestone",
            "Cobblestone",
            "Cobblestone",
            "Cobblestone",
            "Enchanted Cobblestone",
            null,
            "COBBLESTONE",
            "ENCHANTED_COBBLESTONE",
            () -> Items.COBBLESTONE,
            160,
            0,
            1,
            MiningFortuneCategory.BLOCK,
            List.of(
                    block(() -> Blocks.COBBLESTONE, 1)),
            new String[]{"Cobblestone"},
            new String[]{"Enchanted Cobblestone"},
            new String[0]);

    enum ItemTier {
        RAW,
        ENCHANTED,
        ENCHANTED_BLOCK,
        NONE
    }

    private record BlockProfile(Supplier<Block> block, int baseDrop) {
    }

    private final String id;
    private final String displayName;
    private final String pureDisplayName;
    private final String rawItemName;
    private final String rawMetricLabel;
    private final String enchantedItemName;
    private final String enchantedBlockItemName;
    private final String rawBazaarId;
    private final String enchantedBazaarId;
    private final Supplier<Item> iconItem;
    private final int rawPerEnchanted;
    private final int enchantedPerBlock;
    private final int baseDrop;
    private final MiningFortuneCategory fortuneType;
    private final List<BlockProfile> blockProfiles;
    private final String[] rawNames;
    private final String[] enchantedNames;
    private final String[] enchantedBlockNames;

    TrackedMaterial(
            String id,
            String displayName,
            String pureDisplayName,
            String rawItemName,
            String rawMetricLabel,
            String enchantedItemName,
            String enchantedBlockItemName,
            String rawBazaarId,
            String enchantedBazaarId,
            Supplier<Item> iconItem,
            int rawPerEnchanted,
            int enchantedPerBlock,
            int baseDrop,
            MiningFortuneCategory fortuneType,
            List<BlockProfile> blockProfiles,
            String[] rawNames,
            String[] enchantedNames,
            String[] enchantedBlockNames) {
        this.id = id;
        this.displayName = displayName;
        this.pureDisplayName = pureDisplayName;
        this.rawItemName = rawItemName;
        this.rawMetricLabel = rawMetricLabel;
        this.enchantedItemName = enchantedItemName;
        this.enchantedBlockItemName = enchantedBlockItemName;
        this.rawBazaarId = rawBazaarId;
        this.enchantedBazaarId = enchantedBazaarId;
        this.iconItem = iconItem;
        this.rawPerEnchanted = rawPerEnchanted;
        this.enchantedPerBlock = enchantedPerBlock;
        this.baseDrop = baseDrop;
        this.fortuneType = fortuneType;
        this.blockProfiles = List.copyOf(blockProfiles);
        this.rawNames = rawNames;
        this.enchantedNames = enchantedNames;
        this.enchantedBlockNames = enchantedBlockNames;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    String pureDisplayName() {
        return pureDisplayName;
    }

    String rawItemName() {
        return rawItemName;
    }

    String rawMetricLabel() {
        return rawMetricLabel;
    }

    String enchantedItemName() {
        return enchantedItemName;
    }

    String enchantedBlockItemName() {
        return enchantedBlockItemName;
    }

    String enchantedBlockBazaarId() {
        if (enchantedBlockItemName == null) return null;
        return this == LAPIS
                ? "ENCHANTED_LAPIS_LAZULI_BLOCK"
                : "ENCHANTED_" + id + "_BLOCK";
    }

    String rawBazaarId() {
        return rawBazaarId;
    }

    String enchantedBazaarId() {
        return enchantedBazaarId;
    }

    Item iconItem() {
        return iconItem.get();
    }

    int rawPerEnchanted() {
        return rawPerEnchanted;
    }

    int enchantedPerBlock() {
        return enchantedPerBlock;
    }

    int baseDrop() {
        return baseDrop;
    }

    MiningFortuneCategory fortuneType() {
        return fortuneType;
    }

    boolean isTrackedBlock(BlockState state) {
        return baseDrop(state) > 0;
    }

    int baseDrop(BlockState state) {
        if (state == null) return 0;
        for (BlockProfile profile : blockProfiles) {
            if (state.is(profile.block().get())) return profile.baseDrop();
        }
        return 0;
    }

    static TrackedMaterial fromId(String id) {
        if (id == null) return GOLD;
        String normalized = id.trim();
        for (TrackedMaterial material : values()) {
            if (material.id.equalsIgnoreCase(normalized)) return material;
        }
        return GOLD;
    }

    long rawItemCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return matchesRawName(stack.getHoverName().getString())
                ? stack.getCount()
                : 0;
    }

    long rawEquivalent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        long multiplier = displayMultiplier(stack.getHoverName().getString());
        return multiplier * stack.getCount();
    }

    long displayMultiplier(String displayName) {
        return switch (itemTier(displayName)) {
            case RAW -> 1L;
            case ENCHANTED -> rawPerEnchanted;
            case ENCHANTED_BLOCK -> (long) rawPerEnchanted * enchantedPerBlock;
            case NONE -> 0L;
        };
    }

    boolean matchesRawName(String displayName) {
        return matchesAny(displayName, rawNames);
    }

    Set<String> exactItemNames(ItemTier tier) {
        String[] names = switch (tier) {
            case RAW -> rawNames;
            case ENCHANTED -> enchantedNames;
            case ENCHANTED_BLOCK -> enchantedBlockNames;
            case NONE -> new String[0];
        };
        return Set.copyOf(Arrays.asList(names));
    }

    boolean matchesEnchantedName(String displayName) {
        return matchesAny(displayName, enchantedNames);
    }

    boolean matchesEnchantedBlockName(String displayName) {
        return matchesAny(displayName, enchantedBlockNames);
    }

    boolean matchesCompactReward(String messageOrItemName) {
        if (matchesEnchantedName(messageOrItemName)) return true;
        String normalized = normalize(messageOrItemName);
        if (!normalized.startsWith("compact!")) return false;
        return Arrays.stream(enchantedNames)
                .map(TrackedMaterial::normalize)
                .anyMatch(normalized::contains);
    }

    boolean matchesSaleItem(String displayName) {
        ItemTier tier = saleItemTier(displayName);
        return tier == ItemTier.RAW || tier == ItemTier.ENCHANTED;
    }

    ItemTier saleItemTier(String displayName) {
        ItemTier tier = itemTier(displayName);
        return tier == ItemTier.ENCHANTED_BLOCK ? ItemTier.NONE : tier;
    }

    ItemTier itemTier(String displayName) {
        if (matchesRawName(displayName)) return ItemTier.RAW;
        if (matchesEnchantedName(displayName)) return ItemTier.ENCHANTED;
        if (matchesEnchantedBlockName(displayName)) return ItemTier.ENCHANTED_BLOCK;
        return ItemTier.NONE;
    }

    private static boolean matchesAny(String displayName, String[] candidates) {
        String normalized = normalize(displayName);
        if (normalized.isEmpty()) return false;
        for (String candidate : candidates) {
            if (normalized.equals(normalize(candidate))) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static BlockProfile block(
            Supplier<Block> block, int baseDrop) {
        return new BlockProfile(block, baseDrop);
    }
}
