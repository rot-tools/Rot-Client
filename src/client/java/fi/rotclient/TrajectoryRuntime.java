package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

import java.util.List;
import java.util.Optional;

/**
 * Renders bow/pearl trajectories. Start pose follows the render-tick player
 * (lerp of xo/yo/zo), and each step uses vanilla world clip instead of
 * per-voxel collision shapes so the line stays glued while jumping.
 */
final class TrajectoryRuntime {
    private static int chargeTicks;
    private static int lastChargeTicks;

    private TrajectoryRuntime() {
    }

    static void tick(Minecraft client) {
        lastChargeTicks = chargeTicks;
        if (client == null || client.player == null) {
            chargeTicks = 0;
            return;
        }
        LocalPlayer player = client.player;
        chargeTicks = player.isUsingItem() ? player.getTicksUsingItem() : 0;
    }

    static void renderGizmos() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.trajectoriesEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        ItemStack held = player.getMainHandItem();
        String skyBlockId = AutoClickerItemIdentity.skyBlockId(held);
        boolean terminator = TrajectoryPredictor.isTerminatorId(skyBlockId);
        Optional<TrajectoryPredictor.ProjectileKind> kind =
                TrajectoryPredictor.detectHeld(
                        qol.trajectoriesBows,
                        qol.trajectoriesPearls,
                        terminator ? "terminator" : identity(held));
        if (kind.isEmpty()) {
            return;
        }
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float pull = kind.get() == TrajectoryPredictor.ProjectileKind.BOW
                ? interpolatedBowPull(partialTick)
                : 1.0F;
        if (kind.get() == TrajectoryPredictor.ProjectileKind.BOW
                && pull <= 0.1F
                && !player.isUsingItem()) {
            pull = 1.0F;
        }
        float[] yawOffsets = terminator && kind.get() == TrajectoryPredictor.ProjectileKind.BOW
                ? TrajectoryPredictor.terminatorYawOffsets()
                : new float[] {0.0F};
        Vec3 eye = interpolatedEye(player, partialTick);
        float yaw = player.getViewYRot(partialTick);
        float pitch = player.getViewXRot(partialTick);
        TrajectoryPredictor.Vec3d start = TrajectoryPredictor.startPos(
                new TrajectoryPredictor.Vec3d(eye.x, eye.y, eye.z),
                yaw);
        int color = qol.trajectoriesColor;
        float lineWidth = Math.max(0.1F, Math.min(5.0F, qol.trajectoriesWidth));
        GizmoStyle hitStyle = GizmoStyle.strokeAndFill(
                color, lineWidth, withAlpha(color, 0x4D));
        TrajectoryPredictor.SegmentClipper clipper =
                worldClip(client.level, player, qol.trajectoriesEntities);
        for (float yawOffset : yawOffsets) {
            TrajectoryPredictor.Result result = TrajectoryPredictor.simulateWithClip(
                    kind.get(),
                    start,
                    yaw + yawOffset,
                    pitch,
                    pull,
                    qol.trajectoriesRange,
                    clipper);
            drawResult(qol, color, hitStyle, result);
        }
    }

    private static float interpolatedBowPull(float partialTick) {
        float ticks = TrajectoryPredictor.lerp(
                lastChargeTicks, chargeTicks, partialTick);
        return TrajectoryPredictor.bowPull(ticks);
    }

    private static Vec3 interpolatedEye(LocalPlayer player, float partialTick) {
        double x = player.xo + (player.getX() - player.xo) * partialTick;
        double y = player.yo + (player.getY() - player.yo) * partialTick;
        double z = player.zo + (player.getZ() - player.zo) * partialTick;
        return new Vec3(x, y + player.getEyeHeight(), z);
    }

    private static TrajectoryPredictor.SegmentClipper worldClip(
            Level level,
            Entity player,
            boolean entities) {
        return (from, to) -> {
            Vec3 start = new Vec3(from.x(), from.y(), from.z());
            Vec3 end = new Vec3(to.x(), to.y(), to.z());
            Optional<TrajectoryPredictor.Hit> block = Optional.empty();
            BlockHitResult hit = level.clip(new ClipContext(
                    start,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 at = hit.getLocation();
                Direction face = hit.getDirection();
                block = Optional.of(new TrajectoryPredictor.Hit(
                        new TrajectoryPredictor.Vec3d(at.x, at.y, at.z),
                        hit.getBlockPos().getX(),
                        hit.getBlockPos().getY(),
                        hit.getBlockPos().getZ(),
                        face == null ? "inside" : face.getSerializedName()));
            }
            Optional<TrajectoryPredictor.Hit> entityHit = Optional.empty();
            if (entities) {
                AABB sweep = new AABB(start, end).inflate(1.0D);
                EntityHitResult entity = ProjectileUtil.getEntityHitResult(
                        player,
                        start,
                        end,
                        sweep,
                        candidate -> candidate != null
                                && candidate.isAlive()
                                && candidate.isPickable()
                                && candidate != player,
                        0.0D);
                if (entity != null) {
                    Vec3 at = entity.getLocation();
                    entityHit = Optional.of(new TrajectoryPredictor.Hit(
                            new TrajectoryPredictor.Vec3d(at.x, at.y, at.z),
                            (int) Math.floor(at.x),
                            (int) Math.floor(at.y),
                            (int) Math.floor(at.z),
                            "entity"));
                }
            }
            if (entityHit.isEmpty()) {
                return block;
            }
            if (block.isEmpty()) {
                return entityHit;
            }
            double entityDist = distSq(start, entityHit.get().point());
            double blockDist = distSq(start, block.get().point());
            return entityDist <= blockDist ? entityHit : block;
        };
    }

    private static double distSq(Vec3 start, TrajectoryPredictor.Vec3d point) {
        double dx = point.x() - start.x;
        double dy = point.y() - start.y;
        double dz = point.z() - start.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void drawResult(
            QolUtilityConfig qol,
            int color,
            GizmoStyle hitStyle,
            TrajectoryPredictor.Result result) {
        List<TrajectoryPredictor.Vec3d> points = result.points();
        if (qol.trajectoriesLines) {
            for (int i = 1; i < points.size(); i++) {
                TrajectoryPredictor.Vec3d a = points.get(i - 1);
                TrajectoryPredictor.Vec3d b = points.get(i);
                var props = Gizmos.line(
                        new Vec3(a.x(), a.y(), a.z()),
                        new Vec3(b.x(), b.y(), b.z()),
                        color);
                if (!qol.trajectoriesDepth) {
                    props.setAlwaysOnTop();
                }
            }
        }
        if (qol.trajectoriesBoxes && result.hit().isPresent()) {
            TrajectoryPredictor.Hit hit = result.hit().get();
            double s = 0.15D * Math.max(0.5F, qol.trajectoriesBoxSize);
            var props = Gizmos.cuboid(
                    new AABB(
                            hit.point().x() - s,
                            hit.point().y() - s,
                            hit.point().z() - s,
                            hit.point().x() + s,
                            hit.point().y() + s,
                            hit.point().z() + s),
                    hitStyle);
            if (!qol.trajectoriesDepth) {
                props.setAlwaysOnTop();
            }
        }
        if (qol.trajectoriesPlane && result.hit().isPresent()) {
            TrajectoryPredictor.Plane plane =
                    TrajectoryPredictor.impactPlane(result.hit().get(), qol.trajectoriesPlaneSize);
            var props = Gizmos.cuboid(
                    new AABB(
                            plane.minX(),
                            plane.minY(),
                            plane.minZ(),
                            plane.maxX(),
                            plane.maxY(),
                            plane.maxZ()),
                    GizmoStyle.strokeAndFill(
                            color, 0.01F, withAlpha(color, 0x80)));
            if (!qol.trajectoriesDepth) {
                props.setAlwaysOnTop();
            }
        }
    }

    private static String identity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        if (stack.is(Items.ENDER_PEARL)) {
            return "ender pearl";
        }
        if (stack.getItem() instanceof BowItem || stack.is(Items.BOW)) {
            return "bow";
        }
        String hover = stack.getHoverName().getString();
        String nbt = AutoClickerItemIdentity.identify(stack);
        return (hover + " " + nbt).trim();
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
