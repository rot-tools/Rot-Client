// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient.mixin;

import fi.rotclient.AutismToolRuntime;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
abstract class AutismToolMenuMixin {
    @Inject(method="handleContainerInput",at=@At("HEAD"))
    private void rotclient$manualInventoryInput(int containerId,int slot,int button,ContainerInput input,Player player,CallbackInfo ci){
        AutismToolRuntime.menuInput();
    }
}
