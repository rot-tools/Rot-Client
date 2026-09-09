package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure bow/pearl ballistic simulation using tick-step physics.
 */
public final class TrajectoryPredictor {
    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 120;
    public static final int DEFAULT_RANGE = 30;

    public record Vec3d(double x, double y, double z) {
        public Vec3d add(Vec3d other) {
            return new Vec3d(x + other.x, y + other.y, z + other.z);
        }

        public Vec3d scale(double factor) {
            return new Vec3d(x * factor, y * factor, z * factor);
        }
    }

    public enum ProjectileKind {
        BOW,
        PEARL
    }

    public record Hit(
            Vec3d point,
            int blockX,
            int blockY,
            int blockZ,
            String face) {
    }

    public record Plane(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
    }

    public static Plane impactPlane(Hit hit, double planeSize) {
        Vec3d point = hit == null ? new Vec3d(0, 0, 0) : hit.point();
        double s = 0.15D * Math.max(0.5D, planeSize);
        String face = hit == null || hit.face() == null
                ? "up"
                : hit.face().toLowerCase(Locale.ROOT);
        return switch (face) {
            case "north", "south" -> new Plane(
                    point.x() - s, point.y() - s, point.z() - 0.02D,
                    point.x() + s, point.y() + s, point.z() + 0.02D);
            case "east", "west" -> new Plane(
                    point.x() - 0.02D, point.y() - s, point.z() - s,
                    point.x() + 0.02D, point.y() + s, point.z() + s);
            default -> new Plane(
                    point.x() - s, point.y() - 0.02D, point.z() - s,
                    point.x() + s, point.y() + 0.02D, point.z() + s);
        };
    }

    public record Result(List<Vec3d> points, Optional<Hit> hit) {
        public Result {
            points = points == null ? List.of() : List.copyOf(points);
            hit = hit == null ? Optional.empty() : hit;
        }
    }

    public interface Occupancy {
        boolean isSolid(int x, int y, int z);
    }

    /**
     * One ballistic step. The client uses vanilla world clip; unit tests keep
     * a Minecraft-free occupancy grid.
     */
    @FunctionalInterface
    public interface SegmentClipper {
        Optional<Hit> clip(Vec3d from, Vec3d to);
    }

    private TrajectoryPredictor() {
    }

