package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

final class PuzzlerRuntime {
    private static final PuzzlerPolicy.State state = new PuzzlerPolicy.State();
    static void clear() { state.clear(); }
    private static PuzzlerPolicy.Scope scope(Minecraft client) {
        var config = RotClientClient.qolConfigPublic();
        if (client == null || client.level == null || client.player == null
                || !config.extras().miningHelpersEnabled
                || !PlusOpaqueSettings.bool(config, "miningPuzzler", false)
                || !SkyBlockAreaDetector.isInSkyblock()
                || SkyBlockAreaDetector.detect() != SkyBlockArea.DWARVEN_MINES) return null;
        return new PuzzlerPolicy.Scope(client.level, client.player, config, RotClientClient.currentProfileId());
    }
    static void onChat(Component message) {
        state.observe(message == null ? null : message.getString(), scope(Minecraft.getInstance()),
                System.currentTimeMillis());
    }
    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        var target = state.current(scope(client), System.currentTimeMillis()).orElse(null);
        if (target == null || !client.level.hasChunkAt(new BlockPos(target.x(), target.y(), target.z()))) return;
        // Offset the marker above the floor face so depth-tested fill cannot z-fight with the tile.
        AABB box = new AABB(target.x(), target.y() + 1.005, target.z(),
                target.x() + 1, target.y() + 1.025, target.z() + 1);
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(0xFFFF5555, 2.0F, 0x66FF5555));
    }
}
