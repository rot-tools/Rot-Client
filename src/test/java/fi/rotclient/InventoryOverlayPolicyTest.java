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
        assertTrue(InventoryOverlayPolicy.isEquipmentSetsMenu("(2/2) Equipment Sets"));
        assertTrue(InventoryOverlayPolicy.isEquipmentSetsMenu("§a(1/4) Equipment Sets"));
        assertEquals(0, InventoryOverlayPolicy.equipmentSetsColumn(36, "lime_dye").orElse(-1));
        assertTrue(InventoryOverlayPolicy.isEquipmentSetsPieceSlot(0, 0));
        assertTrue(InventoryOverlayPolicy.isEquipmentSetsPieceSlot(27, 0));
        assertFalse(InventoryOverlayPolicy.isEquipmentSetsPieceSlot(36, 0));
        assertFalse(InventoryOverlayPolicy.isEquipmentMenu("Pets"));
    }

    @Test
    void skillsMenuMatchesYourSkillsTitle() {
        assertTrue(InventoryOverlayPolicy.isSkillsMenu("Your Skills"));
        assertTrue(InventoryOverlayPolicy.isSkillsMenu("§aYour Skills"));
        assertTrue(InventoryOverlayPolicy.isSkillsMenu("Skills"));
        assertFalse(InventoryOverlayPolicy.isSkillsMenu("Your Equipment and Stats"));
        assertFalse(InventoryOverlayPolicy.isSkillsMenu("Dungeoneering"));
        assertFalse(InventoryOverlayPolicy.showSkyblockInventoryUi(false));
        assertTrue(InventoryOverlayPolicy.showSkyblockInventoryUi(true));
        assertTrue(InventoryOverlayPolicy.hideInventoryStatusEffects(true, true));
        assertFalse(InventoryOverlayPolicy.hideInventoryStatusEffects(true, false));
        assertFalse(InventoryOverlayPolicy.hideInventoryStatusEffects(false, true));
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
    void emptyStatsOrPetsChestsKeepLastObservedEquipmentAndPet() {
        assertTrue(InventoryOverlayPolicy.shouldKeepExistingCache(true, true));
        assertFalse(InventoryOverlayPolicy.shouldKeepExistingCache(true, false));
        assertFalse(InventoryOverlayPolicy.shouldKeepExistingCache(false, true));
        assertEquals("rotclient-inventory-chrome-cache.json", InventoryOverlayPolicy.CHROME_CACHE_FILE);
        assertEquals(1, InventoryOverlayPolicy.CHROME_CACHE_SCHEMA);
        assertTrue(InventoryOverlayPolicy.shouldReloadChromeCache(true, true, true, true, true));
        assertTrue(InventoryOverlayPolicy.shouldReloadChromeCache(true, false, true, false, true));
        assertFalse(InventoryOverlayPolicy.shouldReloadChromeCache(true, false, true, true, true));
        assertFalse(InventoryOverlayPolicy.shouldReloadChromeCache(true, true, false, false, false));
        assertTrue(InventoryOverlayPolicy.shouldSkipEmptyChromeSave(true, true));
        assertFalse(InventoryOverlayPolicy.shouldSkipEmptyChromeSave(true, false));
        assertFalse(InventoryOverlayPolicy.shouldSkipEmptyChromeSave(false, true));
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
        assertEquals(40 + 176 - InventoryOverlayPolicy.WRENCH_SIZE - InventoryOverlayPolicy.WRENCH_GAP, wrench.x());
        assertEquals(20 + InventoryOverlayPolicy.WRENCH_GAP, wrench.y());
        assertEquals(12, wrench.width());
        assertEquals(12, wrench.height());
        assertTrue(InventoryOverlayPolicy.hitWrench(40, 20, 176, wrench.x() + 1, wrench.y() + 1));
        assertFalse(InventoryOverlayPolicy.hitWrench(40, 20, 176, 40 + 176 + 8, 20 + 4));
        assertEquals(0xAA, InventoryOverlayPolicy.channelValue(0xAA112233, 3));
        assertEquals(0xCC001122, InventoryOverlayPolicy.withChannel(0xAA001122, 3, 0xCC));
        assertEquals(0xE0112233, InventoryOverlayPolicy.withChannelEnsuringVisible(0x00112233, 0, 0x11));
        assertEquals(0x00112233, InventoryOverlayPolicy.withChannelEnsuringVisible(0x00112233, 3, 0));
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
        assertEquals(248, editor.width());
        assertEquals(
                InventoryOverlayPolicy.DEFAULT_INV_PANEL,
                InventoryOverlayPolicy.defaultChromeColor(
                        InventoryOverlayPolicy.ChromeColorRole.INV_PANEL));
        assertEquals(
                InventoryOverlayPolicy.DEFAULT_STORAGE_CARD,
                InventoryOverlayPolicy.defaultChromeColor(
                        InventoryOverlayPolicy.ChromeColorRole.STORAGE_CARD));
        InventoryOverlayPolicy.Rect row = InventoryOverlayPolicy.editorRowRect(editor, 0);
        InventoryOverlayPolicy.Rect reset = InventoryOverlayPolicy.editorResetRect(row);
        InventoryOverlayPolicy.Rect swatch = InventoryOverlayPolicy.editorSwatchRect(row);
        assertTrue(reset.x() + reset.width() <= swatch.x());
        assertTrue(reset.contains(reset.x() + 1, reset.y() + 1));
        InventoryOverlayPolicy.Rect mascot = InventoryOverlayPolicy.mascotCoverRect(40, 20, 176);
        assertEquals(40 + 140, mascot.x());
        assertEquals(20 + 50, mascot.y());
        assertTrue(mascot.x() + mascot.width() >= 40 + 176);
        assertEquals(34, mascot.height());
        InventoryOverlayPolicy.Rect close = InventoryOverlayPolicy.editorCloseRect(editor);
        assertEquals(editor.x() + editor.width() - InventoryOverlayPolicy.EDITOR_CLOSE_SIZE - 4, close.x());
        assertEquals(editor.y() + 4, close.y());
        assertFalse(InventoryOverlayPolicy.editorRowRect(editor, 0).contains(close.x() + 1, close.y() + 1));
        assertTrue(InventoryOverlayPolicy.dismissColorEditor(true, false, false, false));
        assertTrue(InventoryOverlayPolicy.dismissColorEditor(true, false, true, true));
        assertFalse(InventoryOverlayPolicy.dismissColorEditor(true, false, true, false));
        assertFalse(InventoryOverlayPolicy.dismissColorEditor(true, true, false, false));
        assertFalse(InventoryOverlayPolicy.dismissColorEditor(false, false, false, false));
        InventoryOverlayPolicy.Rect value = InventoryOverlayPolicy.valueMarkRect(40, 20);
        InventoryOverlayPolicy.Rect dash = InventoryOverlayPolicy.dashboardButtonRect(40, 20, 176);
        assertEquals(InventoryOverlayPolicy.SLOT_SIZE, dash.width());
        assertEquals(InventoryOverlayPolicy.SLOT_SIZE, dash.height());
        assertEquals(InventoryOverlayPolicy.SLOT_SIZE, value.width());
        assertEquals(InventoryOverlayPolicy.SLOT_SIZE, value.height());
        assertEquals(value.y(), dash.y());
        assertEquals(20 + InventoryOverlayPolicy.DEFAULT_PET_SLOT_Y, value.y());
        assertEquals(
                40 + InventoryOverlayPolicy.DEFAULT_PET_SLOT_X + InventoryOverlayPolicy.SLOT_SIZE
                        + InventoryOverlayPolicy.VALUE_MARK_GAP,
                value.x());
        assertEquals(value.x() + value.width() + InventoryOverlayPolicy.VALUE_MARK_GAP, dash.x());
        assertTrue(InventoryOverlayPolicy.hitDashboardButton(40, 20, 176, dash.x() + 1, dash.y() + 1));
        assertTrue(InventoryOverlayPolicy.hitValueMark(40, 20, value.x() + 1, value.y() + 1));
        assertFalse(value.contains(dash.x(), dash.y()));
        InventoryOverlayPolicy.Rect movedPetValue =
                InventoryOverlayPolicy.valueMarkRect(40, 20, 12, -4);
        InventoryOverlayPolicy.Rect movedPetDash =
                InventoryOverlayPolicy.dashboardButtonRect(40, 20, 12, -4);
        assertEquals(
                40 + InventoryOverlayPolicy.petSlotX(12) + InventoryOverlayPolicy.SLOT_SIZE
                        + InventoryOverlayPolicy.VALUE_MARK_GAP,
                movedPetValue.x());
        assertEquals(20 + InventoryOverlayPolicy.petSlotY(-4), movedPetValue.y());
        assertEquals(movedPetValue.y(), movedPetDash.y());
        assertEquals(
                movedPetValue.x() + movedPetValue.width() + InventoryOverlayPolicy.VALUE_MARK_GAP,
                movedPetDash.x());
        assertTrue(InventoryOverlayPolicy.hitValueMark(
                40, 20, 12, -4, movedPetValue.x() + 1, movedPetValue.y() + 1));
        assertTrue(InventoryOverlayPolicy.hitDashboardButton(
                40, 20, 12, -4, movedPetDash.x() + 1, movedPetDash.y() + 1));
    }

    @Test
    void chromeSkipsPlayerPreview() {
        int left = 40;
        int top = 20;
        InventoryOverlayPolicy.Rect player = InventoryOverlayPolicy.playerPreviewRect(left, top);
        assertEquals(left + 26, player.x());
        assertEquals(top + 8, player.y());
        assertEquals(49, player.width());
        assertEquals(70, player.height());
        InventoryOverlayPolicy.Rect crafting = InventoryOverlayPolicy.craftingGridRect(left, top);
        assertEquals(left + 97, crafting.x());
        assertEquals(top + 6, crafting.y());
        assertTrue(crafting.x() + crafting.width() >= left + 154 + 18);
        assertTrue(crafting.y() + crafting.height() >= top + 36 + 18);

        List<InventoryOverlayPolicy.Rect> header = InventoryOverlayPolicy.chromeFillRects(
                InventoryOverlayPolicy.ChromeRegion.HEADER, left, top, 176, 166);
        List<InventoryOverlayPolicy.Rect> panel = InventoryOverlayPolicy.chromeFillRects(
                InventoryOverlayPolicy.ChromeRegion.PANEL, left, top, 176, 166);
        assertFalse(covers(header, player.x() + 8, player.y() + 8));
        assertFalse(covers(panel, player.x() + 8, player.y() + 8));
        assertTrue(covers(header, left + InventoryOverlayPolicy.ARMOR_COLUMN_X + 2, top + 10));
        assertTrue(covers(header, left + InventoryOverlayPolicy.EQUIPMENT_COLUMN_X + 2, top + 10));
        assertTrue(covers(header, left + InventoryOverlayPolicy.CRAFT_INPUT_X + 2, top + 20));
        assertTrue(covers(panel, left + 10, top + 90));
        assertEquals(0xFF7A628C, InventoryOverlayPolicy.DEFAULT_SLOT_BORDER);
        assertEquals(0xFF100C14, InventoryOverlayPolicy.DEFAULT_SLOT_WELL);
    }

    @Test
    void survivalSlotsCoverArmorCraftingBackpackAndHotbar() {
        int left = 40;
        int top = 20;
        List<InventoryOverlayPolicy.Rect> withOffhand =
                InventoryOverlayPolicy.survivalSlotRects(left, top, true);
        List<InventoryOverlayPolicy.Rect> withoutOffhand =
                InventoryOverlayPolicy.survivalSlotRects(left, top, false);
        assertEquals(46, withOffhand.size());
        assertEquals(45, withoutOffhand.size());
        assertEquals(InventoryOverlayPolicy.slotRect(left, top, 8, 8), withOffhand.get(0));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 98, 18)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 116, 36)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 154, 28)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 8, 84)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 152, 120)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 8, 142)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 152, 142)));
        assertTrue(withOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 77, 62)));
        assertFalse(withoutOffhand.contains(
                InventoryOverlayPolicy.slotRect(left, top, 77, 62)));
        for (InventoryOverlayPolicy.Rect slot : withoutOffhand) {
            InventoryOverlayPolicy.Rect player =
                    InventoryOverlayPolicy.playerPreviewRect(left, top);
            assertFalse(
                    player.contains(slot.x() + 2, slot.y() + 2),
                    "vanilla slot overlaps player preview: " + slot);
        }
    }

    private static boolean covers(List<InventoryOverlayPolicy.Rect> rects, int x, int y) {
        for (InventoryOverlayPolicy.Rect rect : rects) {
            if (rect.contains(x, y)) {
                return true;
            }
        }
        return false;
    }
}
