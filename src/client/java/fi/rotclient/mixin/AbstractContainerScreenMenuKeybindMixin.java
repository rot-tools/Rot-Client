package fi.rotclient.mixin;

import fi.rotclient.DungeonRuntime;
import fi.rotclient.MenuKeybindRuntime;
import fi.rotclient.StallMarketRuntime;
import fi.rotclient.SlotBindsRuntime;
import fi.rotclient.StorageOverlayRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMenuKeybindMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void rotclient$menuKeybinds(
            KeyEvent event,
            CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (StorageOverlayRuntime.keyPressed(event)) {
            cir.setReturnValue(true);
            return;
        }
        if (SlotBindsRuntime.handleKeyPressed(screen, event.key(), hoveredSlot)
                || DungeonRuntime.handleContainerKey(screen, event.key())
                || MenuKeybindRuntime.handleKeyPressed(screen, event.key())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rotclient$wardrobeMouseBinds(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (StallMarketRuntime.shouldBlockClick(
                screen, hoveredSlot, event.button(), event.hasControlDown())
                || DungeonRuntime.handleContainerMouse(screen, event.button())
                || MenuKeybindRuntime.handleMousePressed(screen, event.button())) {
            cir.setReturnValue(true);
        }
    }
}
