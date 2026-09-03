package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DashboardNavStackTest {
    @Test
    void pushPopRestoresPreviousFrameAndDropsDuplicates() {
        DashboardNavStack stack = new DashboardNavStack();
        DashboardNavStack.Frame overview = DashboardNavStack.Frame.overview();
        DashboardNavStack.Frame combat = new DashboardNavStack.Frame(
                "qol_settings", "COMBAT", "", false, false);
        stack.push(overview);
        stack.push(overview);
        assertEquals(1, stack.size());
        stack.push(combat);
        assertEquals(combat, stack.pop());
        assertEquals(overview, stack.pop());
        assertNull(stack.pop());
        assertTrue(stack.isEmpty());
        assertTrue(overview.isOverviewHome());
        assertFalse(combat.isOverviewHome());
    }

    @Test
    void capsDepthAndClearEmpties() {
        DashboardNavStack stack = new DashboardNavStack();
        for (int i = 0; i < DashboardNavStack.MAX_DEPTH + 4; i++) {
            stack.push(new DashboardNavStack.Frame(
                    "qol_settings", "COMBAT", "mod-" + i, false, false));
        }
        assertEquals(DashboardNavStack.MAX_DEPTH, stack.size());
        stack.clear();
        assertTrue(stack.isEmpty());
    }
}
