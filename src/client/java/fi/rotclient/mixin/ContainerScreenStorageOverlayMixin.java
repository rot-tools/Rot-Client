package fi.rotclient.mixin;

import fi.rotclient.StorageOverlayRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hypixel Storage uses {@link ContainerScreen}. That class draws the vanilla
 * chest texture in its own {@code extractBackground} override, so cancelling
 * {@link Screen#extractBackground} never hides it.
 */
@Mixin(ContainerScreen.class)
abstract class ContainerScreenStorageOverlayMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideVanillaStorageTexture(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!StorageOverlayRuntime.shouldReplaceVanilla(screen)) {
            return;
        }
        Screen self = (Screen) (Object) this;
        if (self.isInGameUi()) {
            self.extractTransparentBackground(graphics);
        }
        ci.cancel();
    }
}
