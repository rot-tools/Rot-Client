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
    private static final PickobulusPreviewPolicy.Cache<BlockPos> cache = new PickobulusPreviewPolicy.Cache<>();
    private record MaterialCandidate(TrackedMaterial material, boolean target) {}
    static void clear() { cache.clear(); }

    private static PickobulusPreviewPolicy.Context context(Minecraft client) {
        var config = RotClientClient.qolConfigPublic();
        if (client == null || client.level == null || client.player == null || client.getConnection() == null
                || !config.extras().pickobulusEnabled || !PlusOpaqueSettings.bool(config, "pickobulusPreview", false)
                || !SkyBlockAreaDetector.isInSkyblock()) return null;
        var location = SkyBlockAreaDetector.detectLocation();
        if (location.isUnknown()) return null;
        var held = client.player.getMainHandItem();
        var lore = InventoryChromeRuntime.loreLines(held).stream().limit(128).toList();
        if (!PickobulusPolicy.hasPickobulusAbility(lore)) return null;
        var settings = new PickobulusPreviewPolicy.Settings(
                PlusOpaqueSettings.integer(config, "pickobulusPreviewRadius", 3),
                PlusOpaqueSettings.integer(config, "pickobulusPreviewRange", 32),
                PlusOpaqueSettings.bool(config, "pickobulusPreviewSphere", false),
                PlusOpaqueSettings.bool(config, "pickobulusPreviewThroughWalls", false));
        String tool = SkyBlockItemIdentity.skyBlockId(held) + ":" + SkyBlockItemData.uuid(held);
        return new PickobulusPreviewPolicy.Context(client.level, client.player, config, location,
                RotClientClient.currentProfileId(), RotClientClient.selectedSelection().id(), tool, lore, settings);
    }

    static void tick(Minecraft client) {
        long now = System.currentTimeMillis();
        var context = context(client);
        if (!cache.sync(context, now)) return;
        var settings = context.settings();
        var eye = client.player.getEyePosition();
        var hit = client.level.clip(new ClipContext(eye, eye.add(client.player.getLookAngle().scale(settings.range())),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        // Retire the old footprint immediately on aim changes, without scanning every mouse movement.
        cache.aim(hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().immutable() : null);
        if (!cache.beginScan(context, now)) return;
        var center = hit.getBlockPos();
        var selection = RotClientClient.selectedSelection();
        List<String> selectedMaterials = selection.isMaterial()
                ? selection.materialTarget().materials().stream().map(TrackedMaterial::id).toList() : List.of();
        List<MaterialCandidate> materials = new ArrayList<>();
        for (var material : TrackedMaterial.values()) {
            boolean target = PickobulusPreviewPolicy.materialTarget(selection.isMaterial(), selectedMaterials, material.id());
            if (PickobulusPreviewPolicy.candidateAllowed(material.id(), context.location()))
                materials.add(new MaterialCandidate(material, target));
        }
        List<BlockPos> blocks = new ArrayList<>();
        int selected = 0, radius = settings.radius();
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            if (!PickobulusPreviewPolicy.inside(dx, dy, dz, radius, settings.sphere())) continue;
            BlockPos pos = center.offset(dx, dy, dz);
            if (!client.level.hasChunkAt(pos)) continue;
            var state = client.level.getBlockState(pos);
            boolean candidate = false, target = false;
            for (var material : materials) {
                if (material.material().isTrackedBlock(state)) {
                    candidate = true;
                    target |= material.target();
                }
            }
            var gemstone = GemstoneBlockClassifier.fromBlockState(state);
            if (gemstone != null && PickobulusPreviewPolicy.candidateAllowed(gemstone.id(), context.location())) {
                candidate = true;
                target |= selection.tracksGemstone(gemstone);
            }
            // One position is one estimated candidate, even when multiple block classifiers overlap.
            if (candidate) { blocks.add(pos.immutable()); if (target) selected++; }
        }
        cache.publish(context, blocks, selected, now);
    }
    static List<String> hudLines() {
        var snapshot = cache.snapshot(context(Minecraft.getInstance()), System.currentTimeMillis()).orElse(null);
        if (snapshot == null) return List.of();
        return List.of("Estimated candidates: " + snapshot.candidates().size(), "Target candidates: " + snapshot.targetCount());
    }
    static void renderGizmos() {
        var context = context(Minecraft.getInstance());
        var snapshot = cache.snapshot(context, System.currentTimeMillis()).orElse(null);
        if (snapshot == null) return;
        int color = RotClientClient.qolConfigPublic().extras().pickobulusColor;
        for (BlockPos pos : snapshot.candidates()) {
            var box = Gizmos.cuboid(new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                    GizmoStyle.stroke(color, 1.5F));
            if (context.settings().throughWalls()) box.setAlwaysOnTop();
        }
    }
}
