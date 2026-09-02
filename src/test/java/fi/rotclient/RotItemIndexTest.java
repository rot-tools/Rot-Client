package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RotItemIndexTest {
    @Test
    void bundledIndexSearchesAndAggregatesRecursiveRecipes() {
        assertFalse(RotItemIndex.items().isEmpty());
        assertEquals(
                "ASPECT_OF_THE_END",
                RotItemIndex.search("aspect end", 10).getFirst().id());
        Map<String, Integer> ingredients =
                RotItemIndex.aggregateIngredients("ASPECT_OF_THE_END", 1);
        assertEquals(5_120, ingredients.get("DIAMOND"));
        assertEquals(16, ingredients.get("ENCHANTED_ENDER_PEARL"));
        assertEquals(32, ingredients.get("BLAZE_POWDER"));
    }

    @Test
    void recipeCyclesTerminateAndRemainVisible() {
        RotItemIndex.RecipeNode root =
                RotItemIndex.recipeTree("ROT_TEST_CYCLE_A", 1);
        assertTrue(root.children().getFirst().children().getFirst().cycle());
    }

    @Test
    void museumArmorReportsOnlyMissingPieces() {
        assertEquals(
                Set.of("YOUNG_DRAGON_CHESTPLATE", "YOUNG_DRAGON_LEGGINGS"),
                Set.copyOf(RotItemIndex.missingMuseumPieces(
                        "Young Dragon Armor",
                        Set.of("YOUNG_DRAGON_HELMET", "YOUNG_DRAGON_BOOTS"))));
    }

    @Test
    void animationFramesAreDeterministicAndBounded() {
        assertEquals(0, RotItemIndex.animationFrameIndex(0, 3, 5));
        assertEquals(1, RotItemIndex.animationFrameIndex(5, 3, 5));
        assertEquals(2, RotItemIndex.animationFrameIndex(10, 3, 5));
        assertEquals(0, RotItemIndex.animationFrameIndex(15, 3, 5));
    }
}
