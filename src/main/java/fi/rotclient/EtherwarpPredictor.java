package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure Etherwarp destination prediction. Visualization only — no interaction,
 * rotation, teleport, or packet semantics.
 *
 * {@code useServerPosition} is honored only when a legitimate server position
 * sample is provided; otherwise the predictor fails safely to the client eye
 * origin without fabricating server state.
 */
public final class EtherwarpPredictor {
    public static final double DEFAULT_RANGE = 61.0D;

    public enum Validity {
        VALID,
        INVALID
    }

    public record Target(
            int blockX,
            int blockY,
            int blockZ,
            int surfaceBlockX,
            int surfaceBlockY,
            int surfaceBlockZ,
            Validity validity,
            boolean usedServerPosition) {
        public Target(
                int blockX,
                int blockY,
                int blockZ,
                Validity validity,
                boolean usedServerPosition) {
            this(
                    blockX,
                    blockY,
                    blockZ,
                    blockX,
                    blockY - 1,
                    blockZ,
                    validity,
                    usedServerPosition);
        }
    }

    /** Thin top-face marker for the solid block Etherwarp would land on. */
    public record HighlightPlane(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
    }

    public record Vec3d(double x, double y, double z) {
        public Vec3d add(Vec3d other) {
            return new Vec3d(x + other.x, y + other.y, z + other.z);
        }

        public Vec3d scale(double factor) {
            return new Vec3d(x * factor, y * factor, z * factor);
        }
    }

    public interface BlockOccupancy {
        /** True when the block at xyz is a solid Etherwarp surface. */
        boolean isSolidSurface(int x, int y, int z);

        /** True when feet/head space at xyz is free enough to stand. */
        boolean isStandSpaceClear(int x, int y, int z);
    }

    private EtherwarpPredictor() {
    }

    public static Optional<Target> predict(
            Vec3d clientEye,
            Optional<Vec3d> serverEye,
            boolean preferServerPosition,
            Vec3d lookDirection,
            double maxRange,
            BlockOccupancy world) {
        if (clientEye == null || lookDirection == null || world == null) {
            return Optional.empty();
        }
        boolean usedServer = false;
        Vec3d origin = clientEye;
        if (preferServerPosition) {
            if (serverEye.isEmpty()) {
                // Deferred / unavailable — fail safely to client eye.
                origin = clientEye;
            } else {
                origin = serverEye.get();
                usedServer = true;
            }
        }
        double length = Math.sqrt(
                lookDirection.x * lookDirection.x
                        + lookDirection.y * lookDirection.y
                        + lookDirection.z * lookDirection.z);
        if (!(length > 1.0e-6D) || !Double.isFinite(length) || maxRange <= 0) {
            return Optional.empty();
        }
        Vec3d dir = lookDirection.scale(1.0D / length);
        double step = 0.25D;
        int steps = (int) Math.ceil(maxRange / step);
        for (int i = 1; i <= steps; i++) {
            double t = Math.min(maxRange, i * step);
            Vec3d point = origin.add(dir.scale(t));
            int bx = (int) Math.floor(point.x);
            int by = (int) Math.floor(point.y);
            int bz = (int) Math.floor(point.z);
            if (world.isSolidSurface(bx, by, bz)) {
                // Landing pad is typically the block above the hit surface.
                int landX = bx;
                int landY = by + 1;
                int landZ = bz;
                boolean valid = world.isStandSpaceClear(landX, landY, landZ)
                        && world.isStandSpaceClear(landX, landY + 1, landZ);
                return Optional.of(new Target(
                        landX,
                        landY,
                        landZ,
                        bx,
                        by,
                        bz,
                        valid ? Validity.VALID : Validity.INVALID,
                        usedServer));
            }
        }
        return Optional.empty();
    }

    public static final List<String> RENDER_STYLES = List.of("Filled", "Outline", "Filled Outline");

    public static String normalizeRenderStyle(String value) {
        if (value == null) {
            return "Outline";
        }
        String text = value.trim();
        for (String style : RENDER_STYLES) {
            if (style.equalsIgnoreCase(text)) {
                return style;
            }
        }
        String compact = text.toLowerCase(Locale.ROOT).replace(' ', '_');
        if (compact.contains("filled") && compact.contains("outline")) {
            return "Filled Outline";
        }
        if (compact.contains("fill")) {
            return "Filled";
        }
        return "Outline";
    }

    public static boolean drawFilled(String style) {
        String normalized = normalizeRenderStyle(style);
        return "Filled".equals(normalized) || "Filled Outline".equals(normalized);
    }

    public static boolean drawOutline(String style) {
        String normalized = normalizeRenderStyle(style);
        return "Outline".equals(normalized) || "Filled Outline".equals(normalized);
    }

    public static HighlightPlane highlightBox(Target target, boolean fullBlock) {
        if (!fullBlock) {
            return highlightPlane(target);
        }
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        return new HighlightPlane(
                target.surfaceBlockX(),
                target.surfaceBlockY(),
                target.surfaceBlockZ(),
                target.surfaceBlockX() + 1.0D,
                target.surfaceBlockY() + 1.0D,
                target.surfaceBlockZ() + 1.0D);
    }

    public static HighlightPlane highlightPlane(Target target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        double top = target.surfaceBlockY() + 1.0D;
        return new HighlightPlane(
                target.surfaceBlockX() + 0.03D,
                top + 0.002D,
                target.surfaceBlockZ() + 0.03D,
                target.surfaceBlockX() + 0.97D,
                top + 0.012D,
                target.surfaceBlockZ() + 0.97D);
    }

    public static boolean canWarpTo(Optional<Target> target) {
        return target != null
                && target.isPresent()
                && target.get().validity() == Validity.VALID;
    }

    public static boolean shouldRenderTarget(
            Optional<Target> target,
            boolean showGuess,
            boolean showFailed) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        Validity validity = target.get().validity();
        if (validity == Validity.VALID) {
            return showGuess;
        }
        return showFailed;
    }

    public static boolean depthRespectsOcclusion(boolean depthSetting) {
        // Depth ON → respect world depth/occlusion.
        // Depth OFF → always-on-top / visible-through.
        return depthSetting;
    }

    public static boolean shouldPreviewWhileAiming(
            boolean sneaking,
            String heldItemName) {
        if (!sneaking || heldItemName == null) {
            return false;
        }
        String normalized = heldItemName.toLowerCase(Locale.ROOT);
        return normalized.contains("aspect of the void")
                || normalized.contains("aspect of the end")
                || normalized.contains("etherwarp conduit");
    }
}
