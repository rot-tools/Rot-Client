package fi.rotclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Draws the selection box of the aimed-at block in the configured color, or
 * as a rainbow whose hue flows along the edges. Called from the vanilla
 * block-outline pass in place of the default single-color edge loop.
 */
public final class BlockOutlineRuntime {
    private BlockOutlineRuntime() {
    }

    /**
     * @return true when the outline was drawn here and vanilla must skip it
     */
    public static boolean draw(
            PoseStack poseStack,
            VertexConsumer builder,
            VoxelShape shape,
            BlockPos pos,
            double camX,
            double camY,
            double camZ,
            float vanillaWidth) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.blockOutlineEnabled) {
            return false;
        }
        float width = BlockOutlinePolicy.scaledWidth(vanillaWidth, extras.blockOutlineWidth);
        int solid = BlockOutlinePolicy.solidColor(extras.blockOutlineColor);
        PoseStack.Pose pose = poseStack.last();
        double ox = pos.getX() - camX;
        double oy = pos.getY() - camY;
        double oz = pos.getZ() - camZ;
        Vector3f normal = new Vector3f();
        if (!BlockOutlinePolicy.isRainbow(extras.blockOutlineMode)) {
            shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                normal.set((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
                vertex(builder, pose, normal, width, x1 + ox, y1 + oy, z1 + oz, solid);
                vertex(builder, pose, normal, width, x2 + ox, y2 + oy, z2 + oz, solid);
            });
            return true;
        }
        int alpha = solid >>> 24;
        double seconds = System.nanoTime() * 1.0E-9D;
        double speed = extras.blockOutlineRainbowSpeed;
        double spread = extras.blockOutlineRainbowSpread;
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            normal.set((float) dx, (float) dy, (float) dz).normalize();
            int pieces = BlockOutlinePolicy.segmentCount(Math.sqrt(dx * dx + dy * dy + dz * dz));
            double px = x1;
            double py = y1;
            double pz = z1;
            int pieceColor = BlockOutlinePolicy.rainbowArgb(
                    BlockOutlinePolicy.hue(seconds, speed, spread, px, py, pz), alpha);
            for (int i = 1; i <= pieces; i++) {
                double t = (double) i / pieces;
                double nx = x1 + dx * t;
                double ny = y1 + dy * t;
                double nz = z1 + dz * t;
                int nextColor = BlockOutlinePolicy.rainbowArgb(
                        BlockOutlinePolicy.hue(seconds, speed, spread, nx, ny, nz), alpha);
                vertex(builder, pose, normal, width, px + ox, py + oy, pz + oz, pieceColor);
                vertex(builder, pose, normal, width, nx + ox, ny + oy, nz + oz, nextColor);
                px = nx;
                py = ny;
                pz = nz;
                pieceColor = nextColor;
            }
        });
        return true;
    }

    private static void vertex(
            VertexConsumer builder,
            PoseStack.Pose pose,
            Vector3f normal,
            float width,
            double x,
            double y,
            double z,
            int argb) {
        builder.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(argb)
                .setNormal(pose, normal)
                .setLineWidth(width);
    }
}
