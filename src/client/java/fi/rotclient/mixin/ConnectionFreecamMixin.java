package fi.rotclient.mixin;

import fi.rotclient.FreecamRuntime;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drop position/look move packets while Free Camera is detached so the
 * standing player is not rotated or teleported server-side.
 */
@Mixin(Connection.class)
abstract class ConnectionFreecamMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotclient$blockFreecamMovePackets(
            Packet<?> packet,
            ChannelFutureListener listener,
            boolean flush,
            CallbackInfo ci) {
        if (FreecamRuntime.shouldBlockOutbound(packet)) {
            ci.cancel();
        }
    }
}
