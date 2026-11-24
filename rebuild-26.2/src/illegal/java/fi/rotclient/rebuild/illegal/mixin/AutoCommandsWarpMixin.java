package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.autocommands.AutoCommandsClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class AutoCommandsWarpMixin {
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;change()Lnet/minecraft/world/entity/PositionMoveRotation;"))
    private void rotRebuild$warpPosition(ClientboundPlayerPositionPacket packet, CallbackInfo callback) {
        AutoCommandsClient.onPosition((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void rotRebuild$warpRespawn(ClientboundRespawnPacket packet, CallbackInfo callback) {
        AutoCommandsClient.onRespawn((ClientPacketListener) (Object) this);
    }
}
