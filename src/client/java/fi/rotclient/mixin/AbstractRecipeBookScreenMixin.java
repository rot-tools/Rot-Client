package fi.rotclient.mixin;

import fi.rotclient.InventoryChromeRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideInventoryRecipeBookButton(CallbackInfo ci) {
        if (InventoryChromeRuntime.shouldHideRecipeBook(
                (AbstractRecipeBookScreen<?>) (Object) this)) {
            ci.cancel();
        }
    }
}
