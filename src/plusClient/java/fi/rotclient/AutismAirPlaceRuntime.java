// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client 5.0 AirPlaceModule by Melonik/contributors.
// Rot Tools: 26.3 gizmos, one-hand bounded placement, existing input/market guards (2026-10-16).
package fi.rotclient;

import fi.rotclient.mixin.AutismUseDelayAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class AutismAirPlaceRuntime {
    private static final String ID = AutismModuleSettings.PLACE;
    private static BlockPos guide;
    private record Placement(BlockHitResult hit, InteractionHand hand) {}
    private AutismAirPlaceRuntime() {}

    private static Placement placement(Minecraft client) {
        if (!AutismModuleSettings.enabled(RotClientClient.qolConfigPublic(), ID)
                || client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || client.player.isSpectator() || client.player.isHandsBusy()
                || client.gameMode.isDestroying() || FreecamRuntime.active() || MarketTradeRuntime.enabled()
                || client.hitResult == null || client.hitResult.getType() != HitResult.Type.MISS) return null;
        ItemStack main = client.player.getMainHandItem(), off = client.player.getOffhandItem();
        InteractionHand hand = placeable(main, client) ? InteractionHand.MAIN_HAND
                : main.isEmpty() && placeable(off, client) ? InteractionHand.OFF_HAND : null;
        if (hand == null) return null;
        double range = AutismModuleSettings.number(RotClientClient.qolConfigPublic(), ID + ".range");
        HitResult picked = client.player.pick(range, 0, false);
        if (!(picked instanceof BlockHitResult hit) || picked.getType() != HitResult.Type.MISS) return null;
        BlockPos pos = hit.getBlockPos();
        if (client.level.isOutsideBuildHeight(pos) || !client.level.hasChunkAt(pos)
                || !client.level.getBlockState(pos).canBeReplaced()
                || AutismInteractionRuntime.block(pos, hand, false)) return null;
        // Normal BLOCK hit semantics keep existing NoInteract and dungeon use guards in the call path.
        return new Placement(new BlockHitResult(hit.getLocation(), hit.getDirection(), pos, false), hand);
    }

    private static boolean placeable(ItemStack stack, Minecraft client) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem && stack.isItemEnabled(client.level.enabledFeatures());
    }

    public static boolean handlesClick() {
        Minecraft client = Minecraft.getInstance();
        return client.options.keyUse.isDown() && placement(client) != null;
    }

    static void tick(Minecraft client) {
        guide = null;
        Placement placement = placement(client);
        if (placement == null) return;
        guide = placement.hit().getBlockPos().immutable();
        var delay = (AutismUseDelayAccess) client;
        if (!client.options.keyUse.isDown() || delay.rotclient$airPlaceDelay() > 0) return;
        ItemStack stack = client.player.getItemInHand(placement.hand());
        int before = stack.getCount();
        var swing = stack.getInteractAnimation();
        InteractionResult result = client.gameMode.useItemOn(client.player, placement.hand(), placement.hit());
        if (result instanceof InteractionResult.Success success) {
            if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) client.player.swing(placement.hand(), swing, false);
            if (stack.getCount() != before || client.player.hasInfiniteMaterials())
                client.player.itemUsed(placement.hand());
        }
        delay.rotclient$airPlaceDelay(4);
    }

    static void renderGizmos() {
        var client = Minecraft.getInstance();
        var config = RotClientClient.qolConfigPublic();
        if (guide == null || client.player == null || client.level == null || client.gui.screen() != null
                || !AutismModuleSettings.enabled(config, ID) || !AutismModuleSettings.bool(config, ID + ".guide")) return;
        int color = config.readColor(ID + ".color");
        Gizmos.cuboid(new AABB(guide), AutismModuleSettings.bool(config, ID + ".fill")
                ? GizmoStyle.strokeAndFill(color, 2, (color & 0x00FFFFFF) | 0x40000000)
                : GizmoStyle.stroke(color, 2));
    }

    static void clear() { guide = null; }
}
