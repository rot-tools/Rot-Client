package fi.rotclient.rebuild.illegal;

import java.util.Arrays;
import org.lwjgl.glfw.GLFW;

final class AutoLoadoutSettings {
    static final int SLOT_COUNT = 12;
    boolean enabled;
    int equipDelay = 1;
    int closeDelay = 1;
    int variety = 1;
    int[] keys = unbound();

    void normalize() {
        equipDelay = Math.clamp(equipDelay, 1, 8);
        closeDelay = Math.clamp(closeDelay, 1, 8);
        variety = Math.clamp(variety, 1, 5);
        int[] values = unbound();
        if (keys != null) {
            for (int slot = 0; slot < Math.min(keys.length, SLOT_COUNT); slot++) {
                if (validKey(keys[slot]) && !bound(values, keys[slot])) values[slot] = keys[slot];
            }
        }
        keys = values;
    }

    private static int[] unbound() {
        int[] result = new int[SLOT_COUNT];
        Arrays.fill(result, GLFW.GLFW_KEY_UNKNOWN);
        return result;
    }

    static boolean validKey(int key) {
        return key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST
                && key != GLFW.GLFW_KEY_RIGHT_SHIFT && key != GLFW.GLFW_KEY_ESCAPE;
    }

    private static boolean bound(int[] bindings, int key) {
        for (int existing : bindings) {
            if (existing == key) return true;
        }
        return false;
    }

    int slotForKey(int key) {
        if (!validKey(key)) return -1;
        for (int slot = 0; slot < keys.length; slot++) {
            if (keys[slot] == key) return slot;
        }
        return -1;
    }
}
