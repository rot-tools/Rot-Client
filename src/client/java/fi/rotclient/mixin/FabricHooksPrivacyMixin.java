package fi.rotclient.mixin;

import fi.rotclient.ClientNetworkPrivacyPolicy;
import fi.rotclient.ClientNetworkPrivacyRuntime;
import net.fabricmc.loader.impl.game.minecraft.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Hooks.class, remap = false)
abstract class FabricHooksPrivacyMixin {
    @Inject(method = "insertBranding", at = @At("RETURN"), cancellable = true, remap = false)
    private static void rotclient$vanillaBrandOnMultiplayer(
            String brand,
            CallbackInfoReturnable<String> cir) {
        if (ClientNetworkPrivacyRuntime.shouldScrubOutbound()) {
            cir.setReturnValue(ClientNetworkPrivacyPolicy.VANILLA_BRAND);
        }
    }
}
