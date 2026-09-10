package fi.rotclient;

import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reconstructs Bazaar icons from Hypixel's official SkyBlock item
 * resource metadata.
 *
 * Modern SkyBlock resource-pack models are supplied through item_model.
 * Legacy/custom player-head items are supplied through the skin field.
 *
 * Loading is asynchronous so opening Market Watch never waits on HTTP.
 * Until metadata is available the existing Bazaar icon resolver remains
 * the fallback.
 */
final class MarketWatchSkyBlockResourceIconService {

    private static final URI ITEMS =
            URI.create(
                    "https://api.hypixel.net/v2/resources/skyblock/items");

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(15);

    private static final long REFRESH_MINUTES =
            30L;

    private static final HttpClient CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(8))
                    .build();

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(
                    runnable -> {

                        Thread thread =
                                new Thread(
                                        runnable,
                                        "RotClient-MarketWatch-ItemIcons");

                        thread.setDaemon(true);

                        return thread;
                    });

    private static final AtomicBoolean STARTED =
            new AtomicBoolean();

    private static volatile Map<String, IconMetadata> METADATA =
            Map.of();

    private static final Map<String, ItemStack> STACK_CACHE =
            new ConcurrentHashMap<>();

    private MarketWatchSkyBlockResourceIconService() {
    }

    static ItemStack icon(
            String productId) {

        String id =
                normalizeId(
                        productId);

        if (id.isBlank()) {
            return ItemStack.EMPTY;
        }

        start();

        ItemStack cached =
                STACK_CACHE.get(
                        id);

        if (cached != null
                && !cached.isEmpty()) {

            return cached.copy();
        }

        IconMetadata metadata =
                METADATA.get(
                        id);

        if (metadata == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                createStack(
                        id,
                        metadata);

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        STACK_CACHE.put(
                id,
                stack.copy());

        return stack;
    }

    private static void start() {

        if (!STARTED.compareAndSet(
                false,
                true)) {

            return;
        }

        EXECUTOR.scheduleWithFixedDelay(
                MarketWatchSkyBlockResourceIconService::refreshSafely,
                0L,
                REFRESH_MINUTES,
                TimeUnit.MINUTES);
    }

    private static void refreshSafely() {

        try {
            refresh();
        } catch (RuntimeException ignored) {
            /*
             * Network/resource failures must never affect Market Watch.
             * Existing cached metadata and the normal icon fallback stay
             * available.
             */
        }
    }

    private static void refresh() {

        HttpRequest request =
                HttpRequest.newBuilder(
                                ITEMS)
                        .timeout(
                                REQUEST_TIMEOUT)
                        .header(
                                "Accept",
                                "application/json")
                        .header(
                                "User-Agent",
                                "RotClient-MarketWatch/2")
                        .GET()
                        .build();

        HttpResponse<String> response;

        try {
            response =
                    CLIENT.send(
                            request,
                            HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
            return;
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300
                || response.body() == null
                || response.body().isBlank()) {

            return;
        }

        JsonObject root;

        try {
            JsonElement parsed =
                    JsonParser.parseString(
                            response.body());

            if (!parsed.isJsonObject()) {
                return;
            }

            root =
                    parsed.getAsJsonObject();

        } catch (RuntimeException ignored) {
            return;
        }

        if (!root.has("items")
                || !root.get("items")
                .isJsonArray()) {

            return;
        }

        Map<String, IconMetadata> next =
                new HashMap<>();

        for (JsonElement element
                : root.getAsJsonArray(
                        "items")) {

            if (element == null
                    || !element.isJsonObject()) {

                continue;
            }

            JsonObject item =
                    element.getAsJsonObject();

            String id =
                    normalizeId(
                            text(
                                    item,
                                    "id"));

            if (id.isBlank()) {
                continue;
            }

            String material =
                    text(
                            item,
                            "material")
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT);

            String itemModel =
                    text(
                            item,
                            "item_model")
                            .trim();

            JsonObject skin =
                    object(
                            item,
                            "skin");

            String texture =
                    text(
                            skin,
                            "value")
                            .trim();

            String textureSignature =
                    text(
                            skin,
                            "signature")
                            .trim();

            /*
             * Some resources expose modern component metadata as well.
             * Prefer the explicit top-level item_model, but accept the
             * modern component form when present.
             */
            if (itemModel.isBlank()) {

                JsonObject components =
                        object(
                                item,
                                "components");

                itemModel =
                        firstText(
                                components,
                                "minecraft:item_model",
                                "item_model");
            }

            if (material.isBlank()
                    && itemModel.isBlank()
                    && texture.isBlank()) {

                continue;
            }

            next.put(
                    id,
                    new IconMetadata(
                            material,
                            itemModel,
                            texture,
                            textureSignature,
                            text(
                                    item,
                                    "name")));
        }

        if (next.isEmpty()) {
            return;
        }

        METADATA =
                Map.copyOf(
                        next);

        /*
         * A resource refresh can change model/skin information.
         */
        STACK_CACHE.clear();
    }

    private static ItemStack createStack(
            String productId,
            IconMetadata metadata) {

        if (metadata == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                MarketWatchItemIconResolver
                        .materialIcon(
                                metadata.material(),
                                productId);

        /*
         * Hypixel modern resource-pack model.
         *
         * PAPER is the carrier Hypixel uses for many of these items and
         * is only used here when a real underlying material could not be
         * resolved.
         */
        if (!metadata.itemModel()
                .isBlank()) {

            Identifier model =
                    Identifier.tryParse(
                            metadata.itemModel());

            if (model != null) {

                if (stack.isEmpty()) {
                    stack =
                            new ItemStack(
                                    Items.PAPER);
                }

                stack.set(
                        DataComponents.ITEM_MODEL,
                        model);
            }
        }

        /*
         * Hypixel custom player head.
         */
        if (!metadata.texture()
                .isBlank()) {

            if (stack.isEmpty()
                    || stack.getItem()
                    != Items.PLAYER_HEAD) {

                stack =
                        new ItemStack(
                                Items.PLAYER_HEAD);
            }

            applyProfile(
                    stack,
                    metadata.texture(),
                    metadata.textureSignature());
        }

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!metadata.displayName()
                .isBlank()) {

            stack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(
                            metadata.displayName()));
        }

        return stack;
    }

    private static void applyProfile(
            ItemStack stack,
            String texture,
            String signature) {

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

        Property property;

        /*
         * Authlib does not require the signature for rendering. Keeping
         * it when supplied preserves Hypixel's full texture property.
         */
        if (signature != null
                && !signature.isBlank()) {

            property =
                    new Property(
                            "textures",
                            texture,
                            signature);

        } else {

            property =
                    new Property(
                            "textures",
                            texture);
        }

        GameProfile profile =
                new GameProfile(
                        profileId,
                        "SkyBlock",
                        new PropertyMap(
                                ImmutableListMultimap.of(
                                        "textures",
                                        property)));

        stack.set(
                DataComponents.PROFILE,
                ResolvableProfile.createResolved(
                        profile));
    }

    private static JsonObject object(
            JsonObject parent,
            String key) {

        if (parent == null
                || key == null
                || !parent.has(key)) {

            return null;
        }

        JsonElement element =
                parent.get(
                        key);

        if (element == null
                || !element.isJsonObject()) {

            return null;
        }

        return element.getAsJsonObject();
    }

    private static String firstText(
            JsonObject object,
            String... keys) {

        if (object == null
                || keys == null) {

            return "";
        }

        for (String key : keys) {

            String value =
                    text(
                            object,
                            key)
                            .trim();

            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private static String text(
            JsonObject object,
            String key) {

        if (object == null
                || key == null
                || !object.has(key)) {

            return "";
        }

        JsonElement element =
                object.get(
                        key);

        if (element == null
                || element.isJsonNull()
                || !element.isJsonPrimitive()) {

            return "";
        }

        try {
            return element
                    .getAsString();

        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String normalizeId(
            String value) {

        return value == null
                ? ""
                : value
                .trim()
                .toUpperCase(
                        Locale.ROOT);
    }

    private record IconMetadata(
            String material,
            String itemModel,
            String texture,
            String textureSignature,
            String displayName) {

        private IconMetadata {

            material =
                    material == null
                            ? ""
                            : material.trim();

            itemModel =
                    itemModel == null
                            ? ""
                            : itemModel.trim();

            texture =
                    texture == null
                            ? ""
                            : texture.trim();

            textureSignature =
                    textureSignature == null
                            ? ""
                            : textureSignature.trim();

            displayName =
                    displayName == null
                            ? ""
                            : displayName.trim();
        }
    }
}