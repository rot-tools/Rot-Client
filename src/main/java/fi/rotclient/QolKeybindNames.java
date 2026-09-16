package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;

/**
 * Resolves QoL keybind config strings to Minecraft input codes.
 */
public final class QolKeybindNames {
    private static final boolean[] MOUSE_DOWN = new boolean[8];

    private QolKeybindNames() {
    }

    /**
     * @param raw configured key name; blank uses {@code defaultKey}
     * @return Minecraft keyboard code or {@link InputConstants#UNKNOWN}
     */
    public static int resolveGlfwKey(String raw, String defaultKey) {
        String token = raw == null || raw.isBlank() ? defaultKey : raw;
        if (token == null || token.isBlank()) {
            return InputConstants.UNKNOWN.getValue();
        }
        String key = token.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (key) {
            case "RIGHT_SHIFT", "RSHIFT" -> InputConstants.KEY_RSHIFT;
            case "LEFT_SHIFT", "LSHIFT", "SHIFT" -> InputConstants.KEY_LSHIFT;
            case "RIGHT_CONTROL", "RCTRL", "RIGHT_CTRL" -> InputConstants.KEY_RCONTROL;
            case "LEFT_CONTROL", "LCTRL", "CTRL", "CONTROL" -> InputConstants.KEY_LCONTROL;
            case "RIGHT_ALT", "RALT" -> InputConstants.KEY_RALT;
            case "LEFT_ALT", "LALT", "ALT" -> InputConstants.KEY_LALT;
            case "RIGHT_SUPER", "RSUPER" -> InputConstants.KEY_RGUI;
            case "LEFT_SUPER", "LSUPER", "SUPER" -> InputConstants.KEY_LGUI;
            case "ESCAPE", "ESC" -> InputConstants.KEY_ESCAPE;
            case "TAB" -> InputConstants.KEY_TAB;
            case "SPACE", "SPACEBAR" -> InputConstants.KEY_SPACE;
            case "ENTER", "RETURN" -> InputConstants.KEY_RETURN;
            case "BACKSPACE" -> InputConstants.KEY_BACKSPACE;
            case "INSERT" -> InputConstants.KEY_INSERT;
            case "DELETE", "DEL" -> InputConstants.KEY_DELETE;
            case "HOME" -> InputConstants.KEY_HOME;
            case "END" -> InputConstants.KEY_END;
            case "PAGE_UP", "PGUP" -> InputConstants.KEY_PAGEUP;
            case "PAGE_DOWN", "PGDN", "PGDOWN" -> InputConstants.KEY_PAGEDOWN;
            case "UP", "ARROW_UP" -> InputConstants.KEY_UP;
            case "DOWN", "ARROW_DOWN" -> InputConstants.KEY_DOWN;
            case "LEFT", "ARROW_LEFT" -> InputConstants.KEY_LEFT;
            case "RIGHT", "ARROW_RIGHT" -> InputConstants.KEY_RIGHT;
            case "MINUS", "HYPHEN" -> InputConstants.KEY_MINUS;
            case "EQUAL", "EQUALS" -> InputConstants.KEY_EQUALS;
            case "CAPS_LOCK", "CAPSLOCK" -> InputConstants.KEY_CAPSLOCK;
            case "F1" -> InputConstants.KEY_F1;
            case "F2" -> InputConstants.KEY_F2;
            case "F3" -> InputConstants.KEY_F3;
            case "F4" -> InputConstants.KEY_F4;
            case "F5" -> InputConstants.KEY_F5;
            case "F6" -> InputConstants.KEY_F6;
            case "F7" -> InputConstants.KEY_F7;
            case "F8" -> InputConstants.KEY_F8;
            case "F9" -> InputConstants.KEY_F9;
            case "F10" -> InputConstants.KEY_F10;
            case "F11" -> InputConstants.KEY_F11;
            case "F12" -> InputConstants.KEY_F12;
            default -> key.length() == 1
                    ? resolveSingleKey(key.charAt(0))
                    : InputConstants.UNKNOWN.getValue();
        };
    }

