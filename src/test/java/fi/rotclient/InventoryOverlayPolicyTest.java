package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class InventoryOverlayPolicyTest {
    @Test
    void equipmentMenuMatchesHypixelStatsChestButNotWardrobe() {
        assertTrue(InventoryOverlayPolicy.isEquipmentMenu("Your Equipment and Stats"));
        assertTrue(InventoryOverlayPolicy.isEquipmentMenu("§aYour Equipment and Stats"));
        assertTrue(InventoryOverlayPolicy.isEquipmentMenu("Your Equipment"));
        assertTrue(InventoryOverlayPolicy.isEquipmentMenu("Equipment"));
        assertTrue(InventoryOverlayPolicy.isEquipmentMenu("Stats & Equipment"));
        assertTrue(InventoryOverlayPolicy.isEquipmentMenu("§aStats & Equipment"));
        assertFalse(InventoryOverlayPolicy.isEquipmentMenu("(2/2) Equipment Sets"));
        assertFalse(InventoryOverlayPolicy.isEquipmentMenu("Pets"));
    }

    @Test
    void skillsMenuMatchesYourSkillsTitle() {
        assertTrue(InventoryOverlayPolicy.isSkillsMenu("Your Skills"));
        assertTrue(InventoryOverlayPolicy.isSkillsMenu("§aYour Skills"));
        assertTrue(InventoryOverlayPolicy.isSkillsMenu("Skills"));
        assertFalse(InventoryOverlayPolicy.isSkillsMenu("Your Equipment and Stats"));
        assertFalse(InventoryOverlayPolicy.isSkillsMenu("Dungeoneering"));
    }

    @Test
    void glassPanesAndEmptySlotNamesArePlaceholders() {
        assertTrue(InventoryOverlayPolicy.isPlaceholder("Empty Necklace Slot", "black_stained_glass_pane"));
        assertTrue(InventoryOverlayPolicy.isPlaceholder("Necklace Slot", "gray_stained_glass_pane"));
        assertTrue(InventoryOverlayPolicy.isPlaceholder("Cloak", "glass_pane"));
        assertFalse(InventoryOverlayPolicy.isPlaceholder("Adaptive Belt", "leather_leggings"));
        assertFalse(InventoryOverlayPolicy.isPlaceholder("Great Spook Necklace", "player_head"));
    }

    @Test
    void equipmentColumnHitTestUsesNorthOffsets() {
        assertEquals(0, InventoryOverlayPolicy.hitEquipmentIndex(0, 0, 76, 8));
        assertEquals(1, InventoryOverlayPolicy.hitEquipmentIndex(0, 0, 80, 8 + 18));
        assertEquals(3, InventoryOverlayPolicy.hitEquipmentIndex(10, 20, 10 + 76, 20 + 8 + 54));
        assertEquals(-1, InventoryOverlayPolicy.hitEquipmentIndex(0, 0, 0, 0));
    }

    @Test
    void equipmentKindIsClassifiedFromNameOrLore() {
        assertEquals(0, InventoryOverlayPolicy.classifyEquipmentIndex(
                "Great Spook Necklace", List.of("Necklace")));
        assertEquals(1, InventoryOverlayPolicy.classifyEquipmentIndex(
                "Spirit Cloak", List.of()));
        assertEquals(2, InventoryOverlayPolicy.classifyEquipmentIndex(
                "Adaptive Belt", List.of("Belt")));
        assertEquals(3, InventoryOverlayPolicy.classifyEquipmentIndex(
                "Titanium Gloves", List.of()));
        assertEquals(-1, InventoryOverlayPolicy.classifyEquipmentIndex(
                "Hyperion", List.of("Damage: +260")));
    }

    @Test
    void anyEquipmentBarOpensStatsAndPetSitsRightOfBottomBar() {
        assertTrue(InventoryOverlayPolicy.hitOpenStatsSlot(0, 0, 76, 8));
        assertTrue(InventoryOverlayPolicy.hitOpenStatsSlot(0, 0, 76, 8 + 18));
        assertTrue(InventoryOverlayPolicy.hitOpenStatsSlot(0, 0, 76, 8 + 54));
        assertFalse(InventoryOverlayPolicy.hitOpenStatsSlot(0, 0, 0, 0));
        assertEquals("stats", InventoryOverlayPolicy.OPEN_STATS_COMMAND);
        assertEquals("equipment", InventoryOverlayPolicy.OPEN_WARDROBE_COMMAND);
        assertEquals("pets", InventoryOverlayPolicy.OPEN_PETS_COMMAND);
        assertEquals(94, InventoryOverlayPolicy.DEFAULT_PET_SLOT_X);
        assertEquals(62, InventoryOverlayPolicy.DEFAULT_PET_SLOT_Y);
        assertEquals(94, InventoryOverlayPolicy.petSlotX());
        assertEquals(62, InventoryOverlayPolicy.petSlotY());
        assertTrue(InventoryOverlayPolicy.hitPetSlot(0, 0, 94, 62));
        assertFalse(InventoryOverlayPolicy.hitPetSlot(0, 0, 76, 62));
        assertTrue(InventoryOverlayPolicy.hitPetSlot(0, 0, 10, 5, 104, 67));
        assertTrue(InventoryOverlayPolicy.isVanillaOffhandSlot(77, 62));
        assertFalse(InventoryOverlayPolicy.isVanillaOffhandSlot(76, 62));
    }

    @Test
    void petDragOffsetFollowsMouseAndClamps() {
        assertEquals(2, InventoryOverlayPolicy.petGrabX(0, 96, 0));
        assertEquals(3, InventoryOverlayPolicy.petGrabY(0, 65, 0));
        InventoryOverlayPolicy.PetDragOffset moved =
                InventoryOverlayPolicy.offsetFromDrag(0, 0, 100, 70, 2, 2);
        assertEquals(4, moved.x());
        assertEquals(6, moved.y());
        assertEquals(240, InventoryOverlayPolicy.clampPetOffset(999));
        assertEquals(-240, InventoryOverlayPolicy.clampPetOffset(-999));
        InventoryOverlayPolicy.PetDragOffset clamped =
                InventoryOverlayPolicy.offsetFromDrag(0, 0, 2000, -2000, 0, 0);
        assertEquals(240, clamped.x());
        assertEquals(-240, clamped.y());
    }

    @Test
    void wrenchSitsInTheInventoryTopRightAndSlidersRewriteChannels() {
        InventoryOverlayPolicy.Rect wrench = InventoryOverlayPolicy.wrenchRect(40, 20, 176);
        assertEquals(40 + 176 - 16 - 4, wrench.x());
        assertEquals(20 + 4, wrench.y());
        assertTrue(InventoryOverlayPolicy.hitWrench(40, 20, 176, wrench.x() + 1, wrench.y() + 1));
        assertFalse(InventoryOverlayPolicy.hitWrench(40, 20, 176, 40 + 176 + 8, 20 + 4));
        assertEquals(0xAA, InventoryOverlayPolicy.channelValue(0xAA112233, 3));
        assertEquals(0xCC001122, InventoryOverlayPolicy.withChannel(0xAA001122, 3, 0xCC));
        InventoryOverlayPolicy.Rect slider = new InventoryOverlayPolicy.Rect(0, 0, 256, 10);
        assertEquals(0, InventoryOverlayPolicy.sliderValue(slider, 0));
        assertEquals(255, InventoryOverlayPolicy.sliderValue(slider, 255));
        InventoryOverlayPolicy.Rect panel = InventoryOverlayPolicy.chromeRegion(
                InventoryOverlayPolicy.ChromeRegion.PANEL, 40, 20, 176, 166);
        assertEquals(40, panel.x());
        assertEquals(176, panel.width());
        assertEquals(166, panel.height());
        InventoryOverlayPolicy.Rect header = InventoryOverlayPolicy.chromeRegion(
                InventoryOverlayPolicy.ChromeRegion.HEADER, 40, 20, 176, 166);
        assertEquals(76, header.height());
        InventoryOverlayPolicy.Rect hotbar = InventoryOverlayPolicy.chromeRegion(
                InventoryOverlayPolicy.ChromeRegion.HOTBAR, 40, 20, 176, 166);
        assertEquals(47, hotbar.x());
        assertEquals(161, hotbar.y());
        InventoryOverlayPolicy.Rect editor = InventoryOverlayPolicy.editorRect(
                wrench.x(), wrench.y(), 640, 360);
        assertEquals(wrench.x() + InventoryOverlayPolicy.WRENCH_SIZE + 6, editor.x());
        assertTrue(editor.x() > wrench.x());
    }
}
