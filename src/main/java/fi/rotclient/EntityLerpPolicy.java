package fi.rotclient;

/**
 * Shared camera-tick interpolation used by ESP boxes so they follow the
 * rendered entity instead of the last game-tick snapshot.
 */
public final class EntityLerpPolicy {
    private EntityLerpPolicy() {
    }

    public record Offset(double x, double y, double z) {
    }

    public static double lerp(double previous, double current, float partialTick) {
        if (!Float.isFinite(partialTick)) {
            return current;
        }
        float t = Math.max(0.0F, Math.min(1.0F, partialTick));
        return previous + (current - previous) * t;
    }

    public static Offset renderOffset(
            double x,
            double y,
            double z,
            double previousX,
            double previousY,
            double previousZ,
            float partialTick) {
        return new Offset(
                lerp(previousX, x, partialTick) - x,
                lerp(previousY, y, partialTick) - y,
                lerp(previousZ, z, partialTick) - z);
    }
}
