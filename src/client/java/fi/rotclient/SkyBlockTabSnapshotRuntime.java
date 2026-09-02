package fi.rotclient;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Shared bounded tab-list snapshot for profile, commission and HUD readers. */
final class SkyBlockTabSnapshotRuntime {
    static final long REFRESH_INTERVAL_MILLIS = 250L;
    private static List<String> cached = List.of();
    private static long capturedAtMillis;
    private static Object connectionIdentity;

    private SkyBlockTabSnapshotRuntime() {
    }

    static List<String> lines(Minecraft client, long nowMillis) {
        Object connection = client == null ? null : client.getConnection();
        if (connection == null) {
            clear();
            return List.of();
        }
        if (connection == connectionIdentity
                && nowMillis >= capturedAtMillis
                && nowMillis - capturedAtMillis < REFRESH_INTERVAL_MILLIS) {
            return cached;
        }
        connectionIdentity = connection;
        capturedAtMillis = Math.max(0L, nowMillis);
        cached = List.copyOf(CommissionDisplayRuntime.captureTabLines(client));
        return cached;
    }

    static void clear() {
        cached = List.of();
        capturedAtMillis = 0L;
        connectionIdentity = null;
    }
}
