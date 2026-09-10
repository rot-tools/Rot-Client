package fi.rotclient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

final class MarketWatchItemIconResolver {
    private MarketWatchItemIconResolver() {
    }

    /*
     * Auction item_bytes are not decoded yet, so AH uses a representative
     * category icon. Once item-byte normalization exists this method can be
     * replaced by the exact decoded ItemStack without changing the UI.
     */
    static ItemStack auctionIcon(
            String category,
            String itemName) {

        ItemStack decoded =
                MarketWatchSkyBlockItemDecoder
                        .icon(
                                itemName);

        if (!decoded.isEmpty()) {
            return decoded;
        }

        String normalized =
                normalize(
                        category
                                + " "
                                + itemName);

        if (normalized.contains("armor")
                || normalized.contains("helmet")
                || normalized.contains("chestplate")
                || normalized.contains("leggings")
                || normalized.contains("boots")) {

            return new ItemStack(
                    Items.DIAMOND_CHESTPLATE);
        }

        /*
         * Resolve specific weapon families before the broad AH
         * "weapon" category.
         */
        if (normalized.contains("bow")
                || normalized.contains("juju")
                || normalized.contains("terminator")) {

            return new ItemStack(
                    Items.BOW);
        }

        if (normalized.contains("pickaxe")
                || normalized.contains("drill")) {

            return new ItemStack(
                    Items.DIAMOND_PICKAXE);
        }

        if (normalized.contains("fishing rod")
                || normalized.contains("fishing")) {

            return new ItemStack(
                    Items.FISHING_ROD);
        }

        if (normalized.contains("weapon")
                || normalized.contains("sword")
                || normalized.contains("dagger")
                || normalized.contains("blade")
                || normalized.contains("aspect")) {

            return new ItemStack(
                    Items.DIAMOND_SWORD);
        }

        if (normalized.contains("accessor")
                || normalized.contains("talisman")
                || normalized.contains("artifact")
                || normalized.contains("relic")) {

            return new ItemStack(
                    Items.PLAYER_HEAD);
        }

        if (normalized.contains("book")) {
            return new ItemStack(
                    Items.ENCHANTED_BOOK);
        }

        if (normalized.contains("pet")) {
            return new ItemStack(
                    Items.PLAYER_HEAD);
        }

        if (normalized.contains("consum")
                || normalized.contains("potion")) {

            return new ItemStack(
                    Items.POTION);
        }

        return new ItemStack(
                Items.CHEST);
    }

    static ItemStack bazaarIcon(
            String productId) {

        String id =
                productId == null
                        ? ""
                        : productId
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (id.isBlank()) {
            return new ItemStack(
                    Items.CHEST);
        }

        /*
         * Exact SkyBlock resource icon first:
         * modern Hypixel item_model or custom player-head skin.
         *
         * Loading happens asynchronously. While metadata is still
         * loading, the existing official-material and semantic
         * fallbacks below continue to render normally.
         */
        ItemStack skyBlock =
                MarketWatchSkyBlockResourceIconService
                        .icon(
                                id);

        if (!skyBlock.isEmpty()) {
            return skyBlock;
        }

        String base =
                id;

        while (base.startsWith(
                "ENCHANTED_")) {

            base =
                    base.substring(
                            "ENCHANTED_".length());
        }

        /*
         * First choice: Hypixel's own underlying Minecraft material.
         */
        ItemStack official =
                materialIcon(
                        SkyBlockMarketQuoteService
                                .material(id),
                        id);

        if (!official.isEmpty()) {
            return official;
        }

        ItemStack explicit =
                explicit(base);

        if (!explicit.isEmpty()) {
            return explicit;
        }

        ItemStack registry =
                registryGuess(base);

        if (!registry.isEmpty()) {
            return registry;
        }

        ItemStack semantic =
                semanticFallback(base);

        if (!semantic.isEmpty()) {
            return semantic;
        }

        /*
         * Final generic fallback. PAPER is deliberately not used.
         */
        return new ItemStack(
                Items.CHEST);
    }

