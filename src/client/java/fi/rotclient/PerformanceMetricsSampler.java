package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Samples FPS / ping / TPS for Performance HUD. TPS is a rolling estimate from
 * world game-time advancement; unavailable when not in-world.
 */
final class PerformanceMetricsSampler {
    private static final int TPS_WINDOW = 20;

    private final long[] wallMs = new long[TPS_WINDOW];
    private final long[] gameTicks = new long[TPS_WINDOW];
    private int sampleCount;
    private int writeIndex;
    private long lastSampleWallMs;
    private OptionalDouble cachedTps = OptionalDouble.empty();

    PerformanceHudLayout.Snapshot sample(Minecraft client, long nowMs) {
        Optional<Integer> fps = Optional.empty();
        Optional<Integer> ping = Optional.empty();
        if (client != null) {
            int observedFps = client.getFps();
            if (observedFps >= 0) {
                fps = Optional.of(observedFps);
            }
            LocalPlayer player = client.player;
            if (player != null && client.getConnection() != null) {
                PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) {
                    int latency = info.getLatency();
                    if (latency >= 0) {
                        ping = Optional.of(latency);
                    }
                }
            }
            if (nowMs - lastSampleWallMs >= 250L
                    && client.level != null) {
                lastSampleWallMs = nowMs;
                long tick = client.level.getGameTime();
                wallMs[writeIndex] = nowMs;
                gameTicks[writeIndex] = tick;
                writeIndex = (writeIndex + 1) % TPS_WINDOW;
                if (sampleCount < TPS_WINDOW) {
                    sampleCount++;
                }
                cachedTps = estimateTps();
            }
        }
        OptionalDouble tps = cachedTps;
        if (client == null || client.level == null) {
            tps = OptionalDouble.empty();
        }
        return new PerformanceHudLayout.Snapshot(
                fps,
                tps.isPresent() ? Optional.of(tps.getAsDouble()) : Optional.empty(),
                ping);
    }

    private OptionalDouble estimateTps() {
        if (sampleCount < 2) {
            return OptionalDouble.empty();
        }
        int oldest = sampleCount < TPS_WINDOW
                ? 0
                : writeIndex;
        int newest = (writeIndex + TPS_WINDOW - 1) % TPS_WINDOW;
        long wallDelta = wallMs[newest] - wallMs[oldest];
        long tickDelta = gameTicks[newest] - gameTicks[oldest];
        if (wallDelta < 500L || tickDelta <= 0) {
            return OptionalDouble.empty();
        }
        double tps = tickDelta * 1000.0D / wallDelta;
        if (!Double.isFinite(tps) || tps <= 0.0D || tps > 40.0D) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.min(20.0D, tps));
    }
}
