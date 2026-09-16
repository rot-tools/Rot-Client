package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.platform.InputConstants;

final class MenuKeybindPolicyTest {
    @Test
    void wardrobeTitlesMatchHypixelArmorAndEquipmentSets() {
        assertEquals(1, MenuKeybindPolicy.parseWardrobeTitle("(1/2) Armor Sets").current());
        assertEquals(2, MenuKeybindPolicy.parseWardrobeTitle("(1/2) Armor Sets").total());
        assertEquals(
                "Equipment",
                MenuKeybindPolicy.parseWardrobeTitle("(2/2) Equipment Sets") == null
                        ? ""
                        : "Equipment");
        assertTrue(MenuKeybindPolicy.parseWardrobeTitle("(2/2) Equipment Sets") != null);
        assertTrue(MenuKeybindPolicy.parseWardrobeTitle("Pets") == null);
        assertTrue(MenuKeybindPolicy.parseWardrobeTitle(
                "§c(§e1§c/§e2§c) Armor Sets") != null);
        assertEquals(
                1,
                MenuKeybindPolicy.parseWardrobeTitle(
                        "(1/2)\u00A0Armor Sets").current());
    }

    @Test
    void wardrobeArrowsAndNumberKeysUseKnownSlots() {
        OptionalInt next = MenuKeybindPolicy.resolveWardrobeSlot(
                "(1/2) Armor Sets",
                InputConstants.KEY_RIGHT,
                "RIGHT",
                "LEFT",
                "",
                -1,
                false,
                false);
        assertEquals(MenuKeybindPolicy.WARDROBE_NEXT_SLOT, next.orElse(-1));
        OptionalInt prev = MenuKeybindPolicy.resolveWardrobeSlot(
                "(2/2) Armor Sets",
                InputConstants.KEY_LEFT,
                "RIGHT",
                "LEFT",
                "",
                -1,
                false,
                false);
        assertEquals(MenuKeybindPolicy.WARDROBE_PREVIOUS_SLOT, prev.orElse(-1));
        OptionalInt first = MenuKeybindPolicy.resolveWardrobeSlot(
                "(1/2) Armor Sets",
                InputConstants.KEY_1,
                "RIGHT",
                "LEFT",
                "",
                -1,
                false,
                false);
        assertEquals(36, first.orElse(-1));
        assertTrue(MenuKeybindPolicy.resolveWardrobeSlot(
                "(2/2) Armor Sets",
                InputConstants.KEY_RIGHT,
                "RIGHT",
                "LEFT",
                "",
                -1,
                false,
                false).isEmpty());
    }

    @Test
    void wardrobeUnequipUsesEquippedHoverName() {
        assertTrue(MenuKeybindPolicy.isEquippedWardrobeName("Slot 3: Equipped"));
        assertTrue(MenuKeybindPolicy.isEquippedWardrobeName("§aSlot 3: Equipped"));
        OptionalInt unequip = MenuKeybindPolicy.resolveWardrobeSlot(
                "(1/2) Armor Sets",
                InputConstants.KEY_U,
                "RIGHT",
                "LEFT",
                "U",
                38,
                false,
                false);
        assertEquals(38, unequip.orElse(-1));
        assertTrue(MenuKeybindPolicy.resolveWardrobeSlot(
                "(1/2) Armor Sets",
                InputConstants.KEY_1,
                "RIGHT",
                "LEFT",
                "",
                36,
                true,
                false).isEmpty());
    }

    @Test
    void petsTitlesAndSlotsMatchLayout() {
        assertEquals(1, MenuKeybindPolicy.parsePetsTitle("Pets").current());
        assertEquals(2, MenuKeybindPolicy.parsePetsTitle("(2/4) Pets").current());
        assertEquals(1, MenuKeybindPolicy.parsePetsTitle("§5Pets").current());
        OptionalInt next = MenuKeybindPolicy.resolvePetsSlot(
                "(1/2) Pets",
                InputConstants.KEY_RIGHT,
                "RIGHT",
                "LEFT",
                "",
                -1,
                false,
                false,
                false);
        assertEquals(MenuKeybindPolicy.PETS_NEXT_SLOT, next.orElse(-1));
        OptionalInt pet3 = MenuKeybindPolicy.resolvePetsSlot(
                "Pets",
                InputConstants.KEY_3,
                "RIGHT",
                "LEFT",
                "",
                -1,
                false,
                false,
                false);
        assertEquals(12, pet3.orElse(-1));
        OptionalInt close = MenuKeybindPolicy.resolvePetsSlot(
                "Pets",
                InputConstants.KEY_3,
                "RIGHT",
                "LEFT",
                "",
                12,
                true,
                false,
                true);
        assertEquals(MenuKeybindPolicy.PETS_CLOSE_SLOT, close.orElse(-1));
        assertTrue(MenuKeybindPolicy.loreMeansPetEquipped(
                List.of("Click to despawn!")));
        assertTrue(MenuKeybindPolicy.loreMeansPetEquipped(
                List.of("§eClick to despawn!")));
    }

    @Test
    void loadoutTitlesAndSlotGrid() {
        assertEquals(1, MenuKeybindPolicy.parseLoadoutTitle("(1/3) Loadout").current());
        assertTrue(MenuKeybindPolicy.parseLoadoutTitle("§8(1/3) Loadout") != null);
        OptionalInt next = MenuKeybindPolicy.resolveLoadoutSlot(
                "(1/3) Loadout", InputConstants.KEY_RIGHT, "RIGHT", "LEFT");
        assertEquals(MenuKeybindPolicy.LOADOUT_NEXT_SLOT, next.orElse(-1));
        OptionalInt prev = MenuKeybindPolicy.resolveLoadoutSlot(
                "(2/3) Loadout", InputConstants.KEY_LEFT, "RIGHT", "LEFT");
        assertEquals(MenuKeybindPolicy.LOADOUT_PREVIOUS_SLOT, prev.orElse(-1));
        OptionalInt first = MenuKeybindPolicy.resolveLoadoutSlot(
                "(1/3) Loadout", InputConstants.KEY_1, "RIGHT", "LEFT");
        assertEquals(14, first.orElse(-1));
        OptionalInt tenth = MenuKeybindPolicy.resolveLoadoutSlot(
                "(1/3) Loadout", InputConstants.KEY_0, "RIGHT", "LEFT");
        assertEquals(41, tenth.orElse(-1));
        OptionalInt twelfth = MenuKeybindPolicy.resolveLoadoutSlot(
                "(1/3) Loadout", InputConstants.KEY_EQUALS, "RIGHT", "LEFT");
        assertEquals(43, twelfth.orElse(-1));
    }

    @Test
    void keyNameRoundTripForArrowsAndMouse() {
        assertEquals("RIGHT", QolKeybindNames.formatGlfwKey(InputConstants.KEY_RIGHT));
        assertEquals(InputConstants.KEY_RIGHT, QolKeybindNames.resolveGlfwKey("RIGHT", ""));
        assertEquals("LMB", QolKeybindNames.formatMouseButton(InputConstants.MOUSE_BUTTON_LEFT));
        assertEquals(
                InputConstants.MOUSE_BUTTON_RIGHT,
                QolKeybindNames.resolveMouseButton("RMB"));
        assertEquals("", QolKeybindNames.formatGlfwKey(InputConstants.KEY_ESCAPE));
    }
}
