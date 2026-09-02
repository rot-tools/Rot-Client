package fi.rotclient;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Resolves QoL keybind config strings to GLFW key codes.
 */
public final class QolKeybindNames {
    private QolKeybindNames() {
    }

    /**
     * @param raw configured key name; blank uses {@code defaultKey}
     * @return GLFW key or {@link GLFW#GLFW_KEY_UNKNOWN}
     */
    public static int resolveGlfwKey(String raw, String defaultKey) {
        String token = raw == null || raw.isBlank() ? defaultKey : raw;
        if (token == null || token.isBlank()) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
        String key = token.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (key) {
            case "RIGHT_SHIFT", "RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LEFT_SHIFT", "LSHIFT", "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHT_CONTROL", "RCTRL", "RIGHT_CTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LEFT_CONTROL", "LCTRL", "CTRL", "CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RIGHT_ALT", "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "LEFT_ALT", "LALT", "ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHT_SUPER", "RSUPER" -> GLFW.GLFW_KEY_RIGHT_SUPER;
            case "LEFT_SUPER", "LSUPER", "SUPER" -> GLFW.GLFW_KEY_LEFT_SUPER;
            case "ESCAPE", "ESC" -> GLFW.GLFW_KEY_ESCAPE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "SPACE", "SPACEBAR" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "DELETE", "DEL" -> GLFW.GLFW_KEY_DELETE;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGE_UP", "PGUP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN", "PGDN", "PGDOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "UP", "ARROW_UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN", "ARROW_DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT", "ARROW_LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT", "ARROW_RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "MINUS", "HYPHEN" -> GLFW.GLFW_KEY_MINUS;
            case "EQUAL", "EQUALS" -> GLFW.GLFW_KEY_EQUAL;
            case "CAPS_LOCK", "CAPSLOCK" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "F1" -> GLFW.GLFW_KEY_F1;
            case "F2" -> GLFW.GLFW_KEY_F2;
            case "F3" -> GLFW.GLFW_KEY_F3;
            case "F4" -> GLFW.GLFW_KEY_F4;
            case "F5" -> GLFW.GLFW_KEY_F5;
            case "F6" -> GLFW.GLFW_KEY_F6;
            case "F7" -> GLFW.GLFW_KEY_F7;
            case "F8" -> GLFW.GLFW_KEY_F8;
            case "F9" -> GLFW.GLFW_KEY_F9;
            case "F10" -> GLFW.GLFW_KEY_F10;
            case "F11" -> GLFW.GLFW_KEY_F11;
            case "F12" -> GLFW.GLFW_KEY_F12;
            default -> {
                if (key.length() == 1) {
                    char c = key.charAt(0);
                    if (c >= 'A' && c <= 'Z') {
                        yield GLFW.GLFW_KEY_A + (c - 'A');
                    }
                    if (c >= '0' && c <= '9') {
                        yield GLFW.GLFW_KEY_0 + (c - '0');
                    }
                }
                yield GLFW.GLFW_KEY_UNKNOWN;
            }
        };
    }

    public static boolean isKeyDown(long windowHandle, int glfwKey) {
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN || windowHandle == 0L) {
            return false;
        }
        return GLFW.glfwGetKey(windowHandle, glfwKey) == GLFW.GLFW_PRESS;
    }

    public static boolean isBoundDown(long windowHandle, String raw) {
        if (windowHandle == 0L || raw == null || raw.isBlank()) {
            return false;
        }
        Integer mouse = resolveMouseButton(raw);
        if (mouse != null) {
            return GLFW.glfwGetMouseButton(windowHandle, mouse) == GLFW.GLFW_PRESS;
        }
        return isKeyDown(windowHandle, resolveGlfwKey(raw, ""));
    }

    public static Integer resolveMouseButton(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (key) {
            case "MOUSE_LEFT", "LMB", "LEFT_MOUSE", "MOUSE_1" -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case "MOUSE_RIGHT", "RMB", "RIGHT_MOUSE", "MOUSE_2" -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case "MOUSE_MIDDLE", "MMB", "MIDDLE_MOUSE", "MOUSE_3" -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            case "MOUSE_4", "MOUSE4" -> GLFW.GLFW_MOUSE_BUTTON_4;
            case "MOUSE_5", "MOUSE5" -> GLFW.GLFW_MOUSE_BUTTON_5;
            default -> null;
        };
    }

    public static String formatGlfwKey(int glfwKey) {
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN || glfwKey == GLFW.GLFW_KEY_ESCAPE) {
            return "";
        }
        if (glfwKey >= GLFW.GLFW_KEY_A && glfwKey <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (glfwKey - GLFW.GLFW_KEY_A)));
        }
        if (glfwKey >= GLFW.GLFW_KEY_0 && glfwKey <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (glfwKey - GLFW.GLFW_KEY_0)));
        }
        if (glfwKey >= GLFW.GLFW_KEY_F1 && glfwKey <= GLFW.GLFW_KEY_F12) {
            return "F" + (glfwKey - GLFW.GLFW_KEY_F1 + 1);
        }
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT_SHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT_SHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT_CONTROL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT_CONTROL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT_ALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT_ALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE_DOWN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_MINUS -> "MINUS";
            case GLFW.GLFW_KEY_EQUAL -> "EQUAL";
            default -> "";
        };
    }

    public static String formatMouseButton(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
            case GLFW.GLFW_MOUSE_BUTTON_4 -> "MOUSE_4";
            case GLFW.GLFW_MOUSE_BUTTON_5 -> "MOUSE_5";
            default -> "";
        };
    }
}
