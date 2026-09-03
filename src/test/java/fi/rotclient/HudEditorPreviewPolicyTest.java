package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudEditorPreviewPolicyTest {
    @Test
    void editorHidesMiningAndPowderWhenToggledOff() {
        assertTrue(HudEditorPreviewPolicy.showMiningTracker(true));
        assertFalse(HudEditorPreviewPolicy.showMiningTracker(false));
        assertTrue(HudEditorPreviewPolicy.showPowderChest(true));
        assertFalse(HudEditorPreviewPolicy.showPowderChest(false));
    }

    @Test
    void hidingWardrobeAlsoTurnsOffCheaterOverlay() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.wardrobe_keybinds", true);
        qol.setModuleEnabled("qol.cheater_wardrobe", true);
        assertTrue(HudLayoutLandingPolicy.hide(qol, "qol.wardrobe_keybinds", true));
        assertFalse(HudLayerTogglePolicy.isOn(qol, "qol.wardrobe_keybinds", true));
        assertFalse(HudLayerTogglePolicy.isOn(qol, "qol.cheater_wardrobe", true));
    }
}
