package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class AutoClickerInputMixin {
    @Unique private boolean rotRebuild$holdAttack;
    @Unique private boolean rotRebuild$restoreAttack;
    @Unique private boolean rotRebuild$restoreUse;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void rotRebuild$queueClicks(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        int actions = IllegalEdition.autoClickerActions(client);
        rotRebuild$holdAttack = (actions & IllegalEdition.AUTO_BREAK) != 0;
        rotRebuild$restoreAttack = (actions & IllegalEdition.REPLACE_VANILLA_ATTACK) != 0
                && rotRebuild$replaceNativeInput(client.options.keyAttack);
        rotRebuild$restoreUse = (actions & IllegalEdition.REPLACE_VANILLA_USE) != 0
                && rotRebuild$replaceNativeInput(client.options.keyUse);
        if ((actions & IllegalEdition.AUTO_ATTACK) != 0) rotRebuild$queueClick(client.options.keyAttack);
        if ((actions & IllegalEdition.AUTO_USE) != 0) rotRebuild$queueClick(client.options.keyUse);
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void rotRebuild$restoreNativeInput(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (rotRebuild$restoreAttack) client.options.keyAttack.setDown(true);
        if (rotRebuild$restoreUse) client.options.keyUse.setDown(true);
    }

    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"))
    private boolean rotRebuild$applyBlockHold(boolean attackHeld) {
        return attackHeld || rotRebuild$holdAttack;
    }

    @Unique
    private static boolean rotRebuild$replaceNativeInput(KeyMapping binding) {
        boolean down = binding.isDown();
        binding.setDown(false);
        ((AutoClickerKeyMappingAccessor) binding).rotRebuild$setClickCount(0);
        return down;
    }

    @Unique
    private static void rotRebuild$queueClick(KeyMapping binding) {
        if (binding.isUnbound()) return;
        var queue = (AutoClickerKeyMappingAccessor) binding;
        queue.rotRebuild$setClickCount(queue.rotRebuild$getClickCount() + 1);
    }
}
