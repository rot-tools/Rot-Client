package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Minecraft-free § / §x handling. 26.2 {@code Component.literal} keeps the
 * section sign as a glyph, so HUD rows have to be parsed into styled spans
 * before they are drawn.
 */
public final class LegacyMcText {
    private static final Pattern HEX = Pattern.compile("(?i)§x(?:§[0-9a-f]){6}");
    private static final Pattern CODE = Pattern.compile("(?i)§[0-9a-fk-or]");

    public record Span(
            String text,
            int rgb,
            boolean bold,
            boolean italic,
            boolean underline,
            boolean strike,
            boolean obfuscated) {
        public Span {
            text = text == null ? "" : text;
        }

        public boolean hasColor() {
            return rgb >= 0;
        }
    }

    private LegacyMcText() {
    }

    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String out = HEX.matcher(text).replaceAll("");
        out = CODE.matcher(out).replaceAll("");
        return out.replace('\u00a0', ' ').trim();
    }

    public static String encodeColor(int rgb) {
        int value = rgb & 0xFFFFFF;
        char vanilla = paletteCode(value);
        if (vanilla != 0) {
            return "§" + vanilla;
        }
        String hex = String.format(Locale.US, "%06x", value);
        StringBuilder out = new StringBuilder("§x");
        for (int i = 0; i < hex.length(); i++) {
            out.append('§').append(hex.charAt(i));
        }
        return out.toString();
    }

    public static char paletteCode(int rgb) {
        return switch (rgb & 0xFFFFFF) {
            case 0x000000 -> '0';
            case 0x0000AA -> '1';
            case 0x00AA00 -> '2';
            case 0x00AAAA -> '3';
            case 0xAA0000 -> '4';
            case 0xAA00AA -> '5';
            case 0xFFAA00 -> '6';
            case 0xAAAAAA -> '7';
            case 0x555555 -> '8';
            case 0x5555FF -> '9';
            case 0x55FF55 -> 'a';
            case 0x55FFFF -> 'b';
            case 0xFF5555 -> 'c';
            case 0xFF55FF -> 'd';
            case 0xFFFF55 -> 'e';
            case 0xFFFFFF -> 'f';
            default -> 0;
        };
    }

    public static int paletteRgb(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> -1;
        };
    }

    public static List<Span> parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Span> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int rgb = -1;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strike = false;
        boolean obfuscated = false;
        int i = 0;
        while (i < raw.length()) {
            char current = raw.charAt(i);
            if (current == '§' && i + 1 < raw.length()) {
                char code = raw.charAt(i + 1);
                if (code == 'x' || code == 'X') {
                    int[] consumed = new int[1];
                    int hex = readHex(raw, i + 2, consumed);
                    if (hex >= 0) {
                        flush(out, buf, rgb, bold, italic, underline, strike, obfuscated);
                        rgb = hex;
                        i += 2 + consumed[0];
                        continue;
                    }
                }
                int palette = paletteRgb(code);
                if (palette >= 0) {
                    flush(out, buf, rgb, bold, italic, underline, strike, obfuscated);
                    rgb = palette;
                    i += 2;
                    continue;
                }
                char lower = Character.toLowerCase(code);
                if (lower == 'l' || lower == 'o' || lower == 'n' || lower == 'm'
                        || lower == 'k' || lower == 'r') {
                    flush(out, buf, rgb, bold, italic, underline, strike, obfuscated);
                    switch (lower) {
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        case 'n' -> underline = true;
                        case 'm' -> strike = true;
                        case 'k' -> obfuscated = true;
                        case 'r' -> {
                            rgb = -1;
                            bold = false;
                            italic = false;
                            underline = false;
                            strike = false;
                            obfuscated = false;
                        }
                        default -> {
                        }
                    }
                    i += 2;
                    continue;
                }
            }
            buf.append(current);
            i++;
        }
        flush(out, buf, rgb, bold, italic, underline, strike, obfuscated);
        return List.copyOf(out);
    }

    private static int readHex(String raw, int start, int[] consumed) {
        int[] nibbles = new int[6];
        int pos = start;
        for (int n = 0; n < 6; n++) {
            if (pos >= raw.length()) {
                return -1;
            }
            if (raw.charAt(pos) == '§') {
                pos++;
                if (pos >= raw.length()) {
                    return -1;
                }
            }
            int value = hexValue(raw.charAt(pos));
            if (value < 0) {
                return -1;
            }
            nibbles[n] = value;
            pos++;
        }
        consumed[0] = pos - start;
        return (nibbles[0] << 20)
                | (nibbles[1] << 16)
                | (nibbles[2] << 12)
                | (nibbles[3] << 8)
                | (nibbles[4] << 4)
                | nibbles[5];
    }

    private static int hexValue(char code) {
        if (code >= '0' && code <= '9') {
            return code - '0';
        }
        char lower = Character.toLowerCase(code);
        if (lower >= 'a' && lower <= 'f') {
            return 10 + (lower - 'a');
        }
        return -1;
    }

    private static void flush(
            List<Span> out,
            StringBuilder buf,
            int rgb,
            boolean bold,
            boolean italic,
            boolean underline,
            boolean strike,
            boolean obfuscated) {
        if (buf.isEmpty()) {
            return;
        }
        out.add(new Span(buf.toString(), rgb, bold, italic, underline, strike, obfuscated));
        buf.setLength(0);
    }
}
