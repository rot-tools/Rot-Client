package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class WardrobeKeybindPolicyTest {
    @Test
    void pingAndDelaysClampToConfiguredRanges() {
        assertEquals(10, WardrobeKeybindPolicy.clampPingMs(1));
        assertEquals(1000, WardrobeKeybindPolicy.clampPingMs(5000));
        assertEquals(250, WardrobeKeybindPolicy.clampPingMs(250));
        assertFalse(WardrobeKeybindPolicy.pingReady(100L, 0L, 250));
        assertTrue(WardrobeKeybindPolicy.pingReady(250L, 0L, 250));
        assertEquals(8, WardrobeKeybindPolicy.clampDelayTicks(99));
        assertEquals(5, WardrobeKeybindPolicy.clampVariance(99));
        assertEquals(3, WardrobeKeybindPolicy.delayWithVariance(1, 4, 0.5D));
    }

    @Test
    void titlesAndEquippedMarkersMatchHypixelWardrobe() {
        assertTrue(WardrobeKeybindPolicy.isWardrobeTitle("(1/2) Armor Sets"));
        assertTrue(WardrobeKeybindPolicy.containsArmorSets("§8(1/2) Equipment Sets"));
        assertTrue(WardrobeKeybindPolicy.isEquipped(true, "Slot 1"));
        assertTrue(WardrobeKeybindPolicy.isEquipped(false, "Slot 3: Equipped"));
        assertFalse(WardrobeKeybindPolicy.isEquipped(false, "Slot 3: Empty"));
        assertTrue(WardrobeKeybindPolicy.isEmptyMarker(true, "Slot 3: Empty"));
        assertFalse(WardrobeKeybindPolicy.isEmptyMarker(true, "Slot 3"));
        assertTrue(WardrobeKeybindPolicy.isSlotReady(false, false, false));
        assertFalse(WardrobeKeybindPolicy.isSlotReady(false, true, false));
        assertFalse(WardrobeKeybindPolicy.isSlotReady(false, false, true));
        assertTrue(WardrobeKeybindPolicy.shouldClickAutoEquip(true, false));
        assertFalse(WardrobeKeybindPolicy.shouldClickAutoEquip(true, true));
    }

    @Test
    void cancelAllSwapAndHudFollowPolicy() {
        assertTrue(WardrobeKeybindPolicy.shouldCancelOtherInput(
                true, true, false, false, false));
        assertFalse(WardrobeKeybindPolicy.shouldCancelOtherInput(
                true, true, true, false, false));
        assertFalse(WardrobeKeybindPolicy.shouldCancelOtherInput(
                true, true, false, true, false));
        assertFalse(WardrobeKeybindPolicy.shouldCancelOtherInput(
                true, true, false, false, true));
        assertEquals(37, WardrobeKeybindPolicy.swapClickSlot(true, 36, 37));
        assertEquals(36, WardrobeKeybindPolicy.swapClickSlot(false, 36, 37));
        assertEquals("Equipping §7[§c2§7]", WardrobeKeybindPolicy.equipHudText(2));
        assertEquals(36, WardrobeKeybindPolicy.wardrobeSlotIndex(1));
        assertEquals(
                38,
                WardrobeKeybindPolicy.hotbarSlotForKey(
                        13,
                        new int[] {
                            11, 12, 13, 14, 15, 16, 17, 18, 19
                        }).orElse(-1));
        assertTrue(WardrobeKeybindPolicy.autoEquipTimedOut(0L, 2001L));
        assertFalse(WardrobeKeybindPolicy.autoEquipTimedOut(0L, 2000L));
    }

    @Test
    void configRoundTripsWardrobeSettings() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.writeBoolean("qol.wardrobe_keybinds.auto_equip", true);
        assertTrue(config.readBoolean("qol.wardrobe_keybinds.auto_equip"));
        assertTrue(config.writeNumber("qol.wardrobe_keybinds.ping", 400));
        assertEquals(400.0D, config.readNumber("qol.wardrobe_keybinds.ping"));
        assertTrue(config.writeKeybind("qol.wardrobe_keybinds.override", "LEFT_CONTROL"));
        assertEquals("LEFT_CONTROL", config.readKeybind("qol.wardrobe_keybinds.override"));
        assertTrue(config.writeNumber("qol.wardrobe_keybinds.swap_a", 9));
        assertEquals(9.0D, config.readNumber("qol.wardrobe_keybinds.swap_a"));
        config.setPose("wardrobe", 20.0F, 30.0F);
        assertEquals(20.0F, config.pose("wardrobe")[0], 0.01F);
        assertTrue(config.resetModuleToDefaults("qol.wardrobe_keybinds"));
        assertFalse(config.wardrobeAutoEquip);
        assertEquals(250, config.wardrobePingMs);
        assertEquals(12.0F, config.pose("wardrobe")[0], 0.01F);
    }

    @Test
    void hiddenSwapperResolvesExplicitSlotBindsAndRequiresStationaryPlayer() {
        String[] customBinds = {
            "R", "G", "", "", "", "", "", "", ""
        };
        int[] hotbarBinds = {
            11, 12, 13, 14, 15, 16, 17, 18, 19
        };

        assertEquals(
                WardrobeKeybindPolicy.wardrobeSlotIndex(1),
                WardrobeKeybindPolicy.resolveConfiguredSlotKey(
                        "R",
                        42,
                        QolSkyblockExtras.STYLE_CUSTOM,
                        false,
                        customBinds,
                        hotbarBinds).orElse(-1));
        assertEquals(
                WardrobeKeybindPolicy.wardrobeSlotIndex(2),
                WardrobeKeybindPolicy.resolveConfiguredSlotKey(
                        "2",
                        12,
                        QolSkyblockExtras.STYLE_HOTBAR,
                        true,
                        customBinds,
                        hotbarBinds).orElse(-1));
        assertEquals(
                WardrobeKeybindPolicy.wardrobeSlotIndex(6),
                WardrobeKeybindPolicy.resolveConfiguredSlotKey(
                        "6",
                        16,
                        QolSkyblockExtras.STYLE_SIMPLE,
                        false,
                        customBinds,
                        hotbarBinds).orElse(-1));

        assertTrue(WardrobeKeybindPolicy.canStartHiddenEquip(true, false, 0.0D));
        assertFalse(WardrobeKeybindPolicy.canStartHiddenEquip(true, true, 0.0D));
        assertFalse(WardrobeKeybindPolicy.canStartHiddenEquip(true, false, 0.01D));
        assertTrue(WardrobeKeybindPolicy.canStartHiddenEquip(false, true, 1.0D));
    }

    @Test
    void wardrobeSwapperSlotBindsPersistAndResetIndependently() {
        QolUtilityConfig config = new QolUtilityConfig();

        assertTrue(config.readBoolean("qol.cheater_wardrobe.stationary_only"));
        assertTrue(config.writeKeybind("qol.cheater_wardrobe.slot_1", "R"));
        assertTrue(config.writeKeybind("qol.cheater_wardrobe.slot_9", "MOUSE_5"));
        assertEquals("R", config.readKeybind("qol.cheater_wardrobe.slot_1"));
        assertEquals("MOUSE_5", config.readKeybind("qol.cheater_wardrobe.slot_9"));

        config.writeBoolean("qol.cheater_wardrobe.stationary_only", false);
        assertFalse(config.readBoolean("qol.cheater_wardrobe.stationary_only"));
        assertTrue(config.resetModuleToDefaults("qol.cheater_wardrobe"));
        assertTrue(config.readBoolean("qol.cheater_wardrobe.stationary_only"));
        assertEquals("", config.readKeybind("qol.cheater_wardrobe.slot_1"));
        assertEquals("", config.readKeybind("qol.cheater_wardrobe.slot_9"));
    }
}
