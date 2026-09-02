package fi.rotclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bundled, network-independent item index shared by item search, recipes,
 * museum helpers and visual frame selection.
 */
public final class RotItemIndex {
    private static final String RESOURCE = "/data/rotclient/items.tsv";
    private static final Map<String, ItemDef> ITEMS = load();

    private RotItemIndex() {
    }

    public static Map<String, ItemDef> items() {
        return ITEMS;
    }

    public static ItemDef find(String itemId) {
        return ITEMS.get(normalizeId(itemId));
    }

    public static List<ItemDef> search(String query, int limit) {
        String needle = normalizeSearch(query);
        if (needle.isEmpty()) {
            return ITEMS.values().stream()
                    .filter(item -> !item.source().equals("Internal validation"))
                    .limit(Math.max(0, limit)).toList();
        }
        List<String> tokens = List.of(needle.split(" "));
        return ITEMS.values().stream()
                .filter(item -> !item.source().equals("Internal validation"))
                .filter(item -> {
                    String haystack = normalizeSearch(item.id() + " " + item.name() + " "
                            + item.source() + " " + item.museumSet());
                    return tokens.stream().allMatch(haystack::contains);
                })
                .limit(Math.max(0, limit))
                .toList();
    }

    public static RecipeNode recipeTree(String itemId, int quantity) {
        return recipeTree(normalizeId(itemId), Math.max(1, quantity), new LinkedHashSet<>());
    }

    public static Map<String, Integer> aggregateIngredients(String itemId, int quantity) {
        Map<String, Integer> result = new LinkedHashMap<>();
        aggregate(recipeTree(itemId, quantity), result);
        result.remove(normalizeId(itemId));
        return Collections.unmodifiableMap(result);
    }

    public static List<String> missingMuseumPieces(String museumSet, Set<String> ownedIds) {
        String set = museumSet == null ? "" : museumSet.strip();
        Set<String> owned = new LinkedHashSet<>();
        if (ownedIds != null) {
            ownedIds.forEach(id -> owned.add(normalizeId(id)));
        }
        return ITEMS.values().stream()
                .filter(item -> !set.isEmpty() && item.museumSet().equalsIgnoreCase(set))
                .map(ItemDef::id)
                .filter(id -> !owned.contains(id))
                .toList();
    }

    public static int animationFrameIndex(long gameTime, int frameCount, int ticksPerFrame) {
        if (frameCount <= 1) {
            return 0;
        }
        long step = Math.max(1, ticksPerFrame);
        return (int) Math.floorMod(gameTime / step, frameCount);
    }

    private static RecipeNode recipeTree(
            String itemId, int quantity, LinkedHashSet<String> path) {
        ItemDef item = find(itemId);
        if (item == null) {
            return new RecipeNode(itemId, quantity, true, false, List.of());
        }
        if (!path.add(item.id())) {
            return new RecipeNode(item.id(), quantity, false, true, List.of());
        }
        List<RecipeNode> children = new ArrayList<>();
        for (Ingredient ingredient : item.recipe()) {
            children.add(recipeTree(
                    ingredient.itemId(),
                    safeMultiply(quantity, ingredient.quantity()),
                    new LinkedHashSet<>(path)));
        }
        return new RecipeNode(item.id(), quantity, item.recipe().isEmpty(), false, children);
    }

    private static void aggregate(RecipeNode node, Map<String, Integer> result) {
        if (node.children().isEmpty() || node.cycle()) {
            result.merge(node.itemId(), node.quantity(), RotItemIndex::safeAdd);
            return;
        }
        for (RecipeNode child : node.children()) {
            aggregate(child, result);
        }
    }

    private static Map<String, ItemDef> load() {
        InputStream stream = RotItemIndex.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            return Map.of();
        }
        Map<String, ItemDef> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ItemDef item = parseLine(line);
                if (item != null) {
                    result.put(item.id(), item);
                }
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Collections.unmodifiableMap(result);
    }

    static ItemDef parseLine(String line) {
        if (line == null || line.isBlank() || line.stripLeading().startsWith("#")) {
            return null;
        }
        String[] fields = line.split("\\|", -1);
        if (fields.length < 7) {
            return null;
        }
        String id = normalizeId(fields[0]);
        if (id.isEmpty()) {
            return null;
        }
        List<Ingredient> recipe = new ArrayList<>();
        for (String raw : fields[4].split(",")) {
            String piece = raw.strip();
            if (piece.isEmpty()) {
                continue;
            }
            int star = piece.lastIndexOf('*');
            String ingredientId = normalizeId(star < 0 ? piece : piece.substring(0, star));
            int quantity = star < 0 ? 1 : parsePositive(piece.substring(star + 1));
            recipe.add(new Ingredient(ingredientId, quantity));
        }
        List<Integer> dyeFrames = new ArrayList<>();
        for (String raw : fields[5].split(";")) {
            if (!raw.isBlank()) {
                dyeFrames.add(parseNonNegative(raw));
            }
        }
        List<String> skinFrames = new ArrayList<>();
        for (String raw : fields[6].split(";")) {
            if (!raw.isBlank()) {
                skinFrames.add(raw.strip().toLowerCase(Locale.ROOT));
            }
        }
        return new ItemDef(
                id, fields[1].strip(), fields[2].strip(), fields[3].strip(),
                recipe, dyeFrames, skinFrames);
    }

    private static int parsePositive(String raw) {
        return Math.max(1, parseNonNegative(raw));
    }

    private static int parseNonNegative(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw.strip()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.strip().toUpperCase(Locale.ROOT)
                .replace(' ', '_').replace('-', '_');
    }

    private static String normalizeSearch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('_', ' ').replaceAll("\\s+", " ").strip();
    }

    private static int safeMultiply(int left, int right) {
        long product = (long) left * right;
        return product > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) product;
    }

    private static int safeAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    public record Ingredient(String itemId, int quantity) {
        public Ingredient {
            itemId = normalizeId(itemId);
            quantity = Math.max(1, quantity);
        }
    }

    public record ItemDef(
            String id,
            String name,
            String source,
            String museumSet,
            List<Ingredient> recipe,
            List<Integer> dyeFrames,
            List<String> skinFrames) {
        public ItemDef {
            id = normalizeId(id);
            name = name == null ? "" : name.strip();
            source = source == null ? "" : source.strip();
            museumSet = museumSet == null ? "" : museumSet.strip();
            recipe = recipe == null ? List.of() : List.copyOf(recipe);
            dyeFrames = dyeFrames == null ? List.of() : List.copyOf(dyeFrames);
            skinFrames = skinFrames == null ? List.of() : List.copyOf(skinFrames);
        }
    }

    public record RecipeNode(
            String itemId,
            int quantity,
            boolean leaf,
            boolean cycle,
            List<RecipeNode> children) {
        public RecipeNode {
            itemId = normalizeId(itemId);
            quantity = Math.max(1, quantity);
            children = children == null ? List.of() : List.copyOf(children);
        }
    }
}
