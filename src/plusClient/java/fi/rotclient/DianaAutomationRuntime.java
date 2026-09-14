package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Plus-only command actions for Diana party sharing and automatic warps. */
final class DianaAutomationRuntime {
    private static long lastActionAt;
    private static String lastSharedKey = "";

    private DianaAutomationRuntime() {
    }

    static void reset() {
        lastActionAt = 0L;
        lastSharedKey = "";
    }

    static void maybeAutoWarp(
            Minecraft client,
            boolean moduleEnabled,
            boolean autoWarpEnabled,
            DianaPolicy.WarpPoint warp,
            long now,
            long lastSpadeUseAt) {
        if (!DianaPolicy.shouldAutoWarp(moduleEnabled, autoWarpEnabled)
                || warp == null
                || client == null
                || client.player == null
                || client.player.connection == null
                || now - lastSpadeUseAt >= 1_500L
                || now - lastActionAt <= 2_000L) {
            return;
        }
        lastActionAt = now;
        client.player.connection.sendCommand(warp.command());
    }

    static void maybePartyShare(
            Minecraft client,
            boolean moduleEnabled,
            boolean partyShareEnabled,
            DianaPolicy.RareMob mob,
            BlockPos position,
            long now) {
        if (!DianaPolicy.shouldPartyShare(moduleEnabled, partyShareEnabled, mob)
                || position == null
                || client == null
                || client.player == null
                || client.player.connection == null
                || now - lastActionAt <= DianaPolicy.PARTY_SHARE_COOLDOWN_MS) {
            return;
        }
        String shareKey = mob.display() + ":" + position.getX() + ":" + position.getZ();
        if (shareKey.equals(lastSharedKey)) {
            return;
        }
        lastSharedKey = shareKey;
        lastActionAt = now;
        client.player.connection.sendCommand(
                "pc " + DianaPolicy.partyShareLine(
                        mob, position.getX(), position.getY(), position.getZ()));
    }
}
