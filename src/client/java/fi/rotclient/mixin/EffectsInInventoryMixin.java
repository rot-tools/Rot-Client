package fi.rotclient.mixin;

import fi.rotclient.InventoryChromeRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EffectsInInventory.class)
abstract class EffectsInInventoryMixin {
    @Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideStatusEffectPanel(CallbackInfoReturnable<Boolean> cir) {
        if (InventoryChromeRuntime.shouldHideStatusEffects()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotclient$skipStatusEffectExtract(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        if (InventoryChromeRuntime.shouldHideStatusEffects()) {
            ci.cancel();
        }
    }
}
