// SPDX-License-Identifier: GPL-3.0-only
// AUTISM Client 5.0 FreeLookModule by Melonik/contributors.
// Rot Tools: hold/toggle setup, lifecycle and Free Camera ownership (2026-10-16).
package fi.rotclient;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class AutismFreeLookRuntime {
    private static final String ID = AutismModuleSettings.LOOK;
    private static CameraType previous, applied;
    private static Object world;
    private static boolean active;
    private static float yaw, pitch;
    private AutismFreeLookRuntime() {}

    static void tick(Minecraft client) {
        var config = RotClientClient.qolConfigPublic();
        boolean available = client.player != null && client.level != null && client.gui.screen() == null
                && !client.player.isSpectator() && !FreecamRuntime.active() && !MarketTradeRuntime.enabled();
        String bind = AutismModuleSettings.text(config, ID + ".keybind");
        boolean held = !AutismModuleSettings.bool(config, ID + ".hold") || bind.isBlank()
                || client.getWindow() != null && QolInputRuntime.isBoundDown(client.getWindow().handle(), bind);
        if (!available || !held || !AutismModuleSettings.enabled(config, ID)) {
            clear(client);
            return;
        }
        if (world != client.level) clear(client);
        if (!active) {
            previous = client.options.getCameraType();
            yaw = client.player.getYRot();
            pitch = client.player.getXRot();
            world = client.level;
            active = true;
        }
        applied = front() ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
        client.options.setCameraType(applied);
    }

    public static boolean active() {
        var client = Minecraft.getInstance();
        return active && client.player != null && client.level == world && client.gui.screen() == null && !FreecamRuntime.active();
    }
    public static boolean front() { return "Front".equals(AutismModuleSettings.text(RotClientClient.qolConfigPublic(), ID + ".perspective")); }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static boolean turn(double x, double y) {
        if (!active() || Minecraft.getInstance().gui.screen() != null) return false;
        var next = AutismPortPolicy.look(yaw, pitch, x, y,
                !AutismModuleSettings.bool(RotClientClient.qolConfigPublic(), ID + ".no_pitch_limit"));
        yaw = next.yaw();
        pitch = next.pitch();
        return true;
    }

    static void clear(Minecraft client) {
        if (previous != null && client.options != null && client.options.getCameraType() == applied)
            client.options.setCameraType(previous);
        active = false;
        previous = applied = null;
        world = null;
    }
}
