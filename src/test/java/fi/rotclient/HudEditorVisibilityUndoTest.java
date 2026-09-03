package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudEditorVisibilityUndoTest {
    @Test
    void emptyStackIsNoOpAndHideThenUndoPopsLast() {
        HudEditorVisibilityUndo stack = new HudEditorVisibilityUndo();
        assertFalse(stack.canUndo());
        assertNull(stack.undo());
        stack.pushHide(HudEditorVisibilityUndo.Entry.qol("qol.wardrobe_keybinds", true));
        stack.pushHide(HudEditorVisibilityUndo.Entry.miningTracker());
        HudEditorVisibilityUndo.Entry last = stack.undo();
        assertEquals(HudEditorVisibilityUndo.Kind.MINING_TRACKER, last.kind());
        HudEditorVisibilityUndo.Entry wardrobe = stack.undo();
        assertEquals("qol.wardrobe_keybinds", wardrobe.settingId());
        assertTrue(wardrobe.moduleToggle());
        assertNull(stack.undo());
        HudEditorVisibilityUndo.Entry redone = stack.redo();
        assertEquals("qol.wardrobe_keybinds", redone.settingId());
        assertTrue(stack.canUndo());
    }

    @Test
    void capsAtSixteenAndNewHideClearsRedo() {
        HudEditorVisibilityUndo stack = new HudEditorVisibilityUndo();
        for (int i = 0; i < HudEditorVisibilityUndo.MAX_STEPS + 4; i++) {
            stack.pushHide(HudEditorVisibilityUndo.Entry.qol("qol.item." + i, false));
        }
        int count = 0;
        while (stack.undo() != null) {
            count++;
        }
        assertEquals(HudEditorVisibilityUndo.MAX_STEPS, count);
        stack.pushHide(HudEditorVisibilityUndo.Entry.powderChest());
        assertNull(stack.redo());
        assertEquals(
                HudEditorVisibilityUndo.Kind.POWDER_CHEST,
                stack.undo().kind());
    }
}
