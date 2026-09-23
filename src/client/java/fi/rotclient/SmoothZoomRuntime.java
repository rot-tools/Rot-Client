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
    // Set by the scroll wheel while the key is held; 0 means "use the configured amount".
    private static double targetAmount;
    private static double liveAmount;

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

        boolean held = zoomHeld(client, qol);
        double configured = SmoothZoomPolicy.clampAmount(qol.zoomAmount);

        if (held) {
            if (!qol.zoomScroll || targetAmount <= 0.0D) {
                targetAmount = configured;
            }
            if (liveAmount <= 0.0D) {
                liveAmount = targetAmount;
            }
        }

        progress =
                SmoothZoomPolicy.advanceProgress(
                        progress,
                        held,
                        qol.zoomSpeed,
                        deltaSeconds);

        if (!held && progress <= 0.0D) {
            // Fully zoomed out: the next press starts from the configured amount again.
            targetAmount = 0.0D;
            liveAmount = 0.0D;
        } else if (liveAmount > 0.0D) {
            liveAmount = SmoothZoomPolicy.advanceAmount(
                    liveAmount, targetAmount, qol.zoomSpeed, deltaSeconds);
        }

        return SmoothZoomPolicy.applyToFov(
                vanillaFov,
                liveAmount > 0.0D ? liveAmount : configured,
                progress);
    }

    /**
     * Mouse wheel while the zoom key is held. Returns true when the wheel was used for zooming,
     * so the caller can keep it from also changing the selected hotbar slot.
     */
    public static boolean onScroll(double vertical) {
        Minecraft client = Minecraft.getInstance();
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (client == null
                || qol == null
                || !qol.zoomEnabled
                || !qol.zoomScroll
                || vertical == 0.0D
                || !Double.isFinite(vertical)
                || !zoomHeld(client, qol)) {
            return false;
        }
        double base = targetAmount > 0.0D
                ? targetAmount
                : SmoothZoomPolicy.clampAmount(qol.zoomAmount);
        targetAmount = SmoothZoomPolicy.scrollAmount(base, vertical);
        return true;
    }

    private static boolean zoomHeld(Minecraft client, QolUtilityConfig qol) {
        // Zoom only applies while playing: typing the bound letter in chat or an inventory must
        // not zoom the camera behind the screen, and the wheel must keep scrolling that screen.
        boolean canZoom =
                SmoothZoomPolicy.canZoom(
                        client.level != null
                                && client.player != null
                                && client.getWindow() != null,
                        client.gui != null
                                && client.gui.screen() != null,
                        qol.zoomKeybind != null
                                && !qol.zoomKeybind.isBlank());

        return canZoom
                && QolInputRuntime.isBoundDown(
                        client.getWindow().handle(),
                        qol.zoomKeybind);
    }

    public static void reset() {
        progress = 0.0D;
        lastSampleNanos = 0L;
        targetAmount = 0.0D;
        liveAmount = 0.0D;
    }
}