package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class AutoHealingInputMixin {
    @Inject(method = "handleKeybinds", at = @At("TAIL"))
    private void rotRebuild$healAfterCombatInput(CallbackInfo callback) {
        IllegalEdition.afterInputAutoHealing((Minecraft) (Object) this);
    }
}
