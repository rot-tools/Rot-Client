// SPDX-License-Identifier: GPL-3.0-only
// AutoSign section adapted from AUTISM Client AutismSignEditScreenMixin by Melonik/contributors.
package fi.rotclient.mixin;

import fi.rotclient.AutismSignRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
abstract class AutismSignEditMixin extends Screen {
    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private String[] messages;
    @Unique private boolean rotclient$signSubmitPending;

    protected AutismSignEditMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void rotclient$fillSign(CallbackInfo ci) {
        rotclient$signSubmitPending = AutismSignRuntime.fill(sign, font, messages);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rotclient$submitSign(CallbackInfo ci) {
        if (!rotclient$signSubmitPending) return;
        rotclient$signSubmitPending = false;
        if (AutismSignRuntime.allowed()) onClose();
    }
}
