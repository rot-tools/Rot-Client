package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventoryButtonsPolicyTest {
    @Test void normalizesCommandsAndIcons() {
        assertEquals("storage", InventoryButtonsPolicy.normalizeCommand(" /storage "));
        assertEquals("warp hub", InventoryButtonsPolicy.normalizeCommand("/warp\nhub\t"));
        assertEquals("minecraft:ender_chest", InventoryButtonsPolicy.normalizeIcon("ENDER_CHEST"));
    }

    @Test void anchorsAndHitsButtons() {
        var button = new InventoryButtonsPolicy.Button(2, 3, true, true,
                "ender_chest", "/storage", false);
        assertEquals(122, InventoryButtonsPolicy.screenX(button, 20, 100));
        assertEquals(83, InventoryButtonsPolicy.screenY(button, 30, 50));
        assertTrue(InventoryButtonsPolicy.hit(button, 20, 30, 100, 50, 130, 90));
        assertFalse(InventoryButtonsPolicy.hit(button, 20, 30, 100, 50, 90, 90));
    }

    @Test void presetsContainOnlyRunnableButtons() {
        assertTrue(InventoryButtonsPolicy.simplePreset().stream().allMatch(InventoryButtonsPolicy::valid));
        assertEquals(8, InventoryButtonsPolicy.allWarpsPreset().size());
    }

    @Test void shiftDragPersistsAnchoredScreenPosition() {
        var button = new InventoryButtonsPolicy.Button(-20, 0, false, false,
                "ender_chest", "storage", false);
        InventoryButtonsPolicy.moveTo(button, 40, 80, 100, 50, 176, 166);
        assertFalse(button.anchorRight);
        assertFalse(button.anchorBottom);
        assertEquals(40 - 100, button.x);
        assertEquals(80 - 50, button.y);

        InventoryButtonsPolicy.moveTo(button, 300, 200, 100, 50, 176, 166);
        assertTrue(button.anchorRight);
        assertTrue(button.anchorBottom);
        assertEquals(300 - (100 + 176), button.x);
        assertEquals(200 - (50 + 166), button.y);
    }

    @Test void duplicatePreservesButtonSettingsAndMovesBesideSource() {
        var source = new InventoryButtonsPolicy.Button(10, 4, true, false,
                "ender_chest", "storage", true);
        var duplicate = InventoryButtonsPolicy.duplicatedBeside(source);

        assertNotNull(duplicate);
        assertEquals(10 + InventoryButtonsPolicy.LARGE_SIZE + InventoryButtonsPolicy.GAP, duplicate.x);
        assertEquals(4, duplicate.y);
        assertTrue(duplicate.anchorRight);
        assertEquals("storage", duplicate.command);
    }

    @Test void catalogExposesLocalPresetSaveAndRestore() {
        var module = QolUtilityCatalog.findById("qol.inventory_buttons");

        assertNotNull(module);
        assertTrue(module.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.inventory_buttons.save_preset")));
        assertTrue(module.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.inventory_buttons.load_preset")));
    }

    @Test void moveToStoresAnchoredOffsetsFromTheScreenPosition() {
        var button = new InventoryButtonsPolicy.Button(0, 0, false, false,
                "ender_chest", "storage", false);
        InventoryButtonsPolicy.moveTo(button, 130, 90, 20, 30, 100, 50);
        assertTrue(button.anchorRight);
        assertTrue(button.anchorBottom);
        assertEquals(10, button.x);
        assertEquals(10, button.y);
        assertEquals(130, InventoryButtonsPolicy.screenX(button, 20, 100));
        assertEquals(90, InventoryButtonsPolicy.screenY(button, 30, 50));
    }
}
