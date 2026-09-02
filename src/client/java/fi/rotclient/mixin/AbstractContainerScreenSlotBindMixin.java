package fi.rotclient.mixin;

import fi.rotclient.ItemProtectRuntime;
import fi.rotclient.SlotBindsRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenSlotBindMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void rotclient$slotBindSwap(
            Slot slot,
            int slotId,
            int button,
            ContainerInput input,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (ItemProtectRuntime.shouldBlockSlotClick(screen, slot, input)) {
            ci.cancel();
            return;
        }
        if (SlotBindsRuntime.handleSlotClicked(screen, slot, input)) {
            ci.cancel();
        }
    }
}
