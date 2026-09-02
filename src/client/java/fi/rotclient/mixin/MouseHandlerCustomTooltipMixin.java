package fi.rotclient.mixin;

import fi.rotclient.CustomTooltipRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wheel deltas for hover-box panning live on {@code MouseHandler.onScroll}.
 * {@code Screen} no longer declares {@code mouseScrolled} in 26.2.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerCustomTooltipMixin {
    @Inject(method = "onScroll", at = @At("HEAD"))
    private void rotclient$panHoverBox(long window, double horizontal, double vertical, CallbackInfo ci) {
        ItemStack hovered = ItemStack.EMPTY;
        Minecraft client = Minecraft.getInstance();
        if (client != null
                && client.gui != null
                && client.gui.screen() instanceof AbstractContainerScreen<?> container) {
            Slot slot = ((AbstractContainerScreenAccessor) container).rotclient$hoveredSlot();
            hovered = slot == null ? ItemStack.EMPTY : slot.getItem();
        }
        CustomTooltipRuntime.mouseScrolled(horizontal, vertical, hovered);
    }
}
