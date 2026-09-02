package fi.rotclient.mixin;

import fi.rotclient.DungeonRuntime;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Camera helper: optional noclip zoom and custom third-person
 * distance. Only applies while the Camera module is enabled.
 */
@Mixin(Camera.class)
abstract class CameraClipMixin {
    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private float rotclient$cameraHelperZoom(float maxZoom) {
        return DungeonRuntime.cameraHelperZoom(maxZoom);
    }
}
