package fi.rotclient.mixin;

import fi.rotclient.InventoryChromeRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
    @Shadow
    private boolean visible;

    @Inject(method = "init", at = @At("TAIL"))
    private void rotclient$closeInventoryRecipeBook(
            int width,
            int height,
            Minecraft client,
            boolean narrow,
            CallbackInfo ci) {
        if (client != null
                && client.gui != null
                && InventoryChromeRuntime.shouldHideRecipeBook(client.gui.screen())) {
            this.visible = false;
        }
    }
}
