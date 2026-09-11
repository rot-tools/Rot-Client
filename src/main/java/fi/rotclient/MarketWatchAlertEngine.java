package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MarketWatchAlertEngine {
    /*
     * Auction dynamic reference:
     *
     * - comparable BINs only
     * - candidate itself excluded
     * - cheapest seven comparisons
     * - lower median
     *
     * An arithmetic average is deliberately NOT used because one absurdly
     * expensive listing could otherwise make ordinary auctions appear cheap.
     */
/*
     * Bazaar exposes one current quick-buy value instead of many independent
     * BIN listings. Keep a robust rolling history for percentage watches.
     */
    private static final int BAZAAR_HISTORY_LIMIT = 20;
    private static final int BAZAAR_MIN_HISTORY = 5;

    private final Map<String, Long> auctionLastAlertMillis =
            new HashMap<>();

    /*
     * Last successfully alerted AH price per watch.
     *
     * A newly discovered dynamic BIN that is strictly cheaper than this
     * price is a better opportunity and may bypass the normal watch
     * cooldown. Exact auction UUID dedupe still applies first.
     */
    private final Map<String, Long> auctionLastAlertPrice =
            new HashMap<>();

    private final Map<String, Long> bazaarLastAlertMillis =
            new HashMap<>();

    private final Set<String> seenAuctionMatches =
            new HashSet<>();

    private final Set<String> seenBazaarSnapshots =
            new HashSet<>();

    private final Map<String, ArrayDeque<Double>> bazaarBuyHistory =
            new HashMap<>();

    private final Map<String, Long> bazaarLastRecordedSnapshot =
            new HashMap<>();


    List<MarketWatchAuctionMatch> evaluateAuctions(
            List<MarketWatchAuctionWatch> watches,
            MarketWatchAuctionSnapshot snapshot,
            long nowMillis) {

        if (nowMillis < 0L
                || watches == null
                || watches.isEmpty()
                || snapshot == null
                || snapshot.auctions().isEmpty()) {

            return List.of();
        }

        /*
         * Fixed-price watches still use the original evaluator.
         * That evaluator skips minDiscountPercent watches, so dynamic watches
         * cannot accidentally fire through both paths.
         */
        List<MarketWatchAuctionMatch> candidates =
                new ArrayList<>(
                        MarketWatchEvaluator.evaluateAuctions(
                                watches,
                                snapshot));

        candidates.addAll(
                dynamicAuctionCandidates(
                        watches,
                        snapshot,
                        nowMillis));

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

            if (seenAuctionMatches.contains(
                    matchKey)) {

                continue;
            }

            Long previousAlertPrice =
                    auctionLastAlertPrice.get(
                            candidate.watchId());

            /*
             * Dynamic AH watches may alert again inside the normal cooldown
             * when a genuinely better CURRENT LOWEST BIN appears.
             *
             * Example:
             *
             * 50k qualified and alerted.
             * 26k appears ten seconds later.
             *
             * 26k is a new UUID and strictly cheaper, so alert again.
             *
             * Fixed-price watches retain the normal cooldown behavior.
             */
            boolean betterDynamicDeal =
                    watch.minDiscountPercent > 0.0D
                            && previousAlertPrice != null
                            && candidate.priceCoins() > 0L
                            && candidate.priceCoins()
                            < previousAlertPrice;

            if (!betterDynamicDeal
                    && !cooldownElapsed(
                    auctionLastAlertMillis.get(
                            candidate.watchId()),
                    watch.cooldownSeconds,
                    nowMillis)) {

                continue;
            }

            seenAuctionMatches.add(
                    matchKey);

            auctionLastAlertMillis.put(
                    candidate.watchId(),
                    nowMillis);

            auctionLastAlertPrice.put(
                    candidate.watchId(),
                    candidate.priceCoins());

            alerts.add(
                    candidate);
        }

        return List.copyOf(
                alerts);
    }


    private static List<MarketWatchAuctionMatch>
    dynamicAuctionCandidates(
            List<MarketWatchAuctionWatch> watches,
            MarketWatchAuctionSnapshot snapshot,
            long nowMillis) {

        List<MarketWatchAuctionMatch> result =
                new ArrayList<>();

        for (MarketWatchAuctionWatch sourceWatch : watches) {
            if (sourceWatch == null) {
                continue;
            }

            MarketWatchAuctionWatch watch =
                    sourceWatch.copy();

            if (!watch.enabled
                    || watch.itemName.isBlank()
                    || watch.minDiscountPercent <= 0.0D) {

                continue;
            }

            if (watch.minProfitCoins > 0L
                    || watch.minProfitPercent > 0.0D) {

                continue;
            }

            List<MarketWatchAuction> matching =
                    new ArrayList<>();

            for (MarketWatchAuction auction
                    : snapshot.auctions()) {

                if (!matchesDynamicAuction(
                        watch,
                        auction,
                        nowMillis)) {

                    continue;
                }

                long price =
                        currentAuctionPrice(
                                auction);

                if (price > 0L) {
                    matching.add(
                            auction);
                }
            }

            /*
             * One total listing has no external market reference.
             * Two total listings are allowed, but receive the strongest
             * sparse-market penalty inside MarketWatchDealPolicy.
             */
            if (matching.size() < 2) {
                continue;
            }

            /*
             * Cheapest potential deal first so cooldown prefers the strongest
             * opportunity if several matches appear in one snapshot.
             */
            matching.sort(
                    (left, right) ->
                            Long.compare(
                                    currentAuctionPrice(
                                            left),
                                    currentAuctionPrice(
                                            right)));

            /*
             * V4:
             * Only the cheapest BIN is a possible deal.
             * More expensive listings are irrelevant while a cheaper BIN exists.
             */
            MarketWatchAuction candidate =
                    matching.getFirst();

            {

                long candidatePrice =
                        currentAuctionPrice(
                                candidate);

                if (candidatePrice <= 0L) {
                    continue;
                }

                List<Long> comparisonPrices =
                        new ArrayList<>(
                                Math.max(
                                        1,
                                        matching.size() - 1));

                for (MarketWatchAuction comparison
                        : matching) {

                    /*
                     * Critical rule:
                     * the candidate cannot affect its own market reference.
                     */
                    if (comparison.uuid()
                            .equals(
                                    candidate.uuid())) {

                        continue;
                    }

                    long price =
                            currentAuctionPrice(
                                    comparison);

                    if (price > 0L) {
                        comparisonPrices.add(
                                price);
                    }
                }

                MarketWatchDealPolicy.Analysis analysis =
                        MarketWatchDealPolicy
                                .analyzeAuction(
                                        candidatePrice,
                                        comparisonPrices,
                                        watch.minDiscountPercent);

                if (!analysis.usable()
                        || !analysis.trigger()) {

                    continue;
                }

                result.add(
                        new MarketWatchAuctionMatch(
                                watch.id,
                                candidate.uuid(),
                                candidate.itemName(),
                                candidate.tier(),
                                candidatePrice,
                                true,
                                analysis.referencePrice(),
                                analysis.actualDiscountPercent()));
            }
        }

        return List.copyOf(
                result);
    }


    private static boolean matchesDynamicAuction(
            MarketWatchAuctionWatch watch,
            MarketWatchAuction auction,
            long nowMillis) {

        if (watch == null
                || auction == null
                || auction.uuid().isBlank()
                || !auction.bin()) {

            return false;
        }

        /*
         * Defensive stale-auction filtering.
         */
        if (auction.endMillis() > 0L
                && nowMillis > 0L
                && auction.endMillis() <= nowMillis) {

            return false;
        }

        if (!watch.itemName.equalsIgnoreCase(
                auction.itemName())) {

            return false;
        }

        return watch.tier.isBlank()
                || watch.tier.equalsIgnoreCase(
                        auction.tier());
    }


    private static long currentAuctionPrice(
            MarketWatchAuction auction) {

        if (auction == null) {
            return 0L;
        }

        if (auction.bin()) {
            return auction.startingBid();
        }

        if (auction.highestBidAmount() > 0L) {
            return auction.highestBidAmount();
        }

        return auction.startingBid();
    }


    List<MarketWatchBazaarMatch> evaluateBazaar(
            List<MarketWatchBazaarWatch> watches,
            MarketWatchBazaarSnapshot snapshot,
            long nowMillis) {

        if (nowMillis < 0L
                || watches == null
                || watches.isEmpty()
                || snapshot == null) {

            return List.of();
        }

        /*
         * Build temporary evaluator watches using the PREVIOUS market
         * observations, then record the current observation afterwards.
         * A sudden cheap price therefore cannot lower its own reference first.
         */
        List<MarketWatchBazaarWatch> evaluationWatches =
                bazaarEvaluationWatches(
                        watches,
                        snapshot);

        List<MarketWatchBazaarMatch> candidates =
                MarketWatchEvaluator.evaluateBazaar(
                        evaluationWatches,
                        snapshot);

        recordBazaarSamples(
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

            seenBazaarSnapshots.add(
                    snapshotKey);

            bazaarLastAlertMillis.put(
                    candidate.watchId(),
                    nowMillis);

            alerts.add(
                    candidate);
        }

        return List.copyOf(
                alerts);
    }


    private List<MarketWatchBazaarWatch>
    bazaarEvaluationWatches(
            List<MarketWatchBazaarWatch> watches,
            MarketWatchBazaarSnapshot snapshot) {

        List<MarketWatchBazaarWatch> result =
                new ArrayList<>();

        for (MarketWatchBazaarWatch sourceWatch
                : watches) {

            if (sourceWatch == null) {
                continue;
            }

            MarketWatchBazaarWatch watch =
                    sourceWatch.copy();

            if (watch.buyDealPercent <= 0.0D) {
                result.add(
                        watch);

                continue;
            }

            MarketWatchBazaarProduct product =
                    snapshot.product(
                            watch.productId);

            if (product == null) {
                watch.enabled = false;
                result.add(watch);
                continue;
            }

            ArrayDeque<Double> history =
                    bazaarBuyHistory.get(
                            watch.productId);

            if (history == null
                    || history.size()
                    < BAZAAR_MIN_HISTORY) {

                /*
                 * Disable the temporary copy during warm-up so optional
                 * sell/spread conditions cannot trigger by themselves.
                 */
                watch.enabled = false;
                result.add(watch);
                continue;
            }

            double reference =
                    lowerMedianDouble(
                            history);

            double multiplier =
                    1.0D
                            - watch.buyDealPercent
                            / 100.0D;

            double target =
                    reference
                            * multiplier;

            if (!Double.isFinite(target)
                    || target <= 0.0D) {

                watch.enabled = false;
                result.add(watch);
                continue;
            }

            /*
             * A dynamic watch uses this temporary absolute threshold only for
             * this evaluation. The persisted watch remains a percentage.
             */
            watch.maxInstantBuyPrice =
                    target;

            result.add(
                    watch);
        }

        return List.copyOf(
                result);
    }


    private void recordBazaarSamples(
            List<MarketWatchBazaarWatch> watches,
            MarketWatchBazaarSnapshot snapshot) {

        Set<String> processedProducts =
                new HashSet<>();

        for (MarketWatchBazaarWatch sourceWatch
                : watches) {

            if (sourceWatch == null) {
                continue;
            }

            MarketWatchBazaarWatch watch =
                    sourceWatch.copy();

            if (watch.productId.isBlank()
                    || !processedProducts.add(
                            watch.productId)) {

                continue;
            }

            MarketWatchBazaarProduct product =
                    snapshot.product(
                            watch.productId);

            if (product == null) {
                continue;
            }

            double price =
                    product.quickBuyPrice();

            if (!Double.isFinite(price)
                    || price <= 0.0D) {

                continue;
            }

            Long previousSnapshot =
                    bazaarLastRecordedSnapshot.get(
                            watch.productId);

            if (previousSnapshot != null
                    && previousSnapshot.longValue()
                    == snapshot.lastUpdated()) {

                continue;
            }

            ArrayDeque<Double> history =
                    bazaarBuyHistory.computeIfAbsent(
                            watch.productId,
                            ignored ->
                                    new ArrayDeque<>());

            history.addLast(
                    price);

            while (history.size()
                    > BAZAAR_HISTORY_LIMIT) {

                history.removeFirst();
            }

            bazaarLastRecordedSnapshot.put(
                    watch.productId,
                    snapshot.lastUpdated());
        }
    }


    private static double lowerMedianDouble(
            ArrayDeque<Double> history) {

        if (history == null
                || history.isEmpty()) {

            return 0.0D;
        }

        List<Double> sorted =
                new ArrayList<>(
                        history);

        sorted.removeIf(
                value ->
                        value == null
                                || !Double.isFinite(value)
                                || value <= 0.0D);

        if (sorted.isEmpty()) {
            return 0.0D;
        }

        sorted.sort(
                Double::compare);

        int index =
                (sorted.size() - 1)
                        / 2;

        return sorted.get(
                index);
    }


    void clear() {
        auctionLastAlertMillis.clear();
        auctionLastAlertPrice.clear();
        bazaarLastAlertMillis.clear();

        seenAuctionMatches.clear();
        seenBazaarSnapshots.clear();

        bazaarBuyHistory.clear();
        bazaarLastRecordedSnapshot.clear();
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
            cooldownMillis =
                    Long.MAX_VALUE;
        }

        long elapsed =
                nowMillis >= lastAlertMillis
                        ? nowMillis
                        - lastAlertMillis
                        : 0L;

        return elapsed
                >= cooldownMillis;
    }


    private static MarketWatchAuctionWatch findAuctionWatch(
            List<MarketWatchAuctionWatch> watches,
            String watchId) {

        if (watches == null
                || watchId == null
                || watchId.isBlank()) {

            return null;
        }

        for (MarketWatchAuctionWatch watch
                : watches) {

            if (watch != null
                    && watchId.equals(
                            watch.id)) {

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

        for (MarketWatchBazaarWatch watch
                : watches) {

            if (watch != null
                    && watchId.equals(
                            watch.id)) {

                return watch;
            }
        }

        return null;
    }
}
