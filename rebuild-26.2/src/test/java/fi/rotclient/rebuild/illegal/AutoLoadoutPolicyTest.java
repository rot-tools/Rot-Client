package fi.rotclient.rebuild.illegal;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;

final class AutoLoadoutPolicyTest {
    @Test
    void recognizesTheMenuAndMapsAllTwelveSlots() {
        assertEquals(12, AutoLoadoutPolicy.menuCapacity("(4/12) Loadouts"));
        assertEquals(12, AutoLoadoutPolicy.menuCapacity("(12/12) Loadouts"));
        assertEquals(0, AutoLoadoutPolicy.menuCapacity("(4/12) Wardrobe"));
        assertEquals(0, AutoLoadoutPolicy.menuCapacity("(13/12) Loadouts"));
        assertArrayEquals(new int[]{14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43},
                IntStream.range(0, 12).map(AutoLoadoutPolicy::containerSlot).toArray());
    }

    @Test
    void respectsEmptyAndAlreadySelectedEntries() {
        assertTrue(AutoLoadoutPolicy.equipped(List.of("A loadout", "", "Right-click to edit!")));
        assertFalse(AutoLoadoutPolicy.equipped(List.of("A loadout", "Left-click to equip!", "Right-click to edit!")));
        assertTrue(AutoLoadoutPolicy.unavailable("Empty Loadout", List.of()));
        assertFalse(AutoLoadoutPolicy.unavailable("Mining", List.of("Left-click to equip!")));
    }

    @Test
    void clampsTimingAndRemovesInvalidOrRepeatedBinds() {
        var settings = new AutoLoadoutSettings();
        settings.equipDelay = -1;
        settings.closeDelay = 12;
        settings.variety = 9;
        settings.keys = new int[]{GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_RIGHT_SHIFT};
        settings.normalize();
        assertFalse(settings.enabled);
        assertEquals(1, settings.equipDelay);
        assertEquals(8, settings.closeDelay);
        assertEquals(5, settings.variety);
        assertEquals(12, settings.keys.length);
        assertEquals(0, settings.slotForKey(GLFW.GLFW_KEY_G));
        assertEquals(GLFW.GLFW_KEY_UNKNOWN, settings.keys[1]);
        assertEquals(-1, settings.slotForKey(GLFW.GLFW_KEY_RIGHT_SHIFT));
    }
}
