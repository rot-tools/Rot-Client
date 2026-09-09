package fi.rotclient;

/**
 * Test-server Free Camera: detach the view from the standing player. Pure
 * helpers for toggle, speed, look, and fly math live here so mixins stay thin.
 */
public final class FreecamPolicy {
    public static final double MIN_SPEED = 0.05D;
    public static final double MAX_SPEED = 5.0D;
    public static final double DEFAULT_SPEED = 1.0D;
    public static final double SPRINT_MULTIPLIER = 2.0D;
    public static final double HIT_NUDGE = 0.05D;

    public record Vec3d(double x, double y, double z) {
        public static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);

        public Vec3d add(Vec3d other) {
            return new Vec3d(x + other.x, y + other.y, z + other.z);
        }

        public Vec3d scale(double factor) {
            return new Vec3d(x * factor, y * factor, z * factor);
        }

        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }
    }

    public record Pose(double x, double y, double z, float yaw, float pitch) {
    }

    private FreecamPolicy() {
    }

    public static double clampSpeed(double speed) {
        if (!Double.isFinite(speed)) {
            return DEFAULT_SPEED;
        }
        if (speed < MIN_SPEED) {
            return MIN_SPEED;
        }
        if (speed > MAX_SPEED) {
            return MAX_SPEED;
        }
        return speed;
    }

    public static float clampPitch(float pitch) {
        if (!Float.isFinite(pitch)) {
            return 0.0F;
        }
        if (pitch < -90.0F) {
            return -90.0F;
        }
        if (pitch > 90.0F) {
            return 90.0F;
        }
        return pitch;
    }

    public static boolean shouldRun(boolean moduleEnabled, boolean inWorld) {
        return moduleEnabled && inWorld;
    }

    public static boolean shouldBlockMovePacket(
            boolean active,
            boolean hasPosition,
            boolean hasRotation) {
        return active && (hasPosition || hasRotation);
    }

    public static boolean shouldHideLocalBody(
            boolean active,
            boolean showBody,
            boolean isLocalPlayer) {
        return active && !showBody && isLocalPlayer;
    }

    public static boolean shouldNoclip(boolean collideWithBlocks) {
        return !collideWithBlocks;
    }

    /**
     * Look-aligned spectator fly. Forward follows pitch; space/sneak are world
     * up/down. Sprint doubles the clamped speed. Opposite keys cancel.
     */
    public static Vec3d flyDelta(
            boolean forward,
            boolean back,
            boolean left,
            boolean right,
            boolean up,
            boolean down,
            boolean sprint,
            double yawDeg,
            double pitchDeg,
            double speed) {
        int fwd = (forward ? 1 : 0) - (back ? 1 : 0);
        int strafe = (left ? 1 : 0) - (right ? 1 : 0);
        int vert = (up ? 1 : 0) - (down ? 1 : 0);
        if (fwd == 0 && strafe == 0 && vert == 0) {
            return Vec3d.ZERO;
        }
        Vec3d look = fromPolar(pitchDeg, yawDeg);
        Vec3d leftVec = fromPolar(0.0D, yawDeg - 90.0D);
        Vec3d delta = look.scale(fwd)
                .add(leftVec.scale(strafe))
                .add(new Vec3d(0.0D, vert, 0.0D));
        double length = delta.length();
        if (length < 1.0E-8D) {
            return Vec3d.ZERO;
        }
        double scaled = clampSpeed(speed) * (sprint ? SPRINT_MULTIPLIER : 1.0D);
        return delta.scale(scaled / length);
    }

    public static Vec3d applyCollision(
            Vec3d from,
            Vec3d to,
            boolean collide,
            Vec3d hitOrNull) {
        if (!collide || hitOrNull == null) {
            return to;
        }
        Vec3d dir = new Vec3d(to.x - from.x, to.y - from.y, to.z - from.z);
        double length = dir.length();
        if (length < 1.0E-8D) {
            return from;
        }
        Vec3d nudged = hitOrNull.add(dir.scale(-HIT_NUDGE / length));
        return nudged;
    }

    public static Pose lerp(Pose previous, Pose current, float partialTick) {
        Pose from = previous == null ? current : previous;
        Pose to = current == null ? from : current;
        if (from == null) {
            return new Pose(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        }
        float t = Float.isFinite(partialTick) ? Math.max(0.0F, Math.min(1.0F, partialTick)) : 1.0F;
        return new Pose(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t,
                to.yaw,
                to.pitch);
    }

    static Vec3d fromPolar(double pitchDeg, double yawDeg) {
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);
        double cosPitch = Math.cos(pitch);
        return new Vec3d(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch);
    }
}
