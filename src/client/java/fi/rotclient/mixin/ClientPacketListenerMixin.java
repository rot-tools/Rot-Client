package fi.rotclient.mixin;

import fi.rotclient.ChatCommandsRuntime;
import fi.rotclient.RingKeybindsRuntime;
import fi.rotclient.ClientThreadGuard;
import fi.rotclient.DiagnosticRecorder;
import fi.rotclient.AutoExperimentsRuntime;
import fi.rotclient.AutoHarpRuntime;
import fi.rotclient.DianaRuntime;
import fi.rotclient.ExperimentSolverRuntime;
import fi.rotclient.IotaRuntime;
import fi.rotclient.QolVisualRuntime;
import fi.rotclient.WardrobeAutoEquipRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void rotclient$commandShortcut(String message, CallbackInfo ci) {
        java.util.Optional<String> command = ChatCommandsRuntime.outgoingCommand(message);
        if (command.isEmpty()) {
            return;
        }
        ((ClientPacketListener) (Object) this).sendCommand(command.get());
        ci.cancel();
    }

    @ModifyVariable(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private String rotclient$chatEmotes(String message) {
        return ChatCommandsRuntime.modifyOutgoing(message);
    }

    @ModifyVariable(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private String rotclient$commandEmotes(String message) {
        String outgoing = ChatCommandsRuntime.applyEmotes(message);
        RingKeybindsRuntime.rememberOutgoingCommand(outgoing);
        return outgoing;
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void rotclient$systemChatPacket(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!onClientThread()) return;
        fi.rotclient.RotClientClient.onSystemMessagePacket(packet.content());
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    private void rotclient$muteIotaTerminatorChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!onClientThread() || packet == null || packet.content() == null) {
            return;
        }
        if (IotaRuntime.shouldMuteTerminatorChat(packet.content().getString())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void rotclient$muteIotaSounds(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (!onClientThread() || packet == null || packet.getSound() == null) {
            return;
        }
        if (IotaRuntime.shouldMuteFishingCast()
                && packet.getSound().is(SoundEvents.FISHING_BOBBER_THROW.location())) {
            ci.cancel();
            return;
        }
        if (IotaRuntime.shouldMuteTerminatorSound()
                && packet.getSound().is(SoundEvents.ENDERMAN_TELEPORT.location())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void rotclient$dianaParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!onClientThread() || packet == null || packet.getParticle() == null) {
            return;
        }
        var type = packet.getParticle().getType();
        var key = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        DianaRuntime.observeParticlePacket(
                key == null ? "" : key.toString(),
                packet.getX(),
                packet.getY(),
                packet.getZ(),
                packet.getCount(),
                packet.getMaxSpeed(),
                packet.getXDist(),
                packet.getYDist(),
                packet.getZDist());
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void rotclient$inventorySlotPacket(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!onClientThread()) return;
        fi.rotclient.RotClientClient.onInventoryPacket("slot-packet");
        ExperimentSolverRuntime.onSlotUpdate(packet.getSlot(), packet.getItem());
        AutoExperimentsRuntime.onSlotUpdate();
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void rotclient$inventoryContentPacket(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (!onClientThread()) return;
        fi.rotclient.RotClientClient.onInventoryPacket("content-packet");
        ExperimentSolverRuntime.onContainerRefresh();
        AutoExperimentsRuntime.onSlotUpdate();
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void rotclient$wardrobeHiddenOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (!onClientThread()) {
            return;
        }
        if (WardrobeAutoEquipRuntime.consumeOpenScreen(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"))
    private void rotclient$wardrobeClosed(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        if (!onClientThread()) {
            return;
        }
        WardrobeAutoEquipRuntime.onContainerClosed();
        ExperimentSolverRuntime.onScreenClosed();
        AutoExperimentsRuntime.reset();
        AutoHarpRuntime.reset();
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void rotclient$entityEventPacket(
            ClientboundEntityEventPacket packet,
            CallbackInfo ci) {
        if (!onClientThread()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        fi.rotclient.RotClientClient.onEntityEvent(
                packet.getEntity(client.level),
                packet.getEventId());
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void rotclient$damageEventPacket(
            ClientboundDamageEventPacket packet,
            CallbackInfo ci) {
        if (!onClientThread()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        var source = packet.getSource(client.level);
        if (!fi.rotclient.RotClientClient.isLocalPlayerCombatCause(
                packet.sourceCauseId(),
                packet.sourceDirectId(),
                source == null ? null : source.getEntity(),
                source == null ? null : source.getDirectEntity())) {
            return;
        }
        fi.rotclient.RotClientClient.onPlayerAttack(
                client.level.getEntity(packet.entityId()));
    }

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void rotclient$addEntityPacket(
            net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet,
            CallbackInfo ci) {
        if (!onClientThread()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || packet == null) return;
        fi.rotclient.RotClientClient.onSpawnedCombatProjectile(
                client.level.getEntity(packet.getId()),
                packet.getData());
        fi.rotclient.DungeonRuntime.onLeapEntityPacket(
                packet.getId(), packet.getX(), packet.getY(), packet.getZ());
    }

    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void rotclient$leapMotion(
            net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket packet,
            CallbackInfo ci) {
        if (!onClientThread() || packet == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        var entity = client.level.getEntity(packet.id());
        if (entity != null) {
            fi.rotclient.DungeonRuntime.onLeapEntityPacket(
                    packet.id(), entity.getX(), entity.getY(), entity.getZ());
        }
    }

    @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
    private void rotclient$leapTeleport(
            net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket packet,
            CallbackInfo ci) {
        if (!onClientThread() || packet == null) {
            return;
        }
        var change = packet.change();
        var pos = change.position();
        fi.rotclient.DungeonRuntime.onLeapEntityPacket(packet.id(), pos.x(), pos.y(), pos.z());
    }

    @Inject(method = "handleEntityPositionSync", at = @At("TAIL"))
    private void rotclient$leapSync(
            net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket packet,
            CallbackInfo ci) {
        if (!onClientThread() || packet == null) {
            return;
        }
        var pos = packet.values().position();
        fi.rotclient.DungeonRuntime.onLeapEntityPacket(packet.id(), pos.x(), pos.y(), pos.z());
    }

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"), cancellable = true)
    private void rotclient$singleBlockPacket(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        if (!onClientThread()) return;
        if (fi.rotclient.MiningAssistRuntime.shouldIgnoreUpdate(packet.getPos(), packet.getBlockState())) {
            ci.cancel();
            return;
        }
        fi.rotclient.RotClientClient.onServerBlockUpdate(packet.getPos(), packet.getBlockState());
        record("single", packet.getPos(), packet.getBlockState());
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
    private void rotclient$sectionBlockPacket(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        if (!onClientThread()) return;
        packet.runUpdates((pos, state) -> {
            fi.rotclient.RotClientClient.onServerBlockUpdate(pos, state);
            record("section", pos, state);
        });
    }

    @Inject(method = "handleSetEntityData", at = @At("HEAD"))
    private void rotclient$animationFix(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        if (!onClientThread() || packet == null) {
            return;
        }
        if (!QolVisualRuntime.animationFixEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || packet.id() != client.player.getId()) {
            return;
        }
        packet.packedItems().removeIf(entry ->
                entry.serializer() == EntityDataSerializers.POSE);
    }

    @WrapOperation(
            method = "handleSetEntityData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/syncher/SynchedEntityData;assignValues(Ljava/util/List;)V"))
    private void rotclient$disconnectFix(
            SynchedEntityData data,
            List<SynchedEntityData.DataValue<?>> items,
            Operation<Void> original) {
        if (!QolVisualRuntime.disconnectFixEnabled()) {
            original.call(data, items);
            return;
        }
        try {
            original.call(data, items);
        } catch (Exception corrupted) {
            // A malformed tracker payload would otherwise propagate up and disconnect the client.
        }
    }

    private static boolean onClientThread() {
        // HEAD injections run before PacketUtils.ensureRunningOnSameThread.
        // Vanilla replays the handler on the client thread, where this passes.
        return ClientThreadGuard.shouldHandle(Minecraft.getInstance().isSameThread());
    }

    private static void record(String packetType, BlockPos pos, BlockState state) {
        if (!DiagnosticRecorder.isRecording()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || player.blockPosition().distSqr(pos) > 196.0) return;
        DiagnosticRecorder.record("BLOCK_PACKET",
                "type=" + packetType
                        + " state=" + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
