package fi.rotclient.rebuild.mixin;

import fi.rotclient.rebuild.RebuildClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the vanilla inventory effects panel aligned with the HUD visibility switch. */
@Mixin(EffectsInInventory.class)
abstract class InventoryStatusEffectsMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void rotRebuild$hideInventoryEffects(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                 CallbackInfo callback) {
        if (RebuildClient.hideStatusEffects()) callback.cancel();
    }

    @Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true)
    private void rotRebuild$hideEffectsAvailability(CallbackInfoReturnable<Boolean> callback) {
        if (RebuildClient.hideStatusEffects()) callback.setReturnValue(false);
    }
}