    static ItemStack materialIcon(
            String material,
            String productId) {

        String value =
                material == null
                        ? ""
                        : material
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (value.isBlank()) {
            return ItemStack.EMPTY;
        }

        ItemStack direct =
                registryGuess(value);

        if (!direct.isEmpty()) {
            return direct;
        }

        return switch (value) {
            case "SKULL", "SKULL_ITEM" ->
                    new ItemStack(
                            Items.PLAYER_HEAD);

            case "SULPHUR" ->
                    new ItemStack(
                            Items.GUNPOWDER);

            case "NETHER_STALK" ->
                    new ItemStack(
                            Items.NETHER_WART);

            case "CARROT_ITEM" ->
                    new ItemStack(
                            Items.CARROT);

            case "POTATO_ITEM" ->
                    new ItemStack(
                            Items.POTATO);

            case "EXP_BOTTLE" ->
                    new ItemStack(
                            Items.EXPERIENCE_BOTTLE);

            case "SNOW_BALL" ->
                    new ItemStack(
                            Items.SNOWBALL);

            case "WATER_LILY" ->
                    new ItemStack(
                            Items.LILY_PAD);

            case "NETHER_BRICK_ITEM" ->
                    new ItemStack(
                            Items.NETHER_BRICK);

            case "FIREWORK_CHARGE" ->
                    new ItemStack(
                            Items.FIREWORK_STAR);

            case "SPECKLED_MELON" ->
                    new ItemStack(
                            Items.GLISTERING_MELON_SLICE);

            case "RED_ROSE" ->
                    new ItemStack(
                            Items.POPPY);

            case "YELLOW_FLOWER" ->
                    new ItemStack(
                            Items.DANDELION);

            case "WOOD" ->
                    new ItemStack(
                            Items.OAK_PLANKS);

            case "LOG" ->
                    new ItemStack(
                            Items.OAK_LOG);

            case "LOG_2" ->
                    new ItemStack(
                            Items.ACACIA_LOG);

            case "RAW_FISH" ->
                    fishIcon(
                            productId,
                            false);

            case "COOKED_FISH" ->
                    fishIcon(
                            productId,
                            true);

            case "INK_SACK" ->
                    dyeIcon(
                            productId);

            default ->
                    ItemStack.EMPTY;
        };
    }

    private static ItemStack fishIcon(
            String productId,
            boolean cooked) {

        String id =
                productId == null
                        ? ""
                        : productId
                        .toUpperCase(Locale.ROOT);

        if (id.contains("SALMON")) {
            return new ItemStack(
                    cooked
                            ? Items.COOKED_SALMON
                            : Items.SALMON);
        }

        if (!cooked
                && id.contains("PUFFER")) {

            return new ItemStack(
                    Items.PUFFERFISH);
        }

        if (!cooked
                && id.contains("CLOWNFISH")) {

            return new ItemStack(
                    Items.TROPICAL_FISH);
        }

        return new ItemStack(
                cooked
                        ? Items.COOKED_COD
                        : Items.COD);
    }

    private static ItemStack dyeIcon(
            String productId) {

        String id =
                productId == null
                        ? ""
                        : productId
                        .toUpperCase(Locale.ROOT);

        if (id.contains("LAPIS")) {
            return new ItemStack(
                    Items.LAPIS_LAZULI);
        }

        if (id.contains("COCOA")) {
            return new ItemStack(
                    Items.COCOA_BEANS);
        }

        return new ItemStack(
                Items.INK_SAC);
    }

    private static ItemStack semanticFallback(
            String id) {

        if (id == null
                || id.isBlank()) {

            return ItemStack.EMPTY;
        }

        if (contains(id, "ESSENCE")) {
            return new ItemStack(
                    Items.NETHER_STAR);
        }

        if (contains(id, "FRAGMENT")
                || contains(id, "SHARD")
                || contains(id, "GEM")) {

            return new ItemStack(
                    Items.AMETHYST_SHARD);
        }

        if (contains(id, "DUST")
                || contains(id, "POWDER")) {

            return new ItemStack(
                    Items.GLOWSTONE_DUST);
        }

        if (contains(id, "BOOK")) {
            return new ItemStack(
                    Items.ENCHANTED_BOOK);
        }

        if (contains(id, "COOKIE")) {
            return new ItemStack(
                    Items.COOKIE);
        }

        if (contains(id, "POTION")) {
            return new ItemStack(
                    Items.POTION);
        }

        if (contains(id, "EGG")) {
            return new ItemStack(
                    Items.EGG);
        }

        if (contains(id, "FISH")) {
            return new ItemStack(
                    Items.COD);
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack explicit(
            String id) {

        if (contains(id, "DIAMOND")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.DIAMOND_BLOCK
                            : Items.DIAMOND);
        }

        if (contains(id, "EMERALD")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.EMERALD_BLOCK
                            : Items.EMERALD);
        }

        if (contains(id, "GOLD")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.GOLD_BLOCK
                            : Items.GOLD_INGOT);
        }

