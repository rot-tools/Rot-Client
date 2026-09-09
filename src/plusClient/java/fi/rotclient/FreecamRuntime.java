package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Minecraft bridge for {@link FreecamPolicy}. Mixins query the detached pose
 * and whether WASD / move packets should stay on the standing player.
 */
public final class FreecamRuntime {
    private static boolean active;
    private static double x;
    private static double y;
    private static double z;
    private static double prevX;
    private static double prevY;
    private static double prevZ;
    private static float yaw;
    private static float pitch;
    private static float frozenYaw;
    private static float frozenPitch;
    private static float lookStartYaw;
    private static float lookStartPitch;
    private static boolean lookTick;

    private FreecamRuntime() {
    }

    public static boolean active() {
        return active;
    }

    static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            reset();
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        boolean enabled = FreecamPolicy.shouldRun(extras.freecamEnabled, true);
        if (!enabled) {
            reset();
            return;
        }
        LocalPlayer player = client.player;
        if (!active) {
            attach(player);
        }
        player.setYRot(frozenYaw);
        player.setXRot(frozenPitch);
        player.setYHeadRot(frozenYaw);
        prevX = x;
        prevY = y;
        prevZ = z;
        if (client.gui != null && client.gui.screen() != null) {
            return;
        }
        if (client.options == null) {
            return;
        }
        FreecamPolicy.Vec3d delta = FreecamPolicy.flyDelta(
                client.options.keyUp.isDown(),
                client.options.keyDown.isDown(),
                client.options.keyLeft.isDown(),
                client.options.keyRight.isDown(),
                client.options.keyJump.isDown(),
                client.options.keyShift.isDown(),
                client.options.keySprint.isDown(),
                yaw,
                pitch,
                extras.freecamSpeed);
        FreecamPolicy.Vec3d from = new FreecamPolicy.Vec3d(x, y, z);
        FreecamPolicy.Vec3d to = from.add(delta);
        FreecamPolicy.Vec3d hit = extras.freecamCollide
                ? clipHit(client, player, from, to)
                : null;
        FreecamPolicy.Vec3d next = FreecamPolicy.applyCollision(
                from, to, extras.freecamCollide, hit);
        x = next.x();
        y = next.y();
        z = next.z();
    }

    public static FreecamPolicy.Pose interpolated(float partialTick) {
        if (!active) {
            return null;
        }
        return FreecamPolicy.lerp(
                new FreecamPolicy.Pose(prevX, prevY, prevZ, yaw, pitch),
                new FreecamPolicy.Pose(x, y, z, yaw, pitch),
                partialTick);
    }

    public static boolean shouldHideLocalBody(Entity entity) {
        if (!active || !(entity instanceof Player player)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer local = client == null ? null : client.player;
        boolean isLocal = local != null && player.getId() == local.getId();
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return FreecamPolicy.shouldHideLocalBody(active, extras.freecamShowBody, isLocal);
    }

    public static boolean shouldBlockOutbound(Packet<?> packet) {
        if (!active || !(packet instanceof ServerboundMovePlayerPacket move)) {
            return false;
        }
        return FreecamPolicy.shouldBlockMovePacket(
                true, move.hasPosition(), move.hasRotation());
    }

    public static void noteLookStart() {
        Minecraft client = Minecraft.getInstance();
        if (!active || client == null || client.player == null) {
            lookTick = false;
            return;
        }
        lookStartYaw = client.player.getYRot();
        lookStartPitch = client.player.getXRot();
        lookTick = true;
    }

    public static void absorbLook() {
        Minecraft client = Minecraft.getInstance();
        if (!lookTick || !active || client == null || client.player == null) {
            lookTick = false;
            return;
        }
        LocalPlayer player = client.player;
        yaw += player.getYRot() - lookStartYaw;
        pitch = FreecamPolicy.clampPitch(pitch + (player.getXRot() - lookStartPitch));
        player.setYRot(frozenYaw);
        player.setXRot(frozenPitch);
        player.setYHeadRot(frozenYaw);
        lookTick = false;
    }

    private static void attach(LocalPlayer player) {
        x = player.getX();
        y = player.getY() + player.getEyeHeight();
        z = player.getZ();
        prevX = x;
        prevY = y;
        prevZ = z;
        yaw = player.getYRot();
        pitch = player.getXRot();
        frozenYaw = yaw;
        frozenPitch = pitch;
        active = true;
    }

    private static void reset() {
        active = false;
        lookTick = false;
    }

    private static FreecamPolicy.Vec3d clipHit(
            Minecraft client,
            LocalPlayer player,
            FreecamPolicy.Vec3d from,
            FreecamPolicy.Vec3d to) {
        if (client.level == null) {
            return null;
        }
        Vec3 start = new Vec3(from.x(), from.y(), from.z());
        Vec3 end = new Vec3(to.x(), to.y(), to.z());
        BlockHitResult hit = client.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Vec3 at = hit.getLocation();
        return new FreecamPolicy.Vec3d(at.x, at.y, at.z);
    }
}
