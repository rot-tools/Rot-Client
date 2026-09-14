package fi.rotclient;

import net.minecraft.client.Minecraft;

import java.util.Optional;

/** Plus-only delayed Party Finder auto-kick action. */
final class DungeonPartyJoinAutomationRuntime {
    private record PendingKick(String command, String chat, long atTick) {
    }

    private static PendingKick pending;

    private DungeonPartyJoinAutomationRuntime() {
    }

    static void tick(Minecraft client) {
        if (pending == null || client == null || client.player == null
                || client.player.connection == null
                || client.player.tickCount < pending.atTick()) {
            return;
        }
        PendingKick kick = pending;
        pending = null;
        send(client, kick.command());
        send(client, kick.chat());
    }

    static void maybeKick(
            Minecraft client,
            String player,
            Optional<DungeonPartyFinderPolicy.Stats> stats,
            DungeonAthenSettings settings) {
        if (client == null || client.player == null || settings == null
                || !settings.partyJoinAutoKick || stats == null || stats.isEmpty()) {
            return;
        }
        DungeonPartyFinderPolicy.KickThresholds thresholds =
                new DungeonPartyFinderPolicy.KickThresholds(
                        settings.partyJoinRequiredPb,
                        settings.partyJoinRequiredSecrets,
                        settings.partyJoinRequiredSecretAvg,
                        settings.partyJoinRequiredMp);
        if (!DungeonPartyFinderPolicy.shouldKick(stats.get(), thresholds)) {
            return;
        }
        String reason = DungeonPartyFinderPolicy.kickReason(stats.get(), thresholds);
        pending = new PendingKick(
                DungeonPartyFinderPolicy.partyKickCommand(player),
                settings.partyJoinKickMessage
                        ? (settings.partyJoinSendParty
                        ? DungeonPartyFinderPolicy.partyKickChat(player, reason)
                        : reason)
                        : "",
                client.player.tickCount + Math.max(0, settings.partyJoinMessageDelay));
    }

    private static void send(Minecraft client, String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        client.player.connection.sendCommand(
                command.startsWith("/") ? command.substring(1) : command);
    }
}