        if (contains(id, "IRON")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.IRON_BLOCK
                            : Items.IRON_INGOT);
        }

        if (contains(id, "COAL")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.COAL_BLOCK
                            : Items.COAL);
        }

        if (contains(id, "REDSTONE")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.REDSTONE_BLOCK
                            : Items.REDSTONE);
        }

        if (contains(id, "LAPIS")) {
            return new ItemStack(
                    id.contains("BLOCK")
                            ? Items.LAPIS_BLOCK
                            : Items.LAPIS_LAZULI);
        }

        if (contains(id, "QUARTZ")) {
            return new ItemStack(
                    Items.QUARTZ);
        }

        if (contains(id, "OBSIDIAN")) {
            return new ItemStack(
                    Items.OBSIDIAN);
        }

        if (contains(id, "COBBLESTONE")) {
            return new ItemStack(
                    Items.COBBLESTONE);
        }

        if (contains(id, "GLOWSTONE")) {
            return new ItemStack(
                    Items.GLOWSTONE_DUST);
        }

        if (contains(id, "ENDER_PEARL")) {
            return new ItemStack(
                    Items.ENDER_PEARL);
        }

        if (contains(id, "BLAZE")) {
            return new ItemStack(
                    Items.BLAZE_ROD);
        }

        if (contains(id, "MAGMA")) {
            return new ItemStack(
                    Items.MAGMA_CREAM);
        }

        if (contains(id, "SLIME")) {
            return new ItemStack(
                    Items.SLIME_BALL);
        }

        if (contains(id, "ROTTEN_FLESH")) {
            return new ItemStack(
                    Items.ROTTEN_FLESH);
        }

        if (contains(id, "BONE")) {
            return new ItemStack(
                    Items.BONE);
        }

        if (contains(id, "STRING")) {
            return new ItemStack(
                    Items.STRING);
        }

        if (contains(id, "SPIDER_EYE")) {
            return new ItemStack(
                    Items.SPIDER_EYE);
        }

        if (contains(id, "GHAST")) {
            return new ItemStack(
                    Items.GHAST_TEAR);
        }

        if (contains(id, "WHEAT")) {
            return new ItemStack(
                    Items.WHEAT);
        }

        if (contains(id, "CARROT")) {
            return new ItemStack(
                    Items.CARROT);
        }

        if (contains(id, "POTATO")) {
            return new ItemStack(
                    Items.POTATO);
        }

        if (contains(id, "MELON")) {
            return new ItemStack(
                    Items.MELON_SLICE);
        }

        if (contains(id, "PUMPKIN")) {
            return new ItemStack(
                    Items.PUMPKIN);
        }

        if (contains(id, "CACTUS")) {
            return new ItemStack(
                    Items.CACTUS);
        }

        if (contains(id, "SUGAR_CANE")) {
            return new ItemStack(
                    Items.SUGAR_CANE);
        }

        if (contains(id, "LEATHER")) {
            return new ItemStack(
                    Items.LEATHER);
        }

        if (contains(id, "FEATHER")) {
            return new ItemStack(
                    Items.FEATHER);
        }

        if (contains(id, "RABBIT")) {
            return new ItemStack(
                    Items.RABBIT);
        }

        if (contains(id, "CHICKEN")) {
            return new ItemStack(
                    Items.CHICKEN);
        }

        if (contains(id, "MUTTON")) {
            return new ItemStack(
                    Items.MUTTON);
        }

        if (contains(id, "PORK")) {
            return new ItemStack(
                    Items.PORKCHOP);
        }

        if (contains(id, "BEEF")) {
            return new ItemStack(
                    Items.BEEF);
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack registryGuess(
            String id) {

        String path =
                id.toLowerCase(Locale.ROOT);

        if (path.startsWith("minecraft:")) {
            path =
                    path.substring(
                            "minecraft:".length());
        }

        Identifier identifier =
                Identifier.tryParse(
                        "minecraft:"
                                + path);

        if (identifier == null) {
            return ItemStack.EMPTY;
        }

        Item item =
                BuiltInRegistries.ITEM
                        .getValue(identifier);

        if (item == null
                || item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    private static boolean contains(
            String value,
            String part) {

        return value != null
                && value.contains(part);
    }

    private static String normalize(
            String value) {

        return value == null
                ? ""
                : value
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');
    }
}