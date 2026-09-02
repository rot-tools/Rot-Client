package fi.rotclient.mixin;

import fi.rotclient.InventoryChromeRuntime;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenEffectsMixin {
    @Inject(method = "showsActiveEffects", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideInventoryStatusEffects(CallbackInfoReturnable<Boolean> cir) {
        if (InventoryChromeRuntime.shouldHideStatusEffects()) {
            cir.setReturnValue(false);
        }
    }
}
