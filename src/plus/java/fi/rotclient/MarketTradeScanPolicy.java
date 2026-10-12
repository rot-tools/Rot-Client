package fi.rotclient;

import java.util.*;

/** Incremental selected-market index. A large API response never causes one unbounded client tick. */
final class MarketTradeScanPolicy {
    static final int TICK_LISTINGS = 512;
    static final int MAX_MATCHES = 20_000;
    static final int MAX_PEERS = 128;
    private static final Comparator<MarketWatchAuction> PRICE = Comparator.comparingLong(MarketWatchAuction::startingBid)
            .thenComparing(MarketWatchAuction::uuid);
    private final List<MarketWatchAuction> source;
    private final List<MarketTradeCandidatePolicy.Target> targets;
    private final Map<String, MarketWatchAuction> byUuid = new HashMap<>();
    private final Map<String, Group> groups = new LinkedHashMap<>();
    private final MarketTradeSettings settings;
    private final String owner;
    private final long now;
    private int cursor;
    private boolean invalid;
    MarketTradeScanPolicy(List<MarketWatchAuction> source, List<MarketTradeCandidatePolicy.Target> targets) {
        this(source, targets, new MarketTradeSettings(), "", 0);
    }
    MarketTradeScanPolicy(List<MarketWatchAuction> source, List<MarketTradeCandidatePolicy.Target> targets,
            MarketTradeSettings settings, String owner, long now) {
        this.source = source; this.targets = List.copyOf(targets);
        this.settings = settings; this.owner = owner; this.now = now;
        if (targets.isEmpty()) cursor = source.size();
    }
    int step(int limit) {
        int processed = 0;
        while (!invalid && cursor < source.size() && processed < Math.max(0, Math.min(limit, TICK_LISTINGS))) {
            var a = source.get(cursor++); processed++;
            if (!a.bin() || !a.variant().known() || targets.stream().noneMatch(t -> t.matches(a))) continue;
            if (byUuid.size() >= MAX_MATCHES || byUuid.putIfAbsent(a.uuid(), a) != null) { invalid = true; break; }
            if (!MarketTradeCandidatePolicy.identityEligible(a, settings, owner, now)) continue;
            var group = groups.computeIfAbsent(MarketWatchVariantPolicy.comparableKey(a), key -> new Group());
            group.add(a);
            group.addCandidate(a);
        }
        return processed;
    }
    boolean complete() { return invalid || cursor == source.size(); }
    boolean valid() { return complete() && !invalid; }
    MarketWatchAuction auction(String uuid) { return valid() ? byUuid.get(uuid) : null; }
    List<MarketWatchAuction> candidates() {
        if (!valid()) return List.of();
        // Visit every selected variant fairly. Cheap ineligible items cannot hide a different valid variant.
        var queues = new ArrayDeque<Iterator<MarketWatchAuction>>();
        for (var group : groups.values()) if (!group.candidates.isEmpty()) queues.add(group.candidates.iterator());
        var result = new ArrayList<MarketWatchAuction>();
        while (!queues.isEmpty()) {
            var next = queues.removeFirst(); result.add(next.next()); if (next.hasNext()) queues.addLast(next);
        }
        return List.copyOf(result);
    }
    List<MarketWatchAuction> peers(String key) {
        var group = groups.get(key); return valid() && group != null ? group.peers() : List.of();
    }
    private static final class Group {
        final PriorityQueue<MarketWatchAuction> cheapest = new PriorityQueue<>(PRICE.reversed());
        final List<MarketWatchAuction> candidates = new ArrayList<>();
        List<MarketWatchAuction> cache;
        void add(MarketWatchAuction a) { cheapest.add(a); if (cheapest.size() > MAX_PEERS) cheapest.poll(); }
        void addCandidate(MarketWatchAuction a) {
            candidates.add(a);
            if (candidates.size() > 1 && PRICE.compare(a, candidates.getFirst()) < 0) {
                candidates.set(candidates.size() - 1, candidates.getFirst()); candidates.set(0, a);
            }
        }
        List<MarketWatchAuction> peers() { if (cache == null) cache = cheapest.stream().sorted(PRICE).toList(); return cache; }
    }
}
