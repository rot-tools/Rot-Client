package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import java.util.List;

final class DwarvenWaypointRuntime {
    private static List<CommissionDisplayPolicy.Commission> cachedCommissions;
    private static List<DwarvenWaypointPolicy.Landmark> commissionDestinations = List.of();

    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        QolSkyblockExtras settings = RotClientClient.qolConfigPublic().extras();
        if (!settings.miningHelpersEnabled
                || !(settings.miningHelpersDwarvenWaypoints || settings.miningHelpersCommissionWaypoints)
                || client.player == null || client.level == null
                || SkyBlockAreaDetector.detect() != SkyBlockArea.DWARVEN_MINES) return;
        List<CommissionDisplayPolicy.Commission> commissions = CommissionDisplayRuntime.commissions();
        if (commissions != cachedCommissions) {
            cachedCommissions = commissions;
            commissionDestinations = DwarvenWaypointPolicy.commissionDestinations(commissions);
        }
        var visible = settings.miningHelpersDwarvenWaypoints
                ? DwarvenWaypointPolicy.LANDMARKS : commissionDestinations;
        for (var landmark : visible) {
            BlockPos pos = new BlockPos(landmark.x(), landmark.y() + 8, landmark.z());
            int meters = (int) Math.round(Math.sqrt(client.player.distanceToSqr(
                    landmark.x(), landmark.y(), landmark.z())));
            // Public static navigation labels may show through terrain; no live hidden data is read.
            boolean active = settings.miningHelpersCommissionWaypoints && commissionDestinations.contains(landmark);
            String label = (active ? "Commission: " : "") + landmark.name() + " · " + meters + "m";
            Gizmos.billboardTextOverBlock(label, pos,
                    0, active ? 0xFFFFCC55 : 0xFF55FFFF, 0.035F).setAlwaysOnTop();
        }
    }
}
