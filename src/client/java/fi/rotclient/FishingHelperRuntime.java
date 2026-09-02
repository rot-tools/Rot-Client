package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fishing helper: pull when a {@code !!!} hologram is near the hook,
 * then optionally recast. Recast-check runs every 300 ticks.
 */
final class FishingHelperRuntime {
    private static int pendingPull;
    private static int pendingRecast;
    private static int recastCheckTicks;
    private static boolean busy;

    private FishingHelperRuntime() {
    }

    static void clear() {
        pendingPull = 0;
        pendingRecast = 0;
        recastCheckTicks = 0;
        busy = false;
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.fishingHelperEnabled || client == null || client.player == null || client.level == null) {
            clear();
            return;
        }
        LocalPlayer player = client.player;
        boolean holdingRod = player.getMainHandItem().is(Items.FISHING_ROD);
        FishingHook hook = player.fishing;
        boolean hookAlive = hook != null && hook.isAlive();

        if (pendingPull > 0) {
            pendingPull--;
            if (pendingPull == 0) {
                AutoClickerRuntime.pulseUse(client);
                if (qol.fishingHelperRecast) {
                    pendingRecast = FishingHelperPolicy.recastDelayTicks(
                            qol.fishingHelperRecastDelay,
                            qol.fishingHelperRecastVariance,
                            ThreadLocalRandom.current().nextDouble());
                } else {
                    busy = false;
                }
            }
        } else if (pendingRecast > 0) {
            pendingRecast--;
            if (pendingRecast == 0) {
                AutoClickerRuntime.pulseUse(client);
                busy = false;
            }
        }

        if (!busy
                && FishingHelperPolicy.shouldPull(
                        true,
                        qol.fishingHelperAutoPull,
                        holdingRod,
                        hookAlive,
                        biteNearby(player, hook))) {
            busy = true;
            pendingPull = Math.max(1, FishingHelperPolicy.pullDelayTicks(
                    qol.fishingHelperPullDelay,
                    qol.fishingHelperPullVariance,
                    ThreadLocalRandom.current().nextDouble()));
        }

        recastCheckTicks++;
        if (recastCheckTicks >= FishingHelperPolicy.RECAST_CHECK_PERIOD_TICKS) {
            recastCheckTicks = 0;
            if (!busy
                    && FishingHelperPolicy.shouldRecastCheck(
                            true,
                            qol.fishingHelperRecast,
                            qol.fishingHelperRecastCheck,
                            holdingRod,
                            hookAlive)) {
                AutoClickerRuntime.pulseUse(client);
            }
        }
    }

    static String hudLine() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        if (!qol.fishingHelperEnabled || !qol.fishingHelperBobberTimer
                || client == null || client.player == null) {
            return "";
        }
        FishingHook hook = client.player.fishing;
        if (hook == null || !hook.isAlive()) {
            return "";
        }
        return String.format(java.util.Locale.ROOT, "Bobber %.1fs", hook.tickCount / 20.0F);
    }

    private static boolean biteNearby(LocalPlayer player, FishingHook hook) {
        if (player == null || player.level() == null || hook == null) {
            return false;
        }
        AABB box = hook.getBoundingBox().inflate(FishingHelperPolicy.BITE_RANGE);
        for (ArmorStand stand : player.level().getEntitiesOfClass(ArmorStand.class, box)) {
            String name = stand.getCustomName() == null
                    ? stand.getName().getString()
                    : stand.getCustomName().getString();
            if (FishingHelperPolicy.isBiteHologram(name)
                    && stand.distanceTo(hook) <= FishingHelperPolicy.BITE_RANGE) {
                return true;
            }
        }
        return false;
    }
}
