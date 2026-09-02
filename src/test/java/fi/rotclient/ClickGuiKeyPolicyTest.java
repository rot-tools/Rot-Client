package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

final class ClickGuiKeyPolicyTest {
    @Test
    void keyPressOpensExistingRotUi() {
        assertTrue(ClickGuiKeyPolicy.shouldOpen(
                true, true, false, false, false));
        assertFalse(ClickGuiKeyPolicy.shouldOpen(
                true, true, false, false, true));
    }

    @Test
    void heldKeyDoesNotRepeatedlyOpenUi() {
        assertFalse(ClickGuiKeyPolicy.shouldToggleOpen(
                true, true, true, false));
        assertFalse(ClickGuiKeyPolicy.shouldOpen(
                true, true, true, false, false));
    }

    @Test
    void hudEditorActionUsesExistingHudEditor() {
        assertTrue(QolUtilityCatalog.findById("qol.click_gui").settings().stream()
                .anyMatch(s -> s.id().endsWith("open_hud_editor")
                        && s.type() == QolUtilityCatalog.SettingType.ACTION));
    }

    @Test
    void disabledKeybindDoesNothing() {
        assertFalse(ClickGuiKeyPolicy.shouldToggleOpen(
                false, true, false, false));
        assertFalse(ClickGuiKeyPolicy.shouldOpen(
                false, true, false, false, false));
    }

    @Test
    void defaultKeybindResolvesRightShift() {
        assertEquals(
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                QolKeybindNames.resolveGlfwKey("", "RIGHT_SHIFT"));
        assertEquals(
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                QolKeybindNames.resolveGlfwKey("right shift", "RIGHT_SHIFT"));
    }

    @Test
    void textFieldConsumingBlocksOpen() {
        assertFalse(ClickGuiKeyPolicy.shouldToggleOpen(
                true, true, false, true));
    }
}
