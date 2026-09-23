package fi.rotclient;

import java.util.Locale;

/**
 * Version-independent keybind name normalization used by config and policy
 * code. Live Minecraft input codes belong in the client source set.
 */
public final class QolKeybindNames {
    private QolKeybindNames() {
    }

    public static String canonicalKeyName(String raw, String defaultKey) {
        String token = raw == null || raw.isBlank() ? defaultKey : raw;
        if (token == null || token.isBlank()) {
            return "";
        }
        String key = normalize(token);
        String mouse = canonicalMouseName(key);
        if (mouse != null) {
            return mouse;
        }
        return switch (key) {
            case "RIGHT_SHIFT", "RSHIFT" -> "RIGHT_SHIFT";
            case "LEFT_SHIFT", "LSHIFT", "SHIFT" -> "LEFT_SHIFT";
            case "RIGHT_CONTROL", "RCTRL", "RIGHT_CTRL" -> "RIGHT_CONTROL";
            case "LEFT_CONTROL", "LCTRL", "LEFT_CTRL", "CTRL", "CONTROL" -> "LEFT_CONTROL";
            case "RIGHT_ALT", "RALT" -> "RIGHT_ALT";
            case "LEFT_ALT", "LALT", "ALT" -> "LEFT_ALT";
            case "RIGHT_SUPER", "RSUPER", "RIGHT_GUI", "RGUI" -> "RIGHT_SUPER";
            case "LEFT_SUPER", "LSUPER", "SUPER", "LEFT_GUI", "LGUI" -> "LEFT_SUPER";
            case "ESCAPE", "ESC" -> "ESCAPE";
            case "TAB" -> "TAB";
            case "SPACE", "SPACEBAR" -> "SPACE";
            case "ENTER", "RETURN" -> "ENTER";
            case "BACKSPACE" -> "BACKSPACE";
            case "INSERT" -> "INSERT";
            case "DELETE", "DEL" -> "DELETE";
            case "HOME" -> "HOME";
            case "END" -> "END";
            case "PAGE_UP", "PGUP" -> "PAGE_UP";
            case "PAGE_DOWN", "PGDN", "PGDOWN" -> "PAGE_DOWN";
            case "UP", "ARROW_UP" -> "UP";
            case "DOWN", "ARROW_DOWN" -> "DOWN";
            case "LEFT", "ARROW_LEFT" -> "LEFT";
            case "RIGHT", "ARROW_RIGHT" -> "RIGHT";
            case "MINUS", "HYPHEN" -> "MINUS";
            case "EQUAL", "EQUALS" -> "EQUAL";
            case "LEFT_BRACKET", "LBRACKET" -> "LEFT_BRACKET";
            case "RIGHT_BRACKET", "RBRACKET" -> "RIGHT_BRACKET";
            case "CAPS_LOCK", "CAPSLOCK" -> "CAPS_LOCK";
            default -> isSimpleKey(key) ? key : "";
        };
    }

    public static String canonicalMouseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (normalize(raw)) {
            case "MOUSE_LEFT", "LMB", "LEFT_MOUSE", "MOUSE_1" -> "LMB";
            case "MOUSE_RIGHT", "RMB", "RIGHT_MOUSE", "MOUSE_2" -> "RMB";
            case "MOUSE_MIDDLE", "MMB", "MIDDLE_MOUSE", "MOUSE_3" -> "MMB";
            case "MOUSE_4", "MOUSE4" -> "MOUSE_4";
            case "MOUSE_5", "MOUSE5" -> "MOUSE_5";
            default -> null;
        };
    }

    public static boolean sameBind(String left, String right) {
        if (left == null || left.isBlank() || right == null || right.isBlank()) {
            return false;
        }
        String leftName = canonicalKeyName(left, "");
        String rightName = canonicalKeyName(right, "");
        return !leftName.isEmpty() && leftName.equals(rightName);
    }

    private static boolean isSimpleKey(String key) {
        if (key.length() == 1) {
            char c = key.charAt(0);
            return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
        }
        if (key.length() >= 2 && key.charAt(0) == 'F') {
            try {
                int f = Integer.parseInt(key.substring(1));
                return f >= 1 && f <= 24;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }
}
