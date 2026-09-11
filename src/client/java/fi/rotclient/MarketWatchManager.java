package fi.rotclient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class MarketWatchManager {
    private final Path storagePath;

    private MarketWatchConfig config =
            MarketWatchConfig.defaults();

    MarketWatchManager() {
        this(null);
    }

    MarketWatchManager(Path storagePath) {
        this.storagePath = storagePath;
    }

    void loadFromDisk() {
        config =
                storagePath == null
                        ? MarketWatchStore.load()
                        : MarketWatchStore.load(storagePath);

        if (config == null) {
            config = MarketWatchConfig.defaults();
        }

        config.normalize();
    }

    boolean enabled() {
        config.normalize();
        return config.enabled;
    }

    boolean setEnabled(boolean enabled) {
        if (config.enabled == enabled) {
            return true;
        }

        boolean previous = config.enabled;
        config.enabled = enabled;

        if (!saveNow()) {
            config.enabled = previous;
            return false;
        }

        return true;
    }

    List<MarketWatchAuctionWatch> auctionWatches() {
        config.normalize();

        List<MarketWatchAuctionWatch> result =
                new ArrayList<>();

        for (MarketWatchAuctionWatch watch
                : config.auctionWatches) {
            result.add(watch.copy());
        }

        return List.copyOf(result);
    }

    List<MarketWatchBazaarWatch> bazaarWatches() {
        config.normalize();

        List<MarketWatchBazaarWatch> result =
                new ArrayList<>();

        for (MarketWatchBazaarWatch watch
                : config.bazaarWatches) {
            result.add(watch.copy());
        }

        return List.copyOf(result);
    }

    MarketWatchAuctionWatch createAuctionWatch(
            String itemId,
            String itemName) {

        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemId = itemId;
        watch.itemName = itemName;
        watch.normalize();

        if (watch.itemId.isBlank()
                && watch.itemName.isBlank()) {
            return null;
        }

        config.auctionWatches.add(watch);

        if (!saveNow()) {
            config.auctionWatches.remove(watch);
            return null;
        }

        return watch.copy();
    }

    MarketWatchBazaarWatch createBazaarWatch(
            String productId) {

        MarketWatchBazaarWatch watch =
                new MarketWatchBazaarWatch();

        watch.productId = productId;
        watch.normalize();

        if (watch.productId.isBlank()) {
            return null;
        }

        config.bazaarWatches.add(watch);

        if (!saveNow()) {
            config.bazaarWatches.remove(watch);
            return null;
        }

        return watch.copy();
    }

    boolean updateAuctionWatch(
            MarketWatchAuctionWatch updated) {

        if (updated == null
                || updated.id == null
                || updated.id.isBlank()) {
            return false;
        }

        int index = auctionIndex(updated.id);

        if (index < 0) {
            return false;
        }

        MarketWatchAuctionWatch previous =
                config.auctionWatches.get(index);

        MarketWatchAuctionWatch replacement =
                updated.copy();

        replacement.id = previous.id;
        replacement.normalize();

        if (replacement.itemId.isBlank()
                && replacement.itemName.isBlank()) {
            return false;
        }

        config.auctionWatches.set(
                index,
                replacement);

        if (!saveNow()) {
            config.auctionWatches.set(
                    index,
                    previous);
            return false;
        }

        return true;
    }

    boolean updateBazaarWatch(
            MarketWatchBazaarWatch updated) {

        if (updated == null
                || updated.id == null
                || updated.id.isBlank()) {
            return false;
        }

        int index = bazaarIndex(updated.id);

        if (index < 0) {
            return false;
        }

        MarketWatchBazaarWatch previous =
                config.bazaarWatches.get(index);

        MarketWatchBazaarWatch replacement =
                updated.copy();

        replacement.id = previous.id;
        replacement.normalize();

        if (replacement.productId.isBlank()) {
            return false;
        }

        config.bazaarWatches.set(
                index,
                replacement);

        if (!saveNow()) {
            config.bazaarWatches.set(
                    index,
                    previous);
            return false;
        }

        return true;
    }

    boolean deleteWatch(String watchId) {
        if (watchId == null || watchId.isBlank()) {
            return false;
        }

        String id = watchId.trim();

        int auctionIndex = auctionIndex(id);

        if (auctionIndex >= 0) {
            MarketWatchAuctionWatch removed =
                    config.auctionWatches.remove(
                            auctionIndex);

            if (!saveNow()) {
                config.auctionWatches.add(
                        auctionIndex,
                        removed);
                return false;
            }

            return true;
        }

        int bazaarIndex = bazaarIndex(id);

        if (bazaarIndex >= 0) {
            MarketWatchBazaarWatch removed =
                    config.bazaarWatches.remove(
                            bazaarIndex);

            if (!saveNow()) {
                config.bazaarWatches.add(
                        bazaarIndex,
                        removed);
                return false;
            }

            return true;
        }

        return false;
    }

    MarketWatchAuctionWatch findAuctionWatch(
            String watchId) {

        int index = auctionIndex(watchId);

        if (index < 0) {
            return null;
        }

        return config.auctionWatches
                .get(index)
                .copy();
    }

    MarketWatchBazaarWatch findBazaarWatch(
            String watchId) {

        int index = bazaarIndex(watchId);

        if (index < 0) {
            return null;
        }

        return config.bazaarWatches
                .get(index)
                .copy();
    }

    boolean saveNow() {
        config.normalize();

        return storagePath == null
                ? MarketWatchStore.save(config)
                : MarketWatchStore.save(
                        storagePath,
                        config);
    }

    private int auctionIndex(String watchId) {
        if (watchId == null || watchId.isBlank()) {
            return -1;
        }

        String wanted = watchId.trim();

        for (int i = 0;
                i < config.auctionWatches.size();
                i++) {

            MarketWatchAuctionWatch watch =
                    config.auctionWatches.get(i);

            if (watch != null
                    && wanted.equals(watch.id)) {
                return i;
            }
        }

        return -1;
    }

    private int bazaarIndex(String watchId) {
        if (watchId == null || watchId.isBlank()) {
            return -1;
        }

        String wanted = watchId.trim();

        for (int i = 0;
                i < config.bazaarWatches.size();
                i++) {

            MarketWatchBazaarWatch watch =
                    config.bazaarWatches.get(i);

            if (watch != null
                    && wanted.equals(watch.id)) {
                return i;
            }
        }

        return -1;
    }
}