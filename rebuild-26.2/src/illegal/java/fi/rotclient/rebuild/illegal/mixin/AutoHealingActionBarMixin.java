package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class AutoHealingActionBarMixin {
    @Inject(method = "setActionBarText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void rotRebuild$observeHealthPacket(ClientboundSetActionBarTextPacket packet, CallbackInfo callback) {
        IllegalEdition.observeHealingActionBar(packet.text());
    }
}
