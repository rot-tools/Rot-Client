package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;
import java.util.List;

/** Plus-only estimated aiming footprint. It never updates mining accounting. */
final class PickobulusPreviewRuntime {
    private static List<BlockPos> candidates = List.of();
    private static int targetCount;
    private static long refreshed;
    static void clear() { candidates = List.of(); targetCount = 0; refreshed = 0; }
    private static boolean enabled() {
        var config = RotClientClient.qolConfigPublic();
        return config.extras().pickobulusEnabled && PlusOpaqueSettings.bool(config, "pickobulusPreview", false);
    }
    static void tick(Minecraft client) {
        if (!enabled() || client == null || client.level == null || client.player == null
                || SkyBlockAreaDetector.detect() == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) { clear(); return; }
        var held = client.player.getMainHandItem();
        if (InventoryChromeRuntime.loreLines(held).stream().noneMatch(line -> line.contains("Pickobulus"))) { clear(); return; }
        long now = System.currentTimeMillis();
        if (now - refreshed < 250) return;
        refreshed = now;
        var config = RotClientClient.qolConfigPublic();
        int radius = Math.max(1, Math.min(5, PlusOpaqueSettings.integer(config, "pickobulusPreviewRadius", 3)));
        int range = Math.max(4, Math.min(64, PlusOpaqueSettings.integer(config, "pickobulusPreviewRange", 32)));
        boolean sphere = PlusOpaqueSettings.bool(config, "pickobulusPreviewSphere", false);
        var eye = client.player.getEyePosition();
        var hit = client.level.clip(new ClipContext(eye, eye.add(client.player.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        if (hit.getType() != HitResult.Type.BLOCK) { candidates = List.of(); targetCount = 0; return; }
        var center = hit.getBlockPos();
        List<BlockPos> blocks = new ArrayList<>();
        int selected = 0;
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            if (!PickobulusPreviewPolicy.inside(dx, dy, dz, radius, sphere)) continue;
            BlockPos pos = center.offset(dx, dy, dz);
            if (!client.level.hasChunkAt(pos)) continue;
            var state = client.level.getBlockState(pos);
            boolean candidate = false, target = false;
            for (var material : TrackedMaterial.values()) {
                if (material.isTrackedBlock(state)) {
                    candidate = true;
                    target |= RotClientClient.tracksMaterial(material);
                }
            }
            var gemstone = GemstoneBlockClassifier.fromBlockState(state);
            if (gemstone != null) { candidate = true; target |= RotClientClient.selectedSelection().tracksGemstone(gemstone); }
            // Gemstone TARGET routing stays with the existing gemstone tracker; preview makes no claim.
            if (candidate) { blocks.add(pos.immutable()); if (target) selected++; }
        }
        candidates = List.copyOf(blocks); targetCount = selected;
    }
    static List<String> hudLines() {
        if (!enabled()) return List.of();
        return List.of("Estimated candidates: " + candidates.size(), "Target candidates: " + targetCount);
    }
    static void renderGizmos() {
        if (!enabled()) return;
        var config = RotClientClient.qolConfigPublic();
        int color = config.extras().pickobulusColor;
        for (BlockPos pos : candidates) {
            var box = Gizmos.cuboid(new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                    GizmoStyle.stroke(color, 1.5F));
            if (PlusOpaqueSettings.bool(config, "pickobulusPreviewThroughWalls", false)) box.setAlwaysOnTop();
        }
    }
}
