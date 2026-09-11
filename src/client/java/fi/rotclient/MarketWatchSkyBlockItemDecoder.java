package fi.rotclient;

import com.google.common.collect.ImmutableListMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class MarketWatchSkyBlockItemDecoder {

    private static final int MAX_COMPRESSED_BYTES =
            4 * 1024 * 1024;

    private static final long MAX_NBT_BYTES =
            16L * 1024L * 1024L;

    private static final int CACHE_SIZE =
            384;

    private static final Map<String, ItemStack> CACHE =
            new LinkedHashMap<>(
                    64,
                    0.75F,
                    true) {

                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, ItemStack> eldest) {

                    return size() > CACHE_SIZE;
                }
            };

    /*
     * Quantity cache used by the Opportunity Scanner.
     *
     * Hypixel's starting_bid represents the complete BIN listing, not
     * necessarily one item. Stackable AH listings therefore need their
     * serialized ItemStack count for correct per-unit valuation.
     */
    private static final int STACK_COUNT_CACHE_SIZE =
            65_536;

    private static final Map<String, Integer> STACK_COUNT_CACHE =
            new LinkedHashMap<>(
                    1024,
                    0.75F,
                    true) {

                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, Integer> eldest) {

                    return size()
                            > STACK_COUNT_CACHE_SIZE;
                }
            };

    private MarketWatchSkyBlockItemDecoder() {
    }

    static int stackCount(
            MarketWatchAuction auction) {

        if (auction == null
                || auction.itemBytes().isBlank()) {

            return 1;
        }

        String key =
                !auction.uuid().isBlank()
                        ? auction.uuid()
                        : auction.itemBytes().length()
                        + ":"
                        + auction.itemBytes().hashCode();

        synchronized (STACK_COUNT_CACHE) {
            Integer cached =
                    STACK_COUNT_CACHE.get(
                            key);

            if (cached != null) {
                return cached;
            }
        }

        int count =
                decodeStackCount(
                        auction.itemBytes());

        synchronized (STACK_COUNT_CACHE) {
            STACK_COUNT_CACHE.put(
                    key,
                    count);
        }

        return count;
    }

    private static int decodeStackCount(
            String encoded) {

        String data =
                base64Data(
                        encoded);

        if (data.isBlank()) {
            return 1;
        }

        byte[] bytes;

        try {
            bytes =
                    Base64.getDecoder()
                            .decode(
                                    data);

        } catch (IllegalArgumentException ignored) {

            try {
                bytes =
                        Base64.getMimeDecoder()
                                .decode(
                                        data);

            } catch (IllegalArgumentException ignoredAgain) {
                return 1;
            }
        }

        if (bytes.length == 0
                || bytes.length
                > MAX_COMPRESSED_BYTES) {

            return 1;
        }

        CompoundTag root;

        try (ByteArrayInputStream input =
                     new ByteArrayInputStream(
                             bytes)) {

            root =
                    NbtIo.readCompressed(
                            input,
                            NbtAccounter.create(
                                    MAX_NBT_BYTES));

        } catch (Exception ignored) {
            return 1;
        }

        if (root == null
                || root.isEmpty()) {

            return 1;
        }

        CompoundTag item =
                firstItemTag(
                        root);

        int count =
                numericStackCount(
                        item);

        if (count <= 0
                && item != root) {

            count =
                    numericStackCount(
                            root);
        }

        if (count <= 0) {
            return 1;
        }

        return Math.max(
                1,
                Math.min(
                        64,
                        count));
    }

    private static int numericStackCount(
            CompoundTag tag) {

        if (tag == null
                || tag.isEmpty()) {

            return 0;
        }

        int modern =
                tag.getInt(
                        "count")
                        .orElse(
                                0);

        if (modern > 0) {
            return modern;
        }

        return tag.getInt(
                "Count")
                .orElse(
                        0);
    }

    static ItemStack icon(
            String itemName) {

        if (itemName == null
                || itemName.isBlank()) {

            return ItemStack.EMPTY;
        }

        MarketWatchAuction auction =
                representativeAuction(
                        itemName);

        if (auction == null
                || auction.itemBytes().isBlank()) {

            return ItemStack.EMPTY;
        }

        String cacheKey =
                auction.uuid().isBlank()
                        ? auction.itemName()
                        + ":"
                        + auction.itemBytes().length()
                        + ":"
                        + auction.itemBytes().hashCode()
                        : auction.uuid();

        synchronized (CACHE) {
            ItemStack cached =
                    CACHE.get(
                            cacheKey);

            if (cached != null
                    && !cached.isEmpty()) {

                return cached.copy();
            }
        }

        ItemStack decoded;

        try {
            decoded =
                    decode(
                            auction.itemBytes(),
                            auction.itemName());
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }

        if (decoded == null
                || decoded.isEmpty()) {

            return ItemStack.EMPTY;
        }

        synchronized (CACHE) {
            CACHE.put(
                    cacheKey,
                    decoded.copy());
        }

        return decoded;
    }

    private static MarketWatchAuction representativeAuction(
            String itemName) {

        MarketWatchDataService.AuctionState state =
                MarketWatchDataService
                        .currentAuctions();

        if (state == null
                || !state.available()
                || state.snapshot() == null) {

            return null;
        }

        MarketWatchAuction best =
                null;

        for (MarketWatchAuction auction
                : state.snapshot().auctions()) {

            if (auction == null
                    || !auction.bin()
                    || auction.itemBytes().isBlank()
                    || !itemName.equalsIgnoreCase(
                            auction.itemName())) {

                continue;
            }

            if (best == null
                    || positivePrice(
                            auction)
                    < positivePrice(
                            best)) {

                best =
                        auction;
            }
        }

        return best;
    }

    private static long positivePrice(
            MarketWatchAuction auction) {

        long price =
                auction == null
                        ? 0L
                        : auction.startingBid();

        return price <= 0L
                ? Long.MAX_VALUE
                : price;
    }

    private static ItemStack decode(
            String encoded,
            String itemName) {

        String data =
                base64Data(
                        encoded);

        if (data.isBlank()) {
            return ItemStack.EMPTY;
        }

        byte[] bytes;

        try {
            bytes =
                    Base64.getDecoder()
                            .decode(
                                    data);
        } catch (IllegalArgumentException ignored) {

            try {
                bytes =
                        Base64.getMimeDecoder()
                                .decode(
                                        data);
            } catch (IllegalArgumentException ignoredAgain) {
                return ItemStack.EMPTY;
            }
        }

        if (bytes.length == 0
                || bytes.length > MAX_COMPRESSED_BYTES) {

            return ItemStack.EMPTY;
        }

        CompoundTag root;

        try (ByteArrayInputStream input =
                     new ByteArrayInputStream(
                             bytes)) {

            root =
                    NbtIo.readCompressed(
                            input,
                            NbtAccounter.create(
                                    MAX_NBT_BYTES));

        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }

        if (root == null
                || root.isEmpty()) {

            return ItemStack.EMPTY;
        }

        CompoundTag itemTag =
                firstItemTag(
                        root);

        /*
         * New-format API data can contain the actual modern ItemStack
         * components. If so, this keeps minecraft:item_model, profile,
         * custom data and every other render component intact.
         */
        ItemStack modern =
                decodeModern(
                        itemTag);

        if (modern.isEmpty()
                && itemTag != root) {

            modern =
                    decodeModern(
                            root);
        }

        if (!modern.isEmpty()) {
            return modern;
        }

        /*
         * Legacy Hypixel item_bytes fallback.
         */
        return decodeLegacy(
                itemTag,
                itemName);
    }

    private static CompoundTag firstItemTag(
            CompoundTag root) {

        if (root == null) {
            return null;
        }

        ListTag items =
                root.getList("i")
                        .orElse(null);

        if (items != null
                && !items.isEmpty()) {

            CompoundTag first =
                    items.getCompound(0)
                            .orElse(null);

            if (first != null) {
                return first;
            }
        }

        CompoundTag item =
                root.getCompound("item")
                        .orElse(null);

        return item == null
                ? root
                : item;
    }

    private static ItemStack decodeModern(
            CompoundTag tag) {

        if (tag == null
                || tag.isEmpty()) {

            return ItemStack.EMPTY;
        }

        HolderLookup.Provider registries =
                registries();

        if (registries == null) {
            return ItemStack.EMPTY;
        }

        try {
            return ItemStack.CODEC
                    .parse(
                            RegistryOps.create(
                                    NbtOps.INSTANCE,
                                    registries),
                            tag)
                    .result()
                    .orElse(
                            ItemStack.EMPTY);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack decodeLegacy(
            CompoundTag item,
            String itemName) {

        if (item == null
                || item.isEmpty()) {

            return ItemStack.EMPTY;
        }

        CompoundTag legacyTag =
                item.getCompound("tag")
                        .orElse(null);

        if (legacyTag == null) {
            legacyTag =
                    item.getCompound("components")
                            .orElse(null);
        }

        if (legacyTag == null) {
            legacyTag =
                    item;
        }

        String skyBlockId =
                skyBlockId(
                        legacyTag);

        String material =
                SkyBlockMarketQuoteService
                        .material(
                                skyBlockId);

        ItemStack stack =
                MarketWatchItemIconResolver
                        .materialIcon(
                                material,
                                skyBlockId);

        String texture =
                textureValue(
                        legacyTag,
                        0);

        String model =
                itemModel(
                        item,
                        0);

        if (model.isBlank()) {
            model =
                    itemModel(
                            legacyTag,
                            0);
        }

        /*
         * A skull texture alone is enough to reconstruct the correct
         * player-head render even if the item resource material has not
         * finished loading yet.
         */
        if (!texture.isBlank()
                && (stack.isEmpty()
                || stack.getItem()
                == Items.PLAYER_HEAD)) {

            if (stack.isEmpty()) {
                stack =
                        new ItemStack(
                                Items.PLAYER_HEAD);
            }

            applyProfile(
                    stack,
                    texture);
        }

        /*
         * An explicit item_model completely identifies the resource-pack
         * model. PAPER is safe as a carrier if the old material is missing.
         */
        if (!model.isBlank()) {

            Identifier modelId =
                    Identifier.tryParse(
                            model);

            if (modelId != null) {

                if (stack.isEmpty()) {
                    stack =
                            new ItemStack(
                                    Items.PAPER);
                }

                stack.set(
                        DataComponents.ITEM_MODEL,
                        modelId);
            }
        }

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (legacyTag != null
                && !legacyTag.isEmpty()) {

            stack.set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.of(
                            legacyTag.copy()));
        }

        if (itemName != null
                && !itemName.isBlank()) {

            stack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(
                            itemName));
        }

        /*
         * Apply the texture after CUSTOM_DATA so PROFILE remains the
         * authoritative modern head component.
         */
        if (!texture.isBlank()
                && stack.getItem()
                == Items.PLAYER_HEAD) {

            applyProfile(
                    stack,
                    texture);
        }

        return stack;
    }

    private static String skyBlockId(
            CompoundTag tag) {

        if (tag == null) {
            return "";
        }

        String direct =
                tag.getString("id")
                        .orElse("");

        if (!direct.isBlank()
                && !direct
                .toLowerCase(Locale.ROOT)
                .startsWith("minecraft:")) {

            return direct
                    .trim()
                    .toUpperCase(Locale.ROOT);
        }

        CompoundTag extra =
                tag.getCompound(
                        "ExtraAttributes")
                        .orElse(null);

        if (extra == null) {
            return "";
        }

        return extra
                .getString("id")
                .orElse("")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String textureValue(
            CompoundTag tag,
            int depth) {

        if (tag == null
                || depth > 10) {

            return "";
        }

        for (String key
                : tag.keySet()) {

            if ("Value".equalsIgnoreCase(
                    key)
                    || "value".equalsIgnoreCase(
                    key)) {

                String value =
                        tag.getString(key)
                                .orElse("");

                if (looksLikeTexture(
                        value)) {

                    return value;
                }
            }

            CompoundTag nested =
                    tag.getCompound(key)
                            .orElse(null);

            if (nested != null) {

                String found =
                        textureValue(
                                nested,
                                depth + 1);

                if (!found.isBlank()) {
                    return found;
                }
            }

            ListTag list =
                    tag.getList(key)
                            .orElse(null);

            if (list == null) {
                continue;
            }

            for (int index = 0;
                 index < list.size();
                 index++) {

                CompoundTag entry =
                        list.getCompound(index)
                                .orElse(null);

                if (entry == null) {
                    continue;
                }

                String found =
                        textureValue(
                                entry,
                                depth + 1);

                if (!found.isBlank()) {
                    return found;
                }
            }
        }

        return "";
    }

    private static boolean looksLikeTexture(
            String value) {

        if (value == null
                || value.length() < 40) {

            return false;
        }

        try {
            String decoded =
                    new String(
                            Base64.getDecoder()
                                    .decode(
                                            value),
                            StandardCharsets.UTF_8);

            return decoded.contains(
                    "textures")
                    && decoded.contains(
                    "SKIN");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String itemModel(
            CompoundTag tag,
            int depth) {

        if (tag == null
                || depth > 10) {

            return "";
        }

        for (String key
                : tag.keySet()) {

            String normalized =
                    key.toLowerCase(
                            Locale.ROOT);

            if (normalized.equals(
                    "item_model")
                    || normalized.equals(
                    "minecraft:item_model")) {

                String value =
                        tag.getString(key)
                                .orElse("")
                                .trim();

                if (Identifier.tryParse(
                        value) != null) {

                    return value;
                }
            }

            CompoundTag nested =
                    tag.getCompound(key)
                            .orElse(null);

            if (nested != null) {

                String found =
                        itemModel(
                                nested,
                                depth + 1);

                if (!found.isBlank()) {
                    return found;
                }
            }

            ListTag list =
                    tag.getList(key)
                            .orElse(null);

            if (list == null) {
                continue;
            }

            for (int index = 0;
                 index < list.size();
                 index++) {

                CompoundTag entry =
                        list.getCompound(index)
                                .orElse(null);

                if (entry == null) {
                    continue;
                }

                String found =
                        itemModel(
                                entry,
                                depth + 1);

                if (!found.isBlank()) {
                    return found;
                }
            }
        }

        return "";
    }

    private static void applyProfile(
            ItemStack stack,
            String texture) {

        if (stack == null
                || stack.isEmpty()
                || texture == null
                || texture.isBlank()) {

            return;
        }

        UUID profileId =
                UUID.nameUUIDFromBytes(
                        texture.getBytes(
                                StandardCharsets.UTF_8));

        Property property =
                new Property(
                        "textures",
                        texture);

        GameProfile game =
                new GameProfile(
                        profileId,
                        "SkyBlock",
                        new PropertyMap(
                                ImmutableListMultimap.of(
                                        "textures",
                                        property)));

        stack.set(
                DataComponents.PROFILE,
                ResolvableProfile
                        .createResolved(
                                game));
    }

    private static HolderLookup.Provider registries() {

        Minecraft client =
                Minecraft.getInstance();

        if (client == null) {
            return null;
        }

        if (client.level != null) {
            return client.level
                    .registryAccess();
        }

        if (client.player != null) {
            return client.player
                    .registryAccess();
        }

        if (client.getConnection() != null) {
            return client
                    .getConnection()
                    .registryAccess();
        }

        return null;
    }

    private static String base64Data(
            String raw) {

        if (raw == null) {
            return "";
        }

        String value =
                raw.trim();

        if (value.isBlank()) {
            return "";
        }

        /*
         * Extra safety for cached/older parser output where the entire
         * item_bytes JSON object may have been converted to text.
         */
        if (value.startsWith("{")
                && value.endsWith("}")) {

            try {
                com.google.gson.JsonObject object =
                        com.google.gson.JsonParser
                                .parseString(
                                        value)
                                .getAsJsonObject();

                if (object.has("data")
                        && object.get("data")
                        .isJsonPrimitive()) {

                    return object
                            .get("data")
                            .getAsString()
                            .trim();
                }
            } catch (RuntimeException ignored) {
                return "";
            }
        }

        return value;
    }
}