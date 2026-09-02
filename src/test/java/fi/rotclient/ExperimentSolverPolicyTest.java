package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentSolverPolicyTest {
    private static ExperimentSolverPolicy.SlotUpdate slot(int index, String path) {
        return new ExperimentSolverPolicy.SlotUpdate(index, path, "", 1, List.of(), "");
    }

    private static ExperimentSolverPolicy.SlotUpdate dye(int index, String path, int count) {
        return new ExperimentSolverPolicy.SlotUpdate(index, path, "", count, List.of(), "");
    }

    @Test
    void classifiesExperimentByTitle() {
        assertEquals(ExperimentSolverPolicy.Experiment.CHRONOMATRON,
                ExperimentSolverPolicy.experimentFor("Chronomatron (Beginner)", true));
        assertEquals(ExperimentSolverPolicy.Experiment.ULTRASEQUENCER,
                ExperimentSolverPolicy.experimentFor("Ultrasequencer (Expert)", true));
        assertEquals(ExperimentSolverPolicy.Experiment.SUPERPAIRS,
                ExperimentSolverPolicy.experimentFor("Superpairs (Grand)", true));
        assertEquals(ExperimentSolverPolicy.Experiment.NONE,
                ExperimentSolverPolicy.experimentFor("Chronomatron (Beginner)", false));
        assertEquals(ExperimentSolverPolicy.Experiment.NONE,
                ExperimentSolverPolicy.experimentFor("Your Bags", true));
    }

    @Test
    void chronomatronRemembersSequenceAndHighlightsNextTwo() {
        ExperimentSolverPolicy policy = new ExperimentSolverPolicy();
        policy.updatePhase(slot(4, "glowstone"));
        assertTrue(policy.rememberPhase());

        // Two flashes: slot 12, then slot 14.
        policy.onChronomatronSlot(slot(12, "red_terracotta"));
        policy.onChronomatronSlot(slot(12, "gray_stained_glass"));
        policy.onChronomatronSlot(slot(14, "blue_terracotta"));
        policy.onChronomatronSlot(slot(14, "gray_stained_glass"));

        assertTrue(policy.highlights(ExperimentSolverPolicy.Experiment.CHRONOMATRON).isEmpty());

        policy.updatePhase(slot(4, "clock"));
        assertFalse(policy.rememberPhase());

        Map<Integer, ExperimentSolverPolicy.Highlight> first =
                policy.highlights(ExperimentSolverPolicy.Experiment.CHRONOMATRON);
        assertEquals(ExperimentSolverPolicy.Highlight.FIRST, first.get(12));
        assertEquals(ExperimentSolverPolicy.Highlight.SECOND, first.get(14));

        policy.onChronomatronClick(12);
        Map<Integer, ExperimentSolverPolicy.Highlight> second =
                policy.highlights(ExperimentSolverPolicy.Experiment.CHRONOMATRON);
        assertEquals(ExperimentSolverPolicy.Highlight.FIRST, second.get(14));
        assertFalse(second.containsKey(12));
    }

    @Test
    void chronomatronBlocksOnlyKnownWrongSlots() {
        ExperimentSolverPolicy policy = new ExperimentSolverPolicy();
        policy.updatePhase(slot(4, "glowstone"));
        policy.onChronomatronSlot(slot(12, "red_terracotta"));
        policy.onChronomatronSlot(slot(12, "gray_stained_glass"));
        policy.onChronomatronSlot(slot(14, "blue_terracotta"));
        policy.updatePhase(slot(4, "clock"));

        assertFalse(policy.shouldBlockClick(ExperimentSolverPolicy.Experiment.CHRONOMATRON, 12));
        assertTrue(policy.shouldBlockClick(ExperimentSolverPolicy.Experiment.CHRONOMATRON, 14));
        // Slots outside the puzzle, such as the close button, stay clickable.
        assertFalse(policy.shouldBlockClick(ExperimentSolverPolicy.Experiment.CHRONOMATRON, 49));
    }

    @Test
    void ultrasequencerSortsDyesByStackSize() {
        ExperimentSolverPolicy policy = new ExperimentSolverPolicy();
        List<ExperimentSolverPolicy.SlotUpdate> container = List.of(
                dye(20, "red_dye", 3),
                dye(21, "red_dye", 1),
                dye(22, "red_dye", 2),
                slot(23, "black_stained_glass_pane"));

        policy.updatePhase(slot(4, "glowstone"));
        policy.onUltrasequencerSlot(slot(4, "glowstone"), container);
        policy.updatePhase(slot(4, "clock"));
        policy.onUltrasequencerSlot(slot(4, "clock"), container);

        Map<Integer, ExperimentSolverPolicy.Highlight> highlights =
                policy.highlights(ExperimentSolverPolicy.Experiment.ULTRASEQUENCER);
        assertEquals(ExperimentSolverPolicy.Highlight.FIRST, highlights.get(21));
        assertEquals(ExperimentSolverPolicy.Highlight.SECOND, highlights.get(22));

        assertTrue(policy.shouldBlockClick(ExperimentSolverPolicy.Experiment.ULTRASEQUENCER, 22));
        assertFalse(policy.shouldBlockClick(ExperimentSolverPolicy.Experiment.ULTRASEQUENCER, 21));

        policy.onUltrasequencerClick(21);
        assertEquals(ExperimentSolverPolicy.Highlight.FIRST,
                policy.highlights(ExperimentSolverPolicy.Experiment.ULTRASEQUENCER).get(22));
    }

    @Test
    void superpairsRemembersRevealedItemsAndPowerups() {
        ExperimentSolverPolicy policy = new ExperimentSolverPolicy();
        ExperimentSolverPolicy.SlotUpdate bookA = new ExperimentSolverPolicy.SlotUpdate(
                11, "enchanted_book", "Enchanted Book", 1, List.of("§9Sharpness V"), "");
        ExperimentSolverPolicy.SlotUpdate bookB = new ExperimentSolverPolicy.SlotUpdate(
                20, "enchanted_book", "Enchanted Book", 1, List.of("§9Sharpness V"), "");
        ExperimentSolverPolicy.SlotUpdate powerup = new ExperimentSolverPolicy.SlotUpdate(
                15, "gunpowder", "Instant Find", 1, List.of("§7Powerup!"), "");

        policy.onSuperpairsSlot(bookA);
        assertTrue(policy.highlights(ExperimentSolverPolicy.Experiment.SUPERPAIRS).isEmpty());

        policy.onSuperpairsSlot(bookB);
        Map<Integer, ExperimentSolverPolicy.Highlight> matched =
                policy.highlights(ExperimentSolverPolicy.Experiment.SUPERPAIRS);
        assertEquals(ExperimentSolverPolicy.Highlight.MATCHED, matched.get(11));
        assertEquals(ExperimentSolverPolicy.Highlight.MATCHED, matched.get(20));

        policy.onSuperpairsSlot(powerup);
        assertEquals(ExperimentSolverPolicy.Highlight.POWERUP,
                policy.highlights(ExperimentSolverPolicy.Experiment.SUPERPAIRS).get(15));

        // Superpairs never swallows clicks.
        assertFalse(policy.shouldBlockClick(ExperimentSolverPolicy.Experiment.SUPERPAIRS, 11));
    }

    @Test
    void resetClearsEveryGame() {
        ExperimentSolverPolicy policy = new ExperimentSolverPolicy();
        policy.updatePhase(slot(4, "glowstone"));
        policy.onChronomatronSlot(slot(12, "red_terracotta"));
        policy.updatePhase(slot(4, "clock"));
        assertFalse(policy.highlights(ExperimentSolverPolicy.Experiment.CHRONOMATRON).isEmpty());

        policy.reset();
        assertTrue(policy.rememberPhase());
        assertTrue(policy.highlights(ExperimentSolverPolicy.Experiment.CHRONOMATRON).isEmpty());
    }
}
