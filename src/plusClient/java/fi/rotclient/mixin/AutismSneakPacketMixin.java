// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient.mixin;

import fi.rotclient.AutismMovementRuntime;
import fi.rotclient.AutismInteractionRuntime;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
abstract class AutismSneakPacketMixin {
    @ModifyVariable(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",at=@At("HEAD"),argsOnly=true)
    private Packet<?> rotclient$autismSneak(Packet<?> packet){return AutismMovementRuntime.outgoing(packet);}

    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",at=@At("HEAD"),cancellable=true)
    private void rotclient$autismNoInteract(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (AutismInteractionRuntime.cancelPacket(packet)) ci.cancel();
    }
}
