// SPDX-License-Identifier: GPL-3.0-only
// AUTISM Client 5.0 BuiltinModules.NoInteractModule, adapted by Rot Tools.
package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;

public final class AutismInteractionRuntime {
    private static final String ID=AutismModuleSettings.INTERACT;
    private AutismInteractionRuntime(){}
    private static boolean enabled(){return AutismModuleSettings.enabled(RotClientClient.qolConfigPublic(),ID);}
    private static boolean blocks(String kind,String registry,InteractionHand hand){
        var c=RotClientClient.qolConfigPublic();
        if(!enabled()||hand!=null&&!AutismPortPolicy.handMatches(AutismModuleSettings.text(c,ID+"."+kind+"_hand"),hand==InteractionHand.OFF_HAND))return false;
        return AutismPortPolicy.blocks(registry,AutismPortPolicy.registryEntries(AutismModuleSettings.text(c,ID+"."+kind)),AutismModuleSettings.text(c,ID+"."+kind+"_mode"));
    }
    public static boolean block(BlockPos pos,InteractionHand hand,boolean mining){
        var c=Minecraft.getInstance();
        if(!enabled()||pos==null||c.level==null)return false;
        return blocks(mining?"block_mine":"block_use",BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(pos).getBlock()).toString(),mining?null:hand);
    }
    public static boolean entity(Entity target,InteractionHand hand,boolean attack){
        if(!enabled()||target==null)return false;
        var c=RotClientClient.qolConfigPublic();
        if(!AutismModuleSettings.bool(c,ID+".named")&&target.hasCustomName())return false;
        if(!AutismModuleSettings.bool(c,ID+".babies")&&target instanceof LivingEntity living&&living.isBaby())return false;
        return blocks(attack?"entity_hit":"entity_use",BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(),attack?null:hand);
    }

    /** Direct gameplay packets from client-thread modules obey the same configured filters. */
    public static boolean cancelPacket(Packet<?> packet) {
        if (!(packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundPlayerActionPacket
                || packet instanceof ServerboundInteractPacket || packet instanceof ServerboundAttackPacket)) return false;
        var client = Minecraft.getInstance();
        // Never read a mutable level from a network worker. Vanilla gameplay sends on the client thread.
        if (!client.isSameThread() || client.level == null || client.player == null || !enabled()) return false;
        if (packet instanceof ServerboundUseItemOnPacket use)
            return block(use.hitResult().getBlockPos(), use.hand(), false);
        if (packet instanceof ServerboundPlayerActionPacket action)
            return action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                    && block(action.getPos(), InteractionHand.MAIN_HAND, true);
        if (packet instanceof ServerboundInteractPacket use)
            return entity(client.level.getEntity(use.entityId()), use.hand(), false);
        if (packet instanceof ServerboundAttackPacket attack)
            return entity(client.level.getEntity(attack.entityId()), InteractionHand.MAIN_HAND, true);
        return false;
    }
}
