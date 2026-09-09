package fi.rotclient.mixin;

import fi.rotclient.CustomResourcePackRuntime;
import fi.rotclient.InventoryWalkRuntime;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
abstract class ConnectionInventoryWalkMixin {
    /**
     * Minecraft 26.2 routes {@code send(Packet)} through this 3-arg overload.
     * Injecting only the 1-arg method misses GUI clicks from
     * {@code handleContainerInput}.
     */
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"))
    private void rotclient$noteInventoryClick(
            Packet<?> packet,
            ChannelFutureListener listener,
            boolean flush,
            CallbackInfo ci) {
        if (packet instanceof ServerboundContainerClickPacket click) {
            InventoryWalkRuntime.noteInventoryClick(click.containerInput());
        }
    }

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotclient$noteInventoryWalkInbound(
            ChannelHandlerContext context,
            Packet<?> packet,
            CallbackInfo ci) {
        if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
            InventoryWalkRuntime.noteKeepAliveOrPing();
            if (CustomResourcePackRuntime.isReloading()) {
                ((Connection) (Object) this).send(
                        new ServerboundKeepAlivePacket(keepAlive.getId()));
                ci.cancel();
            }
            return;
        }
        if (packet instanceof ClientboundPingPacket ping) {
            InventoryWalkRuntime.noteKeepAliveOrPing();
            if (CustomResourcePackRuntime.isReloading()) {
                ((Connection) (Object) this).send(new ServerboundPongPacket(ping.getId()));
                ci.cancel();
            }
        }
    }
}
