package fi.rotclient;

import net.minecraft.client.Minecraft;

/** Plus-only bridge for automatically issuing /instancerequeue. */
final class DungeonRequeueRuntime {
    private static int requeueTicks = -1;

    private DungeonRequeueRuntime() {
    }

    static void tick(Minecraft client) {
        if (requeueTicks > 0) {
            requeueTicks--;
            return;
        }
        if (requeueTicks != 0) {
            return;
        }
        requeueTicks = -1;
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (extras.dungeonRequeueEnabled
                && client != null
                && client.player != null
                && client.player.connection != null) {
            client.player.connection.sendCommand("instancerequeue");
        }
    }

    static boolean onChat(
            boolean dungeonRunStarted,
            boolean extraStatsSeen,
            int dungeonWorldTicks,
            String line) {
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (!DungeonPolicy.shouldArmRequeue(
                extras.dungeonRequeueEnabled,
                dungeonRunStarted,
                extraStatsSeen,
                dungeonWorldTicks,
                line)) {
            return false;
        }
        requeueTicks = Math.max(0, extras.dungeonRequeueDelay);
        return true;
    }

    static void reset() {
        requeueTicks = -1;
    }
}
