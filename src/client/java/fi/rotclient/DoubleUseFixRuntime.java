package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

/**
 * Prevents blaze daggers and fishing rods from firing twice.
 */
public final class DoubleUseFixRuntime {
    private DoubleUseFixRuntime() {
    }

    public static boolean shouldCancelItemUseOnBlock() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        Minecraft client = Minecraft.getInstance();
        if (!extras.doubleUseFixEnabled || client == null || client.hitResult == null) {
            return false;
        }
        return DoubleUseFixPolicy.cancelItemUseOnBlock(
                true,
                kind(client),
                client.hitResult.getType() == HitResult.Type.BLOCK);
    }

    public static boolean shouldReplaceBlockUseWithItemUse() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        Minecraft client = Minecraft.getInstance();
        if (!extras.doubleUseFixEnabled || client == null) {
            return false;
        }
        return DoubleUseFixPolicy.replaceBlockUseWithItemUse(true, kind(client));
    }

    private static DoubleUseFixPolicy.Kind kind(Minecraft client) {
        ItemStack held = client.player == null ? ItemStack.EMPTY : client.player.getMainHandItem();
        boolean rod = held.is(Items.FISHING_ROD);
        String ability = String.join(" ", InventoryChromeRuntime.loreLines(held));
        return DoubleUseFixPolicy.kind(rod, ability);
    }
}
