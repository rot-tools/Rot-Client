package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Target-independent mining aim evidence. While the player holds attack on any
 * block, nearby block disappearances for any material profile may correlate —
 * not only when the crosshair matches that material's tracked block.
 */
final class SharedMiningAimEvidence {
    private static final long AIM_TTL_MILLIS = 2_500L;
    private static final Map<BlockPos, Long> recentAims = new LinkedHashMap<>();

    private SharedMiningAimEvidence() {
    }

    static void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            reset();
            return;
        }
        long now = System.currentTimeMillis();
        if (client.options.keyAttack.isDown()
                && client.hitResult instanceof BlockHitResult hit) {
            recentAims.put(hit.getBlockPos().immutable(), now);
            TrackingRuntimeTrace.callbackAlive("BlockAim");
        }
        Iterator<Map.Entry<BlockPos, Long>> iterator =
                recentAims.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > AIM_TTL_MILLIS) {
                iterator.remove();
            }
        }
    }

    static boolean isNearRecentAim(BlockPos changed) {
        if (changed == null || recentAims.isEmpty()) {
            return false;
        }
        for (BlockPos target : recentAims.keySet()) {
            int dx = Math.abs(changed.getX() - target.getX());
            int dy = Math.abs(changed.getY() - target.getY());
            int dz = Math.abs(changed.getZ() - target.getZ());
            if (dx <= 2 && dy <= 2 && dz <= 2) {
                return true;
            }
        }
        return false;
    }

    static void reset() {
        recentAims.clear();
    }

    /** Test-only: inject an aim without a live Minecraft client. */
    static void recordAimForTest(BlockPos pos, long nowMillis) {
        if (pos == null) {
            return;
        }
        recentAims.put(pos.immutable(), nowMillis);
    }

    static int pendingAimCount() {
        return recentAims.size();
    }
}
