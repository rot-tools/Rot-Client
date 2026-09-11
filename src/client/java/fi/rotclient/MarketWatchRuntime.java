package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class MarketWatchRuntime {
    private static final MarketWatchRuntime DEFAULT =
            new MarketWatchRuntime(
                    new MarketWatchManager());

    private final MarketWatchManager manager;
    private final MarketWatchAlertEngine alertEngine =
            new MarketWatchAlertEngine();

    private static final int ALERT_HISTORY_LIMIT = 100;

    private final ArrayDeque<MarketWatchLiveAlert> pendingAlerts =
            new ArrayDeque<>();

    /*
     * Newest alert first. This is independent from the popup queue,
     * so displaying a popup never removes dashboard history.
     */
    private final ArrayDeque<MarketWatchLiveAlert> alertHistory =
            new ArrayDeque<>();

    private boolean started;

    MarketWatchRuntime(
            MarketWatchManager manager) {

        this.manager =
                Objects.requireNonNull(
                        manager,
                        "manager");
    }

    static void start() {
        DEFAULT.startInternal();
    }

    static void publishAuctionSnapshot(
            MarketWatchAuctionSnapshot snapshot,
            long observedAtMillis) {

        DEFAULT.acceptAuctionSnapshot(
                snapshot,
                observedAtMillis);
    }

    static void publishBazaarSnapshot(
            MarketWatchBazaarSnapshot snapshot,
            long observedAtMillis) {

        DEFAULT.acceptBazaarSnapshot(
                snapshot,
                observedAtMillis);
    }

    static List<MarketWatchLiveAlert> drainPendingAlerts() {
        return DEFAULT.drainAlerts();
    }

    static List<MarketWatchLiveAlert> alertHistory() {
        return DEFAULT.historySnapshot();
    }

    static void clearAlertHistory() {
        DEFAULT.clearAlertStateInternal();
    }

    static boolean enabled() {
        return DEFAULT.manager.enabled();
    }

    static boolean setEnabled(boolean enabled) {
        return DEFAULT.manager.setEnabled(enabled);
    }

    static List<MarketWatchAuctionWatch> auctionWatches() {
        return DEFAULT.manager.auctionWatches();
    }

    static List<MarketWatchBazaarWatch> bazaarWatches() {
        return DEFAULT.manager.bazaarWatches();
    }

    static MarketWatchAuctionWatch createAuctionWatch(
            String itemName,
            String tier,
            boolean binOnly,
            long maxPriceCoins,
            long cooldownSeconds) {

        MarketWatchAuctionWatch created =
                DEFAULT.manager.createAuctionWatch(
                        "",
                        itemName);

        if (created == null) {
            return null;
        }

        created.tier = tier;
        created.binOnly = binOnly;
        created.maxPriceCoins = maxPriceCoins;
        created.cooldownSeconds = cooldownSeconds;
        created.normalize();

        if (!DEFAULT.manager.updateAuctionWatch(created)) {
            DEFAULT.manager.deleteWatch(created.id);
            return null;
        }

        return DEFAULT.manager.findAuctionWatch(created.id);
    }

    static MarketWatchBazaarWatch createBazaarWatch(
            String productId,
            double maxInstantBuyPrice,
            double minInstantSellPrice,
            double minSpreadCoins,
            double minSpreadPercent,
            long minWeeklyVolume,
            long cooldownSeconds) {

        MarketWatchBazaarWatch created =
                DEFAULT.manager.createBazaarWatch(productId);

        if (created == null) {
            return null;
        }

        created.maxInstantBuyPrice = maxInstantBuyPrice;
        created.minInstantSellPrice = minInstantSellPrice;
        created.minSpreadCoins = minSpreadCoins;
        created.minSpreadPercent = minSpreadPercent;
        created.minWeeklyVolume = minWeeklyVolume;
        created.cooldownSeconds = cooldownSeconds;
        created.normalize();

        if (!DEFAULT.manager.updateBazaarWatch(created)) {
            DEFAULT.manager.deleteWatch(created.id);
            return null;
        }

        return DEFAULT.manager.findBazaarWatch(created.id);
    }

    static boolean updateAuctionWatch(
            MarketWatchAuctionWatch watch) {

        return DEFAULT.manager.updateAuctionWatch(
                watch);
    }

    static boolean updateBazaarWatch(
            MarketWatchBazaarWatch watch) {

        return DEFAULT.manager.updateBazaarWatch(
                watch);
    }

    static boolean deleteWatch(
            String watchId) {

        return DEFAULT.manager.deleteWatch(
                watchId);
    }
    synchronized void startInternal() {
        if (started) {
            return;
        }

        manager.loadFromDisk();
        alertEngine.clear();
        pendingAlerts.clear();
        alertHistory.clear();

        started = true;
    }

    synchronized List<MarketWatchLiveAlert> acceptAuctionSnapshot(
            MarketWatchAuctionSnapshot snapshot,
            long observedAtMillis) {

        if (!started
                || !manager.enabled()
                || snapshot == null
                || observedAtMillis < 0L) {
            return List.of();
        }

        List<MarketWatchAuctionMatch> matches =
                alertEngine.evaluateAuctions(
                        manager.auctionWatches(),
                        snapshot,
                        observedAtMillis);

        if (matches.isEmpty()) {
            return List.of();
        }

        List<MarketWatchLiveAlert> emitted =
                new ArrayList<>();

        for (MarketWatchAuctionMatch match : matches) {
            MarketWatchLiveAlert alert =
                    MarketWatchLiveAlert.fromAuction(
                            match,
                            auctionSellerUuid(
                                    snapshot,
                                    match.auctionUuid()),
                            match.referencePriceCoins(),
                            observedAtMillis);

            pendingAlerts.addLast(alert);
            rememberAlert(alert);
            emitted.add(alert);
        }

        return List.copyOf(emitted);
    }

    synchronized List<MarketWatchLiveAlert> acceptBazaarSnapshot(
            MarketWatchBazaarSnapshot snapshot,
            long observedAtMillis) {

        if (!started
                || !manager.enabled()
                || snapshot == null
                || observedAtMillis < 0L) {
            return List.of();
        }

        List<MarketWatchBazaarMatch> matches =
                alertEngine.evaluateBazaar(
                        manager.bazaarWatches(),
                        snapshot,
                        observedAtMillis);

        if (matches.isEmpty()) {
            return List.of();
        }

        List<MarketWatchLiveAlert> emitted =
                new ArrayList<>();

        for (MarketWatchBazaarMatch match : matches) {
            MarketWatchLiveAlert alert =
                    MarketWatchLiveAlert.fromBazaar(
                            match,
                            observedAtMillis);

            pendingAlerts.addLast(alert);
            rememberAlert(alert);
            emitted.add(alert);
        }

        return List.copyOf(emitted);
    }

    private static String auctionSellerUuid(
            MarketWatchAuctionSnapshot snapshot,
            String auctionUuid) {

        if (snapshot == null
                || auctionUuid == null
                || auctionUuid.isBlank()) {

            return "";
        }

        for (MarketWatchAuction auction
                : snapshot.auctions()) {

            if (auction != null
                    && auctionUuid.equals(
                            auction.uuid())) {

                return auction.auctioneerUuid();
            }
        }

        return "";
    }
    private void rememberAlert(
            MarketWatchLiveAlert alert) {

        if (alert == null) {
            return;
        }

        alertHistory.addFirst(alert);

        while (alertHistory.size()
                > ALERT_HISTORY_LIMIT) {

            alertHistory.removeLast();
        }
    }

    synchronized List<MarketWatchLiveAlert> historySnapshot() {
        if (alertHistory.isEmpty()) {
            return List.of();
        }

        return List.copyOf(
                alertHistory);
    }

    synchronized void clearAlertStateInternal() {
        alertHistory.clear();
        pendingAlerts.clear();
    }

    synchronized List<MarketWatchLiveAlert> drainAlerts() {
        if (pendingAlerts.isEmpty()) {
            return List.of();
        }

        List<MarketWatchLiveAlert> alerts =
                new ArrayList<>(
                        pendingAlerts);

        pendingAlerts.clear();

        return List.copyOf(alerts);
    }
}