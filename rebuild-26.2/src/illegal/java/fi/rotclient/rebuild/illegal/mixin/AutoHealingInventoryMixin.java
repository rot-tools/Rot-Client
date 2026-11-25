package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class AutoHealingInventoryMixin {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"))
    private void rotRebuild$observeSelection(int slot, CallbackInfo callback) {
        IllegalEdition.healingSlotChanged((Inventory) (Object) this, slot);
    }
}
