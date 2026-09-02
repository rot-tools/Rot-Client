package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Options.class)
abstract class OptionsCloudsMixin {
    @ModifyReturnValue(method = "getCloudStatus", at = @At("RETURN"))
    private CloudStatus rotclient$hideIslandClouds(CloudStatus original) {
        if (!QolVisualRuntime.shouldHideClouds() || original == CloudStatus.OFF) {
            return original;
        }
        return CloudStatus.OFF;
    }
}
