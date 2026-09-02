package fi.rotclient.mixin;

import fi.rotclient.CameraPolicy;
import fi.rotclient.PerspectiveMode;
import fi.rotclient.RotClientClient;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Camera QoL — when enabled, vanilla perspective cycling keeps rear third
 * person but maps front third person back to {@link CameraType#FIRST_PERSON}.
 * A reassigned vanilla perspective key therefore cycles first -> rear -> first.
 */
@Mixin(Options.class)
abstract class OptionsCameraMixin {
    @ModifyVariable(
            method = "setCameraType",
            at = @At("HEAD"),
            argsOnly = true)
    private CameraType rotclient$skipFrontPerspectiveWhenCameraEnabled(
            CameraType requested) {
        PerspectiveMode resolved = CameraPolicy.resolve(
                RotClientClient.isCameraEnabled(),
                toMode(requested));
        return toCameraType(resolved);
    }

    private static PerspectiveMode toMode(CameraType type) {
        if (type == null) {
            return PerspectiveMode.FIRST_PERSON;
        }
        if (type == CameraType.THIRD_PERSON_BACK) {
            return PerspectiveMode.THIRD_PERSON_BACK;
        }
        if (type == CameraType.THIRD_PERSON_FRONT) {
            return PerspectiveMode.THIRD_PERSON_FRONT;
        }
        return PerspectiveMode.FIRST_PERSON;
    }

    private static CameraType toCameraType(PerspectiveMode mode) {
        if (mode == PerspectiveMode.THIRD_PERSON_BACK) {
            return CameraType.THIRD_PERSON_BACK;
        }
        if (mode == PerspectiveMode.THIRD_PERSON_FRONT) {
            return CameraType.THIRD_PERSON_FRONT;
        }
        return CameraType.FIRST_PERSON;
    }
}
