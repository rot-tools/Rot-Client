package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;

/** Input cancellation and client-side block rewriting used only by Rot Client+. */
public final class DungeonPlusInputRuntime {
    private DungeonPlusInputRuntime() { }

    public static boolean shouldCancelBlockUse(BlockPos pos, boolean sneaking) {
        if (pos == null) {
            return false;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (!extras.dungeonF7Enabled) {
            return false;
        }
        if (DungeonF7Policy.blockWrongArrow(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7ArrowAlign && extras.dungeonF7ArrowBlockWrong,
                sneaking,
                false,
                DungeonRuntime.arrowClicks)) {
            return true;
        }
        EmberDungeonPolicy.IntVec next = DungeonRuntime.simon.nextButton();
        if (DungeonF7Policy.blockWrongSimon(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7Simon && extras.dungeonF7SimonBlockWrong,
                sneaking,
                next)) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        boolean holdingRelicOrMenu = player != null && (
                DungeonF7Policy.holdingRelicOrMenu(player.getMainHandItem().getHoverName().getString())
                        || DungeonF7Policy.holdingRelicOrMenu(player.getOffhandItem().getHoverName().getString()));
        if (EmberDungeonPolicy.blockRelicClick(
                extras.dungeonF7RelicBlockWrong,
                DungeonRuntime.lastRelic,
                holdingRelicOrMenu,
                pos.getX(),
                pos.getY(),
                pos.getZ())) {
            return true;
        }
        if (!DungeonRuntime.lastRelic.isBlank()
                && EmberDungeonPolicy.correctRelicCauldron(
                        DungeonRuntime.lastRelic, pos.getX(), pos.getY(), pos.getZ())) {
            DungeonRuntime.lastRelic = "";
            DungeonRuntime.relicPickupAt = 0L;
        }
        return false;
    }

    public static boolean shouldCancelEntityUse(Entity entity, boolean sneaking) {
        if (!(entity instanceof ItemFrame frame)) {
            return false;
        }
        return shouldCancelBlockUse(frame.blockPosition(), sneaking);
    }

    public static boolean tryBreakerInstamine(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null || pos == null) {
            return false;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        var stack = client.player.getMainHandItem();
        String name = stack.getHoverName().getString();
        String id = SkyBlockItemIdentity.skyBlockId(stack);
        boolean holding = TempleDungeonPolicy.isDungeonBreakerItem(name, id);
        int charges = DungeonPolicy.breakerCharges(InventoryChromeRuntime.loreLines(stack)).orElse(0);
        boolean fatigue = client.player.hasEffect(MobEffects.MINING_FATIGUE);
        if (!DungeonAthenPortPolicy.shouldInstamineBreaker(
                extras.dungeonF7Enabled,
                extras.athen().breakerInstamine,
                holding,
                fatigue,
                charges,
                DungeonRuntime.fullBlockId(client, pos))) {
            return false;
        }
        client.level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    public static boolean shouldSkipBreakerSecretMine(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null || pos == null) {
            return false;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (!extras.dungeonF7Enabled || !extras.dungeonF7BreakerPreventSecrets) {
            return false;
        }
        var stack = client.player.getMainHandItem();
        String name = stack.getHoverName().getString();
        String id = SkyBlockItemIdentity.skyBlockId(stack);
        if (!TempleDungeonPolicy.isDungeonBreakerItem(name, id)) {
            return false;
        }
        return TempleDungeonPolicy.shouldBlockBreakerOnSecret(
                true, true, DungeonRuntime.fullBlockId(client, pos));
    }
}
