package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** F7 P3 soul-sand / chest place triggerbot. */
final class DungeonSoulsandRuntime {
    private static long lastPlaceMs;

    private DungeonSoulsandRuntime() {
    }

    static void tick(Minecraft client) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.soulsandEnabled || client == null || client.player == null || client.gameMode == null) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        LocalPlayer player = client.player;
        String held = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath();
        String block = DungeonRuntime.lookedBlockId(client, hit.getBlockPos());
        boolean f7Boss = SkyBlockDungeonDetector.confidentlyInDungeon()
                && DungeonRuntime.sidebar().boss()
                && DungeonCarryPolicy.normalizeFloor(DungeonRuntime.sidebar().floor()).endsWith("7");
        if (!DungeonAthenPortPolicy.soulsandPlaceTarget(
                SkyBlockDungeonDetector.confidentlyInDungeon(),
                f7Boss,
                hit.getBlockPos().getY(),
                block,
                held)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPlaceMs < 150L) {
            return;
        }
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        lastPlaceMs = now;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
