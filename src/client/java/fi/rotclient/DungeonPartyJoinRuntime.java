package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cached SkyCrypt dungeon stats for Party Finder display and join kicks. */
final class DungeonPartyJoinRuntime {
    private record Cached(DungeonPartyFinderPolicy.Stats stats, long at) {
    }

    private record PendingKick(String player, String command, String chat, long atTick) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4)).build();
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
    private static PendingKick pending;

    private DungeonPartyJoinRuntime() {
    }

    static void tick(Minecraft client) {
        if (pending == null || client == null || client.player == null || client.player.connection == null) {
            return;
        }
        if (client.player.tickCount < pending.atTick()) {
            return;
        }
        PendingKick kick = pending;
        pending = null;
        if (!kick.command.isBlank()) {
            client.player.connection.sendCommand(kick.command.startsWith("/")
                    ? kick.command.substring(1) : kick.command);
        }
        if (!kick.chat.isBlank()) {
            client.player.connection.sendCommand(kick.chat.startsWith("/")
                    ? kick.chat.substring(1) : kick.chat);
        }
    }

    static void onChat(String raw, Minecraft client) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.partyJoinEnabled || raw == null) {
            return;
        }
        var player = DungeonPartyFinderPolicy.joinPlayer(raw);
        if (player.isEmpty()) {
            return;
        }
        String name = player.get();
        String floor = athen.partyJoinDetectFloor
                ? DungeonCarryPolicy.normalizeFloor(DungeonRuntime.sidebar().floor())
                : "F7";
        request(name, stats -> {
            if (client == null || client.player == null) {
                return;
            }
            if (athen.partyJoinStats) {
                client.player.sendSystemMessage(Component.literal(
                        DungeonPartyFinderPolicy.statsLine(name, stats.orElse(null))));
            }
            if (!athen.partyJoinAutoKick || stats.isEmpty()) {
                return;
            }
            DungeonPartyFinderPolicy.KickThresholds thresholds = new DungeonPartyFinderPolicy.KickThresholds(
                    athen.partyJoinRequiredPb,
                    athen.partyJoinRequiredSecrets,
                    athen.partyJoinRequiredSecretAvg,
                    athen.partyJoinRequiredMp);
            if (!DungeonPartyFinderPolicy.shouldKick(stats.get(), thresholds)) {
                return;
            }
            String reason = DungeonPartyFinderPolicy.kickReason(stats.get(), thresholds);
            long delayTicks = Math.max(0, athen.partyJoinMessageDelay);
            pending = new PendingKick(
                    name,
                    DungeonPartyFinderPolicy.partyKickCommand(name),
                    athen.partyJoinKickMessage
                            ? (athen.partyJoinSendParty
                            ? DungeonPartyFinderPolicy.partyKickChat(name, reason)
                            : reason)
                            : "",
                    client.player.tickCount + delayTicks);
        }, floor);
    }

    static void prefetch(String player, String floor) {
        request(player, ignored -> {
        }, floor);
    }

    static java.util.Optional<DungeonPartyFinderPolicy.Stats> cached(String player, String floor) {
        Cached cached = CACHE.get(key(player, floor));
        if (cached == null || System.currentTimeMillis() - cached.at > 10 * 60_000L) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(cached.stats);
    }

    private static void request(
            String player,
            java.util.function.Consumer<java.util.Optional<DungeonPartyFinderPolicy.Stats>> callback,
            String floor) {
        java.util.Optional<DungeonPartyFinderPolicy.Stats> hit = cached(player, floor);
        if (hit.isPresent()) {
            callback.accept(hit);
            return;
        }
        String url = DungeonProfileStatsService.urlFor(player);
        if (url.isBlank()) {
            callback.accept(java.util.Optional.empty());
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        var stats = DungeonProfileStatsService.parse(response.body(), floor);
                        stats.ifPresent(value -> CACHE.put(key(player, floor),
                                new Cached(value, System.currentTimeMillis())));
                        callback.accept(stats);
                    })
                    .exceptionally(ignored -> {
                        callback.accept(java.util.Optional.empty());
                        return null;
                    });
        } catch (RuntimeException ignored) {
            callback.accept(java.util.Optional.empty());
        }
    }

    private static String key(String player, String floor) {
        return DungeonCarryPolicy.sanitizePlayer(player).toLowerCase(Locale.ROOT)
                + "|"
                + DungeonCarryPolicy.normalizeFloor(floor);
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
