package fi.rotclient.mixin;

import fi.rotclient.FreecamPolicy;
import fi.rotclient.FreecamRuntime;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After vanilla aligns the camera with the player, overwrite pose with the
 * detached Free Camera. {@code detached} also skips first-person hands so the
 * standing body can render.
 */
@Mixin(Camera.class)
abstract class CameraFreecamMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void rotclient$applyFreecam(float partialTicks, CallbackInfo ci) {
        FreecamPolicy.Pose pose = FreecamRuntime.interpolated(partialTicks);
        if (pose == null) {
            return;
        }
        this.setPosition(pose.x(), pose.y(), pose.z());
        this.setRotation(pose.yaw(), pose.pitch());
        this.detached = true;
    }
}