    private static int resolveSingleKey(char key) {
        return switch (key) {
            case 'A' -> InputConstants.KEY_A;
            case 'B' -> InputConstants.KEY_B;
            case 'C' -> InputConstants.KEY_C;
            case 'D' -> InputConstants.KEY_D;
            case 'E' -> InputConstants.KEY_E;
            case 'F' -> InputConstants.KEY_F;
            case 'G' -> InputConstants.KEY_G;
            case 'H' -> InputConstants.KEY_H;
            case 'I' -> InputConstants.KEY_I;
            case 'J' -> InputConstants.KEY_J;
            case 'K' -> InputConstants.KEY_K;
            case 'L' -> InputConstants.KEY_L;
            case 'M' -> InputConstants.KEY_M;
            case 'N' -> InputConstants.KEY_N;
            case 'O' -> InputConstants.KEY_O;
            case 'P' -> InputConstants.KEY_P;
            case 'Q' -> InputConstants.KEY_Q;
            case 'R' -> InputConstants.KEY_R;
            case 'S' -> InputConstants.KEY_S;
            case 'T' -> InputConstants.KEY_T;
            case 'U' -> InputConstants.KEY_U;
            case 'V' -> InputConstants.KEY_V;
            case 'W' -> InputConstants.KEY_W;
            case 'X' -> InputConstants.KEY_X;
            case 'Y' -> InputConstants.KEY_Y;
            case 'Z' -> InputConstants.KEY_Z;
            case '0' -> InputConstants.KEY_0;
            case '1' -> InputConstants.KEY_1;
            case '2' -> InputConstants.KEY_2;
            case '3' -> InputConstants.KEY_3;
            case '4' -> InputConstants.KEY_4;
            case '5' -> InputConstants.KEY_5;
            case '6' -> InputConstants.KEY_6;
            case '7' -> InputConstants.KEY_7;
            case '8' -> InputConstants.KEY_8;
            case '9' -> InputConstants.KEY_9;
            default -> InputConstants.UNKNOWN.getValue();
        };
    }

    public static boolean isKeyDown(long windowHandle, int key) {
        if (key == InputConstants.UNKNOWN.getValue() || windowHandle == 0L) {
            return false;
        }
        return InputConstants.isKeyDown(key);
    }

    public static boolean isBoundDown(long windowHandle, String raw) {
        if (windowHandle == 0L || raw == null || raw.isBlank()) {
            return false;
        }
        Integer mouse = resolveMouseButton(raw);
        if (mouse != null) {
            return mouse >= 0 && mouse < MOUSE_DOWN.length && MOUSE_DOWN[mouse];
        }
        return isKeyDown(windowHandle, resolveGlfwKey(raw, ""));
    }

    /**
     * Tracks mouse state from Minecraft's MouseHandler callback. Minecraft 26.3
     * moved from GLFW to SDL3, so raw GLFW mouse polling is no longer available.
     */
    public static void updateMouseButton(int button, int action) {
        if (button < 0 || button >= MOUSE_DOWN.length) {
            return;
        }
        if (action == InputConstants.RELEASE) {
            MOUSE_DOWN[button] = false;
        } else if (action == InputConstants.PRESS) {
            MOUSE_DOWN[button] = true;
        }
    }

    public static Integer resolveMouseButton(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (key) {
            case "MOUSE_LEFT", "LMB", "LEFT_MOUSE", "MOUSE_1" -> InputConstants.MOUSE_BUTTON_LEFT;
            case "MOUSE_RIGHT", "RMB", "RIGHT_MOUSE", "MOUSE_2" -> InputConstants.MOUSE_BUTTON_RIGHT;
            case "MOUSE_MIDDLE", "MMB", "MIDDLE_MOUSE", "MOUSE_3" -> InputConstants.MOUSE_BUTTON_MIDDLE;
            case "MOUSE_4", "MOUSE4" -> InputConstants.MOUSE_BUTTON_4;
            case "MOUSE_5", "MOUSE5" -> InputConstants.MOUSE_BUTTON_5;
            default -> null;
        };
    }

