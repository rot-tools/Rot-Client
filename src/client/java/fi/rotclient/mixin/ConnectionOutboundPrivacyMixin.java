package fi.rotclient.mixin;

import fi.rotclient.ClientNetworkPrivacyRuntime;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
abstract class ConnectionOutboundPrivacyMixin {
    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Packet<?> rotclient$sanitizeOutboundPacket(Packet<?> packet) {
        return ClientNetworkPrivacyRuntime.sanitizeOutbound(packet);
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotclient$blockSensitiveOutbound(
            Packet<?> packet,
            ChannelFutureListener listener,
            boolean flush,
            CallbackInfo ci) {
        if (ClientNetworkPrivacyRuntime.shouldBlockOutbound(packet)) {
            ci.cancel();
        }
    }
}
