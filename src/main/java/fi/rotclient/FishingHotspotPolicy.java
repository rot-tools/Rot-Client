package fi.rotclient;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hotspot nametag circles and observed despawn notices.
 */
public final class FishingHotspotPolicy {
    public record Point(double x, double y, double z) {
    }

    public record Circle(double x, double y, double z, double radius) {
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

    /**
     * A hotspot only counts as despawned if it was well inside the scan range
     * last time, otherwise sailing away from it looks the same as it vanishing.
     */
    public static boolean vanishedNearby(
            List<Circle> previous,
            Set<String> currentKeys,
            double playerX,
            double playerZ,
            double maxDistance) {
        if (previous == null || previous.isEmpty() || currentKeys == null) {
            return false;
        }
        double limit = maxDistance * maxDistance;
        for (Circle circle : previous) {
            if (currentKeys.contains(key(circle.x(), circle.z()))) {
                continue;
            }
            double dx = circle.x() - playerX;
            double dz = circle.z() - playerZ;
            if (dx * dx + dz * dz <= limit) {
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

    public static boolean shouldHideParticle(
            boolean moduleEnabled,
            boolean hideParticles,
            boolean nearHotspot) {
        return moduleEnabled && hideParticles && nearHotspot;
    }
}
