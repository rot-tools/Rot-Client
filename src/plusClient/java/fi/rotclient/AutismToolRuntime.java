// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client 5.0 BuiltinModules.AutoToolModule and AutismAutoTool.
// Melonik and contributors; Rot Tools: atomic inventory swap and ownership guards.
package fi.rotclient;

import fi.rotclient.mixin.AutismToolGameModeAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

public final class AutismToolRuntime {
    private static BlockPos target;
    private static boolean switched;
    private static final AutismPortPolicy.ToolOwnership OWNERSHIP = new AutismPortPolicy.ToolOwnership();
    private static final AutismPortPolicy.ToolSwapBudget SWAPS = new AutismPortPolicy.ToolSwapBudget();
    private static boolean ownClick;
    private static int swappedSource=-1, swappedHotbar=-1;
    private static ItemStack expectedSource=ItemStack.EMPTY, expectedTool=ItemStack.EMPTY;
    private static long due,restoreDue=-1,lastMine,lastNow;
    private static Object world, player;
    private static final String ID=AutismModuleSettings.TOOL;
    private AutismToolRuntime(){}
    static void clear(){target=null;switched=false;OWNERSHIP.reset();SWAPS.reset();clearPrediction();due=lastMine=lastNow=0;restoreDue=-1;world=player=null;}
    private static void clearPrediction(){swappedSource=swappedHotbar=-1;expectedSource=expectedTool=ItemStack.EMPTY;}
    public static void menuInput(){
        if(!ownClick&&target!=null){OWNERSHIP.cancel();SWAPS.cancel();}
    }
    private static long delay(QolUtilityConfig q,String suffix){
        return AutismPortPolicy.toolDelay((int)AutismModuleSettings.number(q,ID+suffix),
                AutismModuleSettings.bool(q,ID+".random_delay"),java.util.concurrent.ThreadLocalRandom.current());
    }
    static void tick(Minecraft c){
        var q=RotClientClient.qolConfigPublic();long now=System.currentTimeMillis();
        if(!AutismModuleSettings.enabled(q,ID)||c.player==null||c.level==null||c.gameMode==null||c.gui.screen()!=null
                ||!c.player.isAlive()||c.player.isUsingItem()||c.player.isSpectator()||FreecamRuntime.active()||MarketTradeRuntime.enabled()
                ||WardrobeAutoEquipRuntime.busy()||RotClientPetAutoEquipRuntime.busy()||RotClientEquipmentAutoEquipRuntime.busy()
                ||c.player.containerMenu!=c.player.inventoryMenu||!c.player.inventoryMenu.getCarried().isEmpty()){clear();return;}
        if(world!=c.level||player!=c.player||now<lastNow){clear();world=c.level;player=c.player;}lastNow=now;
        var inv=c.player.getInventory();
        if(swappedSource>=0){
            // Vanilla applies the click prediction locally. It is not a server acknowledgement.
            // A later correction/item change cancels further changes; never retry or roll back it.
            ItemStack live=inv.getItem(swappedHotbar);
            if(live.isDamageableItem()&&expectedTool.isDamageableItem())expectedTool.setDamageValue(live.getDamageValue());
            if(!ItemStack.matches(inv.getItem(swappedSource),expectedSource)||!ItemStack.matches(live,expectedTool)){
                OWNERSHIP.cancel();SWAPS.cancel();clearPrediction();
            }
        }
        if(c.gameMode.isDestroying()){
            restoreDue=-1;
            if(AutismModuleSettings.bool(q,ID+".sneak")&&!c.player.isShiftKeyDown())return;
            BlockPos pos=((AutismToolGameModeAccess)c.gameMode).rotclient$autismDestroyPos();
            if(pos==null||c.level.getBlockState(pos).isAir())return;
            if(SWAPS.cancelled()||!OWNERSHIP.begin(inv.getSelectedSlot())) return;
            lastMine=now;
            if(!pos.equals(target)){target=pos.immutable();switched=false;due=now+delay(q,".delay");}
            if(switched||now<due)return;
            BlockState state=c.level.getBlockState(pos);
            List<AutismPortPolicy.Tool> tools=new ArrayList<>();
            var registry=c.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var efficiencyType=registry.getOrThrow(Enchantments.EFFICIENCY);
            var silkType=registry.getOrThrow(Enchantments.SILK_TOUCH);
            boolean inventory=AutismModuleSettings.bool(q,ID+".inventory");
            for(int slot=0;slot<(inventory?36:9);slot++){
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
            int best=AutismPortPolicy.bestTool(tools,inv.getSelectedSlot(),AutismModuleSettings.bool(q,ID+".ignore_durability"),AutismModuleSettings.bool(q,ID+".silk"),inventory);
            if(best>=9){if(swapToSelected(c,best))OWNERSHIP.assigned(inv.getSelectedSlot());}
            else if(best>=0){inv.setSelectedSlot(best);OWNERSHIP.assigned(best);}switched=true;
        } else {
            int restore=OWNERSHIP.restoreSlot(inv.getSelectedSlot());
            if(AutismModuleSettings.bool(q,ID+".restore")&&restore>=0){
                if(restoreDue<0)restoreDue=lastMine+delay(q,".restore_delay");
                if(now<restoreDue)return;
                inv.setSelectedSlot(restore);
            }
            target=null;switched=false;OWNERSHIP.reset();SWAPS.reset();clearPrediction();restoreDue=-1;
        }
    }

    private static boolean swapToSelected(Minecraft c,int source){
        var p=c.player;var menu=p.inventoryMenu;var inv=p.getInventory();int selected=inv.getSelectedSlot();
        if(p.containerMenu!=menu||!menu.getCarried().isEmpty()||source<9||source>=36||selected<0||selected>=9)return false;
        Slot from=null,to=null;
        for(Slot slot:menu.slots)if(slot.container==inv){
            if(slot.getContainerSlot()==source)from=slot;
            if(slot.getContainerSlot()==selected)to=slot;
        }
        if(from==null||to==null||!from.mayPickup(p)||!to.mayPickup(p)
                ||!from.mayPlace(to.getItem())||!to.mayPlace(from.getItem())||!SWAPS.take())return false;
        ItemStack tool=from.getItem().copy(),previous=to.getItem().copy();
        if(tool.isEmpty())return false;
        ownClick=true;
        try{c.gameMode.handleContainerInput(menu.containerId,from.index,selected,ContainerInput.SWAP,p);}
        finally{ownClick=false;}
        if(p.containerMenu!=menu||!menu.getCarried().isEmpty()
                ||!ItemStack.matches(inv.getItem(source),previous)||!ItemStack.matches(inv.getItem(selected),tool)){
            OWNERSHIP.cancel();SWAPS.cancel();return false;
        }
        swappedSource=source;swappedHotbar=selected;expectedSource=previous;expectedTool=tool;
        return true;
    }
}
