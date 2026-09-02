package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class OverflowListPolicyTest {
    @Test
    void longListsGetSearchAndStayOnScreen() {
        assertFalse(OverflowListPolicy.needsSearch(5));
        assertTrue(OverflowListPolicy.needsSearch(8));
        int height = OverflowListPolicy.menuHeight(8, 28, true);
        assertTrue(height > OverflowListPolicy.SEARCH_HEIGHT);
        int clamped = OverflowListPolicy.clampedHeight(400, 240, 200);
        assertTrue(clamped <= 240 - OverflowListPolicy.SCREEN_MARGIN);
        assertEquals(OverflowListPolicy.SCREEN_MARGIN,
                OverflowListPolicy.clampY(-10, 80, 200));
    }

    @Test
    void searchFiltersLabelsCaseInsensitively() {
        List<Integer> hits = OverflowListPolicy.matchingIndices(
                List.of("Floor", "Class", "Secrets", "Score"),
                "se");
        assertEquals(List.of(2), hits);
        assertEquals(
                List.of(0, 1, 2, 3),
                OverflowListPolicy.matchingIndices(
                        List.of("Floor", "Class", "Secrets", "Score"),
                        " "));
    }
}
