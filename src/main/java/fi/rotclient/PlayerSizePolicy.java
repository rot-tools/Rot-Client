package fi.rotclient;

/**
 * Player Size is render-matrix only. Defaults 1/1/1. Does not expose hitbox /
 * collision / reach / eye-height gameplay geometry changes.
 *
 * Scope: local player visual only (no scope setting in catalog/config).
 */
public final class PlayerSizePolicy {
    public static final float MIN_SCALE = 0.1F;
    public static final float MAX_SCALE = 2.0F;
    public static final float DEFAULT_SCALE = 1.0F;

    public record Scale(float x, float y, float z) {
        public Scale {
            x = clamp(x);
            y = clamp(y);
            z = clamp(z);
        }

        public boolean isIdentity() {
            return x == DEFAULT_SCALE && y == DEFAULT_SCALE && z == DEFAULT_SCALE;
        }
    }

    private PlayerSizePolicy() {
    }

    public static float clamp(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_SCALE;
        }
        if (value < MIN_SCALE) {
            return MIN_SCALE;
        }
        if (value > MAX_SCALE) {
            return MAX_SCALE;
        }
        return value;
    }

    public static Scale resolve(
            boolean moduleEnabled,
            float x,
            float y,
            float z) {
        if (!moduleEnabled) {
            return new Scale(DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE);
        }
        return new Scale(x, y, z);
    }

    /** Policy documents that gameplay geometry must remain vanilla. */
    public static boolean exposesGameplayGeometryChanges() {
        return false;
    }
}
