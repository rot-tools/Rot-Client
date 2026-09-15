package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plus-only inference from still radar particles. */
public final class FishingHotspotRadarPolicy {
    public static final int MAX_RADAR_POINTS = 24;
    public static final double RADAR_SPEED_EPS = 0.02D;

    public record RadarGuess(double x, double y, double z, double dx, double dy, double dz) { }

    private FishingHotspotRadarPolicy() { }

    public static boolean isRadarFlame(String particleId, double xSpeed, double ySpeed, double zSpeed) {
        if (particleId == null) return false;
        String id = particleId.toLowerCase(Locale.ROOT);
        if (!id.contains("flame") && !id.contains("dust")) return false;
        return Math.abs(xSpeed) + Math.abs(ySpeed) + Math.abs(zSpeed) <= RADAR_SPEED_EPS;
    }

    public static List<FishingHotspotPolicy.Point> pushRadar(
            List<FishingHotspotPolicy.Point> current, FishingHotspotPolicy.Point next) {
        List<FishingHotspotPolicy.Point> out = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (next != null) out.add(next);
        while (out.size() > MAX_RADAR_POINTS) out.removeFirst();
        return List.copyOf(out);
    }

    public static RadarGuess guess(List<FishingHotspotPolicy.Point> trail) {
        if (trail == null || trail.size() < 3) return null;
        FishingHotspotPolicy.Point a = trail.getFirst();
        FishingHotspotPolicy.Point b = trail.getLast();
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double dz = b.z() - a.z();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.25D) return null;
        double scale = 24.0D / len;
        return new RadarGuess(b.x(), b.y(), b.z(), dx * scale, dy * scale, dz * scale);
    }
}
