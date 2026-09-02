package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Temporary chat/look-target waypoints. Parsing and expiry live in
 * {@link WaypointPolicy}; this draws gizmos and places look-target pings.
 */
public final class WaypointRuntime {
    private static final List<WaypointPolicy.Marker> MARKERS = new ArrayList<>();
    private static boolean pingWasDown;

    private WaypointRuntime() {
    }

    static void clear() {
        MARKERS.clear();
        pingWasDown = false;
    }

    static void onGameMessage(Component message) {
        if (message == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.waypointsEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        String local = player == null ? "" : player.getScoreboardName();
        WaypointPolicy.parseChat(
                message.getString(),
                qol.waypointsFromParty,
                qol.waypointsFromAll).ifPresent(ping -> {
            if (!WaypointPolicy.shouldAcceptOwnPing(
                    ping.sender(), local, qol.waypointsPersonal)) {
                return;
            }
            addMarker(
                    WaypointPolicy.chatName(ping.sender()),
                    ping.x(),
                    ping.y(),
                    ping.z(),
                    WaypointPolicy.CHAT_DURATION_MS,
                    true);
        });
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (client == null || client.player == null) {
            clear();
            return;
        }
        if (!qol.waypointsEnabled) {
            MARKERS.clear();
            pingWasDown = false;
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<WaypointPolicy.Marker> iterator = MARKERS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expired(now)) {
                iterator.remove();
            }
        }
        tickPingKey(client, qol);
    }

    public static void renderGizmos() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.waypointsEnabled || MARKERS.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null) {
            return;
        }
        for (WaypointPolicy.Marker marker : MARKERS) {
            int color = marker.colorArgb();
            GizmoStyle style = GizmoStyle.strokeAndFill(color, 2.0F, withAlpha(color, 0x44));
            var box = Gizmos.cuboid(
                    new AABB(
                            marker.x(),
                            marker.y(),
                            marker.z(),
                            marker.x() + 1.0D,
                            marker.y() + 1.0D,
                            marker.z() + 1.0D),
                    style);
            box.setAlwaysOnTop();
            var beam = Gizmos.cuboid(
                    new AABB(
                            marker.x() + 0.4D,
                            marker.y(),
                            marker.z() + 0.4D,
                            marker.x() + 0.6D,
                            marker.y() + 32.0D,
                            marker.z() + 0.6D),
                    GizmoStyle.strokeAndFill(color, 1.5F, withAlpha(color, 0x33)));
            beam.setAlwaysOnTop();
        }
    }

    private static void tickPingKey(Minecraft client, QolUtilityConfig qol) {
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen instanceof ChatScreen || screen != null) {
            pingWasDown = false;
            return;
        }
        if (!WaypointPolicy.lookTargetPingEnabled(qol.waypointsPingMode)) {
            pingWasDown = false;
            return;
        }
        boolean down = QolKeybindNames.isBoundDown(
                client.getWindow().handle(), qol.waypointsKeybind);
        if (down && !pingWasDown) {
            placeLookTarget(client);
        }
        pingWasDown = down;
    }

    private static void placeLookTarget(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        var eye = player.getEyePosition();
        var look = player.getViewVector(1.0F);
        EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(eye.x, eye.y, eye.z),
                java.util.Optional.empty(),
                false,
                new EtherwarpPredictor.Vec3d(look.x, look.y, look.z),
                WaypointPolicy.PING_DISTANCE,
                occupancy(client.level)).ifPresent(target -> {
            BlockPos pos = new BlockPos(
                    target.surfaceBlockX(),
                    target.surfaceBlockY(),
                    target.surfaceBlockZ());
            addMarker(
                    "Waypoint",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    WaypointPolicy.PING_DURATION_MS,
                    true);
        });
    }

    private static void addMarker(
            String name,
            int x,
            int y,
            int z,
            long durationMs,
            boolean notify) {
        LocalPlayer player = Minecraft.getInstance().player;
        WaypointPolicy.AddResult result = WaypointPolicy.add(
                true,
                MARKERS,
                name,
                x,
                y,
                z,
                System.currentTimeMillis(),
                durationMs,
                ThreadLocalRandom.current().nextInt());
        if (result.status() == WaypointPolicy.AddStatus.ADDED) {
            MARKERS.add(result.marker());
            if (notify && player != null) {
                player.sendSystemMessage(Component.literal(
                        "Added waypoint at " + x + ", " + y + ", " + z + "."));
            }
            return;
        }
        if (!notify || player == null) {
            return;
        }
        if (result.status() == WaypointPolicy.AddStatus.OUT_OF_BOUNDS) {
            player.sendSystemMessage(Component.literal("Waypoint out of bounds."));
        } else if (result.status() == WaypointPolicy.AddStatus.DUPLICATE) {
            player.sendSystemMessage(Component.literal(
                    "Waypoint already exists at " + x + ", " + y + ", " + z + "."));
        }
    }

    private static EtherwarpPredictor.BlockOccupancy occupancy(
            net.minecraft.world.level.BlockGetter level) {
        return new EtherwarpPredictor.BlockOccupancy() {
            @Override
            public boolean isSolidSurface(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            }

            @Override
            public boolean isStandSpaceClear(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getFluidState(pos).isEmpty();
            }
        };
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
