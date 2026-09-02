package fi.rotclient.mixin;

import fi.rotclient.RotClientClient;
import net.minecraft.client.FramerateLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The display wait uses this argument, not only {@code getFramerateLimit}.
 * Raise it while a Rot Client screen is open so a 30 FPS AFK/menu cap cannot
 * stall the render loop even if another mod wins the tracker mixin.
 */
@Mixin(value = FramerateLimiter.class, priority = 1)
abstract class FramerateLimiterMixin {
    private static final int UNLIMITED_NOTCH = 260;

    @ModifyVariable(method = "limitDisplayFPS", at = @At("HEAD"), argsOnly = true)
    private static int rotclient$uncapDashboardWait(int fps) {
        if (!RotClientClient.wantsMonitorRefreshUi()) {
            return fps;
        }
        return Math.max(fps, UNLIMITED_NOTCH);
    }
}
