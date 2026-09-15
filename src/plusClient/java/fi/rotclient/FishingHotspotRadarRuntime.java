package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Plus-only particle trail and guessed hotspot direction. */
final class FishingHotspotRadarRuntime {
    private static List<FishingHotspotPolicy.Point> trail = List.of();
    private static FishingHotspotRadarPolicy.RadarGuess guess;

    static void clear() {
        trail = List.of();
        guess = null;
    }

    static void observe(String particleId, double x, double y, double z,
                        double xSpeed, double ySpeed, double zSpeed) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !RotClientClient.qolConfigPublic().extras().fishingHotspotsEnabled
                || !FishingHotspotRadarSettings.from(RotClientClient.qolConfigPublic()).enabled()) return;
        String held = SkyBlockItemIdentity.skyBlockId(client.player.getMainHandItem());
        if (held == null || !held.toUpperCase(java.util.Locale.ROOT).contains("HOTSPOT_RADAR")) return;
        if (!FishingHotspotRadarPolicy.isRadarFlame(particleId, xSpeed, ySpeed, zSpeed)) return;
        trail = FishingHotspotRadarPolicy.pushRadar(trail, new FishingHotspotPolicy.Point(x, y, z));
        guess = FishingHotspotRadarPolicy.guess(trail);
    }

    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (player == null || client.level == null || guess == null
                || !config.extras().fishingHotspotsEnabled
                || !FishingHotspotRadarSettings.from(config).tracer()) return;
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 eye = player.getEyePosition(partialTick);
        Gizmos.line(eye, new Vec3(guess.x() + guess.dx(),
                        guess.y() + guess.dy(), guess.z() + guess.dz()),
                config.extras().fishingHotspotsColor).setAlwaysOnTop();
    }
}
