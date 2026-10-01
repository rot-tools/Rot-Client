package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

final class PuzzlerRuntime {
    private static PuzzlerPolicy.Target target;
    private static long seenAt;
    static void clear() { target = null; seenAt = 0; }
    private static boolean enabled() {
        var config = RotClientClient.qolConfigPublic();
        return config.extras().miningHelpersEnabled
                && PlusOpaqueSettings.bool(config, "miningPuzzler", false);
    }
    static void onChat(Component message) {
        if (!enabled() || message == null || SkyBlockAreaDetector.detect() != SkyBlockArea.DWARVEN_MINES) return;
        String text = CommissionDisplayPolicy.normalizeLine(message.getString());
        if (!text.startsWith("[NPC] Puzzler:")) return;
        // A new NPC response retires the previous target, including completion and wrong-answer chat.
        target = PuzzlerPolicy.solve(text).orElse(null);
        seenAt = System.currentTimeMillis();
    }
    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || client.level == null || client.player == null
                || SkyBlockAreaDetector.detect() != SkyBlockArea.DWARVEN_MINES) { clear(); return; }
        if (target == null || System.currentTimeMillis() - seenAt > 120_000L) return;
        AABB box = new AABB(target.x(), target.y(), target.z(), target.x() + 1, target.y() + 1, target.z() + 1);
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(0xFFFF5555, 2.0F, 0x44FF5555));
    }
}
