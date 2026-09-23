package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Minecraft-client input bridge for configured QoL key names. Minecraft 26.3
 * uses SDL3 underneath InputConstants, so callers must not depend on GLFW
 * numeric key or mouse values.
 */
public final class QolInputRuntime {
    private static final Set<Integer> MOUSE_DOWN = new HashSet<>();

    private QolInputRuntime() {
    }

    public static int resolveGlfwKey(String raw, String defaultKey) {
        return resolveKeyName(QolKeybindNames.canonicalKeyName(raw, defaultKey));
    }

    public static int resolveKeyName(String name) {
        if (name == null || name.isBlank()) {
            return InputConstants.UNKNOWN.getValue();
        }
        return switch (name) {
            case "RIGHT_SHIFT" -> InputConstants.KEY_RSHIFT;
            case "LEFT_SHIFT" -> InputConstants.KEY_LSHIFT;
            case "RIGHT_CONTROL" -> InputConstants.KEY_RCONTROL;
            case "LEFT_CONTROL" -> InputConstants.KEY_LCONTROL;
            case "RIGHT_ALT" -> InputConstants.KEY_RALT;
            case "LEFT_ALT" -> InputConstants.KEY_LALT;
            case "RIGHT_SUPER" -> InputConstants.KEY_RGUI;
            case "LEFT_SUPER" -> InputConstants.KEY_LGUI;
            case "ESCAPE" -> InputConstants.KEY_ESCAPE;
            case "TAB" -> InputConstants.KEY_TAB;
            case "SPACE" -> InputConstants.KEY_SPACE;
            case "ENTER" -> InputConstants.KEY_RETURN;
            case "BACKSPACE" -> InputConstants.KEY_BACKSPACE;
            case "INSERT" -> InputConstants.KEY_INSERT;
            case "DELETE" -> InputConstants.KEY_DELETE;
            case "HOME" -> InputConstants.KEY_HOME;
            case "END" -> InputConstants.KEY_END;
            case "PAGE_UP" -> InputConstants.KEY_PAGEUP;
            case "PAGE_DOWN" -> InputConstants.KEY_PAGEDOWN;
            case "UP" -> InputConstants.KEY_UP;
            case "DOWN" -> InputConstants.KEY_DOWN;
            case "LEFT" -> InputConstants.KEY_LEFT;
            case "RIGHT" -> InputConstants.KEY_RIGHT;
            case "MINUS" -> InputConstants.KEY_MINUS;
            case "EQUAL" -> InputConstants.KEY_EQUALS;
            case "LEFT_BRACKET" -> InputConstants.KEY_LBRACKET;
            case "RIGHT_BRACKET" -> InputConstants.KEY_RBRACKET;
            case "CAPS_LOCK" -> InputConstants.KEY_CAPSLOCK;
            case "A" -> InputConstants.KEY_A;
            case "B" -> InputConstants.KEY_B;
            case "C" -> InputConstants.KEY_C;
            case "D" -> InputConstants.KEY_D;
            case "E" -> InputConstants.KEY_E;
            case "F" -> InputConstants.KEY_F;
            case "G" -> InputConstants.KEY_G;
            case "H" -> InputConstants.KEY_H;
            case "I" -> InputConstants.KEY_I;
            case "J" -> InputConstants.KEY_J;
            case "K" -> InputConstants.KEY_K;
            case "L" -> InputConstants.KEY_L;
            case "M" -> InputConstants.KEY_M;
            case "N" -> InputConstants.KEY_N;
            case "O" -> InputConstants.KEY_O;
            case "P" -> InputConstants.KEY_P;
            case "Q" -> InputConstants.KEY_Q;
            case "R" -> InputConstants.KEY_R;
            case "S" -> InputConstants.KEY_S;
            case "T" -> InputConstants.KEY_T;
            case "U" -> InputConstants.KEY_U;
            case "V" -> InputConstants.KEY_V;
            case "W" -> InputConstants.KEY_W;
            case "X" -> InputConstants.KEY_X;
            case "Y" -> InputConstants.KEY_Y;
            case "Z" -> InputConstants.KEY_Z;
            case "0" -> InputConstants.KEY_0;
            case "1" -> InputConstants.KEY_1;
            case "2" -> InputConstants.KEY_2;
            case "3" -> InputConstants.KEY_3;
            case "4" -> InputConstants.KEY_4;
            case "5" -> InputConstants.KEY_5;
            case "6" -> InputConstants.KEY_6;
            case "7" -> InputConstants.KEY_7;
            case "8" -> InputConstants.KEY_8;
            case "9" -> InputConstants.KEY_9;
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
            default -> InputConstants.UNKNOWN.getValue();
        };
    }

    public static Integer resolveMouseButton(String raw) {
        String mouse = QolKeybindNames.canonicalMouseName(raw);
        if (mouse == null) {
            return null;
        }
        return switch (mouse) {
            case "LMB" -> InputConstants.MOUSE_BUTTON_LEFT;
            case "RMB" -> InputConstants.MOUSE_BUTTON_RIGHT;
            case "MMB" -> InputConstants.MOUSE_BUTTON_MIDDLE;
            case "MOUSE_4" -> InputConstants.MOUSE_BUTTON_4;
            case "MOUSE_5" -> InputConstants.MOUSE_BUTTON_5;
            default -> null;
        };
    }

    public static boolean isKeyDown(long windowHandle, int key) {
        return windowHandle != 0L
                && key != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(key);
    }

    public static boolean isBoundDown(long windowHandle, String raw) {
        if (windowHandle == 0L || raw == null || raw.isBlank()) {
            return false;
        }
        Integer mouse = resolveMouseButton(raw);
        if (mouse != null) {
            return MOUSE_DOWN.contains(mouse);
        }
        return isKeyDown(windowHandle, resolveGlfwKey(raw, ""));
    }

    public static void updateMouseButton(int button, int action) {
        if (action == InputConstants.RELEASE) {
            MOUSE_DOWN.remove(button);
        } else if (action == InputConstants.PRESS) {
            MOUSE_DOWN.add(button);
        }
    }

    public static boolean isMouseDown(int button) {
        return MOUSE_DOWN.contains(button);
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
            case InputConstants.KEY_LBRACKET -> "LEFT_BRACKET";
            case InputConstants.KEY_RBRACKET -> "RIGHT_BRACKET";
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
