package fi.rotclient.mixin;

import fi.rotclient.InventoryWalkRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
abstract class MouseHandlerInventoryWalkMixin {
    @Redirect(
            method = "handleAccumulatedMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean rotclient$inventoryWalkLook(MouseHandler self) {
        return self.isMouseGrabbed() || InventoryWalkRuntime.shouldApplyMouseLook();
    }
}
