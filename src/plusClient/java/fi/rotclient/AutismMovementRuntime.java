// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client 5.0 SafeWalkModule and BuiltinModules Parkour/Sneak.
// Melonik and contributors; Rot Tools: 26.3 input/packet adapters and lifecycle guards.
package fi.rotclient;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AutismMovementRuntime {
    private static final AutismPortPolicy.SneakHold HOLD = new AutismPortPolicy.SneakHold();
    private static boolean parkourPulse, packetHeld;
    private static final boolean[] KEY_WAS_DOWN = new boolean[AutismModuleSettings.MODULES.size()];
    private AutismMovementRuntime() {}
    private static QolUtilityConfig config(){return RotClientClient.qolConfigPublic();}
    private static boolean eligible(Minecraft c){return c!=null&&c.player!=null&&c.level!=null&&c.gui.screen()==null
            && !c.player.isSpectator()&&!FreecamRuntime.active();}
    public static Input input(ClientInput source, Input original) {
        Minecraft c=Minecraft.getInstance();
        if(original==null||!eligible(c)||c.player.input!=source){HOLD.reset();parkourPulse=false;return original;}
        var p=c.player;var q=config();
        boolean sneak=original.shift();
        boolean safe=AutismModuleSettings.enabled(q,AutismModuleSettings.SAFE)&&p.onGround()
                &&!p.isPassenger()&&!p.getAbilities().flying&&p.getXRot()>=AutismModuleSettings.number(q,AutismModuleSettings.SAFE+".look_down");
        boolean edge=safe&&dropAhead(c,Math.max(.15,AutismModuleSettings.number(q,AutismModuleSettings.SAFE+".edge_distance")));
        long lo=(long)AutismModuleSettings.number(q,AutismModuleSettings.SAFE+".hold_min"),hi=(long)AutismModuleSettings.number(q,AutismModuleSettings.SAFE+".hold_max");
        sneak|=HOLD.update(safe,edge,System.currentTimeMillis(),edge?ThreadLocalRandom.current().nextLong(Math.min(lo,hi),Math.max(lo,hi)+1):0);
        sneak|=AutismModuleSettings.enabled(q,AutismModuleSettings.SNEAK)&&!p.getAbilities().flying
                &&"Vanilla".equals(AutismModuleSettings.text(q,AutismModuleSettings.SNEAK+".mode"));
        boolean jump=original.jump();
        if(parkourPulse)parkourPulse=false;
        else if(AutismModuleSettings.enabled(q,AutismModuleSettings.PARKOUR)&&p.onGround()&&!sneak&&!jump
                &&!p.isPassenger()&&!p.getAbilities().flying&&source.getMoveVector().lengthSquared()>0){
            Vec3 d=p.getDeltaMovement();
            AABB next=p.getBoundingBox().move(d.x,d.y-.08,d.z).expandTowards(0,-.02,0);
            if(c.level.noCollision(p,next)){jump=true;parkourPulse=true;}
        }
        return new Input(original.forward(),original.backward(),original.left(),original.right(),jump,sneak,original.sprint());
    }
    private static boolean dropAhead(Minecraft c,double distance){
        var p=c.player;Vec3 velocity=p.getDeltaMovement();
        Vec3 direction=velocity.horizontalDistanceSqr()>1.0E-6?new Vec3(velocity.x,0,velocity.z).normalize():Vec3.directionFromRotation(0,p.getYRot());
        Vec3 probe=p.position().add(direction.scale(distance));
        BlockPos under=BlockPos.containing(probe).below();
        // Use actual collision, including partial blocks; do not scan or replace world blocks.
        if(c.level.isOutsideBuildHeight(under))return true;
        double half=Math.max(.05,p.getBbWidth()/2-.05);
        return c.level.noCollision(p,new AABB(probe.x-half,probe.y-.55,probe.z-half,probe.x+half,probe.y,probe.z+half));
    }
    private static boolean packetSneak(){var c=Minecraft.getInstance();return eligible(c)&&!c.player.getAbilities().flying
            &&AutismModuleSettings.enabled(config(),AutismModuleSettings.SNEAK)
            &&"Packet".equals(AutismModuleSettings.text(config(),AutismModuleSettings.SNEAK+".mode"));}
    public static Packet<?> outgoing(Packet<?> packet){
        if(packet instanceof ServerboundPlayerInputPacket p&&packetSneak()){
            Input i=p.input();return new ServerboundPlayerInputPacket(new Input(i.forward(),i.backward(),i.left(),i.right(),i.jump(),true,i.sprint()));
        }
        return packet;
    }
    static void tick(Minecraft c){
        toggleKeys(c);
        boolean desired=packetSneak();
        if(desired!=packetHeld&&c.player!=null&&c.getConnection()!=null){
            Input i=c.player.input.keyPresses;
            c.getConnection().send(new ServerboundPlayerInputPacket(new Input(i.forward(),i.backward(),i.left(),i.right(),i.jump(),desired||i.shift(),i.sprint())));
        }
        packetHeld=desired;
    }
    private static void toggleKeys(Minecraft c) {
        if (c.player == null || c.level == null || c.getWindow() == null || c.gui.screen() != null) {
            java.util.Arrays.fill(KEY_WAS_DOWN, false);
            return;
        }
        var q = config();
        for (int index = 0; index < KEY_WAS_DOWN.length; index++) {
            String id = AutismModuleSettings.MODULES.get(index);
            if (id.equals(AutismModuleSettings.LOOK) && AutismModuleSettings.bool(q, id + ".hold")) {
                KEY_WAS_DOWN[index] = false;
                continue;
            }
            boolean down = QolInputRuntime.isBoundDown(c.getWindow().handle(), AutismModuleSettings.text(q, id + ".keybind"));
            if (down && !KEY_WAS_DOWN[index]) {
                boolean next = !AutismModuleSettings.enabled(q, id);
                q.setModuleEnabled(id, next);
                TrackerStore.save(RotClientClient.trackerConfig());
                var module = QolUtilityCatalog.findById(id);
                RotClientClient.notifyQolModuleToggled(module == null ? id : module.name(), next);
            }
            KEY_WAS_DOWN[index] = down;
        }
    }
    static void clear(){HOLD.reset();parkourPulse=packetHeld=false;java.util.Arrays.fill(KEY_WAS_DOWN,false);}
}
