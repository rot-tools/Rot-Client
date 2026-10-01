package fi.rotclient;

/** Bounded candidate footprint; server shape and projectile mechanics require playtest calibration. */
final class PickobulusPreviewPolicy {
    static boolean inside(int dx, int dy, int dz, int radius, boolean sphere) {
        radius = Math.max(1, Math.min(5, radius));
        if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) return false;
        return !sphere || dx * dx + dy * dy + dz * dz <= radius * radius;
    }
    private PickobulusPreviewPolicy() {}
}
