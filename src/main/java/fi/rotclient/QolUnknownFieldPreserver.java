package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps fields outside this edition's QoL schema as opaque JSON. */
final class QolUnknownFieldPreserver {
    private static final Set<String> KNOWN_FIELDS =
            Arrays.stream(QolUtilityConfig.class.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers())
                            && !Modifier.isTransient(field.getModifiers())
                            && !field.isSynthetic())
                    .map(java.lang.reflect.Field::getName)
                    .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> KNOWN_EXTRA_FIELDS =
            Arrays.stream(QolSkyblockExtras.class.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers())
                            && !Modifier.isTransient(field.getModifiers())
                            && !field.isSynthetic())
                    .map(java.lang.reflect.Field::getName)
                    .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> KNOWN_ATHEN_FIELDS =
            Arrays.stream(DungeonAthenSettings.class.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers())
                            && !Modifier.isTransient(field.getModifiers())
                            && !field.isSynthetic())
                    .map(java.lang.reflect.Field::getName)
                    .collect(Collectors.toUnmodifiableSet());

    private QolUnknownFieldPreserver() {
    }

    static void preserve(JsonObject qol) {
        if (qol == null) return;
        JsonObject opaque = qol.has("extensionFields")
                && qol.get("extensionFields").isJsonObject()
                ? qol.getAsJsonObject("extensionFields") : new JsonObject();
        for (var field : qol.entrySet()) {
            if (!KNOWN_FIELDS.contains(field.getKey()) && !opaque.has(field.getKey())) {
                opaque.add(field.getKey(), field.getValue().deepCopy());
            }
        }
        qol.add("extensionFields", opaque);
        preserveExtras(object(qol, "extras"));
    }

    private static void preserveExtras(JsonObject extras) {
        if (extras == null) return;
        JsonObject opaque = extras.has("extensionFields")
                && extras.get("extensionFields").isJsonObject()
                ? extras.getAsJsonObject("extensionFields") : new JsonObject();
        for (var field : extras.entrySet()) {
            if (!KNOWN_EXTRA_FIELDS.contains(field.getKey()) && !opaque.has(field.getKey())) {
                opaque.add(field.getKey(), field.getValue().deepCopy());
            }
        }
        extras.add("extensionFields", opaque);
        preserveAthen(object(extras, "athen"));
    }

    private static void preserveAthen(JsonObject athen) {
        if (athen == null) return;
        JsonObject opaque = athen.has("extensionFields")
                && athen.get("extensionFields").isJsonObject()
                ? athen.getAsJsonObject("extensionFields") : new JsonObject();
        for (var field : athen.entrySet()) {
            if (!KNOWN_ATHEN_FIELDS.contains(field.getKey()) && !opaque.has(field.getKey())) {
                opaque.add(field.getKey(), field.getValue().deepCopy());
            }
        }
        athen.add("extensionFields", opaque);
    }

    static void preserveProfiles(JsonObject root) {
        if (root == null) return;
        JsonElement profiles = root.get("profiles");
        if (profiles == null || !profiles.isJsonArray()) return;
        JsonArray entries = profiles.getAsJsonArray();
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject()) continue;
            JsonObject profile = entry.getAsJsonObject();
            JsonObject settings = object(profile, "settings");
            preserve(object(settings, "qolUtilities"));
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(key);
    }
}
