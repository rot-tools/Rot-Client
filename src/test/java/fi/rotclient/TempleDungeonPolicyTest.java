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
}
