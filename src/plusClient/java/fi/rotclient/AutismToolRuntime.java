// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client 5.0 BuiltinModules.AutoToolModule and AutismAutoTool.
// Melonik and contributors; Rot Tools: hotbar-only adapter, context and ownership guards.
package fi.rotclient;

import fi.rotclient.mixin.AutismToolGameModeAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

final class AutismToolRuntime {
    private static BlockPos target;
    private static boolean switched;
    private static final AutismPortPolicy.ToolOwnership OWNERSHIP = new AutismPortPolicy.ToolOwnership();
    private static long due,restoreDue=-1,lastMine,lastNow;
    private static Object world;
    private static final String ID=AutismModuleSettings.TOOL;
    private AutismToolRuntime(){}
    static void clear(){target=null;switched=false;OWNERSHIP.reset();due=lastMine=lastNow=0;restoreDue=-1;world=null;}
    static void tick(Minecraft c){
        var q=RotClientClient.qolConfigPublic();long now=System.currentTimeMillis();
        if(!AutismModuleSettings.enabled(q,ID)||c.player==null||c.level==null||c.gameMode==null||c.gui.screen()!=null
                ||c.player.isUsingItem()||c.player.isSpectator()||FreecamRuntime.active()||MarketTradeRuntime.enabled()
                ||c.player.containerMenu!=c.player.inventoryMenu){clear();return;}
        if(world!=c.level||now<lastNow){clear();world=c.level;}lastNow=now;
        var inv=c.player.getInventory();
        if(c.gameMode.isDestroying()){
            restoreDue=-1;
            if(AutismModuleSettings.bool(q,ID+".sneak")&&!c.player.isShiftKeyDown())return;
            BlockPos pos=((AutismToolGameModeAccess)c.gameMode).rotclient$autismDestroyPos();
            if(pos==null||c.level.getBlockState(pos).isAir())return;
            if(!OWNERSHIP.begin(inv.getSelectedSlot())) return;
            lastMine=now;
            if(!pos.equals(target)){target=pos.immutable();switched=false;due=now+(long)AutismModuleSettings.number(q,ID+".delay");}
            if(switched||now<due)return;
            BlockState state=c.level.getBlockState(pos);
            List<AutismPortPolicy.Tool> tools=new ArrayList<>();
            var registry=c.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var efficiencyType=registry.getOrThrow(Enchantments.EFFICIENCY);
            var silkType=registry.getOrThrow(Enchantments.SILK_TOUCH);
            for(int slot=0;slot<9;slot++){
                ItemStack stack=inv.getItem(slot);if(stack.isEmpty())continue;
                float speed=stack.getDestroySpeed(state);
                if(speed<=1&&stack.is(Items.SHEARS)){
                    String path=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    if("cobweb".equals(path)||path.endsWith("_leaves"))speed=15;else if(path.endsWith("_wool"))speed=5;
                }
                int efficiency=EnchantmentHelper.getItemEnchantmentLevel(efficiencyType,stack);
                if(speed>1&&efficiency>0)speed+=Math.min(1024,(float)efficiency*efficiency+1);
                boolean silk=EnchantmentHelper.getItemEnchantmentLevel(silkType,stack)>0;
                tools.add(new AutismPortPolicy.Tool(slot,speed,silk,stack.isDamageableItem(),stack.getMaxDamage()-stack.getDamageValue()));
            }
            int best=AutismPortPolicy.bestTool(tools,inv.getSelectedSlot(),AutismModuleSettings.bool(q,ID+".ignore_durability"),AutismModuleSettings.bool(q,ID+".silk"));
            if(best>=0){inv.setSelectedSlot(best);OWNERSHIP.assigned(best);}switched=true;
        } else {
            int restore=OWNERSHIP.restoreSlot(inv.getSelectedSlot());
            if(AutismModuleSettings.bool(q,ID+".restore")&&restore>=0){
                if(restoreDue<0)restoreDue=lastMine+(long)AutismModuleSettings.number(q,ID+".restore_delay");
                if(now<restoreDue)return;
                inv.setSelectedSlot(restore);
            }
            target=null;switched=false;OWNERSHIP.reset();restoreDue=-1;
        }
    }
}
