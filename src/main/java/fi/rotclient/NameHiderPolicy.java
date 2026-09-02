package fi.rotclient;

import java.util.Locale;

/**
 * Local-only rewrite of the player's own Minecraft username. The server and
 * other players still see the real name; this only decides what the client
 * paints on screen.
 */
public final class NameHiderPolicy {
    public static final int MAX_CUSTOM_LENGTH = 24;
    public static final String MODE_SCRAMBLE = "Scramble";
    public static final String MODE_CUSTOM = "Custom";

    /** Glyphs that the vanilla Minecraft font can draw, used as cryptic clutter. */
    private static final char[] SCRAMBLE_GLYPHS =
            "#@$%&?*+=~^<>/\\|".toCharArray();

    public enum Mode {
        SCRAMBLE,
        CUSTOM;

        public static Mode fromConfig(String raw) {
            if (raw != null && raw.equalsIgnoreCase(MODE_CUSTOM)) {
                return CUSTOM;
            }
            return SCRAMBLE;
        }
    }

    private NameHiderPolicy() {
    }

    public static String sanitizeCustomName(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        text = text.replaceAll("§.", "");
        if (text.length() > MAX_CUSTOM_LENGTH) {
            text = text.substring(0, MAX_CUSTOM_LENGTH);
        }
        return text;
    }

    public static String displayName(Mode mode, String username, String customName) {
        if (username == null || username.isBlank()) {
            return "";
        }
        if (mode == Mode.CUSTOM) {
            String custom = sanitizeCustomName(customName).trim();
            if (!custom.isEmpty()) {
                return custom;
            }
        }
        return scramble(username);
    }

    /**
     * Stable per-letter cipher so the same username always becomes the same mess.
     */
    public static String scramble(String username) {
        if (username == null || username.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(username.length());
        for (int i = 0; i < username.length(); i++) {
            int hash = (username.charAt(i) * 31 + i * 17) & 0x7fffffff;
            out.append(SCRAMBLE_GLYPHS[hash % SCRAMBLE_GLYPHS.length]);
        }
        return out.toString();
    }

    public static boolean containsUsername(String text, String username) {
        return indexOfUsername(text, username) >= 0;
    }

    public static String replaceUsername(String text, String username, String display) {
        if (text == null || text.isEmpty() || username == null || username.length() < 3) {
            return text;
        }
        if (display == null) {
            display = "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        int match;
        while ((match = indexOfUsername(text, username, cursor)) >= 0) {
            out.append(text, cursor, match);
            out.append(display);
            cursor = match + username.length();
        }
        out.append(text, cursor, text.length());
        return out.toString();
    }

    public static int indexOfUsername(String text, String username) {
        return indexOfUsername(text, username, 0);
    }

    public static int indexOfUsername(String text, String username, int from) {
        if (text == null || username == null || username.length() < 3) {
            return -1;
        }
        int nameLen = username.length();
        int end = text.length() - nameLen;
        for (int i = Math.max(0, from); i <= end; i++) {
            if (!regionMatchesIgnoreCase(text, i, username)) {
                continue;
            }
            if (isNameBoundary(text, i - 1) && isNameBoundary(text, i + nameLen)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean regionMatchesIgnoreCase(String text, int offset, String username) {
        return text.regionMatches(true, offset, username, 0, username.length());
    }

    /**
     * Minecraft usernames are [A-Za-z0-9_]. Treat any other character (or the
     * string edge) as a boundary so {@code Alex} does not match {@code Alexander}.
     */
    private static boolean isNameBoundary(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return true;
        }
        char c = text.charAt(index);
        return !(c >= 'A' && c <= 'Z')
                && !(c >= 'a' && c <= 'z')
                && !(c >= '0' && c <= '9')
                && c != '_';
    }

    static String normalizeMode(String raw) {
        return Mode.fromConfig(raw) == Mode.CUSTOM ? MODE_CUSTOM : MODE_SCRAMBLE;
    }
}
