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

        if (normalized.contains("weapon")
                || normalized.contains("sword")
                || normalized.contains("dagger")
                || normalized.contains("blade")) {

            return new ItemStack(
                    Items.DIAMOND_SWORD);
        }

        if (normalized.contains("bow")) {
            return new ItemStack(
                    Items.BOW);
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
                    Items.PAPER);
        }

        String base =
                id;

        while (base.startsWith(
                "ENCHANTED_")) {

            base =
                    base.substring(
                            "ENCHANTED_".length());
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

        return new ItemStack(
                Items.PAPER);
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