package fi.rotclient;

import net.minecraft.client.Minecraft;

/**
 * Smooth hold-to-zoom runtime.
 *
 * Rot modifies only the effective camera FOV returned for rendering.
 * The player's configured Minecraft FOV option is never changed.
 */
public final class SmoothZoomRuntime {
    private static double progress;
    private static long lastSampleNanos;

    private SmoothZoomRuntime() {
    }

    public static float modifyFov(float vanillaFov) {
        Minecraft client =
                Minecraft.getInstance();

        QolUtilityConfig qol =
                RotClientClient.qolConfigPublic();

        if (client == null
                || qol == null
                || !qol.zoomEnabled) {

            reset();
            return vanillaFov;
        }

        long now =
                System.nanoTime();

        double deltaSeconds =
                lastSampleNanos == 0L
                        ? 1.0D / 60.0D
                        : (now - lastSampleNanos)
                                / 1_000_000_000.0D;

        lastSampleNanos = now;

        boolean canZoom =
                client.level != null
                        && client.player != null
                        && client.getWindow() != null
                        && qol.zoomKeybind != null
                        && !qol.zoomKeybind.isBlank();

        boolean held =
                canZoom
                        && QolInputRuntime.isBoundDown(
                                client.getWindow().handle(),
                                qol.zoomKeybind);

        progress =
                SmoothZoomPolicy.advanceProgress(
                        progress,
                        held,
                        qol.zoomSpeed,
                        deltaSeconds);

        return SmoothZoomPolicy.applyToFov(
                vanillaFov,
                qol.zoomAmount,
                progress);
    }

    public static void reset() {
        progress = 0.0D;
        lastSampleNanos = 0L;
    }
}