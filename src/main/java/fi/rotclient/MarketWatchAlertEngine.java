package fi.rotclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MarketWatchAlertEngine {
    private final Map<String, Long> auctionLastAlertMillis =
            new HashMap<>();

    private final Map<String, Long> bazaarLastAlertMillis =
            new HashMap<>();

    private final Set<String> seenAuctionMatches =
            new HashSet<>();

    private final Set<String> seenBazaarSnapshots =
            new HashSet<>();

    List<MarketWatchAuctionMatch> evaluateAuctions(
            List<MarketWatchAuctionWatch> watches,
            MarketWatchAuctionSnapshot snapshot,
            long nowMillis) {

        if (nowMillis < 0L) {
            return List.of();
        }

        List<MarketWatchAuctionMatch> candidates =
                MarketWatchEvaluator.evaluateAuctions(
                        watches,
                        snapshot);

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<MarketWatchAuctionMatch> alerts =
                new ArrayList<>();

        for (MarketWatchAuctionMatch candidate
                : candidates) {

            MarketWatchAuctionWatch watch =
                    findAuctionWatch(
                            watches,
                            candidate.watchId());

            if (watch == null) {
                continue;
            }

            String matchKey =
                    candidate.watchId()
                            + ":"
                            + candidate.auctionUuid();

            /*
             * An AH UUID identifies one specific auction.
             * Never alert twice for the exact same listing during
             * this client session.
             */
            if (seenAuctionMatches.contains(matchKey)) {
                continue;
            }

            if (!cooldownElapsed(
                    auctionLastAlertMillis.get(
                            candidate.watchId()),
                    watch.cooldownSeconds,
                    nowMillis)) {
                continue;
            }

            seenAuctionMatches.add(matchKey);

            auctionLastAlertMillis.put(
                    candidate.watchId(),
                    nowMillis);

            alerts.add(candidate);
        }

        return List.copyOf(alerts);
    }

    List<MarketWatchBazaarMatch> evaluateBazaar(
            List<MarketWatchBazaarWatch> watches,
            MarketWatchBazaarSnapshot snapshot,
            long nowMillis) {

        if (nowMillis < 0L
                || snapshot == null) {
            return List.of();
        }

        List<MarketWatchBazaarMatch> candidates =
                MarketWatchEvaluator.evaluateBazaar(
                        watches,
                        snapshot);

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<MarketWatchBazaarMatch> alerts =
                new ArrayList<>();

        for (MarketWatchBazaarMatch candidate
                : candidates) {

            MarketWatchBazaarWatch watch =
                    findBazaarWatch(
                            watches,
                            candidate.watchId());

            if (watch == null) {
                continue;
            }

            /*
             * Running the evaluator twice against the same Bazaar API
             * snapshot must not create two identical alerts.
             */
            String snapshotKey =
                    candidate.watchId()
                            + ":"
                            + snapshot.lastUpdated();

            if (seenBazaarSnapshots.contains(
                    snapshotKey)) {
                continue;
            }

            if (!cooldownElapsed(
                    bazaarLastAlertMillis.get(
                            candidate.watchId()),
                    watch.cooldownSeconds,
                    nowMillis)) {
                continue;
            }

            seenBazaarSnapshots.add(snapshotKey);

            bazaarLastAlertMillis.put(
                    candidate.watchId(),
                    nowMillis);

            alerts.add(candidate);
        }

        return List.copyOf(alerts);
    }

    void clear() {
        auctionLastAlertMillis.clear();
        bazaarLastAlertMillis.clear();
        seenAuctionMatches.clear();
        seenBazaarSnapshots.clear();
    }

    private static boolean cooldownElapsed(
            Long lastAlertMillis,
            long cooldownSeconds,
            long nowMillis) {

        if (lastAlertMillis == null) {
            return true;
        }

        long safeCooldownSeconds =
                Math.max(
                        0L,
                        cooldownSeconds);

        long cooldownMillis;

        try {
            cooldownMillis =
                    Math.multiplyExact(
                            safeCooldownSeconds,
                            1000L);
        } catch (ArithmeticException ignored) {
            cooldownMillis = Long.MAX_VALUE;
        }

        long elapsed =
                nowMillis >= lastAlertMillis
                        ? nowMillis - lastAlertMillis
                        : 0L;

        return elapsed >= cooldownMillis;
    }

    private static MarketWatchAuctionWatch findAuctionWatch(
            List<MarketWatchAuctionWatch> watches,
            String watchId) {

        if (watches == null
                || watchId == null
                || watchId.isBlank()) {
            return null;
        }

        for (MarketWatchAuctionWatch watch : watches) {
            if (watch != null
                    && watchId.equals(watch.id)) {
                return watch;
            }
        }

        return null;
    }

    private static MarketWatchBazaarWatch findBazaarWatch(
            List<MarketWatchBazaarWatch> watches,
            String watchId) {

        if (watches == null
                || watchId == null
                || watchId.isBlank()) {
            return null;
        }

        for (MarketWatchBazaarWatch watch : watches) {
            if (watch != null
                    && watchId.equals(watch.id)) {
                return watch;
            }
        }

        return null;
    }
}