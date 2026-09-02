package fi.rotclient;

/**
 * HSV helpers for the in-mod color picker.
 */
final class RotClientColorMath {
    private RotClientColorMath() {
    }

    record Hsv(float h, float s, float v) {
    }

    static Hsv fromArgb(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta <= 0.00001F) {
            h = 0.0F;
        } else if (max == r) {
            h = 60.0F * (((g - b) / delta) % 6.0F);
        } else if (max == g) {
            h = 60.0F * (((b - r) / delta) + 2.0F);
        } else {
            h = 60.0F * (((r - g) / delta) + 4.0F);
        }
        if (h < 0.0F) {
            h += 360.0F;
        }
        float s = max <= 0.00001F ? 0.0F : delta / max;
        return new Hsv(h, s, max);
    }

    static int toArgb(float h, float s, float v, int alpha) {
        float hh = ((h % 360.0F) + 360.0F) % 360.0F;
        float ss = clamp01(s);
        float vv = clamp01(v);
        float c = vv * ss;
        float x = c * (1.0F - Math.abs((hh / 60.0F) % 2.0F - 1.0F));
        float m = vv - c;
        float r;
        float g;
        float b;
        if (hh < 60.0F) {
            r = c;
            g = x;
            b = 0.0F;
        } else if (hh < 120.0F) {
            r = x;
            g = c;
            b = 0.0F;
        } else if (hh < 180.0F) {
            r = 0.0F;
            g = c;
            b = x;
        } else if (hh < 240.0F) {
            r = 0.0F;
            g = x;
            b = c;
        } else if (hh < 300.0F) {
            r = x;
            g = 0.0F;
            b = c;
        } else {
            r = c;
            g = 0.0F;
            b = x;
        }
        int ri = Math.round((r + m) * 255.0F);
        int gi = Math.round((g + m) * 255.0F);
        int bi = Math.round((b + m) * 255.0F);
        return ((alpha & 0xFF) << 24)
                | ((ri & 0xFF) << 16)
                | ((gi & 0xFF) << 8)
                | (bi & 0xFF);
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }
}
