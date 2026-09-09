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

    private final ArrayDeque<MarketWatchLiveAlert> pendingAlerts =
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

    synchronized void startInternal() {
        if (started) {
            return;
        }

        manager.loadFromDisk();
        alertEngine.clear();
        pendingAlerts.clear();

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
                            observedAtMillis);

            pendingAlerts.addLast(alert);
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
            emitted.add(alert);
        }

        return List.copyOf(emitted);
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