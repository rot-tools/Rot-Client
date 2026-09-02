package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Detects server-authoritative changes for one tracked SkyBlock mining material.
 * Hypixel's custom mining does not consistently invoke vanilla's client-side
 * destroyBlock path, so both packet updates and a small nearby snapshot are used.
 */
final class MiningBreakDetector {
    private final TrackedMaterial material;
    private final Map<BlockPos, Integer> knownBlocks = new HashMap<>();
    private final Map<BlockPos, Long> recentTargets = new HashMap<>();
    private final Map<BlockPos, Long> recentlyCounted = new HashMap<>();
    private long pickobulusActiveUntil;
    private boolean initialized;
    private BlockPos lastLoggedTarget;

    MiningBreakDetector(TrackedMaterial material) {
        this.material = material;
    }

    void reset() {
        knownBlocks.clear();
        recentTargets.clear();
        recentlyCounted.clear();
        initialized = false;
        pickobulusActiveUntil = 0;
        lastLoggedTarget = null;
    }

    void inspectMessage(Component message) {
        String plain = message.getString();
        if (plain.contains("You used your Pickobulus")
                && plain.contains("Pickaxe Ability")) {
            pickobulusActiveUntil = System.currentTimeMillis() + 5_000L;
            DiagnosticRecorder.record("PICKOBULUS",
                    "material=" + material.id() + " ability window opened");
        }
    }

    void onServerBlockUpdate(
            Minecraft client,
            BlockPos pos,
            BlockState newState,
            boolean explicitlySelectedTarget) {
        if (client.level == null || client.player == null) return;
        if (!MiningBlockEvidencePolicy.allows(
                material.id(),
                SkyBlockAreaDetector.detectLocation(),
                explicitlySelectedTarget)) return;
        BlockState oldState = client.level.getBlockState(pos);
        int baseDrop = material.baseDrop(oldState);
        if (baseDrop <= 0 || material.isTrackedBlock(newState)) return;

        long now = System.currentTimeMillis();
        boolean normalMining = isNearRecentTarget(pos);
        boolean pickobulus = now <= pickobulusActiveUntil
                && client.player.blockPosition().distSqr(pos) <= 400.0;
        if ((!normalMining && !pickobulus) || wasRecentlyCounted(pos, now)) return;

        recentlyCounted.put(pos.immutable(), now);
        DiagnosticRecorder.record("COUNT_BLOCK_PACKET",
                "material=" + material.id()
                        + " source=" + (pickobulus ? "pickobulus" : "mining/spread"));
        RotClientClient.onBlocksBroken(material, 1, baseDrop);
    }

    void tick(
            Minecraft client,
            MiningBreakScanFrame frame,
            boolean explicitlySelectedTarget) {
        if (client.level == null || client.player == null || frame == null) {
            reset();
            return;
        }
        if (!MiningBlockEvidencePolicy.allows(
                material.id(),
                SkyBlockAreaDetector.detectLocation(),
                explicitlySelectedTarget)) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        // Material-local aim retained for diagnostics; correlation proximity
        // also accepts SharedMiningAimEvidence (any attacked block).
        if (client.options.keyAttack.isDown()
                && client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && material.isTrackedBlock(
                        client.level.getBlockState(hit.getBlockPos()))) {
            BlockPos target = hit.getBlockPos().immutable();
            recentTargets.put(target, now);
            if (!target.equals(lastLoggedTarget)) {
                DiagnosticRecorder.record("AIM_BLOCK",
                        "material=" + material.id() + " target changed");
                lastLoggedTarget = target;
            }
        }

        Iterator<Map.Entry<BlockPos, Long>> targets = recentTargets.entrySet().iterator();
        while (targets.hasNext()) {
            if (now - targets.next().getValue() > 2_500L) targets.remove();
        }
        recentlyCounted.entrySet().removeIf(entry -> now - entry.getValue() > 2_500L);

        Map<BlockPos, Integer> currentBlocks = new HashMap<>();
        int broken = 0;
        long baseDrops = 0;

        for (MiningBreakScanFrame.Sample sample : frame.samples()) {
            BlockPos pos = sample.pos();
            int baseDrop = material.baseDrop(sample.state());
            if (baseDrop > 0) {
                currentBlocks.put(pos, baseDrop);
            } else if (initialized
                    && knownBlocks.containsKey(pos)
                    && isNearRecentTarget(pos)
                    && !wasRecentlyCounted(pos, now)) {
                broken++;
                baseDrops += knownBlocks.get(pos);
                recentlyCounted.put(pos, now);
                DiagnosticRecorder.record("COUNT_BLOCK",
                        "material=" + material.id()
                                + " newState=" + sample.state().getBlock());
            }
        }

        knownBlocks.clear();
        knownBlocks.putAll(currentBlocks);
        initialized = true;
        if (broken > 0) {
            RotClientClient.onBlocksBroken(material, broken, baseDrops);
        }
    }

    private boolean isNearRecentTarget(BlockPos changed) {
        if (SharedMiningAimEvidence.isNearRecentAim(changed)) {
            return true;
        }
        for (BlockPos target : recentTargets.keySet()) {
            int dx = Math.abs(changed.getX() - target.getX());
            int dy = Math.abs(changed.getY() - target.getY());
            int dz = Math.abs(changed.getZ() - target.getZ());
            if (dx <= 2 && dy <= 2 && dz <= 2) return true;
        }
        return false;
    }

    private boolean wasRecentlyCounted(BlockPos pos, long now) {
        Long countedAt = recentlyCounted.get(pos);
        return countedAt != null && now - countedAt <= 2_500L;
    }
}
