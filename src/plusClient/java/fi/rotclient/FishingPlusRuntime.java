package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Plus fishing auto-pull / recast. Bite HUD stays on {@link FishingHelperRuntime}.
 */
final class FishingPlusRuntime {
    private static int pendingPull;
    private static int pendingRecast;
    private static int recastCheckTicks;
    private static boolean busy;

    private FishingPlusRuntime() {
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
                ClickPulseHelper.pulseUse(client);
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
                ClickPulseHelper.pulseUse(client);
                busy = false;
            }
        }

        if (!busy
                && FishingHelperPolicy.shouldPull(
                        true,
                        qol.fishingHelperAutoPull,
                        holdingRod,
                        hookAlive,
                        FishingHelperRuntime.biteNearby(player, hook))) {
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
                ClickPulseHelper.pulseUse(client);
            }
        }
    }
}
