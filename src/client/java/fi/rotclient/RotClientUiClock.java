package fi.rotclient;

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
        keepInputFresh(client);
    }

    /**
     * Vanilla AFK throttle is 30 FPS after 60s without input. Call this on
     * client tick as well as extract so a skipped GUI frame cannot arm AFK.
     */
    static void keepInputFresh(Minecraft client) {
        if (client != null && RotClientClient.wantsMonitorRefreshUi()) {
            client.getFramerateLimitTracker().onInputReceived();
        }
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
