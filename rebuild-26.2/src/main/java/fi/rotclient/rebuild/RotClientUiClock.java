package fi.rotclient.rebuild;

import net.minecraft.client.Minecraft;

/**
 * One sampled frame dt per Rot Client screen extract. Uses Minecraft's
 * realtime tick delta so paused worlds still animate the dashboard at the
 * monitor refresh, matching Electron/Qt/OneConfig lerp-on-delta.
 */
final class RotClientUiClock {
    private static double frameSeconds = 1.0D / 120.0D;
    private static long lastFallbackNs;

    private RotClientUiClock() {
    }

    static void beginFrame(Minecraft client) {
        frameSeconds = sampleSeconds(client);
    }

    static double seconds() {
        return frameSeconds;
    }

    static double sampleSeconds(Minecraft client) {
        if (client != null && client.getDeltaTracker() != null) {
            double fromTracker = RotClientEase.secondsFromRealtimeTicks(
                    client.getDeltaTracker().getRealtimeDeltaTicks());
            if (fromTracker > 0.0D) {
                lastFallbackNs = System.nanoTime();
                return fromTracker;
            }
        }
        long now = System.nanoTime();
        double dt = RotClientEase.frameDeltaSeconds(lastFallbackNs, now);
        lastFallbackNs = now;
        return dt;
    }
}
