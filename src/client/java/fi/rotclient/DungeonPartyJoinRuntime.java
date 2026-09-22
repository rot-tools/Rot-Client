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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Cached SkyCrypt dungeon stats for Party Finder display and join kicks. */
final class DungeonPartyJoinRuntime {
    private record Cached(DungeonProfileStatsService.Lookup lookup, long until) {
    }

    private static final int MAX_CACHE_ENTRIES = 256;
    private static final long STATS_TTL_MS = 10 * 60_000L;
    private static final long FAILURE_TTL_MS = 30_000L;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4)).build();
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<DungeonProfileStatsService.Lookup>> IN_FLIGHT =
            new ConcurrentHashMap<>();
    private DungeonPartyJoinRuntime() {
    }

    static void tick(Minecraft client) {
        QolClientFlavorSupport.hooks().dungeonPartyJoinTick(client);
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
        request(name, lookup -> {
            if (client == null) return;
            client.execute(() -> {
                if (client.player == null) return;
                if (athen.partyJoinStats || lookup.failed()) {
                    String line = DungeonPartyFinderPolicy.statsLine(
                            name, lookup.stats().orElse(null), lookup.failure());
                    client.player.sendSystemMessage(Component.literal(line));
                }
                if (!lookup.failed()) {
                    QolClientFlavorSupport.hooks().dungeonPartyJoinMaybeKick(
                            client, name, lookup.stats(), athen);
                }
            });
        }, floor);
    }

    static void prefetch(String player, String floor) {
        request(player, ignored -> {
        }, floor);
    }

    static java.util.Optional<DungeonPartyFinderPolicy.Stats> cached(String player, String floor) {
        return cachedLookup(player, floor).flatMap(DungeonProfileStatsService.Lookup::stats);
    }

    /** The cached lookup, including failures; empty while nothing is cached or a request is pending. */
    static java.util.Optional<DungeonProfileStatsService.Lookup> cachedLookup(String player, String floor) {
        Cached cached = CACHE.get(key(player, floor));
        if (cached == null || System.currentTimeMillis() >= cached.until()) {
            if (cached != null) CACHE.remove(key(player, floor), cached);
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(cached.lookup());
    }

    private static void request(
            String player,
            java.util.function.Consumer<DungeonProfileStatsService.Lookup> callback,
            String floor) {
        String cacheKey = key(player, floor);
        Cached hit = CACHE.get(cacheKey);
        if (hit != null && System.currentTimeMillis() < hit.until()) {
            callback.accept(hit.lookup());
            return;
        }
        if (hit != null) CACHE.remove(cacheKey, hit);
        String url = DungeonProfileStatsService.urlFor(player);
        if (url.isBlank()) {
            callback.accept(new DungeonProfileStatsService.Lookup(
                    java.util.Optional.empty(), ""));
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            CompletableFuture<DungeonProfileStatsService.Lookup> pending =
                    IN_FLIGHT.computeIfAbsent(cacheKey, ignored -> {
                        var future = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                .handle((response, error) -> {
                                    var lookup = error == null && response != null
                                            ? DungeonProfileStatsService.response(
                                                    response.statusCode(), response.body(), floor)
                                            : new DungeonProfileStatsService.Lookup(
                                                    java.util.Optional.empty(), "SkyCrypt request failed");
                                    cache(cacheKey, lookup);
                                    return lookup;
                                });
                        return future;
                    });
            pending.whenComplete((result, error) -> IN_FLIGHT.remove(cacheKey, pending));
            pending.thenAccept(callback);
        } catch (RuntimeException ignored) {
            var failure = new DungeonProfileStatsService.Lookup(
                    java.util.Optional.empty(), "SkyCrypt request failed");
            cache(cacheKey, failure);
            callback.accept(failure);
        }
    }

    private static synchronized void cache(String key, DungeonProfileStatsService.Lookup lookup) {
        long ttl = lookup.failed() ? FAILURE_TTL_MS : STATS_TTL_MS;
        if (CACHE.size() >= MAX_CACHE_ENTRIES && !CACHE.containsKey(key)) {
            long now = System.currentTimeMillis();
            CACHE.entrySet().removeIf(entry -> entry.getValue().until() <= now);
            if (CACHE.size() >= MAX_CACHE_ENTRIES) {
                var keys = CACHE.keySet().iterator();
                if (keys.hasNext()) CACHE.remove(keys.next());
            }
        }
        CACHE.put(key, new Cached(lookup, System.currentTimeMillis() + ttl));
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
