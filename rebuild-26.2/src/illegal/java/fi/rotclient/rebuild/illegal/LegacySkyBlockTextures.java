package fi.rotclient.rebuild.illegal;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

/** Looks up the original vanilla model for a mapped Hypixel item. */
public final class LegacySkyBlockTextures {
    static final String MAP_RESOURCE = "/assets/rotclient_rebuild/data/legacy-item-models.json";
    private static final String SKYBLOCK_NAMESPACE = "hypixel_skyblock";

    private LegacySkyBlockTextures() {}

    public static boolean isSkyBlockModel(Identifier model) {
        return model != null && SKYBLOCK_NAMESPACE.equals(model.getNamespace());
    }

    public static Identifier renderModel(Identifier original, CompoundTag data) {
        if (!isSkyBlockModel(original) || data == null) return original;
        String id = skyBlockId(data);
        if (id.isEmpty()) return original;
        return Models.BY_ITEM.getOrDefault(id, original);
    }

    private static String skyBlockId(CompoundTag data) {
        return data.getString("id").filter(id -> !id.isBlank())
                .or(() -> data.getCompound("ExtraAttributes").flatMap(extra -> extra.getString("id")))
                .map(LegacySkyBlockTextures::normalize).orElse("");
    }

    private static String normalize(String id) {
        return id.strip().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static final class Models {
        private static final Map<String, Identifier> BY_ITEM = load();

        private static Map<String, Identifier> load() {
            try (var stream = LegacySkyBlockTextures.class.getResourceAsStream(MAP_RESOURCE)) {
                if (stream == null) throw new IllegalStateException("Legacy item model map is missing");
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    var models = new HashMap<String, Identifier>();
                    var table = JsonParser.parseReader(reader).getAsJsonObject();
                    table.entrySet().forEach(entry -> {
                        if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) return;
                        Identifier model = Identifier.tryParse(entry.getValue().getAsString());
                        if (model != null && "minecraft".equals(model.getNamespace())) {
                            models.put(normalize(entry.getKey()), model);
                        }
                    });
                    return Map.copyOf(models);
                }
            } catch (Exception error) {
                LoggerFactory.getLogger("rotclient-illegal").warn("Could not load the legacy item model map", error);
                return Map.of();
            }
        }
    }
}