    public static int clampRange(int range) {
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, range));
    }

    public static float bowPull(int ticksUsingItem) {
        return bowPull((float) ticksUsingItem);
    }

    public static float bowPull(float ticksUsingItem) {
        float t = Math.max(0.0F, Math.min(1.0F, ticksUsingItem / 20.0F));
        return (t * t + t * 2.0F) / 3.0F;
    }

    public static float lerp(float previous, float next, float partialTick) {
        float t = partialTick;
        if (!Float.isFinite(t)) {
            t = 1.0F;
        }
        t = Math.max(0.0F, Math.min(1.0F, t));
        return previous + (next - previous) * t;
    }

    public static Vec3d lerp(Vec3d previous, Vec3d next, float partialTick) {
        Vec3d from = previous == null ? new Vec3d(0.0D, 0.0D, 0.0D) : previous;
        Vec3d to = next == null ? from : next;
        float t = lerp(0.0F, 1.0F, partialTick);
        return new Vec3d(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t);
    }

    public static Optional<ProjectileKind> detectHeld(
            boolean bowsEnabled,
            boolean pearlsEnabled,
            String heldName) {
        if (heldName == null || heldName.isBlank()) {
            return Optional.empty();
        }
        String text = heldName.toLowerCase(Locale.ROOT);
        if (pearlsEnabled && text.contains("pearl")) {
            return Optional.of(ProjectileKind.PEARL);
        }
        if (bowsEnabled && (text.contains("bow")
                || text.contains("terminator")
                || text.contains("juju")
                || text.contains("shortbow"))) {
            return Optional.of(ProjectileKind.BOW);
        }
        return Optional.empty();
    }

    public static boolean isTerminatorId(String skyBlockId) {
        return AutoClickerItemIdentity.TERMINATOR_ID.equals(skyBlockId);
    }

    /**
     * Terminator fires three arrows at yaw offsets −5°, 0°, +5°.
     */
    public static float[] terminatorYawOffsets() {
        return new float[] {-5.0F, 0.0F, 5.0F};
    }

    public static Vec3d look(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(-yawDeg) - Math.PI;
        double pitch = Math.toRadians(-pitchDeg);
        double f = -Math.cos(pitch);
        return new Vec3d(Math.sin(yaw) * f, Math.sin(pitch), Math.cos(yaw) * f);
    }

    public static Vec3d startPos(Vec3d eye, float yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        double x = -Math.cos(yaw) * 0.16D;
        double z = -Math.sin(yaw) * 0.16D;
        return new Vec3d(eye.x + x, eye.y - 0.1D, eye.z + z);
    }

    public static Result simulate(
            ProjectileKind kind,
            Vec3d start,
            float yawDeg,
            float pitchDeg,
            float bowPull,
            int range,
            Occupancy occupancy) {
        if (kind == null || start == null || occupancy == null) {
            return new Result(List.of(), Optional.empty());
        }
        return simulateWithClip(
                kind,
                start,
                yawDeg,
                pitchDeg,
                bowPull,
                range,
                (from, to) -> clip(from, to, occupancy));
    }

    public static Result simulateWithClip(
            ProjectileKind kind,
            Vec3d start,
            float yawDeg,
            float pitchDeg,
            float bowPull,
            int range,
            SegmentClipper clipper) {
        if (kind == null || start == null || clipper == null) {
            return new Result(List.of(), Optional.empty());
        }
        boolean pearl = kind == ProjectileKind.PEARL;
        double speed = pearl ? 1.5D : Math.max(0.1D, bowPull) * 3.0D;
        Vec3d motion = look(yawDeg, pitchDeg).scale(speed);
        Vec3d pos = start;
        List<Vec3d> points = new ArrayList<>();
        int ticks = clampRange(range);
        for (int i = 0; i < ticks; i++) {
            points.add(pos);
            Vec3d next = pos.add(motion);
            Optional<Hit> hit = clipper.clip(pos, next);
            if (hit.isPresent()) {
                points.add(hit.get().point());
                return new Result(points, hit);
            }
            pos = next;
            if (pearl) {
                motion = new Vec3d(
                        motion.x * 0.99D,
                        (motion.y - 0.03D) * 0.99D,
                        motion.z * 0.99D);
            } else {
                motion = new Vec3d(
                        motion.x * 0.99D,
                        motion.y * 0.99D - 0.05D,
                        motion.z * 0.99D);
            }
        }
        return new Result(points, Optional.empty());
    }

    static Optional<Hit> clip(Vec3d from, Vec3d to, Occupancy occupancy) {
        int x0 = floor(from.x);
        int y0 = floor(from.y);
        int z0 = floor(from.z);
        int x1 = floor(to.x);
        int y1 = floor(to.y);
        int z1 = floor(to.z);
        if (occupancy.isSolid(x0, y0, z0)) {
            return Optional.of(new Hit(from, x0, y0, z0, "inside"));
        }
        int steps = Math.abs(x1 - x0) + Math.abs(y1 - y0) + Math.abs(z1 - z0);
        steps = Math.max(1, Math.min(64, steps * 2));
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;
            int bx = floor(x);
            int by = floor(y);
            int bz = floor(z);
            if (occupancy.isSolid(bx, by, bz)) {
                String face = face(from, new Vec3d(x, y, z));
                return Optional.of(new Hit(new Vec3d(x, y, z), bx, by, bz, face));
            }
        }
        return Optional.empty();
    }

    private static String face(Vec3d from, Vec3d hit) {
        double dx = Math.abs(hit.x - from.x);
        double dy = Math.abs(hit.y - from.y);
        double dz = Math.abs(hit.z - from.z);
        if (dy >= dx && dy >= dz) {
            return hit.y >= from.y ? "up" : "down";
        }
        if (dx >= dz) {
            return hit.x >= from.x ? "east" : "west";
        }
        return hit.z >= from.z ? "south" : "north";
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
