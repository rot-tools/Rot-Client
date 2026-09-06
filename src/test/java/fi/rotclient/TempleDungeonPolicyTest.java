package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TempleDungeonPolicyTest {
    @Test
    void doorHighlightRecognizesWitherAndBloodChat() {
        assertEquals(
                TempleDungeonPolicy.DoorKeyEvent.WITHER_OBTAINED,
                TempleDungeonPolicy.doorKeyEvent("[MVP+] Henri has obtained Wither Key!"));
        assertEquals(
                TempleDungeonPolicy.DoorKeyEvent.WITHER_PICKED_UP,
                TempleDungeonPolicy.doorKeyEvent("A Wither Key was picked up!"));
        assertEquals(
                TempleDungeonPolicy.DoorKeyEvent.WITHER_DOOR_OPEN,
                TempleDungeonPolicy.doorKeyEvent("Henri opened a WITHER door!"));
        assertEquals(
                TempleDungeonPolicy.DoorKeyEvent.BLOOD_OBTAINED,
                TempleDungeonPolicy.doorKeyEvent("Henri has obtained Blood Key!"));
        assertEquals(
                TempleDungeonPolicy.DoorKeyEvent.BLOOD_DOOR_OPEN,
                TempleDungeonPolicy.doorKeyEvent("The BLOOD DOOR has been opened!"));
        assertEquals(
                TempleDungeonPolicy.DoorKeyEvent.NONE,
                TempleDungeonPolicy.doorKeyEvent("You hear the sound of something opening..."));
        assertEquals("Wither Key", TempleDungeonPolicy.doorKeyTitle(
                TempleDungeonPolicy.DoorKeyEvent.WITHER_OBTAINED).orElseThrow());
    }

    @Test
    void spiritBearBreakerCloseChestAndSuperboom() {
        assertTrue(TempleDungeonPolicy.isSpiritBearHologram("Spirit Bear 2M❤"));
        assertFalse(TempleDungeonPolicy.isSpiritBearHologram("Spirit Leap"));
        assertEquals(DungeonPolicy.EspKind.SPIRIT_BEAR, DungeonPolicy.classifyHologram("Spirit Bear"));
        assertTrue(TempleDungeonPolicy.isDungeonBreakerItem("Dungeon Breaker", "DUNGEON_STONE"));
        assertEquals(
                "Breaker 12/20",
                TempleDungeonPolicy.parseBreakerCharges("Charges: 12/20⸕").orElseThrow().hudLine());
        assertTrue(TempleDungeonPolicy.shouldBlockBreakerOnSecret(
                true, true, "minecraft:chest"));
        assertFalse(TempleDungeonPolicy.shouldBlockBreakerOnSecret(
                true, true, "minecraft:stone"));
        assertTrue(TempleDungeonPolicy.shouldAutoCloseChest(true, "Chest"));
        assertFalse(TempleDungeonPolicy.shouldAutoCloseChest(true, "The Catacombs"));
        assertTrue(TempleDungeonPolicy.isSuperboomItem("Superboom TNT", "SUPERBOOM_TNT"));
        assertTrue(TempleDungeonPolicy.queueTermsSupported());
    }

    @Test
    void cameraHelperClampsAndClip() {
        assertEquals(4.0D, TempleDungeonPolicy.clampCameraDistance(4));
        assertEquals(1.0D, TempleDungeonPolicy.clampCameraDistance(0));
        assertEquals(4.0F, TempleDungeonPolicy.cameraZoom(true, true, true, 4, 12.0F), 0.01F);
        assertEquals(12.0F, TempleDungeonPolicy.cameraZoom(false, true, true, 4, 12.0F), 0.01F);
        assertEquals(12.0F, TempleDungeonPolicy.cameraZoom(true, false, true, 4, 12.0F), 0.01F);
    }

    @Test
    void keyDropSkullsLockedChestAndClickedSecretPolicy() {
        assertEquals(
                TempleDungeonPolicy.KeySkull.WITHER,
                TempleDungeonPolicy.keySkull("2865274b-3097-394e-8149-ec629c72d850"));
        assertEquals(
                TempleDungeonPolicy.KeySkull.BLOOD,
                TempleDungeonPolicy.keySkull("73F6D1F9-DF41-3D1D-B98C-E1442D915885"));
        assertEquals(TempleDungeonPolicy.KeySkull.NONE, TempleDungeonPolicy.keySkull("not-a-key"));
        assertEquals("Wither Key Dropped",
                TempleDungeonPolicy.keyDropTitle(TempleDungeonPolicy.KeySkull.WITHER));
        assertTrue(TempleDungeonPolicy.keyDropClass(DungeonPolicy.DungeonClass.ARCHER, false));
        assertTrue(TempleDungeonPolicy.keyDropClass(DungeonPolicy.DungeonClass.MAGE, false));
        assertFalse(TempleDungeonPolicy.keyDropClass(DungeonPolicy.DungeonClass.HEALER, false));
        assertTrue(TempleDungeonPolicy.keyDropClass(DungeonPolicy.DungeonClass.HEALER, true));
        assertTrue(TempleDungeonPolicy.keyPickupClearsDrop("A Wither Key was picked up!"));
        assertTrue(TempleDungeonPolicy.keyPickupClearsDrop("[MVP+] Henri has obtained Blood Key!"));
        assertFalse(TempleDungeonPolicy.keyPickupClearsDrop("Henri opened a WITHER door!"));
        assertTrue(TempleDungeonPolicy.lockedChestChat("This chest is locked!"));
        assertTrue(TempleDungeonPolicy.lockedChestChat("§cThat chest is locked!"));
        assertFalse(TempleDungeonPolicy.lockedChestChat("You opened a chest."));
        assertEquals(7, TempleDungeonPolicy.clampSecretStaySeconds(7));
        assertEquals(1, TempleDungeonPolicy.clampSecretStaySeconds(0));
        assertEquals(120, TempleDungeonPolicy.clampSecretStaySeconds(999));
        assertTrue(TempleDungeonPolicy.shouldBoxSecretClick(true, true, false, false, true));
        assertFalse(TempleDungeonPolicy.shouldBoxSecretClick(true, true, true, false, true));
        assertTrue(TempleDungeonPolicy.shouldBoxSecretClick(true, true, true, true, true));
    }
}
