package fi.rotclient.mixin;

import fi.rotclient.InventoryChromeRuntime;
import fi.rotclient.SkyBlockMenuHighlightRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Container-specific foreground additions. The custom cursor is submitted by
 * the Screen render-state wrapper only after all subclass content and tooltips.
 */
@Mixin(value = AbstractContainerScreen.class, priority = 2000)
abstract class AbstractContainerScreenCustomCursorMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void rotclient$drawForegroundAndCursor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerScreenAccessor pos = (AbstractContainerScreenAccessor) container;
        InventoryChromeRuntime.afterForeground(
                container, graphics, pos.rotclient$leftPos(), pos.rotclient$topPos());
        if (!(container instanceof InventoryScreen)) {
            SkyBlockMenuHighlightRuntime.afterContainerContents(
                    container, graphics, pos.rotclient$leftPos(), pos.rotclient$topPos());
        }
    }
}
