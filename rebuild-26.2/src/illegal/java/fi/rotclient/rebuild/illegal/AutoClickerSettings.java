package fi.rotclient.rebuild.illegal;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.glfw.GLFW;

final class AutoClickerSettings {
    static final int MIN_CPS = 5;
    static final int MAX_CPS = 20;
    static final int MAX_JITTER = 3;
    static final int UNBOUND = GLFW.GLFW_KEY_UNKNOWN;
    private static final int MOUSE_BINDING_BASE = -100;

    boolean enabled;
    boolean allowBreaking;
    boolean skipDungeonBreaker = true;
    boolean whitelistOnly;
    boolean terminatorOnly;
    String testServer = "";
    int jitter;
    Button left = new Button();
    Button right = new Button();

    void normalize() {
        testServer = testServer == null ? "" : testServer.strip();
        jitter = Math.clamp(jitter, 0, MAX_JITTER);
        if (left == null) left = new Button();
        if (right == null) right = new Button();
        left.normalize();
        right.normalize();
        if (left.binding == right.binding) right.binding = UNBOUND;
    }

    boolean hasBinding(int binding) {
        return binding != UNBOUND && (left.binding == binding || right.binding == binding);
    }

    static int mouseBinding(int button) {
        return MOUSE_BINDING_BASE - button;
    }

    static InputConstants.Key inputKey(int binding) {
        int mouseButton = MOUSE_BINDING_BASE - binding;
        if (mouseButton >= GLFW.GLFW_MOUSE_BUTTON_1 && mouseButton <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return InputConstants.Type.MOUSE.getOrCreate(mouseButton);
        }
        if (AutoLoadoutSettings.validKey(binding)) return InputConstants.Type.KEYSYM.getOrCreate(binding);
        return InputConstants.UNKNOWN;
    }

    static boolean isMouseBinding(InputConstants.Key key) {
        return key.getType() == InputConstants.Type.MOUSE;
    }

    static List<String> items(String names) {
        if (names == null || names.isBlank()) return List.of();
        return Arrays.stream(names.split(",")).map(String::strip)
                .filter(name -> !name.isEmpty()).distinct().limit(32).toList();
    }

    static final class Button {
        boolean enabled;
        int binding = UNBOUND;
        int cps = 5;
        List<String> whitelist = List.of();

        void normalize() {
            if (inputKey(binding) == InputConstants.UNKNOWN) binding = UNBOUND;
            cps = Math.clamp(cps, MIN_CPS, MAX_CPS);
            if (whitelist == null) {
                whitelist = List.of();
            } else {
                whitelist = whitelist.stream().filter(java.util.Objects::nonNull)
                        .map(String::strip).filter(name -> !name.isEmpty()).distinct().limit(32).toList();
            }
        }
    }
}
