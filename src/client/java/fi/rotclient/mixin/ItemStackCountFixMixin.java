package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
abstract class ItemStackCountFixMixin {
    @Inject(method = "limitSize", at = @At("HEAD"), cancellable = true)
    private void rotclient$keepUnstackableCount(int maxStackSize, CallbackInfo ci) {
        if (QolVisualRuntime.itemCountFixEnabled()) {
            ci.cancel();
        }
    }
}
