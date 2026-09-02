package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AutoSprintPolicyTest {
    @Test
    void disabledLeavesOriginalSprintInputAlone() {
        assertFalse(AutoSprintPolicy.resolveSprintInput(false, false));
        assertTrue(AutoSprintPolicy.resolveSprintInput(true, false));
    }

    @Test
    void enabledForcesSprintInputTrue() {
        assertTrue(AutoSprintPolicy.resolveSprintInput(false, true));
        assertTrue(AutoSprintPolicy.resolveSprintInput(true, true));
    }

    @Test
    void matchesOrSemanticsEverywhereIncludingHypixel() {
        // original || AutoSprint.getEnabled()
        // No Hypixel parameter / gate — same result on every server.
        assertTrue(AutoSprintPolicy.resolveSprintInput(false, true));
        assertTrue(AutoSprintPolicy.resolveSprintInput(true, true));
        assertFalse(AutoSprintPolicy.resolveSprintInput(false, false));
        assertTrue(AutoSprintPolicy.resolveSprintInput(true, false));
    }
}
