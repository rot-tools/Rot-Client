package fi.rotclient;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class DungeonPlusSuperboomStateTest {
    @Test
    void worldChangeCancelsPendingSuperboomSwapBack() throws Exception {
        set("superboomOriginalSlot", 4);
        set("superboomSwapBackTicks", 3);
        set("superboomCooldown", 2);
        set("superboomAttackHeld", true);

        DungeonPlusRuntime.onWorldChanged();

        assertEquals(-1, get("superboomOriginalSlot").getInt(null));
        assertEquals(-1, get("superboomSwapBackTicks").getInt(null));
        assertEquals(0, get("superboomCooldown").getInt(null));
        assertFalse(get("superboomAttackHeld").getBoolean(null));
    }

    private static void set(String name, Object value) throws Exception {
        Field field = get(name);
        if (value instanceof Integer number) field.setInt(null, number);
        else field.setBoolean(null, (Boolean) value);
    }

    private static Field get(String name) throws Exception {
        Field field = DungeonPlusRuntime.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
