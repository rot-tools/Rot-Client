package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.FramerateLimitTracker.FramerateThrottleReason;
import fi.rotclient.RotClientClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla caps AFK menus at 30 FPS and title screens at 60. Dynamic FPS also
 * injects {@code getFramerateLimit} at HEAD and can overwrite a HEAD+cancel.
 * Apply last (low mixin priority) and rewrite the return so the dashboard
 * stays at Minecraft's unlimited notch (260) while it is open.
 */
@Mixin(value = FramerateLimitTracker.class, priority = 1)
abstract class FramerateLimitTrackerMixin {
    private static final int UNLIMITED_NOTCH = 260;

    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
    private int rotclient$uncapDashboardFps(int original) {
        if (!RotClientClient.wantsMonitorRefreshUi()) {
            return original;
        }
        return UNLIMITED_NOTCH;
    }

    @ModifyReturnValue(method = "getThrottleReason", at = @At("RETURN"))
    private FramerateThrottleReason rotclient$noDashboardAfkThrottle(
            FramerateThrottleReason original) {
        if (!RotClientClient.wantsMonitorRefreshUi()) {
            return original;
        }
        return FramerateThrottleReason.NONE;
    }
}