    public static String formatGlfwKey(int key) {
        if (key == InputConstants.UNKNOWN.getValue() || key == InputConstants.KEY_ESCAPE) {
            return "";
        }
        return switch (key) {
            case InputConstants.KEY_A -> "A";
            case InputConstants.KEY_B -> "B";
            case InputConstants.KEY_C -> "C";
            case InputConstants.KEY_D -> "D";
            case InputConstants.KEY_E -> "E";
            case InputConstants.KEY_F -> "F";
            case InputConstants.KEY_G -> "G";
            case InputConstants.KEY_H -> "H";
            case InputConstants.KEY_I -> "I";
            case InputConstants.KEY_J -> "J";
            case InputConstants.KEY_K -> "K";
            case InputConstants.KEY_L -> "L";
            case InputConstants.KEY_M -> "M";
            case InputConstants.KEY_N -> "N";
            case InputConstants.KEY_O -> "O";
            case InputConstants.KEY_P -> "P";
            case InputConstants.KEY_Q -> "Q";
            case InputConstants.KEY_R -> "R";
            case InputConstants.KEY_S -> "S";
            case InputConstants.KEY_T -> "T";
            case InputConstants.KEY_U -> "U";
            case InputConstants.KEY_V -> "V";
            case InputConstants.KEY_W -> "W";
            case InputConstants.KEY_X -> "X";
            case InputConstants.KEY_Y -> "Y";
            case InputConstants.KEY_Z -> "Z";
            case InputConstants.KEY_0 -> "0";
            case InputConstants.KEY_1 -> "1";
            case InputConstants.KEY_2 -> "2";
            case InputConstants.KEY_3 -> "3";
            case InputConstants.KEY_4 -> "4";
            case InputConstants.KEY_5 -> "5";
            case InputConstants.KEY_6 -> "6";
            case InputConstants.KEY_7 -> "7";
            case InputConstants.KEY_8 -> "8";
            case InputConstants.KEY_9 -> "9";
            case InputConstants.KEY_F1 -> "F1";
            case InputConstants.KEY_F2 -> "F2";
            case InputConstants.KEY_F3 -> "F3";
            case InputConstants.KEY_F4 -> "F4";
            case InputConstants.KEY_F5 -> "F5";
            case InputConstants.KEY_F6 -> "F6";
            case InputConstants.KEY_F7 -> "F7";
            case InputConstants.KEY_F8 -> "F8";
            case InputConstants.KEY_F9 -> "F9";
            case InputConstants.KEY_F10 -> "F10";
            case InputConstants.KEY_F11 -> "F11";
            case InputConstants.KEY_F12 -> "F12";
            case InputConstants.KEY_RSHIFT -> "RIGHT_SHIFT";
            case InputConstants.KEY_LSHIFT -> "LEFT_SHIFT";
            case InputConstants.KEY_RCONTROL -> "RIGHT_CONTROL";
            case InputConstants.KEY_LCONTROL -> "LEFT_CONTROL";
            case InputConstants.KEY_RALT -> "RIGHT_ALT";
            case InputConstants.KEY_LALT -> "LEFT_ALT";
            case InputConstants.KEY_TAB -> "TAB";
            case InputConstants.KEY_SPACE -> "SPACE";
            case InputConstants.KEY_RETURN -> "ENTER";
            case InputConstants.KEY_BACKSPACE -> "BACKSPACE";
            case InputConstants.KEY_INSERT -> "INSERT";
            case InputConstants.KEY_DELETE -> "DELETE";
            case InputConstants.KEY_HOME -> "HOME";
            case InputConstants.KEY_END -> "END";
            case InputConstants.KEY_PAGEUP -> "PAGE_UP";
            case InputConstants.KEY_PAGEDOWN -> "PAGE_DOWN";
            case InputConstants.KEY_UP -> "UP";
            case InputConstants.KEY_DOWN -> "DOWN";
            case InputConstants.KEY_LEFT -> "LEFT";
            case InputConstants.KEY_RIGHT -> "RIGHT";
            case InputConstants.KEY_MINUS -> "MINUS";
            case InputConstants.KEY_EQUALS -> "EQUAL";
            default -> "";
        };
    }

    public static String formatMouseButton(int button) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> "LMB";
            case InputConstants.MOUSE_BUTTON_RIGHT -> "RMB";
            case InputConstants.MOUSE_BUTTON_MIDDLE -> "MMB";
            case InputConstants.MOUSE_BUTTON_4 -> "MOUSE_4";
            case InputConstants.MOUSE_BUTTON_5 -> "MOUSE_5";
            default -> "";
        };
    }
}
