package fi.rotclient;

import fi.rotclient.mixin.KeyMappingAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.glfw.GLFW;

/**
 * Applies Farm Keys remaps on the local client. Previous vanilla
 * binds are restored when the module turns off.
 */
public final class FarmKeysRuntime {
    private static boolean applied;
    private static boolean cameraLocked;

    private FarmKeysRuntime() {
    }

    public static boolean cameraLocked() {
        return cameraLocked;
    }

    public static void tick(Minecraft client) {
        QolSkyblockExtras extras = extras();
        cameraLocked = extras != null && FarmKeysPolicy.shouldLockCamera(
                extras.farmKeysEnabled, extras.farmKeysLockCamera);
        if (extras == null || client == null || client.options == null) {
            return;
        }
        if (extras.farmKeysEnabled && !applied) {
            apply(client, extras);
        } else if (!extras.farmKeysEnabled && applied) {
            restore(client, extras);
        }
    }

    private static void apply(Minecraft client, QolSkyblockExtras extras) {
        Options options = client.options;
        extras.farmKeysPrevAttack = describe(bound(options.keyAttack));
        extras.farmKeysPrevJump = describe(bound(options.keyJump));
        boolean changed = false;
        if (FarmKeysPolicy.shouldRemap(true, extras.farmKeysAttack)) {
            InputConstants.Key key = toKey(extras.farmKeysAttack);
            if (key != null) {
                options.keyAttack.setKey(key);
                changed = true;
            }
        }
        if (FarmKeysPolicy.shouldRemap(true, extras.farmKeysJump)) {
            InputConstants.Key key = toKey(extras.farmKeysJump);
            if (key != null) {
                options.keyJump.setKey(key);
                changed = true;
            }
        }
        if (changed) {
            options.save();
            KeyMapping.resetMapping();
        }
        applied = true;
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    private static void restore(Minecraft client, QolSkyblockExtras extras) {
        Options options = client.options;
        boolean changed = false;
        InputConstants.Key attack = toKey(extras.farmKeysPrevAttack);
        if (attack == null) {
            attack = options.keyAttack.getDefaultKey();
        }
        if (attack != null) {
            options.keyAttack.setKey(attack);
            changed = true;
        }
        InputConstants.Key jump = toKey(extras.farmKeysPrevJump);
        if (jump == null) {
            jump = options.keyJump.getDefaultKey();
        }
        if (jump != null) {
            options.keyJump.setKey(jump);
            changed = true;
        }
        extras.farmKeysPrevAttack = "";
        extras.farmKeysPrevJump = "";
        if (changed) {
            options.save();
            KeyMapping.resetMapping();
        }
        applied = false;
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    private static InputConstants.Key bound(KeyMapping mapping) {
        if (mapping instanceof KeyMappingAccessor accessor) {
            return accessor.rotclient$boundKey();
        }
        return mapping == null ? null : mapping.getDefaultKey();
    }

    private static InputConstants.Key toKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Integer mouse = QolKeybindNames.resolveMouseButton(name);
        if (mouse != null) {
            return InputConstants.Type.MOUSE.getOrCreate(mouse);
        }
        int glfw = QolKeybindNames.resolveGlfwKey(name, "");
        if (glfw == GLFW.GLFW_KEY_UNKNOWN) {
            return null;
        }
        return InputConstants.Type.KEYSYM.getOrCreate(glfw);
    }

    private static String describe(InputConstants.Key key) {
        if (key == null) {
            return "";
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return QolKeybindNames.formatMouseButton(key.getValue());
        }
        return QolKeybindNames.formatGlfwKey(key.getValue());
    }

    private static QolSkyblockExtras extras() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol == null ? null : qol.extras();
    }
}
