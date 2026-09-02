package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hotspot nametag circles plus a local hotspot-radar flame trail.
 */
public final class FishingHotspotPolicy {
    public static final int MAX_RADAR_POINTS = 24;
    public static final double RADAR_SPEED_EPS = 0.02D;

    public record Point(double x, double y, double z) {
    }

    public record Circle(double x, double y, double z, double radius) {
    }

    public record RadarGuess(double x, double y, double z, double dx, double dy, double dz) {
    }

    private FishingHotspotPolicy() {
    }

    public static boolean isHotspotNametag(String nametag) {
        String text = FishingCreaturesPolicy.strip(nametag).toLowerCase(Locale.ROOT);
        return text.contains("hotspot")
                || (text.contains("fishing speed") && text.contains("%"));
    }

    public static String key(double x, double z) {
        return Math.round(x) + "/" + Math.round(z);
    }

    public static boolean vanished(Set<String> previous, Set<String> current) {
        if (previous == null || previous.isEmpty() || current == null) {
            return false;
        }
        for (String key : previous) {
            if (!current.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> keys(List<Circle> circles) {
        Set<String> out = new HashSet<>();
        if (circles == null) {
            return out;
        }
        for (Circle circle : circles) {
            out.add(key(circle.x(), circle.z()));
        }
        return out;
    }

    public static Circle circleFromStand(double x, double y, double z, double nametagRadius) {
        double radius = nametagRadius > 0.5D ? nametagRadius : 8.0D;
        return new Circle(x, y, z, radius);
    }

    public static boolean isRadarFlame(String particleId, double xSpeed, double ySpeed, double zSpeed) {
        if (particleId == null) {
            return false;
        }
        String id = particleId.toLowerCase(Locale.ROOT);
        if (!id.contains("flame") && !id.contains("dust")) {
            return false;
        }
        double speed = Math.abs(xSpeed) + Math.abs(ySpeed) + Math.abs(zSpeed);
        return speed <= RADAR_SPEED_EPS;
    }

    public static List<Point> pushRadar(List<Point> current, Point next) {
        List<Point> out = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (next != null) {
            out.add(next);
        }
        while (out.size() > MAX_RADAR_POINTS) {
            out.remove(0);
        }
        return List.copyOf(out);
    }

    public static RadarGuess guess(List<Point> trail) {
        if (trail == null || trail.size() < 3) {
            return null;
        }
        Point a = trail.get(0);
        Point b = trail.get(trail.size() - 1);
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double dz = b.z() - a.z();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.25D) {
            return null;
        }
        double scale = 24.0D / len;
        return new RadarGuess(b.x(), b.y(), b.z(), dx * scale, dy * scale, dz * scale);
    }

    public static boolean shouldHideParticle(
            boolean moduleEnabled,
            boolean hideParticles,
            boolean nearHotspot) {
        return moduleEnabled && hideParticles && nearHotspot;
    }
}
