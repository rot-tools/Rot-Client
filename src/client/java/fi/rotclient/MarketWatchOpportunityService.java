package fi.rotclient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class MarketWatchOpportunityService {

    private static final long CHECK_SECONDS =
            2L;

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "RotClient-MarketWatch-Opportunities");

                        thread.setDaemon(true);
                        return thread;
                    });

    private static final AtomicBoolean STARTED =
            new AtomicBoolean();

    private static volatile MarketWatchOpportunitySnapshot CURRENT =
            MarketWatchOpportunitySnapshot.empty();

    private static volatile double budgetCoins =
            MarketWatchOpportunityPreferences
                    .loadBudgetCoins();

    private static volatile long lastAuctionTimestamp =
            Long.MIN_VALUE;

    private static volatile long lastBazaarTimestamp =
            Long.MIN_VALUE;

    private static volatile long lastAuctionObserved =
            Long.MIN_VALUE;

    private static volatile long lastBazaarObserved =
            Long.MIN_VALUE;

    private MarketWatchOpportunityService() {
    }

    static MarketWatchOpportunitySnapshot current() {
        start();
        return CURRENT;
    }

    static double budgetCoins() {
        return budgetCoins;
    }

    static boolean budgetUpdatePending() {
        start();

        MarketWatchOpportunitySnapshot snapshot =
                CURRENT;

        if (snapshot == null
                || !snapshot.hasMarketData()) {

            return false;
        }

        return Math.abs(
                snapshot.budgetCoins()
                        - budgetCoins)
                >= 0.5D;
    }

    static void setBudgetCoins(
            double requestedBudget) {

        double next =
                MarketWatchOpportunityPreferences
                        .normalizeBudget(
                                requestedBudget);

        if (Math.abs(
                next - budgetCoins)
                < 0.5D) {

            return;
        }

        budgetCoins =
                next;

        /*
         * Force the next scan even if Hypixel's snapshot timestamps have
         * not changed, because affordability/position sizing changed.
         */
        lastAuctionTimestamp =
                Long.MIN_VALUE;

        lastBazaarTimestamp =
                Long.MIN_VALUE;

        lastAuctionObserved =
                Long.MIN_VALUE;

        lastBazaarObserved =
                Long.MIN_VALUE;

        if (STARTED.get()) {
            EXECUTOR.execute(
                    MarketWatchOpportunityService
                            ::refreshSafely);
        }
    }

    private static void start() {
        if (!STARTED.compareAndSet(
                false,
                true)) {

            return;
        }

        EXECUTOR.scheduleWithFixedDelay(
                MarketWatchOpportunityService
                        ::refreshSafely,
                0L,
                CHECK_SECONDS,
                TimeUnit.SECONDS);
    }

    private static void refreshSafely() {
        try {
            refresh();
        } catch (RuntimeException ignored) {
            // Opportunity analysis must never affect normal Market Watch.
        }
    }

    private static void refresh() {
        MarketWatchDataService.AuctionState auctionState =
                MarketWatchDataService
                        .currentAuctions();

        MarketWatchDataService.BazaarState bazaarState =
                MarketWatchDataService
                        .currentBazaar();

        MarketWatchAuctionSnapshot auctions =
                auctionState != null
                        && auctionState.available()
                        ? auctionState.snapshot()
                        : null;

        MarketWatchBazaarSnapshot bazaar =
                bazaarState != null
                        && bazaarState.available()
                        ? bazaarState.snapshot()
                        : null;

        long auctionTimestamp =
                auctions == null
                        ? -1L
                        : auctions.lastUpdated();

        long bazaarTimestamp =
                bazaar == null
                        ? -1L
                        : bazaar.lastUpdated();

        long auctionObserved =
                auctionState == null
                        ? -1L
                        : auctionState.observedAtMillis();

        long bazaarObserved =
                bazaarState == null
                        ? -1L
                        : bazaarState.observedAtMillis();

        if (auctionTimestamp
                == lastAuctionTimestamp
                && bazaarTimestamp
                == lastBazaarTimestamp
                && auctionObserved
                == lastAuctionObserved
                && bazaarObserved
                == lastBazaarObserved) {

            return;
        }

        CURRENT =
                MarketWatchOpportunityEngine
                        .scan(
                                auctions,
                                bazaar,
                                System.currentTimeMillis(),
                                budgetCoins,
                                MarketWatchSkyBlockItemDecoder
                                        ::stackCount);

        lastAuctionTimestamp =
                auctionTimestamp;

        lastBazaarTimestamp =
                bazaarTimestamp;

        lastAuctionObserved =
                auctionObserved;

        lastBazaarObserved =
                bazaarObserved;
    }
}