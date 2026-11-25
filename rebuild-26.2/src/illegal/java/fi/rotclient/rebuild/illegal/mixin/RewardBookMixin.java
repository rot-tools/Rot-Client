package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class RewardBookMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void rotRebuild$openRewards(Screen screen, CallbackInfo callback) {
        if (screen instanceof BookViewScreen book
                && IllegalEdition.openRewardBook(book, ((RewardBookAccessor) book).rotRebuild$getBookAccess())) {
            callback.cancel();
        }
    }
}
