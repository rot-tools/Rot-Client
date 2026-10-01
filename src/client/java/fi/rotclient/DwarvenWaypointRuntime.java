package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;

final class DwarvenWaypointRuntime {
    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        QolSkyblockExtras settings = RotClientClient.qolConfigPublic().extras();
        if (!settings.miningHelpersEnabled || !settings.miningHelpersDwarvenWaypoints
                || client.player == null || client.level == null
                || SkyBlockAreaDetector.detect() != SkyBlockArea.DWARVEN_MINES) return;
        for (var landmark : DwarvenWaypointPolicy.LANDMARKS) {
            BlockPos pos = new BlockPos(landmark.x(), landmark.y() + 8, landmark.z());
            int meters = (int) Math.round(Math.sqrt(client.player.distanceToSqr(
                    landmark.x(), landmark.y(), landmark.z())));
            // Public static navigation labels may show through terrain; no live hidden data is read.
            Gizmos.billboardTextOverBlock(landmark.name() + " · " + meters + "m", pos,
                    0, 0xFF55FFFF, 0.035F).setAlwaysOnTop();
        }
    }
}
