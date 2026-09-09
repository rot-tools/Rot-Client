package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Applies movement keys while a GUI is open. Click freeze uses
 * keepalive/ping timestamps plus a 500 ms post-click resume. Jump and sneak
 * are included with WASD; sprint stays vanilla. Container clicks stay blocked
 * while any of those movement keys are held.
 */
public final class InventoryWalkRuntime {
    private static boolean clicked;
    private static long clickTime;
    private static long lastPing = System.currentTimeMillis();
    private static boolean appliedThisTick;
    private static boolean managingKeys;

    private InventoryWalkRuntime() {
    }

    public static void noteInventoryClick(ContainerInput input) {
        if (input == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!InventoryWalkPolicy.shouldFreezeWalkOnClick(movementKeysHeld(client))) {
            return;
        }
        boolean alreadyFrozen = clicked;
        clickTime = System.currentTimeMillis();
        clicked = true;
        if (!InventoryWalkPolicy.shouldReleaseKeysOnClick(alreadyFrozen)) {
            return;
        }
        if (client != null && client.options != null) {
            release(movementKeys(client));
        }
    }

    public static boolean shouldBlockContainerClick() {
        Minecraft client = Minecraft.getInstance();
        return isWalkContext(client) && !InventoryWalkPolicy.shouldAllowContainerClick(
                movementKeysHeld(client));
    }

    public static boolean shouldApplyMouseLook() {
        Minecraft client = Minecraft.getInstance();
        return InventoryWalkPolicy.shouldApplyMouseLook(
                walkEnabled(client),
                screenOpen(client),
                pauseScreen(client),
                textFieldFocused(screenOf(client)),
                movementKeysHeld(client));
    }

    public static void noteKeepAliveOrPing() {
        lastPing = System.currentTimeMillis();
    }

    static void tick(Minecraft client) {
        appliedThisTick = false;
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (client == null || client.options == null || client.getWindow() == null) {
            return;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen == null) {
            clicked = false;
            if (managingKeys) {
                applyHeldKeys(client, movementKeys(client));
                managingKeys = false;
            }
            return;
        }
        boolean apply = InventoryWalkPolicy.shouldApplyMovement(
                qol.inventoryWalkEnabled,
                true,
                screen.isPauseScreen(),
                textFieldFocused(screen),
                clicked,
                System.currentTimeMillis(),
                lastPing,
                clickTime,
                qol.inventoryWalkPingMs);
        KeyMapping[] keys = movementKeys(client);
        if (!apply) {
            release(keys);
            managingKeys = true;
            return;
        }
        appliedThisTick = true;
        managingKeys = true;
        applyHeldKeys(client, keys);
    }

    private static void applyHeldKeys(Minecraft client, KeyMapping[] keys) {
        for (KeyMapping mapping : keys) {
            if (mapping == null) {
                continue;
            }
            InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping)
                    .rotclient$boundKey();
            boolean down = bound != null && isBoundKeyDown(client, bound);
            KeyMapping.set(bound, down);
        }
    }

    private static boolean isBoundKeyDown(Minecraft client, InputConstants.Key bound) {
        long handle = client.getWindow().handle();
        int value = bound.getValue();
        if (bound.getType() == InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, value)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        return org.lwjgl.glfw.GLFW.glfwGetKey(handle, value)
                == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    private static void release(KeyMapping[] keys) {
        for (KeyMapping mapping : keys) {
            if (mapping == null) {
                continue;
            }
            InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping)
                    .rotclient$boundKey();
            if (bound != null) {
                KeyMapping.set(bound, false);
            }
        }
    }

    private static KeyMapping[] movementKeys(Minecraft client) {
        return new KeyMapping[] {
                client.options.keyUp,
                client.options.keyDown,
                client.options.keyLeft,
                client.options.keyRight,
                client.options.keyJump,
                client.options.keyShift
        };
    }

    private static boolean textFieldFocused(Screen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof ChatScreen
                || screen instanceof AbstractSignEditScreen
                || screen instanceof BookEditScreen) {
            return true;
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box && box.isFocused()) {
                return true;
            }
        }
        return false;
    }

    static boolean appliedThisTickForTests() {
        return appliedThisTick;
    }

    private static boolean walkEnabled(Minecraft client) {
        return client != null && RotClientClient.qolConfigPublic().inventoryWalkEnabled;
    }

    private static boolean screenOpen(Minecraft client) {
        return screenOf(client) != null;
    }

    private static boolean pauseScreen(Minecraft client) {
        Screen screen = screenOf(client);
        return screen != null && screen.isPauseScreen();
    }

    private static Screen screenOf(Minecraft client) {
        if (client == null || client.gui == null) {
            return null;
        }
        return client.gui.screen();
    }

    private static boolean isWalkContext(Minecraft client) {
        return walkEnabled(client)
                && screenOpen(client)
                && !pauseScreen(client)
                && !textFieldFocused(screenOf(client));
    }

    static boolean movementKeysHeld(Minecraft client) {
        if (client == null || client.options == null || client.getWindow() == null) {
            return false;
        }
        for (KeyMapping mapping : movementKeys(client)) {
            if (mapping == null) {
                continue;
            }
            InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping)
                    .rotclient$boundKey();
            if (bound != null && isBoundKeyDown(client, bound)) {
                return true;
            }
        }
        return false;
    }
}
