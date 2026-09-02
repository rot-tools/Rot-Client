package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Restores the pre-custom-model SkyBlock look when the Hypixel 26.2 resource
 * pack is still loaded.
 *
 * <p>The server pack must stay enabled: GUI fonts, HUD icons, panorama, and
 * player-head skulls have no vanilla stand-in. This policy only remaps
 * {@code hypixel_skyblock:} item models onto vanilla item models using the
 * legacy models {@code items.json} coverage (bundled, no download). Skulls and
 * heads are omitted so they keep the new pack / skin.
 */
public final class LegacyTexturesPolicy {
    private static final String CLASSPATH = "/assets/rotclient/data/legacy-item-models.json";
    private static final Set<String> KEEP_NEW_PACK_NAMESPACES = Set.of("minecraft");
    private static final Set<String> CUSTOM_PACK_NAMESPACES = Set.of("hypixel_skyblock");
    private static final Map<String, String> VANILLA_MODELS = loadVanillaModels();

    private LegacyTexturesPolicy() {
    }

    public static String vanillaModel(String skyBlockId) {
        if (skyBlockId == null || skyBlockId.isBlank()) {
            return "";
        }
        String mapped = VANILLA_MODELS.get(SkyBlockItemId.normalize(skyBlockId));
        return mapped == null ? "" : mapped;
    }

    public static boolean shouldReplace(boolean moduleOn, boolean itemsOn, String skyBlockId) {
        return shouldReplace(moduleOn, itemsOn, skyBlockId, "hypixel_skyblock");
    }

    public static boolean shouldReplace(
            boolean moduleOn,
            boolean itemsOn,
            String skyBlockId,
            String currentModelNamespace) {
        if (!moduleOn || !itemsOn || vanillaModel(skyBlockId).isBlank()) {
            return false;
        }
        String ns = currentModelNamespace == null
                ? ""
                : currentModelNamespace.trim().toLowerCase(Locale.ROOT);
        if (ns.isBlank()) {
            return true;
        }
        if (KEEP_NEW_PACK_NAMESPACES.contains(ns)) {
            return false;
        }
        return CUSTOM_PACK_NAMESPACES.contains(ns);
    }

    public static int mappedCount() {
        return VANILLA_MODELS.size();
    }

    private static Map<String, String> loadVanillaModels() {
        try (InputStream in = LegacyTexturesPolicy.class.getResourceAsStream(CLASSPATH)) {
            if (in == null) {
                return Map.of();
            }
            JsonObject root = JsonParser.parseString(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> mapped = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue() == null || !entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                String id = SkyBlockItemId.normalize(entry.getKey());
                String model = entry.getValue().getAsString().trim().toLowerCase(Locale.ROOT);
                if (id.isEmpty() || !model.startsWith("minecraft:")) {
                    continue;
                }
                mapped.put(id, model);
            }
            return Collections.unmodifiableMap(mapped);
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
